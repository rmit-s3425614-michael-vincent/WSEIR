import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import util.pointer;
import util.stemmer;

public class search {

	// uses java library for data structure
	private static Map<Integer, String> docMap = new Hashtable<>(175870);
	private static Map<String, pointer> lexicon = new Hashtable<>(353910);
	private static List<String> querylist = new ArrayList<>();

	public static void main(String[] args) {

		try {
			
			// first 3 arguments are used for file input
			// read "lexicon" file and put it in hashtable
			File lexFile = new File(args[0]);
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
			File mapFile = new File(args[2]);
			Scanner mapRead = new Scanner(mapFile);
			while (mapRead.hasNextLine()) {
				String[] in = mapRead.nextLine().split("::");
				docMap.put(Integer.parseInt(in[1]), in[0]);
			}
			mapRead.close();
			
			// make list from remaining arguments
			for (int i = 3; i < args.length; i++) {
				querylist.add(args[i]);
			}
			
			// minimum of 1 query term is needed
			if (!querylist.isEmpty()) {
				
				// parse through list for query
				// use regex to remove punctuation and markup tags
				// use stemmer to tokenise query words
				for (String query : querylist) {
					
					String token = query.replaceAll("[^a-zA-Z\\s+]", "").toLowerCase().trim();
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
						RandomAccessFile raf = new RandomAccessFile(args[1], "r");
						raf.seek(offset);
						int[] docList = new int[docsFreq * 2];
						for (int i = 0; i < docList.length; i++) {
							int index = raf.readInt();
							docList[i] = index;
						}
						raf.close();
						
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
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err.println("Usage is: search <lexicon> <invlists> <map> <queryterm_1>[... <queryterm_N>]");
		}

	}

}
