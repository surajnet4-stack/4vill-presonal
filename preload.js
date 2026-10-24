const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('launcher', {
  start: (options) => ipcRenderer.invoke('launcher:start', options),
  getCatalog: (gameVersion) => ipcRenderer.invoke('catalog:get', gameVersion),
  getLoaderVersions: (gameVersion, loader) => ipcRenderer.invoke('catalog:loader-versions', gameVersion, loader),
  searchExplore: (options) => ipcRenderer.invoke('explore:search', options),
  installExploreProject: (request) => ipcRenderer.invoke('explore:install', request),
  getInstalledContent: (instance) => ipcRenderer.invoke('explore:installed', instance),
  removeInstalledContent: (instance, key) => ipcRenderer.invoke('explore:remove', instance, key),
  openExploreProject: (provider, slug) => ipcRenderer.invoke('explore:open-project', provider, slug),
  openModsFolder: (instance, folderType) => ipcRenderer.invoke('instance:open-mods', instance, folderType),
  addMicrosoftAccount: () => ipcRenderer.invoke('account:microsoft-login'),
  getSkin: (request) => ipcRenderer.invoke('skin:get', request),
  chooseLocalSkin: (username) => ipcRenderer.invoke('skin:local:choose', username),
  uploadSkin: (accountId, variant) => ipcRenderer.invoke('skin:upload', accountId, variant),
  stop: () => ipcRenderer.invoke('launcher:stop'),
  importOptifineProfile: () => ipcRenderer.invoke('optifine:import'),
  minimize: () => ipcRenderer.invoke('window:minimize'),
  close: () => ipcRenderer.invoke('window:close'),
  onStatus: (callback) => {
    const listener = (_event, status) => callback(status);
    ipcRenderer.on('launcher:status', listener);
    return () => ipcRenderer.removeListener('launcher:status', listener);
  }
});
