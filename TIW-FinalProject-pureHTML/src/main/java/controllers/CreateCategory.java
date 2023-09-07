package controllers;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

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
 * This servlet manages the creation of a new category in the database with the form in the HomePage
 */
@WebServlet("/CreateCategory")
public class CreateCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
    
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
    	
    	ServletContext servletContext = getServletContext();
    	ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
    	templateResolver.setTemplateMode(TemplateMode.HTML);
    	this.templateEngine = new TemplateEngine();
    	this.templateEngine.setTemplateResolver(templateResolver);
    	templateResolver.setSuffix(".html");
    }
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(false);
		String path;
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		if(session == null || session.isNew() || session.getAttribute("username") == null) {
			path = "/index.html";
			ctx.setVariable("credentialError", "Please login first");
			templateEngine.process(path, ctx, response.getWriter());
		}
		
		else {
			String name = null;
			long fatherCode = -1;
			boolean badRequest = false;
			long newChildCode;
			
			try {
				name = request.getParameter("name");
				if(request.getParameterValues("isFather")!= null) fatherCode = 0;
				else fatherCode = Long.parseLong(request.getParameter("fatherCode"));
				
				if(name.isEmpty() || fatherCode < 0) 
					badRequest = true;
			}catch(NullPointerException | NumberFormatException e) {
				badRequest = true;
			}
			
			if(badRequest) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing or incorrect parameters");
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
			
			if(String.valueOf(fatherCode).length() == 15) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "the father is the 15th descendants of its family");
				return;
			}
			
			if(bService.hasChildren(fatherCode, allcategories))
			{
				try {
					newChildCode = bService.findLastChild(fatherCode) + 1;
				}catch(Exception e) {
					e.printStackTrace();
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "error in finding lastChildCode");
					return;
				}
			}
			else //if the father doesn't have children the new category is the first child
				newChildCode = fatherCode * 10 + 1; 
			
			if(newChildCode%10 != 0) {
				try {
					bService.createCategory(newChildCode, name, fatherCode);
				}catch(Exception e) {
					e.printStackTrace();
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error in creating the category in database");
					return;
				}
			}
			else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "The father category has already 9 children");
				return;
			}
			
			String ctxpath = getServletContext().getContextPath();
			path = ctxpath + "/GoToHomePage";
			response.sendRedirect(path);
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
