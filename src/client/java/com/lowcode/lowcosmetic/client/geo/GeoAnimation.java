package com.lowcode.lowcosmetic.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeoAnimation {
    public final String name;
    public final boolean loop;
    public final float length;
    private final Map<String, BoneTrack> bones;

    private GeoAnimation(String name, boolean loop, float length, Map<String, BoneTrack> bones) {
        this.name = name;
        this.loop = loop;
        this.length = length;
        this.bones = bones;
    }

    public BoneTrack track(String bone) {
        return bones.get(bone);
    }

    public float localTime(float seconds) {
        if (length <= 0.0F) {
            return 0.0F;
        }
        return loop ? seconds % length : Math.min(seconds, length);
    }

    public static Map<String, GeoAnimation> parseAll(JsonObject root) {
        Map<String, GeoAnimation> result = new LinkedHashMap<>();
        if (root == null || !root.has("animations")) {
            return result;
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("animations").entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            boolean loop = json.has("loop") && isTruthy(json.get("loop"));
            float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 0.0F;
            Map<String, BoneTrack> tracks = new HashMap<>();
            float lastKey = 0.0F;
            if (json.has("bones")) {
                for (Map.Entry<String, JsonElement> bone : json.getAsJsonObject("bones").entrySet()) {
                    JsonObject channels = bone.getValue().getAsJsonObject();
                    BoneTrack track = new BoneTrack(
                        Channel.parse(channels.get("rotation"), 0.0F),
                        Channel.parse(channels.get("position"), 0.0F),
                        Channel.parse(channels.get("scale"), 1.0F)
                    );
                    tracks.put(bone.getKey(), track);
                    lastKey = Math.max(lastKey, track.lastKeyTime());
                }
            }
            if (length <= 0.0F) {
                length = lastKey;
            }
            result.put(entry.getKey(), new GeoAnimation(entry.getKey(), loop, length, tracks));
        }
        return result;
    }

    private static boolean isTruthy(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        return false;
    }

    public record BoneTrack(Channel rotation, Channel position, Channel scale) {
        float lastKeyTime() {
            float t = 0.0F;
            for (Channel c : new Channel[]{rotation, position, scale}) {
                if (c != null && c.times.length > 0) {
                    t = Math.max(t, c.times[c.times.length - 1]);
                }
            }
            return t;
        }
    }

    public static final class Channel {
        final float[] times;
        final float[][] pre;
        final float[][] post;
        final byte[] mode;
        final String[] easing;

        private static final byte LINEAR = 0;
        private static final byte CATMULLROM = 1;
        private static final byte STEP = 2;

        private Channel(float[] times, float[][] pre, float[][] post, byte[] mode, String[] easing) {
            this.times = times;
            this.pre = pre;
            this.post = post;
            this.mode = mode;
            this.easing = easing;
        }

        public void sample(float t, float[] out) {
            int n = times.length;
            if (n == 1 || t <= times[0]) {
                copy(n == 1 ? post[0] : pre[0], out);
                return;
            }
            if (t >= times[n - 1]) {
                copy(post[n - 1], out);
                return;
            }

            int i = 0;
            while (i < n - 2 && t >= times[i + 1]) {
                i++;
            }

            float[] a = post[i];
            float[] b = pre[i + 1];
            float span = times[i + 1] - times[i];
            float f = span <= 0.0F ? 1.0F : (t - times[i]) / span;
            if (mode[i] == STEP) {
                copy(a, out);
                return;
            }
            f = ease(easing[i + 1], f);

            if (mode[i] == CATMULLROM || mode[i + 1] == CATMULLROM) {
                float[] p0 = i > 0 ? post[i - 1] : a;
                float[] p3 = i + 2 < n ? pre[i + 2] : b;
                for (int k = 0; k < 3; k++) {
                    out[k] = catmullRom(p0[k], a[k], b[k], p3[k], f);
                }
            } else {
                for (int k = 0; k < 3; k++) {
                    out[k] = a[k] + (b[k] - a[k]) * f;
                }
            }
        }

        private static void copy(float[] src, float[] out) {
            out[0] = src[0];
            out[1] = src[1];
            out[2] = src[2];
        }

        private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
            float t2 = t * t;
            float t3 = t2 * t;
            return 0.5F * ((2.0F * p1) + (-p0 + p2) * t + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2 + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
        }

        static Channel parse(JsonElement json, float neutral) {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            if (json.isJsonArray() || json.isJsonPrimitive()) {
                float[] v = vector(json, neutral);
                return new Channel(new float[]{0.0F}, new float[][]{v}, new float[][]{v}, new byte[]{LINEAR}, new String[]{null});
            }

            JsonObject object = json.getAsJsonObject();
            if (object.has("vector")) {
                float[] v = vector(object.get("vector"), neutral);
                return new Channel(new float[]{0.0F}, new float[][]{v}, new float[][]{v}, new byte[]{LINEAR}, new String[]{null});
            }

            List<Map.Entry<Float, JsonElement>> keys = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                try {
                    keys.add(Map.entry(Float.parseFloat(entry.getKey()), entry.getValue()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (keys.isEmpty()) {
                return null;
            }
            keys.sort(Map.Entry.comparingByKey());

            int n = keys.size();
            float[] times = new float[n];
            float[][] pre = new float[n][];
            float[][] post = new float[n][];
            byte[] mode = new byte[n];
            String[] easing = new String[n];
            for (int i = 0; i < n; i++) {
                times[i] = keys.get(i).getKey();
                JsonElement value = keys.get(i).getValue();
                if (value.isJsonObject()) {
                    JsonObject key = value.getAsJsonObject();
                    if (key.has("vector")) {
                        pre[i] = post[i] = vector(key.get("vector"), neutral);
                    } else {
                        JsonElement preJson = key.has("pre") ? key.get("pre") : key.get("post");
                        JsonElement postJson = key.has("post") ? key.get("post") : key.get("pre");
                        pre[i] = vector(unwrap(preJson), neutral);
                        post[i] = vector(unwrap(postJson), neutral);
                    }
                    String lerp = key.has("lerp_mode") ? key.get("lerp_mode").getAsString() : "linear";
                    mode[i] = switch (lerp) {
                        case "catmullrom" -> CATMULLROM;
                        case "step" -> STEP;
                        default -> LINEAR;
                    };
                    if (key.has("easing")) {
                        easing[i] = key.get("easing").getAsString();
                    }
                } else {
                    pre[i] = post[i] = vector(value, neutral);
                }
            }
            return new Channel(times, pre, post, mode, easing);
        }

        private static JsonElement unwrap(JsonElement element) {
            if (element != null && element.isJsonObject() && element.getAsJsonObject().has("vector")) {
                return element.getAsJsonObject().get("vector");
            }
            return element;
        }

        private static float[] vector(JsonElement json, float neutral) {
            if (json == null || json.isJsonNull()) {
                return new float[]{neutral, neutral, neutral};
            }
            if (json.isJsonPrimitive()) {
                float v = number(json.getAsJsonPrimitive(), neutral);
                return new float[]{v, v, v};
            }
            if (json.isJsonArray()) {
                JsonArray array = json.getAsJsonArray();
                float[] out = {neutral, neutral, neutral};
                for (int i = 0; i < Math.min(3, array.size()); i++) {
                    out[i] = array.get(i).isJsonPrimitive() ? number(array.get(i).getAsJsonPrimitive(), neutral) : neutral;
                }
                return out;
            }
            return new float[]{neutral, neutral, neutral};
        }

        private static float number(JsonPrimitive primitive, float neutral) {
            if (primitive.isNumber()) {
                return primitive.getAsFloat();
            }
            try {
                return Float.parseFloat(primitive.getAsString().trim());
            } catch (NumberFormatException e) {
                return neutral;
            }
        }

        private static float ease(String name, float t) {
            if (name == null) {
                return t;
            }
            return switch (name) {
                case "easeInSine" -> (float) (1 - Math.cos(t * Math.PI / 2));
                case "easeOutSine" -> (float) Math.sin(t * Math.PI / 2);
                case "easeInOutSine" -> (float) (-(Math.cos(Math.PI * t) - 1) / 2);
                case "easeInQuad" -> t * t;
                case "easeOutQuad" -> 1 - (1 - t) * (1 - t);
                case "easeInOutQuad" -> t < 0.5F ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
                case "easeInCubic" -> t * t * t;
                case "easeOutCubic" -> 1 - (float) Math.pow(1 - t, 3);
                case "easeInOutCubic" -> t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
                case "step" -> t < 1.0F ? 0.0F : 1.0F;
                default -> t;
            };
        }
    }
}
