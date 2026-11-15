package com.lowcode.lowcosmetic.client.gui;

import static com.lowcode.lowcosmetic.client.gui.Theme.DANGER;
import static com.lowcode.lowcosmetic.client.gui.Theme.GOLD;
import static com.lowcode.lowcosmetic.client.gui.Theme.STROKE;
import static com.lowcode.lowcosmetic.client.gui.Theme.SURFACE;
import static com.lowcode.lowcosmetic.client.gui.Theme.SURFACE_HOVER;
import static com.lowcode.lowcosmetic.client.gui.Theme.TEXT;
import static com.lowcode.lowcosmetic.client.gui.Theme.TEXT_DIM;
import static com.lowcode.lowcosmetic.client.gui.Theme.TEXT_MUTED;

import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticRegistry;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticState;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticType;
import com.lowcode.lowcosmetic.client.cosmetic.Rarity;
import com.lowcode.lowcosmetic.client.render.ModelPreview;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public final class CosmeticScreen extends Screen {
    private static final float PAD = 8.0F;
    private static final float GAP = 6.0F;
    private static final float MIN_CARD = 86.0F;
    private static final float OPEN_TIME = 0.26F;
    private static final float CLOSE_TIME = 0.14F;

    private final Tab allTab = new Tab("Все", Icons.ALL, c -> true, true);
    private final Tab equippedTab = new Tab("Надето", Icons.EQUIPPED, CosmeticState::isEquipped, false);
    private final Tab favoritesTab = new Tab("Избранное", Icons.STAR, CosmeticState::isFavorite, false);
    private final List<Tab> tabs = List.of(
        allTab,
        typeTab(CosmeticType.WINGS),
        typeTab(CosmeticType.HAT),
        typeTab(CosmeticType.BODYWEAR),
        typeTab(CosmeticType.PET),
        equippedTab,
        favoritesTab
    );

    private Tab tab = allTab;
    private final StringBuilder query = new StringBuilder();
    private boolean searchFocused;
    private List<Cosmetic> shown = List.of();
    private final Map<Integer, CardState> cards = new HashMap<>();
    private Cosmetic hoveredCard;
    private boolean settingsOpen;

    private long lastFrame;
    private float frameDt;
    private float time;
    private boolean closing;
    private float closeTime;
    private final Anim scroll = new Anim(0.0F, 16.0F);
    private final Anim tabX = new Anim(-1.0F, 18.0F);
    private final Anim tabW = new Anim(0.0F, 18.0F);
    private final Anim searchFocus = new Anim(0.0F, 16.0F);
    private final Anim zoom = new Anim(1.0F, 12.0F);
    private final Anim settingsAnim = new Anim(0.0F, 16.0F);
    private final Map<String, Anim> switches = new HashMap<>();
    private float previewYaw;
    private float yawVelocity;
    private boolean resettingYaw;
    private boolean draggingPreview;
    private boolean draggingScrollbar;
    private float scrollbarGrab;
    private boolean dragged;
    private String toast;
    private Identifier toastIcon;
    private int toastColor;
    private float toastTime = -1.0F;

    private float scale = 1.0F;
    private float vw;
    private float vh;
    private float winX;
    private float winY;
    private float winW;
    private float winH;
    private float leftX;
    private float leftY;
    private float leftW;
    private float leftH;
    private float rightX;
    private float rightW;
    private float headerY;
    private float tabsY;
    private float gridX;
    private float gridY;
    private float gridW;
    private float gridH;
    private int cols;
    private float cardW;
    private float cardH;
    private float previewH;
    private float maxScroll;
    private float previewTop;
    private float previewBottom;

    private final List<Hit> hits = new ArrayList<>();

    public CosmeticScreen() {
        super(Text.literal("Гардероб"));
    }

    private static Tab typeTab(CosmeticType type) {
        return new Tab(type.plural, type.icon, c -> c.type() == type, true);
    }

    @Override
    protected void init() {
        if (lastFrame == 0L) {
            lastFrame = Util.getMeasuringTimeNano();
            UiSounds.OPEN.play();
            refilter();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            closeTime = 0.0F;
            searchFocused = false;
            CosmeticState.setPreview(null);
            UiSounds.CLOSE.play();
        }
    }

    @Override
    public void removed() {
        CosmeticState.setPreview(null);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int rawMouseX, int rawMouseY, float delta) {
        long now = Util.getMeasuringTimeNano();
        frameDt = MathHelper.clamp((now - lastFrame) / 1.0E9F, 0.0F, 0.1F);
        lastFrame = now;
        time += frameDt;
        float appear;
        if (closing) {
            closeTime += frameDt;
            appear = 1.0F - Anim.easeOutCubic(closeTime / CLOSE_TIME);
            if (closeTime >= CLOSE_TIME) {
                client.setScreen(null);
                return;
            }
        } else {
            appear = Anim.easeOutCubic(time / OPEN_TIME);
        }

        float mouseX = rawMouseX / scale();
        float mouseY = rawMouseY / scale();
        layout();
        updateAnimations(mouseX, mouseY);
        hits.clear();

        if (client.world != null && UiSettings.blur) {
            applyBlur(ctx);
        }
        Render2D.gradientV(ctx, 0, 0, width, height, Render2D.alpha(0x99050509, appear), Render2D.alpha(0xCC050509, appear));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);
        float pop = 0.965F + 0.035F * appear;
        ctx.getMatrices().translate(vw / 2.0F, vh / 2.0F + (1.0F - appear) * 8.0F);
        ctx.getMatrices().scale(pop, pop);
        ctx.getMatrices().translate(-vw / 2.0F, -vh / 2.0F);

        drawWindow(ctx);
        drawPreviewPanel(ctx, mouseX, mouseY);
        drawHeader(ctx, mouseX, mouseY);
        drawTabs(ctx, mouseX, mouseY);
        drawGrid(ctx, mouseX, mouseY);
        drawToast(ctx);
        drawSettings(ctx, mouseX, mouseY);
        drawTooltip(ctx, mouseX, mouseY);

        ctx.getMatrices().popMatrix();
    }

    private float scale() {
        return scale = UiSettings.scale();
    }

    private void layout() {
        vw = width / scale;
        vh = height / scale;
        winW = Math.min(vw - 16.0F, 620.0F);
        winH = Math.min(vh - 16.0F, 350.0F);
        winX = (vw - winW) / 2.0F;
        winY = (vh - winH) / 2.0F;

        leftX = winX + PAD;
        leftY = winY + PAD;
        leftW = MathHelper.clamp(winW * 0.28F, 118.0F, 172.0F);
        leftH = winH - PAD * 2.0F;

        rightX = leftX + leftW + PAD + 2.0F;
        rightW = winX + winW - PAD - rightX;
        headerY = winY + PAD;
        tabsY = headerY + 30.0F;
        gridX = rightX;
        gridY = tabsY + 26.0F;
        gridW = rightW - 7.0F;
        gridH = winY + winH - PAD - gridY;

        cols = Math.max(2, (int) ((gridW + GAP) / (MIN_CARD + GAP)));
        cardW = (gridW - GAP * (cols - 1)) / cols;
        previewH = cardW * 0.74F;
        cardH = previewH + 34.0F;
        int rows = (shown.size() + cols - 1) / cols;
        maxScroll = Math.max(0.0F, rows * (cardH + GAP) - GAP - gridH);
        if (scroll.target() > maxScroll) {
            scroll.target(maxScroll);
        }
    }

    private void updateAnimations(float mouseX, float mouseY) {
        float dt = frameDt;
        scroll.update(dt);
        searchFocus.target(searchFocused ? 1.0F : 0.0F).update(dt);
        zoom.update(dt);
        settingsAnim.target(settingsOpen ? 1.0F : 0.0F).update(dt);
        if (toastTime >= 0.0F) {
            toastTime += dt;
            if (toastTime > 2.2F) {
                toastTime = -1.0F;
            }
        }

        if (resettingYaw) {
            previewYaw += -previewYaw * (1.0F - (float) Math.exp(-12.0F * dt));
            if (Math.abs(previewYaw) < 0.2F) {
                previewYaw = 0.0F;
                resettingYaw = false;
            }
        } else if (!draggingPreview) {
            previewYaw += yawVelocity * dt;
            yawVelocity *= (float) Math.exp(-4.5F * dt);
        }

        Cosmetic hovered = closing || settingsOpen ? null : cardAt(mouseX, mouseY);
        if (hovered != hoveredCard) {
            hoveredCard = hovered;
            CosmeticState.setPreview(hovered);
            if (hovered != null) {
                UiSounds.HOVER.play(1.1F + (float) Math.random() * 0.15F);
            }
        }
        for (CardState state : cards.values()) {
            state.hover.update(dt);
            state.equip.update(dt);
        }
    }

    private void drawWindow(DrawContext ctx) {
        Render2D.shadow(ctx, winX, winY + 6.0F, winW, winH, 14.0F, 26.0F, 0x88000000);
        Render2D.roundV(ctx, winX, winY, winW, winH, 14.0F, Theme.WINDOW_TOP, Theme.WINDOW_BOTTOM);
        Render2D.roundOutline(ctx, winX, winY, winW, winH, 14.0F, 1.0F, STROKE);
        Render2D.glow(ctx, winX + winW * 0.78F, winY + 10.0F, winW * 0.32F, 60.0F, Theme.accent(0.09F));
    }

    private void drawPreviewPanel(DrawContext ctx, float mouseX, float mouseY) {
        Render2D.roundV(ctx, leftX, leftY, leftW, leftH, 10.0F, Theme.tint(0xFF17171F, 0.12F), 0xFF15151D);
        Render2D.roundOutline(ctx, leftX, leftY, leftW, leftH, 10.0F, 1.0F, STROKE);

        float cx = leftX + leftW / 2.0F;
        float buttonsY = leftY + leftH - 8.0F - 18.0F;
        float chipY = buttonsY - 8.0F - 14.0F;
        float nameY = chipY - 6.0F - 10.0F;
        previewTop = leftY + 24.0F;
        previewBottom = nameY - 6.0F;
        float headroom = 0.45F;
        float available = previewBottom - previewTop - 4.0F;
        float baseSize = available / (1.8F + headroom);
        float size = baseSize * zoom.get();
        float feetY = previewBottom - 6.0F - (zoom.get() - 1.0F) * baseSize * 0.9F;
        float centerY = feetY - size * 0.9F;

        Render2D.glow(ctx, cx, centerY, leftW * 0.5F, available * 0.55F, Theme.accent(0.16F));
        Render2D.glow(ctx, cx, feetY + 1.0F, size * 0.55F, size * 0.11F, 0x99000000);
        Render2D.glow(ctx, cx, feetY + 1.0F, size * 0.42F, size * 0.07F, Theme.accent(0.40F));

        int hintColor = Render2D.alpha(TEXT_MUTED, dragged ? 0.6F : 1.0F);
        Render2D.icon(ctx, Icons.DRAG, leftX + 9.0F, leftY + 8.0F, 9.0F, hintColor);
        UiFont.draw(ctx, "Крути мышкой", leftX + 22.0F, leftY + 9.0F, hintColor);
        boolean changed = Math.abs(MathHelper.wrapDegrees(previewYaw)) > 1.0F || Math.abs(zoom.target() - 1.0F) > 0.01F;
        if (changed) {
            float bx = leftX + leftW - 23.0F;
            float by = leftY + 5.0F;
            boolean hover = inside(mouseX, mouseY, bx, by, 16, 16);
            Render2D.round(ctx, bx, by, 16, 16, 5, hover ? SURFACE_HOVER : SURFACE);
            Render2D.icon(ctx, Icons.ROTATE, bx + 3.5F, by + 3.5F, 9.0F, hover ? TEXT : TEXT_DIM);
            hit(bx, by, 16, 16, () -> {
                resettingYaw = true;
                yawVelocity = 0.0F;
                zoom.target(1.0F);
                UiSounds.CLICK.play();
            }, "Сбросить вид");
        }

        ctx.enableScissor((int) leftX + 1, (int) previewTop - 2, (int) (leftX + leftW) - 1, (int) previewBottom + 4);
        drawPlayer(ctx, cx, centerY, size, mouseX, mouseY);
        ctx.disableScissor();

        ClientPlayerEntity player = client.player;
        String name = UiFont.ellipsize(player != null ? player.getName().getString() : "Player", leftW - 16, true);
        UiFont.drawBoldCentered(ctx, name, cx, nameY, TEXT);

        int equipped = CosmeticState.equippedCount();
        String chip = equipped + " из " + CosmeticType.values().length + " надето";
        float chipW = UiFont.width(chip) + 16.0F;
        Render2D.round(ctx, cx - chipW / 2.0F, chipY, chipW, 14, 7, equipped > 0 ? Theme.accent(0.2F) : 0x14FFFFFF);
        UiFont.drawCentered(ctx, chip, cx, chipY + 3.5F, equipped > 0 ? Theme.accentLight() : TEXT_MUTED);

        float bw = (leftW - 16.0F - 5.0F) / 2.0F;
        boolean any = equipped > 0;
        button(ctx, mouseX, mouseY, leftX + 8.0F, buttonsY, bw, 18, Icons.TRASH, "Снять", any ? DANGER : TEXT_MUTED, any, () -> {
            CosmeticState.unequipAll();
            UiSounds.UNEQUIP.play();
            showToast("Вся косметика снята", Icons.TRASH, DANGER);
            if (tab == equippedTab) {
                refilter();
            }
        }, "Снять всю косметику");
        boolean visible = CosmeticState.isVisible();
        button(ctx, mouseX, mouseY, leftX + 8.0F + bw + 5.0F, buttonsY, bw, 18, visible ? Icons.EYE : Icons.EYE_OFF,
            visible ? "Видно" : "Скрыто", visible ? Theme.accentLight() : TEXT_MUTED, true, () -> {
                CosmeticState.setVisible(!CosmeticState.isVisible());
                boolean now = CosmeticState.isVisible();
                (now ? UiSounds.EQUIP : UiSounds.UNEQUIP).play();
                showToast(now ? "Косметика видна в мире" : "Косметика скрыта в мире", now ? Icons.EYE : Icons.EYE_OFF, now ? Theme.accentLight() : TEXT_DIM);
            }, visible ? "Скрыть косметику в мире" : "Показать косметику в мире");
    }

    private void drawPlayer(DrawContext ctx, float cx, float centerY, float size, float mouseX, float mouseY) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        float bodyYaw = player.bodyYaw;
        float lastBodyYaw = player.lastBodyYaw;
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float headYaw = player.headYaw;
        float lastHeadYaw = player.lastHeadYaw;

        float lookX = settingsOpen ? 0.0F : (float) Math.atan((cx - mouseX) / 60.0F);
        float lookY = settingsOpen ? 0.0F : (float) Math.atan((centerY - size * 0.75F - mouseY) / 60.0F);
        float body = 180.0F + previewYaw;
        player.bodyYaw = body;
        player.lastBodyYaw = body;
        player.setYaw(body + lookX * 30.0F);
        player.headYaw = player.getYaw();
        player.lastHeadYaw = player.getYaw();
        player.setPitch(-lookY * 20.0F);

        CosmeticState.setForceVisible(true);
        try {
            org.joml.Matrix3x2f m = new org.joml.Matrix3x2f(ctx.getMatrices());
            org.joml.Vector2f center = m.transformPosition(cx, centerY, new org.joml.Vector2f());
            float s = (float) Math.hypot(m.m00, m.m01);
            if (s <= 0.001f) s = 1.0f;
            float screenSize = size * s;

            int x1 = (int) Math.floor(center.x - screenSize);
            int y1 = (int) Math.floor(center.y - screenSize * 1.5f);
            int x2 = (int) Math.ceil(center.x + screenSize);
            int y2 = (int) Math.ceil(center.y + screenSize * 0.5f);

            org.joml.Vector2f mouseScreen = m.transformPosition(mouseX, mouseY, new org.joml.Vector2f());
            int entityScale = (int) Math.max(1, (screenSize / Math.max(0.1f, player.getScale())));
            InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, entityScale, previewYaw, mouseScreen.x, mouseScreen.y, player);
        } finally {
            CosmeticState.setForceVisible(false);
            player.bodyYaw = bodyYaw;
            player.lastBodyYaw = lastBodyYaw;
            player.setYaw(yaw);
            player.setPitch(pitch);
            player.headYaw = headYaw;
            player.lastHeadYaw = lastHeadYaw;
        }
    }

    private void button(DrawContext ctx, float mouseX, float mouseY, float x, float y, float w, float h, Identifier icon, String label,
                        int tint, boolean enabled, Runnable action, String hint) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, w, h);
        Render2D.round(ctx, x, y, w, h, 6, hover ? Render2D.lerpColor(SURFACE_HOVER, tint, 0.12F) : SURFACE);
        Render2D.roundOutline(ctx, x, y, w, h, 6, 1, hover ? Render2D.alpha(tint, 0.45F) : STROKE);
        int color = enabled ? (hover ? TEXT : tint) : TEXT_MUTED;
        float labelW = UiFont.width(label);
        boolean compact = labelW + 9.0F + 4.0F + 10.0F > w;
        if (compact) {
            Render2D.icon(ctx, icon, x + (w - 9.0F) / 2.0F, y + (h - 9.0F) / 2.0F, 9.0F, color);
        } else {
            float contentW = 9.0F + 4.0F + labelW;
            float sx = x + (w - contentW) / 2.0F;
            Render2D.icon(ctx, icon, sx, y + (h - 9.0F) / 2.0F, 9.0F, color);
            UiFont.draw(ctx, label, sx + 13.0F, y + (h - 8.0F) / 2.0F, color);
        }
        if (enabled) {
            hit(x, y, w, h, () -> {
                UiSounds.CLICK.play();
                action.run();
            }, compact ? label : hint);
        }
    }

    private float searchW() {
        return Math.min(150.0F, rightW * 0.36F);
    }

    private float closeX() {
        return rightX + rightW - 22.0F;
    }

    private float gearX() {
        return closeX() - 5.0F - 22.0F;
    }

    private float searchX() {
        return gearX() - 6.0F - searchW();
    }

    private void drawHeader(DrawContext ctx, float mouseX, float mouseY) {
        float iy = headerY + 2.0F;
        Render2D.shadow(ctx, rightX, iy + 2, 22, 22, 7, 6, Theme.accent(0.35F));
        Render2D.roundV(ctx, rightX, iy, 22, 22, 7, Theme.accentLight(), Theme.accentDark());
        Render2D.icon(ctx, Icons.HANGER, rightX + 5.0F, iy + 5.0F, 12.0F, 0xFFFFFFFF);

        UiFont.drawBold(ctx, "Гардероб", rightX + 29.0F, iy + 1.5F, TEXT);
        int total = CosmeticRegistry.all().size();
        String sub = total + " " + plural(total, "предмет", "предмета", "предметов") + "  ·  " + CosmeticState.equippedCount() + " надето";
        UiFont.draw(ctx, sub, rightX + 29.0F, iy + 13.0F, TEXT_MUTED);

        iconButton(ctx, mouseX, mouseY, closeX(), iy, Icons.CLOSE, DANGER, this::close, "Закрыть  (Esc)");
        iconButton(ctx, mouseX, mouseY, gearX(), iy, Icons.GEAR, Theme.accentLight(), () -> {
            settingsOpen = !settingsOpen;
            searchFocused = false;
            UiSounds.CLICK.play();
        }, "Настройки меню");

        float sw = searchW();
        float sx = searchX();
        float focus = searchFocus.get();
        boolean searchHover = inside(mouseX, mouseY, sx, iy, sw, 22);
        if (focus > 0.01F) {
            Render2D.shadow(ctx, sx, iy, sw, 22, 7, 5, Theme.accent(0.4F * focus));
        }
        Render2D.round(ctx, sx, iy, sw, 22, 7, searchHover || searchFocused ? SURFACE_HOVER : SURFACE);
        Render2D.roundOutline(ctx, sx, iy, sw, 22, 7, 1, Render2D.lerpColor(STROKE, Theme.accent(0.8F), focus));
        Render2D.icon(ctx, Icons.SEARCH, sx + 7.0F, iy + 6.5F, 9.0F, Render2D.lerpColor(TEXT_MUTED, Theme.accentLight(), focus));

        float textX = sx + 21.0F;
        float textMax = sw - 21.0F - (query.isEmpty() ? 6.0F : 18.0F);
        float caretX = textX;
        if (query.isEmpty()) {
            if (!searchFocused) {
                UiFont.draw(ctx, "Поиск…", textX, iy + 7.0F, TEXT_MUTED);
            }
        } else {
            String visible = UiFont.tail(query.toString(), textMax);
            UiFont.draw(ctx, visible, textX, iy + 7.0F, TEXT);
            caretX += UiFont.width(visible);
            float clearX = sx + sw - 15.0F;
            boolean clearHover = inside(mouseX, mouseY, clearX - 3, iy + 4, 14, 14);
            Render2D.icon(ctx, Icons.CLOSE, clearX, iy + 7.0F, 8.0F, clearHover ? TEXT : TEXT_MUTED);
            hit(clearX - 3, iy + 4, 14, 14, () -> {
                query.setLength(0);
                refilter();
                UiSounds.CLICK.play();
            }, "Очистить");
        }
        if (searchFocused && (time % 1.0F) < 0.55F) {
            Render2D.rect(ctx, caretX + 1.0F, iy + 6.0F, 1.0F, 10.0F, Theme.accentLight());
        }
        hit(sx, iy, sw, 22, () -> {
            if (!searchFocused) {
                searchFocused = true;
                UiSounds.CLICK.play(1.2F);
            }
        }, null);
    }

    private void iconButton(DrawContext ctx, float mouseX, float mouseY, float x, float y, Identifier icon, int hoverColor, Runnable action, String tip) {
        boolean hover = inside(mouseX, mouseY, x, y, 22, 22);
        Render2D.round(ctx, x, y, 22, 22, 7, hover ? Render2D.lerpColor(SURFACE, hoverColor, 0.22F) : SURFACE);
        Render2D.roundOutline(ctx, x, y, 22, 22, 7, 1, hover ? Render2D.alpha(hoverColor, 0.4F) : STROKE);
        Render2D.icon(ctx, icon, x + 6.0F, y + 6.0F, 10.0F, hover ? hoverColor : TEXT_DIM);
        hit(x, y, 22, 22, action, tip);
    }

    private void drawTabs(DrawContext ctx, float mouseX, float mouseY) {
        float h = 20.0F;
        int mode = 0;
        while (mode < 3 && tabsWidth(mode) > rightW) {
            mode++;
        }

        float x = rightX;
        float selX = 0.0F;
        float selW = 0.0F;
        float[] xs = new float[tabs.size()];
        float[] ws = new float[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            xs[i] = x;
            ws[i] = tabWidth(t, mode);
            if (t == tab) {
                selX = x;
                selW = ws[i];
            }
            x += ws[i] + 3.0F;
        }

        if (tabX.get() < 0.0F) {
            tabX.snap(selX);
            tabW.snap(selW);
        }
        float ax = tabX.target(selX).update(frameDt);
        float aw = tabW.target(selW).update(frameDt);

        Render2D.shadow(ctx, ax, tabsY + 2, aw, h, 7, 6, Theme.accent(0.3F));
        Render2D.roundV(ctx, ax, tabsY, aw, h, 7, Render2D.lerpColor(Theme.accent(), 0xFFFFFFFF, 0.1F), Theme.accentDark());

        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            boolean selected = t == tab;
            boolean hover = inside(mouseX, mouseY, xs[i], tabsY, ws[i], h);
            if (hover && !selected) {
                Render2D.round(ctx, xs[i], tabsY, ws[i], h, 7, 0x12FFFFFF);
            }
            int color = selected ? 0xFFFFFFFF : hover ? TEXT : TEXT_DIM;
            boolean label = showsLabel(t, mode);
            if (label) {
                float ix = xs[i] + 7.0F;
                Render2D.icon(ctx, t.icon, ix, tabsY + 5.5F, 9.0F, color);
                UiFont.draw(ctx, t.label, ix + 13.0F, tabsY + 6.0F, color);
                if (selected) {
                    String count = String.valueOf(count(t));
                    float bx = ix + 13.0F + UiFont.width(t.label) + 5.0F;
                    float bw = UiFont.width(count) + 7.0F;
                    Render2D.round(ctx, bx, tabsY + 4.0F, bw, 12, 5, 0x33FFFFFF);
                    UiFont.draw(ctx, count, bx + 3.5F, tabsY + 6.0F, 0xFFFFFFFF);
                }
            } else {
                Render2D.icon(ctx, t.icon, xs[i] + (ws[i] - 9.0F) / 2.0F, tabsY + 5.5F, 9.0F, color);
            }
            Tab target = t;
            hit(xs[i], tabsY, ws[i], h, () -> selectTab(target), label ? null : t.label + "  ·  " + count(t));
        }

        float lineY = tabsY + h + 3.0F;
        Render2D.gradientH(ctx, rightX, lineY, rightW / 2.0F, 1.0F, 0x00FFFFFF, 0x14FFFFFF);
        Render2D.gradientH(ctx, rightX + rightW / 2.0F, lineY, rightW / 2.0F, 1.0F, 0x14FFFFFF, 0x00FFFFFF);
    }

    private boolean showsLabel(Tab t, int mode) {
        return switch (mode) {
            case 0 -> true;
            case 1 -> t.category || t == tab;
            default -> t == tab;
        };
    }

    private float tabWidth(Tab t, int mode) {
        if (!showsLabel(t, mode)) {
            return 22.0F;
        }
        float w = 7.0F + 9.0F + 4.0F + UiFont.width(t.label) + 8.0F;
        if (t == tab) {
            w += UiFont.width(String.valueOf(count(t))) + 12.0F;
        }
        return w;
    }

    private float tabsWidth(int mode) {
        float w = 0.0F;
        for (Tab t : tabs) {
            w += tabWidth(t, mode) + 3.0F;
        }
        return w - 3.0F;
    }

    private int count(Tab t) {
        int n = 0;
        for (Cosmetic c : CosmeticRegistry.all()) {
            if (t.filter.test(c)) {
                n++;
            }
        }
        return n;
    }

    private void selectTab(Tab t) {
        if (t != tab) {
            tab = t;
            scroll.snap(0.0F);
            refilter();
            UiSounds.CATEGORY.play();
        }
    }

    private void drawGrid(DrawContext ctx, float mouseX, float mouseY) {
        if (shown.isEmpty()) {
            drawEmpty(ctx, mouseX, mouseY);
            return;
        }

        float offset = scroll.get();
        float seconds = Util.getMeasuringTimeMs() / 1000.0F;
        ctx.enableScissor((int) gridX - 8, (int) gridY, (int) (gridX + gridW) + 8, (int) (gridY + gridH));
        for (int i = 0; i < shown.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            float x = gridX + col * (cardW + GAP);
            float y = gridY + row * (cardH + GAP) - offset;
            if (y + cardH < gridY - 4 || y > gridY + gridH + 4) {
                continue;
            }
            drawCard(ctx, shown.get(i), x, y, mouseX, mouseY, seconds);
        }

        float topFade = MathHelper.clamp(offset / 12.0F, 0.0F, 1.0F);
        float bottomFade = MathHelper.clamp((maxScroll - offset) / 12.0F, 0.0F, 1.0F);
        if (topFade > 0) {
            Render2D.gradientV(ctx, gridX - 8, gridY, gridW + 16, 14, Render2D.alpha(0xF0131320, topFade), 0x00131320);
        }
        if (bottomFade > 0) {
            Render2D.gradientV(ctx, gridX - 8, gridY + gridH - 14, gridW + 16, 14, 0x000E0E15, Render2D.alpha(0xF00E0E15, bottomFade));
        }
        ctx.disableScissor();

        drawScrollbar(ctx, mouseX, mouseY);
    }

    private void drawCard(DrawContext ctx, Cosmetic cosmetic, float x, float y, float mouseX, float mouseY, float seconds) {
        CardState state = cards.computeIfAbsent(cosmetic.id(), id -> new CardState(cosmetic));
        boolean equipped = CosmeticState.isEquipped(cosmetic);
        boolean favorite = CosmeticState.isFavorite(cosmetic);
        boolean hover = hoveredCard == cosmetic;
        float h = state.hover.target(hover ? 1.0F : 0.0F).get();
        float e = state.equip.target(equipped ? 1.0F : 0.0F).get();
        int rarity = cosmetic.rarity().color;

        y -= h * 1.5F;
        state.yaw += ((UiSettings.spinCards ? 16.0F : 0.0F) + 70.0F * h) * frameDt;

        if (h > 0.01F) {
            Render2D.shadow(ctx, x, y + 3, cardW, cardH, 9, 8, Render2D.alpha(0x88000000, h));
        }
        if (e > 0.01F) {
            Render2D.shadow(ctx, x, y, cardW, cardH, 9, 7, Theme.accent(0.4F * e));
        }
        Render2D.round(ctx, x, y, cardW, cardH, 9, Render2D.lerpColor(SURFACE, SURFACE_HOVER, h));

        float px = x + 3.0F;
        float py = y + 3.0F;
        float pw = cardW - 6.0F;
        Render2D.roundV(ctx, px, py, pw, previewH, 7, Theme.WELL, Render2D.lerpColor(Theme.WELL, rarity, 0.16F + 0.08F * h));
        float pulse = cosmetic.rarity() == Rarity.LEGENDARY ? 0.75F + 0.25F * (float) Math.sin(seconds * 2.4F + cosmetic.id()) : 1.0F;
        Render2D.glow(ctx, px + pw / 2.0F, py + previewH - 6.0F, pw * 0.42F, 12.0F, Render2D.alpha(rarity, (0.30F + 0.2F * h) * pulse));
        Render2D.gradientH(ctx, px + 6.0F, py + previewH - 1.0F, pw / 2.0F - 6.0F, 1.0F, Render2D.alpha(rarity, 0.0F), Render2D.alpha(rarity, 0.8F));
        Render2D.gradientH(ctx, px + pw / 2.0F, py + previewH - 1.0F, pw / 2.0F - 6.0F, 1.0F, Render2D.alpha(rarity, 0.8F), Render2D.alpha(rarity, 0.0F));

        float grow = 1.0F + 0.07F * h;
        boolean sway = cosmetic.isCape() || cosmetic.type() == CosmeticType.WINGS;
        float yaw = sway ? state.baseYaw + (float) Math.sin(state.yaw * 0.035F) * 38.0F : state.baseYaw + state.yaw;
        ctx.enableScissor((int) px, (int) Math.max(py, gridY), (int) Math.ceil(px + pw), (int) Math.min(py + previewH, gridY + gridH));
        ModelPreview.draw(ctx, cosmetic, px + pw / 2.0F, py + previewH / 2.0F, pw * 0.82F * grow, previewH * 0.72F * grow,
            yaw, sway ? 38.0F : 0.0F, cosmetic.isCape() ? 6.0F : 14.0F, seconds);
        ctx.disableScissor();

        int outline = Render2D.lerpColor(Render2D.lerpColor(STROKE, 0x40FFFFFF, h), Theme.accent(0.88F), e);
        Render2D.roundOutline(ctx, x, y, cardW, cardH, 9, 1, outline);

        if (e > 0.01F) {
            float r = 6.5F * (0.6F + 0.4F * e);
            Render2D.circle(ctx, px + 11.5F, py + 11.5F, r, Theme.accent(e));
            Render2D.icon(ctx, Icons.CHECK, px + 8.0F, py + 8.0F, 7.0F, Render2D.alpha(0xFFFFFFFF, e));
        }

        float starSize = 10.0F;
        float sx = px + pw - starSize - 5.0F;
        float sy = py + 5.0F;
        boolean starHover = inside(mouseX, mouseY, sx - 3, sy - 3, starSize + 6, starSize + 6);
        if (favorite || h > 0.01F) {
            int starColor = favorite ? GOLD : Render2D.alpha(starHover ? TEXT : TEXT_MUTED, h);
            Render2D.icon(ctx, favorite ? Icons.STAR_FILLED : Icons.STAR, sx, sy, starSize, starColor);
        }

        if (h > 0.01F) {
            String action = equipped ? "Снять" : "Надеть";
            int pillColor = equipped ? 0xFFDC4B4B : Theme.accent();
            float pillW = UiFont.width(action) + 22.0F;
            float pillX = px + (pw - pillW) / 2.0F;
            float pillY = py + previewH - 19.0F + (1.0F - h) * 6.0F;
            Render2D.shadow(ctx, pillX, pillY, pillW, 15, 7.5F, 5, Render2D.alpha(pillColor, 0.45F * h));
            Render2D.round(ctx, pillX, pillY, pillW, 15, 7.5F, Render2D.alpha(pillColor, h));
            Render2D.icon(ctx, equipped ? Icons.CLOSE : Icons.CHECK, pillX + 6.5F, pillY + 4.0F, 7.0F, Render2D.alpha(0xFFFFFFFF, h));
            UiFont.draw(ctx, action, pillX + 16.0F, pillY + 4.0F, Render2D.alpha(0xFFFFFFFF, h));
        }

        float ty = py + previewH + 5.0F;
        UiFont.drawBold(ctx, UiFont.ellipsize(cosmetic.name(), cardW - 12.0F, true), x + 6.0F, ty, TEXT);
        Render2D.circle(ctx, x + 8.5F, ty + 15.5F, 2.5F, rarity);
        UiFont.draw(ctx, cosmetic.rarity().label, x + 14.0F, ty + 12.0F, Render2D.alpha(rarity, 0.95F));
        Render2D.icon(ctx, cosmetic.type().icon, x + cardW - 15.0F, ty + 11.5F, 9.0F, TEXT_MUTED);

        float top = Math.max(y, gridY);
        float bottom = Math.min(y + cardH, gridY + gridH);
        if (bottom > top) {
            hits.add(new Hit(x, top, cardW, bottom - top, () -> toggle(cosmetic), null, () -> toggleFavorite(cosmetic)));
            if (sy >= gridY && sy + starSize <= gridY + gridH) {
                hit(sx - 3, sy - 3, starSize + 6, starSize + 6, () -> toggleFavorite(cosmetic), favorite ? "Убрать из избранного" : "В избранное");
            }
        }
    }

    private float thumbHeight() {
        return Math.max(24.0F, gridH * gridH / (gridH + maxScroll));
    }

    private void drawScrollbar(DrawContext ctx, float mouseX, float mouseY) {
        if (maxScroll <= 0.0F) {
            return;
        }
        float trackX = gridX + gridW + 3.0F;
        float thumbH = thumbHeight();
        float thumbY = gridY + (gridH - thumbH) * (scroll.get() / maxScroll);
        boolean hover = draggingScrollbar || inside(mouseX, mouseY, trackX - 3, gridY, 9, gridH);
        Render2D.round(ctx, trackX, gridY, 3, gridH, 1.5F, 0x0CFFFFFF);
        Render2D.round(ctx, trackX, thumbY, 3, thumbH, 1.5F, hover ? Theme.accentLight() : 0x40FFFFFF);
    }

    private void drawEmpty(DrawContext ctx, float mouseX, float mouseY) {
        float cx = gridX + gridW / 2.0F;
        float cy = gridY + gridH / 2.0F - 22.0F;
        Identifier icon;
        String title;
        String hint;
        if (!query.isEmpty()) {
            icon = Icons.SEARCH_OFF;
            title = "Ничего не найдено";
            hint = tab == allTab ? "Попробуй другой запрос" : "В разделе «" + tab.label + "» нет совпадений";
        } else if (tab == favoritesTab) {
            icon = Icons.STAR;
            title = "В избранном пусто";
            hint = "Нажми ☆ или ПКМ по карточке";
        } else if (tab == equippedTab) {
            icon = Icons.HANGER;
            title = "Ничего не надето";
            hint = "Выбери что-нибудь в других разделах";
        } else {
            icon = Icons.SPARKLE;
            title = "Здесь пока пусто";
            hint = "";
        }
        Render2D.glow(ctx, cx, cy, 40, 40, Theme.accent(0.14F));
        Render2D.round(ctx, cx - 17, cy - 17, 34, 34, 11, SURFACE);
        Render2D.roundOutline(ctx, cx - 17, cy - 17, 34, 34, 11, 1, STROKE);
        Render2D.icon(ctx, icon, cx - 8, cy - 8, 16, Theme.accentLight());
        UiFont.drawBoldCentered(ctx, title, cx, cy + 26, TEXT);
        UiFont.drawCentered(ctx, hint, cx, cy + 39, TEXT_MUTED);

        if (!query.isEmpty() && tab != allTab) {
            int elsewhere = matches(allTab).size();
            if (elsewhere > 0) {
                String label = "Искать во всех  ·  " + elsewhere;
                float bw = UiFont.width(label) + 30.0F;
                float bx = cx - bw / 2.0F;
                float by = cy + 54.0F;
                boolean hover = inside(mouseX, mouseY, bx, by, bw, 20);
                Render2D.shadow(ctx, bx, by + 2, bw, 20, 7, 6, Theme.accent(hover ? 0.5F : 0.3F));
                Render2D.roundV(ctx, bx, by, bw, 20, 7, Render2D.lerpColor(Theme.accent(), 0xFFFFFFFF, hover ? 0.2F : 0.1F), Theme.accentDark());
                Render2D.icon(ctx, Icons.SEARCH, bx + 9.0F, by + 5.5F, 9.0F, 0xFFFFFFFF);
                UiFont.draw(ctx, label, bx + 22.0F, by + 6.0F, 0xFFFFFFFF);
                hit(bx, by, bw, 20, () -> selectTab(allTab), null);
            }
        }
    }

    private void drawSettings(DrawContext ctx, float mouseX, float mouseY) {
        float a = settingsAnim.get();
        if (a < 0.01F) {
            return;
        }
        if (settingsOpen) {
            hits.clear();
            hit(winX, winY, winW, winH, () -> settingsOpen = false, null);
        }

        float pw = Math.min(winW - 30.0F, 430.0F);
        float ph = 222.0F;
        float px = winX + (winW - pw) / 2.0F;
        float py = winY + (winH - ph) / 2.0F + (1.0F - a) * 10.0F;

        Render2D.round(ctx, winX, winY, winW, winH, 14.0F, Render2D.alpha(0xB8070710, a));
        Render2D.shadow(ctx, px, py + 6, pw, ph, 12, 22, Render2D.alpha(0xAA000000, a));
        Render2D.roundV(ctx, px, py, pw, ph, 12, Render2D.alpha(Theme.tint(0xFF1C1C28, 0.08F), a), Render2D.alpha(0xFF15151E, a));
        Render2D.roundOutline(ctx, px, py, pw, ph, 12, 1, Render2D.alpha(0x22FFFFFF, a));
        Render2D.glow(ctx, px + pw * 0.2F, py + 8, pw * 0.35F, 40, Theme.accent(0.12F * a));

        Render2D.roundV(ctx, px + 12, py + 11, 20, 20, 6, Render2D.alpha(Theme.accentLight(), a), Render2D.alpha(Theme.accentDark(), a));
        Render2D.icon(ctx, Icons.GEAR, px + 17, py + 16, 10, Render2D.alpha(0xFFFFFFFF, a));
        UiFont.drawBold(ctx, "Настройки меню", px + 39, py + 12, Render2D.alpha(TEXT, a));
        UiFont.draw(ctx, "Сохраняются автоматически", px + 39, py + 23, Render2D.alpha(TEXT_MUTED, a));
        float closeX = px + pw - 32;
        boolean closeHover = inside(mouseX, mouseY, closeX, py + 10, 22, 22);
        Render2D.round(ctx, closeX, py + 10, 22, 22, 7, Render2D.alpha(closeHover ? SURFACE_HOVER : SURFACE, a));
        Render2D.icon(ctx, Icons.CLOSE, closeX + 6, py + 16, 10, Render2D.alpha(closeHover ? TEXT : TEXT_DIM, a));
        if (settingsOpen) {
            hit(px, py, pw, ph, null, null);
            hit(closeX, py + 10, 22, 22, () -> {
                settingsOpen = false;
                UiSounds.CLICK.play();
            }, null);
        }

        float colW = (pw - 12 * 3) / 2.0F;
        float lx = px + 12;
        float rx = lx + colW + 12;
        float y = py + 44;

        sectionLabel(ctx, Icons.SCALE, "Масштаб меню", lx, y, a);
        String[] scaleLabels = new String[UiSettings.SCALES.length];
        for (int i = 0; i < scaleLabels.length; i++) {
            scaleLabels[i] = Math.round(UiSettings.SCALES[i] * 100) + "%";
        }
        segmented(ctx, mouseX, mouseY, lx, y + 14, colW, scaleLabels, UiSettings.scaleIndex, a, index -> {
            UiSettings.scaleIndex = index;
            UiSettings.save();
        });

        y += 46;
        sectionLabel(ctx, Icons.PALETTE, "Акцентный цвет", lx, y, a);
        UiSettings.Accent[] accents = UiSettings.Accent.values();
        float slot = colW / accents.length;
        for (int i = 0; i < accents.length; i++) {
            UiSettings.Accent accent = accents[i];
            float cx = lx + slot * i + slot / 2.0F;
            float cy = y + 25;
            boolean selected = UiSettings.accent == accent;
            boolean hover = inside(mouseX, mouseY, cx - 9, cy - 9, 18, 18);
            if (selected) {
                Render2D.glow(ctx, cx, cy, 14, 14, Render2D.alpha(accent.color, 0.55F * a));
                Render2D.circle(ctx, cx, cy, 9.0F, Render2D.alpha(0xFFFFFFFF, a));
            }
            Render2D.circle(ctx, cx, cy, selected ? 7.5F : hover ? 7.5F : 7.0F, Render2D.alpha(accent.color, a));
            if (selected) {
                Render2D.icon(ctx, Icons.CHECK, cx - 4, cy - 4, 8, Render2D.alpha(0xFFFFFFFF, a));
            }
            if (settingsOpen) {
                hit(cx - 9, cy - 9, 18, 18, () -> {
                    UiSettings.accent = accent;
                    UiSettings.save();
                    UiSounds.CLICK.play(1.25F);
                }, accent.label);
            }
        }

        y += 46;
        sectionLabel(ctx, Icons.FONT, "Шрифт", lx, y, a);
        UiSettings.Font[] fonts = UiSettings.Font.values();
        float fontW = (colW - 8) / 3.0F;
        for (int i = 0; i < fonts.length; i++) {
            UiSettings.Font font = fonts[i];
            float fx = lx + (i % 3) * (fontW + 4);
            float fy = y + 14 + (i / 3) * 22;
            boolean selected = UiSettings.font == font;
            boolean hover = inside(mouseX, mouseY, fx, fy, fontW, 18);
            if (selected) {
                Render2D.shadow(ctx, fx, fy + 1, fontW, 18, 6, 4, Theme.accent(0.35F * a));
                Render2D.roundV(ctx, fx, fy, fontW, 18, 6, Render2D.alpha(Render2D.lerpColor(Theme.accent(), 0xFFFFFFFF, 0.1F), a), Render2D.alpha(Theme.accentDark(), a));
            } else {
                Render2D.round(ctx, fx, fy, fontW, 18, 6, Render2D.alpha(hover ? SURFACE_HOVER : 0xFF111119, a));
                Render2D.roundOutline(ctx, fx, fy, fontW, 18, 6, 1, Render2D.alpha(STROKE, a));
            }
            Text sample = UiFont.text(font.label, font);
            int color = selected ? 0xFFFFFFFF : hover ? TEXT : TEXT_DIM;
            UiFont.draw(ctx, sample, fx + (fontW - UiFont.width(sample)) / 2.0F, fy + 5, Render2D.alpha(color, a));
            if (settingsOpen) {
                hit(fx, fy, fontW, 18, () -> {
                    if (UiSettings.font != font) {
                        UiSettings.font = font;
                        UiSettings.save();
                        UiSounds.CLICK.play(1.15F);
                    }
                }, null);
            }
        }

        float ty = py + 44;
        toggleRow(ctx, mouseX, mouseY, rx, ty, colW, Icons.VOLUME, "Звуки интерфейса", "sounds", UiSettings.sounds, a, v -> UiSettings.sounds = v);
        toggleRow(ctx, mouseX, mouseY, rx, ty + 32, colW, Icons.DROPLET, "Размытие фона", "blur", UiSettings.blur, a, v -> UiSettings.blur = v);
        toggleRow(ctx, mouseX, mouseY, rx, ty + 64, colW, Icons.ROTATE, "Вращение моделей", "spin", UiSettings.spinCards, a, v -> UiSettings.spinCards = v);

        String reset = "Сбросить всё";
        float bw = UiFont.width(reset) + 28;
        float bx = rx + colW - bw;
        float by = py + ph - 30;
        boolean resetHover = inside(mouseX, mouseY, bx, by, bw, 18);
        Render2D.round(ctx, bx, by, bw, 18, 6, Render2D.alpha(resetHover ? 0x33F87171 : SURFACE, a));
        Render2D.roundOutline(ctx, bx, by, bw, 18, 6, 1, Render2D.alpha(resetHover ? 0x66F87171 : STROKE, a));
        Render2D.icon(ctx, Icons.ROTATE, bx + 8, by + 4.5F, 9, Render2D.alpha(resetHover ? DANGER : TEXT_DIM, a));
        UiFont.draw(ctx, reset, bx + 21, by + 5, Render2D.alpha(resetHover ? DANGER : TEXT_DIM, a));
        if (settingsOpen) {
            hit(bx, by, bw, 18, () -> {
                UiSettings.reset();
                UiSounds.CLICK.play(0.8F);
                showToast("Настройки сброшены", Icons.ROTATE, TEXT_DIM);
            }, null);
        }
    }

    private void sectionLabel(DrawContext ctx, Identifier icon, String label, float x, float y, float a) {
        Render2D.icon(ctx, icon, x + 1, y, 9, Render2D.alpha(Theme.accentLight(), a));
        UiFont.draw(ctx, label, x + 14, y + 0.5F, Render2D.alpha(TEXT_DIM, a));
    }

    private void segmented(DrawContext ctx, float mouseX, float mouseY, float x, float y, float w, String[] labels, int selected, float a,
                           java.util.function.IntConsumer onSelect) {
        float h = 20;
        Render2D.round(ctx, x, y, w, h, 7, Render2D.alpha(0xFF111119, a));
        Render2D.roundOutline(ctx, x, y, w, h, 7, 1, Render2D.alpha(STROKE, a));
        float segW = (w - 4) / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float sx = x + 2 + segW * i;
            boolean isSelected = i == selected;
            boolean hover = inside(mouseX, mouseY, sx, y + 2, segW, h - 4);
            if (isSelected) {
                Render2D.shadow(ctx, sx, y + 3, segW, h - 4, 5, 4, Theme.accent(0.35F * a));
                Render2D.roundV(ctx, sx, y + 2, segW, h - 4, 5, Render2D.alpha(Render2D.lerpColor(Theme.accent(), 0xFFFFFFFF, 0.1F), a), Render2D.alpha(Theme.accentDark(), a));
            } else if (hover) {
                Render2D.round(ctx, sx, y + 2, segW, h - 4, 5, Render2D.alpha(0x14FFFFFF, a));
            }
            int color = isSelected ? 0xFFFFFFFF : hover ? TEXT : TEXT_DIM;
            String label = UiFont.ellipsize(labels[i], segW - 4, false);
            UiFont.drawCentered(ctx, label, sx + segW / 2.0F, y + 6, Render2D.alpha(color, a));
            if (settingsOpen) {
                int index = i;
                hit(sx, y + 2, segW, h - 4, () -> {
                    if (index != selected) {
                        onSelect.accept(index);
                        UiSounds.CLICK.play(1.15F);
                    }
                }, null);
            }
        }
    }

    private void toggleRow(DrawContext ctx, float mouseX, float mouseY, float x, float y, float w, Identifier icon, String label, String key,
                           boolean value, float a, java.util.function.Consumer<Boolean> setter) {
        float h = 26;
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        Render2D.round(ctx, x, y, w, h, 7, Render2D.alpha(hover ? SURFACE_HOVER : SURFACE, a));
        Render2D.roundOutline(ctx, x, y, w, h, 7, 1, Render2D.alpha(STROKE, a));
        Render2D.icon(ctx, icon, x + 8, y + 8.5F, 9, Render2D.alpha(value ? Theme.accentLight() : TEXT_MUTED, a));
        UiFont.draw(ctx, UiFont.ellipsize(label, w - 60, false), x + 22, y + 9, Render2D.alpha(TEXT, a));

        Anim knob = switches.computeIfAbsent(key, k -> new Anim(value ? 1.0F : 0.0F, 18.0F));
        float k = knob.target(value ? 1.0F : 0.0F).update(frameDt);
        float sw = 24;
        float sh = 13;
        float sx = x + w - sw - 8;
        float sy = y + (h - sh) / 2.0F;
        Render2D.round(ctx, sx, sy, sw, sh, sh / 2.0F, Render2D.alpha(Render2D.lerpColor(0xFF2E2E3E, Theme.accent(), k), a));
        Render2D.circle(ctx, sx + sh / 2.0F + (sw - sh) * k, sy + sh / 2.0F, sh / 2.0F - 2.0F, Render2D.alpha(0xFFFFFFFF, a));
        if (settingsOpen) {
            hit(x, y, w, h, () -> {
                setter.accept(!value);
                UiSettings.save();
                UiSounds.CLICK.play(value ? 0.9F : 1.2F);
            }, null);
        }
    }

    private void drawToast(DrawContext ctx) {
        if (toastTime < 0.0F || toast == null) {
            return;
        }
        float in = Anim.easeOutBack(toastTime / 0.3F);
        float out = 1.0F - MathHelper.clamp((toastTime - 1.8F) / 0.4F, 0.0F, 1.0F);
        float a = Math.min(MathHelper.clamp(toastTime / 0.15F, 0.0F, 1.0F), out);
        float w = UiFont.width(toast) + 32.0F;
        float h = 21.0F;
        float x = gridX + (gridW - w) / 2.0F;
        float y = winY + winH - PAD - h - 8.0F + (1.0F - in) * 10.0F;

        Render2D.shadow(ctx, x, y + 3, w, h, 10.5F, 10, Render2D.alpha(0xAA000000, a));
        Render2D.round(ctx, x, y, w, h, 10.5F, Render2D.alpha(0xF5232331, a));
        Render2D.roundOutline(ctx, x, y, w, h, 10.5F, 1, Render2D.alpha(toastColor, 0.5F * a));
        Render2D.icon(ctx, toastIcon, x + 9.0F, y + 6.0F, 9.0F, Render2D.alpha(toastColor, a));
        UiFont.draw(ctx, toast, x + 23.0F, y + 6.5F, Render2D.alpha(TEXT, a));
    }

    private void drawTooltip(DrawContext ctx, float mouseX, float mouseY) {
        Hit hit = hitAt(mouseX, mouseY);
        String text = hit != null ? hit.tooltip : null;
        if (text == null || draggingPreview || draggingScrollbar) {
            return;
        }
        float w = UiFont.width(text) + 12.0F;
        float h = 16.0F;
        float x = Math.min(mouseX + 10.0F, vw - w - 4.0F);
        float y = Math.max(4.0F, mouseY - h - 6.0F);
        Render2D.shadow(ctx, x, y + 2, w, h, 6, 6, 0x99000000);
        Render2D.round(ctx, x, y, w, h, 6, 0xF8262636);
        Render2D.roundOutline(ctx, x, y, w, h, 6, 1, 0x24FFFFFF);
        UiFont.draw(ctx, text, x + 6.0F, y + 4.0F, TEXT);
    }

    private void showToast(String text, Identifier icon, int color) {
        toast = text;
        toastIcon = icon;
        toastColor = color;
        toastTime = 0.0F;
    }

    private void toggle(Cosmetic cosmetic) {
        if (CosmeticState.toggle(cosmetic)) {
            UiSounds.EQUIP.play();
            showToast("Надето: " + cosmetic.name(), Icons.CHECK, Theme.accentLight());
        } else {
            UiSounds.UNEQUIP.play();
            showToast("Снято: " + cosmetic.name(), Icons.CLOSE, TEXT_DIM);
        }
        if (tab == equippedTab) {
            refilter();
        }
    }

    private void toggleFavorite(Cosmetic cosmetic) {
        CosmeticState.toggleFavorite(cosmetic);
        boolean favorite = CosmeticState.isFavorite(cosmetic);
        UiSounds.CLICK.play(favorite ? 1.3F : 0.9F);
        showToast(favorite ? "В избранном: " + cosmetic.name() : "Убрано из избранного", favorite ? Icons.STAR_FILLED : Icons.STAR, favorite ? GOLD : TEXT_DIM);
        if (tab == favoritesTab) {
            refilter();
        }
    }

    private List<Cosmetic> matches(Tab within) {
        String q = query.toString().trim().toLowerCase(Locale.ROOT);
        List<Cosmetic> result = new ArrayList<>();
        for (Cosmetic c : CosmeticRegistry.all()) {
            if (!within.filter.test(c)) {
                continue;
            }
            if (!q.isEmpty()
                && !c.name().toLowerCase(Locale.ROOT).contains(q)
                && !c.type().plural.toLowerCase(Locale.ROOT).contains(q)
                && !c.rarity().label.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            result.add(c);
        }
        return result;
    }

    private void refilter() {
        shown = matches(tab);
        if (width > 0) {
            scale();
            layout();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) {
            return true;
        }
        float mouseX = (float) click.x() / scale;
        float mouseY = (float) click.y() / scale;
        int button = click.button();
        Hit target = hitAt(mouseX, mouseY);
        if (settingsOpen) {
            if (target != null && target.action != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                target.action.run();
            }
            return true;
        }
        if (!inside(mouseX, mouseY, searchX(), headerY + 2.0F, searchW(), 22.0F)) {
            searchFocused = false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && maxScroll > 0.0F && inside(mouseX, mouseY, gridX + gridW, gridY, 9, gridH)) {
            draggingScrollbar = true;
            float thumbH = thumbHeight();
            float thumbY = gridY + (gridH - thumbH) * (scroll.get() / maxScroll);
            scrollbarGrab = mouseY - thumbY;
            if (scrollbarGrab < 0 || scrollbarGrab > thumbH) {
                scrollbarGrab = thumbH / 2.0F;
                dragScrollbar(mouseY);
            }
            return true;
        }

        if (target != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && target.action != null) {
                target.action.run();
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && target.rightAction != null) {
                target.rightAction.run();
                return true;
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && inside(mouseX, mouseY, leftX, previewTop, leftW, previewBottom - previewTop)) {
            draggingPreview = true;
            resettingYaw = false;
            yawVelocity = 0.0F;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingPreview) {
            float d = (float) (deltaX / scale) * 1.6F;
            previewYaw += d;
            yawVelocity = d * 60.0F;
            dragged = true;
            return true;
        }
        if (draggingScrollbar) {
            dragScrollbar((float) click.y() / scale);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private void dragScrollbar(float mouseY) {
        float thumbH = thumbHeight();
        float t = (mouseY - scrollbarGrab - gridY) / Math.max(1.0F, gridH - thumbH);
        scroll.snap(MathHelper.clamp(t, 0.0F, 1.0F) * maxScroll);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingPreview) {
            draggingPreview = false;
            if (Math.abs(yawVelocity) < 40.0F) {
                yawVelocity = 0.0F;
            }
        }
        draggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double horizontalAmount, double verticalAmount) {
        if (settingsOpen || closing) {
            return true;
        }
        float mouseX = (float) rawX / scale;
        float mouseY = (float) rawY / scale;
        if (inside(mouseX, mouseY, leftX, leftY, leftW, leftH)) {
            zoom.target(MathHelper.clamp(zoom.target() + (float) verticalAmount * 0.12F, 0.7F, 1.7F));
            return true;
        }
        if (inside(mouseX, mouseY, rightX, gridY, rightW, gridH)) {
            scroll.target(MathHelper.clamp(scroll.target() - (float) verticalAmount * (cardH + GAP) * 0.6F, 0.0F, maxScroll));
            return true;
        }
        return super.mouseScrolled(rawX, rawY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsOpen) {
                settingsOpen = false;
            } else if (searchFocused) {
                searchFocused = false;
            } else {
                close();
            }
            return true;
        }
        if (settingsOpen) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F && input.hasCtrlOrCmd()) {
            searchFocused = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            int index = tabs.indexOf(tab) + (input.hasShift() ? tabs.size() - 1 : 1);
            selectTab(tabs.get(index % tabs.size()));
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                if (input.hasCtrlOrCmd()) {
                    String trimmed = query.toString().stripTrailing();
                    int cut = trimmed.lastIndexOf(' ');
                    query.setLength(cut < 0 ? 0 : cut + 1);
                } else {
                    query.setLength(query.length() - 1);
                }
                refilter();
                UiSounds.TYPING.play(0.9F);
            } else if (keyCode == GLFW.GLFW_KEY_V && input.hasCtrlOrCmd()) {
                String clip = client.keyboard.getClipboard().replaceAll("[\\r\\n]", " ");
                query.append(clip, 0, Math.max(0, Math.min(clip.length(), 40 - query.length())));
                refilter();
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
            }
            return true;
        }
        if (LowcosmeticClient.openKey().matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (closing || settingsOpen || Character.isISOControl(chr)) {
            return false;
        }
        if (!searchFocused) {
            if (chr == ' ') {
                return false;
            }
            searchFocused = true;
        }
        if (query.length() < 40) {
            query.append(chr);
            refilter();
            UiSounds.TYPING.play(0.95F + (float) Math.random() * 0.15F);
        }
        return true;
    }

    private Cosmetic cardAt(float mouseX, float mouseY) {
        if (!inside(mouseX, mouseY, gridX, gridY, gridW, gridH)) {
            return null;
        }
        float lx = mouseX - gridX;
        float ly = mouseY - gridY + scroll.get();
        int col = (int) (lx / (cardW + GAP));
        int row = (int) (ly / (cardH + GAP));
        if (col < 0 || col >= cols || row < 0) {
            return null;
        }
        if (lx - col * (cardW + GAP) > cardW || ly - row * (cardH + GAP) > cardH) {
            return null;
        }
        int index = row * cols + col;
        return index < shown.size() ? shown.get(index) : null;
    }

    private void hit(float x, float y, float w, float h, Runnable action, String tooltip) {
        hits.add(new Hit(x, y, w, h, action, tooltip, null));
    }

    private Hit hitAt(float mouseX, float mouseY) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(mouseX, mouseY)) {
                return hits.get(i);
            }
        }
        return null;
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static String plural(int n, String one, String few, String many) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return many;
        }
        if (mod10 == 1) {
            return one;
        }
        return mod10 >= 2 && mod10 <= 4 ? few : many;
    }

    private record Tab(String label, Identifier icon, Predicate<Cosmetic> filter, boolean category) {
    }

    private record Hit(float x, float y, float w, float h, Runnable action, String tooltip, Runnable rightAction) {
        boolean contains(float mx, float my) {
            return inside(mx, my, x, y, w, h);
        }
    }

    private static final class CardState {
        final Anim hover = new Anim(0.0F, 16.0F);
        final Anim equip;
        final float baseYaw;
        float yaw;

        CardState(Cosmetic cosmetic) {
            this.equip = new Anim(CosmeticState.isEquipped(cosmetic) ? 1.0F : 0.0F, 14.0F);
            this.baseYaw = switch (cosmetic.type()) {
                case HAT -> 205.0F;
                case PET -> 215.0F;
                case WINGS -> 0.0F;
                default -> 30.0F;
            };
        }
    }
}
