package com.lowcode.lowcosmetic.client.gui;

public final class Anim {
    private float value;
    private float target;
    private final float speed;

    public Anim(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public Anim target(float target) {
        this.target = target;
        return this;
    }

    public float target() {
        return target;
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
    }

    public float update(float deltaSeconds) {
        value += (target - value) * (1.0F - (float) Math.exp(-speed * deltaSeconds));
        if (Math.abs(target - value) < 1.0E-4F) {
            value = target;
        }
        return value;
    }

    public float get() {
        return value;
    }

    public static float easeOutCubic(float t) {
        float inv = 1.0F - Math.min(1.0F, Math.max(0.0F, t));
        return 1.0F - inv * inv * inv;
    }

    public static float easeOutBack(float t) {
        t = Math.min(1.0F, Math.max(0.0F, t));
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3) + c1 * (float) Math.pow(t - 1.0F, 2);
    }
}
