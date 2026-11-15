package com.lowcode.lowcosmetic.client;

import com.lowcode.lowcosmetic.client.cosmetic.CosmeticRegistry;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticState;
import com.lowcode.lowcosmetic.client.gui.CosmeticScreen;
import com.lowcode.lowcosmetic.client.gui.Render2D;
import com.lowcode.lowcosmetic.client.gui.UiSettings;
import com.lowcode.lowcosmetic.client.render.CosmeticFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LowcosmeticClient implements ClientModInitializer {
    public static final String MOD_ID = "lowcosmetic";
    public static final Logger LOGGER = LoggerFactory.getLogger("LowCosmetic");

    private static KeyBinding openKey;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static KeyBinding openKey() {
        return openKey;
    }

    public static void init() {
        new LowcosmeticClient().onInitializeClient();
    }

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.lowcosmetic.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer player) {
                helper.register(new CosmeticFeatureRenderer(player));
            }
        });

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id("cosmetics");
            }

            @Override
            public void reload(ResourceManager manager) {
                Render2D.init();
                UiSettings.load();
                CosmeticRegistry.load(manager);
                CosmeticState.load();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new CosmeticScreen());
                }
            }
        });
    }
}
