package com.lowcode.lowcosmetic.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GeoModelParser {
    private static final String[] FACES = {"west", "east", "down", "up", "north", "south"};
    private static final float PLANE_OFFSET = 0.002F;

    private GeoModelParser() {
    }

    @FunctionalInterface
    public interface AlphaTest {
        AlphaTest ALWAYS = (u0, v0, u1, v1) -> true;

        boolean visible(float u0, float v0, float u1, float v1);
    }

    public static GeoModel parse(JsonObject root) {
        return parse(root, AlphaTest.ALWAYS);
    }

    public static GeoModel parse(JsonObject root, AlphaTest alpha) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) {
            throw new IllegalArgumentException("No minecraft:geometry in model");
        }

        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        GeoModel model = new GeoModel();
        if (description != null) {
            model.textureWidth = getInt(description, "texture_width", 64);
            model.textureHeight = getInt(description, "texture_height", 64);
        }

        JsonArray bones = geometry.getAsJsonArray("bones");
        if (bones == null) {
            return model;
        }

        Map<String, GeoBone> byName = new HashMap<>();
        for (JsonElement element : bones) {
            GeoBone bone = parseBone(element.getAsJsonObject(), model.textureWidth, model.textureHeight, alpha);
            byName.put(bone.name, bone);
        }

        for (JsonElement element : bones) {
            JsonObject json = element.getAsJsonObject();
            GeoBone bone = byName.get(json.get("name").getAsString());
            GeoBone parent = json.has("parent") ? byName.get(json.get("parent").getAsString()) : null;
            if (parent != null) {
                parent.children.add(bone);
            } else {
                model.roots.add(bone);
            }
        }

        return model;
    }

    private static GeoBone parseBone(JsonObject json, int texW, int texH, AlphaTest alpha) {
        GeoBone bone = new GeoBone(json.get("name").getAsString());
        float[] pivot = getFloats(json, "pivot", new float[]{0, 0, 0});
        bone.pivotX = -pivot[0];
        bone.pivotY = pivot[1];
        bone.pivotZ = pivot[2];

        float[] rotation = getFloats(json, "rotation", new float[]{0, 0, 0});
        bone.rotX = (float) Math.toRadians(-rotation[0]);
        bone.rotY = (float) Math.toRadians(-rotation[1]);
        bone.rotZ = (float) Math.toRadians(rotation[2]);

        if (json.has("cubes")) {
            for (JsonElement cube : json.getAsJsonArray("cubes")) {
                GeoCube parsed = parseCube(cube.getAsJsonObject(), texW, texH, alpha);
                if (!parsed.quads.isEmpty()) {
                    bone.cubes.add(parsed);
                }
            }
        }

        return bone;
    }

    private static GeoCube parseCube(JsonObject json, int texW, int texH, AlphaTest alpha) {
        float[] origin = getFloats(json, "origin", new float[]{0, 0, 0});
        float[] size = getFloats(json, "size", new float[]{1, 1, 1});
        float[] pivot = getFloats(json, "pivot", origin.clone());
        float[] rotation = getFloats(json, "rotation", new float[]{0, 0, 0});
        float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;
        boolean mirror = json.has("mirror") && json.get("mirror").getAsBoolean();

        GeoCube cube = new GeoCube(size[0], size[1], size[2]);
        GeoQuad[] quads = buildQuads(json, origin, size, inflate, mirror, texW, texH);
        for (int i = 0; i < quads.length; i++) {
            GeoQuad q = quads[i];
            if (q != null && !alpha.visible(min(q.u), min(q.v), max(q.u), max(q.v))) {
                quads[i] = null;
            }
        }

        float rx = (float) Math.toRadians(-rotation[0]);
        float ry = (float) Math.toRadians(-rotation[1]);
        float rz = (float) Math.toRadians(rotation[2]);
        if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
            Quaternionf q = new Quaternionf().rotateZ(rz).rotateY(ry).rotateX(rx);
            float px = -pivot[0] / 16.0F;
            float py = pivot[1] / 16.0F;
            float pz = pivot[2] / 16.0F;
            Vector3f tmp = new Vector3f();
            for (GeoQuad quad : quads) {
                if (quad == null) {
                    continue;
                }
                q.transform(tmp.set(quad.nx, quad.ny, quad.nz));
                quad.nx = tmp.x;
                quad.ny = tmp.y;
                quad.nz = tmp.z;
                for (int i = 0; i < 4; i++) {
                    q.transform(tmp.set(quad.x[i] - px, quad.y[i] - py, quad.z[i] - pz));
                    quad.x[i] = tmp.x + px;
                    quad.y[i] = tmp.y + py;
                    quad.z[i] = tmp.z + pz;
                }
            }
        }

        for (GeoQuad quad : quads) {
            if (quad != null) {
                cube.quads.add(quad);
            }
        }
        return cube;
    }

    private static GeoQuad[] buildQuads(JsonObject json, float[] origin, float[] size, float inflate, boolean mirror, int texW, int texH) {
        float ox = origin[0] - inflate;
        float oy = origin[1] - inflate;
        float oz = origin[2] - inflate;
        float sx = size[0] + inflate * 2.0F;
        float sy = size[1] + inflate * 2.0F;
        float sz = size[2] + inflate * 2.0F;

        float x0 = -(ox + sx) / 16.0F;
        float y0 = oy / 16.0F;
        float z0 = oz / 16.0F;
        float x1 = -ox / 16.0F;
        float y1 = (oy + sy) / 16.0F;
        float z1 = (oz + sz) / 16.0F;

        GeoQuad[] quads = new GeoQuad[6];
        JsonElement uv = json.get("uv");
        if (uv != null && uv.isJsonObject()) {
            JsonObject faces = uv.getAsJsonObject();
            for (int i = 0; i < 6; i++) {
                quads[i] = perFaceQuad(faces, FACES[i], x0, y0, z0, x1, y1, z1, texW, texH);
            }
        } else {
            float u = 0.0F;
            float v = 0.0F;
            if (uv != null && uv.isJsonArray()) {
                u = uv.getAsJsonArray().get(0).getAsFloat();
                v = uv.getAsJsonArray().get(1).getAsFloat();
            }
            for (int i = 0; i < 6; i++) {
                quads[i] = boxUvQuad(FACES[i], x0, y0, z0, x1, y1, z1, u, v, size, mirror, texW, texH);
            }
        }

        if (sx == 0.0F) {
            quads[2] = quads[3] = quads[4] = quads[5] = null;
            nudge(quads[0], -PLANE_OFFSET, 0, 0);
            nudge(quads[1], PLANE_OFFSET, 0, 0);
        }
        if (sy == 0.0F) {
            quads[0] = quads[1] = quads[4] = quads[5] = null;
            nudge(quads[2], 0, -PLANE_OFFSET, 0);
            nudge(quads[3], 0, PLANE_OFFSET, 0);
        }
        if (sz == 0.0F) {
            quads[0] = quads[1] = quads[2] = quads[3] = null;
            nudge(quads[4], 0, 0, -PLANE_OFFSET);
            nudge(quads[5], 0, 0, PLANE_OFFSET);
        }
        return quads;
    }

    private static void nudge(GeoQuad quad, float dx, float dy, float dz) {
        if (quad == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            quad.x[i] += dx;
            quad.y[i] += dy;
            quad.z[i] += dz;
        }
    }

    private static GeoQuad perFaceQuad(JsonObject faces, String face, float x0, float y0, float z0, float x1, float y1, float z1, int texW, int texH) {
        if (!faces.has(face)) {
            return null;
        }
        JsonObject json = faces.getAsJsonObject(face);
        if (!json.has("uv") || !json.has("uv_size")) {
            return null;
        }
        JsonArray uv = json.getAsJsonArray("uv");
        JsonArray uvSize = json.getAsJsonArray("uv_size");
        float u = uv.get(0).getAsFloat();
        float v = uv.get(1).getAsFloat();
        float w = uvSize.get(0).getAsFloat();
        float h = uvSize.get(1).getAsFloat();
        if (w == 0.0F || h == 0.0F || u < 0.0F || v < 0.0F) {
            return null;
        }
        return faceQuad(face, x0, y0, z0, x1, y1, z1, u / texW, v / texH, w / texW, h / texH);
    }

    private static GeoQuad boxUvQuad(String face, float x0, float y0, float z0, float x1, float y1, float z1,
                                     float u, float v, float[] size, boolean mirror, int texW, int texH) {
        float sx = size[0];
        float sy = size[1];
        float sz = size[2];
        String region = face;
        if (mirror && face.equals("east")) {
            region = "west";
        } else if (mirror && face.equals("west")) {
            region = "east";
        }

        float fu;
        float fv;
        float fw;
        float fh;
        switch (region) {
            case "east" -> { fu = u; fv = v + sz; fw = sz; fh = sy; }
            case "north" -> { fu = u + sz; fv = v + sz; fw = sx; fh = sy; }
            case "west" -> { fu = u + sz + sx; fv = v + sz; fw = sz; fh = sy; }
            case "south" -> { fu = u + sz + sx + sz; fv = v + sz; fw = sx; fh = sy; }
            case "up" -> { fu = u + sz; fv = v; fw = sx; fh = sz; }
            case "down" -> { fu = u + sz + sx; fv = v + sz; fw = sx; fh = -sz; }
            default -> { return null; }
        }
        if (mirror) {
            fu += fw;
            fw = -fw;
        }
        return faceQuad(face, x0, y0, z0, x1, y1, z1, fu / texW, fv / texH, fw / texW, fh / texH);
    }

    private static GeoQuad faceQuad(String face, float x0, float y0, float z0, float x1, float y1, float z1,
                                    float u0, float v0, float uw, float vh) {
        float u1 = u0 + uw;
        float v1 = v0 + vh;
        GeoQuad q;
        switch (face) {
            case "north" -> {
                q = new GeoQuad(0, 0, -1);
                q.set(0, x1, y1, z0, u0, v0);
                q.set(1, x0, y1, z0, u1, v0);
                q.set(2, x0, y0, z0, u1, v1);
                q.set(3, x1, y0, z0, u0, v1);
            }
            case "south" -> {
                q = new GeoQuad(0, 0, 1);
                q.set(0, x0, y1, z1, u0, v0);
                q.set(1, x1, y1, z1, u1, v0);
                q.set(2, x1, y0, z1, u1, v1);
                q.set(3, x0, y0, z1, u0, v1);
            }
            case "east" -> {
                q = new GeoQuad(1, 0, 0);
                q.set(0, x1, y1, z1, u0, v0);
                q.set(1, x1, y1, z0, u1, v0);
                q.set(2, x1, y0, z0, u1, v1);
                q.set(3, x1, y0, z1, u0, v1);
            }
            case "west" -> {
                q = new GeoQuad(-1, 0, 0);
                q.set(0, x0, y1, z0, u0, v0);
                q.set(1, x0, y1, z1, u1, v0);
                q.set(2, x0, y0, z1, u1, v1);
                q.set(3, x0, y0, z0, u0, v1);
            }
            case "up" -> {
                q = new GeoQuad(0, 1, 0);
                q.set(0, x0, y1, z0, u0, v0);
                q.set(1, x0, y1, z1, u0, v1);
                q.set(2, x1, y1, z1, u1, v1);
                q.set(3, x1, y1, z0, u1, v0);
            }
            case "down" -> {
                q = new GeoQuad(0, -1, 0);
                q.set(0, x1, y0, z0, u0, v0);
                q.set(1, x1, y0, z1, u0, v1);
                q.set(2, x0, y0, z1, u1, v1);
                q.set(3, x0, y0, z0, u1, v0);
            }
            default -> q = null;
        }
        return q;
    }

    private static float min(float[] values) {
        return Math.min(Math.min(values[0], values[1]), Math.min(values[2], values[3]));
    }

    private static float max(float[] values) {
        return Math.max(Math.max(values[0], values[1]), Math.max(values[2], values[3]));
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static float[] getFloats(JsonObject json, String key, float[] fallback) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray array = json.getAsJsonArray(key);
        float[] out = new float[Math.max(3, array.size())];
        for (int i = 0; i < array.size(); i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }
}
