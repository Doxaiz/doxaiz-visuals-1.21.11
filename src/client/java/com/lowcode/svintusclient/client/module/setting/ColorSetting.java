package com.lowcode.svintusclient.client.module.setting;

import java.awt.Color;

public class ColorSetting extends Setting<Integer> {

    private boolean rainbow = false;
    private float rainbowSpeed = 1.0f;

    public ColorSetting(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public int getRed() {
        return (get() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (get() >> 8) & 0xFF;
    }

    public int getBlue() {
        return get() & 0xFF;
    }

    public int getAlpha() {
        return (get() >> 24) & 0xFF;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public float getRainbowSpeed() {
        return rainbowSpeed;
    }

    public void setRainbowSpeed(float rainbowSpeed) {
        this.rainbowSpeed = rainbowSpeed;
    }

    public int getEffectiveColor() {
        if (!rainbow) return get();
        float hue = (System.currentTimeMillis() % (long) (3000 / rainbowSpeed)) / (3000.0f / rainbowSpeed);
        int rgb = Color.HSBtoRGB(hue, 0.85f, 1.0f);
        int a = getAlpha();
        if (a == 0) a = 255;
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
