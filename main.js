const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron');
const { autoUpdater } = require('electron-updater');
const { Client } = require('minecraft-launcher-core');
const { Authflow, Titles } = require('prismarine-auth');
const AdmZip = require('adm-zip');
const { spawn } = require('node:child_process');
const fsSync = require('node:fs');
const { pipeline } = require('node:stream/promises');
const fs = require('node:fs/promises');
const path = require('node:path');
const { createHash, randomUUID } = require('node:crypto');
const { isPathInside, resolveManagedPath } = require('./path-safety');

const DEFAULT_GAME_VERSION = '1.20.1';
const AZURE_CLIENT_MOD_VERSION = '1.0.0';
const MINECRAFT_MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const FABRIC_META_URL = 'https://meta.fabricmc.net/v2';
const FORGE_PROMOTIONS_URL = 'https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json';
const FORGE_MAVEN_URL = 'https://maven.minecraftforge.net/net/minecraftforge/forge';
const MODRINTH_API_URL = 'https://api.modrinth.com/v2';
const EXPLORE_TYPES = new Set(['mod', 'modpack', 'resourcepack', 'shaderpack']);
const MODRINTH_TYPES = {
  mod: 'mod',
  modpack: 'modpack',
  resourcepack: 'resourcepack',
  shaderpack: 'shader'
};
const PERFORMANCE_MODS = {
  fabric: [
    { title: 'Sodium', projectId: 'AANobbMI' },
    { title: 'Lithium', projectId: 'gvQqBUqZ' },
    { title: 'FerriteCore', projectId: 'uXXizFIs' },
    { title: 'Indium', projectId: 'Orvt0mRa' },
    { title: 'Krypton', projectId: 'fQEb0iXm' },
    { title: 'Fabric API', projectId: 'P7dR8mSH' },
    { title: 'ModernFix', projectId: 'nmDcB62a' }
  ],
  forge: [
    { title: 'Embeddium', projectId: 'sk9rgfiA' },
    { title: 'Canary', projectId: 'qa2H4BS9' },
    { title: 'FerriteCore', projectId: 'uXXizFIs' },
    { title: 'ModernFix', projectId: 'nmDcB62a' },
    { title: 'Pluto', projectId: 'I2K4u1Q7' }
  ]
};

let mainWindow;
let launchInProgress = false;
let activeLaunch = null;
let gameProcessStarted = false;
let quitTimer;
let minecraftManifestCache;
let forgeMetadataCache;
const loaderMetadataCache = new Map();

function sendStatus(message, kind = 'info') {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('launcher:status', { message, kind });
  }
}

function createRequestSignal(timeoutMs, signal) {
  const timeoutSignal = AbortSignal.timeout(timeoutMs);
  return signal ? AbortSignal.any([timeoutSignal, signal]) : timeoutSignal;
}

function createLaunchCancelledError() {
  const error = new Error('Launch cancelled.');
  error.code = 'LAUNCH_CANCELLED';
  return error;
}

function ensureLaunchActive(launch) {
  if (launch.cancelled || launch.abortController.signal.aborted) {
    throw createLaunchCancelledError();
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1120,
    height: 720,
    minWidth: 760,
    minHeight: 560,
    frame: false,
    resizable: true,
    show: false,
    backgroundColor: '#0f0f16',
    icon: path.join(__dirname, 'assets', 'azure-icon.ico'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  mainWindow.once('ready-to-show', () => mainWindow.show());
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
  mainWindow.loadFile(path.join(__dirname, 'index.html'));
}

ipcMain.handle('window:minimize', (event) => {
  if (mainWindow && event.sender === mainWindow.webContents) mainWindow.minimize();
});

ipcMain.handle('window:close', (event) => {
  if (mainWindow && event.sender === mainWindow.webContents) mainWindow.close();
});

function getDataPaths() {
  const root = path.join(app.getPath('userData'), '.4vill-data');
  return {
    root,
    instances: path.join(root, 'instances'),
    authCache: path.join(app.getPath('userData'), 'auth-cache'),
    localSkins: path.join(app.getPath('userData'), 'local-skins'),
    optifineProfilesFile: path.join(root, 'optifine-profiles.json'),
    modsSource: path.join(
      app.isPackaged ? path.dirname(app.getPath('exe')) : app.getAppPath(),
      'bin',
      'mods'
    ),
  };
}

async function fetchJson(url, signal) {
  const signals = [AbortSignal.timeout(20000)];
  if (signal) signals.push(signal);
  const response = await fetch(url, { signal: AbortSignal.any(signals) });
  if (!response.ok) throw new Error(`Metadata request failed (${response.status}): ${url}`);
  return response.json();
}

function compareVersionIds(left, right) {
  const a = left.split(/[.-]/).map((part) => Number(part) || 0);
  const b = right.split(/[.-]/).map((part) => Number(part) || 0);
  for (let index = 0; index < Math.max(a.length, b.length); index += 1) {
    const difference = (a[index] || 0) - (b[index] || 0);
    if (difference !== 0) return difference;
  }
  return 0;
}

function isSupportedRelease(version) {
  if (version.type !== 'release' || !/^\d+\.\d+(?:\.\d+)?$/.test(version.id)) return false;
  return compareVersionIds(version.id, '1.7') >= 0;
}

async function getMinecraftManifest(signal) {
  if (minecraftManifestCache && Date.now() - minecraftManifestCache.fetchedAt < 15 * 60 * 1000) {
    return minecraftManifestCache.value;
  }
  const value = await fetchJson(MINECRAFT_MANIFEST_URL, signal);
  minecraftManifestCache = { fetchedAt: Date.now(), value };
  return value;
}

async function getForgeMetadata(signal) {
  if (forgeMetadataCache && Date.now() - forgeMetadataCache.fetchedAt < 60 * 60 * 1000) {
    return forgeMetadataCache.value;
  }
  const [promotionsResponse, versionsResponse] = await Promise.all([
    fetch(FORGE_PROMOTIONS_URL, { signal: createRequestSignal(20000, signal) }),
    fetch(`${FORGE_MAVEN_URL}/maven-metadata.xml`, { signal: createRequestSignal(20000, signal) })
  ]);
  if (!promotionsResponse.ok || !versionsResponse.ok) {
    throw new Error('Could not load Forge version metadata.');
  }
  const promotions = await promotionsResponse.json();
  const metadataXml = await versionsResponse.text();
  const versions = [...metadataXml.matchAll(/<version>([^<]+)<\/version>/g)].map((match) => match[1]);
  forgeMetadataCache = {
    fetchedAt: Date.now(),
    value: { promotions: promotions.promos || {}, versions }
  };
  return forgeMetadataCache.value;
}

async function getLoaderChoices(gameVersion, loader, signal) {
  const cacheKey = `${loader}:${gameVersion}`;
  const cached = loaderMetadataCache.get(cacheKey);
  if (cached && Date.now() - cached.fetchedAt < 60 * 60 * 1000) return cached.value;

  let versions;
  if (loader === 'fabric') {
    const metadata = await fetchJson(`${FABRIC_META_URL}/versions/loader/${encodeURIComponent(gameVersion)}`, signal);
    versions = metadata.map((item) => item.loader.version).filter(Boolean);
  } else if (loader === 'forge') {
    const metadata = await getForgeMetadata(signal);
    const prefix = `${gameVersion}-`;
    versions = metadata.versions
      .filter((version) => version.startsWith(prefix) && !version.slice(prefix.length).includes('-'))
      .map((version) => version.slice(prefix.length));
    const recommended = metadata.promotions[`${gameVersion}-recommended`];
    const latest = metadata.promotions[`${gameVersion}-latest`];
    versions = [...new Set([recommended, latest, ...versions].filter((version) => version && versions.includes(version)))];
    versions.sort((a, b) => compareVersionIds(b, a));
    if (recommended && versions.includes(recommended)) {
      versions = [recommended, ...versions.filter((version) => version !== recommended)];
    }
  } else {
    throw new Error('Loader metadata requested for an unsupported loader.');
  }

  loaderMetadataCache.set(cacheKey, { fetchedAt: Date.now(), value: versions });
  return versions;
}

function validateInstanceReference(instance) {
  if (!instance || typeof instance.id !== 'string' ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(instance.id)) {
    throw new Error('Save or select a launch instance first.');
  }
  if (typeof instance.gameVersion !== 'string' ||
      !/^\d+\.\d+(?:\.\d+)?$/.test(instance.gameVersion) ||
      !['vanilla', 'fabric', 'forge', 'optifine'].includes(instance.loaderType) ||
      typeof instance.loaderVersion !== 'string' ||
      (instance.legacy !== undefined && typeof instance.legacy !== 'boolean')) {
    throw new Error('Select a valid Minecraft version and loader for this instance.');
  }
}

function getInstanceDirectory(paths, instance) {
  validateInstanceReference(instance);
  if (instance.legacy === true) {
    const legacyId = [instance.gameVersion, instance.loaderType, instance.loaderVersion || 'base']
      .join('-')
      .replace(/[^A-Za-z0-9._-]/g, '_');
    return path.join(paths.instances, legacyId);
  }
  return path.join(paths.instances, instance.id);
}

async function searchModrinth(options) {
  const facets = [[`project_type:${MODRINTH_TYPES[options.type]}`], [`versions:${options.gameVersion}`]];
  if (options.type === 'mod' && ['fabric', 'forge'].includes(options.loaderType)) {
    facets.push([`categories:${options.loaderType}`]);
  }
  const params = new URLSearchParams({
    query: options.query,
    limit: '20',
    index: ['relevance', 'downloads', 'follows', 'newest', 'updated'].includes(options.sort)
      ? options.sort
      : 'relevance',
    facets: JSON.stringify(facets)
  });
  const response = await fetch(`${MODRINTH_API_URL}/search?${params}`, {
    headers: { 'User-Agent': 'Azure-Personal/1.0 (Minecraft launcher)' },
    signal: AbortSignal.timeout(20000)
  });
  if (!response.ok) throw new Error(`Modrinth search failed (HTTP ${response.status}).`);
  const result = await response.json();
  return result.hits.map((project) => ({
    id: project.project_id,
    provider: 'modrinth',
    type: options.type,
    title: project.title,
    description: project.description,
    icon: project.icon_url || '',
    downloads: project.downloads || 0,
    author: project.author || '',
    slug: project.slug,
    url: `https://modrinth.com/${MODRINTH_TYPES[options.type]}/${encodeURIComponent(project.slug)}`
  }));
}

async function searchExploreCatalog(event, options) {
  assertMainWindowEvent(event, 'Explore search');
  if (!options || !EXPLORE_TYPES.has(options.type) ||
      options.provider !== 'modrinth') {
    throw new Error('Choose a valid provider and content type.');
  }
  validateInstanceReference(options.instance);
  if (options.type === 'mod' && !['fabric', 'forge'].includes(options.instance.loaderType)) {
    throw new Error('Choose a Fabric or Forge instance to browse and install mods. Resource packs, shaders, and modpacks can use other instance types.');
  }
  if (options.gameVersion !== options.instance.gameVersion ||
      options.loaderType !== options.instance.loaderType) {
    throw new Error('Search filters must match the selected instance.');
  }
  const query = typeof options.query === 'string' ? options.query.trim().slice(0, 100) : '';
  const searchOptions = {
    ...options,
    query,
    sort: typeof options.sort === 'string' ? options.sort : 'relevance'
  };
  return searchModrinth(searchOptions);
}

async function ensureSafeInstanceDirectory(instanceDirectory, destinationDirectory) {
  const root = path.resolve(instanceDirectory);
  const destination = path.resolve(destinationDirectory);
  if (!isPathInside(root, destination)) {
    throw new Error('The install path is outside the selected instance.');
  }
  try {
    await fs.mkdir(root, { recursive: true });
  } catch (error) {
    throw new Error(`Could not create the instance folder: ${error.message}`);
  }
  const rootStats = await fs.lstat(root);
  if (!rootStats.isDirectory() || rootStats.isSymbolicLink()) {
    throw new Error('The selected instance folder is not a safe directory.');
  }
  const relative = path.relative(root, destination);
  let currentPath = root;
  for (const segment of relative.split(path.sep).filter(Boolean)) {
    currentPath = path.join(currentPath, segment);
    try {
      await fs.mkdir(currentPath);
    } catch (error) {
      if (error.code !== 'EEXIST') throw error;
    }
    const stats = await fs.lstat(currentPath);
    if (!stats.isDirectory() || stats.isSymbolicLink()) {
      throw new Error('The install path contains an unsafe directory or symbolic link.');
    }
  }
}

function getContentDirectory(instanceDirectory, type) {
  if (type === 'mod') return path.join(instanceDirectory, 'mods');
  if (type === 'resourcepack') return path.join(instanceDirectory, 'resourcepacks');
  if (type === 'shaderpack') return path.join(instanceDirectory, 'shaderpacks');
  return instanceDirectory;
}

async function downloadContentFile(url, expectedHashes = {}, signal) {
  let parsedUrl;
  try {
    parsedUrl = new URL(url);
  } catch {
    throw new Error('The provider returned an invalid download link.');
  }
  if (parsedUrl.protocol !== 'https:') throw new Error('Downloads must use a secure HTTPS link.');
  let response;
  let downloadUrl = parsedUrl;
  for (let redirects = 0; redirects <= 5; redirects += 1) {
    response = await fetch(downloadUrl, {
      headers: { 'User-Agent': 'Azure-Personal/1.0 (Minecraft launcher)' },
      redirect: 'manual',
      signal: createRequestSignal(120000, signal)
    });
    if (![301, 302, 303, 307, 308].includes(response.status)) break;
    const location = response.headers.get('location');
    if (!location || redirects === 5) throw new Error('The project download redirected too many times.');
    downloadUrl = new URL(location, downloadUrl);
    if (downloadUrl.protocol !== 'https:') throw new Error('Project download redirects must use HTTPS.');
  }
  if (!response) throw new Error('Could not retrieve the project download.');
  if (!response.ok) throw new Error(`Project download failed (HTTP ${response.status}).`);
  const declaredLength = Number(response.headers.get('content-length') || 0);
  if (declaredLength > 1024 * 1024 * 1024) throw new Error('The project file is larger than the 1 GB install limit.');
  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length === 0 || bytes.length > 1024 * 1024 * 1024) {
    throw new Error('The downloaded project file is empty or exceeds the 1 GB install limit.');
  }
  for (const [algorithm, expected] of Object.entries(expectedHashes)) {
    if (!['sha1', 'sha512'].includes(algorithm) || typeof expected !== 'string') continue;
    const actual = createHash(algorithm).update(bytes).digest('hex');
    if (actual.toLowerCase() !== expected.toLowerCase()) {
      throw new Error(`The downloaded file failed its ${algorithm} integrity check.`);
    }
  }
  return bytes;
}

function getManagedIndexPath(instanceDirectory) {
  return path.join(instanceDirectory, '.4vill-installed-content.json');
}

async function readManagedContent(instanceDirectory) {
  try {
    const rootStats = await fs.lstat(instanceDirectory);
    if (!rootStats.isDirectory() || rootStats.isSymbolicLink()) {
      throw new Error('The selected instance folder is not a safe directory.');
    }
    const indexStats = await fs.lstat(getManagedIndexPath(instanceDirectory));
    if (!indexStats.isFile() || indexStats.isSymbolicLink()) {
      throw new Error('Installed content index is not a regular file.');
    }
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
  try {
    const parsed = JSON.parse(await fs.readFile(getManagedIndexPath(instanceDirectory), 'utf8'));
    if (!Array.isArray(parsed) || parsed.some((item) =>
      !item || typeof item.key !== 'string' ||
      !['modrinth', 'curseforge'].includes(item.provider) ||
      !EXPLORE_TYPES.has(item.type) ||
      typeof item.title !== 'string' ||
      !Array.isArray(item.files) ||
      item.files.some((file) =>
        !file || typeof file.path !== 'string' ||
        typeof file.sha256 !== 'string' || !/^[a-f0-9]{64}$/i.test(file.sha256)
      )
    )) {
      throw new Error('Installed content index has an invalid format.');
    }
    return parsed;
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
}

async function writeManagedContent(instanceDirectory, items) {
  await ensureSafeInstanceDirectory(instanceDirectory, instanceDirectory);
  try {
    const stats = await fs.lstat(getManagedIndexPath(instanceDirectory));
    if (!stats.isFile() || stats.isSymbolicLink()) {
      throw new Error('Installed content index is not a regular file.');
    }
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  await fs.writeFile(getManagedIndexPath(instanceDirectory), JSON.stringify(items, null, 2));
}

async function getExploreProjectFile(options) {
  const params = new URLSearchParams({ game_versions: JSON.stringify([options.gameVersion]) });
  if (options.type === 'mod' && ['fabric', 'forge'].includes(options.loaderType)) {
    params.set('loaders', JSON.stringify([options.loaderType]));
  }
  const response = await fetch(
    `${MODRINTH_API_URL}/project/${encodeURIComponent(options.projectId)}/version?${params}`,
    { headers: { 'User-Agent': 'Azure-Personal/1.0 (Minecraft launcher)' }, signal: AbortSignal.timeout(20000) }
  );
  if (!response.ok) throw new Error(`Could not load compatible Modrinth versions (HTTP ${response.status}).`);
  const versions = await response.json();
  return versions[0] || null;
}

function getVersionFile(version) {
  if (!version || !Array.isArray(version.files)) return null;
  return version.files.find((file) => file.primary) || version.files[0] || null;
}

function getPackConfiguration(packManifest) {
  const dependencies = packManifest.dependencies || {};
  const gameVersion = dependencies.minecraft;
  if (typeof gameVersion !== 'string' || !/^\d+\.\d+(?:\.\d+)?$/.test(gameVersion)) {
    throw new Error('This Modrinth modpack does not specify a supported Minecraft release.');
  }
  if (dependencies['neoforge'] || dependencies['quilt-loader']) {
    throw new Error('NeoForge and Quilt modpacks are not supported by this launcher yet. Choose a Fabric, Forge, or vanilla pack.');
  }
  const loaders = [
    ['fabric-loader', 'fabric'],
    ['forge', 'forge']
  ];
  const loader = loaders.find(([key]) => typeof dependencies[key] === 'string');
  if (!loader) return { gameVersion, loaderType: 'vanilla', loaderVersion: '' };
  return { gameVersion, loaderType: loader[1], loaderVersion: dependencies[loader[0]] };
}

function validatePackArchive(zip) {
  const entries = zip.getEntries();
  if (entries.length > 10000) throw new Error('This modpack contains too many archive entries.');
  let uncompressedBytes = 0;
  for (const entry of entries) {
    if (entry.isDirectory) continue;
    const normalized = normalizeZipPath(entry.entryName);
    if (!normalized) throw new Error('This modpack contains an invalid file path.');
    resolveManagedPath(path.parse(process.cwd()).root, normalized);
    const attributes = Number(entry.header && entry.header.attr) || 0;
    if (((attributes >>> 16) & 0o170000) === 0o120000) {
      throw new Error('Symbolic links are not allowed in modpacks.');
    }
    const size = Number(entry.header && entry.header.size) || 0;
    if (size > 512 * 1024 * 1024) throw new Error('A modpack file exceeds the 512 MB extraction limit.');
    uncompressedBytes += size;
    if (uncompressedBytes > 2 * 1024 * 1024 * 1024) {
      throw new Error('This modpack exceeds the 2 GB extracted size limit.');
    }
  }
  return entries;
}

function normalizeZipPath(pathname) {
  if (pathname.includes('\\')) throw new Error('The downloaded project contains an unsafe archive path.');
  return pathname;
}

function parseMrpackFiles(zip) {
  validatePackArchive(zip);
  const indexEntry = zip.getEntry('modrinth.index.json');
  if (!indexEntry) throw new Error('This is not a valid Modrinth modpack.');
  const manifest = JSON.parse(indexEntry.getData().toString('utf8'));
  if (manifest.formatVersion !== 1 || !Array.isArray(manifest.files)) {
    throw new Error('This Modrinth modpack uses an unsupported format.');
  }
  const files = manifest.files.map((file) => {
    if (!file || file.env && file.env.client === 'unsupported') return null;
    const pathParts = typeof file.path === 'string' ? file.path.split('/') : [];
    const relativePath = pathParts.join('/');
    resolveManagedPath(path.parse(process.cwd()).root, relativePath);
    const downloads = Array.isArray(file.downloads) ? file.downloads : [];
    if (downloads.length === 0) {
      if (file.env && file.env.client === 'required') {
        throw new Error(`Required modpack file "${relativePath}" has no download URL.`);
      }
      return null;
    }
    return {
      path: relativePath,
      url: downloads[0],
      hashes: file.hashes || {},
      optional: Boolean(file.env && file.env.client === 'optional')
    };
  }).filter(Boolean);
  for (const entry of zip.getEntries()) {
    const normalized = normalizeZipPath(entry.entryName);
    if (normalized.startsWith('overrides/')) {
      const relativePath = normalized.slice('overrides/'.length);
      if (!entry.isDirectory) {
        resolveManagedPath(path.parse(process.cwd()).root, relativePath);
        files.push({ path: relativePath, zipEntry: entry, hashes: {} });
      }
    }
  }
  return { manifest, files };
}

async function prepareModpackFiles(version, stagingDirectory) {
  const packFile = getVersionFile(version);
  if (!packFile || !packFile.url) throw new Error('Modrinth did not provide a modpack download.');
  const packBytes = await downloadContentFile(packFile.url);
  const zip = new AdmZip(packBytes);
  const { manifest, files } = parseMrpackFiles(zip);
  if (files.length > 1000) throw new Error('This modpack has too many files to install (limit: 1,000).');
  const prepared = [];
  for (const [index, file] of files.entries()) {
    sendStatus(`Preparing modpack files: ${index + 1} of ${files.length}`);
    const stagedPath = resolveManagedPath(stagingDirectory, file.path);
    await ensureSafeInstanceDirectory(stagingDirectory, path.dirname(stagedPath));
    const bytes = file.zipEntry
      ? file.zipEntry.getData()
      : await downloadContentFile(file.url, file.hashes);
    await fs.writeFile(stagedPath, bytes);
    prepared.push({ path: file.path, stagedPath });
  }
  return { manifest, prepared };
}

async function installExploreProject(event, request) {
  assertMainWindowEvent(event, 'Explore install');
  if (!request || !EXPLORE_TYPES.has(request.type) ||
      request.provider !== 'modrinth' ||
      typeof request.projectId !== 'string' ||
      !/^[A-Za-z0-9_-]{1,80}$/.test(request.projectId)) {
    throw new Error('Choose a valid project to install.');
  }
  validateInstanceReference(request.instance);
  if (request.type === 'mod' && !['fabric', 'forge'].includes(request.instance.loaderType)) {
    throw new Error('Mods require a Fabric or Forge instance. Select a compatible loader before installing this project.');
  }
  if (request.gameVersion !== request.instance.gameVersion ||
      request.loaderType !== request.instance.loaderType) {
    throw new Error('Install target must match the selected instance.');
  }
  const paths = getDataPaths();
  const instanceDirectory = getInstanceDirectory(paths, request.instance);
  await ensureSafeInstanceDirectory(instanceDirectory, instanceDirectory);
  const managedItems = await readManagedContent(instanceDirectory);
  const contentKey = `${request.provider}:${request.type}:${request.projectId}`;
  const previousItem = managedItems.find((item) => item.key === contentKey);
  const version = await getExploreProjectFile({
    ...request,
    projectId: request.projectId,
    gameVersion: request.gameVersion,
    loaderType: request.loaderType
  });
  if (!version) {
    throw new Error(`No compatible version found for Minecraft ${request.gameVersion} and ${request.loaderType}.`);
  }

  const title = typeof request.title === 'string' ? request.title.slice(0, 120) : 'Minecraft content';
  const stagingDirectory = await fs.mkdtemp(path.join(instanceDirectory, '.4vill-install-'));
  try {
    let prepared;
    let targetDirectory;
    let updatedConfiguration = null;
    if (request.type === 'modpack') {
      prepared = await prepareModpackFiles(version, stagingDirectory);
      updatedConfiguration = getPackConfiguration(prepared.manifest);
      if (request.instance.legacy &&
          (updatedConfiguration.gameVersion !== request.instance.gameVersion ||
           updatedConfiguration.loaderType !== request.instance.loaderType ||
           updatedConfiguration.loaderVersion !== request.instance.loaderVersion)) {
        throw new Error('This older profile uses version-specific game folders. Save it as a new instance before installing a pack with a different Minecraft version or loader.');
      }
      targetDirectory = instanceDirectory;
    } else {
      const file = getVersionFile(version);
      if (!file || typeof file.url !== 'string') {
        throw new Error('The provider did not provide a downloadable project file.');
      }
      const fileName = path.basename(file.filename || file.url.split('?')[0]);
      if (!fileName || fileName === '.' || fileName === '..') throw new Error('The provider returned an invalid filename.');
      const stagedPath = path.join(stagingDirectory, fileName);
      sendStatus(`Downloading ${title}...`);
      await fs.writeFile(stagedPath, await downloadContentFile(file.url, file.hashes || {}));
      prepared = { manifest: null, prepared: [{ path: fileName, stagedPath }] };
      targetDirectory = getContentDirectory(instanceDirectory, request.type);
    }

    const managedPaths = new Set(
      (previousItem && Array.isArray(previousItem.files) ? previousItem.files : [])
        .map((item) => item.path)
    );
    const preparedPaths = new Set();
    for (const stagedFile of prepared.prepared) {
      if (preparedPaths.has(stagedFile.path)) {
        throw new Error(`The modpack contains duplicate paths for "${stagedFile.path}".`);
      }
      preparedPaths.add(stagedFile.path);
      const destination = resolveManagedPath(targetDirectory, stagedFile.path);
      await ensureSafeInstanceDirectory(instanceDirectory, path.dirname(destination));
      try {
        const stats = await fs.lstat(destination);
        if (!stats.isFile() || stats.isSymbolicLink()) {
          throw new Error(`Cannot install "${stagedFile.path}" because the destination is not a regular file.`);
        }
        if (!managedPaths.has(stagedFile.path)) {
          throw new Error(`Cannot install "${stagedFile.path}" because a file with that name already exists in this instance.`);
        }
      } catch (error) {
        if (error.code !== 'ENOENT') throw error;
      }
    }
    const installedFiles = [];
    for (const stagedFile of prepared.prepared) {
      const destination = resolveManagedPath(targetDirectory, stagedFile.path);
      await fs.copyFile(stagedFile.stagedPath, destination);
      const fileBytes = await fs.readFile(destination);
      installedFiles.push({
        path: stagedFile.path,
        sha256: createHash('sha256').update(fileBytes).digest('hex')
      });
    }
    const nextItems = managedItems.filter((item) => item.key !== contentKey);
    nextItems.push({
      key: contentKey,
      provider: request.provider,
      type: request.type,
      projectId: request.projectId,
      title,
      versionId: String(version.id || ''),
      files: installedFiles
    });
    await writeManagedContent(instanceDirectory, nextItems);
    if (previousItem && Array.isArray(previousItem.files)) {
      const nextPaths = new Set(installedFiles.map((file) => file.path));
      const previousDirectory = getContentDirectory(instanceDirectory, previousItem.type);
      for (const previousFile of previousItem.files) {
        if (nextPaths.has(previousFile.path)) continue;
        const oldPath = resolveManagedPath(previousDirectory, previousFile.path);
        try {
          await ensureSafeInstanceDirectory(instanceDirectory, path.dirname(oldPath));
          const stats = await fs.lstat(oldPath);
          if (!stats.isFile() || stats.isSymbolicLink()) continue;
          const bytes = await fs.readFile(oldPath);
          const hash = createHash('sha256').update(bytes).digest('hex');
          if (hash === previousFile.sha256) await fs.unlink(oldPath);
        } catch (error) {
          if (error.code !== 'ENOENT') throw error;
        }
      }
    }
    return {
      installed: true,
      title,
      version: version.name || version.version_number || version.displayName || '',
      installedFileCount: installedFiles.length,
      instance: updatedConfiguration
    };
  } finally {
    await fs.rm(stagingDirectory, { recursive: true, force: true });
  }
}

async function listInstalledContent(event, instance) {
  assertMainWindowEvent(event, 'Installed content');
  validateInstanceReference(instance);
  const instanceDirectory = getInstanceDirectory(getDataPaths(), instance);
  await ensureSafeInstanceDirectory(instanceDirectory, instanceDirectory);
  const managedItems = await readManagedContent(instanceDirectory);
  const knownFiles = new Set();
  for (const item of managedItems) {
    for (const file of item.files) {
      const normalizedPath = file.path.replace(/\//g, path.sep).toLowerCase();
      knownFiles.add(`${item.type}:${normalizedPath}`);
      if (item.type === 'modpack') {
        for (const folderType of ['mod', 'resourcepack', 'shaderpack']) {
          const folderName = `${getContentDirectory(instanceDirectory, folderType).split(path.sep).pop()}${path.sep}`;
          if (normalizedPath.startsWith(folderName.toLowerCase())) {
            knownFiles.add(`${folderType}:${normalizedPath.slice(folderName.length)}`);
          }
        }
      }
    }
  }

  const discoveredItems = [];
  for (const type of ['mod', 'resourcepack', 'shaderpack']) {
    const folder = getContentDirectory(instanceDirectory, type);
    try {
      const folderStats = await fs.lstat(folder);
      if (!folderStats.isDirectory() || folderStats.isSymbolicLink()) {
        throw new Error(`The instance ${type} folder is not a safe directory.`);
      }
    } catch (error) {
      if (error.code === 'ENOENT') continue;
      throw error;
    }

    const entries = await fs.readdir(folder, { withFileTypes: true });
    for (const entry of entries) {
      const isSupportedFile = type === 'mod'
        ? entry.isFile() && path.extname(entry.name).toLowerCase() === '.jar'
        : entry.isFile()
          ? ['.zip', '.jar'].includes(path.extname(entry.name).toLowerCase())
          : entry.isDirectory();
      if (!isSupportedFile) continue;
      const relativePath = entry.name;
      const fileKey = `${type}:${relativePath.toLowerCase()}`;
      if (knownFiles.has(fileKey)) continue;
      discoveredItems.push({
        key: '',
        provider: 'Local',
        type,
        title: entry.name,
        files: [{ path: relativePath }],
        managed: false
      });
    }
  }

  return [
    ...managedItems.map((item) => ({ ...item, managed: true })),
    ...discoveredItems
  ];
}

async function removeInstalledContent(event, instance, key) {
  assertMainWindowEvent(event, 'Remove installed content');
  validateInstanceReference(instance);
  if (typeof key !== 'string' || key.length > 250) throw new Error('Choose valid installed content to remove.');
  const instanceDirectory = getInstanceDirectory(getDataPaths(), instance);
  const items = await readManagedContent(instanceDirectory);
  const item = items.find((candidate) => candidate.key === key);
  if (!item) throw new Error('This content is not installed in the selected instance.');
  const targetDirectory = getContentDirectory(instanceDirectory, item.type);
  for (const file of item.files || []) {
    const target = resolveManagedPath(targetDirectory, file.path);
    try {
      await ensureSafeInstanceDirectory(instanceDirectory, path.dirname(target));
      const stats = await fs.lstat(target);
      if (!stats.isFile() || stats.isSymbolicLink()) continue;
      const bytes = await fs.readFile(target);
      const hash = createHash('sha256').update(bytes).digest('hex');
      if (hash === file.sha256) await fs.unlink(target);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }
  await writeManagedContent(instanceDirectory, items.filter((candidate) => candidate.key !== key));
  return { removed: true, title: item.title };
}

async function getCatalog(gameVersion = DEFAULT_GAME_VERSION) {
  const manifest = await getMinecraftManifest();
  const versions = manifest.versions.filter(isSupportedRelease).map(({ id }) => id);
  if (!versions.includes(manifest.latest.release)) versions.unshift(manifest.latest.release);
  if (!versions.includes(gameVersion)) gameVersion = manifest.latest.release;

  const fabricResult = await getLoaderChoices(gameVersion, 'fabric')
    .then((versions) => ({ status: 'fulfilled', value: versions }))
    .catch((error) => ({ status: 'rejected', reason: error }));

  const paths = getDataPaths();
  const optifineProfiles = (await readOptifineProfiles(paths))
    .filter((profile) => profile.gameVersion === gameVersion);
  return {
    versions,
    latest: manifest.latest.release,
    gameVersion,
    fabric: fabricResult.status === 'fulfilled' ? fabricResult.value : [],
    forge: [],
    loaderErrors: {
      fabric: fabricResult.status === 'rejected' ? fabricResult.reason.message : '',
      forge: ''
    },
    optifineProfiles
  };
}

function getVersionMetadata(manifest, gameVersion) {
  const entry = manifest.versions.find((version) => version.id === gameVersion && isSupportedRelease(version));
  if (!entry) throw new Error('Choose a stable Minecraft version from 1.7 or newer.');
  return entry;
}

async function getVersionDetails(gameVersion, signal) {
  const manifest = await getMinecraftManifest(signal);
  const entry = getVersionMetadata(manifest, gameVersion);
  const details = await fetchJson(entry.url, signal);
  return { entry, details };
}

async function getJavaExecutable() {
  const candidates = [];
  if (process.env.JAVA_HOME) {
    candidates.push(path.join(process.env.JAVA_HOME, 'bin', 'java.exe'));
  }

  if (process.platform === 'win32') {
    const javaInstallDirectories = [
      'C:\\Program Files\\Java',
      'C:\\Program Files\\Eclipse Adoptium',
      'C:\\Program Files\\Microsoft',
      'C:\\Program Files\\Zulu',
      'C:\\Program Files (x86)\\Java'
    ];
    for (const javaInstallDirectory of javaInstallDirectories) {
      try {
        const entries = await fs.readdir(javaInstallDirectory, { withFileTypes: true });
        for (const entry of entries) {
          if (entry.isDirectory() && /^(jdk|jre)[-_]?\d/i.test(entry.name)) {
            candidates.push(path.join(javaInstallDirectory, entry.name, 'bin', 'java.exe'));
          }
        }
      } catch (error) {
        if (error.code !== 'ENOENT') throw error;
      }
    }
  }
  candidates.push('java');
  return [...new Set(candidates)];
}

function inspectJavaVersion(javaPath) {
  return new Promise((resolve, reject) => {
    const child = spawn(javaPath, ['-version'], { windowsHide: true });
    let output = '';

    child.stdout.on('data', (chunk) => {
      output += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      output += chunk.toString();
    });
    child.once('error', reject);
    child.once('close', (code) => {
      if (code !== 0) {
        reject(new Error(`Could not run Java at "${javaPath}". Install Java 17 or newer and set JAVA_HOME to its installation folder.`));
        return;
      }

      const match = output.match(/version "([^"]+)"|openjdk\s+(\d+)/i);
      if (!match) {
        reject(new Error(`Could not determine the Java version from "${javaPath}". Install Java 17 or newer.`));
        return;
      }

      const version = match[1] || match[2];
      const majorVersion = Number(version.startsWith('1.')
        ? version.split('.')[1]
        : version.split('.')[0]);
      if (!Number.isInteger(majorVersion) || majorVersion < 1) {
        reject(new Error(`Could not parse Java version "${version}" at "${javaPath}".`));
        return;
      }

      resolve({ path: javaPath, version, majorVersion });
    });
  });
}

async function getJavaVersion(requiredMajor, exact = false) {
  const errors = [];
  const installations = [];
  for (const javaPath of await getJavaExecutable()) {
    try {
      const java = await inspectJavaVersion(javaPath);
      if (exact ? java.majorVersion === requiredMajor : java.majorVersion >= requiredMajor) {
        installations.push(java);
      }
    } catch (error) {
      errors.push(error.message);
    }
  }
  installations.sort((a, b) =>
    Math.abs(a.majorVersion - requiredMajor) - Math.abs(b.majorVersion - requiredMajor)
  );
  if (installations.length > 0) return installations[0];
  throw new Error(
    errors[0] ||
    (exact
      ? `Minecraft 1.7–1.16 requires Java ${requiredMajor}. Install a Java ${requiredMajor} runtime, or set JAVA_HOME to its installation folder.`
      : `Minecraft requires Java ${requiredMajor} or newer. Install a compatible Java runtime and set JAVA_HOME.`)
  );
}

async function getCompatiblePerformanceRelease(project, gameVersion, loaderType, signal) {
  const params = new URLSearchParams({
    game_versions: JSON.stringify([gameVersion]),
    loaders: JSON.stringify([loaderType])
  });
  const response = await fetch(
    `${MODRINTH_API_URL}/project/${project.projectId}/version?${params}`,
    { headers: { 'User-Agent': 'Azure-Personal/1.0 (Minecraft launcher)' }, signal: AbortSignal.timeout(20000) }
  );
  if (!response.ok) throw new Error(`Could not check ${project.title} compatibility (HTTP ${response.status}).`);
  const versions = await response.json();
  return versions.find((version) => version.version_type === 'release' && Array.isArray(version.files)) ||
    versions.find((version) => version.version_type === 'beta' && Array.isArray(version.files)) ||
    versions.find((version) => Array.isArray(version.files)) ||
    null;
}

async function readPerformanceModManifest(instanceDirectory) {
  const manifestPath = path.join(instanceDirectory, '.azure-performance-mods.json');
  try {
    const parsed = JSON.parse(await fs.readFile(manifestPath, 'utf8'));
    if (!Array.isArray(parsed) || parsed.some((item) =>
      !item || typeof item.filename !== 'string' || path.basename(item.filename) !== item.filename ||
      typeof item.sha256 !== 'string' || !/^[a-f0-9]{64}$/i.test(item.sha256)
    )) {
      throw new Error('Managed performance-mod index has an invalid format.');
    }
    return parsed;
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
}

async function getFileSha256(filePath) {
  try {
    return createHash('sha256').update(await fs.readFile(filePath)).digest('hex');
  } catch (error) {
    if (error.code === 'ENOENT') return '';
    throw error;
  }
}

async function installPerformanceMods(gameDirectory, gameVersion, loaderType, launch) {
  const projects = PERFORMANCE_MODS[loaderType];
  if (!projects) return;

  const modsDirectory = path.join(gameDirectory, 'mods');
  await ensureSafeInstanceDirectory(gameDirectory, modsDirectory);
  const previous = await readPerformanceModManifest(gameDirectory);
  const releases = [];
  const compatibleProjects = await Promise.all(projects.map(async (project) => {
    ensureLaunchActive(launch);
    const release = await getCompatiblePerformanceRelease(project, gameVersion, loaderType, launch.abortController.signal);
    if (!release) {
      return { project, release: null };
    }
    const file = release.files.find((candidate) => candidate.primary) || release.files[0];
    if (!file || typeof file.filename !== 'string' || path.basename(file.filename) !== file.filename ||
        !file.filename.toLowerCase().endsWith('.jar') || typeof file.url !== 'string') {
      throw new Error(`Modrinth returned invalid file metadata for ${project.title}.`);
    }
    return { project, release, file };
  }));
  for (const result of compatibleProjects) {
    if (!result.release) {
      sendStatus(`${result.project.title} has no ${loaderType} release for Minecraft ${gameVersion}; skipping it.`, 'warning');
      continue;
    }
    releases.push({ ...result.project, release: result.release, file: result.file });
  }

  for (let index = 0; index < releases.length; index += 1) {
    const release = releases[index];
    const requiredDependencies = (Array.isArray(release.release.dependencies) ? release.release.dependencies : [])
      .filter((dependency) => dependency.dependency_type === 'required');
    for (const dependency of requiredDependencies) {
      if (typeof dependency.project_id !== 'string' ||
          !/^[A-Za-z0-9_-]{1,80}$/.test(dependency.project_id)) {
        throw new Error(`Modrinth returned an invalid required dependency for ${release.title}.`);
      }
      let target = releases.find((candidate) => candidate.projectId === dependency.project_id);
      let requiredVersion;
      if (dependency.version_id) {
        const response = await fetch(
          `${MODRINTH_API_URL}/version/${encodeURIComponent(dependency.version_id)}`,
          { headers: { 'User-Agent': 'Azure-Personal/1.0 (Minecraft launcher)' }, signal: createRequestSignal(20000, launch.abortController.signal) }
        );
        if (!response.ok) throw new Error(`Could not resolve ${release.title}'s required mod version (HTTP ${response.status}).`);
        requiredVersion = await response.json();
      } else if (!target) {
        const response = await fetch(
          `${MODRINTH_API_URL}/project/${encodeURIComponent(dependency.project_id)}`,
          { headers: { 'User-Agent': 'Azure-Personal/1.0 (Minecraft launcher)' }, signal: createRequestSignal(20000, launch.abortController.signal) }
        );
        if (!response.ok) throw new Error(`Could not load a required dependency for ${release.title} (HTTP ${response.status}).`);
        const projectMetadata = await response.json();
        target = {
          projectId: dependency.project_id,
          title: typeof projectMetadata.title === 'string' ? projectMetadata.title : 'required dependency'
        };
        requiredVersion = await getCompatiblePerformanceRelease(
          target, gameVersion, loaderType, launch.abortController.signal
        );
        if (!requiredVersion) {
          throw new Error(`${release.title} requires ${target.title}, but no matching ${loaderType} build exists for Minecraft ${gameVersion}.`);
        }
      } else {
        continue;
      }
      if (!requiredVersion || !Array.isArray(requiredVersion.game_versions) ||
          !requiredVersion.game_versions.includes(gameVersion) ||
          !Array.isArray(requiredVersion.loaders) || !requiredVersion.loaders.includes(loaderType)) {
        throw new Error(`${release.title} requires a mod version that does not match Minecraft ${gameVersion} ${loaderType}.`);
      }
      const requiredFile = Array.isArray(requiredVersion.files)
        ? requiredVersion.files.find((candidate) => candidate.primary) || requiredVersion.files[0]
        : null;
      if (!requiredFile || typeof requiredFile.filename !== 'string' ||
          path.basename(requiredFile.filename) !== requiredFile.filename ||
          !requiredFile.filename.toLowerCase().endsWith('.jar') ||
          typeof requiredFile.url !== 'string') {
        throw new Error(`Modrinth did not provide a valid required dependency file for ${release.title}.`);
      }
      if (!target) {
        throw new Error(`${release.title} has a required dependency without project information.`);
      }
      if (target.requiredVersionId && target.requiredVersionId !== requiredVersion.id) {
        throw new Error(`Performance mods require conflicting versions of ${target.title} for Minecraft ${gameVersion}.`);
      }
      target.requiredVersionId = requiredVersion.id;
      target.release = requiredVersion;
      target.file = requiredFile;
      if (!releases.includes(target)) releases.push(target);
    }
  }

  const nextManifest = [];
  for (const release of releases) {
    const targetPath = path.join(modsDirectory, release.file.filename);
    const existingHash = await getFileSha256(targetPath);
    const previousEntry = previous.find((entry) => entry.filename === release.file.filename);
    if (existingHash && previousEntry && existingHash === previousEntry.sha256) {
      nextManifest.push(previousEntry);
      continue;
    }
    if (existingHash) {
      sendStatus(`${release.file.filename} already exists and is not an unchanged Azure-managed file; leaving it untouched.`, 'warning');
      continue;
    }

    sendStatus(`Downloading ${release.title} for Minecraft ${gameVersion}...`);
    const bytes = await downloadContentFile(release.file.url, release.file.hashes || {}, launch.abortController.signal);
    if (bytes.length < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) {
      throw new Error(`${release.title} download is not a valid JAR file.`);
    }
    const sha256 = createHash('sha256').update(bytes).digest('hex');
    await fs.writeFile(targetPath, bytes, { flag: 'wx' });
    nextManifest.push({ filename: release.file.filename, sha256, title: release.title });
  }

  for (const oldEntry of previous) {
    if (nextManifest.some((entry) => entry.filename === oldEntry.filename)) continue;
    const oldPath = path.join(modsDirectory, oldEntry.filename);
    if (await getFileSha256(oldPath) === oldEntry.sha256) await fs.rm(oldPath);
  }
  await fs.writeFile(
    path.join(gameDirectory, '.azure-performance-mods.json'),
    JSON.stringify(nextManifest, null, 2)
  );
  if (nextManifest.length) sendStatus(`Prepared ${nextManifest.length} compatible ${loaderType} performance mod(s).`);
}

async function installAzureClientMod(gameDirectory, gameVersion) {
  const filename = `azure-client-${gameVersion}-${AZURE_CLIENT_MOD_VERSION}.jar`;
  const modsDirectory = path.join(gameDirectory, 'mods');
  const manifestPath = path.join(gameDirectory, '.azure-client-mod.json');
  await ensureSafeInstanceDirectory(gameDirectory, modsDirectory);

  let managed = null;
  try {
    const parsed = JSON.parse(await fs.readFile(manifestPath, 'utf8'));
    if (parsed && typeof parsed.filename === 'string' &&
        /^azure-client-[0-9.]+-[0-9.]+\.jar$/.test(parsed.filename) &&
        typeof parsed.sha256 === 'string' && /^[a-f0-9]{64}$/i.test(parsed.sha256)) {
      managed = parsed;
    }
  } catch (error) {
    if (error.code !== 'ENOENT' && !(error instanceof SyntaxError)) throw error;
  }

  const removePreviousManagedMod = async () => {
    if (!managed) return true;
    const managedPath = path.join(modsDirectory, managed.filename);
    const actualHash = await getFileSha256(managedPath);
    if (!actualHash) return true;
    if (actualHash !== managed.sha256) {
      sendStatus(`${managed.filename} changed outside Azure; leaving it untouched.`, 'warning');
      return false;
    }
    await fs.unlink(managedPath);
    return true;
  };

  const sourcePath = path.join(
    app.getAppPath(),
    'client-mod',
    'build',
    'libs',
    filename
  );
  let bytes;
  try {
    bytes = await fs.readFile(sourcePath);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await removePreviousManagedMod();
    sendStatus(`Azure in-game features are not yet available for Fabric ${gameVersion}; launching without the client menu.`, 'warning');
    return;
  }
  if (bytes.length < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) {
    throw new Error('The bundled Azure client mod is not a valid JAR file.');
  }

  const targetPath = path.join(modsDirectory, filename);
  const expectedHash = createHash('sha256').update(bytes).digest('hex');
  if (managed && managed.filename !== filename && !(await removePreviousManagedMod())) return;
  const existingHash = await getFileSha256(targetPath);
  if (existingHash === expectedHash) return;

  const previousHash = managed && managed.filename === filename ? managed.sha256 : '';
  if (existingHash && existingHash !== previousHash) {
    sendStatus(`${filename} already exists and is not managed by Azure; leaving it untouched.`, 'warning');
    return;
  }

  const temporaryPath = path.join(modsDirectory, `${filename}.tmp`);
  await fs.writeFile(temporaryPath, bytes);
  await fs.rename(temporaryPath, targetPath);
  await fs.writeFile(manifestPath, JSON.stringify({ filename, sha256: expectedHash }, null, 2));
  sendStatus('Prepared Azure in-game client (Right Shift opens the menu).');
}

async function downloadJar(url, filePath, label, signal) {
  sendStatus(`Downloading ${label}...`);

  const response = await fetch(url, { signal: createRequestSignal(60000, signal) });
  if (!response.ok) {
    throw new Error(`${label} download failed (HTTP ${response.status}).`);
  }

  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) {
    throw new Error(`Downloaded ${label} is not a valid JAR file.`);
  }
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, bytes);
}

async function getFabricInstaller(root, launch) {
  const installers = await fetchJson(`${FABRIC_META_URL}/versions/installer`, launch.abortController.signal);
  const installer = installers.find((item) => item.stable) || installers[0];
  if (!installer || !installer.version || !installer.url) {
    throw new Error('No Fabric installer release is currently available.');
  }
  const installerPath = path.join(root, 'installers', `fabric-installer-${installer.version}.jar`);
  try {
    await fs.access(installerPath);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await downloadJar(installer.url, installerPath, 'Fabric installer', launch.abortController.signal);
  }
  return installerPath;
}

function runFabricInstaller(javaPath, installerPath, root, gameVersion, loaderVersion, launch) {
  return new Promise((resolve, reject) => {
    ensureLaunchActive(launch);
    const child = spawn(javaPath, [
      '-jar',
      installerPath,
      'client',
      '-dir',
      root,
      '-mcversion',
      gameVersion,
      '-loader',
      loaderVersion,
      '-noprofile'
    ], { windowsHide: true });
    launch.installerProcess = child;

    let output = '';
    child.stdout.on('data', (chunk) => {
      const text = chunk.toString();
      output += text;
      sendStatus(text.trim());
    });
    child.stderr.on('data', (chunk) => {
      const text = chunk.toString();
      output += text;
      sendStatus(text.trim(), 'warning');
    });
    child.once('error', reject);
    child.once('close', (code) => {
      launch.installerProcess = null;
      if (launch.cancelled) {
        reject(createLaunchCancelledError());
        return;
      }
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Fabric installation failed with exit code ${code}.${output ? ` ${output.trim()}` : ''}`));
      }
    });
  });
}

function getFabricVersionId(gameVersion, loaderVersion) {
  return `fabric-loader-${loaderVersion}-${gameVersion}`;
}

async function ensureFabricInstalled(paths, javaPath, gameVersion, loaderVersion, launch) {
  ensureLaunchActive(launch);
  const versionId = getFabricVersionId(gameVersion, loaderVersion);
  const versionJson = path.join(paths.root, 'versions', versionId, `${versionId}.json`);
  try {
    await fs.access(versionJson);
    return versionId;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }

  const installerPath = await getFabricInstaller(paths.root, launch);
  ensureLaunchActive(launch);
  sendStatus(`Installing Fabric ${loaderVersion} for Minecraft ${gameVersion}...`);
  await runFabricInstaller(javaPath, installerPath, paths.root, gameVersion, loaderVersion, launch);
  ensureLaunchActive(launch);
  await fs.access(versionJson);
  return versionId;
}

function validateAccountId(accountId) {
  if (typeof accountId !== 'string' ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(accountId)) {
    throw new Error('Choose a saved Microsoft account or sign in again.');
  }
}

async function getMicrosoftProfile(accountId) {
  validateAccountId(accountId);
  const paths = getDataPaths();
  await fs.mkdir(path.join(paths.authCache, accountId), { recursive: true });
  const authflow = new Authflow(
    `4vill-${accountId}`,
    path.join(paths.authCache, accountId),
    { authTitle: Titles.MinecraftJava, deviceType: 'Win32', flow: 'sisu' },
    (deviceCode) => {
      if (deviceCode && deviceCode.message) sendStatus(String(deviceCode.message));
    }
  );
  let session;
  try {
    session = await authflow.getMinecraftJavaToken({ fetchProfile: true });
  } catch (error) {
    const details = [];
    let cause = error;
    for (let depth = 0; cause && depth < 3; depth += 1) {
      if (cause.message && !details.includes(cause.message)) details.push(cause.message);
      if (cause.code !== undefined) details.push(`code ${cause.code}`);
      if (cause.statusCode !== undefined) details.push(`HTTP ${cause.statusCode}`);
      cause = cause.cause;
    }
    throw new Error(`Microsoft authentication failed: ${details.join(' · ') || String(error)}`);
  }
  if (!session || !session.token || !session.profile || !session.profile.id || !session.profile.name) {
    throw new Error('Microsoft sign-in did not return a Minecraft Java profile with game ownership.');
  }
  return session;
}

function skinPreviewForProfile(profile) {
  const uuid = typeof profile.id === 'string' ? profile.id.replace(/-/g, '') : '';
  if (!/^[a-f0-9]{32}$/i.test(uuid)) throw new Error('Minecraft returned an invalid player UUID.');
  return {
    name: profile.name,
    uuid,
    skinUrl: `https://crafatar.com/renders/body/${uuid}?overlay`
  };
}

async function getAuthenticatedMinecraftProfile(accountId) {
  const session = await getMicrosoftProfile(accountId);
  const response = await fetch('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${session.token}` },
    redirect: 'error',
    signal: AbortSignal.timeout(20000)
  });
  if (!response.ok) {
    throw new Error(`Could not load the signed-in Minecraft profile (HTTP ${response.status}).`);
  }
  const profile = await response.json();
  return { session, profile, preview: skinPreviewForProfile(profile) };
}

function validateSkinImage(skin) {
  if (skin.length > 1024 * 1024 || skin.length < 24 ||
      skin.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') {
    throw new Error('Choose a valid PNG skin file smaller than 1 MB.');
  }
  const width = skin.readUInt32BE(16);
  const height = skin.readUInt32BE(20);
  if (width !== 64 || ![32, 64].includes(height)) {
    throw new Error('Minecraft skins must be PNG images sized 64×64 or 64×32 pixels.');
  }
}

function getLocalSkinPath(username) {
  if (typeof username !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(username)) {
    throw new Error('Enter a valid local player nickname before choosing a skin.');
  }
  return path.join(getDataPaths().localSkins, `${username.toLowerCase()}.png`);
}

async function getSkinPreview(event, request) {
  assertMainWindowEvent(event, 'Player skin');
  if (!request || !['local', 'microsoft'].includes(request.authMode)) {
    throw new Error('Choose a valid player profile.');
  }
  if (request.authMode === 'microsoft') {
    validateAccountId(request.accountId);
    return (await getAuthenticatedMinecraftProfile(request.accountId)).preview;
  }
  if (typeof request.username !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(request.username)) {
    return { name: request.username || 'Local profile', uuid: '', skinUrl: '' };
  }
  try {
    const skin = await fs.readFile(getLocalSkinPath(request.username));
    return {
      name: request.username,
      uuid: '',
      skinUrl: `data:image/png;base64,${skin.toString('base64')}`,
      local: true
    };
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  const response = await fetch(
    `https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(request.username)}`,
    { signal: AbortSignal.timeout(15000) }
  );
  if (response.status === 204 || response.status === 404) {
    return { name: request.username, uuid: '', skinUrl: '' };
  }
  if (!response.ok) throw new Error(`Could not look up the Minecraft skin (HTTP ${response.status}).`);
  const profile = await response.json();
  return skinPreviewForProfile(profile);
}

async function chooseLocalSkin(event, username) {
  assertMainWindowEvent(event, 'Local skin');
  const skinPath = getLocalSkinPath(username);
  const result = await dialog.showOpenDialog(mainWindow, {
    title: `Choose a skin PNG for ${username}`,
    properties: ['openFile'],
    filters: [{ name: 'Minecraft skin (PNG)', extensions: ['png'] }]
  });
  if (result.canceled || result.filePaths.length === 0) return null;
  const skin = await fs.readFile(result.filePaths[0]);
  validateSkinImage(skin);
  await fs.mkdir(path.dirname(skinPath), { recursive: true });
  await fs.writeFile(skinPath, skin);
  return {
    name: username,
    uuid: '',
    skinUrl: `data:image/png;base64,${skin.toString('base64')}`,
    local: true
  };
}

async function uploadMinecraftSkin(event, accountId, variant) {
  assertMainWindowEvent(event, 'Skin upload');
  validateAccountId(accountId);
  if (!['classic', 'slim'].includes(variant)) throw new Error('Choose the skin model.');
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Choose a Minecraft skin PNG',
    properties: ['openFile'],
    filters: [{ name: 'Minecraft skin (PNG)', extensions: ['png'] }]
  });
  if (result.canceled || result.filePaths.length === 0) return null;
  const skinPath = result.filePaths[0];
  const skin = await fs.readFile(skinPath);
  validateSkinImage(skin);

  const { session } = await getAuthenticatedMinecraftProfile(accountId);
  const form = new FormData();
  form.append('variant', variant);
  form.append('file', new Blob([skin], { type: 'image/png' }), path.basename(skinPath));
  const response = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', {
    method: 'POST',
    headers: { Authorization: `Bearer ${session.token}` },
    body: form,
    redirect: 'error',
    signal: AbortSignal.timeout(30000)
  });
  if (!response.ok) {
    throw new Error(`Minecraft rejected the skin upload (HTTP ${response.status}).`);
  }
  const profile = await response.json();
  return skinPreviewForProfile(profile);
}

function assertMainWindowEvent(event, action) {
  if (!mainWindow || event.sender !== mainWindow.webContents) {
    throw new Error(`${action} request came from an unexpected window.`);
  }
}

ipcMain.handle('catalog:get', async (event, gameVersion) => {
  assertMainWindowEvent(event, 'Catalog');
  return getCatalog(typeof gameVersion === 'string' ? gameVersion : DEFAULT_GAME_VERSION);
});

ipcMain.handle('explore:search', (event, options) => searchExploreCatalog(event, options));

ipcMain.handle('explore:install', (event, request) => installExploreProject(event, request));

ipcMain.handle('explore:installed', (event, instance) => listInstalledContent(event, instance));

ipcMain.handle('explore:remove', (event, instance, key) => removeInstalledContent(event, instance, key));

ipcMain.handle('explore:open-project', async (event, provider, slug) => {
  assertMainWindowEvent(event, 'Project page');
  if (provider !== 'modrinth' ||
      typeof slug !== 'string' ||
      !/^[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+){1,3}$/.test(slug)) {
    throw new Error('Invalid project page.');
  }
  await shell.openExternal(`https://modrinth.com/${slug}`);
});

ipcMain.handle('instance:open-mods', async (event, instance, folderType) => {
  assertMainWindowEvent(event, 'Mods folder');
  if (['instance', 'root', 'mods', 'resourcepacks', 'shaderpacks'].includes(folderType)) {
    if (!instance || typeof instance.id !== 'string' ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(instance.id)) {
      throw new Error('Select or save a launch instance before opening its mods folder.');
    }
    if (instance.legacy === true &&
        (typeof instance.gameVersion !== 'string' ||
         typeof instance.loaderType !== 'string' ||
         !['vanilla', 'fabric', 'forge', 'optifine'].includes(instance.loaderType))) {
      throw new Error('Invalid legacy instance folder details.');
    }
  } else if (folderType !== 'shared') {
    throw new Error('Choose a valid mods folder.');
  }

  const paths = getDataPaths();
  const legacyId = instance && instance.legacy
    ? [instance.gameVersion, instance.loaderType, instance.loaderVersion || 'base']
      .join('-')
      .replace(/[^A-Za-z0-9._-]/g, '_')
    : '';
  const folderName = folderType === 'instance' ? 'mods' : folderType === 'root' ? '' : folderType;
  const folderPath = folderType === 'shared'
    ? paths.modsSource
    : path.join(paths.instances, instance && instance.legacy ? legacyId : instance.id, folderName);
  await fs.mkdir(folderPath, { recursive: true });
  const error = await shell.openPath(folderPath);
  if (error) throw new Error(`Could not open the mods folder: ${error}`);
});

ipcMain.handle('catalog:loader-versions', async (event, gameVersion, loader) => {
  assertMainWindowEvent(event, 'Loader catalog');
  if (typeof gameVersion !== 'string' || !['fabric', 'forge'].includes(loader)) {
    throw new Error('Choose a valid game version and loader.');
  }
  const manifest = await getMinecraftManifest();
  getVersionMetadata(manifest, gameVersion);
  return getLoaderChoices(gameVersion, loader);
});

ipcMain.handle('optifine:import', (event) => importOptifineProfile(event));

ipcMain.handle('account:microsoft-login', async (event) => {
  assertMainWindowEvent(event, 'Microsoft sign-in');
  const accountId = randomUUID();
  sendStatus('Authenticating Microsoft account...');
  const session = await getMicrosoftProfile(accountId);
  return {
    id: accountId,
    name: session.profile.name,
    uuid: session.profile.id
  };
});

ipcMain.handle('skin:get', (event, request) => getSkinPreview(event, request));
ipcMain.handle('skin:local:choose', (event, username) => chooseLocalSkin(event, username));
ipcMain.handle('skin:upload', (event, accountId, variant) =>
  uploadMinecraftSkin(event, accountId, variant));

async function createAuthorization(profile) {
  if (profile.authMode === 'local') {
    return {
      access_token: '0',
      client_token: randomUUID(),
      uuid: randomUUID().replace(/-/g, ''),
      name: profile.username,
      user_properties: '{}'
    };
  }

  sendStatus('Authenticating with Microsoft. Follow the device sign-in instructions...');
  validateAccountId(profile.accountId);
  const session = await getMicrosoftProfile(profile.accountId);
  return {
    access_token: session.token,
    client_token: randomUUID(),
    uuid: session.profile.id,
    name: session.profile.name,
    user_properties: '{}',
    meta: { type: 'msa' }
  };
}

function validateLaunchOptions(options) {
  if (!options || typeof options !== 'object') {
    throw new Error('Invalid launcher options.');
  }
  if (options.authMode !== 'local' && options.authMode !== 'microsoft') {
    throw new Error('Choose a valid account type.');
  }
  if (typeof options.gameVersion !== 'string' || !/^\d+\.\d+(?:\.\d+)?$/.test(options.gameVersion)) {
    throw new Error('Choose a Minecraft release version.');
  }
  if (!['vanilla', 'fabric', 'forge', 'optifine'].includes(options.loaderType)) {
    throw new Error('Choose a supported game loader.');
  }
  if (typeof options.instanceId !== 'string' ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(options.instanceId)) {
    throw new Error('Save or select a launch instance before starting Minecraft.');
  }
  if (options.legacyInstance !== undefined && typeof options.legacyInstance !== 'boolean') {
    throw new Error('Invalid legacy instance setting.');
  }
  if (options.loaderType !== 'vanilla' && typeof options.loaderVersion !== 'string') {
    throw new Error('Choose a loader version.');
  }
  if (!Number.isInteger(options.ram) || options.ram < 1 || options.ram > 4) {
    throw new Error('RAM allocation must be between 1 and 4 GB.');
  }
  if (options.authMode === 'local' &&
      (typeof options.username !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(options.username))) {
    throw new Error('Local profile names must be 3-16 letters, numbers, or underscores.');
  }
  if (options.authMode === 'microsoft') validateAccountId(options.accountId);
}

async function ensureForgeJar(paths, gameVersion, forgeVersion, launch) {
  const usesUniversalJar = compareVersionIds(gameVersion, '1.12.2') < 0 ||
    (gameVersion === '1.12.2' && forgeVersion.endsWith('.2847'));
  const artifactType = usesUniversalJar ? 'universal' : 'installer';
  const fileName = `forge-${gameVersion}-${forgeVersion}-${artifactType}.jar`;
  const forgeJarPath = path.join(paths.root, 'installers', fileName);
  try {
    await fs.access(forgeJarPath);
    return forgeJarPath;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  const url = `${FORGE_MAVEN_URL}/${gameVersion}-${forgeVersion}/${fileName}`;
  await downloadJar(url, forgeJarPath, `Forge ${artifactType} library`, launch.abortController.signal);
  return forgeJarPath;
}

async function readOptifineProfiles(paths) {
  try {
    const content = await fs.readFile(paths.optifineProfilesFile, 'utf8');
    const profiles = JSON.parse(content);
    if (!Array.isArray(profiles)) throw new Error('OptiFine profile index has an invalid format.');
    return profiles;
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
}

async function importOptifineProfile(event) {
  if (!mainWindow || event.sender !== mainWindow.webContents) {
    throw new Error('Import request came from an unexpected window.');
  }
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Select an installed OptiFine profile JSON',
    properties: ['openFile'],
    filters: [{ name: 'Minecraft version profile', extensions: ['json'] }]
  });
  if (result.canceled || result.filePaths.length === 0) return null;

  const sourceJsonPath = result.filePaths[0];
  const sourceJson = JSON.parse(await fs.readFile(sourceJsonPath, 'utf8'));
  const gameVersion = sourceJson.inheritsFrom;
  if (typeof sourceJson.id !== 'string' ||
      typeof gameVersion !== 'string' ||
      !Array.isArray(sourceJson.libraries) ||
      !sourceJson.libraries.some((library) => /optifine/i.test(library.name || ''))) {
    throw new Error('Select an installed OptiFine profile JSON from your Minecraft versions folder.');
  }

  const { details } = await getVersionDetails(gameVersion);
  const sourceJarPath = path.join(path.dirname(sourceJsonPath), `${sourceJson.id}.jar`);
  await fs.access(sourceJarPath);
  const paths = getDataPaths();
  const profileId = `optifine-${randomUUID()}`;
  const destinationDirectory = path.join(paths.root, 'versions', profileId);
  await fs.mkdir(destinationDirectory, { recursive: true });

  const importedJson = { ...sourceJson, id: profileId, inheritsFrom: gameVersion };
  await fs.writeFile(path.join(destinationDirectory, `${profileId}.json`), JSON.stringify(importedJson, null, 2));
  await fs.writeFile(path.join(destinationDirectory, `${gameVersion}.json`), JSON.stringify(details, null, 2));
  await fs.copyFile(sourceJarPath, path.join(destinationDirectory, `${profileId}.jar`));
  const sourceGameRoot = path.resolve(path.dirname(sourceJsonPath), '..', '..');
  for (const library of importedJson.libraries) {
    const libraryPath = getLibraryArtifactPath(library);
    if (!libraryPath) continue;
    const sourceLibrary = path.join(sourceGameRoot, 'libraries', libraryPath);
    const destinationLibrary = path.join(paths.root, 'libraries', libraryPath);
    try {
      await fs.mkdir(path.dirname(destinationLibrary), { recursive: true });
      await fs.copyFile(sourceLibrary, destinationLibrary);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }

  const profiles = await readOptifineProfiles(paths);
  const profile = {
    id: profileId,
    name: sourceJson.id,
    gameVersion
  };
  profiles.push(profile);
  await fs.writeFile(paths.optifineProfilesFile, JSON.stringify(profiles, null, 2));
  return profile;
}

function getLibraryArtifactPath(library) {
  const artifactPath = library.downloads && library.downloads.artifact && library.downloads.artifact.path;
  if (artifactPath) {
    if (typeof artifactPath !== 'string' ||
        artifactPath.includes('\\') ||
        artifactPath.includes(':') ||
        artifactPath.split('/').some((part) => !part || part === '.' || part === '..')) {
      return '';
    }
    return path.join(...artifactPath.split('/'));
  }
  if (typeof library.name !== 'string') return '';
  const [group, artifact, version, classifier] = library.name.split(':');
  const safeParts = [group, artifact, version, classifier].filter(Boolean);
  if (!group || !artifact || !version ||
      safeParts.some((part) => part === '.' || part === '..' || !/^[A-Za-z0-9_.+-]+$/.test(part)) ||
      group.split('.').some((part) => !part || part === '.' || part === '..')) return '';
  const fileName = `${artifact}-${version}${classifier ? `-${classifier}` : ''}.jar`;
  return path.join(...group.split('.'), artifact, version, fileName);
}

async function getLaunchConfiguration(options, paths, launch) {
  ensureLaunchActive(launch);
  const { entry, details } = await getVersionDetails(options.gameVersion, launch.abortController.signal);
  const javaMajor = details.javaVersion && details.javaVersion.majorVersion
    ? details.javaVersion.majorVersion
    : (compareVersionIds(options.gameVersion, '1.18') >= 0 ? 17 : 8);
  const legacyJava = compareVersionIds(options.gameVersion, '1.17') < 0;
  const java = await getJavaVersion(javaMajor, legacyJava);
  ensureLaunchActive(launch);
  sendStatus(`Using Java ${java.version} for Minecraft ${options.gameVersion}.`);

  const legacyId = [options.gameVersion, options.loaderType, options.loaderVersion || 'base']
    .join('-')
    .replace(/[^A-Za-z0-9._-]/g, '_');
  const gameDirectory = path.join(
    paths.instances,
    options.legacyInstance ? legacyId : options.instanceId
  );
  let version = { number: options.gameVersion, type: entry.type };
  let forge;

  if (options.loaderType === 'fabric') {
    const loaders = await getLoaderChoices(options.gameVersion, 'fabric', launch.abortController.signal);
    if (!loaders.includes(options.loaderVersion)) throw new Error('That Fabric Loader version is not available for the selected Minecraft version.');
    const custom = await ensureFabricInstalled(paths, java.path, options.gameVersion, options.loaderVersion, launch);
    version = { number: options.gameVersion, custom, type: entry.type };
  } else if (options.loaderType === 'forge') {
    const loaders = await getLoaderChoices(options.gameVersion, 'forge', launch.abortController.signal);
    if (!loaders.includes(options.loaderVersion)) throw new Error('That Forge version is not available for the selected Minecraft version.');
    forge = await ensureForgeJar(paths, options.gameVersion, options.loaderVersion, launch);
  } else if (options.loaderType === 'optifine') {
    const profiles = await readOptifineProfiles(paths);
    const selectedProfile = profiles.find((profile) =>
      profile.id === options.loaderVersion && profile.gameVersion === options.gameVersion
    );
    if (!selectedProfile) throw new Error('Import an installed OptiFine profile for this Minecraft version first.');
    version = { number: options.gameVersion, custom: selectedProfile.id, type: entry.type };
  }

  ensureLaunchActive(launch);
  await installPerformanceMods(gameDirectory, options.gameVersion, options.loaderType, launch);
  ensureLaunchActive(launch);
  if (options.loaderType === 'fabric') {
    await installAzureClientMod(gameDirectory, options.gameVersion);
  }
  return { java, version, forge, gameDirectory };
}

function connectLauncherEvents(launcher) {
  const launch = activeLaunch;
  let failureMessage;
  let recentOutput = '';

  launcher.on('progress', (event) => {
    if (!event || typeof event !== 'object') return;
    if (Number.isFinite(event.task) && Number.isFinite(event.total) && event.total > 0) {
      const percent = Math.min(100, Math.round((event.task / event.total) * 100));
      const task = event.type === 'natives' ? 'Preparing game libraries' : 'Downloading client assets';
      sendStatus(`${task}: ${percent}%`);
    } else {
      sendStatus('Preparing Minecraft files...');
    }
  });

  launcher.on('download-status', (event) => {
    if (!event || typeof event !== 'object') return;
    if (Number.isFinite(event.current) && Number.isFinite(event.total) && event.total > 0) {
      const percent = Math.min(100, Math.round((event.current / event.total) * 100));
      const item = typeof event.name === 'string' ? path.basename(event.name) : 'game files';
      sendStatus(`Downloading ${item}: ${percent}%`);
    } else if (typeof event.name === 'string') {
      sendStatus(`Downloading ${path.basename(event.name)}...`);
    }
  });

  launcher.on('debug', (message) => {
    const text = String(message);
    const failurePrefix = '[MCLC]: Failed to start due to ';
    if (text.includes(failurePrefix)) {
      failureMessage = text.slice(text.indexOf(failurePrefix) + failurePrefix.length).replace(/, closing\.\.\.$/, '');
      sendStatus(`Minecraft launch failed: ${failureMessage}`, 'error');
    } else if (/Attempting to download Minecraft version jar|Attempting to download assets|Using Java version/.test(text)) {
      sendStatus(text.replace(/^\[MCLC\]:\s*/, ''));
    }
  });

  launcher.on('data', (data) => {
    const text = Buffer.isBuffer(data) ? data.toString() : String(data);
    if (!text.trim()) return;
    recentOutput = `${recentOutput}${text}`.slice(-12000);
    if (!gameProcessStarted && /(Setting user:|LWJGL Version:|Backend library:|OpenAL initialized)/i.test(text)) {
      gameProcessStarted = true;
      sendStatus('Game Running!', 'success');
      quitTimer = setTimeout(() => app.quit(), 5000);
    }
  });

  launcher.on('close', (code) => {
    if (launch.cancelled) {
      sendStatus('Launch cancelled. Minecraft was stopped.', 'cancelled');
      launchInProgress = false;
      activeLaunch = null;
      return;
    }
    if (!gameProcessStarted) {
      const detail = recentOutput.trim().split(/\r?\n/).filter(Boolean).slice(-12).join(' | ');
      sendStatus(
        `Game process exited before startup completed (code ${code}).${detail ? ` Last output: ${detail}` : ' No game output was received.'}`,
        'error'
      );
    }
    launchInProgress = false;
    activeLaunch = null;
  });

  launcher.on('error', (error) => {
    sendStatus(`Launcher error: ${error.message}`, 'error');
    launchInProgress = false;
    activeLaunch = null;
  });

  return () => failureMessage;
}

ipcMain.handle('launcher:start', async (event, options) => {
  assertMainWindowEvent(event, 'Launcher');
  if (launchInProgress) {
    throw new Error('A launch is already in progress.');
  }
  validateLaunchOptions(options);
  launchInProgress = true;
  gameProcessStarted = false;
  const launch = {
    cancelled: false,
    abortController: new AbortController(),
    requests: new Set(),
    downloadFiles: new Set(),
    installerProcess: null,
    minecraftProcess: null
  };
  activeLaunch = launch;

  try {
    const paths = getDataPaths();
    await fs.mkdir(paths.root, { recursive: true });
    sendStatus('Preparing launcher environment...');
    const configuration = await getLaunchConfiguration(options, paths, launch);
    ensureLaunchActive(launch);
    const authorization = await createAuthorization(options);
    ensureLaunchActive(launch);

    sendStatus('Starting Minecraft...');
    const launcher = new Client();
    const getLaunchFailure = connectLauncherEvents(launcher);
    const launchPromise = launcher.launch({
      authorization,
      root: paths.root,
      javaPath: configuration.java.path,
      version: configuration.version,
      forge: configuration.forge,
      overrides: { gameDirectory: configuration.gameDirectory },
      memory: { min: '1G', max: `${options.ram}G` }
    });
    const handler = launcher.handler;
    if (handler) {
      const originalBaseRequest = handler.baseRequest;
      handler.baseRequest = (...args) => {
        const request = originalBaseRequest(...args);
        launch.requests.add(request);
        request.once('close', () => launch.requests.delete(request));
        if (launch.cancelled) request.abort();
        return request;
      };
      handler.downloadAsync = async (url, directory, name, retry, type) => {
        const destination = path.join(directory, name);
        for (let attempt = 0; attempt <= Number(Boolean(retry)); attempt += 1) {
          if (launch.cancelled) return false;
          await fs.mkdir(directory, { recursive: true });
          const request = handler.baseRequest(url);
          const output = fsSync.createWriteStream(destination);
          launch.downloadFiles.add(output);
          let receivedBytes = 0;
          let totalBytes = 0;
          let failureReason = '';
          request.once('response', (response) => {
            totalBytes = Number(response.headers['content-length']) || 0;
            if (response.statusCode === 404) {
              failureReason = `File not found: ${url}`;
              request.abort();
            }
          });
          request.on('data', (chunk) => {
            receivedBytes += chunk.length;
            launcher.emit('download-status', {
              name,
              type,
              current: receivedBytes,
              total: totalBytes
            });
          });
          try {
            await pipeline(request, output, { signal: launch.abortController.signal });
            launch.downloadFiles.delete(output);
            if (launch.cancelled) {
              await fs.rm(destination, { force: true });
              return false;
            }
            launcher.emit('download', name);
            return { failed: false, asset: null };
          } catch (error) {
            launch.downloadFiles.delete(output);
            await fs.rm(destination, { force: true });
            if (launch.cancelled) return false;
            if (attempt < Number(Boolean(retry))) continue;
            const reason = failureReason || error.message;
            launcher.emit('debug', `[MCLC]: Failed to download asset to ${destination} due to ${reason}.`);
            return false;
          }
        }
        return false;
      };
    }
    const startMinecraft = launcher.startMinecraft.bind(launcher);
    launcher.startMinecraft = (args) => {
      if (launch.cancelled) return null;
      const process = startMinecraft(args);
      launch.minecraftProcess = process;
      return process;
    };
    const process = await launchPromise;
    ensureLaunchActive(launch);
    if (!process) {
      throw new Error(getLaunchFailure() || 'Minecraft could not start. Check your Java installation and internet connection.');
    }
    return { accepted: true };
  } catch (error) {
    launchInProgress = false;
    activeLaunch = null;
    if (launch.cancelled || error.code === 'LAUNCH_CANCELLED') {
      sendStatus('Launch cancelled. No further game downloads will be started.', 'cancelled');
      throw createLaunchCancelledError();
    }
    sendStatus(error.message || 'Unable to start Minecraft.', 'error');
    throw error;
  }
});

ipcMain.handle('launcher:stop', (event) => {
  assertMainWindowEvent(event, 'Stop launch');
  const launch = activeLaunch;
  if (!launch || !launchInProgress) return { stopped: false };

  launch.cancelled = true;
  launch.abortController.abort();
  for (const request of launch.requests) request.abort();
  for (const output of launch.downloadFiles) output.destroy();
  if (launch.installerProcess && !launch.installerProcess.killed) launch.installerProcess.kill();
  if (launch.minecraftProcess && !launch.minecraftProcess.killed) launch.minecraftProcess.kill();
  sendStatus('Stopping launch and canceling downloads…', 'busy');
  return { stopped: true };
});

app.whenReady().then(() => {
  app.setName('Azure Personal');
  createWindow();
  setTimeout(configureAutoUpdates, 3500);
});

function configureAutoUpdates() {
  if (!app.isPackaged) return;
  const updateConfigPath = path.join(process.resourcesPath, 'app-update.yml');
  if (!fsSync.existsSync(updateConfigPath)) {
    sendStatus('Automatic updates are not configured yet; install future setup releases manually.', 'warning');
    return;
  }

  autoUpdater.autoDownload = false;
  autoUpdater.autoInstallOnAppQuit = false;
  autoUpdater.on('checking-for-update', () => {
    sendStatus('Checking for launcher updates...');
  });
  autoUpdater.on('update-available', async (info) => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    try {
      const result = await dialog.showMessageBox(mainWindow, {
        type: 'info',
        title: 'Azure Personal update available',
        message: `Version ${info.version} is ready to download.`,
        detail: 'Your Minecraft instances and saved profiles will remain untouched.',
        buttons: ['Download update', 'Later'],
        defaultId: 0,
        cancelId: 1
      });
      if (result.response === 0) await autoUpdater.downloadUpdate();
      else sendStatus(`Version ${info.version} is available when you are ready.`, 'info');
    } catch (error) {
      console.error('[Azure Personal] Could not start update download:', error);
      sendStatus(`Could not start update download: ${error.message}`, 'error');
    }
  });
  autoUpdater.on('download-progress', (progress) => {
    sendStatus(`Downloading launcher update: ${Math.round(progress.percent)}%`, 'busy');
  });
  autoUpdater.on('update-downloaded', async (info) => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    try {
      const result = await dialog.showMessageBox(mainWindow, {
        type: 'info',
        title: 'Update ready to install',
        message: `Azure Personal ${info.version} has downloaded.`,
        detail: 'Restart the launcher to finish installing the update.',
        buttons: ['Restart and install', 'Later'],
        defaultId: 0,
        cancelId: 1
      });
      if (result.response === 0) autoUpdater.quitAndInstall(false, true);
      else sendStatus('Update downloaded; restart Azure Personal later to install it.', 'success');
    } catch (error) {
      console.error('[Azure Personal] Could not install downloaded update:', error);
      sendStatus(`Update downloaded, but could not start installation: ${error.message}`, 'error');
    }
  });
  autoUpdater.on('error', (error) => {
    console.error('[Azure Personal] Update check failed:', error);
    sendStatus(`Update check failed: ${error.message}`, 'warning');
  });
  autoUpdater.checkForUpdates().catch((error) => {
    console.error('[Azure Personal] Could not check for updates:', error);
    sendStatus(`Could not check for updates: ${error.message}`, 'warning');
  });
}

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', () => {
  if (quitTimer) clearTimeout(quitTimer);
});
