package com.lowcode.lowcosmetic.client.cosmetic;

import com.google.gson.JsonObject;
import com.lowcode.lowcosmetic.client.geo.GeoAnimation;
import com.lowcode.lowcosmetic.client.geo.GeoModel;
import com.lowcode.lowcosmetic.client.geo.GeoModelParser;
import com.lowcode.lowcosmetic.client.render.GeoRenderer;
import java.util.Locale;
import java.util.Map;

public final class CosmeticModel {
    public enum Motion { IDLE, MOVING, GLIDING, SWIMMING }

    private static final String[] IDLE_KEYS = {"idle", "main", "animation", "anim"};
    private static final String[] MOVING_KEYS = {"walk", "walking", "moving", "move", "run"};
    private static final String[] GLIDING_KEYS = {"elytra"};
    private static final String[] SWIMMING_KEYS = {"swimming", "swimmings", "swim"};
    private static final String[] NOT_IDLE = {"gui", "water", "sneak", "elytra", "swim", "afk", "fly", "deep", "surface", "walk", "moving", "run"};

    public final GeoModel geo;
    public final ModelPosition position;
    public final float x;
    public final float y;
    public final float z;
    public final float yaw;
    public final float pitch;
    public final float roll;
    public final float scale;
    public final float[] bounds;

    private final Map<String, GeoAnimation> animations;
    private final GeoAnimation[] picked = new GeoAnimation[Motion.values().length * 2];
    private final boolean[] resolved = new boolean[picked.length];

    private CosmeticModel(JsonObject json, GeoModel geo, Map<String, GeoAnimation> animations) {
        this.geo = geo;
        this.animations = animations;
        this.position = json.has("pos") ? ModelPosition.byId(json.get("pos").getAsInt()) : ModelPosition.HEAD;
        this.x = getFloat(json, "x", 0.0F);
        this.y = getFloat(json, "y", 0.0F);
        this.z = getFloat(json, "z", 0.0F);
        this.yaw = getFloat(json, "yaw", 0.0F);
        this.pitch = getFloat(json, "pitch", 0.0F);
        this.roll = getFloat(json, "roll", 0.0F);
        this.scale = getFloat(json, "scale", 1.0F);
        this.bounds = GeoRenderer.bounds(geo, animation(Motion.IDLE, false));
    }

    public static CosmeticModel parse(JsonObject json, GeoModelParser.AlphaTest alpha) {
        GeoModel geo = GeoModelParser.parse(json.getAsJsonObject("model"), alpha);
        Map<String, GeoAnimation> animations = GeoAnimation.parseAll(json.has("animation") ? json.getAsJsonObject("animation") : null);
        return new CosmeticModel(json, geo, animations);
    }

    public boolean hasAnimations() {
        return !animations.isEmpty();
    }

    public GeoAnimation animation(Motion motion, boolean sneaking) {
        int slot = motion.ordinal() * 2 + (sneaking ? 1 : 0);
        if (!resolved[slot]) {
            picked[slot] = resolve(motion, sneaking);
            resolved[slot] = true;
        }
        return picked[slot];
    }

    private GeoAnimation resolve(Motion motion, boolean sneaking) {
        if (animations.isEmpty()) {
            return null;
        }

        String base = switch (motion) {
            case MOVING -> find(MOVING_KEYS);
            case GLIDING -> find(GLIDING_KEYS);
            case SWIMMING -> find(SWIMMING_KEYS);
            case IDLE -> null;
        };
        if (base == null) {
            base = idleName();
        }

        if (sneaking && animations.containsKey(base + ".sneak")) {
            return animations.get(base + ".sneak");
        }
        return animations.get(base);
    }

    private String idleName() {
        String byKey = find(IDLE_KEYS);
        if (byKey != null) {
            return byKey;
        }
        for (String name : animations.keySet()) {
            String key = baseKey(name);
            if (!name.contains(".") && !containsAny(key, NOT_IDLE)) {
                return name;
            }
        }
        return animations.keySet().iterator().next();
    }

    private String find(String[] keys) {
        for (String wanted : keys) {
            for (String name : animations.keySet()) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("animation.")) {
                    if (baseKey(name).equals(wanted)) {
                        return name;
                    }
                } else if (lower.equals(wanted)) {
                    return name;
                }
            }
        }
        return null;
    }

    private static String baseKey(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("animation.")) {
            return lower.substring(lower.lastIndexOf('.') + 1);
        }
        int dot = lower.indexOf('.');
        return dot >= 0 ? lower.substring(0, dot) : lower;
    }

    private static boolean containsAny(String value, String[] needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static float getFloat(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }
}
