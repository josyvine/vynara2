package com.example.engine;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveGenerator {

    public static Mesh createCube(float width, float height, float depth) {
        float w = width / 2f, h = height / 2f, d = depth / 2f;

        float[] vertices = new float[] {
                // Front
                -w, -h,  d,   w, -h,  d,   w,  h,  d,  -w,  h,  d,
                // Back
                -w, -h, -d,  -w,  h, -d,   w,  h, -d,   w, -h, -d,
                // Top
                -w,  h, -d,  -w,  h,  d,   w,  h,  d,   w,  h, -d,
                // Bottom
                -w, -h, -d,   w, -h, -d,   w, -h,  d,  -w, -h,  d,
                // Right
                 w, -h, -d,   w,  h, -d,   w,  h,  d,   w, -h,  d,
                // Left
                -w, -h, -d,  -w, -h,  d,  -w,  h,  d,  -w,  h, -d
        };

        float[] normals = new float[] {
                0,0,1, 0,0,1, 0,0,1, 0,0,1,
                0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,
                0,1,0, 0,1,0, 0,1,0, 0,1,0,
                0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0,
                1,0,0, 1,0,0, 1,0,0, 1,0,0,
                -1,0,0, -1,0,0, -1,0,0, -1,0,0
        };

        float[] texCoords = new float[] {
                0,1, 1,1, 1,0, 0,0,
                1,1, 1,0, 0,0, 0,1,
                0,1, 0,0, 1,0, 1,1,
                1,1, 0,1, 0,0, 1,0,
                1,1, 1,0, 0,0, 0,1,
                0,1, 1,1, 1,0, 0,0
        };

        short[] indices = new short[] {
                0, 1, 2,  0, 2, 3,
                4, 5, 6,  4, 6, 7,
                8, 9,10,  8,10,11,
               12,13,14, 12,14,15,
               16,17,18, 16,18,19,
               20,21,22, 20,22,23
        };

        return new Mesh(vertices, normals, texCoords, indices);
    }

    public static Mesh createSphere(float radius, int rings, int sectors) {
        List<Float> vList = new ArrayList<>();
        List<Float> nList = new ArrayList<>();
        List<Float> tList = new ArrayList<>();
        List<Short> iList = new ArrayList<>();

        int numRings = rings > 2 ? rings : 16;
        int numSectors = sectors > 2 ? sectors : 16;

        float R = 1f / (float)(numRings - 1);
        float S = 1f / (float)(numSectors - 1);

        for (int r = 0; r < numRings; r++) {
            float phi = (float) (-Math.PI / 2.0 + Math.PI * r * R);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);

            for (int s = 0; s < numSectors; s++) {
                float theta = (float) (2.0 * Math.PI * s * S);
                float cosTheta = (float) Math.cos(theta);
                float sinTheta = (float) Math.sin(theta);

                // Mathematically exact unit sphere projection mapping
                float x = cosTheta * cosPhi;
                float y = sinPhi;
                float z = sinTheta * cosPhi;

                vList.add(x * radius);
                vList.add(y * radius);
                vList.add(z * radius);

                nList.add(x);
                nList.add(y);
                nList.add(z);

                tList.add(s * S);
                tList.add(r * R);
            }
        }

        for (int r = 0; r < numRings - 1; r++) {
            for (int s = 0; s < numSectors - 1; s++) {
                short current = (short) (r * numSectors + s);
                short next = (short) (current + numSectors);

                iList.add(current);
                iList.add(next);
                iList.add((short) (current + 1));

                iList.add((short) (current + 1));
                iList.add(next);
                iList.add((short) (next + 1));
            }
        }

        return toMesh(vList, nList, tList, iList);
    }

    /**
     * Phase 4 Alignment: Real 3D Cylinder geometry generator with top cap, bottom cap, 
     * and smooth radial side walls.
     */
    public static Mesh createCylinder(float radius, float height, int segments) {
        List<Float> vList = new ArrayList<>();
        List<Float> nList = new ArrayList<>();
        List<Float> tList = new ArrayList<>();
        List<Short> iList = new ArrayList<>();

        int segs = segments > 3 ? segments : 16;
        float halfH = height / 2f;

        // 1. Radial Side Walls
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * 2 * Math.PI / segs);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // Bottom Ring Vertex
            vList.add(cos * radius); vList.add(-halfH); vList.add(sin * radius);
            nList.add(cos); nList.add(0f); nList.add(sin);
            tList.add((float) i / segs); tList.add(0f);

            // Top Ring Vertex
            vList.add(cos * radius); vList.add(halfH); vList.add(sin * radius);
            nList.add(cos); nList.add(0f); nList.add(sin);
            tList.add((float) i / segs); tList.add(1f);
        }

        for (short i = 0; i < segs; i++) {
            short b1 = (short) (i * 2);
            short t1 = (short) (b1 + 1);
            short b2 = (short) ((i + 1) * 2);
            short t2 = (short) (b2 + 1);

            iList.add(b1); iList.add(t1); iList.add(b2);
            iList.add(b2); iList.add(t1); iList.add(t2);
        }

        // 2. Bottom Closed Cap
        int bottomCenterIdx = vList.size() / 3;
        vList.add(0f); vList.add(-halfH); vList.add(0f);
        nList.add(0f); nList.add(-1f); nList.add(0f);
        tList.add(0.5f); tList.add(0.5f);

        int bottomCapStart = vList.size() / 3;
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * 2 * Math.PI / segs);
            vList.add((float) Math.cos(angle) * radius); vList.add(-halfH); vList.add((float) Math.sin(angle) * radius);
            nList.add(0f); nList.add(-1f); nList.add(0f);
            tList.add(0.5f + (float) Math.cos(angle) * 0.5f); tList.add(0.5f + (float) Math.sin(angle) * 0.5f);
        }

        for (short i = 0; i < segs; i++) {
            iList.add((short) bottomCenterIdx);
            iList.add((short) (bottomCapStart + i + 1));
            iList.add((short) (bottomCapStart + i));
        }

        // 3. Top Closed Cap
        int topCenterIdx = vList.size() / 3;
        vList.add(0f); vList.add(halfH); vList.add(0f);
        nList.add(0f); nList.add(1f); nList.add(0f);
        tList.add(0.5f); tList.add(0.5f);

        int topCapStart = vList.size() / 3;
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * 2 * Math.PI / segs);
            vList.add((float) Math.cos(angle) * radius); vList.add(halfH); vList.add((float) Math.sin(angle) * radius);
            nList.add(0f); nList.add(1f); nList.add(0f);
            tList.add(0.5f + (float) Math.cos(angle) * 0.5f); tList.add(0.5f + (float) Math.sin(angle) * 0.5f);
        }

        for (short i = 0; i < segs; i++) {
            iList.add((short) topCenterIdx);
            iList.add((short) (topCapStart + i));
            iList.add((short) (topCapStart + i + 1));
        }

        return toMesh(vList, nList, tList, iList);
    }

    /**
     * Phase 4 Alignment: Real 3D Cone geometry generator with apex point and circular base.
     */
    public static Mesh createCone(float radius, float height, int segments) {
        List<Float> vList = new ArrayList<>();
        List<Float> nList = new ArrayList<>();
        List<Float> tList = new ArrayList<>();
        List<Short> iList = new ArrayList<>();

        int segs = segments > 3 ? segments : 16;
        float halfH = height / 2f;

        // 1. Apex Vertex
        short apexIdx = 0;
        vList.add(0f); vList.add(halfH); vList.add(0f);
        nList.add(0f); nList.add(1f); nList.add(0f);
        tList.add(0.5f); tList.add(1f);

        // 2. Base Ring with Slanted Normals
        float slantY = radius / (float) Math.sqrt(radius * radius + height * height);
        float slantRad = height / (float) Math.sqrt(radius * radius + height * height);

        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * 2 * Math.PI / segs);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            vList.add(cos * radius); vList.add(-halfH); vList.add(sin * radius);
            nList.add(cos * slantRad); nList.add(slantY); nList.add(sin * slantRad);
            tList.add((float) i / segs); tList.add(0f);
        }

        for (short i = 1; i <= segs; i++) {
            iList.add(apexIdx);
            iList.add(i);
            iList.add((short) (i + 1));
        }

        // 3. Base Closed Cap
        int centerIdx = vList.size() / 3;
        vList.add(0f); vList.add(-halfH); vList.add(0f);
        nList.add(0f); nList.add(-1f); nList.add(0f);
        tList.add(0.5f); tList.add(0.5f);

        int capStart = vList.size() / 3;
        for (int i = 0; i <= segs; i++) {
            float angle = (float) (i * 2 * Math.PI / segs);
            vList.add((float) Math.cos(angle) * radius); vList.add(-halfH); vList.add((float) Math.sin(angle) * radius);
            nList.add(0f); nList.add(-1f); nList.add(0f);
            tList.add(0.5f + (float) Math.cos(angle) * 0.5f); tList.add(0.5f + (float) Math.sin(angle) * 0.5f);
        }

        for (short i = 0; i < segs; i++) {
            iList.add((short) centerIdx);
            iList.add((short) (capStart + i + 1));
            iList.add((short) (capStart + i));
        }

        return toMesh(vList, nList, tList, iList);
    }

    /**
     * Phase 4 Alignment: Real 2D Quad Plane with upward Y normals and UV mappings.
     */
    public static Mesh createPlane(float width, float depth) {
        float w = width / 2f, d = depth / 2f;

        float[] vertices = new float[] {
                -w, 0f,  d,   w, 0f,  d,   w, 0f, -d,  -w, 0f, -d
        };

        float[] normals = new float[] {
                0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f
        };

        float[] texCoords = new float[] {
                0f, 1f,  1f, 1f,  1f, 0f,  0f, 0f
        };

        short[] indices = new short[] {
                0, 1, 2,  0, 2, 3
        };

        return new Mesh(vertices, normals, texCoords, indices);
    }

    /**
     * Phase 4 Alignment: Real 3D Torus geometry generator.
     */
    public static Mesh createTorus(float mainRadius, float tubeRadius, int radialSegments, int tubularSegments) {
        List<Float> vList = new ArrayList<>();
        List<Float> nList = new ArrayList<>();
        List<Float> tList = new ArrayList<>();
        List<Short> iList = new ArrayList<>();

        int rSegs = radialSegments > 3 ? radialSegments : 16;
        int tSegs = tubularSegments > 3 ? tubularSegments : 16;

        for (int i = 0; i <= rSegs; i++) {
            float u = (float) (i * 2 * Math.PI / rSegs);
            float cosU = (float) Math.cos(u);
            float sinU = (float) Math.sin(u);

            for (int j = 0; j <= tSegs; j++) {
                float v = (float) (j * 2 * Math.PI / tSegs);
                float cosV = (float) Math.cos(v);
                float sinV = (float) Math.sin(v);

                float x = (mainRadius + tubeRadius * cosV) * cosU;
                float y = tubeRadius * sinV;
                float z = (mainRadius + tubeRadius * cosV) * sinU;

                vList.add(x); vList.add(y); vList.add(z);

                float nx = cosV * cosU;
                float ny = sinV;
                float nz = cosV * sinU;
                nList.add(nx); nList.add(ny); nList.add(nz);

                tList.add((float) i / rSegs); tList.add((float) j / tSegs);
            }
        }

        for (int i = 0; i < rSegs; i++) {
            for (int j = 0; j < tSegs; j++) {
                short first = (short) (i * (tSegs + 1) + j);
                short second = (short) (first + tSegs + 1);

                iList.add(first); iList.add(second); iList.add((short) (first + 1));
                iList.add((short) (first + 1)); iList.add(second); iList.add((short) (second + 1));
            }
        }

        return toMesh(vList, nList, tList, iList);
    }

    private static Mesh toMesh(List<Float> vList, List<Float> nList, List<Float> tList, List<Short> iList) {
        float[] vArr = new float[vList.size()];
        for (int i = 0; i < vList.size(); i++) vArr[i] = vList.get(i);

        float[] nArr = new float[nList.size()];
        for (int i = 0; i < nList.size(); i++) nArr[i] = nList.get(i);

        float[] tArr = new float[tList.size()];
        for (int i = 0; i < tList.size(); i++) tArr[i] = tList.get(i);

        short[] iArr = new short[iList.size()];
        for (int i = 0; i < iList.size(); i++) iArr[i] = iList.get(i);

        return new Mesh(vArr, nArr, tArr, iArr);
    }
}