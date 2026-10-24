const path = require('node:path');

function isPathInside(root, destination) {
  const relative = path.relative(path.resolve(root), path.resolve(destination));
  return relative === '' ||
    (relative !== '..' && !relative.startsWith(`..${path.sep}`) && !path.isAbsolute(relative));
}

function resolveManagedPath(root, relativePath) {
  if (typeof relativePath !== 'string' || !relativePath || relativePath.includes('\0') ||
      relativePath.includes('\\') || path.posix.isAbsolute(relativePath) ||
      relativePath.split('/').some((part) => !part || part === '.' || part === '..' || part.includes(':'))) {
    throw new Error('The downloaded project contains an unsafe file path.');
  }
  const base = path.resolve(root);
  const destination = path.resolve(base, ...relativePath.split('/'));
  if (destination === base || !isPathInside(base, destination)) {
    throw new Error('The downloaded project contains a file outside its instance.');
  }
  return destination;
}

module.exports = { isPathInside, resolveManagedPath };
