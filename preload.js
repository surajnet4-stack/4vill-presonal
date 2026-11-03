const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('launcher', {
  start: (options) => ipcRenderer.invoke('launcher:start', options),
  getCatalog: (gameVersion) => ipcRenderer.invoke('catalog:get', gameVersion),
  getLoaderVersions: (gameVersion, loader) => ipcRenderer.invoke('catalog:loader-versions', gameVersion, loader),
  searchExplore: (options) => ipcRenderer.invoke('explore:search', options),
  installExploreProject: (request) => ipcRenderer.invoke('explore:install', request),
  importModpack: () => ipcRenderer.invoke('explore:import-modpack'),
  getInstalledContent: (instance) => ipcRenderer.invoke('explore:installed', instance),
  removeInstalledContent: (instance, key) => ipcRenderer.invoke('explore:remove', instance, key),
  getInstanceMods: (instance) => ipcRenderer.invoke('instance:list-mods', instance),
  setInstanceModEnabled: (instance, fileName, enabled) =>
    ipcRenderer.invoke('instance:toggle-mod', instance, fileName, enabled),
  getCrownCosmetic: (instance) => ipcRenderer.invoke('cosmetic:crown:get', instance),
  setCrownCosmetic: (instance, enabled) => ipcRenderer.invoke('cosmetic:crown:set', instance, enabled),
  openExploreProject: (provider, slug) => ipcRenderer.invoke('explore:open-project', provider, slug),
  openModsFolder: (instance, folderType) => ipcRenderer.invoke('instance:open-mods', instance, folderType),
  addMicrosoftAccount: () => ipcRenderer.invoke('account:microsoft-login'),
  getSkin: (request) => ipcRenderer.invoke('skin:get', request),
  chooseLocalSkin: (username) => ipcRenderer.invoke('skin:local:choose', username),
  uploadSkin: (accountId, variant) => ipcRenderer.invoke('skin:upload', accountId, variant),
  stop: () => ipcRenderer.invoke('launcher:stop'),
  getUpdateStatus: () => ipcRenderer.invoke('updater:status'),
  checkForUpdates: () => ipcRenderer.invoke('updater:check'),
  downloadUpdate: () => ipcRenderer.invoke('updater:download'),
  installUpdate: () => ipcRenderer.invoke('updater:install'),
  getDiscordSettings: () => ipcRenderer.invoke('discord:settings:get'),
  saveDiscordSettings: (settings) => ipcRenderer.invoke('discord:settings:set', settings),
  getPersonalization: () => ipcRenderer.invoke('personalization:get'),
  savePersonalization: (settings) => ipcRenderer.invoke('personalization:set', settings),
  choosePersonalizationBackground: () => ipcRenderer.invoke('personalization:background:choose'),
  clearPersonalizationBackground: () => ipcRenderer.invoke('personalization:background:clear'),
  choosePersonalizationMusic: () => ipcRenderer.invoke('personalization:music:choose'),
  clearPersonalizationMusic: () => ipcRenderer.invoke('personalization:music:clear'),
  importOptifineProfile: () => ipcRenderer.invoke('optifine:import'),
  minimize: () => ipcRenderer.invoke('window:minimize'),
  close: () => ipcRenderer.invoke('window:close'),
  onStatus: (callback) => {
    const listener = (_event, status) => callback(status);
    ipcRenderer.on('launcher:status', listener);
    return () => ipcRenderer.removeListener('launcher:status', listener);
  },
  onUpdateState: (callback) => {
    const listener = (_event, state) => callback(state);
    ipcRenderer.on('updater:state', listener);
    return () => ipcRenderer.removeListener('updater:state', listener);
  },
  onDiscordState: (callback) => {
    const listener = (_event, state) => callback(state);
    ipcRenderer.on('discord:state', listener);
    return () => ipcRenderer.removeListener('discord:state', listener);
  }
});
