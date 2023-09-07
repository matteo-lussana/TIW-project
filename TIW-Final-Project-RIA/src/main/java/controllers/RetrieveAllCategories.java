package controllers;

import java.util.List;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

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
import com.google.gson.GsonBuilder;

import beans.*;
import dao.*;


/**
 * Servlet implementation class RetrieveAllCategories
 * This servlet retrieves all the categories from the database
 *
 */
@WebServlet("/RetrieveAllCategories")
@MultipartConfig
public class RetrieveAllCategories extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
    
    public RetrieveAllCategories() {
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

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();
		String error;
		
		if(session == null || session.isNew() || session.getAttribute("username") == null) {
			error = "Please login first";
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().println(error);
			return;
		}
		
		else {
				CategoryDAO bService = new CategoryDAO(connection);
				List<CategoryClass> allcategories = null;
				
				try {
					allcategories = bService.findAllCategories();
				}catch(Exception e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println("Error in retrieving categories from database.");
					return ;
				}
							
				Gson gson = new GsonBuilder().create();
				String jsonCategories = gson.toJson(allcategories);
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write(jsonCategories);
			
		}
	}

	
	@Override
	public void destroy() {
		if(connection != null) {
			try {
				connection.close();
			}catch(SQLException e) {}
		}
	}

}
