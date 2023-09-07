package controllers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import com.google.gson.Gson;

import beans.CategoryClass;
import beans.MovingFamilyInfo;
import dao.CategoryDAO;

/**
 * Servlet implementation class InsertFamily
 * This servlet inserts the copied subtree in the database
 */
@WebServlet("/InsertFamily")
@MultipartConfig
public class InsertFamily extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
       
    public InsertFamily() {
        super();
    }

    /**
     * This method initiates the connection with the database reading the information from the web.xml
     */
    public void init() throws ServletException{
    	try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);

		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		String error;
		
		if(session == null || session.isNew() || session.getAttribute("username") == null) {
			error = "Please login first";
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println(error);
			return;
		}
		
		long familyFather = -1;
		long targetCode = -1;
		boolean badRequest = false;
		
		//get the parameters from the JSON object
		Gson gson = new Gson();
		MovingFamilyInfo movingFamilyInfo = gson.fromJson(new InputStreamReader(request.getInputStream()), MovingFamilyInfo.class);
		
		try{
			familyFather = movingFamilyInfo.getFamilyFather();
			targetCode = movingFamilyInfo.getTargetCode();
		}catch(NullPointerException | NumberFormatException e) { badRequest = true;}
		
		if(badRequest) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Missing or incorrect parameters");
			return;
		}
		
		//check if the target is a child of the subtree to copy
		if(String.valueOf(targetCode).startsWith(String.valueOf(familyFather))) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().println("The target code cannot be a child of the branch to copy");
			return;
		}
		
		
		CategoryDAO bService = new CategoryDAO(connection);
		List<CategoryClass> allcategories = null;
		try {
			allcategories = bService.findAllCategories();
		} catch (SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error in retrieving categories from database");
			return;
		}
		
		if(targetCode != 0 && bService.binaryResearch(targetCode, allcategories) == -1 || familyFather != 0 && bService.binaryResearch(familyFather, allcategories) == -1) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect parameters");
			return;
		}
		
		//retrieve the family
		List<CategoryClass> family = bService.findFamily(familyFather, allcategories);
		
		if(targetCode != 0) {
			//check he depth of descendants of the target's family plus the depth of the sub-tree to copy is valid
			if(bService.computeDescendantsNum(familyFather, family) - String.valueOf(familyFather).length() + String.valueOf(targetCode).length() > 14) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.getWriter().println("The branch to copy is too deep");
				return;
			}
		}
		
		
		
		//compute the new codes 
		long newAncestorCode = -1;
		
		/*if(targetCode == 0) {
			for(int  i=0; i<allcategories.size(); i++) {
				if(allcategories.get(i).getCode() > 9) {
					newAncestorCode = (long) (i+1);
					System.out.println("newAncestorCode "+newAncestorCode);
					break;
				}
			}
		}
		else*/ if(bService.hasChildren(targetCode, allcategories))
		{
			try {
				newAncestorCode = bService.findLastChild(targetCode) + 1;
			}catch(SQLException e) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.getWriter().println("Error calculating last child");
				return;
			}
		}
		else
			newAncestorCode = targetCode * 10 + 1;
		
		//check if the new code valid
		if(newAncestorCode%10 == 0) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().println("The category already got 9 sub-categories");
			return;
		}
		
		//insert the subtree in the database
		try {
			bService.moveFamily(family, newAncestorCode);
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Error coping categories in database");
			return;
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
	
	}

}
