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
import dao.CategoryDAO;

/**
 * Servlet implementation class ChangeName
 * This servlet changes the name of the category selected
 */
@WebServlet("/ChangeName")
@MultipartConfig
public class ChangeName extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
    public ChangeName() {
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
		
		//read the code and the name of the category from the JSON object
		Gson gson = new Gson();
		CategoryClass category = gson.fromJson(new InputStreamReader(request.getInputStream()), CategoryClass.class);
		Long code = category.getCode();
		String name = category.getName();
		
		//check if the parameters are null or not valid
		if(code == null || code <= 0 || name == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Missing or incorrect parameters");
			return;
		}
		
		CategoryDAO bService = new CategoryDAO(connection);
		List<CategoryClass> allcategories = null;
		
		try {
			allcategories = bService.findAllCategories();
		} catch (SQLException e1) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error retrieving categories from database");
			return;
		}
		
		//check if the category with the code of the parameter exists
		if(bService.binaryResearch(code, allcategories) == -1) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Code not valid");
			return;
		}
		
		//change the name of the category in the database
		try {
			bService.editName(code, name);
		}catch(SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Error in editing category name in database");
			return;
		}
		
		
	}

}
