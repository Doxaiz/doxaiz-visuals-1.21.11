package com.lowcode.lowcosmetic.client.render;

import com.lowcode.lowcosmetic.client.geo.GeoAnimation;
import com.lowcode.lowcosmetic.client.geo.GeoBone;
import com.lowcode.lowcosmetic.client.geo.GeoCube;
import com.lowcode.lowcosmetic.client.geo.GeoModel;
import com.lowcode.lowcosmetic.client.geo.GeoQuad;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GeoRenderer {
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final float[] SAMPLE = new float[3];
    private static final Vector3f POS = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Quaternionf ROTATION = new Quaternionf();

    private GeoRenderer() {
    }

    public static void render(GeoModel model, GeoAnimation animation, float seconds, MatrixStack matrices,
                              VertexConsumer consumer, int light, int overlay, int color) {
        float time = animation != null ? animation.localTime(seconds) : 0.0F;
        QuadSink sink = (entry, cube, quad) -> emit(entry, cube, quad, consumer, light, overlay, color);
        for (GeoBone bone : model.roots) {
            renderBone(bone, animation, time, matrices, sink);
        }
    }

    public static float[] bounds(GeoModel model, GeoAnimation animation) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        MatrixStack matrices = new MatrixStack();
        Vector3f p = new Vector3f();
        QuadSink sink = (entry, cube, quad) -> {
            Matrix4f m = entry.getPositionMatrix();
            for (int i = 0; i < 4; i++) {
                m.transformPosition(quad.x[i], quad.y[i], quad.z[i], p);
                b[0] = Math.min(b[0], p.x);
                b[1] = Math.min(b[1], p.y);
                b[2] = Math.min(b[2], p.z);
                b[3] = Math.max(b[3], p.x);
                b[4] = Math.max(b[4], p.y);
                b[5] = Math.max(b[5], p.z);
            }
        };
        int frames = animation != null && animation.length > 0.0F ? 8 : 1;
        for (int frame = 0; frame < frames; frame++) {
            float time = animation != null ? animation.length * frame / frames : 0.0F;
            for (GeoBone bone : model.roots) {
                renderBone(bone, animation, time, matrices, sink);
            }
        }
        if (b[0] > b[3]) {
            return new float[]{-0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F};
        }
        return b;
    }

    private static void renderBone(GeoBone bone, GeoAnimation animation, float time, MatrixStack matrices, QuadSink sink) {
        float rx = bone.rotX;
        float ry = bone.rotY;
        float rz = bone.rotZ;
        float px = 0.0F;
        float py = 0.0F;
        float pz = 0.0F;
        float sx = 1.0F;
        float sy = 1.0F;
        float sz = 1.0F;

        GeoAnimation.BoneTrack track = animation != null ? animation.track(bone.name) : null;
        if (track != null) {
            if (track.rotation() != null) {
                track.rotation().sample(time, SAMPLE);
                rx += -SAMPLE[0] * DEG;
                ry += -SAMPLE[1] * DEG;
                rz += SAMPLE[2] * DEG;
            }
            if (track.position() != null) {
                track.position().sample(time, SAMPLE);
                px = SAMPLE[0];
                py = SAMPLE[1];
                pz = SAMPLE[2];
            }
            if (track.scale() != null) {
                track.scale().sample(time, SAMPLE);
                sx = SAMPLE[0];
                sy = SAMPLE[1];
                sz = SAMPLE[2];
                if (Math.abs(sx) < 1.0E-4F || Math.abs(sy) < 1.0E-4F || Math.abs(sz) < 1.0E-4F) {
                    return;
                }
            }
        }

        matrices.push();
        if (px != 0.0F || py != 0.0F || pz != 0.0F) {
            matrices.translate(-px / 16.0F, py / 16.0F, pz / 16.0F);
        }
        matrices.translate(bone.pivotX / 16.0F, bone.pivotY / 16.0F, bone.pivotZ / 16.0F);
        if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
            matrices.multiply(ROTATION.rotationZYX(rz, ry, rx));
        }
        if (sx != 1.0F || sy != 1.0F || sz != 1.0F) {
            matrices.scale(sx, sy, sz);
        }
        matrices.translate(-bone.pivotX / 16.0F, -bone.pivotY / 16.0F, -bone.pivotZ / 16.0F);

        MatrixStack.Entry entry = matrices.peek();
        for (GeoCube cube : bone.cubes) {
            for (GeoQuad quad : cube.quads) {
                sink.accept(entry, cube, quad);
            }
        }
        for (GeoBone child : bone.children) {
            renderBone(child, animation, time, matrices, sink);
        }
        matrices.pop();
    }

    private static void emit(MatrixStack.Entry entry, GeoCube cube, GeoQuad quad, VertexConsumer consumer, int light, int overlay, int color) {
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        normalMatrix.transform(NORMAL.set(quad.nx, quad.ny, quad.nz)).normalize();
        float nx = NORMAL.x;
        float ny = NORMAL.y;
        float nz = NORMAL.z;
        if ((cube.flatY || cube.flatZ) && nx < 0.0F) {
            nx = -nx;
        }
        if ((cube.flatX || cube.flatZ) && ny < 0.0F) {
            ny = -ny;
        }
        if ((cube.flatX || cube.flatY) && nz < 0.0F) {
            nz = -nz;
        }
        for (int i = 0; i < 4; i++) {
            position.transformPosition(quad.x[i], quad.y[i], quad.z[i], POS);
            consumer.vertex(POS.x, POS.y, POS.z, color, quad.u[i], quad.v[i], overlay, light, nx, ny, nz);
        }
    }

    @FunctionalInterface
    private interface QuadSink {
        void accept(MatrixStack.Entry entry, GeoCube cube, GeoQuad quad);
    }
}
