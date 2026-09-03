package com.example.engine;

import com.example.character.Bone;
import com.example.character.Character;
import com.example.character.Skeleton;
import com.example.character.Skin;
import com.example.character.SkinWeight;
import com.example.utils.VynaraLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GLTFImporter {
    private static final String TAG = "GLTFImporter";
    private static final int GLB_MAGIC = 0x46546C67; // 'glTF' in ASCII Little-Endian
    private static final int CHUNK_TYPE_JSON = 0x4E4F534A; // 'JSON' in ASCII Little-Endian
    private static final int CHUNK_TYPE_BIN = 0x004E4942;  // 'BIN\0' in ASCII Little-Endian

    public static class ImportResult {
        private final List<SceneObject> sceneObjects;
        private final List<Character> characters;

        public ImportResult(List<SceneObject> sceneObjects, List<Character> characters) {
            this.sceneObjects = sceneObjects;
            this.characters = characters;
        }

        public List<SceneObject> getSceneObjects() {
            return sceneObjects;
        }

        public List<Character> getCharacters() {
            return characters;
        }

        public boolean isEmpty() {
            return sceneObjects.isEmpty() && characters.isEmpty();
        }
    }

    public static ImportResult loadFromFile(File glbFile) throws Exception {
        if (glbFile == null || !glbFile.exists()) {
            throw new IllegalArgumentException("Target GLB file does not exist.");
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(glbFile))) {
            return loadFromStream(is);
        }
    }

    public static ImportResult loadFromStream(InputStream inputStream) throws Exception {
        byte[] fullBytes = readAllBytes(inputStream);
        ByteBuffer buffer = ByteBuffer.wrap(fullBytes).order(ByteOrder.LITTLE_ENDIAN);

        if (buffer.remaining() < 12) {
            throw new IllegalArgumentException("Invalid GLB container: File is too small for standard header.");
        }

        int magic = buffer.getInt();
        if (magic != GLB_MAGIC) {
            throw new IllegalArgumentException("Invalid GLB header magic. Expected 0x46546C67, got: 0x" + Integer.toHexString(magic));
        }

        int version = buffer.getInt();
        int totalLength = buffer.getInt();

        VynaraLogger.i(TAG, "Parsing GLB binary version: " + version + ", total bytes: " + totalLength);

        JSONObject jsonMetadata = null;
        byte[] binaryDataChunk = null;

        while (buffer.hasRemaining()) {
            if (buffer.remaining() < 8) break;
            int chunkLength = buffer.getInt();
            int chunkType = buffer.getInt();

            if (chunkLength < 0 || chunkLength > buffer.remaining()) {
                break;
            }

            byte[] chunkData = new byte[chunkLength];
            buffer.get(chunkData);

            if (chunkType == CHUNK_TYPE_JSON && jsonMetadata == null) {
                String jsonStr = new String(chunkData, StandardCharsets.UTF_8);
                jsonMetadata = new JSONObject(jsonStr);
            } else if (chunkType == CHUNK_TYPE_BIN && binaryDataChunk == null) {
                binaryDataChunk = chunkData;
            }
        }

        if (jsonMetadata == null) {
            throw new IllegalArgumentException("Corrupted GLB file: JSON chunk missing.");
        }
        if (binaryDataChunk == null) {
            binaryDataChunk = new byte[0];
        }

        return parseGLTFStructure(jsonMetadata, binaryDataChunk);
    }

    private static ImportResult parseGLTFStructure(JSONObject json, byte[] binaryBuffer) throws Exception {
        List<SceneObject> sceneObjects = new ArrayList<>();
        List<Character> characters = new ArrayList<>();

        JSONArray bufferViewsJson = json.optJSONArray("bufferViews");
        JSONArray accessorsJson = json.optJSONArray("accessors");
        JSONArray meshesJson = json.optJSONArray("meshes");
        JSONArray materialsJson = json.optJSONArray("materials");
        JSONArray nodesJson = json.optJSONArray("nodes");
        JSONArray skinsJson = json.optJSONArray("skins");

        List<Material> parsedMaterials = new ArrayList<>();
        if (materialsJson != null) {
            for (int i = 0; i < materialsJson.length(); i++) {
                JSONObject matObj = materialsJson.getJSONObject(i);
                Material material = new Material("mat_" + i);

                JSONObject pbr = matObj.optJSONObject("pbrMetallicRoughness");
                if (pbr != null) {
                    JSONArray baseColorArr = pbr.optJSONArray("baseColorFactor");
                    if (baseColorArr != null && baseColorArr.length() >= 3) {
                        float r = (float) baseColorArr.getDouble(0);
                        float g = (float) baseColorArr.getDouble(1);
                        float b = (float) baseColorArr.getDouble(2);
                        material.setColor(rgbToHex(r, g, b));
                    }
                    material.setMetallic((float) pbr.optDouble("metallicFactor", 0.0));
                    material.setRoughness((float) pbr.optDouble("roughnessFactor", 0.5));
                }
                parsedMaterials.add(material);
            }
        }

        List<Mesh> parsedMeshes = new ArrayList<>();
        if (meshesJson != null) {
            for (int m = 0; m < meshesJson.length(); m++) {
                JSONObject meshObj = meshesJson.getJSONObject(m);
                String meshName = meshObj.optString("name", "mesh_" + m);
                JSONArray primitives = meshObj.optJSONArray("primitives");

                if (primitives != null && primitives.length() > 0) {
                    JSONObject prim = primitives.getJSONObject(0);
                    JSONObject attributes = prim.optJSONObject("attributes");

                    Mesh mesh = new Mesh(meshName);

                    if (attributes != null) {
                        if (attributes.has("POSITION")) {
                            int posAccessorIdx = attributes.getInt("POSITION");
                            float[] positions = readFloatAccessor(posAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                            mesh.setVertices(positions);
                        }

                        if (attributes.has("NORMAL")) {
                            int normAccessorIdx = attributes.getInt("NORMAL");
                            float[] normals = readFloatAccessor(normAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                            mesh.setNormals(normals);
                        }
                    }

                    if (prim.has("indices")) {
                        int indicesAccessorIdx = prim.getInt("indices");
                        int[] indices = readIntAccessor(indicesAccessorIdx, accessorsJson, bufferViewsJson, binaryBuffer);
                        mesh.setIndices(indices);
                    }

                    parsedMeshes.add(mesh);
                }
            }
        }

        Map<Integer, Bone> boneNodeMap = new HashMap<>();
        List<Skeleton> parsedSkeletons = new ArrayList<>();

        if (skinsJson != null && nodesJson != null) {
            for (int s = 0; s < skinsJson.length(); s++) {
                JSONObject skinObj = skinsJson.getJSONObject(s);
                String skinName = skinObj.optString("name", "skeleton_" + s);
                JSONArray joints = skinObj.optJSONArray("joints");

                if (joints != null && joints.length() > 0) {
                    Skeleton skeleton = new Skeleton(skinName);
                    for (int j = 0; j < joints.length(); j++) {
                        int nodeIdx = joints.getInt(j);
                        JSONObject nodeObj = nodesJson.getJSONObject(nodeIdx);
                        String boneName = nodeObj.optString("name", "bone_" + nodeIdx);

                        Bone bone = new Bone(boneName, nodeIdx);
                        applyNodeTransformToBone(nodeObj, bone);
                        boneNodeMap.put(nodeIdx, bone);
                        skeleton.addBone(bone);
                    }

                    for (int j = 0; j < joints.length(); j++) {
                        int nodeIdx = joints.getInt(j);
                        JSONObject nodeObj = nodesJson.getJSONObject(nodeIdx);
                        JSONArray children = nodeObj.optJSONArray("children");
                        if (children != null) {
                            Bone parentBone = boneNodeMap.get(nodeIdx);
                            for (int c = 0; c < children.length(); c++) {
                                int childNodeIdx = children.getInt(c);
                                Bone childBone = boneNodeMap.get(childNodeIdx);
                                if (parentBone != null && childBone != null) {
                                    childBone.setParent(parentBone);
                                    parentBone.addChild(childBone);
                                }
                            }
                        }
                    }
                    parsedSkeletons.add(skeleton);
                }
            }
        }

        if (nodesJson != null) {
            for (int n = 0; n < nodesJson.length(); n++) {
                JSONObject nodeObj = nodesJson.getJSONObject(n);
                String nodeName = nodeObj.optString("name", "node_" + n);

                if (nodeObj.has("mesh")) {
                    int meshIdx = nodeObj.getInt("mesh");
                    if (meshIdx < parsedMeshes.size()) {
                        Mesh mesh = parsedMeshes.get(meshIdx);
                        SceneObject sceneObject = new SceneObject(nodeName, mesh);
                        applyNodeTransformToObject(nodeObj, sceneObject);

                        if (parsedMaterials.size() > 0) {
                            sceneObject.setMaterial(parsedMaterials.get(0));
                        }

                        if (nodeObj.has("skin") && parsedSkeletons.size() > 0) {
                            Character character = new Character(nodeName);
                            character.setMesh(mesh);
                            character.setSkeleton(parsedSkeletons.get(0));
                            characters.add(character);
                        } else {
                            sceneObjects.add(sceneObject);
                        }
                    }
                }
            }
        }

        if (sceneObjects.isEmpty() && characters.isEmpty() && !parsedMeshes.isEmpty()) {
            for (int i = 0; i < parsedMeshes.size(); i++) {
                SceneObject obj = new SceneObject("imported_object_" + i, parsedMeshes.get(i));
                if (i < parsedMaterials.size()) {
                    obj.setMaterial(parsedMaterials.get(i));
                }
                sceneObjects.add(obj);
            }
        }

        VynaraLogger.i(TAG, "Import completed: " + sceneObjects.size() + " static objects, " + characters.size() + " rigged characters.");
        return new ImportResult(sceneObjects, characters);
    }

    private static float[] readFloatAccessor(int accessorIndex, JSONArray accessors, JSONArray bufferViews, byte[] binaryData) throws Exception {
        JSONObject accessor = accessors.getJSONObject(accessorIndex);
        int count = accessor.getInt("count");
        String type = accessor.getString("type");
        int bufferViewIndex = accessor.getInt("bufferView");
        int byteOffset = accessor.optInt("byteOffset", 0);

        JSONObject bufferView = bufferViews.getJSONObject(bufferViewIndex);
        int viewByteOffset = bufferView.optInt("byteOffset", 0);

        int componentsPerElement = getComponentCount(type);
        float[] result = new float[count * componentsPerElement];

        ByteBuffer bb = ByteBuffer.wrap(binaryData).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(viewByteOffset + byteOffset);

        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getFloat();
        }
        return result;
    }

    private static int[] readIntAccessor(int accessorIndex, JSONArray accessors, JSONArray bufferViews, byte[] binaryData) throws Exception {
        JSONObject accessor = accessors.getJSONObject(accessorIndex);
        int count = accessor.getInt("count");
        int componentType = accessor.getInt("componentType");
        int bufferViewIndex = accessor.getInt("bufferView");
        int byteOffset = accessor.optInt("byteOffset", 0);

        JSONObject bufferView = bufferViews.getJSONObject(bufferViewIndex);
        int viewByteOffset = bufferView.optInt("byteOffset", 0);

        int[] result = new int[count];
        ByteBuffer bb = ByteBuffer.wrap(binaryData).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(viewByteOffset + byteOffset);

        for (int i = 0; i < count; i++) {
            if (componentType == 5123) { // UNSIGNED_SHORT
                result[i] = bb.getShort() & 0xFFFF;
            } else if (componentType == 5125) { // UNSIGNED_INT
                result[i] = bb.getInt();
            } else if (componentType == 5121) { // UNSIGNED_BYTE
                result[i] = bb.get() & 0xFF;
            } else {
                result[i] = bb.getShort() & 0xFFFF;
            }
        }
        return result;
    }

    private static int getComponentCount(String type) {
        switch (type) {
            case "SCALAR": return 1;
            case "VEC2": return 2;
            case "VEC3": return 3;
            case "VEC4":
            case "MAT2": return 4;
            case "MAT3": return 9;
            case "MAT4": return 16;
            default: return 1;
        }
    }

    private static void applyNodeTransformToObject(JSONObject nodeObj, SceneObject object) {
        Transform transform = object.getTransform();
        if (transform == null) return;

        JSONArray translation = nodeObj.optJSONArray("translation");
        if (translation != null && translation.length() >= 3) {
            transform.setPosition(
                    (float) translation.optDouble(0, 0.0),
                    (float) translation.optDouble(1, 0.0),
                    (float) translation.optDouble(2, 0.0)
            );
        }

        JSONArray scale = nodeObj.optJSONArray("scale");
        if (scale != null && scale.length() >= 3) {
            transform.setScale(
                    (float) scale.optDouble(0, 1.0),
                    (float) scale.optDouble(1, 1.0),
                    (float) scale.optDouble(2, 1.0)
            );
        }
    }

    private static void applyNodeTransformToBone(JSONObject nodeObj, Bone bone) {
        JSONArray translation = nodeObj.optJSONArray("translation");
        if (translation != null && translation.length() >= 3) {
            bone.setLocalPosition(
                    (float) translation.optDouble(0, 0.0),
                    (float) translation.optDouble(1, 0.0),
                    (float) translation.optDouble(2, 0.0)
            );
        }
    }

    private static String rgbToHex(float r, float g, float b) {
        int ir = Math.min(255, Math.max(0, (int) (r * 255)));
        int ig = Math.min(255, Math.max(0, (int) (g * 255)));
        int ib = Math.min(255, Math.max(0, (int) (b * 255)));
        return String.format("#%02X%02X%02X", ir, ig, ib);
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[16384];
        int bytesRead;
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}