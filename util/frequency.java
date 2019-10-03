package util;

// helper class to store lexicon frequency in a document
public class frequency {

	private int freq;

	public frequency() {
		this.freq = 1;
	}

	public int getFreq() {
		return freq;
	}

	public void incFreq() {
		this.freq++;
	}

}
