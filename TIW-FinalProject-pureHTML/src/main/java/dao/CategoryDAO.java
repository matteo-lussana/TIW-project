package dao;

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import beans.CategoryClass;

/**
 * This class stores all the function to manages the categories
 *
 */
public class CategoryDAO {
	private Connection connection;
	
	public CategoryDAO(Connection connection) {
		this.connection = connection;
	}
	
	/**
	 * This method retrieves the categories from the database and saves them in a list of CategoryClass
	 * @return a list of all the categories
	 * @throws SQLException
	 */
	public List<CategoryClass> findAllCategories() throws SQLException{
		List<CategoryClass> categories = new ArrayList<CategoryClass>();
		String query = "SELECT * FROM category ORDER BY code ASC"; //ordering the categories by the code
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);){
			try(ResultSet result = pstatement.executeQuery();){
				while(result.next()) {
					CategoryClass cat = new CategoryClass();
					cat.setCode(result.getLong("code"));
					cat.setName(result.getString("name"));
					cat.setFather(result.getLong("father"));
					
					//if the category isn't an ancestor and it's the last child, set the father complete
					if((cat.getFather()!=0) && cat.getCode() % 10 == 9) categories.get(binaryResearch(cat.getFather(), categories)).setIsComplete(true);
					
					//if the category is the 15th descendant, set this category complete
					if(String.valueOf(cat.getCode()).length() == 15) cat.setIsComplete(true);
					
					categories.add(cat);
				}
			}
		}
		return categories;
	}
	
	/** 
	 * This method finds the ancestors (father == 0)
	 * @param categories : list of the categories
	 * @return list of the ancestors
	 */
	public List<CategoryClass> findAncestors(List<CategoryClass> categories){
		List<CategoryClass> ancestors = new ArrayList<CategoryClass>();
		
		for(CategoryClass category : categories) {
			if(category.getFather() == 0) ancestors.add(category);
		}
		return ancestors;	
	}
	
	/**
	 * Binary research of the category by its code
	 * @param codeToFind : the code of the category to find
	 * @param categories : list of the categories
	 * @return the position of the category in the list or -1 if the category is not present
	 */
	public int binaryResearch(long codeToFind, List<CategoryClass> categories) {
		int end = categories.size()-1;
		int start = 0;
		int mid;
		while(start<=end) {
			mid=(start + end)/2;
			if(categories.get(mid).getCode() == codeToFind) return mid;
			else if(categories.get(mid).getCode() < codeToFind) start = mid + 1;
			else end = mid - 1; 
		}
		return -1;
	}
	
	//- - - - - - - - - - - - - - - - -| CREATION OF NEW CATEGORIES |- - - - - - - - - - - - - - - - -
	
	/**
	 * This method checks if the category has any sub-categories
	 * @param fatherCode: code of the category to check
	 * @param categories: list of the categories
	 * @return true if the father category has children, false otherwise
	 */
	public boolean hasChildren(long fatherCode, List<CategoryClass> categories) {
		for(CategoryClass category : categories) {
			if(category.getCode() == fatherCode * 10 + 1)
				return true;
		}
		return false;
	}
	
	/**
	 * This method finds the last child of a father category
	 * @param fatherCode: code of the father
	 * @return the code of the last child
	 * @throws SQLException
	 */
	public long findLastChild(long fatherCode) throws SQLException {
		
		String query = "SELECT MAX(code) as maxCode FROM category WHERE father = ?";
		long queryResult = 0;
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);){
			pstatement.setLong(1, fatherCode);
			
			try(ResultSet result = pstatement.executeQuery();){
				if(!result.isBeforeFirst()) {
					return fatherCode*10;					
				}
				else {
					result.next();
					queryResult = result.getLong("maxCode");
					return queryResult;
				}
			}
			
		}
	}
	
	/**
	 * This method creates a new category and inserts it in the database
	 * @param newCategoryCode is the code of the new category
	 * @param name of the new category
	 * @param fatherCode is the code of the father of the new category
	 * @throws SQLException
	 */
	public void createCategory(long newCategoryCode, String name, long fatherCode) throws SQLException{
		
		String query = "INSERT into category (code, name, father) VALUES (?, ?, ?)";
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);){
			pstatement.setLong(1, newCategoryCode);
			pstatement.setString(2, name);
			pstatement.setLong(3, fatherCode);
			pstatement.executeUpdate();
		}
		
	}
	
	//- - - - - - - - - - - - - - - - -| COPY OF SUB-TREES |- - - - - - - - - - - - - - - - - - -
	
	/**
	 * This method finds the sub-tree of the ancestor category 
	 * @param ancestorCode: code of the category which sub-tree is to find
	 * @param categories: list of all the categories
	 * @return list of the categories in the sub-tree
	 */
	public List<CategoryClass> findFamily(long ancestorCode, List<CategoryClass> categories){
		List<CategoryClass> family = new ArrayList<CategoryClass>();
		
		int ancestorPos = binaryResearch(ancestorCode, categories);
		int digitCountAncestor = String.valueOf(ancestorCode).length();
		
		family.add(categories.get(ancestorPos));
		
		for(int i=ancestorPos+1; i<categories.size(); i++) {
			int digitCountSon = String.valueOf(categories.get(i).getCode()).length();
			long ancestorOfI = (long)(categories.get(i).getCode()/Math.pow(10, digitCountSon-digitCountAncestor));
			
			if(ancestorCode == ancestorOfI)
				family.add(categories.get(i));
		}
		
		return family;
	}
	
	/**
	 * This method creates the new codes for the categories in the sub-tree created by the "copy" link
	 * and calls the createCategory() method to insert the new categories in the database
	 * @param family: list of the categories to be copied
	 * @param ancestorCode: new code for the first category of the subtree
	 * @throws SQLException
	 */
	public void moveFamily(List<CategoryClass> family, long newAncestorCode) throws SQLException {
		int familyFatherDigitsNum = String.valueOf(family.get(0).getCode()).length();
		try{
			connection.setAutocommit(false);
			createCategory(newAncestorCode, family.get(0).getName(), newAncestorCode/10);
			
			for(int i=1; i<family.size(); i++) {
				int digitsNum = String.valueOf(family.get(i).getCode()).length();	
				long staticDigits = (long) (family.get(i).getCode()%(Math.pow(10, digitsNum-familyFatherDigitsNum)));
				
				long newCode = staticDigits + (long)(Math.pow(10, digitsNum-familyFatherDigitsNum))* newAncestorCode;
				
				createCategory(newCode, family.get(i).getName(), newCode/10);
			}
			connection.commit();
		}catch(SQLException e){
			if(connection != null)
				connection.rollback();
		}finally{
			assert connection != null;
			connection.setAutocommit(true);
		}
		
		
	}
	
	//- - - - - - - - - - - - - -| MAXIMUM CODE LENGTH MANAGEMENT |- - - - - - - - - - - - -  - - - - - - - -
	
	/**
	 * This method computes how many depth of descendant the category has
	 * @param ancestorCode: code of the category
	 * @param categories: list of all categories
	 * @return number of the depth of descendants
	 */
	public int computeDescendantsNum(long ancestorCode, List<CategoryClass> categories) {
		List<CategoryClass> family = findFamily(ancestorCode, categories);
		long longestCode = ancestorCode;
		
		for(CategoryClass cat : family) {
			if(longestCode < cat.getCode())
				longestCode = cat.getCode();
		}
		
		//difference between the number of digits of longestCode and the number of digits of ancestorCode
		int descendantsNum = String.valueOf(longestCode).length();
		return descendantsNum;
	}
	
	/**
	 * This method finds the categories that cannot have the "copy" link link because their depth of descendants
	 * is 15
	 * @param categories: list of all categories
	 * @return a list of the categories that cannot have the "copy" link
	 */
	public List<CategoryClass> findNoCopyCategories(List<CategoryClass> categories){
		List<CategoryClass> noCopyCategories = new ArrayList<>();
		
		for(CategoryClass cat : categories) {
			int descendantsNum = computeDescendantsNum(cat.getCode(), categories)- String.valueOf(cat.getCode()).length() + 1;
			if(descendantsNum == 15)
				noCopyCategories.add(cat);
		}
		
		return noCopyCategories;
	}
	
	/**
	 * This method finds the categories that cannot have the "copy here" link because their depth of descendants
	 * plus the depth of the sub-tree to copy is greater than 15
	 * @param familyToCopy : list of the categories to be copied
	 * @param categories : list of all categories
	 * @return list of categories that can't have the "copy here" link
	 */
	public List<CategoryClass> findNoCopyHereCategories(List<CategoryClass> familyToCopy, List<CategoryClass> categories){
		List<CategoryClass> noCopyHereCategories = new ArrayList<>();
		int familyDepth = computeDescendantsNum(familyToCopy.get(0).getCode(), familyToCopy) - String.valueOf(familyToCopy.get(0).getCode()).length() + 1;
		
		for(CategoryClass cat : categories) {
			int catDigitsNum = String.valueOf(cat.getCode()).length();
			if(catDigitsNum + familyDepth > 15)
				noCopyHereCategories.add(cat);
		}
		
		return noCopyHereCategories;
	}
}
