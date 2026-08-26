package com.irisa.decode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CtDecoder {

    /**
     * Decodes a Vreeken code table into the original item identifiers.
     *
     * @param input The Vreeken code table (.ct)
     * @param output The decoded code table (.ct)
     * @param conversion The mapping from Vreeken item identifiers to original item identifiers
     */
    public static void decode(Path input, Path output, Map<Integer,Integer> conversion)
            throws IOException {

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

            StringBuilder out = new StringBuilder();
            String[] tokens = line.split("\\s+");
            int j=0;

            while(j<tokens.length && !tokens[j].startsWith("(")) {
                
                int vreeken = Integer.parseInt(tokens[j]);

                // Replace the Vreeken code with the original code
                out.append(conversion.get(vreeken));
                out.append(" ");
                j++;
            }

            // Copy (usage,support)
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
}