package com.example.engine.generators;

import com.example.character.CharacterSpecification;
import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class HumanoidGenerator {

    /**
     * Phase 6 & 23 Alignment: Constructs a detailed procedural anatomical humanoid character mesh hierarchy.
     * Generates distinct sub-nodes for head, neck, torso, pelvis, joints, arms, hands, legs, and feet,
     * dynamically binding custom PBR clothing materials (shirt, trousers) and skin color configurations.
     */
    public static SceneObject generateHumanoidMesh(String rootId, CharacterSpecification spec, MaterialManager matMgr) {
        float h = spec != null ? spec.getHeight() : 1.8f;
        float sw = spec != null ? spec.getShoulderWidth() : 0.45f;
        float limbRatio = spec != null ? spec.getLimbLengthRatio() : 1.0f;
        float headRatio = spec != null ? spec.getHeadSizeRatio() : 1.0f;
        float build = spec != null ? spec.getBodyBuildFactor() : 1.0f;

        // 1. DYNAMIC MATERIAL RESOLUTION (Scans for custom-generated Gemini assets)
        Material skinMat = matMgr.getMaterial("mat_skin");
        Material shirtMat = matMgr.getMaterial("mat_fabric_grey");
        Material trouserMat = matMgr.getMaterial("mat_fabric_grey");

        // Verify skin tinting overrides from character specifications
        if (spec != null && spec.getSkinColorHex() != null) {
            skinMat = skinMat.cloneMaterial(skinMat.getId() + "_tinted", "Tinted Skin");
            skinMat.setColorHex(spec.getSkinColorHex());
        }

        // Map custom clothing materials loaded into the manager by the AI Orchestrator
        for (String matId : matMgr.getAllMaterials().keySet()) {
            String lowerId = matId.toLowerCase();
            if (lowerId.contains("shirt") || lowerId.contains("plaid") || lowerId.contains("top")) {
                shirtMat = matMgr.getMaterial(matId);
            } else if (lowerId.contains("trouser") || lowerId.contains("pants") || lowerId.contains("bottom")) {
                trouserMat = matMgr.getMaterial(matId);
            } else if (lowerId.contains("skin") || lowerId.contains("body")) {
                skinMat = matMgr.getMaterial(matId);
            }
        }

        // 2. ROOT PELVIS NODE (Anchors the hierarchy tree and sits at half-height)
        SceneObject pelvis = new SceneObject(rootId, spec != null ? spec.getName() : "Humanoid", "CHARACTER",
                PrimitiveGenerator.createCube(0.35f * build, 0.16f, 0.24f * build), trouserMat);
        pelvis.getTransform().setPosition(0f, h * 0.52f, 0f);

        // 3. TORSO CHEST NODE (Connected directly above the pelvis)
        SceneObject torso = new SceneObject(rootId + "_torso", "Torso", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.18f * build, h * 0.28f, 12), shirtMat);
        torso.getTransform().setPosition(0f, h * 0.22f, 0f);
        pelvis.addChild(torso);

        // 4. NECK & HEAD NODE CHAIN
        SceneObject neck = new SceneObject(rootId + "_neck", "Neck", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.07f * build, h * 0.08f, 8), skinMat);
        neck.getTransform().setPosition(0f, h * 0.18f, 0f);
        torso.addChild(neck);

        SceneObject head = new SceneObject(rootId + "_head", "Head", "CHARACTER",
                PrimitiveGenerator.createSphere(0.12f * headRatio, 16, 16), skinMat);
        head.getTransform().setPosition(0f, h * 0.1f, 0f);
        neck.addChild(head);

        // 5. ARMS (Shoulder -> Upper arm -> Elbow Joint -> Forearm -> Hand)
        float armLen = h * 0.22f * limbRatio;
        float shoulderOffset = sw / 2f;

        // --- LEFT ARM CHAIN ---
        // Left Shoulder Joint Sphere
        SceneObject lShoulderJoint = new SceneObject(rootId + "_l_shoulder_joint", "Left Shoulder Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.07f * build, 8, 8), shirtMat);
        lShoulderJoint.getTransform().setPosition(-shoulderOffset, h * 0.12f, 0f);
        torso.addChild(lShoulderJoint);

        SceneObject lUpperArm = new SceneObject(rootId + "_l_up_arm", "Left Upper Arm", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.055f * build, armLen, 8), shirtMat);
        lUpperArm.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        lUpperArm.getTransform().setRotation(0f, 0f, 15f); // Natural dynamic angle
        lShoulderJoint.addChild(lUpperArm);

        // Left Elbow Joint Sphere
        SceneObject lElbowJoint = new SceneObject(rootId + "_l_elbow_joint", "Left Elbow Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.05f * build, 8, 8), skinMat);
        lElbowJoint.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        lUpperArm.addChild(lElbowJoint);

        SceneObject lForearm = new SceneObject(rootId + "_l_fore", "Left Forearm", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.045f * build, armLen, 8), skinMat);
        lForearm.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        lElbowJoint.addChild(lForearm);

        SceneObject lHand = new SceneObject(rootId + "_l_hand", "Left Hand", "CHARACTER",
                PrimitiveGenerator.createSphere(0.042f, 10, 10), skinMat);
        lHand.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        lForearm.addChild(lHand);


        // --- RIGHT ARM CHAIN ---
        // Right Shoulder Joint Sphere
        SceneObject rShoulderJoint = new SceneObject(rootId + "_r_shoulder_joint", "Right Shoulder Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.07f * build, 8, 8), shirtMat);
        rShoulderJoint.getTransform().setPosition(shoulderOffset, h * 0.12f, 0f);
        torso.addChild(rShoulderJoint);

        SceneObject rUpperArm = new SceneObject(rootId + "_r_up_arm", "Right Upper Arm", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.055f * build, armLen, 8), shirtMat);
        rUpperArm.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        rUpperArm.getTransform().setRotation(0f, 0f, -15f);
        rShoulderJoint.addChild(rUpperArm);

        // Right Elbow Joint Sphere
        SceneObject rElbowJoint = new SceneObject(rootId + "_r_elbow_joint", "Right Elbow Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.05f * build, 8, 8), skinMat);
        rElbowJoint.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        rUpperArm.addChild(rElbowJoint);

        SceneObject rForearm = new SceneObject(rootId + "_r_fore", "Right Forearm", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.045f * build, armLen, 8), skinMat);
                rForearm.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        rElbowJoint.addChild(rForearm);

        SceneObject rHand = new SceneObject(rootId + "_r_hand", "Right Hand", "CHARACTER",
                PrimitiveGenerator.createSphere(0.042f, 10, 10), skinMat);
        rHand.getTransform().setPosition(0f, -armLen * 0.5f, 0f);
        rForearm.addChild(rHand);


        // 6. LEGS (Hip Joint -> Thigh -> Knee Joint -> Calf -> Foot)
        float legLen = h * 0.28f * limbRatio;
        float hipOffset = 0.12f * build;

        // --- LEFT LEG CHAIN ---
        // Left Hip Joint
        SceneObject lHipJoint = new SceneObject(rootId + "_l_hip_joint", "Left Hip Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.082f * build, 8, 8), trouserMat);
        lHipJoint.getTransform().setPosition(-hipOffset, -0.06f, 0f);
        pelvis.addChild(lHipJoint);

        SceneObject lThigh = new SceneObject(rootId + "_l_thigh", "Left Thigh", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.078f * build, legLen, 10), trouserMat);
        lThigh.getTransform().setPosition(0f, -legLen * 0.5f, 0f);
        lHipJoint.addChild(lThigh);

        // Left Knee Joint
        SceneObject lKneeJoint = new SceneObject(rootId + "_l_knee_joint", "Left Knee Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.062f * build, 8, 8), skinMat);
        lKneeJoint.getTransform().setPosition(0f, -legLen * 0.5f, 0f);
        lThigh.addChild(lKneeJoint);

        SceneObject lCalf = new SceneObject(rootId + "_l_calf", "Left Calf", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.058f * build, legLen, 10), skinMat);
        lCalf.getTransform().setPosition(0f, -legLen * 0.5f, 0f);
        lKneeJoint.addChild(lCalf);

        SceneObject lFoot = new SceneObject(rootId + "_l_foot", "Left Foot", "CHARACTER",
                PrimitiveGenerator.createCube(0.08f * build, 0.05f, 0.16f), trouserMat);
        lFoot.getTransform().setPosition(0f, -legLen * 0.5f, 0.04f);
        lCalf.addChild(lFoot);


        // --- RIGHT LEG CHAIN ---
        // Right Hip Joint
        SceneObject rHipJoint = new SceneObject(rootId + "_r_hip_joint", "Right Hip Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.082f * build, 8, 8), trouserMat);
        rHipJoint.getTransform().setPosition(hipOffset, -0.06f, 0f);
        pelvis.addChild(rHipJoint);

        SceneObject rThigh = new SceneObject(rootId + "_r_thigh", "Right Thigh", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.078f * build, legLen, 10), trouserMat);
        rThigh.getTransform().setPosition(0f, -legLen * 0.5f, 0f);
        rHipJoint.addChild(rThigh);

        // Right Knee Joint
        SceneObject rKneeJoint = new SceneObject(rootId + "_r_knee_joint", "Right Knee Joint", "CHARACTER",
                PrimitiveGenerator.createSphere(0.062f * build, 8, 8), skinMat);
        rKneeJoint.getTransform().setPosition(0f, -legLen * 0.5f, 0f);
        rThigh.addChild(rKneeJoint);

        SceneObject rCalf = new SceneObject(rootId + "_r_calf", "Right Calf", "CHARACTER",
                PrimitiveGenerator.createCylinder(0.058f * build, legLen, 10), skinMat);
        rCalf.getTransform().setPosition(0f, -legLen * 0.5f, 0f);
        rKneeJoint.addChild(rCalf);

        SceneObject rFoot = new SceneObject(rootId + "_r_foot", "Right Foot", "CHARACTER",
                PrimitiveGenerator.createCube(0.08f * build, 0.05f, 0.16f), trouserMat);
        rFoot.getTransform().setPosition(0f, -legLen * 0.5f, 0.04f);
        rCalf.addChild(rFoot);

        return pelvis;
    }
}