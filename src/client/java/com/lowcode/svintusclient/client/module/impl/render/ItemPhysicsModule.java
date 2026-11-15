package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class ItemPhysicsModule extends Module {

    public final ModeSetting mode = add(new ModeSetting("Режим", "Физика 3D (Physics)", "Лежачий (Flat)", "Вращение (Rotate)"));
    public final BooleanSetting rotation = add(new BooleanSetting("Вращение предметов", false));
    public final BooleanSetting bobbing = add(new BooleanSetting("Покачивание в воздухе", false));
    public final NumberSetting rotationSpeed = add(new NumberSetting("Скорость вращения", 1.2f, 0.1f, 4.0f, 0.1f));
    public final NumberSetting bobStrength = add(new NumberSetting("Амплитуда покачивания", 0.7f, 0.0f, 2.5f, 0.1f));
    public final NumberSetting flatAngle = add(new NumberSetting("Угол наклона", 90.0f, 0.0f, 90.0f, 5.0f));
    public final NumberSetting scale = add(new NumberSetting("Размер предмета", 1.0f, 0.5f, 2.5f, 0.05f));

    public ItemPhysicsModule() {
        super("Item Physics", "Реалистичная 3D физика выпавших предметов на земле", Category.VISUALS);
    }

    public void applyTransform(MatrixStack matrices, ItemEntityRenderState state, boolean onGround) {
        if (!isEnabled()) return;

        float sc = scale.get();
        if (Math.abs(sc - 1.0f) > 0.01f) {
            matrices.scale(sc, sc, sc);
        }

        float age = state.age;
        float offset = state.uniqueOffset;
        float speed = rotationSpeed.get();
        boolean doRotate = rotation.get();
        boolean doBob = bobbing.get();
        String m = mode.get();

        if ("Лежачий (Flat)".equals(m)) {
            if (doRotate) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-ItemEntity.getRotation(age, offset)));
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(flatAngle.get()));
            matrices.translate(0.0f, -0.15f, 0.0f);
        } else if ("Вращение (Rotate)".equals(m)) {
            if (doRotate) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((age * speed * 20f) % 360f));
            }
            if (doBob) {
                float bob = MathHelper.sin(age * 0.1f) * 0.08f * bobStrength.get();
                matrices.translate(0.0f, bob, 0.0f);
            }
        } else {
            // Full 3D Physics
            if (onGround) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(flatAngle.get()));
                matrices.translate(0.0f, -0.18f, 0.0f);
            } else {
                if (doRotate) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((age * 15.0f * speed + offset * 360.0f) % 360.0f));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((age * 10.0f * speed) % 360.0f));
                }
                if (doBob) {
                    float bob = MathHelper.sin(age * 0.12f) * 0.05f * bobStrength.get();
                    matrices.translate(0.0f, bob, 0.0f);
                }
            }
        }
    }

    public void applyTransform(MatrixStack matrices, ItemEntityRenderState state) {
        applyTransform(matrices, state, false);
    }
}
