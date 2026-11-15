package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * SwingAnimationModule — кастомные анимации взмаха мечом и оружием.
 * Адаптировано из Kimiko / Doxaiz-visuals (primer vizuals).
 */
public final class SwingAnimationModule extends Module {

    public final ModeSetting mode = add(new ModeSetting("Стиль удара",
            "1.7 BlockHit", "Взмах 2", "Сдвиг", "Слом", "Выпад", "Копьё",
            "Вниз", "Желе", "Шлепок", "Бонк", "Круг", "Катана-слэш",
            "Smooth", "Chop", "Punch", "Slide", "Whip", "Spin", "Sharp (Шарп)", "Glide (Скольжение)"));
    public final NumberSetting speed = add(new NumberSetting("Скорость удара", 1.2f, 0.4f, 3.0f, 0.1f));
    public final NumberSetting strength = add(new NumberSetting("Сила взмаха", 1.0f, 0.3f, 2.5f, 0.1f));
    public final BooleanSetting ignoreOffhand = add(new BooleanSetting("Только главная рука", true));

    public SwingAnimationModule() {
        super("Анимация удара", "Кастомные анимации взмаха мечом (1.7 блок-хит, сдвиг, слом, катана-слэш)", Category.VISUALS);
    }

    public void applySwing(MatrixStack matrices, float swingProgress) {
        applySwing(matrices, swingProgress, net.minecraft.util.Arm.RIGHT, 1);
    }

    public void applySwing(MatrixStack matrices, float swingProgress, net.minecraft.util.Arm arm, int armX) {
        if (!isEnabled() || swingProgress <= 0.0f) return;

        float f = Math.min(1.0f, swingProgress * speed.get());
        float f2 = MathHelper.sin(f * f * (float) Math.PI);
        float f3 = MathHelper.sin(MathHelper.sqrt(f) * (float) Math.PI);
        float f4 = (float) Math.sin(f * Math.PI);
        float f5 = strength.get();
        int n = (arm == net.minecraft.util.Arm.LEFT ? -1 : 1) * (armX < 0 ? -1 : 1);

        String m = mode.get();
        switch (m) {
            case "1.7 BlockHit" -> {
                matrices.translate(n * -0.08f * f4, 0.06f * f4, -0.1f * f4);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * (45.0f * f * f5)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0f * f4 * f5));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(n * (-30.0f * f4 * f5)));
            }
            case "Взмах 2" -> {
                matrices.scale(1.0f, 1.0f, f4 + 1.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f * f4 * (f5 * 0.3f)));
            }
            case "Сдвиг" -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(n * -60.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f - 20.0f * f4 * f5));
            }
            case "Слом" -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(n * -30.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f - 20.0f * f4 * f5));
            }
            case "Выпад" -> {
                matrices.translate(0.0f, 0.0f, -f4 * 0.2f * f5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 45.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * f2 * -20.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(n * f3 * -20.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f3 * -Math.min(80.0f, Math.max(55.0f, f5 * 55.0f))));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 10.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 60.0f));
            }
            case "Копьё" -> {
                matrices.translate(0.0f, 0.0f, -f4 * 0.2f * f5);
                matrices.scale(1.0f, 1.0f, f4 * (f5 * 0.35f) + 1.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
            }
            case "Вниз" -> {
                matrices.translate(-f4 * 0.5f * f5, f4 * 0.25f * f5, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 65.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-110.0f - 80.0f * f4 * f5));
            }
            case "Желе" -> {
                matrices.translate(0.0f, 0.0f, -f4 * 0.2f * f5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * 35.0f));
                matrices.scale(1.0f, 1.0f, f4 * (f * 0.65f * f5 * 0.2f) + 1.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
            }
            case "Шлепок" -> {
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(f3 * 55.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f3 * f2 * -120.0f * f5));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * (f3 * 60.0f)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f3 * 55.0f));
            }
            case "Бонк" -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * n));
                float bonkEase = easeOutElastic(f * f);
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(95.0f * bonkEase * f5));
            }
            case "Круг" -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * n));
                float circleEase = (float) Math.pow(f, 1.8);
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(360.0f * circleEase * f5 + 45.0f));
            }
            case "Катана-слэш" -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * (45.0f + f2 * -20.0f)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(n * f3 * -20.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f3 * -f5 * 35.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n * -45.0f));
            }
            case "Smooth" -> {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0f * f4 * f5));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0f * f4 * f5));
            }
            case "Chop" -> {
                matrices.translate(0.0, -0.15 * f4 * f5, 0.0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-45.0f * f4 * f5));
            }
            case "Punch" -> matrices.translate(0.0, 0.0, -0.3 * f4 * f5);
            case "Slide" -> {
                matrices.translate(0.15 * f4 * f5, -0.05 * f4, 0.0);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-15.0f * f4 * f5));
            }
            case "Whip" -> {
                matrices.translate(0.1 * f4 * f5, 0.05 * f4 * f5, -0.2 * f4 * f5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0f * f4 * f5));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0f * f4 * f5));
            }
            case "Spin" -> {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(360.0f * f * f5));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0f * f4));
            }
            case "Sharp (Шарп)" -> {
                float sharp = (float) Math.pow(f4, 2.5);
                matrices.translate(0.05 * sharp * f5, -0.2 * sharp * f5, -0.15 * sharp * f5);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0f * sharp * f5));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25.0f * sharp * f5));
            }
            case "Glide (Скольжение)" -> {
                float glide = (float) Math.sin(f * Math.PI * 0.85);
                matrices.translate(-0.15 * glide * f5, 0.05 * glide, -0.1 * glide);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0f * glide * f5));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-25.0f * glide * f5));
            }
        }
    }

    private static float easeOutElastic(float x) {
        if (x <= 0.0f) return 0.0f;
        if (x >= 1.0f) return 1.0f;
        double c4 = (2.0 * Math.PI) / 3.0;
        return (float) (Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * c4) + 1.0);
    }
}
