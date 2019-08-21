package util;

import java.util.Map;
import java.util.Hashtable;

// helper class to store frequency of documents containing lexicon
// also store offset for pointer to invlists
public class pointer {
	
	private int docsFreq;
	private int offset;
	private Map<Integer, doc> invIndex;

	public pointer() {
		this.docsFreq = 1;
		this.offset = 0;
		this.invIndex = new Hashtable<>(2000);
	}

	public int getDocsFreq() {
		return docsFreq;
	}

	public void incDocsFreq() {
		this.docsFreq++;
	}
	
	public void setDocsFreq(int docsFreq) {
		this.docsFreq = docsFreq;
	}
		
	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public Map<Integer, doc> getInvIndex() {
		return invIndex;
	}

}
