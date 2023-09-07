package beans;
import java.io.Serializable;

/**
 * Class to manage the categories
 */

public class CategoryClass implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private long code;
	private boolean isAncestor = false;
	private String name;
	private long father; //code of the father
	private boolean isComplete = false; //true if the category has 9 sons or it's the 18th descendant
	
	public CategoryClass() {}
	
	//- - - - - - - - - - - - - - - - - - -| GETTER METHODS |- - - - - - - - - - - - - - - - - -//
	public long getCode() {
		return this.code;
	}
	public long getFather() {
		return this.father;
	}
	public boolean getIsAncestor() {
		return this.isAncestor;
	}
	public String getName() {
		return this.name;
	}
	public boolean getIsComplete() {
		return this.isComplete;
	}
	//- - - - - - - - - - - - - - - - - - -| SETTER METHODS |- - - - - - - - - - - - - - - - - -//
	
	public void setCode(long code) {
		this.code = code;
	}
	public void setFather(long father) {
		this.father = father;
	}
	public void setIsAncestor(boolean isAncestor) {
		this.isAncestor = isAncestor;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setIsComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}
	
	
	@Override
	public String toString() {
		StringBuffer aBuffer = new StringBuffer("CategoryClass");
		aBuffer.append(" Code: ");
		aBuffer.append(this.code);
		aBuffer.append(" Name: ");
		aBuffer.append(this.name);
		return aBuffer.toString();
	}
	
}
