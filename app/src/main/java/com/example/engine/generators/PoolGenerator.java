package com.example.engine.generators;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class PoolGenerator {

    /**
     * Phase 4 & 23 Alignment: Generates a procedural multi-component swimming pool asset.
     * Assembles a surrounding tiled deck platform, concrete basin border walls, and a translucent 
     * water surface plane under a parent-child root node hierarchy.
     */
    public static SceneObject generatePool(String rootId, String rootName, MaterialManager matMgr) {
        Material deckMat = matMgr.getMaterial("mat_tiles_deck");
        Material wallMat = matMgr.getMaterial("mat_concrete");
        Material waterMat = matMgr.getMaterial("mat_pool_water");

        // 1. Root Surrounding Tiled Deck (Acts as primary transform anchor)
        SceneObject poolRoot = new SceneObject(rootId, rootName, "POOL",
                PrimitiveGenerator.createCube(4.6f, 0.1f, 8.6f), deckMat);
        poolRoot.getTransform().setPosition(0f, 0.05f, 0f);

        // 2. Basin Border Walls (Left, Right, Front, Back boundaries)
        float wallH = 0.4f;
        
        // Left Side Wall
        SceneObject lWall = new SceneObject(rootId + "_wall_l", "Basin Wall Left", "STRUCTURE",
                PrimitiveGenerator.createCube(0.2f, wallH, 8.0f), wallMat);
        lWall.getTransform().setPosition(-2.1f, 0.2f, 0f);
        poolRoot.addChild(lWall);

        // Right Side Wall
        SceneObject rWall = new SceneObject(rootId + "_wall_r", "Basin Wall Right", "STRUCTURE",
                PrimitiveGenerator.createCube(0.2f, wallH, 8.0f), wallMat);
        rWall.getTransform().setPosition(2.1f, 0.2f, 0f);
        poolRoot.addChild(rWall);

        // Front End Wall
        SceneObject fWall = new SceneObject(rootId + "_wall_f", "Basin Wall Front", "STRUCTURE",
                PrimitiveGenerator.createCube(4.0f, wallH, 0.2f), wallMat);
        fWall.getTransform().setPosition(0f, 0.2f, 4.1f);
        poolRoot.addChild(fWall);

        // Back End Wall
        SceneObject bWall = new SceneObject(rootId + "_wall_b", "Basin Wall Back", "STRUCTURE",
                PrimitiveGenerator.createCube(4.0f, wallH, 0.2f), wallMat);
        bWall.getTransform().setPosition(0f, 0.2f, -4.1f);
        poolRoot.addChild(bWall);

        // 3. Translucent Water Surface (Flat horizontal plane inset within the walls)
        // Uses transparent 'mat_pool_water' (0.65 opacity) to allow viewing through to the deck base
        SceneObject waterSurface = new SceneObject(rootId + "_water", "Pool Water Surface", "STRUCTURE",
                PrimitiveGenerator.createPlane(4.0f, 8.0f), waterMat);
        waterSurface.getTransform().setPosition(0f, 0.3f, 0f); // Water height is slightly below border walls
        poolRoot.addChild(waterSurface);

        return poolRoot;
    }
}