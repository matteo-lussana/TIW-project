package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import dao.UserDAO;

/**
 * Servlet implementation class SignIn
 */
@WebServlet("/SignIn")
public class SignIn extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
       
    public SignIn() {
        super();
    }
    
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
		String path = "signin";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		String name = null, lastname = null, username = null, password = null, password2 = null;
		int code = -1;
		boolean badRequest = false;
		
		try {
			name = request.getParameter("name").toUpperCase();
			lastname = request.getParameter("lastname").toUpperCase();
			username = request.getParameter("username");
			password = request.getParameter("password");
			password2 = request.getParameter("password2");
			code = Integer.parseInt(request.getParameter("code"));
			
			if(name == null || lastname == null || username == null || password == null || password2 == null || code <0)
				badRequest = true;
		}catch(NullPointerException | NumberFormatException e) {
			badRequest = true;
		}
		
		if(badRequest) {
			ctx.setVariable("credentialError", "All field must be filled and the code must be a number");
			templateEngine.process(path, ctx, response.getWriter());
		}
		
		else {
			if(!password.equals(password2)) {
				ctx.setVariable("credentialError", "ERROR: password do not match");
				templateEngine.process(path, ctx, response.getWriter());
			}
			else {
				UserDAO bService = new UserDAO(connection);
				boolean check = false;
				boolean dbError = false;
				
				try {
					check = bService.checkCode(code);
				}catch(SQLException e) {
					dbError = true;
				}
				
				if(dbError) {
					ctx.setVariable("credentialError", "Problems connecting to the database");
					templateEngine.process(path, ctx, response.getWriter());
				}
				
				else if(!check) {
					ctx.setVariable("credentialError", "ERROR: code not valid, if you don't have one ask to your manager");
					templateEngine.process(path, ctx, response.getWriter());
				}
				
				else {
					int check2 = -1;
					try {
						check2 = bService.checkNameAndLastName(code, name, lastname);
					}catch(SQLException e) {
						dbError = true;
					}
					
					if(dbError) {
						ctx.setVariable("credentialError", "Problems connecting to the database");
						templateEngine.process(path, ctx, response.getWriter());
					}
					
					else if(check2 == 0) {
						ctx.setVariable("credentialError", "ERROR: code, name and/or lastname don't match");
						templateEngine.process(path, ctx, response.getWriter());
					}
					else if(check2 == 1){
						ctx.setVariable("credentialError", "You have already an account");
						templateEngine.process(path, ctx, response.getWriter());
					}
					
					else {
						check = false;
						
						try {
							check = bService.checkUsername(username);
						}catch(SQLException e) {
							dbError = true;
						}
						
						if(dbError) {
							ctx.setVariable("credentialError", "Problems connecting to the database");
							templateEngine.process(path, ctx, response.getWriter());
						}
						
						else if(!check) {
							ctx.setVariable("credentialError", "Username already in use");
							templateEngine.process(path, ctx, response.getWriter());
						}
						
						else {
							try {
								bService.createUser(username, password, code);
							}catch(SQLException e) {
								dbError = true;
							}
							
							if(dbError) {
								ctx.setVariable("credentialError", "Problems connecting to the database");
								templateEngine.process(path, ctx, response.getWriter());
							}
							
							else {
								path = "/index.html";
								ctx.setVariable("credentialError", "Registration successful, login with the new credentials!");
								templateEngine.process(path, ctx, response.getWriter());
							}
						}
					}
					
					
				}
			}
		}
		
	}

}
