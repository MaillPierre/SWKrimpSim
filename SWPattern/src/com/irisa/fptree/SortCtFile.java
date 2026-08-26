package com.irisa.fptree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;

public class SortCtFile {

    /**
     * Reorders a Vreeken code table so that, in each pattern, following the ItemCategory order.
     *
     * @param input The Vreeken code table (.ct)
     * @param output The reordered code table (.ct)
     * @param conversion The mapping from Vreeken item identifiers to original item identifiers
     * @param translations The mapping from original item identifiers to their translation (uri, type)
     */
    public static void sortFile(Path input, Path output, Map<Integer,Integer> conversion, 
                                Map<Integer, ItemTranslation> translations) throws IOException {

        List<String> lines = Files.readAllLines(input);
        BufferedWriter writer = Files.newBufferedWriter(output);

        for(int i=0; i<lines.size(); i++) {
            String line = lines.get(i).trim();

            if(line.isEmpty()) continue;

            if (i < 2) {
                writer.write(line);
                writer.newLine();
                continue;
            }

            String[] tokens = line.split("\\s+");
            int j=0;

            List<Integer> items = new ArrayList<>();
            while(j < tokens.length && !tokens[j].startsWith("(")) {
                
                items.add(Integer.parseInt(tokens[j]));
                j++;
            }

            items.sort(Comparator.comparingInt(v -> categoryOf(v, translations).ordinal()));


            StringBuilder out = new StringBuilder();
            for (int item : items) {
                out.append(item).append(" ");
            }

            // Copy the (usage,support)
            while(j<tokens.length) {
                out.append(tokens[j]);
                out.append(" ");
                j++;
            }

            writer.write(out.toString().trim());
            writer.newLine();

        }
        writer.close();
    }

    private static ItemCategory categoryOf(int vreeken, Map<Integer, ItemTranslation> translations) {
        ItemTranslation translation = translations.get(vreeken);
        String type = translation != null ? translation.getType() : null;

        return ItemCategory.fromType(type);
    }
}