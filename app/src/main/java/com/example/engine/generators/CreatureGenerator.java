package com.example.engine.generators;

import com.example.character.CharacterSpecification;
import com.example.engine.Material;
import com.example.engine.MaterialManager;
import com.example.engine.PrimitiveGenerator;
import com.example.engine.SceneObject;

public class CreatureGenerator {

    /**
     * Phase 7 & 23 Alignment: Generates a detailed procedural multi-component quadruped or bird creature.
     * Assembles body segments, smooth leg chains, joint hinges, wings, tails, neck, and head under a parent-child scene graph 
     * hierarchy, dynamically binding custom planning materials (fur, feathers, snout, beak) and skin parameters.
     */
    public static SceneObject generateCreatureMesh(String rootId, CharacterSpecification spec, MaterialManager matMgr) {
        String species = spec != null ? spec.getSpecies().toLowerCase() : "quadruped";
        String name = spec != null ? spec.getName() : "Creature";

        if ("bird".equalsIgnoreCase(species)) {
            return generateBird(rootId, name, matMgr);
        } else {
            return generateQuadruped(rootId, name, species, matMgr);
        }
    }

    private static SceneObject generateQuadruped(String rootId, String name, String species, MaterialManager matMgr) {
        // DYNAMIC MATERIAL RESOLUTION (Scans for custom-generated animal PBR materials)
        Material skinMat = matMgr.getMaterial("mat_leather_brown");
        Material snoutMat = matMgr.getMaterial("mat_default");

        for (String matId : matMgr.getAllMaterials().keySet()) {
            String lowerId = matId.toLowerCase();
            if (lowerId.contains("fur") || lowerId.contains("skin") || lowerId.contains("leather") || lowerId.contains("body") || lowerId.contains("dog") || lowerId.contains("animal") || lowerId.contains("creature")) {
                skinMat = matMgr.getMaterial(matId);
            } else if (lowerId.contains("snout") || lowerId.contains("muzzle") || lowerId.contains("nose") || lowerId.contains("beak")) {
                snoutMat = matMgr.getMaterial(matId);
            }
        }

        // 1. Root Pelvis Node (Anchors the quadruped and sits on the ground)
        SceneObject pelvis = new SceneObject(rootId, name, "CREATURE",
                PrimitiveGenerator.createCube(0.35f, 0.35f, 0.4f), skinMat);
        pelvis.getTransform().setPosition(0f, 0.5f, -0.2f);

        // 2. Spine Chest Node (Connected forward relative to pelvis)
        SceneObject chest = new SceneObject(rootId + "_chest", "Chest", "CREATURE",
                PrimitiveGenerator.createCube(0.4f, 0.4f, 0.5f), skinMat);
        chest.getTransform().setPosition(0f, 0.05f, 0.45f);
        pelvis.addChild(chest);

        // 3. Neck & Head Node Assembly
        SceneObject neckJoint = new SceneObject(rootId + "_neck_joint", "Neck Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.1f, 8, 8), skinMat);
        neckJoint.getTransform().setPosition(0f, 0.22f, 0.22f);
        chest.addChild(neckJoint);

        SceneObject neck = new SceneObject(rootId + "_neck", "Neck", "CREATURE",
                PrimitiveGenerator.createCylinder(0.08f, 0.26f, 8), skinMat);
        neck.getTransform().setPosition(0f, 0.1f, 0.1f);
        neck.getTransform().setRotation(35.0f, 0f, 0f); // Natural dynamic neck slant
        neckJoint.addChild(neck);

        SceneObject head = new SceneObject(rootId + "_head", "Head", "CREATURE",
                PrimitiveGenerator.createSphere(0.12f, 10, 10), skinMat);
        head.getTransform().setPosition(0f, 0.18f, 0.12f);
        neck.addChild(head);

        // Snout detail for dog/cat muzzle shape (Mounted relative to head skull)
        SceneObject snout = new SceneObject(rootId + "_snout", "Snout", "CREATURE",
                PrimitiveGenerator.createCube(0.08f, 0.08f, 0.12f), snoutMat);
        snout.getTransform().setPosition(0f, -0.02f, 0.12f);
        head.addChild(snout);

        // 4. Front Legs (Hip/Shoulder Joint -> Upper leg -> Knee Joint -> Lower leg -> Foot)
        float flLegLen = 0.22f;
        float flX = 0.18f;

        // --- Front Left Leg ---
        SceneObject flShoulder = new SceneObject(rootId + "_fl_shoulder", "Front Left Shoulder Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.08f, 8, 8), skinMat);
        flShoulder.getTransform().setPosition(-flX, -0.16f, 0.16f);
        chest.addChild(flShoulder);

        SceneObject flUpper = new SceneObject(rootId + "_fl_up", "Front Left Upper Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.07f, flLegLen, 8), skinMat);
        flUpper.getTransform().setPosition(0f, -flLegLen * 0.5f, 0f);
        flShoulder.addChild(flUpper);

        SceneObject flKnee = new SceneObject(rootId + "_fl_knee", "Front Left Knee Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.06f, 8, 8), skinMat);
        flKnee.getTransform().setPosition(0f, -flLegLen * 0.5f, 0f);
        flUpper.addChild(flKnee);

        SceneObject flLower = new SceneObject(rootId + "_fl_low", "Front Left Lower Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.05f, flLegLen, 8), skinMat);
        flLower.getTransform().setPosition(0f, -flLegLen * 0.5f, 0f);
        flKnee.addChild(flLower);

        SceneObject flFoot = new SceneObject(rootId + "_fl_foot", "Front Left Foot", "CREATURE",
                PrimitiveGenerator.createSphere(0.055f, 8, 8), skinMat);
        flFoot.getTransform().setPosition(0f, -flLegLen * 0.5f, 0.02f);
        flLower.addChild(flFoot);

        // --- Front Right Leg ---
        SceneObject frShoulder = new SceneObject(rootId + "_fr_shoulder", "Front Right Shoulder Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.08f, 8, 8), skinMat);
        frShoulder.getTransform().setPosition(flX, -0.16f, 0.16f);
        chest.addChild(frShoulder);

        SceneObject frUpper = new SceneObject(rootId + "_fr_up", "Front Right Upper Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.07f, flLegLen, 8), skinMat);
        frUpper.getTransform().setPosition(0f, -flLegLen * 0.5f, 0f);
        frShoulder.addChild(frUpper);

        SceneObject frKnee = new SceneObject(rootId + "_fr_knee", "Front Right Knee Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.06f, 8, 8), skinMat);
        frKnee.getTransform().setPosition(0f, -flLegLen * 0.5f, 0f);
        frUpper.addChild(frKnee);

        SceneObject frLower = new SceneObject(rootId + "_fr_low", "Front Right Lower Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.05f, flLegLen, 8), skinMat);
        frLower.getTransform().setPosition(0f, -flLegLen * 0.5f, 0f);
        frKnee.addChild(frLower);

        SceneObject frFoot = new SceneObject(rootId + "_fr_foot", "Front Right Foot", "CREATURE",
                PrimitiveGenerator.createSphere(0.055f, 8, 8), skinMat);
        frFoot.getTransform().setPosition(0f, -flLegLen * 0.5f, 0.02f);
        frLower.addChild(frFoot);

        // 5. Rear Legs (Hip Joint -> Thigh -> Knee Joint -> Calf -> Foot)
        float rlLegLen = 0.24f;
        float rlX = 0.16f;

        // --- Rear Left Leg ---
        SceneObject rlHip = new SceneObject(rootId + "_rl_hip", "Rear Left Hip Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.09f, 8, 8), skinMat);
        rlHip.getTransform().setPosition(-rlX, -0.16f, -0.12f);
        pelvis.addChild(rlHip);

        SceneObject rlUpper = new SceneObject(rootId + "_rl_up", "Rear Left Upper Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.08f, rlLegLen, 8), skinMat);
        rlUpper.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0f);
        rlHip.addChild(rlUpper);

        SceneObject rlKnee = new SceneObject(rootId + "_rl_knee", "Rear Left Knee Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.07f, 8, 8), skinMat);
        rlKnee.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0f);
        rlUpper.addChild(rlKnee);

        SceneObject rlLower = new SceneObject(rootId + "_rl_low", "Rear Left Lower Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.06f, rlLegLen, 8), skinMat);
        rlLower.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0f);
        rlKnee.addChild(rlLower);

        SceneObject rlFoot = new SceneObject(rootId + "_rl_foot", "Rear Left Foot", "CREATURE",
                PrimitiveGenerator.createSphere(0.062f, 8, 8), skinMat);
        rlFoot.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0.02f);
        rlLower.addChild(rlFoot);

        // --- Rear Right Leg ---
        SceneObject rrHip = new SceneObject(rootId + "_rr_hip", "Rear Right Hip Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.09f, 8, 8), skinMat);
        rrHip.getTransform().setPosition(rlX, -0.16f, -0.12f);
        pelvis.addChild(rrHip);

        SceneObject rrUpper = new SceneObject(rootId + "_rr_up", "Rear Right Upper Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.08f, rlLegLen, 8), skinMat);
        rrUpper.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0f);
        rrHip.addChild(rrUpper);

        SceneObject rrKnee = new SceneObject(rootId + "_rr_knee", "Rear Right Knee Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.07f, 8, 8), skinMat);
        rrKnee.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0f);
        rrUpper.addChild(rrKnee);

        SceneObject rrLower = new SceneObject(rootId + "_rr_low", "Rear Right Lower Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.06f, rlLegLen, 8), skinMat);
        rrLower.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0f);
        rrKnee.addChild(rrLower);

        SceneObject rrFoot = new SceneObject(rootId + "_rr_foot", "Rear Right Foot", "CREATURE",
                PrimitiveGenerator.createSphere(0.062f, 8, 8), skinMat);
        rrFoot.getTransform().setPosition(0f, -rlLegLen * 0.5f, 0.02f);
        rrLower.addChild(rrFoot);

        // 6. Tail (Mounted relative to pelvis anchor)
        SceneObject tailJoint = new SceneObject(rootId + "_tail_joint", "Tail Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.05f, 8, 8), skinMat);
        tailJoint.getTransform().setPosition(0f, 0.12f, -0.22f);
        pelvis.addChild(tailJoint);

        SceneObject tail = new SceneObject(rootId + "_tail", "Tail", "CREATURE",
                PrimitiveGenerator.createCylinder(0.04f, 0.35f, 8), skinMat);
        tail.getTransform().setPosition(0f, 0f, -0.175f);
        tail.getTransform().setRotation(-35.0f, 0f, 0f); // Slanted downward tail
        tailJoint.addChild(tail);

        return pelvis;
    }

    private static SceneObject generateBird(String rootId, String name, MaterialManager matMgr) {
        // DYNAMIC BIRD MATERIAL RESOLUTION (Scans for custom dynamic feather and beak mappings)
        Material skinMat = matMgr.getMaterial("mat_foliage");
        Material beakMat = matMgr.getMaterial("mat_metallic_gold");

        for (String matId : matMgr.getAllMaterials().keySet()) {
            String lowerId = matId.toLowerCase();
            if (lowerId.contains("feather") || lowerId.contains("wing") || lowerId.contains("body") || lowerId.contains("bird") || lowerId.contains("foliage")) {
                skinMat = matMgr.getMaterial(matId);
            } else if (lowerId.contains("beak") || lowerId.contains("nose") || lowerId.contains("mouth") || lowerId.contains("gold")) {
                beakMat = matMgr.getMaterial(matId);
            }
        }

        // 1. Root Spine Body Node (Acts as primary transform anchor)
        SceneObject spine = new SceneObject(rootId, name, "CREATURE",
                PrimitiveGenerator.createCube(0.35f, 0.35f, 0.5f), skinMat);
        spine.getTransform().setPosition(0f, 0.5f, 0f);

        // 2. Neck & Head Node Assembly
        SceneObject neckJoint = new SceneObject(rootId + "_neck_joint", "Neck Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.07f, 8, 8), skinMat);
        neckJoint.getTransform().setPosition(0f, 0.16f, 0.16f);
        spine.addChild(neckJoint);

        SceneObject neck = new SceneObject(rootId + "_neck", "Neck", "CREATURE",
                PrimitiveGenerator.createCylinder(0.06f, 0.18f, 8), skinMat);
        neck.getTransform().setPosition(0f, 0.08f, 0.08f);
        neck.getTransform().setRotation(20.0f, 0f, 0f); // Curved neck slant
        neckJoint.addChild(neck);

        SceneObject head = new SceneObject(rootId + "_head", "Head", "CREATURE",
                PrimitiveGenerator.createSphere(0.1f, 10, 10), skinMat);
        head.getTransform().setPosition(0f, 0.16f, 0.08f);
        neck.addChild(head);

        // Beak (Procedural cone mounted relative to head skull)
        SceneObject beak = new SceneObject(rootId + "_beak", "Beak", "CREATURE",
                PrimitiveGenerator.createCylinder(0.03f, 0.12f, 4), beakMat);
        beak.getTransform().setPosition(0f, 0f, 0.1f);
        beak.getTransform().setRotation(90.0f, 0f, 0f); // Forward facing beak
        head.addChild(beak);

        // 3. Wings (Shoulder Joint -> Wing Arm -> Elbow Joint -> Wing Tip)
        float wingLen = 0.45f;

        // --- Left Wing Assembly ---
        SceneObject lWingShoulder = new SceneObject(rootId + "_l_wing_shoulder", "Left Wing Shoulder Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.06f, 8, 8), skinMat);
        lWingShoulder.getTransform().setPosition(-0.18f, 0.08f, 0f);
        spine.addChild(lWingShoulder);

        SceneObject lWingArm = new SceneObject(rootId + "_l_wing_arm", "Left Wing Arm", "CREATURE",
                PrimitiveGenerator.createCube(wingLen * 0.5f, 0.04f, 0.25f), skinMat);
        lWingArm.getTransform().setPosition(-wingLen * 0.25f, 0f, 0f);
        lWingArm.getTransform().setRotation(0f, 0f, 20.0f); // Angled upward
        lWingShoulder.addChild(lWingArm);

        SceneObject lWingElbow = new SceneObject(rootId + "_l_wing_elbow", "Left Wing Elbow Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.045f, 8, 8), skinMat);
        lWingElbow.getTransform().setPosition(-wingLen * 0.25f, 0f, 0f);
        lWingArm.addChild(lWingElbow);

        SceneObject lWingTip = new SceneObject(rootId + "_l_wing_tip", "Left Wing Tip", "CREATURE",
                PrimitiveGenerator.createCube(wingLen * 0.5f, 0.02f, 0.18f), skinMat);
        lWingTip.getTransform().setPosition(-wingLen * 0.25f, 0f, 0f);
        lWingElbow.addChild(lWingTip);

        // --- Right Wing Assembly ---
        SceneObject rWingShoulder = new SceneObject(rootId + "_r_wing_shoulder", "Right Wing Shoulder Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.06f, 8, 8), skinMat);
        rWingShoulder.getTransform().setPosition(0.18f, 0.08f, 0f);
        spine.addChild(rWingShoulder);

        SceneObject rWingArm = new SceneObject(rootId + "_r_wing_arm", "Right Wing Arm", "CREATURE",
                PrimitiveGenerator.createCube(wingLen * 0.5f, 0.04f, 0.25f), skinMat);
        rWingArm.getTransform().setPosition(wingLen * 0.25f, 0f, 0f);
        rWingArm.getTransform().setRotation(0f, 0f, -20.0f); // Angled upward
        rWingShoulder.addChild(rWingArm);

        SceneObject rWingElbow = new SceneObject(rootId + "_r_wing_elbow", "Right Wing Elbow Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.045f, 8, 8), skinMat);
        rWingElbow.getTransform().setPosition(wingLen * 0.25f, 0f, 0f);
        rWingArm.addChild(rWingElbow);

        SceneObject rWingTip = new SceneObject(rootId + "_r_wing_tip", "Right Wing Tip", "CREATURE",
                PrimitiveGenerator.createCube(wingLen * 0.5f, 0.02f, 0.18f), skinMat);
        rWingTip.getTransform().setPosition(wingLen * 0.25f, 0f, 0f);
        rWingElbow.addChild(rWingTip);

        // 4. Supporting Legs (Hip Joint -> Leg cylinder -> Claws)
        float legHeight = 0.22f;

        // --- Left Leg ---
        SceneObject lHip = new SceneObject(rootId + "_l_leg_hip", "Left Leg Hip Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.05f, 8, 8), beakMat);
        lHip.getTransform().setPosition(-0.1f, -0.15f, 0f);
        spine.addChild(lHip);

        SceneObject lLeg = new SceneObject(rootId + "_l_leg", "Left Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.03f, legHeight, 8), beakMat);
        lLeg.getTransform().setPosition(0f, -legHeight * 0.5f, 0f);
        lHip.addChild(lLeg);

        // --- Right Leg ---
        SceneObject rHip = new SceneObject(rootId + "_r_leg_hip", "Right Leg Hip Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.05f, 8, 8), beakMat);
        rHip.getTransform().setPosition(0.1f, -0.15f, 0f);
        spine.addChild(rHip);

        SceneObject rLeg = new SceneObject(rootId + "_r_leg", "Right Leg", "CREATURE",
                PrimitiveGenerator.createCylinder(0.03f, legHeight, 8), beakMat);
        rLeg.getTransform().setPosition(0f, -legHeight * 0.5f, 0f);
        rHip.addChild(rLeg);

        // Feather Tail
        SceneObject tailJoint = new SceneObject(rootId + "_tail_joint", "Tail Joint", "CREATURE",
                PrimitiveGenerator.createSphere(0.05f, 8, 8), skinMat);
        tailJoint.getTransform().setPosition(0f, -0.05f, -0.22f);
        spine.addChild(tailJoint);

        SceneObject tail = new SceneObject(rootId + "_tail", "Feather Tail", "CREATURE",
                PrimitiveGenerator.createCube(0.2f, 0.02f, 0.3f), skinMat);
        tail.getTransform().setPosition(0f, 0f, -0.08f);
        tail.getTransform().setRotation(-10.0f, 0f, 0f);
        tailJoint.addChild(tail);

        return spine;
    }
}