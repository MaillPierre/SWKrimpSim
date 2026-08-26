package com.irisa.fptree;

import java.util.*;

/**
 * A compacted node of the FP-tree graph: one or more items that always
 * co-occur together as one block, with their usage, start/end role, and
 * source .ct patterns.
 */
public class FPNode {
    private final List<Integer> items;
    private long usage;
    private long support;
    private double normalizedUsage;
    private final List<FPNode> children = new ArrayList<>();
    private boolean start, end;

    private final List<CtPattern> sourcePatterns = new ArrayList<>();
    private final Set<CtPattern> sourcePatternsSeen = new HashSet<>();
    private final Map<CtPattern, Double> patternUsage = new LinkedHashMap<>();


    public FPNode(List<Integer> items) {
        this.items = (items != null) ? new ArrayList<>(items) : Collections.emptyList();
    }

    public FPNode(int singleItem) {
        this.items = new ArrayList<>();
        if (singleItem != -1) {
            this.items.add(singleItem);
        }
    }

    public List<Integer> getItems() {
        return items;
    }

    public int getItem() {
        return items.isEmpty() ? -1 : items.get(0);
    }

    public long getUsage() {
        return usage;
    }

    public void setUsage(long usage) {
        this.usage = usage;
    }

    public double getNormalizedUsage() {
        return normalizedUsage;
    }

    public void setNormalizedUsage(double normalizedUsage) {
        this.normalizedUsage = normalizedUsage;
    }

    public long getSupport() {
        return support;
    }

    public void setSupport(long support) {
        this.support = support;
    }

    public Collection<FPNode> getChildren() {
        return children;
    }

    public void addChild(FPNode child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    public boolean isRoot() {
        return items.isEmpty();
    }

    public boolean isStart() {
        return start;
    }
 
    public void setStart(boolean start) {
        this.start = start;
    }
 
    public boolean isEnd() {
        return end;
    }
 
    public void setEnd(boolean end) {
        this.end = end;
    }

    public void addCounts(long usage, long support) {
        this.usage += usage;
        this.support += support;
    }

    public void addSourcePattern(CtPattern pattern, double lineUsage) {
        if (sourcePatternsSeen.add(pattern)) {
            sourcePatterns.add(pattern);
            patternUsage.put(pattern, lineUsage);
        }
    }
 
    public List<CtPattern> getSourcePatterns() {
        return sourcePatterns;
    }
 
    public double getPatternUsage(CtPattern pattern) {
        return patternUsage.getOrDefault(pattern, 0.0);
    }

}