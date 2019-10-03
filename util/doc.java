package util;

public class doc {

	private String docNo;
	private int docLength;

	public doc(String docNo, int docLength) {
		this.docNo = docNo;
		this.docLength = docLength;
	}

	public String getDocNo() {
		return docNo;
	}

	public void setDocNo(String docNo) {
		this.docNo = docNo;
	}

	public int getDocLength() {
		return docLength;
	}
	
	public void setDocLength(int docLength) {
		this.docLength = docLength;
	}

}
