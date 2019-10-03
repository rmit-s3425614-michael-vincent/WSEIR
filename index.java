import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import util.doc;
import util.frequency;
import util.pointer;
import util.stemmer;

public class index {

	private static final String USAGE = "Usage is: index [-s <stoplist>] [-p] <document>";

	// uses java library for data structure
	private static Set<String> printList = new HashSet<>();
	private static Map<Integer, doc> docMap = new Hashtable<>(180000);
	private static Map<String, pointer> lexicon = new Hashtable<>(230000);
	private static Set<String> stopwords = new HashSet<>();

	public static void main(String[] args) {

		try {

			OptionParser parser = new OptionParser("s:p");
			OptionSet options = parser.parse(args);

			String inputStoplist = null;
			String inputDoclist = null;

			// option for specifying stoplist to use
			if (options.has("s")) {
				if (options.hasArgument("s")) {
					inputStoplist = (String) options.valueOf("s");
				}
			}

			// non-option arguments
			List<?> tempArgs = options.nonOptionArguments();
			List<String> remainArgs = new ArrayList<String>();
			for (Object object : tempArgs) {
				remainArgs.add((String) object);
			}

			inputDoclist = remainArgs.get(0);

			// scan stoplist and make hashset of stop words if provided
			if (inputStoplist != null) {
				try {

					File stop = new File(inputStoplist);
					Scanner read = new Scanner(stop);
					read.useDelimiter("\\Z");
					String[] stoplist = read.next().split("[^\\p{L}]+");
					Collections.addAll(stopwords, stoplist);
					read.close();

				} catch (FileNotFoundException ex) {
					System.err.println("File " + inputStoplist + " not found.");
					System.err.println(USAGE);
				}
			}
						
			try {

				// build collections of elements from documents
				// elements are enclosed within DOC tags
				File input = new File(inputDoclist);
				Document latimes = Jsoup.parse(input, "UTF-8");
				Elements docs = latimes.getElementsByTag("DOC");

				// iterate through elements collections
				// get all elements enclosed within DOCNO, DOCID, HEADLINE, TEXT tags
				// make mapping of elements and insert to hashtables
				for (Element doc : docs) {
					
					Elements docno = doc.getElementsByTag("DOCNO");
					String no = docno.text();
					Elements docid = doc.getElementsByTag("DOCID");
					Integer id = Integer.parseInt(docid.text());
					index.docMap.put(id, new doc(no, 0));
					
					Elements headline = doc.getElementsByTag("HEADLINE");
					List<String> hllist = new ArrayList<>(Arrays.asList(
							headline.text().replaceAll("[^a-zA-Z\\s]", " ").toLowerCase().trim().split("\\s+")));
					
					Elements text = doc.getElementsByTag("TEXT");
					List<String> txtlist = new ArrayList<>(Arrays
							.asList(text.text().replaceAll("[^a-zA-Z\\s]", " ").toLowerCase().trim().split("\\s+")));
							
					docMap.get(id).setDocLength(hllist.size() + txtlist.size());
					
					hllist.removeAll(stopwords);
					txtlist.removeAll(stopwords);
					printList.addAll(hllist);
					printList.addAll(txtlist);
					
					for (String hl : hllist) {
						stemmer stem = new stemmer();
						char[] ch = hl.toCharArray();
						stem.add(ch, ch.length);
						stem.stem();
						hl = stem.toString();
						if (lexicon.containsKey(hl)) {
							lexicon.get(hl).incTotalFreq();
							if (lexicon.get(hl).getInvIndex().containsKey(id)) {
								lexicon.get(hl).getInvIndex().get(id).incFreq();
							} else {
								lexicon.get(hl).getInvIndex().put(id, new frequency());
								lexicon.get(hl).incDocsFreq();
							}
						} else {
							pointer lx = new pointer();
							lx.getInvIndex().put(id, new frequency());
							lexicon.put(hl, lx);
						}
					}
					
					for (String txt : txtlist) {
						stemmer stem = new stemmer();
						char[] ch = txt.toCharArray();
						stem.add(ch, ch.length);
						stem.stem();
						txt = stem.toString();
						if (lexicon.containsKey(txt)) {
							lexicon.get(txt).incTotalFreq();
							if (lexicon.get(txt).getInvIndex().containsKey(id)) {
								lexicon.get(txt).getInvIndex().get(id).incFreq();
							} else {
								lexicon.get(txt).getInvIndex().put(id, new frequency());
								lexicon.get(txt).incDocsFreq();
							}
						} else {
							pointer lx = new pointer();
							lx.getInvIndex().put(id, new frequency());
							lexicon.put(txt, lx);
						}
					}
					
				}

			} catch (IOException ex) {
				System.err.println("Cannot open file " + inputDoclist);
				System.err.println(USAGE);
			}
			
			// create text file "map" to store mapping of document no and id
			File mapFile = new File("./map");
			mapFile.createNewFile();
			PrintWriter mapOut = new PrintWriter(mapFile);
			
			// write document no and id to map file
			StringJoiner map = new StringJoiner("\n");
			for (Integer id : docMap.keySet()) {
				String out = docMap.get(id).getDocNo() + "::" + docMap.get(id).getDocLength() + "::" + id;
				map.add(out);
			}
			mapOut.write(map.toString());
			mapOut.close();
			
			// create text file "lexicon" to store lexicon and pointer
			File lexFile = new File("./lexicon");
			lexFile.createNewFile();
			PrintWriter lexOut = new PrintWriter(lexFile);
			
			// create binary file "invlists" to store the inverted list
			DataOutputStream invlists = new DataOutputStream(new FileOutputStream("./invlists"));
			
			// write lexicon and invlists to files
			StringJoiner lx = new StringJoiner("\n");
			int offset = 0;
			for (String lex : lexicon.keySet()) {
				lexicon.get(lex).setOffset(offset);
				String out = lex + "::" + lexicon.get(lex).getTotalFreq() + "::" + lexicon.get(lex).getDocsFreq() + "::"
						+ lexicon.get(lex).getOffset();
				for (Integer doc : lexicon.get(lex).getInvIndex().keySet()) {
					int freq = lexicon.get(lex).getInvIndex().get(doc).getFreq();
					invlists.writeInt(doc);
					invlists.writeInt(freq);
				}
				offset += lexicon.get(lex).getDocsFreq() * 8;
				lx.add(out);
			}
			
			lexOut.write(lx.toString());
			lexOut.close();
			
			invlists.flush();
			invlists.close();

			// print the lexicon to standard output if -p is given
			if (options.has("p")) {
				for (String out : printList) {
					System.out.println(out);
				}
			}

		} catch (Exception ex) {
			System.err.println(ex);
			System.err.println(USAGE);
		}

	}

}
