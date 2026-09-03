package com.example.knowledge;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeManager {
    private final ConceptGraph conceptGraph;

    public KnowledgeManager() {
        this.conceptGraph = new ConceptGraph();
    }

    /**
     * Phase 3 Alignment: Retrieves the primary domain knowledge entry for a prompt.
     */
    public KnowledgeEntry retrieveKnowledgeForPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return conceptGraph.getConcept("house");
        }
        
        List<KnowledgeEntry> entries = retrieveAllKnowledgeForPrompt(userPrompt);
        return !entries.isEmpty() ? entries.get(0) : conceptGraph.getConcept("house");
    }

    /**
     * Phase 3 Alignment: Multi-concept extractor. Scans user prompts and returns 
     * all matching domain knowledge concepts (e.g., villa, pool, sofa, and tree).
     */
    public List<KnowledgeEntry> retrieveAllKnowledgeForPrompt(String userPrompt) {
        List<KnowledgeEntry> matchedConcepts = new ArrayList<>();
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            matchedConcepts.add(conceptGraph.getConcept("house"));
            return matchedConcepts;
        }

        String p = userPrompt.toLowerCase();

        if (p.contains("human") || p.contains("man") || p.contains("woman") || p.contains("character") || p.contains("superhero") || p.contains("person")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("humanoid"));
        }
        
        if (p.contains("dog") || p.contains("cat") || p.contains("animal") || p.contains("wolf") || p.contains("quadruped")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("dog"));
        }
        
        if (p.contains("bird") || p.contains("eagle") || p.contains("fly") || p.contains("dragon") || p.contains("wing")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("bird"));
        }
        
        if (p.contains("house") || p.contains("villa") || p.contains("building") || p.contains("architecture") || p.contains("room")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("house"));
        }
        
        if (p.contains("pool") || p.contains("swimming")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("pool"));
        }

        if (p.contains("sofa") || p.contains("couch") || p.contains("chair") || p.contains("furniture")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("sofa"));
        }
        
        if (p.contains("table") || p.contains("desk")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("table"));
        }
        
        if (p.contains("tree") || p.contains("plant") || p.contains("forest") || p.contains("palm")) {
            addConceptIfMissing(matchedConcepts, conceptGraph.getConcept("tree"));
        }

        if (matchedConcepts.isEmpty()) {
            KnowledgeEntry directMatch = conceptGraph.getConcept(p);
            if (directMatch != null) {
                matchedConcepts.add(directMatch);
            } else {
                matchedConcepts.add(conceptGraph.getConcept("house"));
            }
        }

        return matchedConcepts;
    }

    private void addConceptIfMissing(List<KnowledgeEntry> list, KnowledgeEntry entry) {
        if (entry != null && !list.contains(entry)) {
            list.add(entry);
        }
    }

    public List<KnowledgeEntry> getConceptsByCategory(String category) {
        List<KnowledgeEntry> results = new ArrayList<>();
        if (category == null || conceptGraph.getAllConcepts() == null) return results;

        for (KnowledgeEntry entry : conceptGraph.getAllConcepts().values()) {
            if (category.equalsIgnoreCase(entry.getCategory())) {
                results.add(entry);
            }
        }
        return results;
    }

    public ConceptGraph getConceptGraph() {
        return conceptGraph;
    }
}