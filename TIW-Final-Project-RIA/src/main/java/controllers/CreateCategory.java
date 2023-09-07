package controllers;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import beans.CategoryClass;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import dao.*;

/**
 * Servlet implementation class CreateCategory
 * This servlet manages the creation of a new category using the form
 */
@WebServlet("/CreateCategory")
@MultipartConfig
public class CreateCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

    public CreateCategory() {
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
		
		HttpSession session = request.getSession();
		request.setCharacterEncoding("UTF-8"); //to read all the characters of the name
		String error;
		
		
		if(session == null || session.isNew() || session.getAttribute("username") == null) {
			error = "Please login first";
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println(error);
			return;
		}
		
		else {
			String name = null;
			long fatherCode = -1;
			boolean badRequest = false;
			long newChildCode;
			
			//get name and father's code
			try {
				name = request.getParameter("name");
				if(request.getParameterValues("isFather")!= null) fatherCode = 0;
				else fatherCode = Long.parseLong(request.getParameter("fatherCodeList"));
				if(name.isEmpty() || fatherCode < 0) {
					badRequest = true;
				}
			}catch(NullPointerException |  NumberFormatException e) {
				badRequest = true;
			}
			
			if(badRequest) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("missing or incorrect parameters");
				return;
			}
			
			
			CategoryDAO bService = new CategoryDAO(connection);
			List<CategoryClass> allcategories = null;
			try {
				allcategories = bService.findAllCategories(); 
			}catch(SQLException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "error in retrieving categories from database");
				return;
			}
			
			if(fatherCode != 0 && bService.binaryResearch(fatherCode, allcategories) == -1) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters");
				return;
			}
			
			//check the family's depth of descendants
			if(String.valueOf(fatherCode).length() == 15) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "the father is the 15th descendants of its family");
				return;
			}
			
			//compute the code of the new category
			if(bService.hasChildren(fatherCode, allcategories))
			{
				try {
					newChildCode = bService.findLastChild(fatherCode) + 1;
				}catch(Exception e) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "error in finding lastChildCode");
					return;
				}
			}
			else //if the father has not got children, the new category is the first one
				newChildCode = fatherCode * 10 + 1;
			
			if(newChildCode%10 != 0) { //check if the father has not already 9 children
				try {
					bService.createCategory(newChildCode, name, fatherCode);
				}catch(Exception e) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "error in creating the category in database");
					return;
				}
			}
			else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "the father category has already 9 children");
				return;
			}
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
		}
		
	}
	
	@Override
	public void destroy() {
		try {
			if(connection != null)
				connection.close();
		}catch(SQLException sqle) {}
	}

}
