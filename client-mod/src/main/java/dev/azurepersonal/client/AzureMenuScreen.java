package dev.azurepersonal.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AzureMenuScreen extends Screen {
    public enum Category {
        ALL("ALL", "Browse every module"),
        HUD("HUD", "On-screen widgets"),
        PVP("PVP", "Combat utilities"),
        VISUALS("VISUAL", "Rendering and lighting"),
        MECHANICS("MECH", "Movement and gameplay"),
        THEMES("THEME", "Accent colors and opacity"),
        PRESETS("PRESET", "Quick loadouts"),
        COSMETICS("COSMETIC", "Free Azure cosmetics"),
        CONTROLS("KEYS", "Key bindings");

        public final String title;
        public final String subtitle;

        Category(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public static final class ModEntry {
        public final String id;
        public final String name;
        public final String description;
        public final Category category;

        public ModEntry(String id, String name, String description, Category category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
        }

        public boolean isEnabled() {
            AzureConfig cfg = AzureClient.config();
            return switch (id) {
                case "watermark" -> cfg.showWatermark;
                case "fps" -> cfg.showFps;
                case "keycaps" -> cfg.showKeycaps;
                case "cps" -> cfg.showCps;
                case "ping" -> cfg.showPing;
                case "armor" -> cfg.showArmor;
                case "potions" -> cfg.showPotions;
                case "coords" -> cfg.showCoordinates;
                case "direction" -> cfg.showDirection;
                case "speed" -> cfg.showSpeedometer;
                case "combo" -> cfg.showCombo;
                case "reach" -> cfg.showReach;
                case "clock" -> cfg.showClock;
                case "memory" -> cfg.showMemory;
                case "health" -> cfg.showHealth;
                case "hunger" -> cfg.showHunger;
                case "server" -> cfg.showServerInfo;
                case "worldtime" -> cfg.showWorldTime;
                case "day" -> cfg.showDayCounter;
                case "fpsgraph" -> cfg.showFpsGraph;
                case "sprint" -> AzureClient.isSprintToggled();
                case "sneak" -> AzureClient.isSneakToggled();
                case "autosprint" -> cfg.autoSprint;
                case "crosshair" -> cfg.customCrosshair;
                case "fullbright" -> cfg.fullBright;
                case "outline" -> cfg.blockOutline;
                case "blockoverlay" -> cfg.showBlockOverlay;
                case "lowfire" -> cfg.lowFire;
                case "nohurtcam" -> cfg.noHurtCam;
                case "chroma" -> cfg.chromaHud;
                case "freelook" -> AzureClient.isFreeLookActive();
                default -> false;
            };
        }

        public void toggle() {
            AzureConfig cfg = AzureClient.config();
            switch (id) {
                case "watermark" -> cfg.showWatermark = !cfg.showWatermark;
                case "fps" -> cfg.showFps = !cfg.showFps;
                case "keycaps" -> cfg.showKeycaps = !cfg.showKeycaps;
                case "cps" -> cfg.showCps = !cfg.showCps;
                case "ping" -> cfg.showPing = !cfg.showPing;
                case "armor" -> cfg.showArmor = !cfg.showArmor;
                case "potions" -> cfg.showPotions = !cfg.showPotions;
                case "coords" -> cfg.showCoordinates = !cfg.showCoordinates;
                case "direction" -> cfg.showDirection = !cfg.showDirection;
                case "speed" -> cfg.showSpeedometer = !cfg.showSpeedometer;
                case "combo" -> cfg.showCombo = !cfg.showCombo;
                case "reach" -> cfg.showReach = !cfg.showReach;
                case "clock" -> cfg.showClock = !cfg.showClock;
                case "memory" -> cfg.showMemory = !cfg.showMemory;
                case "health" -> cfg.showHealth = !cfg.showHealth;
                case "hunger" -> cfg.showHunger = !cfg.showHunger;
                case "server" -> cfg.showServerInfo = !cfg.showServerInfo;
                case "worldtime" -> cfg.showWorldTime = !cfg.showWorldTime;
                case "day" -> cfg.showDayCounter = !cfg.showDayCounter;
                case "fpsgraph" -> cfg.showFpsGraph = !cfg.showFpsGraph;
                case "sprint" -> AzureClient.setSprintToggled(!AzureClient.isSprintToggled());
                case "sneak" -> AzureClient.setSneakToggled(!AzureClient.isSneakToggled());
                case "autosprint" -> AzureClient.setAutoSprint(!cfg.autoSprint);
                case "crosshair" -> cfg.customCrosshair = !cfg.customCrosshair;
                case "fullbright" -> cfg.fullBright = !cfg.fullBright;
                case "outline" -> cfg.blockOutline = !cfg.blockOutline;
                case "blockoverlay" -> cfg.showBlockOverlay = !cfg.showBlockOverlay;
                case "lowfire" -> cfg.lowFire = !cfg.lowFire;
                case "nohurtcam" -> cfg.noHurtCam = !cfg.noHurtCam;
                case "chroma" -> cfg.chromaHud = !cfg.chromaHud;
                case "freelook" -> AzureClient.toggleFreeLook(net.minecraft.client.MinecraftClient.getInstance());
                default -> {
                }
            }
            cfg.hudPreset = "custom";
            cfg.save();
        }
    }

    private static final int COLOR_TEXT = 0xfff0f4fa;
    private static final int COLOR_MUTED = 0xff8798ad;
    private static final int COLOR_SUBTLE = 0xff5d7088;
    private static final int COLOR_CARD = 0xff111925;
    private static final int COLOR_CARD_HOVER = 0xff182334;
    private static final int COLOR_LINE = 0xff26384c;
    private static final List<ModEntry> MODS = new ArrayList<>();

    static {
        // HUD
        MODS.add(new ModEntry("watermark", "Watermark", "Branded badge with FPS and ping", Category.HUD));
        MODS.add(new ModEntry("fps", "FPS Counter", "Live frame rate display", Category.HUD));
        MODS.add(new ModEntry("keycaps", "Keystrokes", "WASD, mouse, and space keys", Category.HUD));
        MODS.add(new ModEntry("cps", "CPS Counter", "Left and right clicks per second", Category.HUD));
        MODS.add(new ModEntry("ping", "Ping Display", "Server latency with color alerts", Category.HUD));
        MODS.add(new ModEntry("armor", "Armor Status", "Equipped armor and durability", Category.HUD));
        MODS.add(new ModEntry("potions", "Potion Effects", "Active effect timers", Category.HUD));
        MODS.add(new ModEntry("coords", "Coordinates", "XYZ coords, biome, nether calc", Category.HUD));
        MODS.add(new ModEntry("direction", "Direction", "Facing compass and yaw angle", Category.HUD));
        MODS.add(new ModEntry("speed", "Speedometer", "Horizontal movement speed", Category.HUD));
        MODS.add(new ModEntry("clock", "Clock", "Real-world time", Category.HUD));
        MODS.add(new ModEntry("memory", "Memory / RAM", "JVM heap usage display", Category.HUD));
        MODS.add(new ModEntry("blockoverlay", "Block Info", "Target block name and position", Category.HUD));
        MODS.add(new ModEntry("fpsgraph", "FPS Graph", "Animated frame rate graph", Category.HUD));

        // New utility modules
        MODS.add(new ModEntry("health", "Health Display", "Exact health and absorption", Category.HUD));
        MODS.add(new ModEntry("hunger", "Hunger & Saturation", "Food level and saturation", Category.HUD));
        MODS.add(new ModEntry("server", "Server Info", "Current server address", Category.HUD));
        MODS.add(new ModEntry("worldtime", "World Time", "In-game clock", Category.HUD));
        MODS.add(new ModEntry("day", "Day Counter", "Current world day", Category.HUD));

        // PVP
        MODS.add(new ModEntry("combo", "Combo Counter", "Consecutive hit tracker", Category.PVP));
        MODS.add(new ModEntry("reach", "Reach Display", "Attack distance in blocks", Category.PVP));
        MODS.add(new ModEntry("lowfire", "Low Fire", "Lower screen fire", Category.PVP));
        MODS.add(new ModEntry("nohurtcam", "No Hurt Cam", "Removes camera shake", Category.PVP));

        // VISUALS
        MODS.add(new ModEntry("crosshair", "Custom Crosshair", "Cross, dot, circle and hit color", Category.VISUALS));
        MODS.add(new ModEntry("fullbright", "Fullbright", "Bright caves and night", Category.VISUALS));
        MODS.add(new ModEntry("outline", "Block Outline", "Custom block highlight", Category.VISUALS));
        MODS.add(new ModEntry("chroma", "Chroma HUD", "Animated RGB rainbow", Category.VISUALS));
        MODS.add(new ModEntry("freelook", "Free Look (V)", "360-degree third person", Category.VISUALS));

        // MECHANICS
        MODS.add(new ModEntry("sprint", "Toggle Sprint (K)", "Sticky sprint with badge", Category.MECHANICS));
        MODS.add(new ModEntry("sneak", "Toggle Sneak (J)", "Sticky sneak with badge", Category.MECHANICS));
        MODS.add(new ModEntry("autosprint", "Auto Sprint", "Sprint while moving forward", Category.MECHANICS));
    }

    private Category selectedCategory = Category.ALL;
    private String searchQuery = "";
    private boolean searchFocused;
    private int scrollOffset;
    private boolean draggingMenuOpacity;
    private boolean draggingHudOpacity;
    private long openedAt;
    private final Screen parent;

    public AzureMenuScreen() {
        this(null);
    }

    public AzureMenuScreen(Screen parent) {
        super(Text.literal("Azure Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        scrollOffset = 0;
    }

    private int panelWidth() {
        return Math.min(640, width - 20);
    }

    private int panelHeight() {
        return Math.min(412, height - 20);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int contentLeft() {
        return panelLeft() + 14;
    }

    private int contentWidth() {
        return panelWidth() - 28;
    }

    private int contentTop() {
        return panelTop() + 84;
    }

    private int contentHeight() {
        return Math.max(24, panelHeight() - 84 - 26);
    }

    private int columns() {
        return contentWidth() >= 520 ? 3 : 2;
    }

    private int cardWidth() {
        return (contentWidth() - (columns() - 1) * 8) / columns();
    }

    private int cardHeight() {
        return 74;
    }

    private int visibleRows() {
        return Math.max(1, contentHeight() / (cardHeight() + 8));
    }

    private int searchLeft() {
        return panelLeft() + panelWidth() - 14 - 132;
    }

    private int tabsTop() {
        return panelTop() + 52;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AzureConfig config = AzureClient.config();
        int accent = config.chromaHud ? AzureClient.getChromaColor(0) : config.accentColor();
        int left = panelLeft();
        int top = panelTop();
        int panelW = panelWidth();
        int panelH = panelHeight();

        context.fill(0, 0, width, height, 0xaa05080d);
        context.fill(left + 5, top + 6, left + panelW + 5, top + panelH + 6, 0x66000000);
        context.fill(left, top, left + panelW, top + panelH, AzureHud.withOpacity(0xff0b1018, config.menuOpacity));
        context.fill(left, top, left + panelW, top + 2, accent);
        AzureHud.drawBorder(context, left, top, panelW, panelH, COLOR_LINE);

        // Header
        context.fill(left + 14, top + 9, left + 40, top + 35, accent);
        context.drawCenteredTextWithShadow(textRenderer, "A", left + 27, top + 18, 0xff0b1018);
        context.drawTextWithShadow(textRenderer, "AZURE CLIENT", left + 48, top + 10, COLOR_TEXT);
        String modVersion = FabricLoader.getInstance().getModContainer("azureclient")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("1.2.0");
        context.drawTextWithShadow(textRenderer, "v" + modVersion + "  ·  1.20.x  ·  CLIENT MENU",
            left + 48, top + 22, accent);

        String account = client != null ? client.getSession().getUsername() : "PLAYER";
        int accountWidth = Math.min(120, textRenderer.getWidth(account) + 34);
        int accountX = left + panelW - 14 - 18 - 6 - accountWidth;
        context.fill(accountX, top + 9, accountX + accountWidth, top + 31, 0xff111a26);
        context.fill(accountX + 3, top + 11, accountX + 21, top + 29, accent);
        context.drawCenteredTextWithShadow(textRenderer, account.substring(0, 1).toUpperCase(Locale.ROOT),
            accountX + 12, top + 16, 0xff0b1018);
        context.drawTextWithShadow(textRenderer, account, accountX + 25, top + 16, COLOR_TEXT);

        int hudX = accountX - 40;
        boolean hudHovered = inside(mouseX, mouseY, hudX, top + 9, 34, 22);
        context.fill(hudX, top + 9, hudX + 34, top + 31, hudHovered ? 0xff24384c : 0xff111a26);
        AzureHud.drawBorder(context, hudX, top + 9, 34, 22, hudHovered ? accent : COLOR_LINE);
        context.drawCenteredTextWithShadow(textRenderer, "HUD", hudX + 17, top + 16,
            hudHovered ? accent : COLOR_MUTED);

        int closeX = left + panelW - 14 - 16;
        boolean closeHovered = inside(mouseX, mouseY, closeX, top + 9, 16, 22);
        context.fill(closeX, top + 9, closeX + 16, top + 31, closeHovered ? 0xff5a2330 : 0xff1a1118);
        context.drawCenteredTextWithShadow(textRenderer, "x", closeX + 8, top + 16,
            closeHovered ? 0xffffd6de : 0xffc98d9a);

        // Tabs
        int tabX = left + 14;
        for (Category category : Category.values()) {
            int tabWidth = textRenderer.getWidth(category.title) + 16;
            boolean active = selectedCategory == category;
            boolean hovered = inside(mouseX, mouseY, tabX, tabsTop(), tabWidth, 22);
            context.fill(tabX, tabsTop(), tabX + tabWidth, tabsTop() + 22,
                active ? 0xff20344a : (hovered ? 0xff182333 : 0xff101722));
            if (active) context.fill(tabX, tabsTop() + 20, tabX + tabWidth, tabsTop() + 22, accent);
            AzureHud.drawBorder(context, tabX, tabsTop(), tabWidth, 22, active ? accent : COLOR_LINE);
            context.drawCenteredTextWithShadow(textRenderer, category.title, tabX + tabWidth / 2,
                tabsTop() + 7, active ? accent : COLOR_MUTED);
            tabX += tabWidth + 4;
        }

        // Search
        int searchX = searchLeft();
        context.fill(searchX, tabsTop(), searchX + 132, tabsTop() + 22,
            searchFocused ? 0xff16202e : 0xff101722);
        AzureHud.drawBorder(context, searchX, tabsTop(), 132, 22, searchFocused ? accent : COLOR_LINE);
        String searchText = searchQuery.isEmpty() ? (searchFocused ? "" : "Search modules...") : searchQuery;
        context.drawTextWithShadow(textRenderer, searchText, searchX + 7, tabsTop() + 7,
            searchQuery.isEmpty() && !searchFocused ? COLOR_SUBTLE : COLOR_TEXT);
        if (searchFocused && (System.currentTimeMillis() - openedAt) / 500 % 2 == 0) {
            int caretX = searchX + 7 + textRenderer.getWidth(searchText);
            context.fill(caretX, tabsTop() + 5, caretX + 1, tabsTop() + 17, accent);
        }

        switch (selectedCategory) {
            case THEMES -> renderThemes(context, mouseX, mouseY, accent);
            case PRESETS -> renderPresets(context, mouseX, mouseY, accent);
            case COSMETICS -> renderCosmetics(context, mouseX, mouseY, accent);
            case CONTROLS -> renderControls(context, mouseX, mouseY, accent);
            default -> renderModules(context, mouseX, mouseY, accent);
        }

        context.drawTextWithShadow(textRenderer, "AZURE CLIENT  ·  AZURE PERSONAL",
            left + 14, top + panelH - 18, COLOR_SUBTLE);
        String hint = "R-SHIFT / ESC CLOSE";
        context.drawTextWithShadow(textRenderer, hint, left + panelW - 14 - textRenderer.getWidth(hint),
            top + panelH - 18, COLOR_SUBTLE);
    }

    private List<ModEntry> filteredMods() {
        List<ModEntry> list = MODS;
        if (selectedCategory != Category.ALL && selectedCategory != Category.THEMES
            && selectedCategory != Category.PRESETS && selectedCategory != Category.COSMETICS
            && selectedCategory != Category.CONTROLS) {
            list = MODS.stream().filter(mod -> mod.category == selectedCategory).toList();
        }
        if (!searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase(Locale.ROOT);
            list = list.stream().filter(mod ->
                mod.name.toLowerCase(Locale.ROOT).contains(query) ||
                    mod.description.toLowerCase(Locale.ROOT).contains(query)).toList();
        }
        return list;
    }

    private void renderModules(DrawContext context, int mouseX, int mouseY, int accent) {
        List<ModEntry> mods = filteredMods();
        int cols = columns();
        int cardW = cardWidth();
        int cardH = cardHeight();
        int x = contentLeft();
        int y = contentTop();
        int rows = (mods.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows - visibleRows());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(x, y, x + contentWidth(), y + contentHeight());
        for (int index = scrollOffset * cols; index < mods.size(); index++) {
            int visibleIndex = index - scrollOffset * cols;
            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            if (row >= visibleRows()) break;
            ModEntry mod = mods.get(index);
            int cardX = x + col * (cardW + 8);
            int cardY = y + row * (cardH + 8);
            boolean hovered = inside(mouseX, mouseY, cardX, cardY, cardW, cardH);
            boolean enabled = mod.isEnabled();

            context.fill(cardX, cardY, cardX + cardW, cardY + cardH,
                hovered ? COLOR_CARD_HOVER : COLOR_CARD);
            AzureHud.drawBorder(context, cardX, cardY, cardW, cardH, hovered ? 0xff3c5570 : COLOR_LINE);
            context.fill(cardX, cardY, cardX + 3, cardY + cardH, enabled ? accent : 0xff26384c);

            context.drawTextWithShadow(textRenderer, mod.category.title, cardX + 10, cardY + 8, accent);
            context.drawTextWithShadow(textRenderer, mod.name, cardX + 10, cardY + 22, COLOR_TEXT);
            String description = mod.description;
            if (textRenderer.getWidth(description) > cardW - 20) {
                while (description.length() > 3 && textRenderer.getWidth(description + "...") > cardW - 20) {
                    description = description.substring(0, description.length() - 1);
                }
                description += "...";
            }
            context.drawTextWithShadow(textRenderer, description, cardX + 10, cardY + 34, COLOR_MUTED);

            int statusW = 74;
            int statusX = cardX + 10;
            int statusY = cardY + cardH - 22;
            context.fill(statusX, statusY, statusX + statusW, statusY + 15,
                enabled ? 0xff1c6b43 : 0xff1a2433);
            AzureHud.drawBorder(context, statusX, statusY, statusW, 15,
                enabled ? 0xff5ef0a0 : 0xff2c3d51);
            context.drawCenteredTextWithShadow(textRenderer, enabled ? "ENABLED" : "DISABLED",
                statusX + statusW / 2, statusY + 4, enabled ? 0xffb6ffd4 : COLOR_MUTED);

            context.fill(cardX + cardW - 36, cardY + 12, cardX + cardW - 12, cardY + 36,
                enabled ? 0x332f8f74 : 0x221f3044);
            context.drawCenteredTextWithShadow(textRenderer,
                mod.name.substring(0, 1).toUpperCase(Locale.ROOT),
                cardX + cardW - 24, cardY + 20, enabled ? accent : COLOR_SUBTLE);
        }
        context.disableScissor();

        if (mods.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No modules match this filter.",
                contentLeft() + contentWidth() / 2, contentTop() + 24, COLOR_MUTED);
        } else if (maxScroll > 0) {
            String scrollHint = "SCROLL FOR MORE  ·  " + mods.size() + " MODULES";
            context.drawTextWithShadow(textRenderer, scrollHint,
                contentLeft() + contentWidth() - textRenderer.getWidth(scrollHint),
                contentTop() + contentHeight() - 12, COLOR_SUBTLE);
        }
    }

    private void renderThemes(DrawContext context, int mouseX, int mouseY, int accent) {
        String[] names = {"AZURE", "EMBER", "VIOLET", "ROSE", "MINT", "FROST", "CHROMA"};
        int[] colors = {0xff49d8ce, 0xffff8a5b, 0xffa78bfa, 0xfff472b6, 0xff80e0a0, 0xffe6edf7,
            AzureClient.getChromaColor(0)};
        AzureConfig config = AzureClient.config();
        context.drawTextWithShadow(textRenderer, "ACCENT COLOR", contentLeft(), contentTop(), COLOR_MUTED);
        int chipW = 88;
        int chipH = 40;
        for (int i = 0; i < names.length; i++) {
            int col = i % 4;
            int row = i / 4;
            int chipX = contentLeft() + col * (chipW + 8);
            int chipY = contentTop() + 16 + row * (chipH + 8);
            boolean active = config.theme == i;
            boolean hovered = inside(mouseX, mouseY, chipX, chipY, chipW, chipH);
            context.fill(chipX, chipY, chipX + chipW, chipY + chipH,
                active ? 0xff1b2a3a : (hovered ? COLOR_CARD_HOVER : COLOR_CARD));
            context.fill(chipX, chipY, chipX + chipW, chipY + 3, colors[i]);
            AzureHud.drawBorder(context, chipX, chipY, chipW, chipH, active ? colors[i] : COLOR_LINE);
            context.drawCenteredTextWithShadow(textRenderer, names[i], chipX + chipW / 2,
                chipY + 16, active ? colors[i] : COLOR_TEXT);
        }

        int sliderY = contentTop() + 16 + 2 * (chipH + 8) + 18;
        drawSlider(context, contentLeft(), sliderY, 210, "Menu opacity", config.menuOpacity, accent);
        drawSlider(context, contentLeft(), sliderY + 34, 210, "HUD opacity", config.hudOpacity, accent);
    }

    private void drawSlider(DrawContext context, int x, int y, int sliderW, String label, int value, int accent) {
        context.drawTextWithShadow(textRenderer, label + ": " + value + "%", x, y, COLOR_TEXT);
        int trackY = y + 13;
        context.fill(x, trackY, x + sliderW, trackY + 6, 0xff16202c);
        int fill = (int) (sliderW * Math.max(0, Math.min(100, value)) / 100.0f);
        context.fill(x, trackY, x + fill, trackY + 6, accent);
        AzureHud.drawBorder(context, x, trackY, sliderW, 6, COLOR_LINE);
    }

    private void renderPresets(DrawContext context, int mouseX, int mouseY, int accent) {
        String[] ids = {"pvp", "bedwars", "smp", "minimal"};
        String[] titles = {"PVP / HYPIXEL", "BEDWARS & BRIDGE", "SURVIVAL / SMP", "MINIMAL"};
        String[] descriptions = {
            "Watermark, CPS, keystrokes, armor, health, hunger, low fire.",
            "Watermark, keystrokes, coords, direction, server info.",
            "Watermark, coords, biome, potions, server, time and day.",
            "Watermark, FPS, CPS and ping. Clean and distraction-free."
        };
        int[] colors = {0xffff4757, 0xff49d8ce, 0xff80e0a0, 0xffffca78};
        for (int i = 0; i < ids.length; i++) {
            int cardY = contentTop() + i * 56;
            boolean active = ids[i].equalsIgnoreCase(AzureClient.config().hudPreset);
            boolean hovered = inside(mouseX, mouseY, contentLeft(), cardY, contentWidth(), 48);
            context.fill(contentLeft(), cardY, contentLeft() + contentWidth(), cardY + 48,
                hovered || active ? COLOR_CARD_HOVER : COLOR_CARD);
            context.fill(contentLeft(), cardY, contentLeft() + 3, cardY + 48, colors[i]);
            AzureHud.drawBorder(context, contentLeft(), cardY, contentWidth(), 48,
                active ? colors[i] : COLOR_LINE);
            context.drawTextWithShadow(textRenderer, titles[i], contentLeft() + 12, cardY + 9,
                active ? colors[i] : COLOR_TEXT);
            context.drawTextWithShadow(textRenderer, descriptions[i], contentLeft() + 12, cardY + 24, COLOR_MUTED);
            String apply = active ? "ACTIVE" : "APPLY";
            context.drawTextWithShadow(textRenderer, apply,
                contentLeft() + contentWidth() - 12 - textRenderer.getWidth(apply), cardY + 17,
                active ? colors[i] : COLOR_SUBTLE);
        }
    }

    private void renderCosmetics(DrawContext context, int mouseX, int mouseY, int accent) {
        AzureConfig config = AzureClient.config();
        context.fill(contentLeft(), contentTop(), contentLeft() + contentWidth(), contentTop() + 150, COLOR_CARD);
        AzureHud.drawBorder(context, contentLeft(), contentTop(), contentWidth(), 150, COLOR_LINE);
        context.drawCenteredTextWithShadow(textRenderer, "FREE STARTER COSMETIC  ·  AZURE CROWN",
            contentLeft() + contentWidth() / 2, contentTop() + 12, accent);
        context.drawCenteredTextWithShadow(textRenderer, "Obsidian and Azure headwear, visible in third person",
            contentLeft() + contentWidth() / 2, contentTop() + 28, COLOR_MUTED);

        int cx = contentLeft() + contentWidth() / 2 - 40;
        int cy = contentTop() + 52;
        int dark = 0xff111824;
        context.fill(cx + 4, cy + 18, cx + 76, cy + 34, dark);
        context.fill(cx, cy + 12, cx + 12, cy + 34, dark);
        context.fill(cx + 26, cy + 4, cx + 38, cy + 34, dark);
        context.fill(cx + 42, cy + 4, cx + 54, cy + 34, dark);
        context.fill(cx + 68, cy + 12, cx + 80, cy + 34, dark);
        context.fill(cx + 2, cy + 32, cx + 78, cy + 36, accent);
        context.fill(cx + 36, cy + 18, cx + 44, cy + 26, accent);

        int btnW = 200;
        int btnH = 30;
        int btnX = contentLeft() + (contentWidth() - btnW) / 2;
        int btnY = contentTop() + 108;
        boolean hovered = inside(mouseX, mouseY, btnX, btnY, btnW, btnH);
        context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hovered ? 0xff253a4e : 0xff162332);
        AzureHud.drawBorder(context, btnX, btnY, btnW, btnH,
            config.azureCrownEquipped ? accent : COLOR_LINE);
        context.drawCenteredTextWithShadow(textRenderer,
            config.azureCrownEquipped ? "EQUIPPED  ·  CLICK TO REMOVE" : "EQUIP AZURE CROWN",
            btnX + btnW / 2, btnY + 11, config.azureCrownEquipped ? accent : COLOR_TEXT);
    }

    private void renderControls(DrawContext context, int mouseX, int mouseY, int accent) {
        context.drawTextWithShadow(textRenderer, "AZURE KEY BINDINGS", contentLeft(), contentTop(), COLOR_TEXT);
        context.drawTextWithShadow(textRenderer, "Change the menu, HUD editor, zoom, and movement keys",
            contentLeft(), contentTop() + 18, COLOR_MUTED);
        context.drawTextWithShadow(textRenderer, "in Minecraft Controls under Azure Personal.",
            contentLeft(), contentTop() + 30, COLOR_MUTED);
        context.drawTextWithShadow(textRenderer, "Menu shortcut:  RIGHT SHIFT",
            contentLeft(), contentTop() + 52, accent);

        int buttonX = contentLeft();
        int buttonY = contentTop() + 76;
        int buttonW = 180;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, 26);
        context.fill(buttonX, buttonY, buttonX + buttonW, buttonY + 26,
            hovered ? 0xff24384c : 0xff162232);
        AzureHud.drawBorder(context, buttonX, buttonY, buttonW, 26, hovered ? accent : COLOR_LINE);
        context.drawCenteredTextWithShadow(textRenderer, "OPEN KEY BINDINGS",
            buttonX + buttonW / 2, buttonY + 9, hovered ? accent : COLOR_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int left = panelLeft();
        int top = panelTop();

        if (inside((int) mouseX, (int) mouseY, left + panelWidth() - 30, top + 9, 16, 22)) {
            close();
            return true;
        }

        if (inside((int) mouseX, (int) mouseY, searchLeft(), tabsTop(), 132, 22)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        int accountWidth = Math.min(120, textRenderer.getWidth(client.getSession().getUsername()) + 34);
        int accountX = left + panelWidth() - 14 - 18 - 6 - accountWidth;
        if (inside((int) mouseX, (int) mouseY, accountX - 40, top + 9, 34, 22)) {
            client.setScreen(new AzureHudEditorScreen(this));
            return true;
        }

        int tabX = left + 14;
        for (Category category : Category.values()) {
            int tabWidth = textRenderer.getWidth(category.title) + 16;
            if (inside((int) mouseX, (int) mouseY, tabX, tabsTop(), tabWidth, 22)) {
                selectedCategory = category;
                searchQuery = "";
                scrollOffset = 0;
                return true;
            }
            tabX += tabWidth + 4;
        }

        if (selectedCategory == Category.THEMES) {
            AzureConfig config = AzureClient.config();
            int chipW = 88;
            int chipH = 40;
            for (int i = 0; i < 7; i++) {
                int col = i % 4;
                int row = i / 4;
                int chipX = contentLeft() + col * (chipW + 8);
                int chipY = contentTop() + 16 + row * (chipH + 8);
                if (inside((int) mouseX, (int) mouseY, chipX, chipY, chipW, chipH)) {
                    config.theme = i;
                    config.save();
                    return true;
                }
            }
            int sliderY = contentTop() + 16 + 2 * (chipH + 8) + 18;
            if (inside((int) mouseX, (int) mouseY, contentLeft(), sliderY + 10, 210, 12)) {
                draggingMenuOpacity = true;
                updateOpacity(mouseX, true);
                return true;
            }
            if (inside((int) mouseX, (int) mouseY, contentLeft(), sliderY + 44, 210, 12)) {
                draggingHudOpacity = true;
                updateOpacity(mouseX, false);
                return true;
            }
            return true;
        }

        if (selectedCategory == Category.PRESETS) {
            String[] ids = {"pvp", "bedwars", "smp", "minimal"};
            for (int i = 0; i < ids.length; i++) {
                int cardY = contentTop() + i * 56;
                if (inside((int) mouseX, (int) mouseY, contentLeft(), cardY, contentWidth(), 48)) {
                    AzureClient.config().applyHudPreset(ids[i]);
                    return true;
                }
            }
            return true;
        }

        if (selectedCategory == Category.COSMETICS) {
            int btnW = 200;
            int btnH = 30;
            int btnX = contentLeft() + (contentWidth() - btnW) / 2;
            int btnY = contentTop() + 108;
            if (inside((int) mouseX, (int) mouseY, btnX, btnY, btnW, btnH)) {
                AzureConfig config = AzureClient.config();
                config.azureCrownEquipped = !config.azureCrownEquipped;
                config.save();
                return true;
            }
            return true;
        }

        if (selectedCategory == Category.CONTROLS) {
            int buttonY = contentTop() + 76;
            if (inside((int) mouseX, (int) mouseY, contentLeft(), buttonY, 180, 26)) {
                client.setScreen(new ControlsOptionsScreen(this, client.options));
                return true;
            }
            return true;
        }

        List<ModEntry> mods = filteredMods();
        int cols = columns();
        int cardW = cardWidth();
        int cardH = cardHeight();
        for (int index = scrollOffset * cols; index < mods.size(); index++) {
            int visibleIndex = index - scrollOffset * cols;
            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            if (row >= visibleRows()) break;
            int cardX = contentLeft() + col * (cardW + 8);
            int cardY = contentTop() + row * (cardH + 8);
            if (inside((int) mouseX, (int) mouseY, cardX, cardY, cardW, cardH)) {
                mods.get(index).toggle();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingMenuOpacity) {
            updateOpacity(mouseX, true);
            return true;
        }
        if (button == 0 && draggingHudOpacity) {
            updateOpacity(mouseX, false);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingMenuOpacity = false;
            draggingHudOpacity = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateOpacity(double mouseX, boolean menu) {
        int value = Math.max(menu ? 35 : 20, Math.min(100,
            (int) Math.round((mouseX - contentLeft()) * 100.0 / 210.0)));
        AzureConfig config = AzureClient.config();
        if (menu) config.menuOpacity = value;
        else config.hudOpacity = value;
        config.save();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < contentLeft() || mouseX > contentLeft() + contentWidth() ||
            mouseY < contentTop() || mouseY > contentTop() + contentHeight()) {
            return false;
        }
        if (selectedCategory == Category.THEMES || selectedCategory == Category.PRESETS
            || selectedCategory == Category.COSMETICS || selectedCategory == Category.CONTROLS) {
            return false;
        }
        int rows = (filteredMods().size() + columns() - 1) / columns();
        int maxScroll = Math.max(0, rows - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (amount < 0 ? 1 : -1)));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && chr >= 32 && chr != 127) {
            searchQuery += chr;
            scrollOffset = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
