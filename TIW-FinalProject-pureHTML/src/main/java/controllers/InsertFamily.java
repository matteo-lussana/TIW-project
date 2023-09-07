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
 * Servlet implementation class InsertFamily
 * This servlet inserts the sub-tree copied using the link "copy" in the database
 */
@WebServlet("/InsertFamily")
public class InsertFamily extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
    
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
			long familyFather = -1;
			long targetCode = -1;
			boolean badRequest = false;
			
			try{
				familyFather = Long.parseLong(request.getParameter("familyFather"));
				targetCode = Long.parseLong(request.getParameter("targetCode"));
				
				if(familyFather < 0 || targetCode < 0)
					badRequest = true;
			}catch(NullPointerException | NumberFormatException e) { badRequest = true;}
			
			if(badRequest) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or incorrect parameters");
				return;
			}
			
			if(String.valueOf(targetCode).startsWith(String.valueOf(familyFather))) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "The target code cannot be a child of the branch to copy");
				return;
			}
			
			CategoryDAO bService = new CategoryDAO(connection);
			List<CategoryClass> allcategories = null;
			
			try {
				allcategories = bService.findAllCategories();
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error in retrieving categories from database");
				return;
			}
			
			if(targetCode != 0 && bService.binaryResearch(targetCode, allcategories) == -1 || familyFather != 0 && bService.binaryResearch(familyFather, allcategories) == -1) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters");
				return;
			}
			
			List<CategoryClass> family = bService.findFamily(familyFather, allcategories);
			
			if(targetCode != 0) {	
				if(bService.computeDescendantsNum(familyFather, family) - String.valueOf(familyFather).length() + String.valueOf(targetCode).length() > 14) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "The branch to copy is too deep");
					return;
				}
			}
			
			long newAncestorCode = -1;
			if(bService.hasChildren(targetCode, allcategories))
			{
				try {
					newAncestorCode = bService.findLastChild(targetCode) + 1;
				}catch(SQLException e) {
					e.printStackTrace();
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error calculating last child");
					return;
				}
			}
			else
				newAncestorCode = targetCode * 10 + 1;
			if(newAncestorCode%10 == 0) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "The target category has 9 sub-categories");
				return;
			}
			
			try {
				bService.moveFamily(family, newAncestorCode);
			}catch(SQLException e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error coping categories in database");
				return;
			}
			
			//find all the categories with the copied sub-tree
			allcategories=null;
			try {
				allcategories = bService.findAllCategories();
			} catch (SQLException e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error in retrieving categories from database");
				return;
			}
			
			List<CategoryClass> ancestors = bService.findAncestors(allcategories);
			List<CategoryClass> noCopyCategories = bService.findNoCopyCategories(allcategories);
			
			path = "/WEB-INF/HomePage.html";
			ctx.setVariable("allcategories", allcategories);
			ctx.setVariable("ancestors", ancestors);
			ctx.setVariable("family", null);
			ctx.setVariable("noCopyHereCategories", null);
			ctx.setVariable("noCopyCategories", noCopyCategories);
			templateEngine.process(path, ctx, response.getWriter());
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
