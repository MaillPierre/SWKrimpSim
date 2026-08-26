package com.irisa.fptree;

public class ItemTranslation {
    
    private final String uri;
    private final String type;

    /**
     * Holds the translation of an item: its URI and type.
     *
     * @param uri The item's URI
     * @param type The item's category
     */
    public ItemTranslation(String uri, String type) {
        this.uri = uri;
        this.type = type;
    }

    public String getURI() {
        return uri;
    }

    public String getType() {
        return type;
    }
}