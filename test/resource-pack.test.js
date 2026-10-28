const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');
const { createOptionsBackgroundPng, ensureAzureResourcePack } = require('../azure-resource-pack');

test('creates a valid PNG texture for the Azure resource pack', () => {
  const png = createOptionsBackgroundPng();
  assert.equal(png.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
  assert.equal(png.readUInt32BE(16), 64);
  assert.equal(png.readUInt32BE(20), 64);
});

test('installs and selects the Azure resource pack without losing other options', async (context) => {
  const gameDirectory = await fs.mkdtemp(path.join(os.tmpdir(), 'azure-resource-pack-'));
  context.after(() => fs.rm(gameDirectory, { recursive: true, force: true }));
  await fs.writeFile(path.join(gameDirectory, 'options.txt'), 'soundVolume:1.0\nresourcePacks:["vanilla","file/Existing Pack"]\n');

  const result = await ensureAzureResourcePack(gameDirectory);
  const metadata = JSON.parse(await fs.readFile(path.join(result.directory, 'pack.mcmeta'), 'utf8'));
  const texture = await fs.readFile(path.join(
    result.directory,
    'assets',
    'minecraft',
    'textures',
    'gui',
    'options_background.png'
  ));
  const options = await fs.readFile(path.join(gameDirectory, 'options.txt'), 'utf8');

  assert.equal(metadata.pack.pack_format, 15);
  assert.equal(texture.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
  assert.match(options, /soundVolume:1\.0/);
  assert.match(options, /file\/4vill Azure UI/);
  assert.match(options, /file\/Existing Pack/);

  await fs.writeFile(
    path.join(gameDirectory, 'options.txt'),
    options.replace(/^resourcePacks:.*$/m, 'resourcePacks:["vanilla","file/Existing Pack"]')
  );
  await ensureAzureResourcePack(gameDirectory);
  const updatedOptions = await fs.readFile(path.join(gameDirectory, 'options.txt'), 'utf8');
  assert.doesNotMatch(updatedOptions, /file\/4vill Azure UI/);
});
