package com.example.engine.generators;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class ChairGenerator {

    /**
     * Phase 4 & 23 Alignment: Generates a procedural multi-component chair asset.
     * Assembles a comfortable seat cushion, a high backrest, support struts, and four legs
     * under a root node hierarchy with distinct PBR materials.
     */
    public static SceneObject generateChair(String rootId, String rootName, MaterialManager matMgr) {
        Material seatMat = matMgr.getMaterial("mat_fabric_grey");
        Material frameMat = matMgr.getMaterial("mat_wood_walnut");

        // 1. Root Seat Cushion (Flat horizontal pad - acts as primary transform anchor)
        SceneObject chairRoot = new SceneObject(rootId, rootName, "CHAIR",
                PrimitiveGenerator.createCube(0.5f, 0.08f, 0.5f), seatMat);
        chairRoot.getTransform().setPosition(0f, 0.48f, 0f);

        // 2. Seat Frame Underlay (Support base linking legs)
        SceneObject seatFrame = new SceneObject(rootId + "_frame", "Seat Frame Support", "STRUCTURE",
                PrimitiveGenerator.createCube(0.48f, 0.04f, 0.48f), frameMat);
        seatFrame.getTransform().setPosition(0f, -0.06f, 0f);
        chairRoot.addChild(seatFrame);

        // 3. Backrest Cushion (Thin vertical PBR support)
        SceneObject backrest = new SceneObject(rootId + "_back", "Chair Backrest Cushion", "STRUCTURE",
                PrimitiveGenerator.createCube(0.46f, 0.42f, 0.06f), seatMat);
        backrest.getTransform().setPosition(0f, 0.28f, -0.21f);
        chairRoot.addChild(backrest);

        // 4. Backrest Support Struts (2 walnut struts linking base frame and backrest)
        SceneObject lStrut = new SceneObject(rootId + "_strut_l", "Backrest Strut Left", "STRUCTURE",
                PrimitiveGenerator.createCube(0.04f, 0.44f, 0.04f), frameMat);
        lStrut.getTransform().setPosition(-0.18f, 0.16f, -0.21f);
        chairRoot.addChild(lStrut);

        SceneObject rStrut = new SceneObject(rootId + "_strut_r", "Backrest Strut Right", "STRUCTURE",
                PrimitiveGenerator.createCube(0.04f, 0.44f, 0.04f), frameMat);
        rStrut.getTransform().setPosition(0.18f, 0.16f, -0.21f);
        chairRoot.addChild(rStrut);

        // 5. Supporting Legs (4 cylindrical legs extending down to the ground)
        float lx = 0.20f, lz = 0.20f;
        float[][] legPositions = new float[][] {
                { -lx, -0.24f,  lz },
                {  lx, -0.24f,  lz },
                { -lx, -0.24f, -lz },
                {  lx, -0.24f, -lz }
        };

        for (int i = 0; i < 4; i++) {
            SceneObject leg = new SceneObject(rootId + "_leg_" + i, "Chair Leg " + (i + 1), "STRUCTURE",
                    PrimitiveGenerator.createCylinder(0.026f, 0.44f, 8), frameMat);
            leg.getTransform().setPosition(legPositions[i][0], legPositions[i][1], legPositions[i][2]);
            chairRoot.addChild(leg);
        }

        return chairRoot;
    }
}