const owner = process.env.AZURE_GITHUB_OWNER;
const repo = process.env.AZURE_GITHUB_REPO;

module.exports = {
  appId: 'dev.azurepersonal.launcher',
  productName: 'Azure Personal',
  directories: {
    output: 'dist/installer'
  },
  files: [
    'main.js',
    'preload.js',
    'index.html',
    'path-safety.js',
    'assets/**/*',
    'bin/**/*',
    'client-mod/build/libs/azure-client-*-1.0.0.jar',
    'package.json'
  ],
  asar: true,
  win: {
    target: ['nsis'],
    icon: 'assets/azure-icon-installer.ico',
    artifactName: 'Azure-Personal-Setup-${version}.${ext}'
  },
  nsis: {
    oneClick: false,
    perMachine: false,
    allowToChangeInstallationDirectory: true,
    createDesktopShortcut: true,
    createStartMenuShortcut: true,
    shortcutName: 'Azure Personal',
    runAfterFinish: true
  },
  ...(owner && repo ? {
    publish: [{
      provider: 'github',
      owner,
      repo,
      releaseType: 'release'
    }]
  } : {})
};
