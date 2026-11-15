package com.lowcode.svintusclient.client.gui;

import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticRegistry;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticState;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticType;
import com.lowcode.lowcosmetic.client.cosmetic.Rarity;
import com.lowcode.lowcosmetic.client.gui.CosmeticScreen;
import com.lowcode.lowcosmetic.client.gui.Icons;
import com.lowcode.lowcosmetic.client.gui.UiSounds;
import com.lowcode.lowcosmetic.client.render.ModelPreview;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.cosmetics.CosmeticsModule;
import com.lowcode.svintusclient.client.module.impl.render.CustomSwordsModule;
import com.lowcode.svintusclient.client.module.impl.render.ItemModelsModule;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.util.Anim;
import com.lowcode.svintusclient.client.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class CosmeticsCatalogPanel {

    public interface CatalogItem {
        String id();
        String name();
        Rarity rarity();
        Identifier icon();
        String typeLabel();
        boolean isEquipped();
        boolean isFavorite();
        boolean toggleEquip();
        void toggleFavorite();
        void renderPreview(DrawContext ctx, float px, float py, float pw, float ph, float grow, float yaw, float seconds, float gridY, float gridH);
    }

    private static final Set<String> FAVORITE_SWORDS = new HashSet<>();
    private static final Set<String> FAVORITE_TOTEMS = new HashSet<>();

    public static final class CosmeticEntry implements CatalogItem {
        private final Cosmetic cosmetic;

        public CosmeticEntry(Cosmetic cosmetic) {
            this.cosmetic = cosmetic;
        }

        @Override public String id() { return "cosmetic_" + cosmetic.id(); }
        @Override public String name() { return cosmetic.name(); }
        @Override public Rarity rarity() { return cosmetic.rarity(); }
        @Override public Identifier icon() { return cosmetic.type().icon; }
        @Override public String typeLabel() { return cosmetic.type().plural; }
        @Override public boolean isEquipped() { return CosmeticState.isEquipped(cosmetic); }
        @Override public boolean isFavorite() { return CosmeticState.isFavorite(cosmetic); }
        @Override public boolean toggleEquip() { return CosmeticState.toggle(cosmetic); }
        @Override public void toggleFavorite() { CosmeticState.toggleFavorite(cosmetic); }

        @Override
        public void renderPreview(DrawContext ctx, float px, float py, float pw, float ph, float grow, float yaw, float seconds, float gridY, float gridH) {
            boolean sway = cosmetic.type() == CosmeticType.WINGS;
            float baseYaw = switch (cosmetic.type()) {
                case HAT -> 205.0f;
                case PET -> 215.0f;
                case WINGS -> 0.0f;
                default -> 30.0f;
            };
            float y = sway ? baseYaw + (float) Math.sin(yaw * 0.05f) * 36f : baseYaw + yaw;
            float p = sway ? 10.0f : (cosmetic.type() == CosmeticType.HAT ? 14.0f : 8.0f);
            ModelPreview.draw(ctx, cosmetic, px + pw / 2f, py + ph / 2f, pw * 0.88f * grow, ph * 0.82f * grow,
                    y, sway ? 38.0f : 0.0f, p, seconds);
        }
    }

    public static final class SwordEntry implements CatalogItem {
        private final String swordName;
        private final ItemStack stack;
        private final Rarity rarity;

        public SwordEntry(String swordName) {
            this.swordName = swordName;
            this.stack = new ItemStack(Items.DIAMOND_SWORD);
            this.stack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new CustomModelDataComponent(List.of(), List.of(), List.of(swordName), List.of()));
            if (swordName.contains("Katana") || swordName.contains("Divine") || swordName.contains("Mythic") || swordName.contains("God")) {
                this.rarity = Rarity.LEGENDARY;
            } else if (swordName.contains("Cyber") || swordName.contains("Soul") || swordName.contains("Blade")) {
                this.rarity = Rarity.EPIC;
            } else {
                this.rarity = Rarity.RARE;
            }
        }

        @Override public String id() { return "sword_" + swordName; }
        @Override public String name() { return swordName; }
        @Override public Rarity rarity() { return rarity; }
        @Override public Identifier icon() { return Identifier.of("doxaiz", "textures/ui/categories/combat.png"); }
        @Override public String typeLabel() { return "Мечи"; }

        @Override
        public boolean isEquipped() {
            CustomSwordsModule swords = ModuleManager.get(CustomSwordsModule.class);
            return swords != null && swords.isEnabled() && swordName.equalsIgnoreCase(swords.getSelectedWeapon());
        }

        @Override
        public boolean isFavorite() {
            return FAVORITE_SWORDS.contains(swordName);
        }

        @Override
        public boolean toggleEquip() {
            CustomSwordsModule swords = ModuleManager.get(CustomSwordsModule.class);
            if (swords == null) return false;
            if (isEquipped()) {
                swords.swordModel.set("Стандарт");
                return false;
            } else {
                swords.setEnabled(true);
                swords.swordModel.set(swordName);
                return true;
            }
        }

        @Override
        public void toggleFavorite() {
            if (FAVORITE_SWORDS.contains(swordName)) FAVORITE_SWORDS.remove(swordName);
            else FAVORITE_SWORDS.add(swordName);
        }

        @Override
        public void renderPreview(DrawContext ctx, float px, float py, float pw, float ph, float grow, float yaw, float seconds, float gridY, float gridH) {
            float top = Math.max(py, gridY);
            float bot = Math.min(py + ph, gridY + gridH);
            if (bot <= top) return;
            Render2D.pushScissor(ctx, px, top, pw, bot - top);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(px + pw / 2f, py + ph / 2f);
            ctx.getMatrices().scale(2.5f * grow, 2.5f * grow);
            ctx.drawItemWithoutEntity(stack, -8, -8);
            ctx.getMatrices().popMatrix();
            Render2D.popScissor(ctx);
        }
    }

    public static final class TotemEntry implements CatalogItem {
        private final String totemName;
        private final ItemStack stack;
        private final Rarity rarity;

        public TotemEntry(String totemName) {
            this.totemName = totemName;
            this.stack = new ItemStack(Items.TOTEM_OF_UNDYING);
            this.stack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new CustomModelDataComponent(List.of(), List.of(), List.of(totemName), List.of()));
            if (totemName.contains("Anime") || totemName.contains("Rimuru") || totemName.contains("Shape")) {
                this.rarity = Rarity.LEGENDARY;
            } else {
                this.rarity = Rarity.EPIC;
            }
        }

        @Override public String id() { return "totem_" + totemName; }
        @Override public String name() { return totemName; }
        @Override public Rarity rarity() { return rarity; }
        @Override public Identifier icon() { return Identifier.of("minecraft", "textures/item/totem_of_undying.png"); }
        @Override public String typeLabel() { return "Тотемы"; }

        @Override
        public boolean isEquipped() {
            ItemModelsModule totems = ModuleManager.get(ItemModelsModule.class);
            return totems != null && totems.isEnabled() && totemName.equalsIgnoreCase(totems.getSelectedTotem());
        }

        @Override
        public boolean isFavorite() {
            return FAVORITE_TOTEMS.contains(totemName);
        }

        @Override
        public boolean toggleEquip() {
            ItemModelsModule totems = ModuleManager.get(ItemModelsModule.class);
            if (totems == null) return false;
            if (isEquipped()) {
                totems.totemStyle.set("Ванильный");
                return false;
            } else {
                totems.setEnabled(true);
                totems.totemStyle.set(totemName);
                return true;
            }
        }

        @Override
        public void toggleFavorite() {
            if (FAVORITE_TOTEMS.contains(totemName)) FAVORITE_TOTEMS.remove(totemName);
            else FAVORITE_TOTEMS.add(totemName);
        }

        @Override
        public void renderPreview(DrawContext ctx, float px, float py, float pw, float ph, float grow, float yaw, float seconds, float gridY, float gridH) {
            float top = Math.max(py, gridY);
            float bot = Math.min(py + ph, gridY + gridH);
            if (bot <= top) return;
            Render2D.pushScissor(ctx, px, top, pw, bot - top);
            String cleanTotem = (totemName.equalsIgnoreCase("Fury") || totemName.equalsIgnoreCase("Furry")) ? "furry" : totemName.toLowerCase(Locale.ROOT);
            Identifier tex = Identifier.of("shape", "textures/item/custom_totem_" + cleanTotem + ".png");
            float drawSize = 46f * grow;
            float drawX = px + (pw - drawSize) / 2f;
            float drawY = py + (ph - drawSize) / 2f - 2f;
            if (totemName.equalsIgnoreCase("Furry") || totemName.equalsIgnoreCase("Fury")) {
                int frame = (int) ((seconds * 10f) % 23);
                ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, tex, (int)drawX, (int)drawY, 0f, (float)(frame * 256), (int)drawSize, (int)drawSize, 256, 5888, 0xFFFFFFFF);
            } else {
                com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, tex, drawX, drawY, drawSize, 0xFFFFFFFF);
            }
            Render2D.popScissor(ctx);
        }
    }

    public record SubTab(String title, Identifier icon, Predicate<CatalogItem> filter) {}

    private final List<CatalogItem> allItems = new ArrayList<>();
    private final List<SubTab> tabs = new ArrayList<>();

    private int activeTabIdx = 0;
    private final Anim tabIndicator = new Anim(16f);
    private boolean tabIndicatorPlaced;
    private final Anim scroll = new Anim(16f);
    private float contentH = 0f;

    // 3D Player Preview State
    private float previewYaw = 0f;
    private float previewPitch = 0f;
    private float previewZoom = 1.0f;
    private boolean draggingPreview = false;
    private float dragStartMouseX = 0f;
    private float dragStartMouseY = 0f;
    private float dragStartYaw = 0f;
    private float dragStartPitch = 0f;
    private float lastPrevX = 0f, lastPrevY = 0f, lastPrevW = 0f, lastPrevH = 0f;
    private Cosmetic selectedPet = null;
    private String selectedSword = null;
    private String selectedTotem = null;

    private static class CardHoverState {
        final Anim hover = new Anim(14f);
        float yaw = 0f;
    }

    private final Map<String, CardHoverState> cardStates = new HashMap<>();
    private final List<ClickableCard> renderedCards = new ArrayList<>();

    private record ClickableCard(CatalogItem item, float x, float y, float w, float h, float starX, float starY, float starSize) {}

    public CosmeticsCatalogPanel() {
        initItems();
    }

    private void initItems() {
        allItems.clear();
        // 1. Standard Geo cosmetics
        for (Cosmetic c : CosmeticRegistry.all()) {
            allItems.add(new CosmeticEntry(c));
        }
        // 2. Custom Swords
        for (String w : CustomSwordsModule.WEAPONS) {
            if (!w.equalsIgnoreCase("Стандарт") && !w.equalsIgnoreCase("Ванильные")) {
                allItems.add(new SwordEntry(w));
            }
        }
        // 3. Custom Totems
        for (String t : ItemModelsModule.TOTEMS) {
            if (!t.equalsIgnoreCase("Ванильный")) {
                allItems.add(new TotemEntry(t));
            }
        }

        // Subtabs
        tabs.clear();
        tabs.add(new SubTab("Все", Icons.ALL, item -> true));
        tabs.add(new SubTab("Крылья", Icons.WINGS, item -> item instanceof CosmeticEntry ce && ce.cosmetic.type() == CosmeticType.WINGS));
        tabs.add(new SubTab("Шляпы", Icons.HAT, item -> item instanceof CosmeticEntry ce && ce.cosmetic.type() == CosmeticType.HAT));
        tabs.add(new SubTab("Одежда", Icons.BODYWEAR, item -> item instanceof CosmeticEntry ce && ce.cosmetic.type() == CosmeticType.BODYWEAR));
        tabs.add(new SubTab("Питомцы", Icons.PET, item -> item instanceof CosmeticEntry ce && ce.cosmetic.type() == CosmeticType.PET));
        tabs.add(new SubTab("Мечи", Identifier.of("doxaiz", "textures/ui/categories/combat.png"), item -> item instanceof SwordEntry));
        tabs.add(new SubTab("Тотемы", Identifier.of("minecraft", "textures/item/totem_of_undying.png"), item -> item instanceof TotemEntry));
        tabs.add(new SubTab("Надето", Icons.EQUIPPED, CatalogItem::isEquipped));
        tabs.add(new SubTab("Избранное", Icons.STAR, CatalogItem::isFavorite));
    }

    public void render(DrawContext ctx, float x, float y, float w, float h, float mx, float my, boolean inBody, String searchQuery) {
        renderedCards.clear();

        // 1. Determine Layout (Left: 3D Player Preview, Right: Catalog Grid)
        boolean hasPreview = w >= 340f;
        float prevW = hasPreview ? Math.min(170f, Math.max(140f, w * 0.28f)) : 0f;
        float catalogX = hasPreview ? (x + prevW + 8f) : x;
        float catalogW = hasPreview ? (w - prevW - 8f) : w;

        if (hasPreview) {
            lastPrevX = x;
            lastPrevY = y;
            lastPrevW = prevW;
            lastPrevH = h;
            drawPlayerPreview(ctx, x, y, prevW, h, mx, my, inBody);
        } else {
            lastPrevW = 0f;
        }

        // 2. Draw Subcategory Tabs Bar
        float tabH = 22f;
        float gap = 4f;
        drawTabsBar(ctx, catalogX, y, catalogW, tabH, mx, my, inBody);

        // 3. Grid bounds
        float gridY = y + tabH + gap + 4f;
        float gridH = h - (tabH + gap + 4f);

        // 4. Filter items
        SubTab currentTab = tabs.get(activeTabIdx);
        List<CatalogItem> list = allItems.stream()
                .filter(currentTab.filter())
                .filter(item -> {
                    if (searchQuery == null || searchQuery.isBlank()) return true;
                    String q = searchQuery.toLowerCase(Locale.ROOT).trim();
                    return item.name().toLowerCase(Locale.ROOT).contains(q)
                            || item.typeLabel().toLowerCase(Locale.ROOT).contains(q);
                })
                .toList();

        if (list.isEmpty()) {
            contentH = 0f;
            drawEmptyState(ctx, catalogX, gridY, catalogW, gridH);
            return;
        }

        // 5. Calculate Grid Layout
        float cardGap = 6f;
        int cols = Math.max(2, Math.min(4, (int) ((catalogW - 4f) / 102f)));
        float cardW = (catalogW - (cols - 1) * cardGap) / cols;
        float cardH = 138f;
        int rows = (int) Math.ceil((double) list.size() / cols);
        contentH = rows * (cardH + cardGap);

        float maxScroll = Math.max(0f, contentH - gridH);
        if (scroll.target() > maxScroll) scroll.to(maxScroll);

        Render2D.pushScissor(ctx, catalogX - 2f, gridY, catalogW + 4f, gridH);

        float topY = gridY - scroll.get();
        float seconds = (System.currentTimeMillis() % 100000L) / 1000f;

        for (int i = 0; i < list.size(); i++) {
            CatalogItem item = list.get(i);
            int col = i % cols;
            int row = i / cols;

            float cx = catalogX + col * (cardW + cardGap);
            float cy = topY + row * (cardH + cardGap);

            // Scissor cull
            if (cy + cardH < gridY || cy > gridY + gridH) continue;

            drawCard(ctx, item, cx, cy, cardW, cardH, mx, my, inBody, seconds, gridY, gridH);
        }

        Render2D.popScissor(ctx);

        // 6. Scrollbar
        if (maxScroll > 0.5f) {
            float trackH = gridH - 8f;
            float thumbH = Math.max(18f, trackH * gridH / contentH);
            float thumbY = gridY + 4f + (trackH - thumbH) * (scroll.get() / maxScroll);
            Render2D.rect(ctx, catalogX + catalogW - 3f, thumbY, 2.5f, thumbH, 1.25f, 0x30FFFFFF);
        }
    }

    private void drawPlayerPreview(DrawContext ctx, float x, float y, float w, float h, float mx, float my, boolean inBody) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Background panel matching 3D Wardrobe
        Render2D.rect(ctx, x, y, w, h, 8f, 0xFF14141E);
        Render2D.outline(ctx, x, y, w, h, 8f, 0.9f, 0x22FFFFFF);

        float cx = x + w / 2f;
        float headerY = y + 8f;

        // Top drag hint: ↔ Крути мышкой & reset button
        com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, Icons.ROTATE, x + 8f, headerY + 1f, 8f, 0x88FFFFFF);
        Fonts.REGULAR.drawCentered(ctx, "Крути мышкой", x + 19f, headerY + 4f, 5.0f, 0x88FFFFFF);

        // Reset icon on top right
        float resetX = x + w - 16f;
        boolean resetHover = inBody && Widgets.inside(mx, my, resetX - 2f, headerY - 2f, 13f, 13f);
        com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, Icons.ROTATE, resetX, headerY, 9f, resetHover ? Theme.TEXT : Theme.TEXT_MUTE);

        // Calculate preview drawing area
        float btnH = 15f;
        float btnY = y + h - btnH - 6f;
        float badgeH = 12f;
        float badgeY = btnY - badgeH - 5f;
        float nameY = badgeY - 11f;
        float feetY = nameY - 8f;
        float topLimit = headerY + 16f;
        float availH = Math.max(40f, feetY - topLimit);
        float availW = Math.max(40f, w - 16f);

        // Glow ellipse under feet
        com.lowcode.lowcosmetic.client.gui.Render2D.glow(ctx, cx, feetY + 1f, w * 0.45f, 8f, 0x77000000);
        com.lowcode.lowcosmetic.client.gui.Render2D.glow(ctx, cx, feetY + 1f, w * 0.35f, 5f, Theme.accent(0.35f));

        String displayName;
        String badgeLabel;
        float seconds = (System.currentTimeMillis() % 100000L) / 1000f;

        if (activeTabIdx == 4) {
            // --- ПИТОМЦЫ: Показываем 3D модель питомца вместо игрока ---
            Cosmetic pet = selectedPet;
            if (pet == null) pet = CosmeticState.equipped(CosmeticType.PET);
            if (pet == null) {
                for (CatalogItem it : allItems) {
                    if (it instanceof CosmeticEntry ce && ce.cosmetic.type() == CosmeticType.PET) {
                        pet = ce.cosmetic;
                        break;
                    }
                }
            }
            if (pet != null) {
                float petBox = Math.min(availH, w - 20f) * 0.82f * previewZoom;
                float petCenterY = (topLimit + feetY) / 2.0f;
                ModelPreview.draw(ctx, pet, cx, petCenterY, petBox, petBox, previewYaw, 0f, previewPitch, seconds);
                displayName = pet.name();
                badgeLabel = pet.rarity().label + " питомец";
            } else {
                displayName = "Питомец";
                badgeLabel = "Не выбран";
            }
        } else if (activeTabIdx == 6) {
            // --- ТОТЕМЫ: Показываем модель тотема вместо игрока ---
            String totem = selectedTotem;
            if (totem == null) {
                ItemModelsModule totemsMod = ModuleManager.get(ItemModelsModule.class);
                if (totemsMod != null && totemsMod.isEnabled() && !totemsMod.getSelectedTotem().equalsIgnoreCase("Ванильный")) {
                    totem = totemsMod.getSelectedTotem();
                }
            }
            if (totem == null) totem = "Anime_Alisa";

            float totemBox = Math.min(availH, w - 24f) * 0.78f * previewZoom;
            float totemX = cx - totemBox / 2f;
            float totemY = (topLimit + feetY - totemBox) / 2.0f;
            String cleanTotem = (totem.equalsIgnoreCase("Fury") || totem.equalsIgnoreCase("Furry")) ? "furry" : totem.toLowerCase(Locale.ROOT);
            Identifier totemTex = Identifier.of("shape", "textures/item/custom_totem_" + cleanTotem + ".png");
            if (totem.equalsIgnoreCase("Furry") || totem.equalsIgnoreCase("Fury")) {
                int frame = (int) ((seconds * 10f) % 23);
                ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, totemTex, (int) totemX, (int) totemY, 0f, (float)(frame * 256), (int) totemBox, (int) totemBox, 256, 5888, 0xFFFFFFFF);
            } else {
                com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, totemTex, totemX, totemY, totemBox, 0xFFFFFFFF);
            }
            displayName = "Тотем " + totem;
            badgeLabel = "3D Модель тотема";
        } else if (activeTabIdx == 5) {
            // --- МЕЧИ: Показываем персонажа с выбранным мечом в руках ---
            String sword = selectedSword;
            if (sword == null) {
                CustomSwordsModule swordsMod = ModuleManager.get(CustomSwordsModule.class);
                if (swordsMod != null && swordsMod.isEnabled() && !swordsMod.swordModel.get().equalsIgnoreCase("Ванильные")) {
                    sword = swordsMod.swordModel.get();
                }
            }
            if (sword == null) sword = "Катана";

            ItemStack origHand = player.getMainHandStack();
            ItemStack swordStack = new ItemStack(Items.NETHERITE_SWORD);
            swordStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(sword), List.of()));
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, swordStack);

            float centerY = (topLimit + feetY) / 2.0f;
            float playerH = availH * 0.84f * previewZoom;
            float size = playerH / 1.8f;

            Matrix3x2f m = new Matrix3x2f(ctx.getMatrices());
            Vector2f center = m.transformPosition(cx, centerY, new Vector2f());
            float k = (float) Math.hypot(m.m00, m.m01);
            if (k <= 0.001f) k = 1.0f;

            float halfBoxW = (w / 2.0f) * k;
            float halfBoxH = (availH / 2.0f) * k;

            int x1 = (int) Math.floor(center.x - halfBoxW);
            int y1 = (int) Math.floor(center.y - halfBoxH);
            int x2 = (int) Math.ceil(center.x + halfBoxW);
            int y2 = (int) Math.ceil(center.y + halfBoxH);

            CosmeticState.setForceVisible(true);
            try {
                EntityRenderManager dispatcher = mc.getEntityRenderDispatcher();
                @SuppressWarnings("unchecked")
                EntityRenderer<ClientPlayerEntity, ?> renderer =
                        (EntityRenderer<ClientPlayerEntity, ?>) dispatcher.getRenderer(player);
                EntityRenderState renderState = renderer.getAndUpdateRenderState(player, 1.0f);
                renderState.light = 15728880;
                renderState.shadowPieces.clear();
                renderState.shadowRadius = 0.0f;
                renderState.outlineColor = 0;

                if (renderState instanceof LivingEntityRenderState livingState) {
                    livingState.bodyYaw = 180.0f;
                    livingState.relativeHeadYaw = 0f;
                    livingState.pitch = 0f;
                    if (livingState.baseScale != 0f) {
                        livingState.width /= livingState.baseScale;
                        livingState.height /= livingState.baseScale;
                        livingState.baseScale = 1.0f;
                    }
                }

                Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
                rot.rotateY(previewYaw * 0.017453292f);
                rot.rotateX(previewPitch * 0.017453292f);

                Quaternionf pitchRot = new Quaternionf().rotateX(previewPitch * 0.017453292f);
                Vector3f translation = new Vector3f(0.0f, renderState.height / 2.0f, 0.0f);

                int entityScale = (int) Math.max(1, (size * k / Math.max(0.1f, player.getScale())));
                ctx.addEntity(renderState, (float) entityScale, translation, rot, pitchRot, x1, y1, x2, y2);
            } catch (Throwable ignored) {
            } finally {
                CosmeticState.setForceVisible(false);
                player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, origHand);
            }
            displayName = player.getName().getString();
            badgeLabel = "С мечом " + sword;
        } else {
            // --- ВСЕ / КРЫЛЬЯ / ШЛЯПЫ / ОДЕЖДА / НАДЕТО: 3D модель игрока с надетой косметикой ---
            float centerY = (topLimit + feetY) / 2.0f;
            float playerH = availH * 0.84f * previewZoom;
            float size = playerH / 1.8f;

            Matrix3x2f m = new Matrix3x2f(ctx.getMatrices());
            Vector2f center = m.transformPosition(cx, centerY, new Vector2f());
            float k = (float) Math.hypot(m.m00, m.m01);
            if (k <= 0.001f) k = 1.0f;

            float halfBoxW = (w / 2.0f) * k;
            float halfBoxH = (availH / 2.0f) * k;

            int x1 = (int) Math.floor(center.x - halfBoxW);
            int y1 = (int) Math.floor(center.y - halfBoxH);
            int x2 = (int) Math.ceil(center.x + halfBoxW);
            int y2 = (int) Math.ceil(center.y + halfBoxH);

            CosmeticState.setForceVisible(true);
            try {
                EntityRenderManager dispatcher = mc.getEntityRenderDispatcher();
                @SuppressWarnings("unchecked")
                EntityRenderer<ClientPlayerEntity, ?> renderer =
                        (EntityRenderer<ClientPlayerEntity, ?>) dispatcher.getRenderer(player);
                EntityRenderState renderState = renderer.getAndUpdateRenderState(player, 1.0f);
                renderState.light = 15728880;
                renderState.shadowPieces.clear();
                renderState.shadowRadius = 0.0f;
                renderState.outlineColor = 0;

                if (renderState instanceof LivingEntityRenderState livingState) {
                    livingState.bodyYaw = 180.0f;
                    livingState.relativeHeadYaw = 0f;
                    livingState.pitch = 0f;
                    if (livingState.baseScale != 0f) {
                        livingState.width /= livingState.baseScale;
                        livingState.height /= livingState.baseScale;
                        livingState.baseScale = 1.0f;
                    }
                }

                Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
                rot.rotateY(previewYaw * 0.017453292f);
                rot.rotateX(previewPitch * 0.017453292f);

                Quaternionf pitchRot = new Quaternionf().rotateX(previewPitch * 0.017453292f);
                Vector3f translation = new Vector3f(0.0f, renderState.height / 2.0f, 0.0f);

                int entityScale = (int) Math.max(1, (size * k / Math.max(0.1f, player.getScale())));
                ctx.addEntity(renderState, (float) entityScale, translation, rot, pitchRot, x1, y1, x2, y2);
            } catch (Throwable ignored) {
            } finally {
                CosmeticState.setForceVisible(false);
            }
            displayName = player.getName().getString();
            int equippedCount = (int) allItems.stream().filter(CatalogItem::isEquipped).count();
            badgeLabel = equippedCount + " надето";
        }

        // Name below object
        Fonts.SEMIBOLD.drawCentered(ctx, Fonts.SEMIBOLD.trim(displayName, 6.5f, w - 16f), cx, nameY + 3.5f, 6.5f, Theme.TEXT);

        // Badge below name
        float badgeW = Fonts.REGULAR.width(badgeLabel, 5.2f) + 12f;
        float badgeX = cx - badgeW / 2f;
        Render2D.shadow(ctx, badgeX, badgeY, badgeW, badgeH, 6f, 4f, Theme.accent(0.35f));
        Render2D.rect(ctx, badgeX, badgeY, badgeW, badgeH, 6f, Theme.accent(0.22f));
        Render2D.outline(ctx, badgeX, badgeY, badgeW, badgeH, 6f, 0.8f, Theme.accent(0.6f));
        Fonts.REGULAR.drawCentered(ctx, badgeLabel, cx, badgeY + badgeH / 2f, 5.2f, Theme.accent());

        // Bottom action buttons: [🗑 Снять] and [👁 Видно / Скрыто]
        float halfW = (w - 18f) / 2f;
        float b1X = x + 6f;
        float b2X = x + 12f + halfW;

        boolean b1Hover = inBody && Widgets.inside(mx, my, b1X, btnY, halfW, btnH);
        Render2D.rect(ctx, b1X, btnY, halfW, btnH, 5f, b1Hover ? 0x22EF4444 : 0x14FFFFFF);
        Render2D.outline(ctx, b1X, btnY, halfW, btnH, 5f, 0.8f, b1Hover ? 0x66EF4444 : 0x1AFFFFFF);
        com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, Icons.TRASH, b1X + 4f, btnY + (btnH - 8f) / 2f, 8f, b1Hover ? 0xFFEF4444 : Theme.TEXT_MUTE);
        Fonts.SEMIBOLD.drawCentered(ctx, "Снять", b1X + 15f, btnY + btnH / 2f, 5.2f, b1Hover ? 0xFFEF4444 : Theme.TEXT_MUTE);

        boolean visible = CosmeticState.isVisible();
        boolean b2Hover = inBody && Widgets.inside(mx, my, b2X, btnY, halfW, btnH);
        int b2Bg = visible ? (b2Hover ? Theme.accent(0.35f) : Theme.accent(0.22f)) : (b2Hover ? 0x22EF4444 : 0x14FFFFFF);
        int b2Border = visible ? (b2Hover ? Theme.accent(0.85f) : Theme.accent(0.6f)) : (b2Hover ? 0x66EF4444 : 0x1AFFFFFF);
        int b2TextCol = visible ? Theme.accent() : Theme.TEXT_MUTE;

        Render2D.rect(ctx, b2X, btnY, halfW, btnH, 5f, b2Bg);
        Render2D.outline(ctx, b2X, btnY, halfW, btnH, 5f, 0.8f, b2Border);
        com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, visible ? Icons.EYE : Icons.EYE_OFF, b2X + 4f, btnY + (btnH - 8f) / 2f, 8f, b2TextCol);
        Fonts.SEMIBOLD.drawCentered(ctx, visible ? "Видно" : "Скрыто", b2X + 15f, btnY + btnH / 2f, 5.2f, b2TextCol);
    }

    private float getTabWidth(int tabIdx, int mode) {
        SubTab t = tabs.get(tabIdx);
        if (mode == 2) return 22f; // Icon only
        boolean active = (tabIdx == activeTabIdx);
        float tw = 7f + 9f + 4f + Fonts.SEMIBOLD.width(t.title(), 5.8f) + 8f;
        if (active) {
            long count = allItems.stream().filter(t.filter()).count();
            tw += Fonts.REGULAR.width(" " + count, 5.2f) + 4f;
        }
        return tw;
    }

    private float getTabsTotalWidth(int mode) {
        float total = 0f;
        for (int i = 0; i < tabs.size(); i++) {
            total += getTabWidth(i, mode) + 3f;
        }
        return total - 3f;
    }

    private boolean showsLabel(int tabIdx, int mode) {
        return mode != 2;
    }

    private void drawTabsBar(DrawContext ctx, float x, float y, float w, float h, float mx, float my, boolean inBody) {
        float availTabsW = w;
        int mode = 0;
        while (mode < 2 && getTabsTotalWidth(mode) > availTabsW) {
            mode++;
        }

        float[] tabWidths = new float[tabs.size()];
        float[] tabXs = new float[tabs.size()];
        float curX = x;
        for (int i = 0; i < tabs.size(); i++) {
            tabXs[i] = curX;
            tabWidths[i] = getTabWidth(i, mode);
            curX += tabWidths[i] + 3f;
        }

        // Active indicator capsule
        float targetX = tabXs[activeTabIdx];
        float targetW = tabWidths[activeTabIdx];

        if (!tabIndicatorPlaced) {
            tabIndicator.snap(targetX);
            tabIndicatorPlaced = true;
        }
        float indX = tabIndicator.to(targetX).get();

        Render2D.rect(ctx, indX, y + 1f, targetW, h - 2f, (h - 2f) / 2f, Theme.accent(0.25f));
        Render2D.outline(ctx, indX, y + 1f, targetW, h - 2f, (h - 2f) / 2f, 0.8f, Theme.accent(0.7f));

        // Render tab buttons
        for (int i = 0; i < tabs.size(); i++) {
            SubTab t = tabs.get(i);
            float tx = tabXs[i];
            float tw = tabWidths[i];
            boolean active = (i == activeTabIdx);
            boolean hover = inBody && Widgets.inside(mx, my, tx, y + 1f, tw, h - 2f);

            int textCol = active ? Theme.TEXT : (hover ? Theme.TEXT_DIM : Theme.TEXT_MUTE);
            int iconCol = active ? Theme.accent() : (hover ? Theme.TEXT_DIM : Theme.TEXT_MUTE);

            boolean hasLabel = showsLabel(i, mode);
            float iconX = hasLabel ? (tx + 7f) : (tx + (tw - 9f) / 2f);
            com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, t.icon(), iconX, y + (h - 9f) / 2f, 9f, iconCol);

            if (hasLabel) {
                float textX = tx + 7f + 9f + 4f;
                Fonts.SEMIBOLD.drawCentered(ctx, t.title(), textX, y + h / 2f, 5.8f, textCol);
                if (active) {
                    long count = allItems.stream().filter(t.filter()).count();
                    float badgeX = textX + Fonts.SEMIBOLD.width(t.title(), 5.8f) + 3f;
                    Fonts.REGULAR.drawCentered(ctx, String.valueOf(count), badgeX, y + h / 2f, 5.2f, Theme.accent());
                }
            }
        }
    }

    private void drawCard(DrawContext ctx, CatalogItem item, float x, float y, float w, float h,
                          float mx, float my, boolean inBody, float seconds, float gridY, float gridH) {
        CardHoverState state = cardStates.computeIfAbsent(item.id(), id -> new CardHoverState());
        boolean equipped = item.isEquipped();
        boolean favorite = item.isFavorite();
        boolean hover = inBody && Widgets.inside(mx, my, x, y, w, h);

        float hAnim = state.hover.to(hover ? 1f : 0f).get();
        state.yaw += (hover ? 60f : 18f) * 0.016f;

        // Card offset on hover
        float cardY = y - hAnim * 1.5f;

        // Background & shadow
        if (hAnim > 0.01f) {
            Render2D.shadow(ctx, x, cardY + 2f, w, h, 8f, 7f, Colors.withAlpha(0x000000, 0.4f * hAnim));
        }
        if (equipped) {
            Render2D.shadow(ctx, x, cardY, w, h, 8f, 6f, Theme.accent(0.35f));
        }
        Render2D.rect(ctx, x, cardY, w, h, 8f, Colors.lerp(0xFF14141E, 0xFF1C1C28, hAnim));

        // Rarity color
        int rarityCol = item.rarity().color;
        int outlineCol = equipped ? Theme.accent() : Colors.lerp(0x18FFFFFF, rarityCol | 0x88000000, hAnim);
        Render2D.outline(ctx, x, cardY, w, h, 8f, equipped ? 1.2f : 0.8f, outlineCol);

        // 3D Preview Viewport (matching Photo 4 glowing well)
        float prevPadding = 3.5f;
        float px = x + prevPadding;
        float py = cardY + prevPadding;
        float pw = w - prevPadding * 2f;
        float ph = 88f;

        Render2D.rect(ctx, px, py, pw, ph, 6f, 0xFF161622);
        Render2D.gradientV(ctx, px, py, pw, ph, 6f, 0x00000000, Colors.withAlpha(rarityCol, 0.16f + 0.08f * hAnim));
        com.lowcode.lowcosmetic.client.gui.Render2D.glow(ctx, px + pw / 2f, py + ph - 4f, pw * 0.42f, 10f, Colors.withAlpha(rarityCol, 0.35f + 0.15f * hAnim));

        // Draw 3D Model or Item directly!
        float clipTop = Math.max(py, gridY);
        float clipBot = Math.min(py + ph, gridY + gridH);
        if (clipBot > clipTop) {
            float grow = 1.0f + 0.07f * hAnim;
            item.renderPreview(ctx, px, py, pw, ph, grow, state.yaw, seconds, gridY, gridH);
        }

        // Equipped Badge on top-left (pink circle with white checkmark)
        if (equipped) {
            Render2D.rect(ctx, px + 4f, py + 4f, 13f, 13f, 6.5f, Theme.accent());
            com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, Icons.CHECK, px + 6.5f, py + 6.5f, 8f, 0xFFFFFFFF);
        }

        // Star Favorite Button on top-right
        float starSize = 10f;
        float sx = px + pw - starSize - 4f;
        float sy = py + 4f;
        boolean starHover = inBody && Widgets.inside(mx, my, sx - 2f, sy - 2f, starSize + 4f, starSize + 4f);
        if (favorite || hAnim > 0.01f) {
            int starCol = favorite ? 0xFFFBBF24 : (starHover ? 0xFFFFFFFF : 0x88FFFFFF);
            com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, favorite ? Icons.STAR_FILLED : Icons.STAR, sx, sy, starSize, starCol);
        }

        // Card Labels (Name + Rarity)
        float textY = cardY + ph + 8f;
        String name = item.name();
        Fonts.SEMIBOLD.drawCentered(ctx, Fonts.SEMIBOLD.trim(name, 6.2f, w - 12f), x + 6f, textY, 6.2f, Theme.TEXT);

        float subY = textY + 12f;
        Render2D.circle(ctx, x + 8.5f, subY + 3.5f, 2.2f, rarityCol);
        Fonts.REGULAR.drawCentered(ctx, item.rarity().label, x + 13.5f, subY + 3.5f, 5.2f, Colors.withAlpha(rarityCol, 0.95f));
        com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, item.icon(), x + w - 14f, subY - 1f, 8f, 0x88FFFFFF);

        // Hover Action Pill: [✓ Надеть] or [✕ Снять]
        if (hAnim > 0.05f) {
            float btnW = 54f;
            float btnHb = 15f;
            float bx = x + (w - btnW) / 2f;
            float by = cardY + ph - btnHb - 5f;

            int actionCol = equipped ? 0xFFDC4B4B : Theme.accent();
            String actionTxt = equipped ? "Снять" : "Надеть";

            Render2D.shadow(ctx, bx, by, btnW, btnHb, btnHb / 2f, 4f, Colors.withAlpha(actionCol, 0.45f * hAnim));
            Render2D.rect(ctx, bx, by, btnW, btnHb, btnHb / 2f, Colors.withAlpha(actionCol, 0.95f * hAnim));
            com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, equipped ? Icons.CLOSE : Icons.CHECK, bx + 6f, by + (btnHb - 7f) / 2f, 7f, 0xFFFFFFFF);
            Fonts.SEMIBOLD.drawCentered(ctx, actionTxt, bx + 16f, by + btnHb / 2f, 5.4f, 0xFFFFFFFF);
        }

        renderedCards.add(new ClickableCard(item, x, cardY, w, h, sx, sy, starSize));
    }

    private void drawEmptyState(DrawContext ctx, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        float cy = y + h / 2f - 10f;
        Render2D.circle(ctx, cx, cy, 18f, 0x0AFFFFFF);
        com.lowcode.lowcosmetic.client.gui.Render2D.icon(ctx, Icons.HANGER, cx - 8f, cy - 8f, 16f, Theme.TEXT_MUTE);

        String title = "В этом разделе пока пусто";
        String sub = "Выберите другую вкладку или снимите фильтр поиска";
        Fonts.SEMIBOLD.drawCentered(ctx, title, cx - Fonts.SEMIBOLD.width(title, 7.2f) / 2f, cy + 26f, 7.2f, Theme.TEXT_DIM);
        Fonts.REGULAR.drawCentered(ctx, sub, cx - Fonts.REGULAR.width(sub, 5.8f) / 2f, cy + 37f, 5.8f, Theme.TEXT_MUTE);
    }

    public boolean mouseClicked(float mx, float my, int button, boolean inBody) {
        if (!inBody) return false;

        // Check player preview interaction
        if (lastPrevW > 0f && Widgets.inside(mx, my, lastPrevX, lastPrevY, lastPrevW, lastPrevH)) {
            // Check top-right reset button
            float resetX = lastPrevX + lastPrevW - 16f;
            float headerY = lastPrevY + 8f;
            if (Widgets.inside(mx, my, resetX - 2f, headerY - 2f, 13f, 13f)) {
                previewYaw = 0f;
                previewPitch = 0f;
                previewZoom = 1.0f;
                UiSounds.CLICK.play();
                return true;
            }

            // Check bottom buttons: [Снять / Снять всё] and [Видно / Скрыто]
            float btnH = 15f;
            float btnY = lastPrevY + lastPrevH - btnH - 6f;
            float halfW = (lastPrevW - 18f) / 2f;
            float b1X = lastPrevX + 6f;
            float b2X = lastPrevX + 12f + halfW;

            if (Widgets.inside(mx, my, b1X, btnY, halfW, btnH)) {
                if (activeTabIdx == 1) {
                    CosmeticState.unequip(CosmeticType.WINGS);
                } else if (activeTabIdx == 2) {
                    CosmeticState.unequip(CosmeticType.HAT);
                } else if (activeTabIdx == 3) {
                    CosmeticState.unequip(CosmeticType.BODYWEAR);
                } else if (activeTabIdx == 4) {
                    CosmeticState.unequip(CosmeticType.PET);
                    selectedPet = null;
                } else if (activeTabIdx == 5) {
                    CustomSwordsModule swords = ModuleManager.get(CustomSwordsModule.class);
                    if (swords != null) swords.swordModel.set("Стандарт");
                    selectedSword = null;
                } else if (activeTabIdx == 6) {
                    ItemModelsModule totems = ModuleManager.get(ItemModelsModule.class);
                    if (totems != null) totems.totemStyle.set("Ванильный");
                    selectedTotem = null;
                } else {
                    CosmeticState.unequipAll();
                    CustomSwordsModule swords = ModuleManager.get(CustomSwordsModule.class);
                    if (swords != null) swords.swordModel.set("Стандарт");
                    ItemModelsModule totems = ModuleManager.get(ItemModelsModule.class);
                    if (totems != null) totems.totemStyle.set("Ванильный");
                    selectedPet = null;
                    selectedSword = null;
                    selectedTotem = null;
                }
                UiSounds.UNEQUIP.play();
                return true;
            }

            if (Widgets.inside(mx, my, b2X, btnY, halfW, btnH)) {
                boolean next = !CosmeticState.isVisible();
                CosmeticState.setVisible(next);
                CosmeticsModule cm = ModuleManager.get(CosmeticsModule.class);
                if (cm != null) {
                    cm.showInWorld.set(next);
                    if (next && !cm.isEnabled()) {
                        cm.setEnabled(true);
                    }
                }
                UiSounds.CLICK.play();
                return true;
            }

            if (button == 0) {
                draggingPreview = true;
                dragStartMouseX = mx;
                dragStartMouseY = my;
                dragStartYaw = previewYaw;
                dragStartPitch = 0f;
                previewPitch = 0f;
                return true;
            } else if (button == 1) {
                // Right click resets preview view
                previewYaw = 0f;
                previewPitch = 0f;
                previewZoom = 1.0f;
                UiSounds.CLICK.play();
                return true;
            }
        }

        // Check card clicks
        for (ClickableCard card : renderedCards) {
            if (Widgets.inside(mx, my, card.x, card.y, card.w, card.h)) {
                // Check if star clicked
                if (Widgets.inside(mx, my, card.starX - 3f, card.starY - 3f, card.starSize + 6f, card.starSize + 6f)) {
                    card.item.toggleFavorite();
                    UiSounds.CLICK.play();
                    return true;
                }

                // Card clicked -> update selection for contextual preview and toggle equip
                if (card.item instanceof CosmeticEntry ce) {
                    if (ce.cosmetic.type() == CosmeticType.PET) {
                        selectedPet = ce.cosmetic;
                    }
                } else if (card.item instanceof SwordEntry se) {
                    selectedSword = se.swordName;
                } else if (card.item instanceof TotemEntry te) {
                    selectedTotem = te.totemName;
                }

                boolean equipped = card.item.toggleEquip();
                if (equipped) {
                    UiSounds.EQUIP.play();
                } else {
                    UiSounds.UNEQUIP.play();
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
        if (draggingPreview) {
            previewYaw = dragStartYaw + (mx - dragStartMouseX) * 1.2f;
            previewPitch = 0f; // Lock pitch to 0, rotate Yaw horizontally only
            return true;
        }
        return false;
    }

    public boolean handleTopBarClick(float mx, float my, float x, float y, float w, float h) {
        boolean hasPreview = w >= 340f;
        float prevW = hasPreview ? Math.min(170f, Math.max(140f, w * 0.28f)) : 0f;
        float catalogX = hasPreview ? (x + prevW + 8f) : x;
        float catalogW = hasPreview ? (w - prevW - 8f) : w;

        float availTabsW = catalogW;
        int mode = 0;
        while (mode < 2 && getTabsTotalWidth(mode) > availTabsW) {
            mode++;
        }

        float curX = catalogX;
        for (int i = 0; i < tabs.size(); i++) {
            float tw = getTabWidth(i, mode);
            if (Widgets.inside(mx, my, curX, y + 1f, tw, h - 2f)) {
                activeTabIdx = i;
                scroll.snap(0f);
                UiSounds.CATEGORY.play();
                return true;
            }
            curX += tw + 3f;
        }

        return false;
    }

    public boolean mouseScrolled(float mx, float my, double h, double v, float bodyH) {
        // If scrolling over preview panel, zoom the player preview
        if (lastPrevW > 0f && Widgets.inside(mx, my, lastPrevX, lastPrevY, lastPrevW, lastPrevH)) {
            previewZoom = Math.max(0.6f, Math.min(1.8f, previewZoom + (float) v * 0.08f));
            return true;
        }

        float max = Math.max(0f, contentH - bodyH);
        scroll.to(Math.max(0f, Math.min(scroll.target() - (float) v * 32f, max)));
        return true;
    }

    public void mouseReleased() {
        draggingPreview = false;
    }
}
