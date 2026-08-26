package com.irisa.decode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Main {

    /**
     * Main method for decoding a Vreeken code table into the original item identifiers.
     *
     * @param args The dataset name.
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: java -jar DecodeVreeken.jar <dataset>");
            System.exit(1);
        }

        String dataset = args[0];
        Path resultsDir = Paths.get("results", dataset + "-output");

        if (!Files.isDirectory(resultsDir)) {
            System.err.println("Results directory not found:");
            System.err.println(resultsDir);
            System.exit(1);
        }

        Path ct = resultsDir.resolve("ct-latest.ct");
        Path analysis = resultsDir.resolve(dataset + ".db.analysis.txt");
        Path output = resultsDir.resolve("ct-decoded.ct");

        if (!Files.exists(ct)) {
            System.err.println("Missing file: " + ct);
            System.exit(1);
        }

        if (!Files.exists(analysis)) {
            System.err.println("Missing file: " + analysis);
            System.exit(1);
        }

        try {

            Map<Integer,Integer> conversion = AnalysisReader.readConversionTable(analysis);

            CtDecoder.decode(ct, output, conversion);

            System.out.println("Decoded code table written to:");
            System.out.println(output);

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}