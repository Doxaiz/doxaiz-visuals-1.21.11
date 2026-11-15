package com.lowcode.svintusclient.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class Render3D {

    private static final List<Line> LINES = new ArrayList<>();
    private static final List<Quad> QUADS = new ArrayList<>();
    private static final List<GradientQuad> GRADIENT_QUADS = new ArrayList<>();

    public record Line(Vec3d start, Vec3d end, int color, float width) {}
    public record Quad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int color) {}
    public record GradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4) {}

    private Render3D() {}

    public static void drawLine(Vec3d start, Vec3d end, int color, float width) {
        LINES.add(new Line(start, end, color, width));
    }

    public static void drawQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int color) {
        QUADS.add(new Quad(p1, p2, p3, p4, color));
    }

    public static void drawGradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4) {
        GRADIENT_QUADS.add(new GradientQuad(p1, p2, p3, p4, c1, c2, c3, c4));
    }

    public static void drawGradientTriangle(Vec3d tip, Vec3d p1, Vec3d p2, int topColor, int color1, int color2) {
        GRADIENT_QUADS.add(new GradientQuad(tip, tip, p1, p2, topColor, topColor, color1, color2));
    }

    public static void drawBox(Box box, int color, boolean fill, boolean outline, float outlineWidth) {
        drawBox(box, color, color, fill, outline, outlineWidth);
    }

    public static void drawBox(Box box, int fillColor, int outlineColor, boolean fill, boolean outline, float outlineWidth) {
        Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);

        Vec3d p1 = new Vec3d(min.x, min.y, min.z);
        Vec3d p2 = new Vec3d(max.x, min.y, min.z);
        Vec3d p3 = new Vec3d(max.x, min.y, max.z);
        Vec3d p4 = new Vec3d(min.x, min.y, max.z);

        Vec3d p5 = new Vec3d(min.x, max.y, min.z);
        Vec3d p6 = new Vec3d(max.x, max.y, min.z);
        Vec3d p7 = new Vec3d(max.x, max.y, max.z);
        Vec3d p8 = new Vec3d(min.x, max.y, max.z);

        if (fill) {
            QUADS.add(new Quad(p1, p2, p3, p4, fillColor));
            QUADS.add(new Quad(p5, p6, p7, p8, fillColor));
            QUADS.add(new Quad(p1, p2, p6, p5, fillColor));
            QUADS.add(new Quad(p2, p3, p7, p6, fillColor));
            QUADS.add(new Quad(p3, p4, p8, p7, fillColor));
            QUADS.add(new Quad(p4, p1, p5, p8, fillColor));
        }

        if (outline) {
            drawLine(p1, p2, outlineColor, outlineWidth);
            drawLine(p2, p3, outlineColor, outlineWidth);
            drawLine(p3, p4, outlineColor, outlineWidth);
            drawLine(p4, p1, outlineColor, outlineWidth);

            drawLine(p5, p6, outlineColor, outlineWidth);
            drawLine(p6, p7, outlineColor, outlineWidth);
            drawLine(p7, p8, outlineColor, outlineWidth);
            drawLine(p8, p5, outlineColor, outlineWidth);

            drawLine(p1, p5, outlineColor, outlineWidth);
            drawLine(p2, p6, outlineColor, outlineWidth);
            drawLine(p3, p7, outlineColor, outlineWidth);
            drawLine(p4, p8, outlineColor, outlineWidth);
        }
    }

    public static void drawCircle(Vec3d center, double radius, int segments, int color, float lineWidth) {
        if (segments < 3) segments = 16;
        double step = (2.0 * Math.PI) / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            Vec3d p1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            Vec3d p2 = center.add(Math.cos(a2) * radius, 0, Math.sin(a2) * radius);
            drawLine(p1, p2, color, lineWidth);
        }
    }

    public static void drawFilledCircle(Vec3d center, double radius, int segments, int centerColor, int edgeColor) {
        if (segments < 3) segments = 24;
        double step = (2.0 * Math.PI) / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            Vec3d p1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            Vec3d p2 = center.add(Math.cos(a2) * radius, 0, Math.sin(a2) * radius);
            drawGradientTriangle(center, p1, p2, centerColor, edgeColor, edgeColor);
        }
    }

    public static void drawRotatedCube(Vec3d center, double size, double yawRad, double pitchRad, double rollRad, int fillColor, int outlineColor, boolean fill, boolean outline, float outlineWidth) {
        double hs = size * 0.5;
        Vec3d[] base = new Vec3d[]{
                new Vec3d(-hs, -hs, -hs), new Vec3d(hs, -hs, -hs), new Vec3d(hs, -hs, hs), new Vec3d(-hs, -hs, hs),
                new Vec3d(-hs, hs, -hs), new Vec3d(hs, hs, -hs), new Vec3d(hs, hs, hs), new Vec3d(-hs, hs, hs)
        };
        Vec3d[] rot = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            rot[i] = center.add(rotateVector(base[i], yawRad, pitchRad, rollRad));
        }

        if (fill) {
            QUADS.add(new Quad(rot[0], rot[1], rot[2], rot[3], fillColor));
            QUADS.add(new Quad(rot[4], rot[5], rot[6], rot[7], fillColor));
            QUADS.add(new Quad(rot[0], rot[1], rot[5], rot[4], fillColor));
            QUADS.add(new Quad(rot[1], rot[2], rot[6], rot[5], fillColor));
            QUADS.add(new Quad(rot[2], rot[3], rot[7], rot[6], fillColor));
            QUADS.add(new Quad(rot[3], rot[0], rot[4], rot[7], fillColor));
        }

        if (outline) {
            drawLine(rot[0], rot[1], outlineColor, outlineWidth);
            drawLine(rot[1], rot[2], outlineColor, outlineWidth);
            drawLine(rot[2], rot[3], outlineColor, outlineWidth);
            drawLine(rot[3], rot[0], outlineColor, outlineWidth);

            drawLine(rot[4], rot[5], outlineColor, outlineWidth);
            drawLine(rot[5], rot[6], outlineColor, outlineWidth);
            drawLine(rot[6], rot[7], outlineColor, outlineWidth);
            drawLine(rot[7], rot[4], outlineColor, outlineWidth);

            drawLine(rot[0], rot[4], outlineColor, outlineWidth);
            drawLine(rot[1], rot[5], outlineColor, outlineWidth);
            drawLine(rot[2], rot[6], outlineColor, outlineWidth);
            drawLine(rot[3], rot[7], outlineColor, outlineWidth);
        }
    }

    private static Vec3d rotateVector(Vec3d v, double yaw, double pitch, double roll) {
        // Roll around Z
        double cosR = Math.cos(roll);
        double sinR = Math.sin(roll);
        double x1 = v.x * cosR - v.y * sinR;
        double y1 = v.x * sinR + v.y * cosR;
        double z1 = v.z;

        // Pitch around X
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double x2 = x1;
        double y2 = y1 * cosP - z1 * sinP;
        double z2 = y1 * sinP + z1 * cosP;

        // Yaw around Y
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double x3 = x2 * cosY + z2 * sinY;
        double y3 = y2;
        double z3 = -x2 * sinY + z2 * cosY;

        return new Vec3d(x3, y3, z3);
    }

    public static void render(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Vec3d cameraPos) {
        if (!LINES.isEmpty()) {
            VertexConsumer lineConsumer = consumers.getBuffer(RenderLayers.lines());
            MatrixStack.Entry entry = matrices.peek();
            Matrix4f positionMatrix = entry.getPositionMatrix();

            for (Line l : LINES) {
                float x1 = (float) (l.start.x - cameraPos.x);
                float y1 = (float) (l.start.y - cameraPos.y);
                float z1 = (float) (l.start.z - cameraPos.z);

                float x2 = (float) (l.end.x - cameraPos.x);
                float y2 = (float) (l.end.y - cameraPos.y);
                float z2 = (float) (l.end.z - cameraPos.z);

                int c = l.color;
                int a = (c >> 24) & 0xFF;
                if (a == 0) a = 255;
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;

                Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
                if (normal.lengthSquared() > 0.00001f) {
                    normal.normalize();
                } else {
                    normal.set(0, 1, 0);
                }

                float lw = l.width > 0 ? l.width : 1.0f;

                lineConsumer.vertex(entry, x1, y1, z1)
                        .color(r, g, b, a)
                        .normal(entry, normal.x, normal.y, normal.z)
                        .lineWidth(lw);

                lineConsumer.vertex(entry, x2, y2, z2)
                        .color(r, g, b, a)
                        .normal(entry, normal.x, normal.y, normal.z)
                        .lineWidth(lw);
            }
            LINES.clear();
        }

        if (!QUADS.isEmpty()) {
            VertexConsumer quadConsumer = consumers.getBuffer(RenderLayers.lightning());
            MatrixStack.Entry entry = matrices.peek();
            Matrix4f positionMatrix = entry.getPositionMatrix();

            for (Quad q : QUADS) {
                int c = q.color;
                int a = (c >> 24) & 0xFF;
                if (a == 0) a = 255;
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;

                putQuadVertex(quadConsumer, positionMatrix, q.p1, cameraPos, r, g, b, a);
                putQuadVertex(quadConsumer, positionMatrix, q.p2, cameraPos, r, g, b, a);
                putQuadVertex(quadConsumer, positionMatrix, q.p3, cameraPos, r, g, b, a);
                putQuadVertex(quadConsumer, positionMatrix, q.p4, cameraPos, r, g, b, a);
            }
            QUADS.clear();
        }

        if (!GRADIENT_QUADS.isEmpty()) {
            VertexConsumer quadConsumer = consumers.getBuffer(RenderLayers.lightning());
            MatrixStack.Entry entry = matrices.peek();
            Matrix4f positionMatrix = entry.getPositionMatrix();

            for (GradientQuad gq : GRADIENT_QUADS) {
                putColorVertex(quadConsumer, positionMatrix, gq.p1, cameraPos, gq.c1);
                putColorVertex(quadConsumer, positionMatrix, gq.p2, cameraPos, gq.c2);
                putColorVertex(quadConsumer, positionMatrix, gq.p3, cameraPos, gq.c3);
                putColorVertex(quadConsumer, positionMatrix, gq.p4, cameraPos, gq.c4);
            }
            GRADIENT_QUADS.clear();
        }
    }

    private static void putQuadVertex(VertexConsumer consumer, Matrix4f mat, Vec3d pos, Vec3d cam, int r, int g, int b, int a) {
        consumer.vertex(mat, (float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z))
                .color(r, g, b, a);
    }

    private static void putColorVertex(VertexConsumer consumer, Matrix4f mat, Vec3d pos, Vec3d cam, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        consumer.vertex(mat, (float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z))
                .color(r, g, b, a);
    }

    public static void drawBillboardTexture(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Camera camera,
            Identifier texture,
            Vec3d pos,
            float sizeX,
            float sizeY,
            float rotation,
            int color
    ) {
        if (consumers == null || camera == null || texture == null) return;
        RenderLayer layer = RenderLayers.entityTranslucent(texture);
        VertexConsumer buffer = consumers.getBuffer(layer);

        Vec3d camPos = camera.getCameraPos();
        float halfX = sizeX * 0.5f;
        float halfY = sizeY * 0.5f;

        matrices.push();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        matrices.multiply(camera.getRotation());
        if (rotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }

        MatrixStack.Entry entry = matrices.peek();

        int a = (color >>> 24) & 0xFF;
        if (a == 0) a = 255;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        buffer.vertex(entry, -halfX, -halfY, 0.0f).color(r, g, b, a).texture(0.0f, 1.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
        buffer.vertex(entry, halfX, -halfY, 0.0f).color(r, g, b, a).texture(1.0f, 1.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
        buffer.vertex(entry, halfX, halfY, 0.0f).color(r, g, b, a).texture(1.0f, 0.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
        buffer.vertex(entry, -halfX, halfY, 0.0f).color(r, g, b, a).texture(0.0f, 0.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);

        matrices.pop();
    }

    public static void drawGroundTexture(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Camera camera,
            Identifier texture,
            Vec3d pos,
            float radius,
            float rotation,
            int color
    ) {
        if (consumers == null || camera == null || texture == null) return;
        RenderLayer layer = RenderLayers.entityTranslucent(texture);
        VertexConsumer buffer = consumers.getBuffer(layer);

        Vec3d camPos = camera.getCameraPos();

        matrices.push();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y + 0.02, pos.z - camPos.z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        if (rotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }

        MatrixStack.Entry entry = matrices.peek();

        int a = (color >>> 24) & 0xFF;
        if (a == 0) a = 255;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        buffer.vertex(entry, -radius, -radius, 0.0f).color(r, g, b, a).texture(0.0f, 1.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 0.0f, 1.0f);
        buffer.vertex(entry, radius, -radius, 0.0f).color(r, g, b, a).texture(1.0f, 1.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 0.0f, 1.0f);
        buffer.vertex(entry, radius, radius, 0.0f).color(r, g, b, a).texture(1.0f, 0.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 0.0f, 1.0f);
        buffer.vertex(entry, -radius, radius, 0.0f).color(r, g, b, a).texture(0.0f, 0.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 0.0f, 1.0f);

        matrices.pop();
    }

    public static Vec3d interpolate(LivingEntity entity, float tickDelta) {
        return entity.getLerpedPos(tickDelta);
    }
}
