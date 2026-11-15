package com.lowcode.lowcosmetic.client.geo;

import java.util.ArrayList;
import java.util.List;

public final class GeoBone {
    public final String name;
    public final List<GeoBone> children = new ArrayList<>();
    public final List<GeoCube> cubes = new ArrayList<>();
    public float pivotX;
    public float pivotY;
    public float pivotZ;
    public float rotX;
    public float rotY;
    public float rotZ;

    GeoBone(String name) {
        this.name = name;
    }
}
