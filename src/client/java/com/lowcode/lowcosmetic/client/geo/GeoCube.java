package com.lowcode.lowcosmetic.client.geo;

import java.util.ArrayList;
import java.util.List;

public final class GeoCube {
    public final List<GeoQuad> quads = new ArrayList<>(6);
    public final boolean flatX;
    public final boolean flatY;
    public final boolean flatZ;

    GeoCube(float sizeX, float sizeY, float sizeZ) {
        this.flatX = sizeX == 0.0F;
        this.flatY = sizeY == 0.0F;
        this.flatZ = sizeZ == 0.0F;
    }
}
