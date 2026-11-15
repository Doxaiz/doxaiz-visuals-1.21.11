package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

public final class AspectRatioModule extends Module {

    public final ModeSetting preset = add(new ModeSetting("Пресет",
            "4:3 Stretched", "16:9 Widescreen", "21:9 Cinematic", "1:1 Квадрат", "5:4 CS", "Кастом"));
    public final NumberSetting customRatio = add(new NumberSetting("Своё соотношение", 1.33f, 0.4f, 2.6f, 0.05f));
    public final BooleanSetting fovCompensation = add(new BooleanSetting("Компенсация FOV", true));

    public AspectRatioModule() {
        super("Растяг экрана", "Настройка соотношения сторон проекции (4:3 стрейч, 21:9 ультравайд)", Category.UTILITIES);
    }

    public void apply(Matrix4f matrix4f) {
        if (!isEnabled() || matrix4f == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        float w = mc.getWindow().getFramebufferWidth();
        float h = mc.getWindow().getFramebufferHeight();
        if (w <= 0.0f || h <= 0.0f) return;

        float currentRatio = w / h;
        float targetRatio = switch (preset.get()) {
            case "4:3 Stretched" -> 4.0f / 3.0f;
            case "16:9 Widescreen" -> 16.0f / 9.0f;
            case "21:9 Cinematic" -> 21.0f / 9.0f;
            case "1:1 Квадрат" -> 1.0f;
            case "5:4 CS" -> 5.0f / 4.0f;
            default -> customRatio.get();
        };

        if (Float.isFinite(currentRatio) && Float.isFinite(targetRatio) && targetRatio > 0.05f) {
            if (fovCompensation.get() && targetRatio > currentRatio) {
                targetRatio = (targetRatio + currentRatio) * 0.5f;
            }
            matrix4f.m00(matrix4f.m00() * (currentRatio / targetRatio));
        }
    }
}
