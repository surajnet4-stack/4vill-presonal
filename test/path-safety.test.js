const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const { isPathInside, resolveManagedPath } = require('../path-safety');

test('allows archive override paths under a Windows drive root', () => {
  const driveRoot = path.parse(process.cwd()).root;
  assert.equal(
    resolveManagedPath(driveRoot, 'overrides/config/options.txt'),
    path.join(driveRoot, 'overrides', 'config', 'options.txt')
  );
});

test('rejects archive paths that escape the chosen root', () => {
  const root = path.join(process.cwd(), 'test-instance');
  assert.throws(() => resolveManagedPath(root, '../outside.txt'), /unsafe file path/);
  assert.throws(() => resolveManagedPath(root, 'mods/../../outside.txt'), /unsafe file path/);
  assert.equal(isPathInside(root, path.join(root, 'mods', 'example.jar')), true);
  assert.equal(isPathInside(root, path.resolve(root, '..', 'outside.txt')), false);
});

test('rejects absolute, separator-confused, and empty archive paths', () => {
  const root = path.parse(process.cwd()).root;
  assert.throws(() => resolveManagedPath(root, ''), /unsafe file path/);
  assert.throws(() => resolveManagedPath(root, '\\outside.txt'), /unsafe file path/);
  assert.throws(() => resolveManagedPath(root, 'C:/outside.txt'), /unsafe file path/);
});
