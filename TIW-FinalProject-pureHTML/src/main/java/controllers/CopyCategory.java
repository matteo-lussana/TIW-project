package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
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
import dao.CategoryDAO;

/**
 * Servlet implementation class CopyCategory
 * This servlet manages the copy of sub-trees: after checking everything is valid, this servlet calls the 
 * HomePage setting the correct parameters.
 */
@WebServlet("/CopyCategory")
public class CopyCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
       
    
    public CopyCategory() {
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
    
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			boolean badRequest = false;
			long ancestorCode = -1;
			
			try {
				ancestorCode = Long.parseLong(request.getParameter("categoryCode"));
				if(ancestorCode <0)
					badRequest = true;
			}catch(NumberFormatException | NullPointerException e) { badRequest = true; }
			
			if(badRequest) { 
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'categoryCode' with format number is required");
				return;
			}
			
			List<CategoryClass> allcategories = null;
			List<CategoryClass> ancestors = null;
			
			CategoryDAO bService = new CategoryDAO(connection);
			try {
				allcategories = bService.findAllCategories();
				ancestors = bService.findAncestors(allcategories);
			}catch(Exception e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retriving categories from database");
				return;
			}
			
			//sub-tree to copy
			List<CategoryClass> family = bService.findFamily(ancestorCode, allcategories);
			
			List<CategoryClass> noCopyHereCategories = bService.findNoCopyHereCategories(family, allcategories);

			path = "/WEB-INF/HomePage.html";
			ctx.setVariable("allcategories", allcategories);
			ctx.setVariable("ancestors", ancestors);
			ctx.setVariable("family", family);
			ctx.setVariable("noCopyHereCategories", noCopyHereCategories);
			templateEngine.process(path, ctx, response.getWriter());
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
