package beans;

public class MovingFamilyInfo {
	
	private long familyFather;
	private long targetCode;
	
	public MovingFamilyInfo() {}
	
	public void setFamilyFather(long familyFather) {
		this.familyFather = familyFather;
	}
	
	public void setTargetCode(long targetCode) {
		this.targetCode = targetCode;
	}
	
	public long getFamilyFather() {
		return this.familyFather;
	}
	
	public long getTargetCode() {
		return this.targetCode;
	}

}
