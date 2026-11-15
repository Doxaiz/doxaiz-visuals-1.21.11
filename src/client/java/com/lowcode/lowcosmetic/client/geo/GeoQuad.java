package com.lowcode.lowcosmetic.client.geo;

public final class GeoQuad {
    public final float[] x = new float[4];
    public final float[] y = new float[4];
    public final float[] z = new float[4];
    public final float[] u = new float[4];
    public final float[] v = new float[4];
    public float nx;
    public float ny;
    public float nz;

    GeoQuad(float nx, float ny, float nz) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
    }

    void set(int i, float px, float py, float pz, float tu, float tv) {
        x[i] = px;
        y[i] = py;
        z[i] = pz;
        u[i] = tu;
        v[i] = tv;
    }
}
