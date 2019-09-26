import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import util.pointer;
import util.stemmer;

public class search {
	
	private static final String USAGE = "Usage is: search -BM25 -q <query-label> -n <num-results> -l <lexicon> -i <invlists> -m <map> [-s <stoplist>] <queryterm-1> [<queryterm-2>... <queryterm-N>]";

	// uses java library for data structure
	private static Map<Integer, String> docMap = new Hashtable<>(180000);
	private static Map<String, pointer> lexicon = new Hashtable<>(230000);
	private static Set<String> stopwords = new HashSet<>();
	private static List<String> queryList = new ArrayList<>();

	public static void main(String[] args) {

		try {
			
			OptionParser parser = new OptionParser();
			parser.accepts("BM25");
			parser.accepts("q").withRequiredArg().required().isRequired();
			parser.accepts("n").withRequiredArg().ofType(Integer.class).required();
			parser.accepts("l").withRequiredArg().required();
			parser.accepts("i").withRequiredArg().required();
			parser.accepts("m").withRequiredArg().required();
			parser.accepts("s").withRequiredArg();
			
			OptionSet options = parser.parse(args);
			
			String inputQueryLabel = null;
			Integer inputNumResults = null;
			String inputLexicon = null;
			String inputInvlists = null;
			String inputMap = null;
			String inputStoplist = null;
			
			// option for specifying BM25 to be used
			if (options.has("BM25")) {
				
			} else {
				throw new Exception("Missing BM25");
			}
			
			// option for specifying query label
			if (options.hasArgument("q")) {
				inputQueryLabel = (String) options.valueOf("q");
			} else {
				System.err.println("Missing query label");
			}
			
			// option for specifying number of results
			if (options.hasArgument("n")) {
				inputNumResults = (Integer) options.valueOf("n");
			} else {
				System.err.println("Missing number of results");
			}
			
			// option for specifying lexicon file
			if (options.hasArgument("l")) {
				inputLexicon = (String) options.valueOf("l");
			} else {
				System.err.println("Lexicon not specified");
			}
			
			// option for specifying invlists file
			if (options.hasArgument("i")) {
				inputInvlists = (String) options.valueOf("i");
			} else {
				System.err.println("Invlists not specified");
			}
			
			// option for specifying map file
			if (options.hasArgument("m")) {
				inputMap = (String) options.valueOf("m");
			} else {
				System.err.println("Map not specified");
			}
			
			// option for specifying stoplist to use
			if (options.has("s")) {
				if (options.hasArgument("s")) {
					inputStoplist = (String) options.valueOf("s");
				} else {
					System.err.println("Stoplist not specified");
				}
			}
			
			// remaining non-option arguments as query list
			List<?> remainArgs = options.nonOptionArguments();
			for (Object object : remainArgs) {
				queryList.add((String) object);
			}
			
			// read "lexicon" file and put it in hashtable
			File lexFile = new File(inputLexicon);
			Scanner lexRead = new Scanner(lexFile);
			while (lexRead.hasNextLine()) {
				String[] in = lexRead.nextLine().split("::");
				pointer pointer = new pointer();
				pointer.setDocsFreq(Integer.parseInt(in[1]));
				pointer.setOffset(Integer.parseInt(in[2]));
				lexicon.put(in[0], pointer);
			}
			lexRead.close();
			
			// read "map" file and put it in hashtable
			File mapFile = new File(inputMap);
			Scanner mapRead = new Scanner(mapFile);
			while (mapRead.hasNextLine()) {
				String[] in = mapRead.nextLine().split("::");
				docMap.put(Integer.parseInt(in[1]), in[0]);
			}
			mapRead.close();
			
			RandomAccessFile raf = new RandomAccessFile(inputInvlists, "r");
						
			// minimum of 1 query term is needed
			if (!queryList.isEmpty()) {
				
				// parse through list for query
				// use regex to remove punctuation and markup tags
				// use stemmer to tokenise query words
				for (String query : queryList) {
					
					String token = query.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase().trim();
					stemmer stem = new stemmer();
					char[] ch = token.toCharArray();
					stem.add(ch, ch.length);
					stem.stem();
					token = stem.toString();
					
					System.out.println(query);
					if (lexicon.containsKey(token)) {
						
						int docsFreq = lexicon.get(token).getDocsFreq();
						int offset = lexicon.get(token).getOffset();
						
						// read and get data from "invlists"
						
						raf.seek(offset);
						int[] docList = new int[docsFreq * 2];
						for (int i = 0; i < docList.length; i++) {
							int index = raf.readInt();
							docList[i] = index;
						}
						
						System.out.println(docsFreq);
						for (int i = 0; i < docList.length; i++) {
							if ((i % 2) == 0) {
								String docno = docMap.get(docList[i]);
								int freq = docList[i + 1];
								System.out.println(docno + " " + freq);
							}
						}
						
					} else {
						System.out.println(0);
					}
					System.out.println();
					
				}
				
			} else {
				System.err.println("No query term entered.");
				System.err.println(USAGE);
			}
			
			raf.close();
			
		} catch (Exception ex) {
			System.err.println(ex);
			System.err.println(USAGE);
		}

	}

}
