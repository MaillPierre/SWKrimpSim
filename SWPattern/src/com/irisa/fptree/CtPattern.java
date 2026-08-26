package com.irisa.fptree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A .ct pattern: items, usage and support.
 */
public class CtPattern {
    private List<Integer> items;
    private long usage;
    private int support;

    public CtPattern(List<Integer> items, int usage, int support) {
        this.items = new ArrayList<>(items);
        this.usage = usage;
        this.support = support;
    }

    public List<Integer> getItems() {
        return Collections.unmodifiableList(items);
    }

    public long getUsage() {
        return usage;
    }

    public int getSupport() {
        return support;
    }

    public boolean contains(int item) {
        return items.contains(item);
    }
}