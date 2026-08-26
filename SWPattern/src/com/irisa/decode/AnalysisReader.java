package com.irisa.decode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisReader {

    /**
     * Reads the Vreeken-to-original item identifier mapping stored
     * in the database analysis file.
     * 
     *      Vreeken item id => original item id
     * 
     * @param analysis The database analysis file (.db.analysis.txt)
     */
    public static Map<Integer,Integer> readConversionTable(Path analysis)
            throws IOException {

        Map<Integer,Integer> map = new HashMap<>();
        List<String> lines = Files.readAllLines(analysis);

        // The item-id conversion table is located between the "* Alphabet"
        // and "* Row lengths:" sections in the analysis file
        boolean alphabet = false;

        for(String line : lines) {

            line = line.trim();

            if(line.equals("* Alphabet")) {
                alphabet = true;
                continue;
            }

            if(line.equals("* Row lengths:")) break;

            if(alphabet && line.contains("=>")) {
                String[] parts = line.split("=>");
                int vreekenItem = Integer.parseInt(parts[0].trim());
                int ourItem = Integer.parseInt(parts[1].trim().split("\\s+")[0]);
                map.put(vreekenItem, ourItem);
            }

        }

        return map;
    }
}