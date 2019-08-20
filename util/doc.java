package util;

// helper class to store lexicon frequency in a document
public class doc {

	private int freq;

	public doc() {
		this.freq = 1;
	}

	public int getFreq() {
		return freq;
	}

	public void incFreq() {
		this.freq++;
	}

}
