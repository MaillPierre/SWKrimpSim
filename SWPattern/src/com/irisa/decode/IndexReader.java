package com.irisa.decode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexReader {

    /**
     * Reads the index (.idx).
     *
     * The last column is the original item identifier.
     * Everything before it is kept as the textual representation of the item.
     * 
     * @param index The index file (.idx)
     */
    public static Map<Integer, String> readIndex(Path index)
            throws IOException {

        Map<Integer, String> map = new HashMap<>();
        List<String> lines = Files.readAllLines(index);

        for (String line : lines) {

            line = line.trim();

            if (line.isEmpty()) continue;

            String[] parts = line.split("\\t");

            // Item identifier
            int id = Integer.parseInt(parts[parts.length - 1]);

            StringBuilder value = new StringBuilder();

            for (int i = 0; i < parts.length - 1; i++) {

                if (i > 0) value.append('\t');
                value.append(parts[i]);
            }
            map.put(id, value.toString());
        }
        return map;
    }
}