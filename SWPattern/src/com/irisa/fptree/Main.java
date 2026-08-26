package com.irisa.fptree;

import com.irisa.decode.AnalysisReader;
import com.irisa.decode.CtDecoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Main {

    private static void printUsage() {
        System.err.println("Usage: java -jar FPTreeBuilder.jar <dataset> [selectedItem] [--sorted]");
        System.err.println("  <dataset>       required, dataset name (results/<dataset>-output)");
        System.err.println("  [selectedItem]  optional, filters the graph by this item; if omitted, the full graph is built");
        System.err.println("  [--sorted]      optional, reorders each pattern (TYPE items first) before building the graph");
    }

    /**
     * Reads a dataset's Vreeken code table, decodes it, optionally reorders it
     * and filters it by item, builds the compacted graph, and writes the
     * resulting JSON to results/<dataset>-output/.
     * 
     * @param args <dataset> [selectedItem] [--sorted], see printUsage()
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            printUsage();
            System.exit(1);
            return;
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
        Path idx = resultsDir.resolve("conversionIndex.idx");
        Path ct_decoded = resultsDir.resolve("ct-decoded.ct");
        Path ct_sorted = resultsDir.resolve("ct-sorted.ct");
 
        if (!Files.exists(ct)) {
            System.err.println("Error: missing file: " + ct);
            System.exit(1);
            return;
        }

        if (!Files.exists(analysis)) {
            System.err.println("Missing file: " + analysis);
            System.exit(1);
            return;
        }

        if (!Files.exists(idx)) {
            System.err.println("Error: missing file: " + idx);
            System.exit(1);
            return;
        }

        boolean sorted = false;
        Integer selectedItemArg = null;

        for (int i = 1; i < args.length; i++){
            if (args[i].equals("--sorted")) {
                sorted = true;
                continue;
            }

            try {
                selectedItemArg = Integer.parseInt(args[i]);
            } catch (NumberFormatException e) {
                System.err.println("Error: unrecognized argument \"" + args[i] + "\"");
                printUsage();
                System.exit(1);
                return;
            }
        }

        final Integer selectedItem = selectedItemArg;

        try {

            Map<Integer,Integer> conversion = AnalysisReader.readConversionTable(analysis);
            Map<Integer, ItemTranslation> translations = IdxReader.readTranslationsFromFile(idx);

            CtDecoder.decode(ct, ct_decoded, conversion);
            System.out.println("Decoded code table written to:");
            System.out.println("    " + ct_decoded + "\n"); 

            Path ctToUse = ct;
            if (sorted) {
                SortCtFile.sortFile(ct, ct_sorted, conversion, translations);
                System.out.println("Sorted code table written to:");
                System.out.println("    " + ct_sorted + "\n");
                ctToUse = ct_sorted;
            }

            List<CtPattern> patterns = CtReader.readPatternsFromFile(ctToUse);
            String outputName = "fptree-" + (selectedItem != null ? selectedItem : "all") + 
                                (sorted ? "-sorted" : "") + ".json";

            if (selectedItem != null && (patterns.stream().noneMatch(p -> p.contains(selectedItem)))) {
                System.err.println("Error: the selected item " + selectedItem + " is not present in any pattern.");
                System.exit(1);
                return;
            }
            
            Path output = resultsDir.resolve(outputName);

            FPTreeBuilder builder = new FPTreeBuilder();
            FPNode root = builder.build(patterns, selectedItem);
 
            JsonExporter.export(root, translations, conversion, output);
 
            System.out.println("JSON generated in " + output);
 
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}