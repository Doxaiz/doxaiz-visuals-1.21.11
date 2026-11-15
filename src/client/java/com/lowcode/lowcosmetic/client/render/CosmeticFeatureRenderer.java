package com.lowcode.lowcosmetic.client.render;

import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticModel;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticState;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticType;
import com.lowcode.lowcosmetic.client.geo.GeoAnimation;
import com.lowcode.lowcosmetic.client.geo.GeoModel;
import java.util.List;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.math.RotationAxis;

public final class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (state.invisible || state.spectator) {
            return;
        }

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        boolean isLocalPlayer = mc.player != null && state.id == mc.player.getId();
        boolean inGui = mc.currentScreen != null;
        if (!inGui) {
            com.lowcode.svintusclient.client.module.impl.cosmetics.CosmeticsModule cm = com.lowcode.svintusclient.client.module.ModuleManager.get(com.lowcode.svintusclient.client.module.impl.cosmetics.CosmeticsModule.class);
            if (cm != null && !cm.isEnabled()) {
                return;
            }
            if (cm != null && !cm.showInWorld.get()) {
                return;
            }
            if (cm != null && !cm.shouldRenderOtherPlayers() && !isLocalPlayer) {
                return;
            }
        }

        List<Cosmetic> worn = CosmeticState.wornBy(state.id);
        if (worn.isEmpty()) {
            return;
        }

        boolean elytra = state.equippedChestStack.contains(DataComponentTypes.GLIDER);
        boolean chestArmor = !state.equippedChestStack.isEmpty() && !elytra;
        CosmeticModel.Motion motion = state.isGliding ? CosmeticModel.Motion.GLIDING
            : state.isSwimming ? CosmeticModel.Motion.SWIMMING
            : state.limbSwingAmplitude > 0.15F ? CosmeticModel.Motion.MOVING
            : CosmeticModel.Motion.IDLE;
        float seconds = state.age / 20.0F;

        com.lowcode.svintusclient.client.module.impl.cosmetics.WingsCosmeticModule wings = isLocalPlayer ? com.lowcode.svintusclient.client.module.ModuleManager.get(com.lowcode.svintusclient.client.module.impl.cosmetics.WingsCosmeticModule.class) : null;
        com.lowcode.svintusclient.client.module.impl.cosmetics.HatsCosmeticModule hats = isLocalPlayer ? com.lowcode.svintusclient.client.module.ModuleManager.get(com.lowcode.svintusclient.client.module.impl.cosmetics.HatsCosmeticModule.class) : null;
        com.lowcode.svintusclient.client.module.impl.cosmetics.BackWearCosmeticModule back = isLocalPlayer ? com.lowcode.svintusclient.client.module.ModuleManager.get(com.lowcode.svintusclient.client.module.impl.cosmetics.BackWearCosmeticModule.class) : null;
        com.lowcode.svintusclient.client.module.impl.cosmetics.PetsCosmeticModule pets = isLocalPlayer ? com.lowcode.svintusclient.client.module.ModuleManager.get(com.lowcode.svintusclient.client.module.impl.cosmetics.PetsCosmeticModule.class) : null;

        for (Cosmetic cosmetic : worn) {
            CosmeticModel model = cosmetic.model();
            if (model == null || (cosmetic.type() == CosmeticType.WINGS && elytra)) {
                continue;
            }
            boolean onBack = cosmetic.type() == CosmeticType.WINGS || cosmetic.type() == CosmeticType.BODYWEAR;

            matrices.push();
            attach(model, getContextModel(), matrices, onBack && chestArmor);

            if (isLocalPlayer) {
                if (cosmetic.type() == CosmeticType.HAT && hats != null && hats.isEnabled()) {
                    matrices.translate(0.0F, hats.yOffset.get(), 0.0F);
                    float s = hats.scale.get();
                    matrices.scale(s, s, s);
                    if (hats.shouldRotate()) {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((seconds * 45.0F) % 360.0F));
                    }
                } else if (cosmetic.type() == CosmeticType.WINGS && wings != null && wings.isEnabled()) {
                    float s = wings.scale.get();
                    matrices.scale(s, s, s);
                } else if (cosmetic.type() == CosmeticType.BODYWEAR && back != null && back.isEnabled()) {
                    matrices.translate(0.0F, 0.0F, back.getZOffset());
                    float s = back.scale.get();
                    matrices.scale(s, s, s);
                } else if (cosmetic.type() == CosmeticType.PET && pets != null && pets.isEnabled()) {
                    float bob = (float) Math.sin(seconds * pets.getBobbingSpeed() * 3.0F) * 0.08F;
                    matrices.translate(0.0F, bob, 0.0F);
                }
            }

            final GeoModel geo = model.geo;
            final GeoAnimation anim = model.animation(motion, state.isInSneakingPose);
            float speedMult = (cosmetic.type() == CosmeticType.WINGS && wings != null && wings.isEnabled()) ? wings.getFlapSpeed() : 1.0F;
            final float sec = seconds * speedMult;
            queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(cosmetic.texture()), (entry, consumer) -> {
                MatrixStack boneMatrices = new MatrixStack();
                boneMatrices.multiplyPositionMatrix(entry.getPositionMatrix());
                GeoRenderer.render(geo, anim, sec, boneMatrices,
                    consumer, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
            });
            matrices.pop();
        }
    }

    private static void attach(CosmeticModel model, PlayerEntityModel player, MatrixStack matrices, boolean overArmor) {
        float yOffset = switch (model.position) {
            case HEAD -> {
                player.head.applyTransform(matrices);
                yield 0.0F;
            }
            case ABOVE_HEAD -> 0.75F;
            case BODY -> {
                player.body.applyTransform(matrices);
                yield -0.3F;
            }
            case RIGHT_ARM -> {
                player.rightArm.applyTransform(matrices);
                yield -0.25F;
            }
            case LEFT_ARM -> {
                player.leftArm.applyTransform(matrices);
                yield -0.25F;
            }
            case RIGHT_LEG -> {
                player.rightLeg.applyTransform(matrices);
                yield -0.35F;
            }
            case LEFT_LEG -> {
                player.leftLeg.applyTransform(matrices);
                yield -0.35F;
            }
            case FREE -> 0.0F;
        };

        if (overArmor) {
            matrices.translate(0.0F, 0.0F, 0.0625F);
        }
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.translate(model.x, model.y + yOffset, model.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(model.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(model.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(model.roll));
        matrices.scale(model.scale, model.scale, model.scale);
    }
}
