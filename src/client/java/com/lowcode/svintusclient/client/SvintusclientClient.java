package com.lowcode.svintusclient.client;

import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import com.lowcode.svintusclient.client.gui.ClickGuiScreen;
import com.lowcode.svintusclient.client.hud.HudManager;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.impl.render.ChinaHatModule;
import com.lowcode.svintusclient.client.render.Render3D;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class SvintusclientClient implements ClientModInitializer {

    public static final String MOD_ID = "svintusclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("SvintusClient");

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
    private static final Set<Integer> HELD_KEYS = new HashSet<>();
    private static KeyBinding clickGuiKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        HudManager.register();
        LowcosmeticClient.init();
        com.lowcode.svintusclient.client.holyworld.HolyWorldApiService.startPolling();
        Config.load();

        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            com.lowcode.svintusclient.client.module.impl.misc.FoodInfoModule foodInfo = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.FoodInfoModule.class);
            if (foodInfo != null && foodInfo.isEnabled() && stack.contains(net.minecraft.component.DataComponentTypes.FOOD)) {
                net.minecraft.component.type.FoodComponent food = stack.get(net.minecraft.component.DataComponentTypes.FOOD);
                if (food != null) {
                    int nutrition = food.nutrition();
                    float saturation = food.saturation();
                    StringBuilder sb = new StringBuilder();
                    if (foodInfo.showHungerPreview.get()) {
                        sb.append("§6• Сытость: §e+").append(nutrition);
                    }
                    if (foodInfo.showSaturation.get()) {
                        if (sb.length() > 0) sb.append(" §7| ");
                        sb.append("§6Насыщение: §e+").append(String.format(java.util.Locale.US, "%.1f", saturation * nutrition * 2.0f));
                    }
                    if (foodInfo.shouldShowHealing()) {
                        float estHeal = Math.min((float) nutrition, (saturation * (float) nutrition * 2.0f) / 1.5f) / 2.0f;
                        if (estHeal > 0.05f) {
                            if (sb.length() > 0) sb.append(" §7| ");
                            sb.append("§c❤ §a+").append(String.format(java.util.Locale.US, "%.1f", estHeal));
                        }
                    }
                    if (sb.length() > 0) {
                        lines.add(net.minecraft.text.Text.literal(sb.toString()));
                    }
                }
            }
            com.lowcode.svintusclient.client.module.impl.misc.ItemHintsModule hints = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.ItemHintsModule.class);
            if (hints != null) {
                hints.handleTooltip(stack, lines);
            }
            com.lowcode.svintusclient.client.module.impl.misc.CoinPriceModule cpm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.CoinPriceModule.class);
            if (cpm != null) {
                cpm.handleTooltip(stack, lines);
            }
            com.lowcode.svintusclient.client.module.impl.misc.MarketHelperModule mhm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.MarketHelperModule.class);
            if (mhm != null && mhm.isEnabled()) {
                mhm.handleTooltip(stack, lines);
            }
            com.lowcode.svintusclient.client.module.impl.misc.SpawnerContentsModule scm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.SpawnerContentsModule.class);
            if (scm != null && scm.isEnabled()) {
                scm.handleTooltip(stack, lines);
            }
            com.lowcode.svintusclient.client.module.impl.misc.CompassCdModule ccd = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.CompassCdModule.class);
            if (ccd != null && ccd.isEnabled()) {
                ccd.handleTooltip(stack, lines);
            }
            com.lowcode.svintusclient.client.module.impl.misc.CoinRateModule crm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.CoinRateModule.class);
            if (crm != null && crm.isEnabled()) {
                double price = mhm != null ? mhm.extractPrice(lines) : -1.0;
                if (price > 0) {
                    String coinStr = crm.convertRubToCoins(price);
                    if (coinStr != null) {
                        lines.add(net.minecraft.text.Text.literal("§e🪙 " + coinStr));
                    }
                }
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            com.lowcode.svintusclient.client.module.impl.render.ChunksAnimationModule cam = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ChunksAnimationModule.class);
            if (cam != null && cam.isEnabled()) {
                cam.onChunkLoad(chunk.getPos().x, chunk.getPos().z);
                float offY = cam.getChunkOffsetY(chunk.getPos().x, chunk.getPos().z);
            }
        });

        net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String lower = command.trim().toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("friend ") || lower.startsWith("f ")) {
                String[] parts = command.trim().split("\\s+");
                MinecraftClient mc = MinecraftClient.getInstance();
                if (parts.length >= 3 && parts[1].equalsIgnoreCase("add")) {
                    String name = parts[2];
                    com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.addFriend(name);
                    if (mc.player != null) mc.player.sendMessage(net.minecraft.text.Text.literal("§a[Friends] §fИгрок §e" + name + " §fдобавлен в друзья!"), false);
                    return false;
                } else if (parts.length >= 3 && (parts[1].equalsIgnoreCase("remove") || parts[1].equalsIgnoreCase("del"))) {
                    String name = parts[2];
                    com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.removeFriend(name);
                    if (mc.player != null) mc.player.sendMessage(net.minecraft.text.Text.literal("§c[Friends] §fИгрок §e" + name + " §fудален из друзей!"), false);
                    return false;
                } else if (parts.length >= 2 && parts[1].equalsIgnoreCase("list")) {
                    if (mc.player != null) {
                        java.util.Set<String> frs = com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.getFriends();
                        String listStr = frs.isEmpty() ? "§7(пусто)" : String.join(", ", frs);
                        mc.player.sendMessage(net.minecraft.text.Text.literal("§e[Friends] §fСписок друзей: §a" + listStr), false);
                    }
                    return false;
                }
            }
            return true;
        });

        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.svintusclient.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
            }
            handleModuleBinds(client);
            if (client.world != null && client.player != null) {
                for (Module module : ModuleManager.all()) {
                    if (module.isEnabled() && module instanceof TickListener tl) {
                        tl.onTick();
                    }
                }
            }
            com.lowcode.svintusclient.client.module.impl.render.PerformanceModule perf = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PerformanceModule.class);
            if (perf != null && perf.isEnabled()) {
                boolean fastFont = perf.isFastFont();
                int freed = perf.getLastFreedMb();
                boolean allowP = perf.allowParticle(50);
            }
            com.lowcode.svintusclient.client.module.impl.render.MediaPlayerHudModule mph = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.MediaPlayerHudModule.class);
            if (mph != null && mph.isEnabled()) {
                mph.setTrack("Doxaiz - Ambient Beats", "Svintus Lo-Fi");
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrices = context.matrices();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices == null || consumers == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.gameRenderer == null) return;

            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera != null ? camera.getCameraPos() : Vec3d.ZERO;
            RenderTickCounter tickCounter = client.getRenderTickCounter();

            VertexConsumerProvider.Immediate immediate = consumers instanceof VertexConsumerProvider.Immediate imm
                    ? imm
                    : client.getBufferBuilders().getEntityVertexConsumers();

            for (Module module : ModuleManager.all()) {
                if (module.isEnabled() && module instanceof Render3DListener r3d) {
                    r3d.onRender3D(matrices, immediate, camera, tickCounter);
                }
            }

            if (client.player != null && client.world != null) {
                float tickDelta = tickCounter != null ? tickCounter.getTickProgress(false) : 0f;
                ChinaHatModule chinaHat = ModuleManager.get(ChinaHatModule.class);
                if (chinaHat != null && chinaHat.isEnabled()) {
                    String tm = chinaHat.targetMode.get();
                    if ("Всем игрокам".equals(tm)) {
                        for (net.minecraft.entity.player.PlayerEntity p : client.world.getPlayers()) {
                            if (p.isAlive() && (p == client.player || client.player.canSee(p))) {
                                chinaHat.renderForPlayer(matrices, immediate, p, tickDelta);
                            }
                        }
                    } else if ("Себе и союзникам".equals(tm)) {
                        for (net.minecraft.entity.player.PlayerEntity p : client.world.getPlayers()) {
                            if (p == client.player || client.player.isTeammate(p)) {
                                if (p.isAlive() && (p == client.player || client.player.canSee(p))) {
                                    chinaHat.renderForPlayer(matrices, immediate, p, tickDelta);
                                }
                            }
                        }
                    } else {
                        chinaHat.renderForPlayer(matrices, immediate, client.player, tickDelta);
                    }
                }
            }

            Render3D.render(matrices, immediate, cameraPos);
            immediate.draw();
        });
    }

    private static void handleModuleBinds(MinecraftClient client) {
        com.lowcode.svintusclient.client.module.impl.render.FreeLookModule freeLook = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.FreeLookModule.class);
        if (freeLook != null && freeLook.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            boolean flDown = InputUtil.isKeyPressed(client.getWindow(), freeLook.getKey());
            freeLook.updateActiveState(flDown && client.currentScreen == null);
        }

        com.lowcode.svintusclient.client.module.impl.misc.CommandBindsModule cmdBinds = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.CommandBindsModule.class);
        if (cmdBinds != null && cmdBinds.isEnabled() && client.currentScreen == null) {
            for (java.util.Map.Entry<Integer, String> entry : com.lowcode.svintusclient.client.module.impl.misc.CommandBindsModule.getBinds().entrySet()) {
                int k = entry.getKey();
                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) continue;
                boolean kDown = InputUtil.isKeyPressed(client.getWindow(), k);
                if (kDown && HELD_KEYS.add(k)) {
                    cmdBinds.onKey(k);
                } else if (!kDown) {
                    HELD_KEYS.remove(k);
                }
            }
        }

        for (Module module : ModuleManager.all()) {
            if (module == freeLook) continue;
            int key = module.getKey();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            boolean down = InputUtil.isKeyPressed(client.getWindow(), key);
            if (down && HELD_KEYS.add(key)) {
                if (client.currentScreen == null) {
                    if (module instanceof com.lowcode.svintusclient.client.module.impl.misc.ClickPearlModule cp) {
                        cp.trigger();
                    } else {
                        module.toggle();
                    }
                }
            } else if (!down) {
                HELD_KEYS.remove(key);
            }
        }
    }

    public static int clickGuiKey() {
        return KeyBindingHelper.getBoundKeyOf(clickGuiKey).getCode();
    }
}
