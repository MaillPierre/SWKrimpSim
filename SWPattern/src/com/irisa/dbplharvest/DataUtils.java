package com.irisa.dbplharvest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.log4j.Logger;

import com.irisa.jenautils.QueryResultIterator;


/**
 * Taken from https://github.com/dbpedia/dbpedia-live-mirror/blob/master/src/main/java/org/dbpedia/extraction/live/mirror/helper/Utils.java
 * Modified by Pierre Maillot
 *
 * @author Dimitris Kontokostas
 * @since 9/24/14 1:16 PM
 * 
 * 
 */
public final class DataUtils {

    private static final Logger logger = Logger.getLogger(DataUtils.class);

    private DataUtils() {
    }
    
    public static String dateBasedName(String year, String month, String day, String hour, String num) {
    	return year +"-" + month + "-" + day + "-" +hour + "-" + num;
    }

    public static List<String> getTriplesFromFile(String filename) {
        List<String> lines = new ArrayList<>();

        try (
                FileInputStream fileInputStream= new FileInputStream(filename);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
                BufferedReader in = new BufferedReader(inputStreamReader)
        ) {

            String line;
            while ((line = in.readLine()) != null) {
                String triple = line.trim();

                // Ends with is a hack for not correctly decompressed changesets
                if (!triple.isEmpty() && !triple.startsWith("#") && triple.endsWith(" .")) {
                    lines.add(triple);
                }

            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File " + filename + " not fount!", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UnsupportedEncodingException: ", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException in file " + filename, e);
        }

        return lines;

    }

    public static String getFileAsString(String filename) {
        StringBuilder str = new StringBuilder();

        try (
                FileInputStream fileInputStream= new FileInputStream(filename);
                InputStreamReader in = new InputStreamReader(fileInputStream, "UTF-8")
        ) {
            int ch;
            while ((ch = in.read()) != -1) {
                str.append((char) ch);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File " + filename + " not fount!", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UnsupportedEncodingException: ", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException in file " + filename, e);
        }

        return str.toString();
    }

    public static boolean deleteFile(String filename) {
        try {
            File file = new File(filename);
            boolean retVal = file.delete();
            return retVal;
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateStringFromList(Collection<String> strList, String sep) {

        StringBuilder finalString = new StringBuilder();


        for (String str : strList) {
            finalString.append(str);
            finalString.append(sep);
        }

        return finalString.toString();
    }

    public static boolean writeLinesToFile(List<String> triples, String filename) {

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(filename);
                OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, "UTF8")
        ) {

            for (String triple : triples) {
                out.write(triple + "\n");
            }

            return true;

        } catch (IOException e) {
            logger.error("Error writing file: " + filename, e);
        }

        return false;
    }
    
    public static boolean writeResourcesToFile(Collection<Resource> ress, String filename) {
    	LinkedList<String> strList = new LinkedList<String>();
    	for(Resource res : ress) {
    		strList.add(res.getURI());
    	}
    	return writeLinesToFile(strList, filename);
    }

    /**
     * Decompresses the passed GZip file, and returns the filename of the decompressed file
     *
     * @param filename The filename of compressed file
     * @return The filename of the output file, or empty string if a problem occurs
     */
    public static String decompressGZipFile(String filename) {

        String outFilename;
        //The output filename is the same as input filename without last .gz
        int lastDotPosition = filename.lastIndexOf('.');
        outFilename = filename.substring(0, lastDotPosition);

        try (
                FileInputStream fis = new FileInputStream(filename);
                //GzipCompressorInputStream(
                GZIPInputStream gis = new GZIPInputStream(fis);
                InputStreamReader isr = new InputStreamReader(gis, "UTF8");
                //BufferedReader in = new BufferedReader(isr);
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFilename), "UTF8")
        ) {
            int character;
            while ((character = isr.read()) != -1) {
                out.write(character);

            }

            logger.debug("File : " + filename + " decompressed successfully to " + outFilename);
        } catch (EOFException e) {
            // probably Wrong compression, out stream will close and existing contents will remain
            // but might leave incomplete triples
            logger.error("EOFException in compressed file: " + filename + " - Trying to recover");
        } catch (IOException ioe) {
            logger.warn("File " + filename + " cannot be decompressed due to " + ioe.getMessage(), ioe);
            outFilename = "";
        } finally {
            DataUtils.deleteFile(filename);
        }
        return outFilename;
    }

    /**
     * Downloads the file with passed URL to the passed folder
     * http://stackoverflow.com/a/921400/318221
     *
     * @param fileURL    URL of the file that should be downloaded
     * @param folderPath The path to which this file should be saved
     * @return The local full path of the downloaded file, empty string is returned if a problem occurs
     */
    public static String downloadFile(String fileURL, String folderPath) {

        //Extract filename only without full path
        int lastSlashPos = fileURL.lastIndexOf('/');
        if (lastSlashPos < 0) {
            return null;
        }

        String fullFileName = folderPath + fileURL.substring(lastSlashPos + 1);

        //Create parent folder if it does not already exist
        File file = new File(fullFileName);
        file.getParentFile().mkdirs();

        URL url;

        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            return null;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() != 200) {
                conn.getErrorStream().read();
                conn.disconnect();
                return null;
            }
            InputStream in = conn.getInputStream();
            Closeable res = in;
            try {
                ReadableByteChannel rbc = Channels.newChannel(in);
                FileOutputStream fos = new FileOutputStream(file);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } finally {
                res.close();
                conn.disconnect();
            }
        } catch (IOException e) {
            return null;
        }

        return fullFileName;
    }
    
    /**
     * Check if the given file url is accessible
     * @param fileURL
     * @return
     */
    public static boolean checkRemoteFile(String fileURL) {

        //Extract filename only without full path
        int lastSlashPos = fileURL.lastIndexOf('/');
        if (lastSlashPos < 0) {
            return false;
        }


        //Create parent folder if it does not already exist

        URL url;

        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            return false;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() != 200) {
                conn.getErrorStream().read();
                conn.disconnect();
                return false;
            }
            conn.disconnect();
            
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    }    
    
    /**
     * Filter all lines from source containing elements from filter file to destination file. Call external process.
     * @param sourceFilename
     * @param destinationFilename
     * @param filterFilename
     */
    public static void printFilter(String sourceFilename, String destinationFilename, String filterFilename) {
    	try{
    		ProcessBuilder builder = new ProcessBuilder("grep", "-f", filterFilename, sourceFilename);
            File combinedFile = new File(destinationFilename);
            builder.redirectOutput(combinedFile);
            builder.redirectError(combinedFile);
            Process p = builder.start();
            while(p.isAlive()) {}
        } catch(IOException e){
        	logger.error(e);
        } finally {
		}
    }

	private static Thread bigdataParserThread(String filename, PipedTriplesStream dataStream) {
		Thread parser=new Thread(){
			@Override public void run(){
				try {
					RDFParser.source(filename)
					.checking(false) // SHOULD NOT stop at malformed IRIs, but does anyway when they contain spaces...
					.errorHandler(ErrorHandlerFactory.errorHandlerNoWarnings)
					.errorHandler(new ErrorHandler() {
						private Logger rdfParserLogger = Logger.getLogger(RDFParser.class);
						
						@Override
						public void warning(String arg0, long arg1, long arg2) {
							rdfParserLogger.warn("line: " + arg1 + " col: " + arg2 + ", " + arg0);
						}
						@Override
						public void fatal(String arg0, long arg1, long arg2) {
							warning(arg0, arg1, arg2);
						}
						@Override
						public void error(String arg0, long arg1, long arg2) {
							warning(arg0, arg1, arg2);
						}
					})
					.parse(dataStream);
				} catch(Exception e) {
					logger.error("Unhandled exception during parsing, Dataset probably badly formated: ", e);
					throw e;
				}
			}};

			return parser;
	}
	
	/**
	 * Read a file and put its triples in the model
	 * @param model modified model
	 * @param filename read file containing triples
	 */
	public static void putTriplesInModel(Model model, String filename) {
		PipedRDFIterator<Triple> dataIt = new PipedRDFIterator<Triple>();
		PipedTriplesStream dataSteam = new PipedTriplesStream(dataIt);
		ExecutorService executor=Executors.newSingleThreadExecutor();
		
		try {

			Thread parser = bigdataParserThread(filename, dataSteam);
			executor.submit(parser);
			while(dataIt.hasNext()) {
				try {
					Triple stat = dataIt.next();
					Property prop = null;
					Resource subj = null;
					RDFNode obj = null;
					if(stat.getSubject() != null && stat.getSubject().getURI() != null) {
						subj = model.createResource(stat.getSubject().getURI());
					}
					if(stat.getPredicate() != null && stat.getPredicate().getURI() != null) {
						prop = model.createProperty(stat.getPredicate().getURI());
					}
					if(stat.getObject() != null) {
						if(stat.getObject().isLiteral()) {
							obj = model.createTypedLiteral(stat.getObject().getLiteralValue(), stat.getObject().getLiteralDatatype());
						} else if(stat.getObject().isURI()) {
							obj = model.createResource(stat.getObject().getURI());
						} else if(stat.getObject().isBlank()) {
							obj = model.createResource(new AnonId(stat.getObject().getBlankNodeId())); 
						}
					}
					
					if(subj != null && prop != null && obj != null) {
						model.add(subj, prop, obj);
					}
				} catch(Exception e) {
					logger.trace("Exception during this line treatment: ", e);
				}
			}
		} finally {
			executor.shutdown();
			dataIt.close();
		}
	}
	
	/**
	 * Extract the description of a set of resources from source to result
	 * @param result
	 * @param source
	 * @param resList
	 */
	public static void extractDescriptionTriples(Model result, Model source, Set<Resource> resList) {
		Iterator<Resource> itRes= resList.iterator();
		while(itRes.hasNext()) {
			Resource res = itRes.next();
			
			StmtIterator subjectIt = source.listStatements(res, null, (RDFNode)null);
			result.add(subjectIt);
			subjectIt.close();
			StmtIterator objectIt = source.listStatements(null, null, res.as(RDFNode.class));
			result.add(objectIt);
			objectIt.close();
		}
	}
}
