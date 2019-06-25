///////////////////////////////////////////////////////////////////////////////
// File: WalkGenerator.java 
// Author: Carlos Bobed 
// Date: June 2019
// Comments: Random walk generator required for obtaining the required training 
// 		to update the rdf2vec model and obtain the new embeddings of the 
// 		updated resources 
// 		The current code is an adaptation of WalkGenerator class in rdf2vec 
// 		project by Petar Ristoski, Jessica Rosati, Tommaso Di Noia,  Renato De Leone 
// 		and Heiko Paulheim. Their approach is presented in several papers, 
// 		the most complete being: 
// 		
// 		Ristoski, P., Rosati, J., Di Noia, T., De Leone, R., & Paulheim, H. (2018). 
// 		RDF2Vec: RDF Graph Embeddings and Their Applications. 
// 		Semantic Web Journal, Pre-press, 1–32. https://doi.org/10.1007/978-3-319-46523-4
// 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////


package com.irisa.swpatterns.measures.experiments.rdf2vec;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.query.Dataset; 
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

public class WalkGenerator {
	public static final Logger log = LoggerFactory
			.getLogger(WalkGenerator.class);

	public static String directPropsQuery = "SELECT ?p ?o WHERE {$ENTITY$ ?p ?o}";

	/**
	 * defines the depth of the walk (only nodes are considered as a step)
	 */
	// CBL: modified due to the size of the knowledge graph we are dealing with
	// we have had to adapt it as the depth is twice as much in fact when 
	// compared to the original Graph kernels paper they base on (the 
	// graphs are decoupled and the properties become a node in their graph)
	// here: +1 depth => ?p(i) ?o(i+1)  
	public static int depthWalk = 3;
	/**
	 * defines the number of walks from each node
	 */
	public static int numberWalks = 200;

	/**
	 * the query for extracting paths
	 */
	public static String walkQuery = "";

	public static int processedEntities = 0;
	public static int processedWalks = 0;
	public static int fileProcessedLines = 0;

	public static long startTime = 0;

	/**
	 * the rdf model
	 */
	public static Model model;

	public static Dataset dataset;

	public static String fileName = "walks.txt";

	/**
	 * file writer for all the paths
	 */
	public static Writer writer;

	public void generateWalks(String repoLocation, String udpateFileID,
			int nmWalks, int dpWalks, int nmThreads, int offset, int limit) {
		// set the parameters
		numberWalks = nmWalks;
		depthWalk = dpWalks;
		fileName = udpateFileID+"-walks.txt";

		// int the writer
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(fileName, false), "utf-8"),
					32 * 1024);
		} catch (UnsupportedEncodingException | FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// generate the query
		walkQuery = generateQuery(depthWalk, numberWalks);

		// open the dataset
		dataset = TDBFactory.createDataset(repoLocation);

		model = dataset.getDefaultModel();
		
		// CBL: here it comes the magic :) 
		
		
		
		
		System.out.println("SELECTING all entities from repo");
		List<String> entities = selectAllEntities(offset, limit);

		System.out.println("Total number of entities to process: "
				+ entities.size());
		ThreadPoolExecutor pool = new ThreadPoolExecutor(nmThreads, nmThreads,
				0, TimeUnit.SECONDS,
				new java.util.concurrent.ArrayBlockingQueue<Runnable>(
						entities.size()));

		startTime = System.currentTimeMillis();
		for (String entity : entities) {

			EntityThread th = new EntityThread(entity);

			pool.execute(th);

		}

		pool.shutdown();
		try {
			pool.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Instead of selecting all the entities from the repo, 
	// we just select the entities we are focused on 
	// we must feed the beast with a List<String> 
	public static List<String> affectedResources () {
		return void; 
	}
	
	
	/**
	 * Adds new walks to the list; If the list is filled it is written to the
	 * file
	 * 
	 * @param tmpList
	 */
	public synchronized static void writeToFile(List<String> tmpList) {
		processedEntities++;
		processedWalks += tmpList.size();
		fileProcessedLines += tmpList.size();
		for (String str : tmpList)
			try {
				writer.write(str + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		if (processedEntities % 100 == 0) {
			System.out
					.println("TOTAL PROCESSED ENTITIES: " + processedEntities);
			System.out.println("TOTAL NUMBER OF PATHS : " + processedWalks);
			System.out.println("TOTAL TIME:"
					+ ((System.currentTimeMillis() - startTime) / 1000));
		}
		// flush the file
		if (fileProcessedLines > 3000000) {
			fileProcessedLines = 0;
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int tmpNM = (processedWalks / 3000000);
			String tmpFilename = fileName.replace(".txt", tmpNM + ".txt");
			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(tmpFilename, false), "utf-8"),
						32 * 1024);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * generates the query with the given depth
	 * 
	 * @param depth
	 * @return
	 */
	public static String generateQuery(int depth, int numberWalks) {
		String selectPart = "SELECT ?p ?o1";
		String mainPart = "{ $ENTITY$ ?p ?o1  ";
		String query = "";
		for (int i = 1; i < depth; i++) {
			mainPart += ". ?o" + i + " ?p" + i + " ?o" + (i + 1);
			selectPart += " ?p" + i + " ?o" + (i + 1);
		}
		query = selectPart + " WHERE " + mainPart + "} LIMIT 1000";
		// + " BIND(RAND() AS ?sortKey) } ORDER BY ?sortKey LIMIT "
		// + numberWalks;
		return query;
	}

	static class EntityThread implements Runnable {

		private String entity;

		private List<String> finalList;

		public EntityThread(String entity) {
			this.entity = entity;
			finalList = new ArrayList<String>();
		}

		@Override
		public void run() {
			processEntity();
			writeToFile(finalList);

		}

		private void processEntity() {

			// get all the walks
			List<String> tmpList = new ArrayList<String>();
			String queryStr = walkQuery.replace("$ENTITY$", "<" + entity + ">");

			executeQuery(queryStr, tmpList);

			Random rand = new Random();
			for (int i = 0; i < numberWalks; i++) {
				if (tmpList.size() < 1)
					break;
				int randomNum = rand.nextInt(tmpList.size());
				if (randomNum > tmpList.size() - 1)
					randomNum = tmpList.size() - 1;
				finalList.add(tmpList.get(randomNum));

				tmpList.remove(randomNum);
			}

			// get all the direct properties
			queryStr = directPropsQuery.replace("$ENTITY$", "<" + entity + ">");
			executeQuery(queryStr, finalList);

		}

		public void executeQuery(String queryStr, List<String> walkList) {
			Query query = QueryFactory.create(queryStr);
			dataset.begin(ReadWrite.READ);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			ResultSet resultsTmp = qe.execSelect();
			String entityShort = entity.replace("http://dbpedia.org/resource/",
					"dbr:");
			ResultSet results = ResultSetFactory.copyResults(resultsTmp);
			qe.close();
			dataset.end();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				String singleWalk = entityShort + "->";
				// construct the walk from each node or property on the path
				for (String var : results.getResultVars()) {
					try {
						// clean it if it is a literal
						if (result.get(var) != null
								&& result.get(var).isLiteral()) {
							String val = result.getLiteral(var).toString();
							val = val.replace("\n", " ").replace("\t", " ")
									.replace("->", "");
							singleWalk += val + "->";
						} else if (result.get(var) != null) {
							singleWalk += result
									.get(var)
									.toString()
									.replace("http://dbpedia.org/resource/",
											"dbr:")
									.replace("http://dbpedia.org/ontology/",
											"dbo:")
									.replace(
											"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
											"rdf:")
									.replace(
											"http://www.w3.org/2000/01/rdf-schema#",
											"rdfs:").replace("->", "")
									+ "->";
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
// 				walkList.add(singleWalk);
				walkList.add(singleWalk.substring(0, singleWalk.length() - 2));

			}

		}
	}

	public static void main(String[] args) {
		
		// * load the update pair
		// * apply the update 
		// * generate the paths for the final state
		// 		NOTE: we don't need the original ones as we have already calculated the 
		// 		embeddings in such state. 
		
		System.out
				.println("USAGE:  repoLocation updateToEvaluate nmWalks dpWalks nmThreads ");
		WalkGenerator generator = new WalkGenerator();
		generator.generateWalks(args[0], args[1], Integer.parseInt(args[2]),
				Integer.parseInt(args[3]), Integer.parseInt(args[4]),
				Integer.parseInt(args[5]), Integer.parseInt(args[6]));
	}
}
