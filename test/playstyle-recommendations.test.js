const test = require('node:test');
const assert = require('node:assert/strict');
const { recommendPlaystyle } = require('../playstyle-recommendations');

test('finds modpacks for broad playstyle prompts', () => {
  assert.deepEqual(recommendPlaystyle('I want a cozy multiplayer progression modpack'), {
    type: 'modpack',
    query: 'cozy multiplayer progression',
    gameVersion: ''
  });
});

test('does not classify a general adventure modpack as a world datapack', () => {
  assert.equal(
    recommendPlaystyle('I want a cozy exploration adventure modpack').type,
    'modpack'
  );
});

test('finds mods for explicit performance requests', () => {
  const recommendation = recommendPlaystyle('Suggest performance mods for better FPS');
  assert.equal(recommendation.type, 'mod');
  assert.equal(recommendation.query, 'performance better fps');
});

test('finds world datapacks for map and world requests', () => {
  const recommendation = recommendPlaystyle('I want new biomes and structures in my world');
  assert.equal(recommendation.type, 'datapack');
  assert.equal(recommendation.query, 'new biomes structures');
});

test('limits generated search terms and rejects an empty prompt', () => {
  assert.equal(
    recommendPlaystyle('cozy magic farming building survival multiplayer exploration').query,
    'cozy magic farming building survival multiplayer'
  );
  assert.throws(() => recommendPlaystyle('  '), /Describe the Minecraft experience/);
});
