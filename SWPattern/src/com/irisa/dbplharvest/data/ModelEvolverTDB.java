///////////////////////////////////////////////////////////////////////////////
//File: ModelEvolverTDB.java 
//Author: Carlos Bobed
//Date: September 2018
//Comments: 
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.tdb.TDBFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.krimp.data.KItemset;
import com.irisa.swpatterns.data.AttributeIndex;

public class ModelEvolverTDB {

private static Logger logger = Logger.getLogger(ModelEvolverTDB.class);
	
	public static String INPUT_INDEX = "inputIndex"; 
	public static String OUTPUT_INDEX = "outputIndex"; 
	public static String UPDATE_LIST_OPTION = "updateList"; 
	public static String OUTPUT_STATS_OPTION = "outputStats";
	public static String TDB_LOCATION_OPTION = "TDBPath"; 
	public static String PREVIOUSLY_CANONIZED_OPTION = "alreadyCanonized"; 
	public static String HELP_OPTION = "help";
	
	public static String TRANSACTIONS_EXTENSION = ".trans"; 
	public static String NO_TRANSACTIONS_EXTENSION = ".noTrans";
	public static String SEPARATED_RES_EXTENSION = ".separated.nt";
	
	
	
	public static String STATES_SEPARATOR="----";
	
	public static void main(String[] args) {
		
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(TDB_LOCATION_OPTION, true, "tdb location path"); 
		options.addOption(INPUT_INDEX, true, "filename of the index used to translate the transactions");
		options.addOption(OUTPUT_STATS_OPTION, true, "output file for the stats");
		options.addOption(OUTPUT_INDEX, true, "filename of the resulting index, useful to check non-seen properties"); 
		options.addOption(UPDATE_LIST_OPTION, true, "filename with the list of update files, ordered by date and hour");
		options.addOption(PREVIOUSLY_CANONIZED_OPTION, false, "forces to work with the already canonized version of the updates");
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 
			
			// we read the index
			// recall: its singleton
			AttributeIndex index = AttributeIndex.getInstance();
			if(cmd.hasOption(INPUT_INDEX)) {
				index.readAttributeIndex(cmd.getOptionValue(INPUT_INDEX));
			}
			
			String tdbPath = cmd.getOptionValue(TDB_LOCATION_OPTION); 
			
			long initialIndexSize = AttributeIndex.getInstance().size(); 
			long finalIndexSize = 0; 
			
			long modelRelatedTimeStart = System.nanoTime(); 
			long accumEvolvingTime = 0; 
			
			
			Dataset originalDataset = TDBFactory.createDataset(tdbPath); 
			long modelLoadingTime = System.nanoTime()-modelRelatedTimeStart; 
			
			System.err.println("Model loaded after "+((double)modelLoadingTime)/1000000000.0 + " s."); 
			// we force a garbage collection
			System.gc();
			
			String fileListFilename = cmd.getOptionValue(UPDATE_LIST_OPTION); 
			long start = System.nanoTime(); 
			long progress = 0; 
			long deltasProcessed = 0; // number of inner updates  
			long filteredTriples = 0; // triples removed from add and del 
			long actualUpdateSizes = 0; // the triples in the add and del after filtering
			long emptyUpdates = 0; 
			
			BufferedReader br = Files.newBufferedReader(Paths.get(fileListFilename)); 
			String currentUpdateFilename = null; 
			
			ChangesetTransactionConverterTDB converter = new ChangesetTransactionConverterTDB(); 
			converter.setDataset(originalDataset);
			
			while ( (currentUpdateFilename = br.readLine()) != null) { 
				progress++; 
				if (progress%100 == 0) { 
					System.out.println(progress + " already processed "); 
					// after checking the memory issues, 
					// we force collection more frequently
					System.gc();
				}
				
				// we prepare the changesetFile 
				try { 
					
					StringTokenizer filenameParser = new StringTokenizer(currentUpdateFilename, File.separator);
					Stack<String> auxStack = new Stack<String>(); 
					while (filenameParser.hasMoreElements()) { 
						auxStack.push(filenameParser.nextToken());
					}
					
					String number = auxStack.pop(); 
					String hour = auxStack.pop(); 
					String day = auxStack.pop(); 
					String month = auxStack.pop(); 
					String year = auxStack.pop();
					
					// if they haven't been previously canonized, we have to do it 
					// at creation time
					boolean canonizing = !cmd.hasOption(PREVIOUSLY_CANONIZED_OPTION); 
					
					ChangesetFile changeFile = null; 
					
					if (canonizing) { 
						changeFile = new ChangesetFile(year, month, day, hour, number, 
												currentUpdateFilename+ChangesetFile.ADDED_EXTENSION, currentUpdateFilename+ChangesetFile.DELETED_EXTENSION); 
					} 
					else { 
						changeFile = new ChangesetFile(year, month, day, hour, number, 
								currentUpdateFilename+ChangesetFile.ADDED_CANONIZED_EXTENSION, 
								currentUpdateFilename+ChangesetFile.DELETED_CANONIZED_EXTENSION);
					}
					Changeset changeset = new Changeset(changeFile, canonizing);  
					
					if (Files.exists(Paths.get(currentUpdateFilename+SEPARATED_RES_EXTENSION))) { 
						logger.debug("reading the affected resources ...");
						BufferedReader in = Files.newBufferedReader(Paths.get(currentUpdateFilename+SEPARATED_RES_EXTENSION)); 
						changeset.readAffectedResources(in);
						in.close();
					}
					else { 
						// we force to calculate them if they are not precalculated
						logger.debug("calculating the affected resources ... ");
						changeset.getAffectedResources(); 
					}
					
					// we only apply the update if it is not empty
					if (changeset.getUpdateSize() !=0) {
						
						// the affected resources are calculated after filtering the 
						// triples (while reading) so we have only affected resources 
						// according to the filtered vocabulary (given by the index calculated 
						// in the conversion of the main versions)
						
						// we update the stats 
						// the number of aggregated affected resources
						deltasProcessed += changeset.getAffectedResources().size(); 
						filteredTriples += changeset.getNumberFilteredTriples(); 
						actualUpdateSizes += changeset.getUpdateSize(); 
						
						// we get the first set of transactions
						// before applying the evolution
						
						HashMap<Resource, KItemset> initialTransactions = converter.extractTransactionsFromAffectedResources(changeset); 
						
						modelRelatedTimeStart = System.nanoTime();		
						
						originalDataset.begin(ReadWrite.WRITE);						
						// we now apply the modification to the model
						if (changeset.getDelTriples() != null) { 
							originalDataset.getDefaultModel().remove(changeset.getDelTriples().listStatements());
						}
						if (changeset.getAddTriples() != null) { 
							originalDataset.getDefaultModel().add(changeset.getAddTriples().listStatements());
						}
						originalDataset.commit();
						originalDataset.end(); 
						
						accumEvolvingTime += (System.nanoTime()-modelRelatedTimeStart); 
						
						HashMap<Resource, KItemset> finalTransactions = converter.extractTransactionsFromAffectedResources(changeset); 
						
						
						File resultsFile = new File(currentUpdateFilename+TRANSACTIONS_EXTENSION);
						PrintWriter out = null; 
						if (resultsFile.exists()) {
							resultsFile.delete(); 
						}	
						out = new PrintWriter(resultsFile); 
						
						Iterator<HashSet<Resource>> itSets = changeset.getAffectedResources().iterator();
						HashSet<Resource> auxSet = null; 
						Iterator<Resource> itRes = null; 
						Resource auxRes = null; 
						
						while (itSets.hasNext()) { 
							auxSet = itSets.next();
							// we write the original state
							itRes = auxSet.iterator(); 
							while (itRes.hasNext()) { 
								auxRes= itRes.next(); 
								if (initialTransactions.containsKey(auxRes)) { 
									out.println(initialTransactions.get(auxRes));
								}
							}
							out.println(STATES_SEPARATOR); 
							// now we write the final state
							itRes = auxSet.iterator(); 
							while (itRes.hasNext()) { 
								auxRes = itRes.next(); 
								if (finalTransactions.containsKey(auxRes)) { 
									out.println(finalTransactions.get(auxRes)); 
								}
							}
							// we separate the groups by a blank line
							out.println(); 
						}
						
						out.flush();
						out.close();
					} 
					else { 
						logger.debug("empty update ...");
						emptyUpdates++; 
						
						File resultsFile = new File(currentUpdateFilename+TRANSACTIONS_EXTENSION);
						if (resultsFile.exists()) {
							resultsFile.delete(); 
						}	
						resultsFile = new File(currentUpdateFilename+NO_TRANSACTIONS_EXTENSION); 
						resultsFile.createNewFile(); 
						
					}
					
					changeset.closeResources();
					
				} 
				catch (IOException e) { 
					logger.error(currentUpdateFilename+": couldn't be processed"); 
				}
			}
			
			finalIndexSize = AttributeIndex.getInstance().size(); 
			
			originalDataset.close(); 
			
			if (cmd.hasOption(OUTPUT_STATS_OPTION)) { 
				
				File statsFile = new File(cmd.getOptionValue(OUTPUT_STATS_OPTION));
				
				if (statsFile.exists()) {
					statsFile.delete(); 
				}
				PrintWriter out = new PrintWriter(statsFile); 

				out.println("TDB: "+cmd.getOptionValue(tdbPath));
				out.println();
				out.println("UpdateList: "+cmd.getOptionValue(UPDATE_LIST_OPTION)); 
				out.println("## Updates: "+progress); 
				out.println("---- Empty ones: "+ emptyUpdates); 
				out.println("---- Updates size (triples): "+actualUpdateSizes);
				out.println("---- Filtered triples: "+ filteredTriples);
				out.println("---- Mean update size (triples): "+ ((double)actualUpdateSizes)/(double)progress); 
				out.println("---- Mean update size excluding empty ones (triples): "+ ((double)actualUpdateSizes)/(double)(progress-emptyUpdates)); 
				out.println("## Deltas: "+deltasProcessed);
				out.println("---- Mean delta size (triples): "+ ((double)actualUpdateSizes)/(double)deltasProcessed); 
				out.println();
				out.println("Index: "+cmd.getOptionValue(INPUT_INDEX)); 
				out.println("## Initial items: "+initialIndexSize); 
				out.println("## Final items: "+finalIndexSize); 
				out.println("## Non-seen items in the conversion: "+ (finalIndexSize-initialIndexSize));
				out.println(); 
				out.println("Execution time: "+( ((double)(System.nanoTime()-start))/1000000000)+" s.");
				out.println("Model loading time: "+((double)modelLoadingTime/1000000000.0)+ " s.");
				out.println("Model evolving time: "+((double)accumEvolvingTime/1000000000.0)+" s."); 
				out.println("## Mean evolving step time: "+(((double)accumEvolvingTime/(double)(progress-emptyUpdates))/1000000000.0) + " s. "); 
				out.flush();
				out.close();
				
			}
			
			System.out.println("Processed : "+progress);
			System.out.println("Finished after: "+ ( ((double)(System.nanoTime()-start))/1000000000)+" s.");
			if (cmd.hasOption(OUTPUT_INDEX)) { 
				index.printAttributeIndex(cmd.getOptionValue(OUTPUT_INDEX));
			} 
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
}
