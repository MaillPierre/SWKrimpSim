///////////////////////////////////////////////////////////////////////////////
//File: UpdateFilesCanonizer.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Program which filters out the triples of the updates that are not 
// 		within the scope of the vocabulary analyzed. It uses the canonalize 
// 		function to select them. In this way, the evolver does not have to 
// 		do it in a sequential way. 
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.Changeset;
import com.irisa.dbplharvest.data.ChangesetFile;

public class UpdateFilesCanonizer {

private static Logger logger = Logger.getLogger(UpdateFilesCanonizer.class);

// file ID is the the complete path, but for the .added.nt.gz or the .removed.nt.gz extension
public static String FILE_ID_OPTION = "fileList"; 
public static String HELP_OPTION = "help"; 

public static String OUTPUT_EXTENSION = ".separated.nt"; 

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

			filenames.parallelStream().forEach( fileID -> { 

				try { 
					try { 
						Model mod = ModelFactory.createDefaultModel(); 
						File aux = new File (fileID+ChangesetFile.ADDED_EXTENSION); 
						if (aux.exists()) {
							RDFDataMgr.read(mod, fileID+ChangesetFile.ADDED_EXTENSION);
							LinkedList<Statement> stmtsToRemove = new LinkedList<Statement> (); 
							mod.listStatements().filterDrop(stmt -> { 
								boolean subjectOk = Changeset.filterAcceptURI(stmt.getSubject());
								boolean objectOk = stmt.getObject().isLiteral() 
										|| (stmt.getObject().isResource() 
												&& Changeset.filterAcceptURI(stmt.getObject().asResource()));
								boolean relationOK = Changeset.filterAcceptURI(stmt.getPredicate());
								return subjectOk && objectOk && relationOK;
							} ).forEachRemaining(stmt -> stmtsToRemove.add(stmt)); 
							mod.remove(stmtsToRemove); 
							if (!mod.isEmpty()) { 
								// we write the canonized version
								OutputStream outStream = new GZIPOutputStream(new FileOutputStream(
											new File(fileID+ChangesetFile.ADDED_CANONIZED_EXTENSION))); 
								
								RDFDataMgr.write(outStream, mod, RDFFormat.NTRIPLES_UTF8); 
								outStream.flush();
								outStream.close();
							}
						}
					}
					catch (Exception e) { 
						logger.error(fileID+ChangesetFile.ADDED_EXTENSION); 
						wrongFiles.add(fileID+ChangesetFile.ADDED_EXTENSION); 
					}
					
					try { 
						Model mod = ModelFactory.createDefaultModel(); 
						File aux = new File (fileID+ChangesetFile.DELETED_EXTENSION); 
						if (aux.exists()) { 
							RDFDataMgr.read(mod, fileID+ChangesetFile.DELETED_EXTENSION);
							LinkedList<Statement> stmtsToRemove = new LinkedList<Statement> (); 
							mod.listStatements().filterDrop(stmt -> { 
								boolean subjectOk = Changeset.filterAcceptURI(stmt.getSubject());
								boolean objectOk = stmt.getObject().isLiteral() 
										|| (stmt.getObject().isResource() 
												&& Changeset.filterAcceptURI(stmt.getObject().asResource()));
								boolean relationOK = Changeset.filterAcceptURI(stmt.getPredicate());
								return subjectOk && objectOk && relationOK;
							} ).forEachRemaining(stmt -> stmtsToRemove.add(stmt)); 
							mod.remove(stmtsToRemove); 
							if (!mod.isEmpty()) { 
								// we write the canonized version
								OutputStream outStream = new GZIPOutputStream(new FileOutputStream(
											new File(fileID+ChangesetFile.DELETED_CANONIZED_EXTENSION))); 
								
								RDFDataMgr.write(outStream, mod, RDFFormat.NTRIPLES_UTF8); 
								outStream.flush();
								outStream.close();
							}
						}
					}
					catch (Exception e) { 
						logger.error(fileID+ChangesetFile.DELETED_EXTENSION);
						wrongFiles.add(fileID+ChangesetFile.DELETED_EXTENSION); 
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

