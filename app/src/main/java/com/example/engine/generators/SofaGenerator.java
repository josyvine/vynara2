package com.example.engine.generators;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class SofaGenerator {

    /**
     * Phase 4 & 23 Alignment: Generates a procedural multi-component sofa asset.
     * Assembles frame base, backrest, left/right armrests, seat cushions, and corner legs 
     * under a root node hierarchy with distinct PBR materials.
     */
    public static SceneObject generateSofa(String rootId, String rootName, MaterialManager matMgr) {
        Material frameMat = matMgr.getMaterial("mat_wood_walnut");
        Material cushionMat = matMgr.getMaterial("mat_leather_brown");
        Material legMat = matMgr.getMaterial("mat_metallic_steel");

        // 1. Root Sofa Base Frame (Acts as primary structural body)
        SceneObject sofaRoot = new SceneObject(rootId, rootName, "SOFA", 
                PrimitiveGenerator.createCube(2.4f, 0.15f, 0.9f), frameMat);
        sofaRoot.getTransform().setPosition(0f, 0.2f, 0f);

        // 2. Comfortable Seat Base Cushion Platform (Procedural overlay block)
        SceneObject cushionPlatform = new SceneObject(rootId + "_platform", "Cushion Base Platform", "STRUCTURE",
                PrimitiveGenerator.createCube(2.36f, 0.08f, 0.86f), frameMat);
        cushionPlatform.getTransform().setPosition(0f, 0.11f, 0.01f);
        sofaRoot.addChild(cushionPlatform);

        // 3. Backrest Support Slab
        SceneObject backrest = new SceneObject(rootId + "_backrest", "Sofa Backrest Cushion", "STRUCTURE",
                PrimitiveGenerator.createCube(2.36f, 0.6f, 0.16f), cushionMat);
        backrest.getTransform().setPosition(0f, 0.45f, -0.36f);
        sofaRoot.addChild(backrest);

        // 4. Left Armrest Block
        SceneObject leftArm = new SceneObject(rootId + "_l_arm", "Sofa Left Armrest", "STRUCTURE",
                PrimitiveGenerator.createCube(0.16f, 0.48f, 0.88f), cushionMat);
        leftArm.getTransform().setPosition(-1.12f, 0.24f, 0f);
        sofaRoot.addChild(leftArm);

        // 5. Right Armrest Block
        SceneObject rightArm = new SceneObject(rootId + "_r_arm", "Sofa Right Armrest", "STRUCTURE",
                PrimitiveGenerator.createCube(0.16f, 0.48f, 0.88f), cushionMat);
        rightArm.getTransform().setPosition(1.12f, 0.24f, 0f);
        sofaRoot.addChild(rightArm);

        // 6. Cushions (3 separate PBR seat cushions spaced sequentially)
        float cushionWidth = 0.68f;
        for (int i = 0; i < 3; i++) {
            SceneObject cushion = new SceneObject(rootId + "_cushion_" + i, "Seat Cushion " + (i + 1), "STRUCTURE",
                    PrimitiveGenerator.createCube(cushionWidth, 0.16f, 0.72f), cushionMat);
            float xOffset = (i - 1) * (cushionWidth + 0.03f);
            cushion.getTransform().setPosition(xOffset, 0.18f, 0.06f);
            sofaRoot.addChild(cushion);
        }

        // 7. Leg Support Pegs (4 metallic cylinders structural support)
        float lx = 1.08f, lz = 0.36f;
        float[][] legPositions = new float[][] {
                { -lx, -0.1f,  lz },
                {  lx, -0.1f,  lz },
                { -lx, -0.1f, -lz },
                {  lx, -0.1f, -lz }
        };

        for (int i = 0; i < 4; i++) {
            SceneObject leg = new SceneObject(rootId + "_leg_" + i, "Sofa Leg " + (i + 1), "STRUCTURE",
                    PrimitiveGenerator.createCylinder(0.04f, 0.2f, 10), legMat);
            leg.getTransform().setPosition(legPositions[i][0], legPositions[i][1], legPositions[i][2]);
            sofaRoot.addChild(leg);
        }

        return sofaRoot;
    }
}