package com.lowcode.svintusclient.client.gui;

import com.lowcode.svintusclient.client.Config;
import com.lowcode.svintusclient.client.SvintusclientClient;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.client.ClickGuiModule;
import com.lowcode.svintusclient.client.render.BlurEffect;
import com.lowcode.svintusclient.client.render.PigHead;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.render.icon.Icons;
import com.lowcode.svintusclient.client.util.Anim;
import com.lowcode.svintusclient.client.util.Colors;
import com.lowcode.svintusclient.client.util.I18n;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickGuiScreen extends Screen {

    private static final float DEFAULT_W = 550f, DEFAULT_H = 345f;
    private static final float MIN_W = 440f, MIN_H = 325f;
    private static final float RADIUS = 14f, MARGIN = 14f;
    private static final float HEADER_Y = 12f, LOGO = 26f, HEADER_H = 30f;
    private static final float SIDEBAR_W = 108f, CAT_ITEM_H = 21.5f, CAT_GAP = 2f;
    private static final float FOOTER_H = 26f;
    private static final float SEARCH_W = 126f, SEARCH_H = 20f;
    private static final float COL_GAP = 8f, CARD_GAP = 6f;
    private static final float DOT_GAP = 13f;
    private static final float RESIZE_BORDER = 6f, RESIZE_CORNER = 14f;

    private static float windowW = DEFAULT_W;
    private static float windowH = DEFAULT_H;

    private static Category category = Category.BATTLE;
    private static float offsetX, offsetY;

    private final Anim open = new Anim(0f, 10f);
    private boolean closing;

    private float uiScale = 1f, appliedScale = -1f, animScale = 1f, panelX, panelY;
    private float rawMouseX, rawMouseY;
    private boolean mouseDown, pointer, ibeam, swallowChar;

    private final Anim catIndicator = new Anim(16f);
    private boolean catIndicatorPlaced;
    private final Map<Category, Anim> tabHover = new EnumMap<>(Category.class);
    private final Map<Category, Anim> tabActive = new EnumMap<>(Category.class);

    private final Map<Theme.Accent, Anim> accentHover = new EnumMap<>(Theme.Accent.class);
    private final Map<Theme.Accent, Anim> accentActive = new EnumMap<>(Theme.Accent.class);

    private final Map<Module, ModuleCard> cards = new HashMap<>();
    private final CosmeticsCatalogPanel cosmeticsPanel = new CosmeticsCatalogPanel();
    private final PreferencesPanel preferencesPanel = new PreferencesPanel();
    private final Anim scroll = new Anim(16f);
    private final Anim content = new Anim(0f, 9f);
    private float contentH;
    private List<ModuleCard> visibleCards = List.of();

    private String search = "";
    private boolean searchFocused;
    private final Anim searchFocus = new Anim(14f);
    private final Anim searchHover = new Anim(14f);

    private boolean dragging;
    private float dragStartX, dragStartY, dragOffsetX, dragOffsetY;

    private boolean scrollDragging;
    private float scrollDragStartMouseY, scrollDragStartScroll;

    // Resizing state: 0 = none, 1 = corner (width & height), 2 = right edge (width), 3 = bottom edge (height)
    private int resizeMode;
    private float resizeStartMouseX, resizeStartMouseY;
    private float resizeStartW, resizeStartH;

    public ClickGuiScreen() {
        super(Text.literal("Click GUI"));
        open.to(1f);
        content.to(1f);
        com.lowcode.svintusclient.client.module.client.ClientSoundsModule cs = ModuleManager.get(com.lowcode.svintusclient.client.module.client.ClientSoundsModule.class);
        if (cs != null) cs.playGui(true);
    }

    public static float getWindowWidth() {
        return windowW;
    }

    public static void setWindowWidth(float w) {
        windowW = clamp(w, MIN_W, 1200f);
    }

    public static float getWindowHeight() {
        return windowH;
    }

    public static void setWindowHeight(float h) {
        windowH = clamp(h, MIN_H, 900f);
    }

    private static ClickGuiModule settings() {
        return ModuleManager.CLICK_GUI;
    }

    private float sidebarX() {
        return panelX + MARGIN;
    }

    private float sidebarY() {
        return panelY + HEADER_Y + HEADER_H + 6f;
    }

    private float sidebarH() {
        return windowH - (HEADER_Y + HEADER_H + 6f) - FOOTER_H - MARGIN;
    }

    private float dividerX() {
        return sidebarX() + SIDEBAR_W + 6f;
    }

    private float bodyX() {
        return dividerX() + 7f;
    }

    private float bodyW() {
        return (panelX + windowW - MARGIN) - bodyX();
    }

    private float bodyY() {
        return sidebarY();
    }

    private float bodyH() {
        return sidebarH();
    }

    private float columnW() {
        return Math.max(100f, (bodyW() - COL_GAP) / 2f);
    }

    private float searchX() {
        return panelX + windowW - MARGIN - SEARCH_W;
    }

    private float searchY() {
        return panelY + HEADER_Y + (LOGO - SEARCH_H) / 2f;
    }

    private float footerY() {
        return panelY + windowH - FOOTER_H;
    }

    private float dotX(int i) {
        int n = Theme.Accent.values().length;
        return panelX + windowW - MARGIN - 5f - (n - 1 - i) * DOT_GAP;
    }

    private float dotY() {
        return footerY() + FOOTER_H / 2f;
    }

    private void updateLayout() {
        if (!mouseDown || appliedScale < 0) appliedScale = settings().scale.get();
        Window window = client.getWindow();
        float guiScale = window.getScaleFactor();
        float desired = Math.max(1f, window.getFramebufferHeight() / 1080f * 2f) * appliedScale;
        float fit = Math.min((width - 16f) / windowW, (height - 16f) / windowH);
        uiScale = Math.min(desired / guiScale, fit);
        Render2D.setPixelScale(guiScale * uiScale);

        float vw = width / uiScale, vh = height / uiScale;
        float maxW = Math.max(MIN_W, vw - 24f);
        float maxH = Math.max(MIN_H, vh - 24f);
        windowW = clamp(windowW, MIN_W, maxW);
        windowH = clamp(windowH, MIN_H, maxH);

        float baseX = (vw - windowW) / 2f, baseY = (vh - windowH) / 2f;
        offsetX = clamp(offsetX, 4f - baseX, vw - windowW - 4f - baseX);
        offsetY = clamp(offsetY, 4f - baseY, vh - windowH - 4f - baseY);
        panelX = Render2D.snap(baseX + offsetX);
        panelY = Render2D.snap(baseY + offsetY);
    }

    private float toVirtualX(double x) {
        float cx = panelX + windowW / 2f;
        return ((float) x / uiScale - cx) / animScale + cx;
    }

    private float toVirtualY(double y) {
        float cy = panelY + windowH / 2f;
        return ((float) y / uiScale - cy) / animScale + cy;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client.world == null) renderPanoramaBackground(ctx, delta);
        float dim = settings().dim.get() * Anim.easeOutCubic(open.get());
        if (dim > 0.005f) {
            float prev = Render2D.alpha();
            Render2D.setAlpha(1f);
            Render2D.gradientV(ctx, 0, 0, width, height, 0, Colors.withAlpha(Theme.dimBackdrop(), dim * 0.5f), Colors.withAlpha(Theme.dimBackdrop(), dim));
            Render2D.setAlpha(prev);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float openT = open.get();
        if (closing && openT < 0.02f) {
            super.close();
            return;
        }
        float ease = Anim.easeOutCubic(openT);

        updateLayout();
        if (settings().blur.get()) BlurEffect.capture(ctx);

        animScale = 0.94f + 0.06f * ease;
        rawMouseX = mouseX / uiScale;
        rawMouseY = mouseY / uiScale;
        float mx = toVirtualX(mouseX), my = toVirtualY(mouseY);
        pointer = false;
        ibeam = false;

        float vw = width / uiScale, vh = height / uiScale;
        float maxW = Math.max(MIN_W, vw - 24f);
        float maxH = Math.max(MIN_H, vh - 24f);

        if (resizeMode != 0) {
            float dx = rawMouseX - resizeStartMouseX;
            float dy = rawMouseY - resizeStartMouseY;
            if (resizeMode == 1 || resizeMode == 2) {
                windowW = clamp(resizeStartW + dx, MIN_W, maxW);
            }
            if (resizeMode == 1 || resizeMode == 3) {
                windowH = clamp(resizeStartH + dy, MIN_H, maxH);
            }
        } else if (dragging) {
            offsetX = dragOffsetX + (rawMouseX - dragStartX);
            offsetY = dragOffsetY + (rawMouseY - dragStartY);
        }

        if (scrollDragging) {
            float bh = bodyH();
            float maxScroll = Math.max(0f, contentH - bh);
            if (maxScroll > 0.5f) {
                float trackH = bh - 8f;
                float thumbH = Math.max(18f, trackH * bh / contentH);
                float travel = trackH - thumbH;
                if (travel > 1f) {
                    float dy = my - scrollDragStartMouseY;
                    float newScroll = scrollDragStartScroll + dy * (maxScroll / travel);
                    scroll.snap(clamp(newScroll, 0f, maxScroll));
                }
            }
        }

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(uiScale, uiScale);
        float cx = panelX + windowW / 2f, cy = panelY + windowH / 2f;
        m.translate(cx, cy);
        m.scale(animScale, animScale);
        m.translate(-cx, -cy);
        Render2D.setAlpha(ease);

        drawWindow(ctx);
        drawHeader(ctx, mx, my);
        drawSidebar(ctx, mx, my);
        drawBody(ctx, mx, my);
        drawFooter(ctx, mx, my);
        drawResizeHandle(ctx, mx, my);

        for (ModuleCard card : visibleCards) card.renderOverlays(ctx, mx, my);
        for (ModuleCard card : visibleCards) {
            card.renderHelpTooltip(ctx, width / uiScale, height / uiScale, mx, my);
            if (card.isOverHelp(mx, my)) pointer = true;
        }
        if (anyOverlay()) pointer = true;

        Render2D.setAlpha(1f);
        m.popMatrix();

        // Cursor state management
        boolean overCorner = isOverCorner(mx, my);
        boolean overRight = isOverRightEdge(mx, my);
        boolean overBottom = isOverBottomEdge(mx, my);

        if (resizeMode == 1 || overCorner) {
            ctx.setCursor(StandardCursors.RESIZE_ALL);
        } else if (resizeMode == 2 || overRight) {
            ctx.setCursor(StandardCursors.RESIZE_EW);
        } else if (resizeMode == 3 || overBottom) {
            ctx.setCursor(StandardCursors.RESIZE_NS);
        } else if (dragging) {
            ctx.setCursor(StandardCursors.RESIZE_ALL);
        } else if (ibeam) {
            ctx.setCursor(StandardCursors.IBEAM);
        } else if (pointer) {
            ctx.setCursor(StandardCursors.POINTING_HAND);
        }
    }

    private void drawWindow(DrawContext ctx) {
        float x = panelX, y = panelY, w = windowW, h = windowH;
        Render2D.shadow(ctx, x, y, w, h, RADIUS, 40f, 0xA0000000);
        Render2D.shadow(ctx, x, y, w, h, RADIUS, 90f, Theme.accent(0.07f));

        boolean glass = settings().blur.get()
                && Render2D.blur(ctx, x, y, w, h, RADIUS, Colors.withAlpha(Theme.windowTop(), settings().glass.get()));
        if (!glass) Render2D.gradientV(ctx, x, y, w, h, RADIUS, Theme.windowTop(), Theme.windowBottom());
        else Render2D.gradientV(ctx, x, y, w, h, RADIUS, Colors.withAlpha(Theme.windowTop(), 0.35f), Colors.withAlpha(Theme.windowBottom(), 0.35f));

        Render2D.rect(ctx, x, y, w, h, RADIUS, Theme.accent(0.08f), Theme.accent(0.02f), 0x00000000, Theme.accent(0.03f));
        Render2D.gradientV(ctx, x, y, w, 60f, RADIUS, 0x0CFFFFFF, 0x00FFFFFF);
        Render2D.outline(ctx, x, y, w, h, RADIUS, 1f, 0x2AFFFFFF, 0x1CFFFFFF, 0x0AFFFFFF, 0x0EFFFFFF);
    }

    private void drawHeader(DrawContext ctx, float mx, float my) {
        float lx = panelX + MARGIN, ly = panelY + HEADER_Y;

        Render2D.shadow(ctx, lx, ly, LOGO, LOGO, 8f, 10f, Theme.accent(0.30f));
        Render2D.rect(ctx, lx, ly, LOGO, LOGO, 8f, Theme.accent(0.32f), Theme.accent(0.16f),
                Colors.withAlpha(Theme.accent2(), 0.12f), Colors.withAlpha(Theme.accent2(), 0.22f));
        Render2D.outline(ctx, lx, ly, LOGO, LOGO, 8f, 1f, Theme.accent(0.55f));
        PigHead.draw(ctx, lx + 5f, ly + 5f, LOGO - 10f);

        float tx = lx + LOGO + 9f;
        float w = Fonts.SEMIBOLD.drawCentered(ctx, "Doxaiz", tx, ly + 9f, 9f, Theme.TEXT);
        Fonts.SEMIBOLD.drawCentered(ctx, "Visuals", tx + w + 3f, ly + 9f, 9f, Theme.accent());
        float by = tx + Fonts.REGULAR.drawCentered(ctx, "v1.2 · ", tx, ly + 20f, 5.8f, Theme.TEXT_DIM);
        Fonts.SEMIBOLD.drawGradient(ctx, "Svintus Style", by, ly + 20f, 5.8f, Theme.accent2(), Theme.accent());

        float sx = searchX(), sy = searchY();
        boolean over = Widgets.inside(mx, my, sx, sy, SEARCH_W, SEARCH_H);
        if (over) ibeam = true;
        float f = searchFocus.to(searchFocused).get();
        float hv = searchHover.to(over).get();
        float r = SEARCH_H / 2f;

        Render2D.rect(ctx, sx, sy, SEARCH_W, SEARCH_H, r, Colors.lerp(0x0DFFFFFF, 0x16FFFFFF, Math.max(hv, f)));
        if (f > 0.01f) Render2D.shadow(ctx, sx, sy, SEARCH_W, SEARCH_H, r, 8f, Theme.accent(0.18f * f));
        Render2D.outline(ctx, sx, sy, SEARCH_W, SEARCH_H, r, 1f, Colors.lerp(0x0CFFFFFF, Theme.accent(0.6f), f));
        Icons.SEARCH.drawCentered(ctx, sx + 11f, sy + r, 8f, Colors.lerp(Theme.TEXT_MUTE, Theme.accent(), f));

        float textX = sx + 20f, maxW = SEARCH_W - 28f;
        if (search.isEmpty()) {
            Fonts.REGULAR.drawCentered(ctx, I18n.get("Поиск..."), textX, sy + r, 6.2f, Theme.TEXT_MUTE);
        } else {
            Fonts.REGULAR.drawCentered(ctx, Fonts.REGULAR.trim(search, 6.2f, maxW), textX, sy + r, 6.2f, Theme.TEXT);
        }
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float caret = textX + Math.min(Fonts.REGULAR.width(search, 6.2f), maxW) + 0.8f;
            Render2D.rect(ctx, caret, sy + r - 4f, 0.8f, 8f, 0.4f, Theme.accent());
        }
    }

    private void drawSidebar(DrawContext ctx, float mx, float my) {
        float sx = sidebarX(), sy = sidebarY(), sw = SIDEBAR_W, sh = sidebarH();

        // Subtle vertical divider between categories column and modules
        float dx = dividerX();
        Render2D.rect(ctx, dx, sy, 0.8f, sh, 0.4f, 0x12FFFFFF);

        Category[] categories = Category.values();
        float targetY = sy + category.ordinal() * (CAT_ITEM_H + CAT_GAP);
        if (!catIndicatorPlaced) {
            catIndicator.snap(targetY);
            catIndicatorPlaced = true;
        }
        float iy = catIndicator.to(targetY).get();
        float searching = search.isBlank() ? 1f : 0.35f;

        // Animated capsule indicator
        Render2D.shadow(ctx, sx, iy, sw, CAT_ITEM_H, CAT_ITEM_H / 2f, 9f, Theme.accent(0.25f * searching));
        Render2D.gradientH(ctx, sx, iy, sw, CAT_ITEM_H, CAT_ITEM_H / 2f,
                Theme.accent(0.30f * searching), Colors.withAlpha(Theme.accent2(), 0.20f * searching));
        Render2D.outline(ctx, sx, iy, sw, CAT_ITEM_H, CAT_ITEM_H / 2f, 0.8f, Theme.accent(0.55f * searching));

        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            float itemY = sy + i * (CAT_ITEM_H + CAT_GAP);
            boolean over = Widgets.inside(mx, my, sx, itemY, sw, CAT_ITEM_H);
            if (over) pointer = true;

            float hv = tabHover.computeIfAbsent(cat, c -> new Anim(14f)).to(over).get();
            float a = tabActive.computeIfAbsent(cat, c -> new Anim(c == category ? 1f : 0f, 12f))
                    .to(cat == category).get() * searching;

            if (a <= 0.01f && hv > 0.01f) {
                Render2D.rect(ctx, sx, itemY, sw, CAT_ITEM_H, CAT_ITEM_H / 2f, Colors.withAlpha(0x10FFFFFF, hv));
            }

            float cy = itemY + CAT_ITEM_H / 2f;
            int textCol = Colors.lerp(Colors.lerp(Theme.TEXT_MUTE, Theme.TEXT_DIM, hv), Theme.TEXT, a);
            int iconCol = Colors.lerp(Colors.lerp(Theme.TEXT_MUTE, Theme.TEXT_DIM, hv), Theme.accent(), a);

            cat.icon().drawCentered(ctx, sx + 13f, cy, 9f, iconCol);
            Fonts.SEMIBOLD.drawCentered(ctx, I18n.get(cat.title), sx + 25f, cy, 6.4f, textCol);
        }
    }

    private List<Module> filteredModules() {
        if (search.isBlank()) return ModuleManager.byCategory(category);
        String q = search.toLowerCase(Locale.ROOT).trim();
        return ModuleManager.all().stream()
                .filter(mod -> mod.getName().toLowerCase(Locale.ROOT).contains(q)
                        || mod.getDescription().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private void drawBody(DrawContext ctx, float mx, float my) {
        float x = bodyX(), y = bodyY(), w = bodyW(), h = bodyH();
        float colW = columnW();
        boolean inBody = Widgets.inside(mx, my, x, y, w, h) && !dragging && resizeMode == 0 && !anyOverlay();

        if (category == Category.SETTINGS && search.isBlank()) {
            visibleCards = List.of();
            preferencesPanel.render(ctx, x, y, w, h, mx, my, inBody);
            return;
        }

        if (category == Category.COSMETICS && search.isBlank()) {
            visibleCards = List.of();
            cosmeticsPanel.render(ctx, x, y, w, h, mx, my, inBody, search);
            return;
        }

        List<Module> modules = filteredModules();
        if (modules.isEmpty()) {
            visibleCards = List.of();
            contentH = 0;
            drawEmpty(ctx, x, y, w, h);
            return;
        }

        float c = Anim.easeOutCubic(content.get());
        float prev = Render2D.alpha();
        Render2D.setAlpha(prev * c);
        Render2D.pushScissor(ctx, x - 4f, y, w + 8f, h);

        float top = y - scroll.get() + (1f - c) * 10f;
        float[] columns = {top, top};
        List<ModuleCard> visible = new ArrayList<>(modules.size());

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            ModuleCard card = cards.computeIfAbsent(module, ModuleCard::new);
            int col = i % 2;
            float cardX = x + col * (colW + COL_GAP);
            card.render(ctx, cardX, columns[col], colW, mx, my, inBody);
            if (inBody && card.isPointerOver(mx, my)) pointer = true;
            columns[col] += card.height() + CARD_GAP;
            visible.add(card);
        }
        visibleCards = visible;
        contentH = Math.max(columns[0], columns[1]) - top - CARD_GAP;

        Render2D.popScissor(ctx);
        Render2D.setAlpha(prev);

        float max = Math.max(0f, contentH - h);
        if (scroll.target() > max) scroll.to(max);
        if (max > 0.5f) {
            float trackH = h - 8f;
            float thumbH = Math.max(18f, trackH * h / contentH);
            float thumbY = y + 4f + (trackH - thumbH) * (scroll.get() / max);
            boolean sbOver = inBody && Widgets.inside(mx, my, x + w - 8f, y + 4f, 8f, trackH);
            if (sbOver || scrollDragging) pointer = true;
            int thumbCol = scrollDragging ? Theme.accent() : (sbOver ? 0x60FFFFFF : 0x2AFFFFFF);
            float thumbW = (sbOver || scrollDragging) ? 3.5f : 2.5f;
            Render2D.rect(ctx, x + w - thumbW - 0.5f, thumbY, thumbW, thumbH, thumbW / 2f, thumbCol);
        }
    }

    private void drawEmpty(DrawContext ctx, float x, float y, float w, float h) {
        boolean searching = !search.isBlank();
        float cx = x + w / 2f, cy = y + h / 2f - 12f;
        Render2D.circle(ctx, cx, cy - 4f, 18f, 0x0AFFFFFF);
        Render2D.circleOutline(ctx, cx, cy - 4f, 18f, 0.8f, 0x10FFFFFF);
        (searching ? Icons.SEARCH : category.icon()).drawCentered(ctx, cx, cy - 4f, 16f, Theme.TEXT_MUTE);

        String title = searching ? I18n.get("Ничего не найдено") : I18n.get("Ничего не найдено");
        String sub = searching ? I18n.get("Попробуйте изменить поисковый запрос") : I18n.get("Попробуйте изменить поисковый запрос");
        Fonts.SEMIBOLD.drawCentered(ctx, title, cx - Fonts.SEMIBOLD.width(title, 7.4f) / 2f, cy + 24f, 7.4f, Theme.TEXT_DIM);
        Fonts.REGULAR.drawCentered(ctx, sub, cx - Fonts.REGULAR.width(sub, 6f) / 2f, cy + 35f, 6f, Theme.TEXT_MUTE);
    }

    private void drawFooter(DrawContext ctx, float mx, float my) {
        float y = footerY(), cy = y + FOOTER_H / 2f;
        Render2D.rect(ctx, panelX + MARGIN, y, windowW - MARGIN * 2f, 0.7f, 0.35f, 0x0EFFFFFF);

        float hx = panelX + MARGIN;
        hx = hint(ctx, hx, cy, I18n.get("ЛКМ"), I18n.get("вкл / выкл"));
        hx = hint(ctx, hx + 8f, cy, I18n.get("ПКМ"), I18n.get("настройки"));
        hint(ctx, hx + 8f, cy, I18n.get("Угол"), I18n.get("размер окна"));

        Theme.Accent[] accents = Theme.Accent.values();
        for (int i = 0; i < accents.length; i++) {
            Theme.Accent accent = accents[i];
            float dx = dotX(i), dy = dotY();
            boolean over = Widgets.inside(mx, my, dx - 6f, dy - 6f, 12f, 12f);
            if (over) pointer = true;
            float h = accentHover.computeIfAbsent(accent, a -> new Anim(16f)).to(over).get();
            float a = accentActive.computeIfAbsent(accent, a2 -> new Anim(Theme.current() == accent ? 1f : 0f, 12f))
                    .to(Theme.current() == accent).get();
            if (a > 0.01f) {
                Render2D.shadow(ctx, dx - 4f, dy - 4f, 8f, 8f, 4f, 6f, Colors.withAlpha(accent.primary, 0.5f * a));
                Render2D.circleOutline(ctx, dx, dy, 6f, 1f, Colors.withAlpha(accent.primary, 0.85f * a));
            }
            Render2D.circle(ctx, dx, dy, 3.4f + h * 0.6f - a * 0.3f,
                    Colors.lerp(Colors.lerp(accent.primary, 0xFF000000, 0.2f), accent.primary, Math.max(a, h)));
        }
    }

    private void drawResizeHandle(DrawContext ctx, float mx, float my) {
        float rx = panelX + windowW - 14f;
        float ry = panelY + windowH - 14f;
        boolean over = isOverCorner(mx, my) || resizeMode == 1;

        int gripColor = over ? Theme.accent() : 0x30FFFFFF;
        Render2D.circle(ctx, rx + 8f, ry + 8f, 1.2f, gripColor);
        Render2D.circle(ctx, rx + 4f, ry + 8f, 1.2f, gripColor);
        Render2D.circle(ctx, rx + 8f, ry + 4f, 1.2f, gripColor);
        Render2D.circle(ctx, rx, ry + 8f, 1.0f, gripColor);
        Render2D.circle(ctx, rx + 4f, ry + 4f, 1.0f, gripColor);
        Render2D.circle(ctx, rx + 8f, ry, 1.0f, gripColor);
    }

    private float hint(DrawContext ctx, float x, float cy, String key, String text) {
        float kw = Fonts.SEMIBOLD.width(key, 5.2f) + 8f;
        Render2D.rect(ctx, x, cy - 5f, kw, 10f, 3f, 0x10FFFFFF);
        Fonts.SEMIBOLD.drawCentered(ctx, key, x + 4f, cy, 5.2f, Theme.TEXT_DIM);
        return x + kw + 4f + Fonts.REGULAR.drawCentered(ctx, text, x + kw + 4f, cy, 5.6f, Theme.TEXT_MUTE);
    }

    private boolean isOverCorner(float mx, float my) {
        return Widgets.inside(mx, my, panelX + windowW - RESIZE_CORNER, panelY + windowH - RESIZE_CORNER,
                RESIZE_CORNER + 4f, RESIZE_CORNER + 4f);
    }

    private boolean isOverRightEdge(float mx, float my) {
        return Widgets.inside(mx, my, panelX + windowW - RESIZE_BORDER, panelY + 16f,
                RESIZE_BORDER * 2f, windowH - 32f);
    }

    private boolean isOverBottomEdge(float mx, float my) {
        return Widgets.inside(mx, my, panelX + 16f, panelY + windowH - RESIZE_BORDER,
                windowW - 32f, RESIZE_BORDER * 2f);
    }

    private boolean anyOverlay() {
        for (ModuleCard card : visibleCards) if (card.hasOverlay()) return true;
        return false;
    }

    private void setCategory(Category cat) {
        if (cat == category && search.isBlank()) return;
        category = cat;
        search = "";
        searchFocused = false;
        scroll.snap(0f);
        content.snap(0f).to(1f);
        com.lowcode.svintusclient.client.module.client.ClientSoundsModule csm = ModuleManager.get(com.lowcode.svintusclient.client.module.client.ClientSoundsModule.class);
        if (csm != null) csm.playCategoryChange();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) return true;
        mouseDown = true;
        float mx = toVirtualX(click.x()), my = toVirtualY(click.y());
        int button = click.button();

        if (anyOverlay()) {
            boolean consumed = false;
            for (ModuleCard card : visibleCards) if (card.overlayClicked(mx, my, button)) consumed = true;
            if (consumed) return true;
        }

        // Resizing handles click
        if (button == 0) {
            if (isOverCorner(mx, my)) {
                resizeMode = 1; // corner
                resizeStartMouseX = rawMouseX;
                resizeStartMouseY = rawMouseY;
                resizeStartW = windowW;
                resizeStartH = windowH;
                return true;
            }
            if (isOverRightEdge(mx, my)) {
                resizeMode = 2; // right edge
                resizeStartMouseX = rawMouseX;
                resizeStartMouseY = rawMouseY;
                resizeStartW = windowW;
                resizeStartH = windowH;
                return true;
            }
            if (isOverBottomEdge(mx, my)) {
                resizeMode = 3; // bottom edge
                resizeStartMouseX = rawMouseX;
                resizeStartMouseY = rawMouseY;
                resizeStartW = windowW;
                resizeStartH = windowH;
                return true;
            }
        }

        // Accent dots click
        Theme.Accent[] accents = Theme.Accent.values();
        for (int i = 0; i < accents.length; i++) {
            if (Widgets.inside(mx, my, dotX(i) - 6f, dotY() - 6f, 12f, 12f)) {
                Theme.set(accents[i]);
                Config.save();
                return true;
            }
        }

        // Category selection click
        Category[] categories = Category.values();
        float sx = sidebarX(), sy = sidebarY(), sw = SIDEBAR_W;
        for (int i = 0; i < categories.length; i++) {
            float itemY = sy + i * (CAT_ITEM_H + CAT_GAP);
            if (Widgets.inside(mx, my, sx, itemY, sw, CAT_ITEM_H)) {
                setCategory(categories[i]);
                return true;
            }
        }

        searchFocused = Widgets.inside(mx, my, searchX(), searchY(), SEARCH_W, SEARCH_H);
        if (searchFocused) return true;

        float bx = bodyX(), by = bodyY(), bw = bodyW(), bh = bodyH();
        float maxScroll = Math.max(0f, contentH - bh);
        if (button == 0 && maxScroll > 0.5f && Widgets.inside(mx, my, bx + bw - 8f, by + 4f, 8f, bh - 8f)) {
            scrollDragging = true;
            scrollDragStartMouseY = my;
            scrollDragStartScroll = scroll.get();
            float trackH = bh - 8f;
            float thumbH = Math.max(18f, trackH * bh / contentH);
            float thumbY = by + 4f + (trackH - thumbH) * (scroll.get() / maxScroll);
            if (!Widgets.inside(mx, my, bx + bw - 8f, thumbY, 8f, thumbH)) {
                float travel = trackH - thumbH;
                if (travel > 1f) {
                    float targetThumbY = clamp(my - thumbH / 2f, by + 4f, by + 4f + travel);
                    float targetScroll = (targetThumbY - (by + 4f)) / travel * maxScroll;
                    scroll.to(clamp(targetScroll, 0f, maxScroll));
                    scrollDragStartScroll = targetScroll;
                }
            }
            return true;
        }

        boolean inBody = Widgets.inside(mx, my, bx, by, bw, bh);

        if (category == Category.SETTINGS && search.isBlank() && inBody) {
            if (preferencesPanel.mouseClicked(mx, my, button, inBody)) return true;
        }

        if (category == Category.COSMETICS && search.isBlank() && inBody) {
            float tabH = 22f;
            if (my <= by + tabH + 8f) {
                if (cosmeticsPanel.handleTopBarClick(mx, my, bx, by, bw, tabH)) return true;
            }
            if (cosmeticsPanel.mouseClicked(mx, my, button, inBody)) return true;
        }

        boolean handled = false;
        for (ModuleCard card : visibleCards) {
            if (!handled && inBody && card.mouseClicked(mx, my, button)) handled = true;
            else card.mouseClicked(-1e4f, -1e4f, button);
        }
        if (handled) return true;

        // Window dragging header or empty footer
        boolean header = Widgets.inside(mx, my, panelX, panelY, windowW, HEADER_Y + HEADER_H + 4f);
        boolean footer = Widgets.inside(mx, my, panelX, footerY(), windowW - 40f, FOOTER_H);
        if (button == 0 && (header || footer)) {
            dragging = true;
            dragStartX = rawMouseX;
            dragStartY = rawMouseY;
            dragOffsetX = offsetX;
            dragOffsetY = offsetY;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        mouseDown = false;
        scrollDragging = false;
        if (resizeMode != 0) {
            resizeMode = 0;
            Config.save();
        }
        dragging = false;
        preferencesPanel.mouseReleased();
        cosmeticsPanel.mouseReleased();
        for (ModuleCard card : cards.values()) card.mouseReleased();
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        float mx = toVirtualX(click.x()), my = toVirtualY(click.y());
        if (category == Category.SETTINGS && search.isBlank()) {
            if (preferencesPanel.mouseDragged(mx, my, click.button(), (float) deltaX, (float) deltaY)) {
                return true;
            }
        }

        if (category == Category.COSMETICS && search.isBlank()) {
            if (cosmeticsPanel.mouseDragged(mx, my, click.button(), (float) deltaX, (float) deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (anyOverlay()) return true;
        float mx = toVirtualX(mouseX), my = toVirtualY(mouseY);
        if (Widgets.inside(mx, my, bodyX(), bodyY(), bodyW(), bodyH())) {
            if (category == Category.SETTINGS && search.isBlank()) {
                preferencesPanel.mouseScrolled(mx, my, horizontal, vertical, bodyH());
                return true;
            }
            if (category == Category.COSMETICS && search.isBlank()) {
                cosmeticsPanel.mouseScrolled(mx, my, horizontal, vertical, bodyH());
                return true;
            }
            float max = Math.max(0f, contentH - bodyH());
            scroll.to(clamp(scroll.target() - (float) vertical * 28f, 0f, max));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        swallowChar = false;
        for (ModuleCard card : visibleCards) {
            if (card.keyPressed(key)) {
                swallowChar = true;
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (anyOverlay()) {
                for (ModuleCard card : visibleCards) card.overlayClicked(-1e4f, -1e4f, 0);
            } else if (!search.isEmpty()) {
                search = "";
                searchFocused = false;
            } else {
                close();
            }
            return true;
        }
        if (key == SvintusclientClient.clickGuiKey() && !searchFocused) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
            search = ctrl ? "" : search.substring(0, search.offsetByCodePoints(search.length(), -1));
            scroll.snap(0f);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && searchFocused) {
            searchFocused = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        if (!input.isValidChar()) return false;
        if (search.length() >= 24) return true;
        search += input.asString();
        searchFocused = true;
        scroll.snap(0f);
        return true;
    }

    @Override
    public void close() {
        if (closing) return;
        Config.save();
        closing = true;
        dragging = false;
        resizeMode = 0;
        open.to(0f);
        com.lowcode.svintusclient.client.module.client.ClientSoundsModule cs = ModuleManager.get(com.lowcode.svintusclient.client.module.client.ClientSoundsModule.class);
        if (cs != null) cs.playGui(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static float clamp(float v, float min, float max) {
        return max < min ? min : Math.max(min, Math.min(max, v));
    }
}
