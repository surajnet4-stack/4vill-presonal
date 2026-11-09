(function initializePlaystyleRecommendations(root) {
  const stopWords = new Set([
    'a', 'an', 'and', 'are', 'be', 'for', 'from', 'fun', 'game', 'i', 'im', 'in',
    'is', 'it', 'like', 'looking', 'me', 'minecraft', 'my', 'of', 'on', 'or', 'play',
    'playing', 'please', 'some', 'suggest', 'that', 'the', 'to', 'want', 'with', 'would'
  ]);
  const categoryWords = new Set([
    'datapack', 'datapacks', 'map', 'maps', 'mod', 'mods', 'modpack', 'modpacks',
    'pack', 'packs', 'world', 'worlds'
  ]);
  const categoryPatterns = [
    { type: 'datapack', pattern: /\b(world|worlds|map|maps|skyblock|terrain|biome|structure|structures|dungeon|dungeons)\b/i },
    { type: 'modpack', pattern: /\b(modpack|modpacks|pack|packs|collection|progression|kitchen sink|questing)\b/i },
    { type: 'mod', pattern: /\b(mod|mods|performance|fps|optimization|optimisation|shader|shaders|minimap|backpack|farming|magic)\b/i }
  ];

  function recommendPlaystyle(prompt) {
    if (typeof prompt !== 'string' || !prompt.trim()) {
      throw new Error('Describe the Minecraft experience you want first.');
    }

    const normalized = prompt.trim().replace(/\s+/g, ' ').slice(0, 180);
    const match = categoryPatterns.find((item) => item.pattern.test(normalized));
    const type = match ? match.type : 'modpack';
    const queryTerms = normalized.toLowerCase()
      .split(/[^a-z0-9+.-]+/)
      .filter((term) => term.length > 1 && !stopWords.has(term) && !categoryWords.has(term))
      .slice(0, 6);
    const defaults = {
      mod: 'performance survival',
      modpack: 'adventure survival',
      datapack: 'adventure structures'
    };

    return {
      type,
      query: (queryTerms.join(' ') || defaults[type]).slice(0, 100),
      gameVersion: ''
    };
  }

  const api = { recommendPlaystyle };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.PlaystyleRecommendations = api;
})(globalThis);
