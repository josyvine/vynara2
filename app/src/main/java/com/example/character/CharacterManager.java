package com.example.character;

import com.example.engine.MaterialManager;
import com.example.engine.SceneObject;
import com.example.engine.ThreeDEngine;
import com.example.engine.generators.CreatureGenerator;
import com.example.engine.generators.HumanoidGenerator;

import java.util.HashMap;
import java.util.Map;

public class CharacterManager {
    private final ThreeDEngine engine;
    private final Map<String, Character> characterMap = new HashMap<>();

    public CharacterManager(ThreeDEngine engine) {
        this.engine = engine;
    }

    /**
     * Phase 6 Alignment: Constructs a procedural humanoid character assembling 
     * head, neck, torso, arms, legs, and feet geometry nodes instead of a single cube.
     */
    public Character createHumanoid(CharacterSpecification spec) {
        if (spec == null) {
            spec = new CharacterSpecification("HUMANOID", "Humanoid Character");
        }

        String id = "char_" + System.currentTimeMillis();
        Skeleton skeleton = SkeletonBuilder.buildHumanoidSkeleton(spec.getHeight());
        MaterialManager matMgr = engine != null ? engine.getMaterialManager() : new MaterialManager();

        // Generate procedural anatomical humanoid mesh node hierarchy
        SceneObject characterMeshObj = HumanoidGenerator.generateHumanoidMesh(id, spec, matMgr);

        if (engine != null && engine.getSceneManager() != null && engine.getSceneManager().getActiveScene() != null) {
            engine.getSceneManager().getActiveScene().addObject(characterMeshObj);
        }

        Character character = new Character(id, spec, characterMeshObj, skeleton);
        
        // Initialize multi-influence vertex skinning weights against the skeleton
        if (character.getSkin() != null) {
            character.getSkin().normalizeWeights();
        }

        characterMap.put(id, character);
        return character;
    }

    /**
     * Phase 7 Alignment: Constructs procedural animal/creature body geometry
     * for quadrupeds and birds instead of a single primitive cube.
     */
    public Character createCreature(CharacterSpecification spec) {
        if (spec == null) {
            spec = new CharacterSpecification("QUADRUPED", "Creature");
        }

        String id = "creature_" + System.currentTimeMillis();
        Skeleton skeleton;
        if ("bird".equalsIgnoreCase(spec.getSpecies())) {
            skeleton = SkeletonBuilder.buildBirdSkeleton();
        } else {
            skeleton = SkeletonBuilder.buildQuadrupedSkeleton();
        }

        MaterialManager matMgr = engine != null ? engine.getMaterialManager() : new MaterialManager();

        // Generate procedural creature body node hierarchy
        SceneObject creatureMeshObj = CreatureGenerator.generateCreatureMesh(id, spec, matMgr);

        if (engine != null && engine.getSceneManager() != null && engine.getSceneManager().getActiveScene() != null) {
            engine.getSceneManager().getActiveScene().addObject(creatureMeshObj);
        }

        Character character = new Character(id, spec, creatureMeshObj, skeleton);
        
        if (character.getSkin() != null) {
            character.getSkin().normalizeWeights();
        }

        characterMap.put(id, character);
        return character;
    }

    public Character getCharacter(String id) {
        if (id == null) return null;
        return characterMap.get(id);
    }

    public boolean removeCharacter(String id) {
        if (id == null) return false;
        Character removed = characterMap.remove(id);
        if (removed != null && engine != null && engine.getSceneManager() != null && engine.getSceneManager().getActiveScene() != null) {
            engine.getSceneManager().getActiveScene().removeObject(removed.getId());
            return true;
        }
        return false;
    }

    public Map<String, Character> getCharacterMap() {
        return characterMap;
    }

    public void clearCharacters() {
        characterMap.clear();
    }
}