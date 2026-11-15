package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

public final class ViewModelModule extends Module {

    public final NumberSetting rightX = add(new NumberSetting("Правая рука X", 0.0f, -1.5f, 1.5f, 0.05f));
    public final NumberSetting rightY = add(new NumberSetting("Правая рука Y", 0.0f, -1.5f, 1.5f, 0.05f));
    public final NumberSetting rightZ = add(new NumberSetting("Правая рука Z", 0.0f, -1.5f, 1.5f, 0.05f));
    public final NumberSetting rightPitch = add(new NumberSetting("Правая наклон (Pitch)", 0.0f, -180.0f, 180.0f, 2.0f));
    public final NumberSetting rightYaw = add(new NumberSetting("Правая поворот (Yaw)", 0.0f, -180.0f, 180.0f, 2.0f));
    public final NumberSetting rightRoll = add(new NumberSetting("Правая крен (Roll)", 0.0f, -180.0f, 180.0f, 2.0f));
    public final NumberSetting rightScale = add(new NumberSetting("Масштаб правой", 1.0f, 0.2f, 2.5f, 0.05f));

    public final NumberSetting leftX = add(new NumberSetting("Левая рука X", 0.0f, -1.5f, 1.5f, 0.05f));
    public final NumberSetting leftY = add(new NumberSetting("Левая рука Y", 0.0f, -1.5f, 1.5f, 0.05f));
    public final NumberSetting leftZ = add(new NumberSetting("Левая рука Z", 0.0f, -1.5f, 1.5f, 0.05f));
    public final NumberSetting leftPitch = add(new NumberSetting("Левая наклон (Pitch)", 0.0f, -180.0f, 180.0f, 2.0f));
    public final NumberSetting leftYaw = add(new NumberSetting("Левая поворот (Yaw)", 0.0f, -180.0f, 180.0f, 2.0f));
    public final NumberSetting leftRoll = add(new NumberSetting("Левая крен (Roll)", 0.0f, -180.0f, 180.0f, 2.0f));
    public final NumberSetting leftScale = add(new NumberSetting("Масштаб левой", 1.0f, 0.2f, 2.5f, 0.05f));

    public final BooleanSetting swapHands = add(new BooleanSetting("Поменять руки", false));
    public final BooleanSetting noEat = add(new BooleanSetting("Отключить покачивание при еде", false));

    public ViewModelModule() {
        super("ViewModel / Рука", "Кастомное положение, угол, масштаб и смещение рук от первого лица", Category.VISUALS);
    }

    public void transform(MatrixStack matrices, Arm arm) {
        if (!isEnabled()) return;

        Arm effectiveArm = swapHands.get() ? arm.getOpposite() : arm;
        if (effectiveArm == Arm.RIGHT) {
            matrices.translate(rightX.get(), rightY.get(), rightZ.get());
            if (rightPitch.get() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rightPitch.get()));
            if (rightYaw.get() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rightYaw.get()));
            if (rightRoll.get() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rightRoll.get()));
            float s = rightScale.get();
            matrices.scale(s, s, s);
        } else {
            matrices.translate(leftX.get(), leftY.get(), leftZ.get());
            if (leftPitch.get() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leftPitch.get()));
            if (leftYaw.get() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(leftYaw.get()));
            if (leftRoll.get() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(leftRoll.get()));
            float s = leftScale.get();
            matrices.scale(s, s, s);
        }
    }
}
