package com.example.engine.generators;

import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class TreeGenerator {

    /**
     * Phase 4 & 23 Alignment: Generates a procedural multi-component tree asset.
     * Assembles a thick vertical bark trunk, angled supporting branches, a large main foliage crown, 
     * and secondary foliage spheres at branch ends under a parent-child root node hierarchy.
     */
    public static SceneObject generateTree(String rootId, String rootName, MaterialManager matMgr) {
        Material barkMat = matMgr.getMaterial("mat_tree_bark");
        Material leafMat = matMgr.getMaterial("mat_foliage");

        // 1. Root Tree Trunk (Vertical cylinder - acts as primary transform anchor)
        SceneObject treeRoot = new SceneObject(rootId, rootName, "TREE",
                PrimitiveGenerator.createCylinder(0.12f, 2.0f, 10), barkMat);
        treeRoot.getTransform().setPosition(0f, 1.0f, 0f);

        // 2. Main Foliage Canopy (Large green sphere on top of the trunk)
        SceneObject mainCanopy = new SceneObject(rootId + "_canopy_main", "Foliage Main Crown", "STRUCTURE",
                PrimitiveGenerator.createSphere(0.8f, 12, 12), leafMat);
        mainCanopy.getTransform().setPosition(0f, 1.2f, 0f);
        treeRoot.addChild(mainCanopy);

        // 3. Procedural Supporting Branches (Angled cylinder struts extending outwards)
        float[][] branchData = new float[][] {
                { -0.3f, 0.6f,  0.0f,  0f, 0f,  45.0f }, // [x, y, z, rx, ry, rz]
                {  0.3f, 0.6f,  0.0f,  0f, 0f, -45.0f },
                {  0.0f, 0.6f, -0.3f, -45.0f, 0f,  0f },
                {  0.0f, 0.6f,  0.3f,  45.0f, 0f,  0f }
        };

        for (int i = 0; i < 4; i++) {
            SceneObject branch = new SceneObject(rootId + "_branch_" + i, "Trunk Branch " + (i + 1), "STRUCTURE",
                    PrimitiveGenerator.createCylinder(0.06f, 0.6f, 8), barkMat);
            branch.getTransform().setPosition(branchData[i][0], branchData[i][1], branchData[i][2]);
            branch.getTransform().setRotation(branchData[i][3], branchData[i][4], branchData[i][5]);
            treeRoot.addChild(branch);

            // 4. Secondary Foliage Spheres (Small green crowns at branch ends)
            SceneObject branchFoliage = new SceneObject(rootId + "_foliage_" + i, "Branch Foliage " + (i + 1), "STRUCTURE",
                    PrimitiveGenerator.createSphere(0.45f, 10, 10), leafMat);
            
            // Position foliage at the exact tip of the angled branches
            float fx = branchData[i][0] * 1.8f;
            float fy = branchData[i][1] + 0.2f;
            float fz = branchData[i][2] * 1.8f;
            
            branchFoliage.getTransform().setPosition(fx, fy, fz);
            treeRoot.addChild(branchFoliage);
        }

        return treeRoot;
    }
}