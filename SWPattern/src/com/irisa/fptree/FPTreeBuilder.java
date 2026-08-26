package com.irisa.fptree;

import java.util.stream.Collectors;
import java.util.*;

public class FPTreeBuilder {

    private long maxSupport = 0;

    public long getMaxSupport(){
        return maxSupport;
    }

    /**
     * Builds a compacted graph from a code table: merges chains of items that
     * always co-occur into single blocks, links blocks across pattern
     * transitions, and marks each block's start/end role and usage.
     *
     * @param patterns The .ct patterns to build the graph from
     * @param selectedItem The item to filter by, or null to use all patterns
     */
    public FPNode build(List<CtPattern> patterns, Integer selectedItem) {

        if (selectedItem != null) {
            patterns = patterns.stream()
                    .filter(p -> p.contains(selectedItem))
                    .collect(Collectors.toList());
        } 

        if (selectedItem != null) {
            for (CtPattern pattern : patterns) {

                if (pattern.getItems().size() == 1){
                    maxSupport = Math.max(maxSupport, pattern.getSupport());
                    if (pattern.getUsage() <= 0) continue;
                }
            }
        } else {
            long totalUsage = 0;
            for (CtPattern pattern : patterns){
                totalUsage += pattern.getUsage();
            }
            maxSupport = totalUsage;
        }

        // Only patterns with usage > 0
        List<CtPattern> activePatterns = patterns.stream()
                .filter(p -> p.getUsage() > 0)
                .collect(Collectors.toList());

        // occurrences[u] / follows[u->v]: if equal, u is always followed by v
        Map<Integer, Integer> occurrences = new HashMap<>();
        Map<String, Integer> follows = new HashMap<>();
        Map<Integer, Long> itemUsageSum = new HashMap<>();

        for (CtPattern pattern : activePatterns){
            List<Integer> items = pattern.getItems();
            for (int i = 0; i < items.size(); i++){
                int item = items.get(i);
                occurrences.put(item, occurrences.getOrDefault(item, 0) + 1);
                itemUsageSum.merge(item, pattern.getUsage(), Long::sum);

                if (i < items.size() - 1){ // no follower
                    int nextItem = items.get(i + 1);
                    String key = item + "-" + nextItem;
                    follows.put(key, follows.getOrDefault(key, 0) + 1);
                }
            }
        }

        // Items that always appear together
        Map<Integer, Integer> nextMap = new HashMap<>();
        Map<Integer, Integer> prevMap = new HashMap<>();

        for (CtPattern pattern : activePatterns){
            List<Integer> items = pattern.getItems();
            for (int i = 0; i < items.size() - 1; i++){
                int item = items.get(i);
                int nextItem = items.get(i + 1);

                int itemOccur = occurrences.getOrDefault(item, 0);
                int nextItemOccur = occurrences.getOrDefault(nextItem, 0);
                int followCount = follows.getOrDefault(item + "-" + nextItem, 0);

                if (followCount > 0 && followCount == itemOccur && followCount == nextItemOccur){
                    nextMap.put(item, nextItem);
                    prevMap.put(nextItem, item);
                }
            }
        }

        // Build compressed patterns
        Set<Integer> processed = new HashSet<>();
        List<List<Integer>> compressedPatterns = new ArrayList<>();

        for (CtPattern p : activePatterns) {
            for (int item : p.getItems()) {
                if (processed.contains(item)) continue;
 
                // Find the head of the block
                int head = item;
                while (prevMap.containsKey(head)) {
                    head = prevMap.get(head);
                }

                if (!processed.contains(head)) {
                    List<Integer> block = new ArrayList<>();
                    int currentItem = head;
                    while (true) {
                        block.add(currentItem);
                        processed.add(currentItem);
                        if (nextMap.containsKey(currentItem)) {
                            currentItem = nextMap.get(currentItem);
                        } else {
                            break;
                        }
                    }
                    compressedPatterns.add(block);
                }
            }
        }

        // Create FPNode for each compressed patterns
        Map<Integer, FPNode> itemToNode = new HashMap<>();

        for (List<Integer> blockItems : compressedPatterns){
            FPNode node = new FPNode(blockItems);
            long sumUsage = itemUsageSum.getOrDefault(blockItems.get(0), 0L);

            node.setUsage(sumUsage);
            node.setNormalizedUsage((maxSupport > 0) ? (double) sumUsage / maxSupport : 0.0);

            for (int item : blockItems){
                itemToNode.put(item, node);
            }
        }

        // Link compresses patterns + pattern usage
        for (CtPattern p : activePatterns) {
            List<Integer> items = p.getItems();
            double lineUsage = (maxSupport > 0) ? (double) p.getUsage() / maxSupport : 0.0;
            for (int i = 0; i < items.size(); i++) {
                FPNode node = itemToNode.get(items.get(i));
                if (node != null){
                    node.addSourcePattern(p, lineUsage);
                }
                if (i < items.size() - 1) {
                    FPNode srcNode = node;
                    FPNode tgtNode = itemToNode.get(items.get(i + 1));
                    if (srcNode != null && tgtNode != null && srcNode != tgtNode) {
                        srcNode.addChild(tgtNode);
                    }
                }
            }
        }

        // Virtual node. Start/end
        FPNode root = new FPNode(-1);
        for (CtPattern p : activePatterns) {
            List<Integer> items = p.getItems();
            if (items.isEmpty()) continue;

            FPNode startNode = itemToNode.get(items.get(0));
            if (startNode != null){
                startNode.setStart(true);
             
                if (!root.getChildren().contains(startNode)) {
                    root.addChild(startNode);
                }
            }

            FPNode endNode = itemToNode.get(items.get(items.size() - 1));
            if (endNode != null) {
                endNode.setEnd(true);
            }
        }

        return root;
    }

}