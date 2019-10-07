import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import util.doc;
import util.frequency;
import util.pointer;
import util.query;
import util.BoundedPriorityQueue;
import util.Stemmer;

public class search {

	private static final String USAGE = "Usage is: search --<retrieval-function-1> [--<retrieval-function-2>] -q <query-label> -n <num-results> -l <lexicon> -i <invlists> -m <map> [-s <stoplist>] <queryterm-1> [<queryterm-2>... <queryterm-N>]";

	// uses java library for data structure
	private static Map<Integer, doc> docMap = new Hashtable<>(180000);
	private static Map<String, pointer> lexicon = new Hashtable<>(230000);
	private static Set<String> stopwords = new HashSet<>();
	private static List<String> queryList = new ArrayList<>();
	private static boolean BM25 = false;
	private static boolean MMR = false;

	public static void main(String[] args) {

		try {

			long startTime = System.nanoTime();

			OptionParser parser = new OptionParser();
			parser.accepts("BM25");
			parser.accepts("MMR");
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

			// option for specifying retrieval function to be used
			if (options.has("BM25")) {
				BM25 = true;
			} else if (options.has("MMR")) {
				MMR = true;
			} else {
				throw new Exception("Missing retrieval function to use");
			}

			// option for specifying query label
			if (options.hasArgument("q")) {
				inputQueryLabel = (String) options.valueOf("q");
			}

			// option for specifying number of results
			if (options.hasArgument("n")) {
				inputNumResults = (Integer) options.valueOf("n");
				if (inputNumResults <= 0) {
					throw new Exception("Must have at least 1 results");
				}
			}

			// option for specifying lexicon file
			if (options.hasArgument("l")) {
				inputLexicon = (String) options.valueOf("l");
			}

			// option for specifying invlists file
			if (options.hasArgument("i")) {
				inputInvlists = (String) options.valueOf("i");
			}

			// option for specifying map file
			if (options.hasArgument("m")) {
				inputMap = (String) options.valueOf("m");
			}

			// option for specifying stoplist to use
			if (options.has("s")) {
				if (options.hasArgument("s")) {
					inputStoplist = (String) options.valueOf("s");
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
				lexicon.put(in[0], new pointer() {
					{
						setTotalFreq(Integer.parseInt(in[1]));
						setDocsFreq(Integer.parseInt(in[2]));
						setOffset(Integer.parseInt(in[3]));
					}
				});
			}
			lexRead.close();

			// read "map" file and put it in hashtable
			File mapFile = new File(inputMap);
			Scanner mapRead = new Scanner(mapFile);
			while (mapRead.hasNextLine()) {
				String[] in = mapRead.nextLine().split("::");
				docMap.put(Integer.parseInt(in[2]), new doc(in[0], Integer.parseInt(in[1])));
			}
			mapRead.close();

			// scan stoplist and make hashset of stop words if provided
			if (inputStoplist != null) {
				File stop = new File(inputStoplist);
				Scanner read = new Scanner(stop);
				read.useDelimiter("\\Z");
				String[] stoplist = read.next().split("[^\\p{L}]+");
				Collections.addAll(stopwords, stoplist);
				read.close();
			}

			// open invlists with random access method
			RandomAccessFile raf = new RandomAccessFile(inputInvlists, "r");

			// minimum of 1 query term is needed
			if (!queryList.isEmpty()) {
				
				StringJoiner sj = new StringJoiner(" ");
				for (String query : queryList) {
					sj.add(query);
				}

				// remove words in stoplist from query
				queryList.removeAll(stopwords);

				// a local lexicon mapping that contains only query results
				Map<String, pointer> queryLexicon = new Hashtable<>();
				List<List<Integer>> finalResults = new ArrayList<List<Integer>>();

				// parse through list for query
				// use regex to remove punctuation and markup tags
				// use stemmer to tokenise query words
				for (String query : queryList) {

					List<Integer> singleResults = new ArrayList<>();

					String token = query.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase().trim();
					Stemmer stem = new Stemmer();
					char[] ch = token.toCharArray();
					stem.add(ch, ch.length);
					stem.stem();
					token = stem.toString();

					if (lexicon.containsKey(token)) {

						int totalFreq = lexicon.get(token).getTotalFreq();
						int docsFreq = lexicon.get(token).getDocsFreq();
						int offset = lexicon.get(token).getOffset();

						queryLexicon.put(token, new pointer() {
							{
								setTotalFreq(totalFreq);
								setDocsFreq(docsFreq);
							}
						});

						// read "invlists" and jump pointer with seek
						raf.seek(offset);
						int[] docList = new int[docsFreq * 2];
						for (int i = 0; i < docList.length; i++) {
							int index = raf.readInt();
							docList[i] = index;
						}

						for (int i = 0; i < docList.length; i++) {
							if ((i % 2) == 0) {
								int id = docList[i];
								singleResults.add(id);
								int freq = docList[i + 1];
								queryLexicon.get(token).getInvIndex().put(id, new frequency() {
									{
										setFreq(freq);
									}
								});
							}
						}

					}

					finalResults.add(singleResults);

				}

				if (BM25 == true) {

					// map to accumulate weight result
					Map<Integer, Double> docK = new Hashtable<>();
					Map<Integer, Double> accumulator = new Hashtable<>();
					Set<Integer> queryResults = getCommonElements(finalResults);
					int N = docMap.size();
					int TL = 0;
					for (Integer id : docMap.keySet()) {
						int length = docMap.get(id).getDocLength();
						TL += length;
					}
					double AL = TL / N;
					for (Integer id : queryResults) {
						int Ld = docMap.get(id).getDocLength();
						double K = 1.2 * ((1 - 0.75) + ((0.75 * Ld) / AL));
						docK.put(id, K);
					}
					for (String lexicon : queryLexicon.keySet()) {
						pointer pointer = queryLexicon.get(lexicon);
						int ft = pointer.getDocsFreq();
						for (Integer id : queryResults) {
							int fdt = pointer.getInvIndex().get(id).getFreq();
							Double BM25 = Math.log((N - ft + 0.5) / (ft + 0.5))
									* (((1.2 + 1) * fdt) / (docK.get(id) + fdt));
							accumulator.merge(id, BM25, Double::sum);
						}
					}
					
					// custom comparator for comparing weight to order priority queue
					Comparator<query> compare = new Comparator<query>() {

						@Override
						public int compare(query o1, query o2) {
							if (o1.getWeight() < o2.getWeight())
								return -1;
							if (o1.getWeight() > o2.getWeight())
								return 1;
							return 0;
						}

					};
					
					// priority queue data structure used for min heap
					BoundedPriorityQueue<query> heap = new BoundedPriorityQueue<query>(compare, inputNumResults);
					for (Integer id : accumulator.keySet()) {
						query qr = new query(id, accumulator.get(id));
						heap.offer(qr);
					}
					
					System.out.println(sj.toString());
					Iterator<query> itr = heap.iterator();
					int count = 1;
					while (itr.hasNext()) {
						int id = itr.next().getId();
						System.out.println(inputQueryLabel + " " + docMap.get(id).getDocNo() + " " + count + " "
								+ accumulator.get(id));
						count++;
					}

				}
				
				if (MMR == true) {
					// TODO Implement MMR here
					Map<Integer, Map<String, Integer>> fIndex = new Hashtable<>();
					Set<Integer> queryResults = getCommonElements(finalResults);
					
					for(Integer id : queryResults) {
						fIndex.put(id, new Hashtable<>());
					}
					
					for(String token : lexicon.keySet()) {
						
						int docsFreq = lexicon.get(token).getDocsFreq();
						int offset = lexicon.get(token).getOffset();
						
						// read "invlists" and jump pointer with seek
						raf.seek(offset);
						int[] docList = new int[docsFreq * 2];
						for (int i = 0; i < docList.length; i++) {
							int index = raf.readInt();
							docList[i] = index;
						}
						
						for (int i = 0; i < docList.length; i++) {
							if ((i % 2) == 0) {
								int id = docList[i];
								int freq = docList[i + 1];
								if (fIndex.containsKey(id)) {
									fIndex.get(id).put(token, freq);
								}
							}
						}
						
					}
					
					// for debugging forward index
					System.out.println(sj.toString());
					for(Integer id : queryResults) {
						StringJoiner words = new StringJoiner(", ");
						for (String token : fIndex.get(id).keySet()) {
							String tokens = token + ":" + fIndex.get(id).get(token).toString();
							words.add(tokens);
						}
						System.out.println(docMap.get(id).getDocNo() + " -> " + words.toString());
					}
					
				}

			} else {
				System.err.println("No query term entered.");
				System.err.println(USAGE);
			}

			raf.close();

			long endTime = System.nanoTime();

			System.out.print("Time taken = " + ((double) (endTime - startTime)) / Math.pow(10, 6) + " ms");

		} catch (Exception ex) {
			System.err.println(ex);
			System.err.println(USAGE);
		}

	}

	// copied from https://dzone.com/articles/computing-common-and-unique
	public static <T> Set<T> getCommonElements(Collection<? extends Collection<T>> collections) {
		Set<T> common = new LinkedHashSet<T>();
		if (!collections.isEmpty()) {
			Iterator<? extends Collection<T>> iterator = collections.iterator();
			common.addAll(iterator.next());
			while (iterator.hasNext()) {
				common.retainAll(iterator.next());
			}
		}
		return common;
	}

}
