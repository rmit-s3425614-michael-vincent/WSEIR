package util;

import java.util.Map;
import java.util.Hashtable;

// helper class to store frequency of documents containing lexicon
// also store offset for pointer to invlists
public class pointer {
	
	private int totalFreq;
	private int docsFreq;
	private int offset;
	private Map<Integer, frequency> invIndex;

	public pointer() {
		this.totalFreq = 1;
		this.docsFreq = 1;
		this.offset = 0;
		this.invIndex = new Hashtable<>(2500);
	}

	public int getTotalFreq() {
		return totalFreq;
	}
	
	public void incTotalFreq() {
		this.totalFreq++;
	}
	
	public void setTotalFreq(int totalFreq) {
		this.totalFreq = totalFreq;
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

	public Map<Integer, frequency> getInvIndex() {
		return invIndex;
	}

}
