package com.irisa.fptree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JsonExporter {

    /**
     * Exports the FP-tree graph as JSON (nodes, links, patterns).
     *
     * @param root The virtual root node of the graph
     * @param translations The mapping from original item identifiers to their translation (uri, type)
     * @param conversion The mapping from Vreeken item identifiers to original item identifiers
     * @param output The output JSON file
     */
    public static void export(FPNode root, Map<Integer, ItemTranslation> translations, 
                                Map<Integer, Integer> conversion, Path output) throws IOException {

        List<FPNode> nodeList = new ArrayList<>();
        Map<FPNode, String> nodeIds = new IdentityHashMap<>();
        Set<String> linkSet = new LinkedHashSet<>();
        List<String[]> linkList = new ArrayList<>();

        Queue<FPNode> queue = new LinkedList<>();

        for (FPNode startNode : root.getChildren()) {
            if (!nodeIds.containsKey(startNode)) {
                String id = "node_" + nodeList.size();
                nodeIds.put(startNode, id);
                nodeList.add(startNode);
                queue.add(startNode);
            }
        }

        while (!queue.isEmpty()) {
            FPNode curr = queue.poll();
            String currId = nodeIds.get(curr);

            for (FPNode child : curr.getChildren()) {
                if (!nodeIds.containsKey(child)) {
                    String childId = "node_" + nodeList.size();
                    nodeIds.put(child, childId);
                    nodeList.add(child);
                    queue.add(child);
                }
                String childId = nodeIds.get(child);
                String linkKey = currId + "-" + childId;
                if (linkSet.add(linkKey)) {
                    linkList.add(new String[]{currId, childId});
                }
            }
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Nodes
        json.append("  \"nodes\": [\n");
        for (int i = 0; i < nodeList.size(); i++) {
            FPNode node = nodeList.get(i);
            String id = nodeIds.get(node);

            json.append("    {\n");
            json.append("      \"id\": \"").append(id).append("\",\n");

            // Items in the compress patterns
            json.append("      \"items\": [");
            List<Integer> items = node.getItems();
            for (int j = 0; j < items.size(); j++) {
                json.append(items.get(j));
                if (j < items.size() - 1) json.append(", ");
            }
            json.append("],\n");

            // Item translation
            json.append("      \"details\": [\n");
            for (int j = 0; j < items.size(); j++) {
                int vreekenItem = items.get(j);
                Integer originalItem = conversion.get(vreekenItem);
                ItemTranslation tr = translations.get(vreekenItem);

                json.append("        {\n");
                json.append("          \"item\": ").append(vreekenItem).append(",\n");
                json.append("          \"originalItem\": ").append(originalItem != null ? originalItem : "null").append(",\n");
                json.append("          \"uri\": ").append(tr != null ? "\"" + escapeJson(tr.getURI()) + "\"" : "null").append(",\n");
                json.append("          \"type\": ").append(tr != null ? "\"" + escapeJson(tr.getType()) + "\"" : "null").append("\n");
                json.append("        }").append(j < items.size() - 1 ? ",\n" : "\n");
            }
            json.append("      ],\n");
            json.append("      \"rawUsage\": ").append(node.getUsage()).append(",\n");
            json.append("      \"usage\": ").append(String.format(Locale.US, "%.6f", node.getNormalizedUsage())).append(",\n");
            json.append("      \"isStart\": ").append(node.isStart()).append(",\n");
            json.append("      \"isEnd\": ").append(node.isEnd()).append(",\n");

            // .ct patterns
            List<CtPattern> sourcePatterns = node.getSourcePatterns();
            json.append("      \"patterns\": [");
            for (int j = 0; j < sourcePatterns.size(); j++) {
                CtPattern pattern = sourcePatterns.get(j);
                List<Integer> patternItems = pattern.getItems();
                json.append("\n        {\n");
                json.append("          \"items\": [");
                for (int k = 0; k < patternItems.size(); k++){
                    json.append(patternItems.get(k));
                    if (k < patternItems.size() - 1) json.append(", ");
                }
                json.append("],\n");

                json.append("          \"originalItems\": [");
                for (int k = 0; k < patternItems.size(); k++){
                    Integer originalPatternItem = conversion.get(patternItems.get(k));
                    json.append(originalPatternItem != null ? originalPatternItem : "null");
                    if (k < patternItems.size() - 1) json.append(", ");
                }
                json.append("],\n");

                json.append("          \"nodeItems\": [");
                for (int k = 0; k < items.size(); k++){
                    json.append(items.get(k));
                    if (k < items.size() - 1) json.append(", ");
                }
                json.append("],\n");

                json.append("          \"originalNodeItems\": [");
                for (int k = 0; k < items.size(); k++){
                    Integer originalNodeItem = conversion.get(items.get(k));
                    json.append(originalNodeItem != null ? originalNodeItem : "null");
                    if (k < items.size() - 1) json.append(", ");
                }
                json.append("],\n");

                json.append("          \"rawUsage\": ").append(pattern.getUsage()).append(",\n");
                json.append("          \"usage\": ")
                    .append(String.format(Locale.US, "%.6f", node.getPatternUsage(pattern)))
                    .append("\n");
                json.append("        }");
                json.append(j < sourcePatterns.size() - 1 ? "," : "");
            }
            if (!sourcePatterns.isEmpty()) {
                json.append("\n      ");
            }
            json.append("]\n");
            json.append("    }").append(i < nodeList.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");

        // Links
        json.append("  \"links\": [\n");
        for (int i = 0; i < linkList.size(); i++) {
            String[] link = linkList.get(i);
            json.append("    {\n");
            json.append("      \"source\": \"").append(link[0]).append("\",\n");
            json.append("      \"target\": \"").append(link[1]).append("\"\n");
            json.append("    }").append(i < linkList.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ]\n");
        json.append("}\n");

        Files.writeString(output, json.toString());
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}