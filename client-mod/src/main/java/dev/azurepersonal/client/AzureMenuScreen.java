package dev.azurepersonal.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AzureMenuScreen extends Screen {
    public enum Category {
        ALL("ALL MODS", "Browse all 25+ features"),
        HUD("HUD & OVERLAYS", "Custom on-screen widgets"),
        PVP("PVP & COMBAT", "Combat utilities and hit indicators"),
        VISUALS("RENDER & VISUALS", "Visual enhancements and lighting"),
        MECHANICS("MECHANICS", "Movement and gameplay utilities"),
        THEMES("THEMES & COLORS", "Color schemes and transparency"),
        PRESETS("PRESETS", "Pre-configured profiles"),
        COSMETICS("COSMETICS", "Free cosmetics and accessories"),
        CONTROLS("CONTROLS", "Change Azure key bindings");

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
        public final String icon;
        public float animProgress; // 0.0 to 1.0 for smooth toggle animation

        public ModEntry(String id, String name, String description, Category category, String icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.icon = icon;
            this.animProgress = isEnabled() ? 1.0f : 0.0f;
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
            }
            cfg.hudPreset = "custom";
            cfg.save();
        }
    }

    private static final int PANEL_WIDTH = 680;
    private static final int PANEL_HEIGHT = 460;
    private static final Category[] CATEGORIES = Category.values();
    private static final List<ModEntry> MODS = new ArrayList<>();

    static {
        // HUD
        MODS.add(new ModEntry("watermark", "Watermark", "Branded corner HUD badge", Category.HUD, "✦"));
        MODS.add(new ModEntry("fps", "FPS Counter", "Live frame rate display", Category.HUD, "F"));
        MODS.add(new ModEntry("keycaps", "Keystrokes", "WASD + mouse + space clicks", Category.HUD, "K"));
        MODS.add(new ModEntry("cps", "CPS Counter", "Dual Left & Right click tracker", Category.HUD, "C"));
        MODS.add(new ModEntry("ping", "Ping Display", "Server latency with color alerts", Category.HUD, "P"));
        MODS.add(new ModEntry("armor", "Armor Status", "Equipped armor durability & icons", Category.HUD, "A"));
        MODS.add(new ModEntry("potions", "Potion Effects", "Active effect timers & icons", Category.HUD, "P"));
        MODS.add(new ModEntry("coords", "Coordinates", "XYZ coords + biome + nether calc", Category.HUD, "X"));
        MODS.add(new ModEntry("direction", "Direction", "Facing compass & yaw angle", Category.HUD, "N"));
        MODS.add(new ModEntry("speed", "Speedometer", "Real-time horizontal motion speed", Category.HUD, "S"));
        MODS.add(new ModEntry("clock", "Clock", "Real-world time or in-game clock", Category.HUD, "T"));
        MODS.add(new ModEntry("memory", "Memory / RAM", "JVM heap usage display", Category.HUD, "M"));
        MODS.add(new ModEntry("blockoverlay", "Block Info", "Target block name & coordinates", Category.HUD, "B"));

        // PVP
        MODS.add(new ModEntry("combo", "Combo Counter", "PvP consecutive hit tracker", Category.PVP, "⚔"));
        MODS.add(new ModEntry("reach", "Reach Display", "Calculated attack distance in blocks", Category.PVP, "R"));
        MODS.add(new ModEntry("lowfire", "Low Fire", "Lowers screen fire for clear vision", Category.PVP, "F"));
        MODS.add(new ModEntry("nohurtcam", "No Hurt Cam", "Eliminates camera shake on damage", Category.PVP, "H"));

        // VISUALS
        MODS.add(new ModEntry("crosshair", "Custom Crosshair", "Cross, Dot, Circle & hit colors", Category.VISUALS, "+"));
        MODS.add(new ModEntry("fullbright", "Fullbright", "Boosts brightness for caves & night", Category.VISUALS, "☀"));
        MODS.add(new ModEntry("outline", "Block Outline", "Custom colored highlight on blocks", Category.VISUALS, "O"));
        MODS.add(new ModEntry("chroma", "Chroma HUD", "Smooth animated RGB rainbow wave", Category.VISUALS, "C"));
        MODS.add(new ModEntry("freelook", "Free Look (V)", "360-degree third-person perspective", Category.VISUALS, "V"));

        // MECHANICS
        MODS.add(new ModEntry("sprint", "Toggle Sprint (K)", "Sticky toggle sprint with badge", Category.MECHANICS, "S"));
        MODS.add(new ModEntry("sneak", "Toggle Sneak (J)", "Sticky toggle sneak with badge", Category.MECHANICS, "N"));
        MODS.add(new ModEntry("autosprint", "Auto Sprint", "Forward movement automatically sprints", Category.MECHANICS, "»"));
    }

    private Category selectedCategory = Category.ALL;
    private long openedAt;
    private float animatedTabY = -1;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private int scrollOffset = 0;
    private boolean draggingMenuOpacity;
    private boolean draggingHudOpacity;
    private final Screen parent;

    public AzureMenuScreen() {
        this(null);
    }

    public AzureMenuScreen(Screen parent) {
        super(Text.literal("Azure Personal"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        animatedTabY = -1;
        scrollOffset = 0;
    }

    private int panelLeft() {
        return Math.max(6, (width - PANEL_WIDTH) / 2);
    }

    private int panelTop() {
        return Math.max(6, (height - PANEL_HEIGHT) / 2);
    }

    private int panelEntranceOffset() {
        float progress = Math.min(1.0f, (System.currentTimeMillis() - openedAt) / 200.0f);
        return Math.round((1.0f - progress) * 10.0f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background dim
        context.fill(0, 0, width, height, 0x6e060911);

        AzureConfig config = AzureClient.config();
        int accent = config.chromaHud ? AzureClient.getChromaColor(0) : config.accentColor();

        // Smooth panel entrance animation
        int left = panelLeft();
        int top = panelTop() + panelEntranceOffset();

        // Shadow & Backdrop
        context.fill(left + 6, top + 8, left + PANEL_WIDTH + 6, top + PANEL_HEIGHT + 8, 0x55000000);
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, AzureHud.withOpacity(0xff0d131d, config.menuOpacity));

        // Accent top border
        context.fill(left, top, left + PANEL_WIDTH, top + 2, accent);
        AzureHud.drawBorder(context, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0x4430425a);

        // Sidebar backdrop
        int sidebarWidth = 156;
        context.fill(left + 1, top + 3, left + sidebarWidth, top + PANEL_HEIGHT - 1,
            AzureHud.withOpacity(0xff111824, config.menuOpacity));
        context.fill(left + sidebarWidth, top + 3, left + sidebarWidth + 1, top + PANEL_HEIGHT - 1, 0x442e3f56);

        // Brand Logo in Sidebar Header
        context.fill(left + 14, top + 14, left + 36, top + 36, accent);
        context.drawCenteredTextWithShadow(textRenderer, "A", left + 25, top + 20, 0xff0b1017);
        context.drawTextWithShadow(textRenderer, "AZURE CLIENT", left + 43, top + 15, 0xfff0f4fa);
        String modVersion = FabricLoader.getInstance().getModContainer("azureclient")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
        context.drawTextWithShadow(textRenderer, "v" + modVersion + " · FABRIC", left + 43, top + 26, accent);

        // Header Action Bar (Search Bar, HUD Editor button, Preset Pills)
        int headerX = left + sidebarWidth + 16;
        int headerY = top + 12;
        renderHeaderBar(context, headerX, headerY, PANEL_WIDTH - sidebarWidth - 28, mouseX, mouseY, accent);

        // Animated Tab Navigation in Sidebar
        renderSidebarTabs(context, left + 10, top + 52, sidebarWidth - 20, mouseX, mouseY, accent, delta);

        // Main Content Area
        int contentX = left + sidebarWidth + 16;
        int contentY = top + 46;
        int contentWidth = PANEL_WIDTH - sidebarWidth - 28;
        int contentHeight = PANEL_HEIGHT - 56;

        switch (selectedCategory) {
            case THEMES -> renderThemesTab(context, contentX, contentY, contentWidth, mouseX, mouseY, accent);
            case PRESETS -> renderPresetsTab(context, contentX, contentY, contentWidth, mouseX, mouseY, accent);
            case COSMETICS -> renderCosmeticsTab(context, contentX, contentY, contentWidth, mouseX, mouseY, accent);
            case CONTROLS -> renderControlsTab(context, contentX, contentY, contentWidth, mouseX, mouseY, accent);
            default -> renderModCardsGrid(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, accent, delta);
        }

        // Footer helper
        context.drawTextWithShadow(textRenderer, "ESC / R-SHIFT · CLOSE", left + 14, top + PANEL_HEIGHT - 18, 0xff67788e);
        context.drawTextWithShadow(textRenderer, "R-CTRL · HUD EDITOR", left + sidebarWidth - 65, top + PANEL_HEIGHT - 18, 0xff67788e);
    }

    private void renderHeaderBar(DrawContext context, int x, int y, int barWidth, int mouseX, int mouseY, int accent) {
        // 1. Search Box
        int searchWidth = 190;
        int searchHeight = 22;
        context.fill(x, y, x + searchWidth, y + searchHeight, searchFocused ? 0xff16202e : 0xff101722);
        AzureHud.drawBorder(context, x, y, searchWidth, searchHeight, searchFocused ? accent : 0x44394d68);
        String searchDisplay = searchQuery.isEmpty()
            ? (searchFocused ? "" : "Search mods...")
            : searchQuery;
        int textColor = searchQuery.isEmpty() && !searchFocused ? 0xff6b7d94 : 0xfff0f4fa;
        context.drawTextWithShadow(textRenderer, searchDisplay, x + 7, y + 7, textColor);

        // 2. [✏ EDIT HUD] Button
        int editorBtnX = x + searchWidth + 10;
        int editorBtnWidth = 86;
        boolean editorHover = inside(mouseX, mouseY, editorBtnX, y, editorBtnWidth, 22);
        context.fill(editorBtnX, y, editorBtnX + editorBtnWidth, y + 22, editorHover ? 0xff25384c : 0xff162332);
        AzureHud.drawBorder(context, editorBtnX, y, editorBtnWidth, 22, editorHover ? 0xffffffff : accent);
        context.drawCenteredTextWithShadow(textRenderer, "✏ EDIT HUD", editorBtnX + editorBtnWidth / 2, y + 7,
            editorHover ? 0xffffffff : accent);

        // 3. Quick Preset Pills
        String[] presets = {"PVP", "BEDWARS", "SMP", "MIN"};
        int pillStartX = editorBtnX + editorBtnWidth + 10;
        int pillWidth = 44;
        for (int i = 0; i < presets.length; i++) {
            int px = pillStartX + i * (pillWidth + 4);
            boolean isCur = presets[i].equalsIgnoreCase(AzureClient.config().hudPreset);
            boolean hov = inside(mouseX, mouseY, px, y, pillWidth, 22);
            context.fill(px, y, px + pillWidth, y + 22, isCur ? 0xff203746 : (hov ? 0xff1d2838 : 0xff121824));
            AzureHud.drawBorder(context, px, y, pillWidth, 22, isCur ? accent : 0x33455973);
            context.drawCenteredTextWithShadow(textRenderer, presets[i], px + pillWidth / 2, y + 7,
                isCur ? accent : (hov ? 0xfff0f4fa : 0xff8ca0b8));
        }
    }

    private void renderSidebarTabs(DrawContext context, int x, int startY, int tabWidth,
                                   int mouseX, int mouseY, int accent, float delta) {
        int tabHeight = 28;
        int gap = 3;
        int targetY = startY + selectedCategory.ordinal() * (tabHeight + gap);

        if (animatedTabY < 0) {
            animatedTabY = targetY;
        } else {
            animatedTabY += (targetY - animatedTabY) * 0.35f;
        }

        // Gliding indicator capsule behind active tab
        context.fill(x, (int) animatedTabY, x + tabWidth, (int) animatedTabY + tabHeight, 0xff1b2636);
        context.fill(x, (int) animatedTabY, x + 3, (int) animatedTabY + tabHeight, accent);
        AzureHud.drawBorder(context, x, (int) animatedTabY, tabWidth, tabHeight, 0x444b6485);

        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = startY + i * (tabHeight + gap);
            boolean active = selectedCategory == CATEGORIES[i];
            boolean hovered = inside(mouseX, mouseY, x, y, tabWidth, tabHeight);

            if (hovered && !active) {
                context.fill(x, y, x + tabWidth, y + tabHeight, 0x3328394e);
            }

            int labelColor = active ? 0xfff0f4fa : (hovered ? 0xffdbe5f2 : 0xff8798ad);
            context.drawTextWithShadow(textRenderer, CATEGORIES[i].title, x + 10, y + 10, labelColor);
        }
    }

    // --- Mod Cards Grid with Smooth Sliding Animated Switches ---
    private void renderModCardsGrid(DrawContext context, int x, int y, int width, int height,
                                    int mouseX, int mouseY, int accent, float delta) {
        List<ModEntry> list = getFilteredMods();
        int columns = 2;
        int cardGap = 8;
        int cardWidth = (width - cardGap) / columns;
        int cardHeight = 52;
        int rowsPerPage = 6;
        int maxScroll = Math.max(0, (list.size() + 1) / columns - rowsPerPage);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int startRow = scrollOffset;
        int visibleCount = rowsPerPage * columns;

        for (int i = 0; i < visibleCount; i++) {
            int index = startRow * columns + i;
            if (index >= list.size()) break;

            ModEntry mod = list.get(index);
            int col = i % columns;
            int row = i / columns;
            int cardX = x + col * (cardWidth + cardGap);
            int cardY = y + row * (cardHeight + cardGap);

            // Animate toggle switch progress
            boolean enabled = mod.isEnabled();
            float targetAnim = enabled ? 1.0f : 0.0f;
            mod.animProgress += (targetAnim - mod.animProgress) * 0.35f;

            boolean hovered = inside(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight);

            // Card background & borders
            int cardBg = hovered ? 0xff162232 : 0xff101723;
            context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, cardBg);
            context.fill(cardX, cardY, cardX + 3, cardY + cardHeight, enabled ? accent : 0xff253245);
            AzureHud.drawBorder(context, cardX, cardY, cardWidth, cardHeight,
                hovered ? 0x88496482 : (enabled ? 0x444b6c8f : 0x22354a63));

            // Icon & Labels
            context.drawTextWithShadow(textRenderer, mod.icon, cardX + 11, cardY + 12, enabled ? accent : 0xff6a7f99);
            context.drawTextWithShadow(textRenderer, mod.name, cardX + 27, cardY + 11, 0xfff0f4fa);
            context.drawTextWithShadow(textRenderer, mod.description, cardX + 27, cardY + 26, 0xff7c8fa6);

            // --- Animated Toggle Pill Switch ---
            int pillWidth = 28;
            int pillHeight = 14;
            int pillX = cardX + cardWidth - pillWidth - 12;
            int pillY = cardY + (cardHeight - pillHeight) / 2;

            // Interpolate pill track color
            int trackColor = interpolateColor(0xff233042, accent, mod.animProgress);
            context.fill(pillX, pillY, pillX + pillWidth, pillY + pillHeight, trackColor);
            AzureHud.drawBorder(context, pillX, pillY, pillWidth, pillHeight, 0x44587396);

            // Sliding knob
            int knobMinX = pillX + 2;
            int knobMaxX = pillX + pillWidth - 12;
            int knobX = (int) (knobMinX + (knobMaxX - knobMinX) * mod.animProgress);
            context.fill(knobX, pillY + 2, knobX + 10, pillY + pillHeight - 2, 0xffffffff);
        }

        if (list.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No features match this search.", x + 8, y + 16, 0xff9aabc0);
        }

        if (maxScroll > 0) {
            context.drawTextWithShadow(textRenderer, "Scroll to browse more (" + list.size() + " mods)",
                x + 4, y + rowsPerPage * (cardHeight + cardGap) + 4, 0xff6c7f96);
        }
    }

    private List<ModEntry> getFilteredMods() {
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            return MODS.stream()
                .filter(m -> m.name.toLowerCase(Locale.ROOT).contains(q) ||
                    m.description.toLowerCase(Locale.ROOT).contains(q))
                .toList();
        }
        if (selectedCategory == Category.ALL) return MODS;
        return MODS.stream().filter(m -> m.category == selectedCategory).toList();
    }

    // --- Themes Tab ---
    private void renderThemesTab(DrawContext context, int x, int y, int width, int mouseX, int mouseY, int accent) {
        String[] themeNames = {"AZURE", "EMBER", "VIOLET", "ROSE", "MINT", "FROST", "CHROMA (RGB)"};
        int[] colors = {0xff49d8ce, 0xffff8a5b, 0xffa78bfa, 0xfff472b6, 0xff80e0a0, 0xffe6edf7, AzureClient.getChromaColor(0)};
        AzureConfig config = AzureClient.config();

        context.drawTextWithShadow(textRenderer, "SELECT COLOR SCHEME", x, y, 0xffa0b3c9);

        int chipWidth = 110;
        int chipHeight = 44;
        int startY = y + 16;
        for (int i = 0; i < themeNames.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int chipX = x + col * (chipWidth + 12);
            int chipY = startY + row * (chipHeight + 10);
            boolean active = config.theme == i;
            boolean hovered = inside(mouseX, mouseY, chipX, chipY, chipWidth, chipHeight);

            context.fill(chipX, chipY, chipX + chipWidth, chipY + chipHeight,
                active ? 0xff1e2d3e : (hovered ? 0xff162232 : 0xff101724));
            context.fill(chipX, chipY, chipX + chipWidth, chipY + 3, colors[i]);
            AzureHud.drawBorder(context, chipX, chipY, chipWidth, chipHeight,
                active ? colors[i] : (hovered ? 0x88547094 : 0x333b4e68));
            context.drawCenteredTextWithShadow(textRenderer, themeNames[i], chipX + chipWidth / 2, chipY + 20,
                active ? colors[i] : 0xfff0f4fa);
        }

        // Opacity Controls
        int ctrlY = startY + 3 * (chipHeight + 10) + 10;
        context.drawTextWithShadow(textRenderer, "TRANSPARENCY & OPACITY", x, ctrlY, 0xffa0b3c9);
        drawSlider(context, x, ctrlY + 16, 240, "Menu Opacity", config.menuOpacity, 35, 100, mouseX, mouseY, accent);
        drawSlider(context, x, ctrlY + 54, 240, "HUD Background Opacity", config.hudOpacity, 20, 100, mouseX, mouseY, accent);
    }

    // --- Presets Tab ---
    private void renderPresetsTab(DrawContext context, int x, int y, int width, int mouseX, int mouseY, int accent) {
        String[] titles = {"PVP / HYPIXEL", "BEDWARS & BRIDGE", "SURVIVAL / SMP", "MINIMALIST"};
        String[] ids = {"pvp", "bedwars", "smp", "minimal"};
        String[] descs = {
            "Watermark, Dual CPS, Keystrokes, Armor, Combo, Reach, Low Fire, No Hurt Cam",
            "Watermark, Keystrokes, Dual CPS, Coordinates, Direction, Dot Crosshair",
            "Watermark, XYZ coords + Biome + Nether conversion, Potions, Compass, Clock",
            "Watermark, FPS, CPS, Ping. Clean and distraction-free."
        };
        int[] presetColors = {0xffff4757, 0xff49d8ce, 0xff80e0a0, 0xffffca78};

        int cardW = width;
        int cardH = 58;
        for (int i = 0; i < titles.length; i++) {
            int cardY = y + i * (cardH + 10);
            boolean isCur = ids[i].equalsIgnoreCase(AzureClient.config().hudPreset);
            boolean hovered = inside(mouseX, mouseY, x, cardY, cardW, cardH);

            context.fill(x, cardY, x + cardW, cardY + cardH, isCur ? 0xff1b2b3d : (hovered ? 0xff162232 : 0xff101723));
            context.fill(x, cardY, x + 3, cardY + cardH, presetColors[i]);
            AzureHud.drawBorder(context, x, cardY, cardW, cardH, isCur ? presetColors[i] : 0x33445873);

            context.drawTextWithShadow(textRenderer, titles[i], x + 14, cardY + 12, isCur ? presetColors[i] : 0xfff0f4fa);
            context.drawTextWithShadow(textRenderer, isCur ? "[ACTIVE PROFILE]" : "[CLICK TO APPLY]",
                x + cardW - 110, cardY + 12, isCur ? presetColors[i] : 0xff7b91a8);
            context.drawTextWithShadow(textRenderer, descs[i], x + 14, cardY + 32, 0xff8ca0b8);
        }
    }

    // --- Cosmetics Tab ---
    private void renderCosmeticsTab(DrawContext context, int x, int y, int width, int mouseX, int mouseY, int accent) {
        AzureConfig config = AzureClient.config();
        context.fill(x, y, x + width, y + 170, 0xff101723);
        AzureHud.drawBorder(context, x, y, width, 170, 0x33455973);

        context.drawCenteredTextWithShadow(textRenderer, "✦ FREE STARTER COSMETIC · AZURE CROWN ✦",
            x + width / 2, y + 14, accent);
        context.drawCenteredTextWithShadow(textRenderer, "Equip in-game Obsidian & Azure headwear (visible in F5 third-person)",
            x + width / 2, y + 30, 0xff8ca0b8);

        // Crown Visual Graphic
        int cx = x + width / 2 - 40;
        int cy = y + 65;
        int dark = 0xff111824;
        context.fill(cx + 4, cy + 18, cx + 76, cy + 34, dark);
        context.fill(cx, cy + 12, cx + 12, cy + 34, dark);
        context.fill(cx + 26, cy + 4, cx + 38, cy + 34, dark);
        context.fill(cx + 42, cy + 4, cx + 54, cy + 34, dark);
        context.fill(cx + 68, cy + 12, cx + 80, cy + 34, dark);
        context.fill(cx + 2, cy + 32, cx + 78, cy + 36, accent);
        context.fill(cx + 36, cy + 18, cx + 44, cy + 26, accent);

        // Toggle button
        int btnW = 200;
        int btnH = 34;
        int btnX = x + (width - btnW) / 2;
        int btnY = y + 120;
        boolean btnHov = inside(mouseX, mouseY, btnX, btnY, btnW, btnH);
        context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHov ? 0xff253a4e : 0xff162332);
        AzureHud.drawBorder(context, btnX, btnY, btnW, btnH, config.azureCrownEquipped ? accent : 0x444b6485);
        context.drawCenteredTextWithShadow(textRenderer,
            config.azureCrownEquipped ? "EQUIPPED · CLICK TO REMOVE" : "EQUIP AZURE CROWN",
            btnX + btnW / 2, btnY + 13, config.azureCrownEquipped ? accent : 0xfff0f4fa);
    }

    private void renderControlsTab(DrawContext context, int x, int y, int width, int mouseX, int mouseY, int accent) {
        context.drawTextWithShadow(textRenderer, "CUSTOMIZE YOUR AZURE SHORTCUTS", x, y + 18, 0xfff0f4fa);
        context.drawTextWithShadow(textRenderer, "Change the menu, HUD editor, zoom, and movement keys",
            x, y + 38, 0xffaab8cb);
        context.drawTextWithShadow(textRenderer, "in Minecraft's Controls screen under the Azure Personal category.",
            x, y + 52, 0xffaab8cb);

        int buttonWidth = Math.min(230, width);
        int buttonX = x;
        int buttonY = y + 88;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 30);
        context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 30,
            hovered ? 0xff25384c : 0xff162332);
        AzureHud.drawBorder(context, buttonX, buttonY, buttonWidth, 30, hovered ? 0xffffffff : accent);
        context.drawCenteredTextWithShadow(textRenderer, "OPEN KEY BINDINGS",
            buttonX + buttonWidth / 2, buttonY + 11, hovered ? 0xffffffff : accent);
    }

    private void drawSlider(DrawContext context, int x, int y, int sliderWidth, String label,
                            int value, int min, int max, int mouseX, int mouseY, int accent) {
        context.drawTextWithShadow(textRenderer, label + ": " + value + "%", x, y, 0xffd2dbe5);
        int trackY = y + 14;
        int trackH = 8;
        context.fill(x, trackY, x + sliderWidth, trackY + trackH, 0xff16202c);
        float fillRatio = (value - min) / (float) Math.max(1, max - min);
        int fillW = (int) (sliderWidth * fillRatio);
        context.fill(x, trackY, x + fillW, trackY + trackH, accent);
        AzureHud.drawBorder(context, x, trackY, sliderWidth, trackH, 0x444b6485);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int left = panelLeft();
        int top = panelTop() + panelEntranceOffset();
        int sidebarWidth = 156;

        // 1. Search Box Click
        int searchX = left + sidebarWidth + 16;
        int searchY = top + 12;
        if (inside((int) mouseX, (int) mouseY, searchX, searchY, 190, 22)) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // 2. HUD Editor Button Click
        int editorBtnX = searchX + 190 + 10;
        if (inside((int) mouseX, (int) mouseY, editorBtnX, searchY, 86, 22)) {
            client.setScreen(new AzureHudEditorScreen(this));
            return true;
        }

        // 3. Header Preset Pills Click
        String[] presets = {"pvp", "bedwars", "smp", "minimal"};
        int pillStartX = editorBtnX + 86 + 10;
        for (int i = 0; i < presets.length; i++) {
            int px = pillStartX + i * 48;
            if (inside((int) mouseX, (int) mouseY, px, searchY, 44, 22)) {
                AzureClient.config().applyHudPreset(presets[i]);
                return true;
            }
        }

        // 4. Sidebar Tabs Click
        int tabStartY = top + 52;
        int tabHeight = 28;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int ty = tabStartY + i * 31;
            if (inside((int) mouseX, (int) mouseY, left + 10, ty, sidebarWidth - 20, tabHeight)) {
                selectedCategory = CATEGORIES[i];
                searchQuery = "";
                scrollOffset = 0;
                return true;
            }
        }

        // 5. Mod Cards Grid Click
        int contentX = left + sidebarWidth + 16;
        int contentY = top + 46;
        int contentWidth = PANEL_WIDTH - sidebarWidth - 28;
        if (selectedCategory == Category.ALL || selectedCategory == Category.HUD ||
            selectedCategory == Category.PVP || selectedCategory == Category.VISUALS ||
            selectedCategory == Category.MECHANICS) {
            List<ModEntry> list = getFilteredMods();
            int columns = 2;
            int cardGap = 8;
            int cardWidth = (contentWidth - cardGap) / columns;
            int cardHeight = 52;
            int rowsPerPage = 6;
            int visibleCount = rowsPerPage * columns;

            for (int i = 0; i < visibleCount; i++) {
                int index = scrollOffset * columns + i;
                if (index >= list.size()) break;
                int col = i % columns;
                int row = i / columns;
                int cardX = contentX + col * (cardWidth + cardGap);
                int cardY = contentY + row * (cardHeight + cardGap);

                if (inside((int) mouseX, (int) mouseY, cardX, cardY, cardWidth, cardHeight)) {
                    list.get(index).toggle();
                    return true;
                }
            }
        } else if (selectedCategory == Category.THEMES) {
            AzureConfig config = AzureClient.config();
            int chipWidth = 110;
            int chipHeight = 44;
            int startY = contentY + 16;
            for (int i = 0; i < 7; i++) {
                int col = i % 3;
                int row = i / 3;
                int chipX = contentX + col * (chipWidth + 12);
                int chipY = startY + row * (chipHeight + 10);
                if (inside((int) mouseX, (int) mouseY, chipX, chipY, chipWidth, chipHeight)) {
                    config.theme = i;
                    config.save();
                    return true;
                }
            }

            int ctrlY = startY + 3 * (chipHeight + 10) + 10;
            if (inside((int) mouseX, (int) mouseY, contentX, ctrlY + 28, 240, 12)) {
                draggingMenuOpacity = true;
                updateOpacitySlider(mouseX, contentX, 240, 35, 100, true);
                return true;
            }
            if (inside((int) mouseX, (int) mouseY, contentX, ctrlY + 66, 240, 12)) {
                draggingHudOpacity = true;
                updateOpacitySlider(mouseX, contentX, 240, 20, 100, false);
                return true;
            }
        } else if (selectedCategory == Category.PRESETS) {
            String[] ids = {"pvp", "bedwars", "smp", "minimal"};
            int cardH = 58;
            for (int i = 0; i < ids.length; i++) {
                int cy = contentY + i * (cardH + 10);
                if (inside((int) mouseX, (int) mouseY, contentX, cy, contentWidth, cardH)) {
                    AzureClient.config().applyHudPreset(ids[i]);
                    return true;
                }
            }
        } else if (selectedCategory == Category.COSMETICS) {
            int btnW = 200;
            int btnH = 34;
            int btnX = contentX + (contentWidth - btnW) / 2;
            int btnY = contentY + 120;
            if (inside((int) mouseX, (int) mouseY, btnX, btnY, btnW, btnH)) {
                AzureConfig config = AzureClient.config();
                config.azureCrownEquipped = !config.azureCrownEquipped;
                config.save();
                return true;
            }
        } else if (selectedCategory == Category.CONTROLS) {
            int buttonWidth = Math.min(230, contentWidth);
            int buttonY = contentY + 88;
            if (inside((int) mouseX, (int) mouseY, contentX, buttonY, buttonWidth, 30)) {
                client.setScreen(new ControlsOptionsScreen(this, client.options));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int top = panelTop() + panelEntranceOffset();
        int contentX = panelLeft() + 156 + 16;
        int contentY = top + 46;
        int contentWidth = PANEL_WIDTH - 156 - 28;
        if (mouseX < contentX || mouseX >= contentX + contentWidth ||
            mouseY < contentY || mouseY >= top + PANEL_HEIGHT - 24) {
            return false;
        }
        if (selectedCategory == Category.THEMES || selectedCategory == Category.PRESETS ||
            selectedCategory == Category.COSMETICS || selectedCategory == Category.CONTROLS) {
            return false;
        }
        int rows = (getFilteredMods().size() + 1) / 2;
        int maxScroll = Math.max(0, rows - 6);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (amount < 0 ? 1 : -1)));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingMenuOpacity) {
            int contentX = panelLeft() + 156 + 16;
            updateOpacitySlider(mouseX, contentX, 240, 35, 100, true);
            return true;
        }
        if (button == 0 && draggingHudOpacity) {
            int contentX = panelLeft() + 156 + 16;
            updateOpacitySlider(mouseX, contentX, 240, 20, 100, false);
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

    private void updateOpacitySlider(double mouseX, int x, int sliderWidth, int min, int max, boolean menu) {
        int value = Math.max(min, Math.min(max,
            (int) Math.round(min + ((mouseX - x) / sliderWidth) * (max - min))));
        AzureConfig config = AzureClient.config();
        if (menu) config.menuOpacity = value;
        else config.hudOpacity = value;
        config.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || (keyCode == GLFW.GLFW_KEY_ESCAPE && !searchFocused)) {
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

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static int interpolateColor(int colorA, int colorB, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int aA = (colorA >> 24) & 0xff;
        int rA = (colorA >> 16) & 0xff;
        int gA = (colorA >> 8) & 0xff;
        int bA = colorA & 0xff;

        int aB = (colorB >> 24) & 0xff;
        int rB = (colorB >> 16) & 0xff;
        int gB = (colorB >> 8) & 0xff;
        int bB = colorB & 0xff;

        int a = (int) (aA + (aB - aA) * ratio);
        int r = (int) (rA + (rB - rA) * ratio);
        int g = (int) (gA + (gB - gA) * ratio);
        int b = (int) (bA + (bB - bA) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
