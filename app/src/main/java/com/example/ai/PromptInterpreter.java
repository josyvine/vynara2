package com.example.ai;

import com.example.ai.protocol.AIToolCall;
import com.example.ai.protocol.AIProductionPlan;
import com.example.ai.protocol.AIProductionRequest;
import com.example.ai.validation.PlanValidator;
import com.example.knowledge.KnowledgeEntry;
import com.example.knowledge.KnowledgeManager;
import com.example.tasks.ProductionPlan;
import com.example.tasks.TaskGraph;
import com.example.tasks.TaskNode;
import com.example.tools.ToolOperation;
import com.example.tools.ToolRegistry;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PromptInterpreter {
    private final KnowledgeManager knowledgeManager;

    public PromptInterpreter(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    public ProductionPlan createProductionPlan(String userPrompt, String style, String targetEngine) {
        return createProductionPlan(userPrompt, style, targetEngine, new ArrayList<>());
    }

    /**
     * Dynamic offline fallback generator.
     * Uses KnowledgeManager multi-concept extraction to generate tasks for ALL detected entities
     * in the prompt (e.g., house + pool + sofa + tree) using an optimized parallel dependency tree.
     */
    public ProductionPlan createProductionPlan(String userPrompt, String style, String targetEngine, List<String> referenceImageUris) {
        List<KnowledgeEntry> matchedKnowledge = knowledgeManager.retrieveAllKnowledgeForPrompt(userPrompt);
        KnowledgeEntry primaryKnowledge = matchedKnowledge.get(0);

        String projectName = extractProjectName(userPrompt, primaryKnowledge.getCategory());
        ProductionPlan plan = new ProductionPlan(projectName, userPrompt, primaryKnowledge.getCategory(), primaryKnowledge, referenceImageUris);
        TaskGraph graph = plan.getTaskGraph();

        int taskCounter = 1;
        
        // Track the geometries created so far to link modifiers and lights cleanly in parallel
        List<String> geometryTaskIds = new ArrayList<>();
        List<String> leafTaskIds = new ArrayList<>();

        // Step 0: Reference Image Analysis Task if images are attached
        String referenceTaskId = null;
        if (plan.hasReferenceImages()) {
            referenceTaskId = "task_" + taskCounter++;
            TaskNode t0 = new TaskNode(referenceTaskId, "Processing Reference Images", "Analyzing " + plan.getReferenceImageUris().size() + " visual reference image(s)", null);
            t0.setStatus(TaskNode.Status.COMPLETED);
            graph.addTask(t0);
        }

        // Generate procedural creation steps for EVERY concept detected in the prompt
        for (KnowledgeEntry entry : matchedKnowledge) {
            String cat = entry.getCategory();
            String conceptId = entry.getId();

            if ("CHARACTER".equalsIgnoreCase(cat)) {
                String tMeshId = "task_" + taskCounter++;
                TaskNode tMesh = new TaskNode(tMeshId, "Generating " + entry.getName() + " Mesh", "Tool: character.create_humanoid",
                        new ToolOperation("character.create_humanoid").setParam("name", entry.getName()).setParam("style", style).setParam("height", 1.8f));
                if (referenceTaskId != null) tMesh.addDependency(referenceTaskId);
                graph.addTask(tMesh);
                geometryTaskIds.add(tMeshId);

                String tBindId = "task_" + taskCounter++;
                TaskNode tBind = new TaskNode(tBindId, "Binding Skeleton & Skin Weights", "Tool: skeleton.bind",
                        new ToolOperation("skeleton.bind"));
                tBind.addDependency(tMeshId);
                graph.addTask(tBind);

                String tRigId = "task_" + taskCounter++;
                TaskNode tRig = new TaskNode(tRigId, "Configuring IK Limb Controllers", "Tool: rig.create_ik",
                        new ToolOperation("rig.create_ik").setParam("limb", "left_arm"));
                tRig.addDependency(tBindId);
                graph.addTask(tRig);

                String tAnimId = "task_" + taskCounter++;
                String clipName = userPrompt.toLowerCase().contains("run") ? "run" : (userPrompt.toLowerCase().contains("jump") ? "jump" : "walk");
                TaskNode tAnim = new TaskNode(tAnimId, "Applying Animation Clip (" + clipName + ")", "Tool: animation.create_clip",
                        new ToolOperation("animation.create_clip").setParam("clipName", clipName));
                tAnim.addDependency(tRigId);
                graph.addTask(tAnim);

                leafTaskIds.add(tAnimId);

            } else if ("ANIMAL".equalsIgnoreCase(cat)) {
                String species = conceptId.contains("bird") ? "bird" : "dog";
                String tCreatureId = "task_" + taskCounter++;
                TaskNode tCreature = new TaskNode(tCreatureId, "Generating " + entry.getName() + " Anatomy", "Tool: character.create_creature",
                        new ToolOperation("character.create_creature").setParam("species", species).setParam("name", entry.getName()));
                if (referenceTaskId != null) tCreature.addDependency(referenceTaskId);
                graph.addTask(tCreature);
                geometryTaskIds.add(tCreatureId);

                String tAnimId = "task_" + taskCounter++;
                TaskNode tAnim = new TaskNode(tAnimId, "Applying Locomotion Animation", "Tool: animation.create_clip",
                        new ToolOperation("animation.create_clip").setParam("clipName", "walk"));
                tAnim.addDependency(tCreatureId);
                graph.addTask(tAnim);

                leafTaskIds.add(tAnimId);

            } else {
                // Procedural Architecture, Furniture, Environment, or Vehicle
                String tStructId = "task_" + taskCounter++;
                TaskNode tStruct = new TaskNode(tStructId, "Building " + entry.getName(), "Tool: geometry.create_procedural",
                        new ToolOperation("geometry.create_procedural").setParam("type", conceptId).setParam("name", entry.getName()));
                if (referenceTaskId != null) tStruct.addDependency(referenceTaskId);
                graph.addTask(tStruct);
                geometryTaskIds.add(tStructId);
                
                leafTaskIds.add(tStructId);
            }
        }

        // Add Lighting Setup (Depends on all physical geometry structures being generated)
        String tLightId = "task_" + taskCounter++;
        TaskNode tLight = new TaskNode(tLightId, "Configuring Scene Lighting", "Tool: scene.add_light",
                new ToolOperation("scene.add_light").setParam("type", "directional").setParam("intensity", 1.2f).setParam("colorHex", "#FFF4E0"));
        for (String geomId : geometryTaskIds) {
            tLight.addDependency(geomId);
        }
        graph.addTask(tLight);

        // Add Validation Check Step (Must run strictly after all modifiers, shapes, and lighting tasks are completed)
        String tValidId = "task_" + taskCounter;
        TaskNode tValid = new TaskNode(tValidId, "Inspecting Mesh & Scene Integrity", "Tool: validation.check_mesh",
                new ToolOperation("validation.check_mesh"));
        for (String leafId : leafTaskIds) {
            tValid.addDependency(leafId);
        }
        tValid.addDependency(tLightId);
        graph.addTask(tValid);

        return plan;
    }

    /**
     * Converts Gemini's structured JSON output into an executable TaskGraph.
     * Generates a fully parallelized Directed Acyclic Graph (DAG) using explicit and semantic rules.
     */
    public ProductionPlan convertStructuredPlanToExecutablePlan(AIProductionRequest request, AIProductionPlan structuredPlan) {
        if (structuredPlan == null || request == null) {
            return createProductionPlan(request != null ? request.getUserPrompt() : "", "Photorealistic", "OpenGL ES / GLTF");
        }

        KnowledgeEntry knowledge = knowledgeManager.retrieveKnowledgeForPrompt(request.getUserPrompt());
        String projectName = extractProjectName(request.getUserPrompt(), structuredPlan.getIntent());
        ProductionPlan plan = new ProductionPlan(projectName, request.getUserPrompt(), structuredPlan.getIntent(), knowledge, request.getReferenceImageUris());
        TaskGraph graph = plan.getTaskGraph();

        List<AIToolCall> toolCalls = structuredPlan.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return createProductionPlan(request.getUserPrompt(), request.getStyle(), request.getTargetEngine(), request.getReferenceImageUris());
        }

        // ENFORCE CONTRACT: Run raw Gemini tool calls through PlanValidator to resolve capabilities
        ToolRegistry toolRegistry = new ToolRegistry();
        PlanValidator planValidator = new PlanValidator(toolRegistry);
        List<AIToolCall> validatedToolCalls = planValidator.validateAndMap(toolCalls);

        // Keep track of the last geometry creation task to build local modifiers dependency mapping
        String lastGeometryTaskId = null;
        Map<String, TaskNode> taskNodeMap = new HashMap<>();

        for (int i = 0; i < validatedToolCalls.size(); i++) {
            AIToolCall call = validatedToolCalls.get(i);
            String defaultTaskId = "task_ai_" + (i + 1);
            
            ToolOperation op = new ToolOperation(call.getToolId());
            if (call.getParameters() != null) {
                for (Map.Entry<String, Object> entry : call.getParameters().entrySet()) {
                    op.setParam(entry.getKey(), entry.getValue());
                }
            }

            // Read explicit custom ID from AI, fallback to default sequential ID
            String taskId = op.getStringParam("id", defaultTaskId);

            String desc = call.getDescription() != null && !call.getDescription().isEmpty()
                    ? call.getDescription()
                    : "Executing tool: " + call.getToolId();

            TaskNode node = new TaskNode(taskId, call.getToolId(), desc, op);
            taskNodeMap.put(taskId, node);
            
            List<String> dependencies = new ArrayList<>();

            // A. Look for explicit JSON dependencies defined by Gemini
            Object dependsOnObj = op.getParam("dependsOn", null);
            if (dependsOnObj instanceof String) {
                dependencies.add(((String) dependsOnObj).trim());
            } else if (dependsOnObj instanceof List) {
                for (Object item : (List<?>) dependsOnObj) {
                    if (item != null) dependencies.add(item.toString().trim());
                }
            } else if (dependsOnObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) dependsOnObj;
                for (int j = 0; j < arr.length(); j++) {
                    dependencies.add(arr.optString(j).trim());
                }
            }

            // B. Semantic Fallback: Map local dependencies if Gemini left "dependsOn" empty
            if (dependencies.isEmpty()) {
                String toolId = call.getToolId().toLowerCase();
                
                // Identify modification, animation, validation, and export tasks
                boolean isModifier = toolId.contains("material.set_properties") ||
                                     toolId.contains("skeleton.bind") ||
                                     toolId.contains("rig.create_ik") ||
                                     toolId.contains("animation.create_clip") ||
                                     toolId.contains("geometry.transform.") ||
                                     toolId.contains("validation.check_mesh") || // CRITICAL EXPORT BINDER!
                                     toolId.contains("export.gltf") ||           // CRITICAL EXPORT BINDER!
                                     toolId.contains("project.save");             // CRITICAL EXPORT BINDER!

                if (isModifier) {
                    if (lastGeometryTaskId != null) {
                        dependencies.add(lastGeometryTaskId);
                    }
                } else {
                    // Check if it is a geometry or character creation tool
                    boolean isGeometryCreator = toolId.contains("geometry.create_") ||
                                                toolId.contains("character.create_");
                    if (isGeometryCreator) {
                        lastGeometryTaskId = taskId;
                    }
                }
            }

            // Apply calculated dependencies to the task node
            for (String depId : dependencies) {
                if (!depId.equals(taskId)) {
                    node.addDependency(depId);
                }
            }
            
            graph.addTask(node);
        }

        // C. Link the final quality validation check strictly to all leaf nodes (terminal branches) of the graph
        Set<String> dependencyTargets = new HashSet<>();
        for (TaskNode node : graph.getAllNodes()) {
            dependencyTargets.addAll(node.getDependencyTaskIds());
        }

        String finalValidationId = "task_ai_validation";
        TaskNode validationNode = new TaskNode(finalValidationId, "validation.check_mesh",
                "Inspecting Generated Scene Integrity", new ToolOperation("validation.check_mesh"));

        for (TaskNode node : graph.getAllNodes()) {
            if (!dependencyTargets.contains(node.getId())) {
                validationNode.addDependency(node.getId());
            }
        }

        // Safety fallback if no leaf branches were identified
        if (validationNode.getDependencyTaskIds().isEmpty() && !graph.getAllNodes().isEmpty()) {
            validationNode.addDependency(graph.getAllNodes().get(graph.getAllNodes().size() - 1).getId());
        }

        graph.addTask(validationNode);

        return plan;
    }

    private String extractProjectName(String prompt, String category) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "3D Project";
        }
        String p = prompt.trim();
        if (p.length() > 28) {
            return p.substring(0, 25) + "...";
        }
        return p;
    }
}