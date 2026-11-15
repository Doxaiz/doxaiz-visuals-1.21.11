package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public final class TrajectoryModule extends Module implements Render3DListener {

    public final BooleanSetting impactBox = add(new BooleanSetting("Маркер попадания", true));
    public final com.lowcode.svintusclient.client.module.setting.ModeSetting impactStyle = add(new com.lowcode.svintusclient.client.module.setting.ModeSetting("Стиль маркера", "Круг (Hit Circle)", "Бокс", "Диск", "Крест"));
    public final NumberSetting circleRadius = add(new NumberSetting("Радиус круга", 0.45f, 0.2f, 1.5f, 0.05f));
    public final NumberSetting lineWidth = add(new NumberSetting("Толщина траектории", 2.0f, 1.0f, 5.0f, 0.5f));
    public final ColorSetting lineColor = add(new ColorSetting("Цвет траектории", 0xFF00CEC9));
    public final ColorSetting impactColor = add(new ColorSetting("Цвет маркера", 0xFFFF7675));

    public TrajectoryModule() {
        super("Trajectory", "Траектория полета стрел, перлов, снежков и зелий", Category.BATTLE);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        ItemStack hand = mc.player.getMainHandStack();
        if (hand.isEmpty()) hand = mc.player.getOffHandStack();
        if (hand.isEmpty()) return;

        Item item = hand.getItem();
        boolean isBow = item instanceof BowItem || item instanceof CrossbowItem;
        boolean isPearl = item == Items.ENDER_PEARL;
        boolean isPotion = item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
        boolean isThrowable = isPearl || isPotion || item == Items.SNOWBALL || item == Items.EGG || item == Items.TRIDENT || item == Items.WIND_CHARGE;

        if (!isBow && !isThrowable) return;

        float tickDelta = tickCounter.getTickProgress(false);
        Vec3d eyePos = mc.player.getCameraPosVec(tickDelta);
        Vec3d rot = mc.player.getRotationVec(tickDelta);

        double velocity = 1.5;
        double gravity = 0.03;
        double drag = 0.99;

        if (isBow) {
            float pull = 1.0f;
            if (mc.player.isUsingItem() && mc.player.getActiveItem() == hand) {
                int useTicks = mc.player.getItemUseTime();
                pull = useTicks / 20.0f;
                pull = (pull * pull + pull * 2.0f) / 3.0f;
                if (pull > 1.0f) pull = 1.0f;
            }
            velocity = pull * 3.0;
            gravity = 0.05;
        } else if (isPearl) {
            velocity = 1.5;
            gravity = 0.03;
        } else if (isPotion) {
            velocity = 0.5;
            gravity = 0.05;
        } else if (item == Items.TRIDENT) {
            velocity = 2.5;
            gravity = 0.05;
        }

        Vec3d motion = rot.multiply(velocity);
        Vec3d currentPos = eyePos;

        List<Vec3d> path = new ArrayList<>();
        path.add(currentPos);

        HitResult hit = null;
        boolean hitEntity = false;
        for (int i = 0; i < 120; i++) {
            Vec3d nextPos = currentPos.add(motion);
            hit = mc.world.raycast(new RaycastContext(
                    currentPos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            // Check entity collision
            Box stepBox = new Box(currentPos, nextPos).expand(0.4);
            for (net.minecraft.entity.Entity e : mc.world.getOtherEntities(mc.player, stepBox)) {
                if (e.canHit() && e instanceof net.minecraft.entity.LivingEntity && mc.player.canSee(e)) {
                    Box eBox = e.getBoundingBox().expand(0.2);
                    var opt = eBox.raycast(currentPos, nextPos);
                    if (opt.isPresent()) {
                        hit = new net.minecraft.util.hit.EntityHitResult(e, opt.get());
                        hitEntity = true;
                        break;
                    }
                }
            }

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getPos());
                break;
            }

            path.add(nextPos);
            currentPos = nextPos;
            motion = motion.multiply(drag).subtract(0, gravity, 0);
        }

        int lColor = hitEntity ? 0xFFFF3366 : lineColor.getEffectiveColor();
        float lWidth = lineWidth.get();

        for (int i = 0; i < path.size() - 1; i++) {
            Render3D.drawLine(path.get(i), path.get(i + 1), lColor, lWidth);
        }

        if (impactBox.get() && hit != null) {
            Vec3d impact = hit.getPos();
            int iColor = hitEntity ? 0xFFFF2244 : impactColor.getEffectiveColor();
            String st = impactStyle.get();
            float rad = circleRadius.get();

            if (st.equals("Бокс")) {
                Box box = new Box(impact.x - 0.2, impact.y - 0.2, impact.z - 0.2, impact.x + 0.2, impact.y + 0.2, impact.z + 0.2);
                Render3D.drawBox(box, 0x44000000 | (iColor & 0x00FFFFFF), iColor, true, true, 2.0f);
            } else if (st.equals("Круг (Hit Circle)")) {
                Render3D.drawCircle(impact.add(0, 0.02, 0), rad, 24, iColor, 2.5f);
                Render3D.drawCircle(impact.add(0, 0.02, 0), rad * 0.4, 16, (0x88000000) | (iColor & 0x00FFFFFF), 1.5f);
            } else if (st.equals("Диск")) {
                Render3D.drawCircle(impact.add(0, 0.02, 0), rad, 24, iColor, 2.0f);
                Render3D.drawFilledCircle(impact.add(0, 0.02, 0), rad, 24, (0x66000000) | (iColor & 0x00FFFFFF), (0x11000000) | (iColor & 0x00FFFFFF));
            } else if (st.equals("Крест")) {
                Render3D.drawLine(impact.add(-rad, 0.02, -rad), impact.add(rad, 0.02, rad), iColor, 2.0f);
                Render3D.drawLine(impact.add(-rad, 0.02, rad), impact.add(rad, 0.02, -rad), iColor, 2.0f);
            }
        }
    }
}
