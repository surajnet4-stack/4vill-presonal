const fs = require('node:fs/promises');
const path = require('node:path');
const zlib = require('node:zlib');

const RESOURCE_PACK_NAME = '4vill Azure UI';
const RESOURCE_PACK_FORMAT = 15;
const CRC_TABLE = new Uint32Array(256);

for (let index = 0; index < CRC_TABLE.length; index += 1) {
  let value = index;
  for (let bit = 0; bit < 8; bit += 1) {
    value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
  }
  CRC_TABLE[index] = value >>> 0;
}

function crc32(buffer) {
  let value = 0xffffffff;
  for (const byte of buffer) value = CRC_TABLE[(value ^ byte) & 0xff] ^ (value >>> 8);
  return (value ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
  const typeBuffer = Buffer.from(type, 'ascii');
  const chunk = Buffer.alloc(12 + data.length);
  chunk.writeUInt32BE(data.length, 0);
  typeBuffer.copy(chunk, 4);
  data.copy(chunk, 8);
  chunk.writeUInt32BE(crc32(Buffer.concat([typeBuffer, data])), 8 + data.length);
  return chunk;
}

function createOptionsBackgroundPng() {
  const size = 64;
  const scanlines = Buffer.alloc(size * (size * 4 + 1));
  for (let y = 0; y < size; y += 1) {
    const rowOffset = y * (size * 4 + 1);
    scanlines[rowOffset] = 0;
    for (let x = 0; x < size; x += 1) {
      const horizontal = (1 + Math.cos((x / size) * Math.PI * 2)) / 2;
      const vertical = (1 + Math.cos((y / size) * Math.PI * 2)) / 2;
      const grid = x % 16 === 0 || y % 16 === 0 ? 5 : 0;
      const glow = Math.max(0, 1 - Math.hypot((x - 48) / 32, (y - 16) / 36)) * 10;
      const offset = rowOffset + 1 + x * 4;
      scanlines[offset] = Math.round(14 + horizontal * 3 + glow + grid);
      scanlines[offset + 1] = Math.round(17 + vertical * 4 + glow + grid);
      scanlines[offset + 2] = Math.round(27 + horizontal * 8 + vertical * 8 + glow * 2 + grid);
      scanlines[offset + 3] = 255;
    }
  }

  const header = Buffer.alloc(13);
  header.writeUInt32BE(size, 0);
  header.writeUInt32BE(size, 4);
  header[8] = 8;
  header[9] = 6;
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    pngChunk('IHDR', header),
    pngChunk('IDAT', zlib.deflateSync(scanlines)),
    pngChunk('IEND', Buffer.alloc(0))
  ]);
}

async function ensureAzureResourcePack(gameDirectory) {
  const resourcePacksDirectory = path.join(gameDirectory, 'resourcepacks');
  await fs.mkdir(resourcePacksDirectory, { recursive: true });
  const resourcePacksStats = await fs.lstat(resourcePacksDirectory);
  if (!resourcePacksStats.isDirectory() || resourcePacksStats.isSymbolicLink()) {
    throw new Error('The instance resource packs folder is not a safe directory.');
  }
  const packDirectory = path.join(resourcePacksDirectory, RESOURCE_PACK_NAME);
  try {
    const packStats = await fs.lstat(packDirectory);
    if (!packStats.isDirectory() || packStats.isSymbolicLink()) {
      throw new Error('The Azure resource pack path is not a safe directory.');
    }
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  const textureDirectory = path.join(packDirectory, 'assets', 'minecraft', 'textures', 'gui');
  await fs.mkdir(textureDirectory, { recursive: true });

  const metadataPath = path.join(packDirectory, 'pack.mcmeta');
  try {
    await fs.access(metadataPath);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await fs.writeFile(metadataPath, JSON.stringify({
      pack: {
        pack_format: RESOURCE_PACK_FORMAT,
        description: 'A dark, softly accented Azure Personal menu background.'
      }
    }, null, 2));
  }

  const texturePath = path.join(textureDirectory, 'options_background.png');
  try {
    await fs.access(texturePath);
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await fs.writeFile(texturePath, createOptionsBackgroundPng());
  }

  const optionsPath = path.join(gameDirectory, 'options.txt');
  const initializedPath = path.join(gameDirectory, '.4vill-resource-pack-initialized');
  let optionsText = '';
  let previouslyInitialized = false;
  try {
    optionsText = await fs.readFile(optionsPath, 'utf8');
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  try {
    await fs.access(initializedPath);
    previouslyInitialized = true;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  const resourcePacksLine = optionsText.match(/^resourcePacks:(.*)$/m);
  let selectedPacks = ['vanilla'];
  if (resourcePacksLine) {
    try {
      selectedPacks = JSON.parse(resourcePacksLine[1]);
    } catch (error) {
      throw new Error(`Could not read the selected Minecraft resource packs: ${error.message}`);
    }
    if (!Array.isArray(selectedPacks) || selectedPacks.some((pack) => typeof pack !== 'string')) {
      throw new Error('Minecraft resource pack settings have an invalid format.');
    }
  }
  const packId = `file/${RESOURCE_PACK_NAME}`;
  if (!selectedPacks.includes(packId) && !previouslyInitialized) selectedPacks.push(packId);
  if (!selectedPacks.includes('vanilla')) selectedPacks.unshift('vanilla');
  const nextLine = `resourcePacks:${JSON.stringify(selectedPacks)}`;
  const nextText = resourcePacksLine
    ? optionsText.replace(/^resourcePacks:.*$/m, nextLine)
    : `${optionsText}${optionsText && !optionsText.endsWith('\n') ? '\n' : ''}${nextLine}\n`;
  await fs.writeFile(optionsPath, nextText);
  if (!previouslyInitialized) await fs.writeFile(initializedPath, 'installed\n');
  return { name: RESOURCE_PACK_NAME, directory: packDirectory };
}

module.exports = { createOptionsBackgroundPng, ensureAzureResourcePack };
