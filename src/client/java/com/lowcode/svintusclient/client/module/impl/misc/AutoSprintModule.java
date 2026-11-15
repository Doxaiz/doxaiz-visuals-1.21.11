package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class AutoSprintModule extends Module implements TickListener {

    public final ModeSetting mode = add(new ModeSetting("Режим", "Legit (Умный)", "Всегда (Rage)", "Импульсный"));
    public final BooleanSetting omni = add(new BooleanSetting("Всенаправленный", false));
    public final BooleanSetting keepSprint = add(new BooleanSetting("Удерживать при ударах", true));
    public final BooleanSetting particles = add(new BooleanSetting("Частицы спринта", true));

    private boolean heldByUs = false;

    public AutoSprintModule() {
        super("AutoSprint", "Автоматически удерживает спринт при движении вперёд", Category.UTILITIES);
        enableByDefault();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options == null) {
            releaseSprint();
            return;
        }

        boolean forward = mc.options.forwardKey.isPressed();
        boolean moving = omni.get()
                ? (forward || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed())
                : forward;

        boolean hungerOk = mc.player.getHungerManager().getFoodLevel() > 6 || "Всегда (Rage)".equals(mode.get());
        boolean canSprint = moving && !mc.player.isSneaking() && !mc.player.isRiding() && !mc.player.isSubmergedInWater() && hungerOk;

        if (canSprint) {
            if ("Импульсный".equals(mode.get())) {
                if (mc.player.age % 6 < 4) {
                    mc.options.sprintKey.setPressed(true);
                    heldByUs = true;
                } else {
                    mc.options.sprintKey.setPressed(false);
                }
            } else {
                mc.options.sprintKey.setPressed(true);
                heldByUs = true;
            }

            if (omni.get() || "Всегда (Rage)".equals(mode.get())) {
                mc.player.setSprinting(true);
            }

            if (keepSprint.get() && mc.player.hurtTime > 0) {
                mc.player.setSprinting(true);
            }

            if (particles.get() && mc.player.isSprinting() && mc.player.isOnGround() && mc.world != null) {
                if (mc.player.age % 4 == 0) {
                    mc.world.addParticleClient(net.minecraft.particle.ParticleTypes.CLOUD,
                            mc.player.getX() + (Math.random() - 0.5) * 0.2,
                            mc.player.getY() + 0.05,
                            mc.player.getZ() + (Math.random() - 0.5) * 0.2,
                            0.0, 0.02, 0.0);
                }
            }
        } else {
            releaseSprint();
        }
    }

    private void releaseSprint() {
        if (heldByUs) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null && mc.options.sprintKey != null) {
                mc.options.sprintKey.setPressed(false);
            }
            heldByUs = false;
        }
    }

    @Override
    protected void onDisable() {
        releaseSprint();
    }
}
