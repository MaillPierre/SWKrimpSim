package com.irisa.fptree;

import java.io.IOException;
import java.nio.file.NoSuchFileException; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CtReader {

    /**
     * Reads the patterns from a .ct file.
     * Each line is a list of item ids followed by a "(usage,support)" token.
     *
     * @param filePath The .ct file
     */
    public static List<CtPattern> readPatternsFromFile(Path filePath) throws IOException {

        List<CtPattern> patterns = new ArrayList<>();

        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (NoSuchFileException e) {
            throw new IOException(".ct file does not exist: " + filePath, e);
        }

        try {
            for (int i = 2; i < lines.size(); i++) {
 
                String line = lines.get(i).trim();

                if (line.isEmpty()) continue;

                String[] tokens = line.split("\\s+");

                List<Integer> items = new ArrayList<>();

                int usage = 0, support = 0;

                for (String token : tokens) {

                    if (token.startsWith("(")) {

                        String[] counts = token.substring(1, token.length() - 1).split(",");
                        usage = Integer.parseInt(counts[0]);
                        support = Integer.parseInt(counts[1]);
                        break;
                    }

                    items.add(Integer.parseInt(token));
                }

                patterns.add(new CtPattern(items, usage, support));
            }
        } catch (Exception e) {
            throw new IOException("Error parsing .ct file: " + filePath, e);
        }

        return patterns;
    }
}