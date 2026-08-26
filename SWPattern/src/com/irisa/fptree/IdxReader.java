package com.irisa.fptree;

import java.io.IOException;
import java.nio.file.NoSuchFileException; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class IdxReader {

    /**
     * Reads the item translations from a .idx file: each line has the item's
     * URI tokens, its type, and its original.
     *
     * @param idxFile The index file (.idx)
     */
    public static Map<Integer, ItemTranslation> readTranslationsFromFile(Path idxFile) throws IOException {
        
        Map<Integer, ItemTranslation> translations = new HashMap<>();

        List<String> lines;
        try {
            lines = Files.readAllLines(idxFile);

        } catch (NoSuchFileException e) {
            throw new IOException(".idx file does not exist: " + idxFile, e);
        }

        try {
            for (String s : lines) {

                String line = s.trim();
                if (line.isEmpty()) continue;

                String[] tokens = line.split("\t");

                String type = tokens[tokens.length - 2];
                int id = Integer.parseInt(tokens[tokens.length - 1].trim());
 
                String[] uriTokens = Arrays.copyOfRange(tokens, 0, tokens.length - 2);
                String uri = String.join(", ", uriTokens);
 
                translations.put(id, new ItemTranslation(uri, type));
            }

        } catch (Exception e) {
            throw new IOException("Error parsing .idx file: " + idxFile, e);
        }
        return translations;
    }
}