package com.example.engine;

import com.example.engine.generators.HouseGenerator;
import com.example.engine.generators.PoolGenerator;
import com.example.engine.generators.SofaGenerator;
import com.example.engine.generators.TableGenerator;
import com.example.engine.generators.TreeGenerator;
import com.example.engine.generators.VillaGenerator;

public class ThreeDEngine {
    private final SceneManager sceneManager;
    private final MaterialManager materialManager;
    private final LightManager lightManager;
    private final CameraManager cameraManager;

    public ThreeDEngine() {
        this.sceneManager = new SceneManager();
        this.materialManager = new MaterialManager();
        this.lightManager = new LightManager();
        this.cameraManager = new CameraManager();
    }

    /**
     * Phase 1 Alignment: Injection constructor allowing ThreeDEngine to share 
     * managers with the unified ProjectRuntime singleton.
     */
    public ThreeDEngine(SceneManager sceneManager, MaterialManager materialManager, 
                         LightManager lightManager, CameraManager cameraManager) {
        this.sceneManager = sceneManager != null ? sceneManager : new SceneManager();
        this.materialManager = materialManager != null ? materialManager : new MaterialManager();
        this.lightManager = lightManager != null ? lightManager : new LightManager();
        this.cameraManager = cameraManager != null ? cameraManager : new CameraManager();
    }

    public SceneManager getSceneManager() { return sceneManager; }
    public MaterialManager getMaterialManager() { return materialManager; }
    public LightManager getLightManager() { return lightManager; }
    public CameraManager getCameraManager() { return cameraManager; }

    public SceneObject createPrimitive(String type, float width, float height, float depth) {
        Mesh mesh;
        String primitiveType = type != null ? type.toLowerCase() : "cube";

        if ("sphere".equalsIgnoreCase(primitiveType)) {
            mesh = PrimitiveGenerator.createSphere(width > 0 ? width : 1.0f, 16, 16);
        } else if ("cylinder".equalsIgnoreCase(primitiveType)) {
            mesh = PrimitiveGenerator.createCylinder(width > 0 ? width : 1.0f, height > 0 ? height : 2.0f, 16);
        } else if ("plane".equalsIgnoreCase(primitiveType)) {
            mesh = PrimitiveGenerator.createPlane(width > 0 ? width : 4.0f, depth > 0 ? depth : 4.0f);
        } else if ("cone".equalsIgnoreCase(primitiveType)) {
            mesh = PrimitiveGenerator.createCylinder(width > 0 ? width : 1.0f, height > 0 ? height : 2.0f, 3); // Tapered primitive
        } else {
            mesh = PrimitiveGenerator.createCube(width > 0 ? width : 1.5f, height > 0 ? height : 1.5f, depth > 0 ? depth : 1.5f);
        }

        Material mat = materialManager.getMaterial("mat_default");
        String id = "obj_" + primitiveType + "_" + System.currentTimeMillis();
        SceneObject obj = new SceneObject(id, primitiveType.toUpperCase(), "PRIMITIVE", mesh, mat);
        sceneManager.getActiveScene().addObject(obj);
        return obj;
    }

    /**
     * Phase 4 & 23 Alignment: Replaced single-cube placeholders with true multi-component
     * procedural construction assemblies.
     */
    public SceneObject createProceduralStructure(String structureType, String name) {
        String type = structureType != null ? structureType.toLowerCase() : "sofa";
        String objName = name != null && !name.isEmpty() ? name : structureType.toUpperCase();
        String id = "struct_" + type + "_" + System.currentTimeMillis();

        SceneObject rootObject;

        if ("sofa".equalsIgnoreCase(type) || "couch".equalsIgnoreCase(type)) {
            rootObject = SofaGenerator.generateSofa(id, objName, materialManager);
        } else if ("villa".equalsIgnoreCase(type)) {
            rootObject = VillaGenerator.generateVilla(id, objName, materialManager);
        } else if ("house".equalsIgnoreCase(type) || "building".equalsIgnoreCase(type)) {
            rootObject = HouseGenerator.generateHouse(id, objName, materialManager);
        } else if ("pool".equalsIgnoreCase(type)) {
            rootObject = PoolGenerator.generatePool(id, objName, materialManager);
        } else if ("table".equalsIgnoreCase(type) || "desk".equalsIgnoreCase(type)) {
            rootObject = TableGenerator.generateTable(id, objName, materialManager);
        } else if ("tree".equalsIgnoreCase(type) || "plant".equalsIgnoreCase(type)) {
            rootObject = TreeGenerator.generateTree(id, objName, materialManager);
        } else {
            // Default structural asset fallback
            Mesh mesh = PrimitiveGenerator.createCube(2.0f, 2.0f, 2.0f);
            Material mat = materialManager.getMaterial("mat_default");
            rootObject = new SceneObject(id, objName, "STRUCTURE", mesh, mat);
        }

        sceneManager.getActiveScene().addObject(rootObject);
        return rootObject;
    }
}