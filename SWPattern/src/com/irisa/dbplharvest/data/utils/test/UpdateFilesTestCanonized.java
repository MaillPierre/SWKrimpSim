///////////////////////////////////////////////////////////////////////////////
//File: UpdateFilesTestCanonized.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Program to compare the canonized files vs the models canonized
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data.utils.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.Changeset;
import com.irisa.dbplharvest.data.ChangesetFile;

public class UpdateFilesTestCanonized {

private static Logger logger = Logger.getLogger(UpdateFilesTestCanonized.class);

// file ID is the the complete path, but for the .added.nt.gz or the .removed.nt.gz extension
public static String FILE_ID_OPTION = "fileList"; 
public static String HELP_OPTION = "help"; 

public static void main(String[] args) {

	BasicConfigurator.configure();
	PropertyConfigurator.configure("log4j-config.txt");

	CommandLineParser parser = new DefaultParser();
	Options options = new Options();
	options.addOption(FILE_ID_OPTION, true, "fileList is the file with the file IDs, i.d., their complete path, "
			+ "but for the .added.nt.gz or the .removed.nt.gz extension");
	options.addOption(HELP_OPTION, false, "display this help"); 

	Vector<String> wrongFiles = new Vector<String> (); 

	try  {
		CommandLine cmd = parser.parse( options, args);

		boolean helpAsked = cmd.hasOption(HELP_OPTION);
		if(helpAsked) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "OrientedMeasuresCalculator", options );
			System.exit(0);
		} 

		String fileListFilename = cmd.getOptionValue(FILE_ID_OPTION); 
		long start = System.nanoTime(); 

		// first, we read all the file names in memory to paralellize everything
		List<String> filenames= new ArrayList<String>(); 

		try (BufferedReader br = Files.newBufferedReader(Paths.get(fileListFilename))) {
			filenames =br.lines().collect(Collectors.toList()); 
		}
		catch (IOException e) { 
			e.printStackTrace();
		}

		if (!filenames.isEmpty()) {

			filenames.stream().forEach( fileID -> { 
				
				// we load the changeset canonizing
				
				StringTokenizer filenameParser = new StringTokenizer(fileID, File.separator);
				Stack<String> auxStack = new Stack<String>(); 
				while (filenameParser.hasMoreElements()) { 
					auxStack.push(filenameParser.nextToken());
				}
				
				String filenameBase = auxStack.pop(); 
				String number = auxStack.pop(); 
				String hour = auxStack.pop(); 
				String day = auxStack.pop(); 
				String month = auxStack.pop(); 
				String year = auxStack.pop();
				
				ChangesetFile changeFile = new ChangesetFile(year, month, day, hour, number, 
											fileID+ChangesetFile.ADDED_EXTENSION, fileID+ChangesetFile.DELETED_EXTENSION); 
				// we force the canonization
				Changeset changeset = new Changeset(changeFile, true); 
				// we check first the added model 
				try { 
					try { 
						Model mod = ModelFactory.createDefaultModel(); 
						File aux = new File (fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION); 
						if (aux.exists()) {
							RDFDataMgr.read(mod, fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION);
							StmtIterator stmtIt = mod.listStatements(); 
							boolean addOK = true; 
							while (stmtIt.hasNext() && addOK) { 
								addOK = changeset.getAddTriples().contains(stmtIt.next()); 
							}
							if (!addOK) { 
								wrongFiles.add(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION); 
							}
							else { 
								System.out.println(fileID+ChangesetFile.ADDED_EXTENSION+" Size: "+changeset.getAddTriples().size()); 
								System.out.println(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION+" Size: "+mod.size()); 
							}
						} 
						else { 
							if (!changeset.getAddTriples().isEmpty()) { 
								wrongFiles.add(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION); 
							}
							else { 
								System.out.println(fileID+ChangesetFile.ADDED_EXTENSION+" Empty");
								System.out.println(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION+" Empty");
							}
						}
					}
					catch (Exception e) { 
						logger.error(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION); 
						wrongFiles.add(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION); 
					}
					
					try { 
						Model mod = ModelFactory.createDefaultModel(); 
						File aux = new File (fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION); 
						if (aux.exists()) { 
							RDFDataMgr.read(mod, fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION);
							StmtIterator stmtIt = mod.listStatements(); 
							boolean removeOK = true; 
							while (stmtIt.hasNext() && removeOK) { 
								removeOK = changeset.getDelTriples().contains(stmtIt.next()); 
							}
							if (!removeOK) { 
								wrongFiles.add(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION); 
							}
							else { 
								System.out.println(fileID+ChangesetFile.DELETED_EXTENSION+" Size: "+changeset.getDelTriples().size()); 
								System.out.println(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION+" Size: "+mod.size()); 
							}
						}
						else { 
							if (!changeset.getDelTriples().isEmpty()) { 
								wrongFiles.add(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION); 
							}
							else { 
								System.out.println(fileID+ChangesetFile.DELETED_EXTENSION+" Empty");
								System.out.println(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION+" Empty");
							}
						}
					}
					catch (Exception e) { 
						logger.error(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION);
						wrongFiles.add(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION); 
					}
				} 
				catch (Exception e) { 
					logger.error(fileID+": couldn't be processed"); 
				}
			}

					); 
		}
		System.out.println("Processed : "+filenames.size());
		System.out.println("Finished after: "+ ( ((double)(System.nanoTime()-start))/1000000000)+" s."); 
		System.out.println("Wrong: "+wrongFiles.size()); 

		File resultsFile = new File(cmd.getOptionValue(FILE_ID_OPTION)+".wrong");
		PrintWriter out = null; 
		if (resultsFile.exists()) {
			resultsFile.delete(); 
		}	
		out = new PrintWriter(resultsFile); 

		for (String s: wrongFiles) { 
			out.println(s); 
		}
		out.flush(); 
		out.close();


	}
	catch (Exception e) {
		e.printStackTrace();
		System.exit(-1); 
	}
}

}

