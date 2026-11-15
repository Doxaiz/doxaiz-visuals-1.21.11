package com.lowcode.svintusclient.client.module.impl.cosmetics;

import com.lowcode.lowcosmetic.client.cosmetic.CosmeticState;
import com.lowcode.lowcosmetic.client.gui.CosmeticScreen;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class CosmeticsModule extends Module implements TickListener {

    public final BooleanSetting showInWorld = add(new BooleanSetting("Показывать в мире", true));
    public final BooleanSetting showFirstPerson = add(new BooleanSetting("Видно от 1-го лица", true));
    public final BooleanSetting showOtherPlayers = add(new BooleanSetting("Косметика других игроков", true));
    public final BooleanSetting animatedGui = add(new BooleanSetting("Анимация в меню", true));

    public boolean isVisibleInFirstPerson() {
        return isEnabled() && showFirstPerson.get();
    }

    public boolean shouldRenderOtherPlayers() {
        return isEnabled() && showOtherPlayers.get();
    }

    public boolean isAnimatedGui() {
        return isEnabled() && animatedGui.get();
    }

    public CosmeticsModule() {
        super("Cosmetics", "3D гардероб косметики (крылья, шляпы, одежда, питомцы)", Category.COSMETICS);
        setKey(GLFW.GLFW_KEY_K);
        enableByDefault();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        boolean cur = showInWorld.get();
        if (CosmeticState.isVisible() != cur) {
            CosmeticState.setVisible(cur);
        }
    }

    @Override
    protected void onEnable() {
        CosmeticState.setVisible(showInWorld.get());
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            client.setScreen(new CosmeticScreen());
        }
    }

    @Override
    protected void onDisable() {
        CosmeticState.setVisible(false);
    }
}
