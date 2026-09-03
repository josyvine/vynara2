package com.example.engine.generators;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class HouseGenerator {

    /**
     * Phase 4 & 23 Alignment: Generates a procedural multi-component house structure asset.
     * Assembles a thick concrete foundation slab, four walls (front wall split with door frame openings),
     * window frames with translucent PBR glass panes, and a sloped roof cap under a root node hierarchy.
     */
    public static SceneObject generateHouse(String rootId, String rootName, MaterialManager matMgr) {
        Material concreteMat = matMgr.getMaterial("mat_concrete");
        Material woodMat = matMgr.getMaterial("mat_wood_walnut");
        Material glassMat = matMgr.getMaterial("mat_glass");

        // 1. Root Concrete Foundation Slab (Sits on the ground - acts as primary transform anchor)
        SceneObject houseRoot = new SceneObject(rootId, rootName, "HOUSE",
                PrimitiveGenerator.createCube(6.2f, 0.2f, 5.2f), concreteMat);
        houseRoot.getTransform().setPosition(0f, 0.1f, 0f);

        // 2. Main Perimeter Walls
        // Left Wall
        SceneObject lWall = new SceneObject(rootId + "_wall_l", "Wall Left", "STRUCTURE",
                PrimitiveGenerator.createCube(0.2f, 3.0f, 5.0f), concreteMat);
        lWall.getTransform().setPosition(-2.9f, 1.6f, 0f);
        houseRoot.addChild(lWall);

        // Right Wall
        SceneObject rWall = new SceneObject(rootId + "_wall_r", "Wall Right", "STRUCTURE",
                PrimitiveGenerator.createCube(0.2f, 3.0f, 5.0f), concreteMat);
        rWall.getTransform().setPosition(2.9f, 1.6f, 0f);
        houseRoot.addChild(rWall);

        // Back Wall
        SceneObject bWall = new SceneObject(rootId + "_wall_b", "Wall Back", "STRUCTURE",
                PrimitiveGenerator.createCube(6.0f, 3.0f, 0.2f), concreteMat);
        bWall.getTransform().setPosition(0f, 1.6f, -2.4f);
        houseRoot.addChild(bWall);

        // Front Wall Left Panel
        SceneObject fWallLeft = new SceneObject(rootId + "_wall_f_l", "Front Wall Left Panel", "STRUCTURE",
                PrimitiveGenerator.createCube(2.2f, 3.0f, 0.2f), concreteMat);
        fWallLeft.getTransform().setPosition(-1.9f, 1.6f, 2.4f);
        houseRoot.addChild(fWallLeft);

        // Front Wall Right Panel
        SceneObject fWallRight = new SceneObject(rootId + "_wall_f_r", "Front Wall Right Panel", "STRUCTURE",
                PrimitiveGenerator.createCube(2.2f, 3.0f, 0.2f), concreteMat);
        fWallRight.getTransform().setPosition(1.9f, 1.6f, 2.4f);
        houseRoot.addChild(fWallRight);

        // Front Wall Door Header Beam
        SceneObject fWallHeader = new SceneObject(rootId + "_wall_f_h", "Front Wall Door Header", "STRUCTURE",
                PrimitiveGenerator.createCube(1.6f, 0.8f, 0.2f), concreteMat);
        fWallHeader.getTransform().setPosition(0f, 2.7f, 2.4f);
        houseRoot.addChild(fWallHeader);

        // 3. Entrance Door Frame
        SceneObject doorFrame = new SceneObject(rootId + "_door_frame", "Entrance Door Frame", "STRUCTURE",
                PrimitiveGenerator.createCube(1.0f, 2.2f, 0.25f), woodMat);
        doorFrame.getTransform().setPosition(0f, 1.1f, 2.4f);
        houseRoot.addChild(doorFrame);

        // 4. Left Window Assembly
        SceneObject lWindowFrame = new SceneObject(rootId + "_win_frame_l", "Window Frame Left", "STRUCTURE",
                PrimitiveGenerator.createCube(0.25f, 1.2f, 1.5f), woodMat);
        lWindowFrame.getTransform().setPosition(-2.9f, 1.7f, 0.8f);
        houseRoot.addChild(lWindowFrame);

        SceneObject lWindowGlass = new SceneObject(rootId + "_win_glass_l", "Window Glass Left", "STRUCTURE",
                PrimitiveGenerator.createCube(0.08f, 1.1f, 1.4f), glassMat);
        lWindowGlass.getTransform().setPosition(-2.9f, 1.7f, 0.8f);
        houseRoot.addChild(lWindowGlass);

        // 5. Right Window Assembly
        SceneObject rWindowFrame = new SceneObject(rootId + "_win_frame_r", "Window Frame Right", "STRUCTURE",
                PrimitiveGenerator.createCube(0.25f, 1.2f, 1.5f), woodMat);
        rWindowFrame.getTransform().setPosition(2.9f, 1.7f, 0.8f);
        houseRoot.addChild(rWindowFrame);

        SceneObject rWindowGlass = new SceneObject(rootId + "_win_glass_r", "Window Glass Right", "STRUCTURE",
                PrimitiveGenerator.createCube(0.08f, 1.1f, 1.4f), glassMat);
        rWindowGlass.getTransform().setPosition(2.9f, 1.7f, 0.8f);
        houseRoot.addChild(rWindowGlass);

        // 6. Sloped Roof Cap (Rotated wood slab - pitched for drainage)
        SceneObject roof = new SceneObject(rootId + "_roof", "Sloped Roof Cap", "STRUCTURE",
                PrimitiveGenerator.createCube(6.4f, 0.3f, 5.6f), woodMat);
        roof.getTransform().setPosition(0f, 3.2f, 0f);
        roof.getTransform().setRotation(5.0f, 0f, 0f);
        houseRoot.addChild(roof);

        return houseRoot;
    }
}