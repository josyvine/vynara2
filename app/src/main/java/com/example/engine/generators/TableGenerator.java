package com.example.engine.generators;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class TableGenerator {

    /**
     * Phase 4 & 23 Alignment: Generates a procedural multi-component table asset.
     * Assembles a thick walnut tabletop, structural steel supporting beams, and four corner legs 
     * under a root node hierarchy with PBR materials.
     */
    public static SceneObject generateTable(String rootId, String rootName, MaterialManager matMgr) {
        Material topMat = matMgr.getMaterial("mat_wood_walnut");
        Material legMat = matMgr.getMaterial("mat_metallic_steel");

        // 1. Root Tabletop Surface (Acts as primary transform anchor)
        SceneObject tableRoot = new SceneObject(rootId, rootName, "TABLE",
                PrimitiveGenerator.createCube(1.8f, 0.08f, 1.0f), topMat);
        tableRoot.getTransform().setPosition(0f, 0.76f, 0f);

        // 2. Under-Top Support Beams (2 structural steel beams running lengthwise)
        SceneObject lBeam = new SceneObject(rootId + "_beam_l", "Support Beam Left", "STRUCTURE",
                PrimitiveGenerator.createCube(1.6f, 0.06f, 0.06f), legMat);
        lBeam.getTransform().setPosition(0f, -0.07f, -0.35f);
        tableRoot.addChild(lBeam);

        SceneObject rBeam = new SceneObject(rootId + "_beam_r", "Support Beam Right", "STRUCTURE",
                PrimitiveGenerator.createCube(1.6f, 0.06f, 0.06f), legMat);
        rBeam.getTransform().setPosition(0f, -0.07f, 0.35f);
        tableRoot.addChild(rBeam);

        // 3. Supporting Legs (4 cylindrical steel legs extending to the ground)
        float lx = 0.76f, lz = 0.38f;
        float[][] legPositions = new float[][] {
                { -lx, -0.4f,  lz },
                {  lx, -0.4f,  lz },
                { -lx, -0.4f, -lz },
                {  lx, -0.4f, -lz }
        };

        for (int i = 0; i < 4; i++) {
            SceneObject leg = new SceneObject(rootId + "_leg_" + i, "Table Leg " + (i + 1), "STRUCTURE",
                    PrimitiveGenerator.createCylinder(0.04f, 0.72f, 10), legMat);
            leg.getTransform().setPosition(legPositions[i][0], legPositions[i][1], legPositions[i][2]);
            tableRoot.addChild(leg);
        }

        return tableRoot;
    }
}