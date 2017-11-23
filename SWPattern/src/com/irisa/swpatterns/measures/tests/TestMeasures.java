
///////////////////////////////////////////////////////////////////////////////
//File: TestMeasures.java 
//Author: Carlos Bobed
//Date: November 2017
//Comments: Program to calculate both distances  
//Modifications: 
//Example command line:
// For comparing and recalculating the measures, load using our table format: 
// -comparedCT iswc-2012-complete.nt.Property.krimp.dat 
// -originalCT ekaw-2016-complete.nt.Property.krimp.dat 
// -originalDataset ekaw-2016-complete.nt.dbProperty.dat 
// -dataset iswc-2012-complete.nt.dbProperty.dat 
// -measure regular 
// -recalculate

// For comparing and recalculating, load using Vreeken's format: 
// -comparedCT CTFilename 
// -comparedDBAnalysis DBAnalysisFilename
// -originalCT CTFilename 
// -originalDataset DBAnalysisFilename 
// -originalDBAnalysis DBAnalysisFilename
// -dataset DatabaseFilename 
// -measure regular 
// -recalculate 
// -vreekenFormat
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.tests;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.measures.CodificationMeasure;
import com.irisa.swpatterns.measures.Measures;

public class TestMeasures {

	public static String MEASURE_OPTION = "measure"; 
	public static String ORIGINAL_CT_OPTION ="originalCT"; 
	public static String ORIGINAL_DB_ANALYSIS_OPTION = "originalDBAnalysis"; 
	public static String COMPARED_CT_OPTION = "comparedCT"; 
	public static String COMPARED_DB_ANALYSIS_OPTION = "comparedDBAnalysis"; 
	public static String DATASET_OPTION = "dataset"; 
	public static String VREEKEN_OPTION = "vreekenFormat"; 
	public static String HELP_OPTION = "help"; 
	public static String ORIGINAL_DATASET = "originalDataset"; 

	public static String RESULTS_HEADERS = "originalCT;comparedCT;comparedDB;ourFormat;measure;measureValue;execTime";  

	public static void main(String[] args) {

		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(MEASURE_OPTION, true, "measure type to be used - regular|usingLengths|keepingUsages"); 
		options.addOption(ORIGINAL_CT_OPTION, true, "codeTable of the dataset AGAINST which we compare the dataset");
		options.addOption(ORIGINAL_DB_ANALYSIS_OPTION, true, "original db analysis file required to read Vreeken CTs "); 
		options.addOption(COMPARED_CT_OPTION, true, "codeTable of the compared dataset"); 
		options.addOption(COMPARED_DB_ANALYSIS_OPTION, true, "compared db analysis file required to read Vreeken CTs"); 
		options.addOption(DATASET_OPTION, true, "compared dataset"); 
		options.addOption(VREEKEN_OPTION, false, "whether we use or not the Vreeken Format");
		options.addOption(HELP_OPTION, false, "display this help"); 
		options.addOption(ORIGINAL_DATASET, true, "original dataset");
		try  {
			CommandLine cmd = parser.parse( options, args);

			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 

			String measure = cmd.getOptionValue(MEASURE_OPTION); 
			String datasetFilename = cmd.getOptionValue(DATASET_OPTION); 
			String originalDatasetFilename = cmd.getOptionValue(ORIGINAL_DATASET); 

			String originalCTFilename = cmd.getOptionValue(ORIGINAL_CT_OPTION);
			String comparedCTFilename = cmd.getOptionValue(COMPARED_CT_OPTION);
			ItemsetSet originalItemCT = null; 
			ItemsetSet comparedItemCT = null; 

			if (cmd.hasOption(VREEKEN_OPTION)) {
				String originalDBAnalysisFilename = cmd.getOptionValue(ORIGINAL_DB_ANALYSIS_OPTION);
				String comparedDBAnalysisFilename = cmd.getOptionValue(COMPARED_DB_ANALYSIS_OPTION);
				originalItemCT = Utils.readVreekenEtAlCodeTable(originalCTFilename, originalDBAnalysisFilename); 
				comparedItemCT = Utils.readVreekenEtAlCodeTable(comparedCTFilename, comparedDBAnalysisFilename); 
			}
			else {
				originalItemCT = Utils.readItemsetSetFile(originalCTFilename); 
				comparedItemCT = Utils.readItemsetSetFile(comparedCTFilename); 
			}

			ItemsetSet transactions = Utils.readItemsetSetFile(datasetFilename); 
			ItemsetSet originalTransactions = Utils.readItemsetSetFile(originalDatasetFilename);
			
//			DataIndexes index = new DataIndexes(transactions); 
			CodeTable comparedCT = new CodeTable(comparedItemCT); 
			CodeTable originalCT = new CodeTable(originalItemCT); 
			
			System.out.println(comparedCT);
			System.out.println(originalCT);

			System.out.println("------------------");
			System.out.println("originalTransactionsFile: " + originalDatasetFilename);
			System.out.println("Size: "+originalTransactions.size());
			System.out.println("Density: "+originalTransactions.density());

			System.out.println("------------------");
			System.out.println("TransactionsFile: " + datasetFilename);
			System.out.println("Size: "+transactions.size());
			System.out.println("Density: "+transactions.density());

			System.out.println("-----------");
			System.out.println("OriginalCT: " + originalCTFilename);
			System.out.println("Size: "+originalCT.getCodes().size()); 
			Utils.printCodeTableCodes(originalItemCT, "originalCT-output.dat");
			

			System.out.println("-----------");
			System.out.println("ComparedCT: " + comparedCTFilename);
			System.out.println("Size: "+comparedCT.getCodes().size());
			Utils.printCodeTableCodes(comparedItemCT, "comparedCT-output.dat");
			System.out.println("-----------");
			
			double value = -1.0; 
			double opposedValue = -1.0; 
			double refValue =-1.0; 
			double evalValue = -1.0; 
			double oldMeasure = -1.0; 
			
			System.out.println("Measure: " + measure);
			
			switch (measure) {
			case "regular":
				value = Measures.structuralSimilarityWithoutKeepingDistribution(transactions, comparedCT, originalCT);
				opposedValue  = Measures.structuralSimilarityWithoutKeepingDistribution(originalTransactions, originalCT, comparedCT);
				break; 
			case "usingLengths": 
				value = Measures.structuralSimilarityWithoutKeepingDistributionUsingLengths(transactions, comparedCT, originalCT); 
				opposedValue = Measures.structuralSimilarityWithoutKeepingDistributionUsingLengths(originalTransactions, originalCT, comparedCT); 
				break; 
			case "keepingUsages":
				value = Measures.structuralSimilarityKeepingDistribution(transactions, comparedCT, originalCT);
				opposedValue = Measures.structuralSimilarityKeepingDistribution(originalTransactions, originalCT, comparedCT);
				break;
			default: 
				break; 
			}

			CodificationMeasure oldCodMeasure1 = new CodificationMeasure(transactions, comparedCT);
			evalValue = oldCodMeasure1.codificationLength(); 
			
			CodificationMeasure oldCodMeasure2 = new CodificationMeasure(transactions, originalCT);
			oldCodMeasure2.updateUsages();
			refValue = oldCodMeasure2.codificationLength();
			
			oldMeasure = refValue / evalValue; 
						
			System.out.println("OriginalCTFilename: "+originalCTFilename);
			System.out.println("ComparedCTFilename: "+comparedCTFilename); 
			System.out.println("OriginalDatasetFilename: "+originalDatasetFilename); 
			System.out.println("DatasetFilename: "+datasetFilename); 
			System.out.println("Vreeken Format: "+cmd.hasOption(VREEKEN_OPTION)); 
			System.out.println("OldMeasure: "+oldMeasure);
			System.out.println("Measure: "+value);
			System.out.println("Opposed Measure: "+opposedValue);
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}

