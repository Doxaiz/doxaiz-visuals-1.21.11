package com.lowcode.lowcosmetic.client.gui;

import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import net.minecraft.util.Identifier;

public final class Icons {
    public static final Identifier ALL = icon("all");
    public static final Identifier EQUIPPED = icon("equipped");
    public static final Identifier STAR = icon("star");
    public static final Identifier STAR_FILLED = icon("star_filled");
    public static final Identifier CAPE = icon("cape");
    public static final Identifier WINGS = icon("wings");
    public static final Identifier HAT = icon("hat");
    public static final Identifier BODYWEAR = icon("bodywear");
    public static final Identifier PET = icon("pet");
    public static final Identifier SEARCH = icon("search");
    public static final Identifier SEARCH_OFF = icon("search_off");
    public static final Identifier CLOSE = icon("close");
    public static final Identifier TRASH = icon("trash");
    public static final Identifier EYE = icon("eye");
    public static final Identifier EYE_OFF = icon("eye_off");
    public static final Identifier ROTATE = icon("rotate");
    public static final Identifier HANGER = icon("hanger");
    public static final Identifier CHECK = icon("check");
    public static final Identifier MOUSE = icon("mouse");
    public static final Identifier SPARKLE = icon("sparkle");
    public static final Identifier GEAR = icon("gear");
    public static final Identifier PALETTE = icon("palette");
    public static final Identifier FONT = icon("font");
    public static final Identifier VOLUME = icon("volume");
    public static final Identifier DROPLET = icon("droplet");
    public static final Identifier SCALE = icon("scale");
    public static final Identifier DRAG = icon("drag");

    private Icons() {
    }

    private static Identifier icon(String name) {
        return LowcosmeticClient.id("textures/gui/icons/" + name + ".png");
    }
}
