package com.irisa.fptree;

/**
 * Priority order used by SortCtFile to reorder pattern items
 * Declaration order is output order. Unknow types fall back to OTHER
 */
public enum ItemCategory {

    TYPE,
    IN_PROPERTY, 
    IN_NEIGHBOUR_TYPE,
    OUT_PROPERTY, 
    OUT_NEIGHBOUR_TYPE,
    OTHER;

    /**
     * Translate type string into its corresponding category
     * 
     * @param type The item type
     */
    public static ItemCategory fromType(String type) {
        if (type == null) return OTHER;
        try {
            return ItemCategory.valueOf(type);
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
    
}
