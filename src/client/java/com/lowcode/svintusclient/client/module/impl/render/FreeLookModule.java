package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

public final class FreeLookModule extends Module {

    public final BooleanSetting autoThirdPerson = add(new BooleanSetting("Вид от 3-го лица при зажатии", true));
    public final BooleanSetting smooth = add(new BooleanSetting("Плавность", true));
    public final NumberSetting sensitivity = add(new NumberSetting("Чувствительность", 1.0f, 0.2f, 3.0f, 0.1f));
    public final BooleanSetting invertPitch = add(new BooleanSetting("Инверсия по вертикали", false));
    public final ModeSetting defaultView = add(new ModeSetting("Режим обзора", "Сзади", "Спереди", "Сверху"));

    private boolean active;
    private float freeYaw;
    private float freePitch;
    private float targetYaw;
    private float targetPitch;
    private Perspective prevPerspective = Perspective.FIRST_PERSON;

    public FreeLookModule() {
        super("Perspective", "Свободный обзор камеры от 3-го лица без изменения движения", Category.UTILITIES);
        setKey(GLFW.GLFW_KEY_LEFT_ALT);
    }

    public boolean isActive() {
        return isEnabled() && active;
    }

    public float getFreeYaw() {
        if (smooth.get()) {
            freeYaw += (targetYaw - freeYaw) * 0.45f;
            return freeYaw;
        }
        return targetYaw;
    }

    public float getFreePitch() {
        if (smooth.get()) {
            freePitch += (targetPitch - freePitch) * 0.45f;
            return freePitch;
        }
        return targetPitch;
    }

    public void updateActiveState(boolean pressed) {
        if (!isEnabled()) {
            active = false;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (pressed && !active) {
            active = true;
            if (mc.player != null) {
                freeYaw = mc.player.getYaw();
                freePitch = mc.player.getPitch();
                targetYaw = freeYaw;
                targetPitch = freePitch;
            }
            if (autoThirdPerson.get() && mc.options != null) {
                prevPerspective = mc.options.getPerspective();
                Perspective p = "Спереди".equals(defaultView.get()) ? Perspective.THIRD_PERSON_FRONT : Perspective.THIRD_PERSON_BACK;
                mc.options.setPerspective(p);
                if ("Сверху".equals(defaultView.get())) {
                    targetPitch = 85.0f;
                    freePitch = 85.0f;
                }
            }
        } else if (!pressed && active) {
            active = false;
            if (autoThirdPerson.get() && mc.options != null) {
                mc.options.setPerspective(prevPerspective);
            }
        }
    }

    @Override
    protected void onDisable() {
        if (active) {
            updateActiveState(false);
        }
    }

    public void onMouseDelta(double dx, double dy) {
        if (!isActive()) return;
        float sens = sensitivity.get();
        targetYaw += (float) (dx * 0.15 * sens);
        float dySens = (float) (dy * 0.15 * sens);
        if (invertPitch.get()) dySens = -dySens;
        targetPitch = Math.max(-90.0f, Math.min(90.0f, targetPitch + dySens));
    }
}
