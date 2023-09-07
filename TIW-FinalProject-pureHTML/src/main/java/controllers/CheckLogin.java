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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import beans.User;
import dao.UserDAO;

/**
 * Servlet implementation class CheckLogin
 * This servlet checks if the login is valid
 * 
 */
@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
    
    public CheckLogin() {
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

		String usrn = request.getParameter("username");
		String pwd = request.getParameter("password");
		
		String path;
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		if(usrn == null || usrn.isEmpty()){
			path = "/index.html";
			ctx.setVariable("credentialError", "Insert your username, please");
			templateEngine.process(path, ctx, response.getWriter());
		}
		else if(pwd == null || pwd.isEmpty()) {
			path = "/index.html";
			ctx.setVariable("credentialError", "Insert your password, please");
			templateEngine.process(path, ctx, response.getWriter());
		}
		else{
			UserDAO usr = new UserDAO(connection); 
			User u = null;
			
			try {
				u = usr.checkCredential(usrn, pwd);
			}catch(SQLException e) {
				
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Fail in credential checking");
				return;
			}
			
			if(u == null) {
				path = "/index.html";
				ctx.setVariable("credentialError", "Username and/or password incorrect. Try again!");
				templateEngine.process(path, ctx, response.getWriter());
			}
			else {
				HttpSession session = request.getSession();
				session.setAttribute("username", usrn);
				path = getServletContext().getContextPath()+"/GoToHomePage";
				response.sendRedirect(path);
			}
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
