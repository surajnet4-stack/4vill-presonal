package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AzureMenuScreen extends Screen {
    private enum Tab {
        ALL("ALL", "Everything"),
        HUD("HUD", "On-screen modules"),
        UTILITY("UTIL", "World & player info"),
        THEMES("THEMES", "Accent colors"),
        PRESETS("PRESET", "Quick loadouts"),
        CONTROLS("KEYS", "Key bindings");

        final String label;
        final String subtitle;

        Tab(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    private static final class ModEntry {
        final String id;
        final String name;
        final String description;
        final Tab tab;

        ModEntry(String id, String name, String description, Tab tab) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.tab = tab;
        }

        boolean enabled() {
            AzureConfig cfg = AzureClient.config();
            return switch (id) {
                case "watermark" -> cfg.showWatermark;
                case "fps" -> cfg.showFps;
                case "ping" -> cfg.showPing;
                case "coords" -> cfg.showCoordinates;
                case "direction" -> cfg.showDirection;
                case "server" -> cfg.showServerInfo;
                case "health" -> cfg.showHealth;
                case "hunger" -> cfg.showHunger;
                case "worldtime" -> cfg.showWorldTime;
                case "day" -> cfg.showDayCounter;
                case "memory" -> cfg.showMemory;
                default -> false;
            };
        }

        void toggle() {
            AzureConfig cfg = AzureClient.config();
            switch (id) {
                case "watermark" -> cfg.showWatermark = !cfg.showWatermark;
                case "fps" -> cfg.showFps = !cfg.showFps;
                case "ping" -> cfg.showPing = !cfg.showPing;
                case "coords" -> cfg.showCoordinates = !cfg.showCoordinates;
                case "direction" -> cfg.showDirection = !cfg.showDirection;
                case "server" -> cfg.showServerInfo = !cfg.showServerInfo;
                case "health" -> cfg.showHealth = !cfg.showHealth;
                case "hunger" -> cfg.showHunger = !cfg.showHunger;
                case "worldtime" -> cfg.showWorldTime = !cfg.showWorldTime;
                case "day" -> cfg.showDayCounter = !cfg.showDayCounter;
                case "memory" -> cfg.showMemory = !cfg.showMemory;
                default -> {
                }
            }
            cfg.save();
        }
    }

    private static final List<ModEntry> MODS = new ArrayList<>();

    static {
        MODS.add(new ModEntry("watermark", "Watermark", "Azure badge with version, FPS & ping", Tab.HUD));
        MODS.add(new ModEntry("fps", "FPS Counter", "Live frames per second", Tab.HUD));
        MODS.add(new ModEntry("ping", "Ping Display", "Server latency with color alerts", Tab.HUD));
        MODS.add(new ModEntry("coords", "Coordinates", "XYZ position on screen", Tab.UTILITY));
        MODS.add(new ModEntry("direction", "Direction", "Facing compass", Tab.UTILITY));
        MODS.add(new ModEntry("server", "Server Info", "Current server address", Tab.UTILITY));
        MODS.add(new ModEntry("health", "Health Display", "Exact health and absorption", Tab.UTILITY));
        MODS.add(new ModEntry("hunger", "Hunger & Saturation", "Food level and saturation", Tab.UTILITY));
        MODS.add(new ModEntry("worldtime", "World Time", "In-game clock", Tab.UTILITY));
        MODS.add(new ModEntry("day", "Day Counter", "Current world day", Tab.UTILITY));
        MODS.add(new ModEntry("memory", "Memory / RAM", "JVM heap usage", Tab.UTILITY));
    }

    private static final int COLOR_TEXT = 0xfff0f4fa;
    private static final int COLOR_MUTED = 0xff8798ad;
    private static final int COLOR_SUBTLE = 0xff5d7088;
    private static final int COLOR_CARD = 0xff111925;
    private static final int COLOR_CARD_HOVER = 0xff182334;
    private static final int COLOR_LINE = 0xff26384c;

    private Tab selectedTab = Tab.ALL;
    private String searchQuery = "";
    private boolean searchFocused;
    private int scrollOffset;
    private boolean draggingMenuOpacity;
    private boolean draggingHudOpacity;
    private long openedAt;

    public AzureMenuScreen() {
        super(Text.literal("Azure Client"));
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

    private int cardColumns() {
        return contentWidth() >= 520 ? 3 : 2;
    }

    private int cardWidth() {
        return (contentWidth() - (cardColumns() - 1) * 8) / cardColumns();
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

        // Dim the game without touching the vanilla blur/background pipeline.
        context.fill(0, 0, width, height, 0xaa05080d);

        // Panel and accent edge.
        context.fill(left + 5, top + 6, left + panelW + 5, top + panelH + 6, 0x66000000);
        context.fill(left, top, left + panelW, top + panelH, AzureHud.opacity(0xff0b1018, config.menuOpacity));
        context.fill(left, top, left + panelW, top + 2, accent);
        AzureHud.border(context, left, top, panelW, panelH, COLOR_LINE);

        // Header.
        context.fill(left + 14, top + 9, left + 40, top + 35, accent);
        context.drawCenteredTextWithShadow(textRenderer, "A", left + 27, top + 18, 0xff0b1018);
        context.drawTextWithShadow(textRenderer, "AZURE CLIENT", left + 48, top + 10, COLOR_TEXT);
        context.drawTextWithShadow(textRenderer, "v1.2.0  ·  1.21.11  ·  CLIENT MENU", left + 48, top + 22, accent);

        // Account chip and close button.
        String account = client != null ? client.getSession().getUsername() : "PLAYER";
        int accountWidth = Math.min(120, textRenderer.getWidth(account) + 34);
        int accountX = left + panelW - 14 - 18 - 6 - accountWidth;
        context.fill(accountX, top + 9, accountX + accountWidth, top + 31, 0xff111a26);
        context.fill(accountX + 3, top + 11, accountX + 21, top + 29, accent);
        context.drawCenteredTextWithShadow(textRenderer, account.substring(0, 1).toUpperCase(Locale.ROOT),
            accountX + 12, top + 16, 0xff0b1018);
        context.drawTextWithShadow(textRenderer, account, accountX + 25, top + 16, COLOR_TEXT);

        int closeX = left + panelW - 14 - 16;
        boolean closeHovered = inside(mouseX, mouseY, closeX, top + 9, 16, 22);
        context.fill(closeX, top + 9, closeX + 16, top + 31, closeHovered ? 0xff5a2330 : 0xff1a1118);
        context.drawCenteredTextWithShadow(textRenderer, "x", closeX + 8, top + 16,
            closeHovered ? 0xffffd6de : 0xffc98d9a);

        // Tab row.
        int tabX = left + 14;
        for (Tab tab : Tab.values()) {
            int tabWidth = textRenderer.getWidth(tab.label) + 16;
            boolean active = selectedTab == tab;
            boolean hovered = inside(mouseX, mouseY, tabX, tabsTop(), tabWidth, 22);
            context.fill(tabX, tabsTop(), tabX + tabWidth, tabsTop() + 22,
                active ? 0xff20344a : (hovered ? 0xff182333 : 0xff101722));
            if (active) context.fill(tabX, tabsTop() + 20, tabX + tabWidth, tabsTop() + 22, accent);
            AzureHud.border(context, tabX, tabsTop(), tabWidth, 22, active ? accent : COLOR_LINE);
            context.drawCenteredTextWithShadow(textRenderer, tab.label, tabX + tabWidth / 2,
                tabsTop() + 7, active ? accent : COLOR_MUTED);
            tabX += tabWidth + 5;
        }

        // Search.
        int searchX = searchLeft();
        context.fill(searchX, tabsTop(), searchX + 132, tabsTop() + 22,
            searchFocused ? 0xff16202e : 0xff101722);
        AzureHud.border(context, searchX, tabsTop(), 132, 22, searchFocused ? accent : COLOR_LINE);
        String searchText = searchQuery.isEmpty()
            ? (searchFocused ? "" : "Search modules...")
            : searchQuery;
        context.drawTextWithShadow(textRenderer, searchText, searchX + 7, tabsTop() + 7,
            searchQuery.isEmpty() && !searchFocused ? COLOR_SUBTLE : COLOR_TEXT);
        if (searchFocused && (System.currentTimeMillis() - openedAt) / 500 % 2 == 0) {
            int caretX = searchX + 7 + textRenderer.getWidth(searchText);
            context.fill(caretX, tabsTop() + 5, caretX + 1, tabsTop() + 17, accent);
        }

        // Content.
        switch (selectedTab) {
            case THEMES -> renderThemes(context, mouseX, mouseY, accent);
            case PRESETS -> renderPresets(context, mouseX, mouseY, accent);
            case CONTROLS -> renderControls(context, mouseX, mouseY, accent);
            default -> renderModules(context, mouseX, mouseY, accent);
        }

        // Footer.
        String version = "AZURE CLIENT 1.2.0  ·  1.21.11";
        context.drawTextWithShadow(textRenderer, version, left + 14, top + panelH - 18, COLOR_SUBTLE);
        String hint = "R-SHIFT / ESC CLOSE";
        context.drawTextWithShadow(textRenderer, hint, left + panelW - 14 - textRenderer.getWidth(hint),
            top + panelH - 18, COLOR_SUBTLE);
    }

    private List<ModEntry> filteredMods() {
        List<ModEntry> list = MODS;
        if (selectedTab == Tab.HUD || selectedTab == Tab.UTILITY) {
            list = MODS.stream().filter(mod -> mod.tab == selectedTab).toList();
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
        int columns = cardColumns();
        int cardW = cardWidth();
        int cardH = cardHeight();
        int x = contentLeft();
        int y = contentTop();
        int rows = (mods.size() + columns - 1) / columns;
        int maxScroll = Math.max(0, rows - visibleRows());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(x, y, x + contentWidth(), y + contentHeight());
        for (int index = scrollOffset * columns; index < mods.size(); index++) {
            int visibleIndex = index - scrollOffset * columns;
            int col = visibleIndex % columns;
            int row = visibleIndex / columns;
            if (row >= visibleRows()) break;
            ModEntry mod = mods.get(index);
            int cardX = x + col * (cardW + 8);
            int cardY = y + row * (cardH + 8);
            boolean hovered = inside(mouseX, mouseY, cardX, cardY, cardW, cardH);
            boolean enabled = mod.enabled();

            context.fill(cardX, cardY, cardX + cardW, cardY + cardH,
                hovered ? COLOR_CARD_HOVER : COLOR_CARD);
            AzureHud.border(context, cardX, cardY, cardW, cardH, hovered ? 0xff3c5570 : COLOR_LINE);
            context.fill(cardX, cardY, cardX + 3, cardY + cardH, enabled ? accent : 0xff26384c);

            context.drawTextWithShadow(textRenderer, mod.tab.label, cardX + 10, cardY + 8, accent);
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
            AzureHud.border(context, statusX, statusY, statusW, 15,
                enabled ? 0xff5ef0a0 : 0xff2c3d51);
            context.drawCenteredTextWithShadow(textRenderer, enabled ? "ENABLED" : "DISABLED",
                statusX + statusW / 2, statusY + 4, enabled ? 0xffb6ffd4 : COLOR_MUTED);

            // Small module mark on the right.
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
            String scrollHint = "SCROLL TO SEE " + mods.size() + " MODULES";
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
            AzureHud.border(context, chipX, chipY, chipW, chipH, active ? colors[i] : COLOR_LINE);
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
        AzureHud.border(context, x, trackY, sliderW, 6, COLOR_LINE);
    }

    private void renderPresets(DrawContext context, int mouseX, int mouseY, int accent) {
        String[] ids = {"pvp", "bedwars", "smp", "minimal"};
        String[] titles = {"PVP / HYPIXEL", "BEDWARS", "SURVIVAL / SMP", "MINIMAL"};
        String[] descriptions = {
            "Watermark, FPS, ping, coordinates, health and hunger.",
            "Clean combat HUD with server info and coordinates.",
            "Survival info: coordinates, direction, time and day.",
            "Only the watermark and FPS counter."
        };
        for (int i = 0; i < ids.length; i++) {
            int cardY = contentTop() + i * 56;
            boolean hovered = inside(mouseX, mouseY, contentLeft(), cardY, contentWidth(), 48);
            context.fill(contentLeft(), cardY, contentLeft() + contentWidth(), cardY + 48,
                hovered ? COLOR_CARD_HOVER : COLOR_CARD);
            context.fill(contentLeft(), cardY, contentLeft() + 3, cardY + 48, accent);
            AzureHud.border(context, contentLeft(), cardY, contentWidth(), 48, COLOR_LINE);
            context.drawTextWithShadow(textRenderer, titles[i], contentLeft() + 12, cardY + 9, COLOR_TEXT);
            context.drawTextWithShadow(textRenderer, descriptions[i], contentLeft() + 12, cardY + 24, COLOR_MUTED);
            String apply = ids[i].equalsIgnoreCase(AzureClient.config().hudPreset) ? "ACTIVE" : "APPLY";
            context.drawTextWithShadow(textRenderer, apply,
                contentLeft() + contentWidth() - 12 - textRenderer.getWidth(apply), cardY + 17, accent);
        }
    }

    private void renderControls(DrawContext context, int mouseX, int mouseY, int accent) {
        context.drawTextWithShadow(textRenderer, "AZURE KEY BINDINGS", contentLeft(), contentTop(), COLOR_TEXT);
        context.drawTextWithShadow(textRenderer, "Open Minecraft Controls to change these shortcuts.",
            contentLeft(), contentTop() + 18, COLOR_MUTED);
        context.drawTextWithShadow(textRenderer, "Menu shortcut:  RIGHT SHIFT", contentLeft(), contentTop() + 42, COLOR_TEXT);

        int buttonX = contentLeft();
        int buttonY = contentTop() + 70;
        int buttonW = 180;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, 26);
        context.fill(buttonX, buttonY, buttonX + buttonW, buttonY + 26,
            hovered ? 0xff24384c : 0xff162232);
        AzureHud.border(context, buttonX, buttonY, buttonW, 26, hovered ? accent : COLOR_LINE);
        context.drawCenteredTextWithShadow(textRenderer, "OPEN KEY BINDINGS",
            buttonX + buttonW / 2, buttonY + 9, hovered ? accent : COLOR_TEXT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int left = panelLeft();
        int top = panelTop();

        if (inside(mouseX, mouseY, left + panelWidth() - 30, top + 9, 16, 22)) {
            close();
            return true;
        }

        if (inside(mouseX, mouseY, searchLeft(), tabsTop(), 132, 22)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        int tabX = left + 14;
        for (Tab tab : Tab.values()) {
            int tabWidth = textRenderer.getWidth(tab.label) + 16;
            if (inside(mouseX, mouseY, tabX, tabsTop(), tabWidth, 22)) {
                selectedTab = tab;
                scrollOffset = 0;
                return true;
            }
            tabX += tabWidth + 5;
        }

        if (selectedTab == Tab.THEMES) {
            AzureConfig config = AzureClient.config();
            int chipW = 88;
            int chipH = 40;
            for (int i = 0; i < 7; i++) {
                int col = i % 4;
                int row = i / 4;
                int chipX = contentLeft() + col * (chipW + 8);
                int chipY = contentTop() + 16 + row * (chipH + 8);
                if (inside(mouseX, mouseY, chipX, chipY, chipW, chipH)) {
                    config.theme = i;
                    config.save();
                    return true;
                }
            }
            int sliderY = contentTop() + 16 + 2 * (chipH + 8) + 18;
            if (inside(mouseX, mouseY, contentLeft(), sliderY + 10, 210, 12)) {
                draggingMenuOpacity = true;
                updateOpacity(mouseX, true);
                return true;
            }
            if (inside(mouseX, mouseY, contentLeft(), sliderY + 44, 210, 12)) {
                draggingHudOpacity = true;
                updateOpacity(mouseX, false);
                return true;
            }
            return true;
        }

        if (selectedTab == Tab.PRESETS) {
            String[] ids = {"pvp", "bedwars", "smp", "minimal"};
            for (int i = 0; i < ids.length; i++) {
                int cardY = contentTop() + i * 56;
                if (inside(mouseX, mouseY, contentLeft(), cardY, contentWidth(), 48)) {
                    AzureClient.config().applyPreset(ids[i]);
                    return true;
                }
            }
            return true;
        }

        if (selectedTab == Tab.CONTROLS) {
            int buttonY = contentTop() + 70;
            if (inside(mouseX, mouseY, contentLeft(), buttonY, 180, 26)) {
                AzureClient.openControls(client, this);
                return true;
            }
            return true;
        }

        List<ModEntry> mods = filteredMods();
        int columns = cardColumns();
        int cardW = cardWidth();
        int cardH = cardHeight();
        for (int index = scrollOffset * columns; index < mods.size(); index++) {
            int visibleIndex = index - scrollOffset * columns;
            int col = visibleIndex % columns;
            int row = visibleIndex / columns;
            if (row >= visibleRows()) break;
            int cardX = contentLeft() + col * (cardW + 8);
            int cardY = contentTop() + row * (cardH + 8);
            if (inside(mouseX, mouseY, cardX, cardY, cardW, cardH)) {
                mods.get(index).toggle();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click.button() == 0 && draggingMenuOpacity) {
            updateOpacity((int) click.x(), true);
            return true;
        }
        if (click.button() == 0 && draggingHudOpacity) {
            updateOpacity((int) click.x(), false);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            draggingMenuOpacity = false;
            draggingHudOpacity = false;
        }
        return super.mouseReleased(click);
    }

    private void updateOpacity(int mouseX, boolean menu) {
        int value = Math.max(menu ? 35 : 20, Math.min(100,
            (int) Math.round((mouseX - contentLeft()) * 100.0 / 210.0)));
        AzureConfig config = AzureClient.config();
        if (menu) config.menuOpacity = value;
        else config.hudOpacity = value;
        config.save();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < contentLeft() || mouseX > contentLeft() + contentWidth() ||
            mouseY < contentTop() || mouseY > contentTop() + contentHeight()) {
            return false;
        }
        if (selectedTab != Tab.THEMES && selectedTab != Tab.PRESETS && selectedTab != Tab.CONTROLS) {
            int rows = (filteredMods().size() + cardColumns() - 1) / cardColumns();
            int maxScroll = Math.max(0, rows - visibleRows());
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (verticalAmount < 0 ? 1 : -1)));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchFocused) {
            String typed = input.asString();
            if (!typed.isEmpty() && typed.charAt(0) >= 32 && typed.charAt(0) != 127) {
                searchQuery += typed;
                scrollOffset = 0;
                return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
