package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.UserDAO;

/**
 * Servlet implementation class SignIn
 */
@WebServlet("/SignIn")
@MultipartConfig
public class SignIn extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

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
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String error = null;
		
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
			error = "All field must be filled and the code must be a number";
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(error);
			return;
		}
		
		else {
			if(!password.equals(password2)) {
				error = "ERROR: passwords do not match";
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println(error);
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
					error = "Problems connecting to the database";
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().println(error);
				}
				
				else if(!check) {
					error = "ERROR: code not valid, if you don't have one ask to your manager";
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().println(error);
				}
				
				else {
					int check2 = -1;
					try {
						check2 = bService.checkNameAndLastName(code, name, lastname);
					}catch(SQLException e) {
						dbError = true;
					}
					
					if(dbError) {
						error = "Problems connecting to the database";
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						response.getWriter().println(error);
					}
					
					else if(check2 == 0) {
						error = "ERROR: code, name and/or lastname don't match";
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.getWriter().println(error);
					}
					else if(check2 == 1){
						error = "You have already an account";
						response.setStatus(HttpServletResponse.SC_CONFLICT);
						response.getWriter().println(error);
					}
					
					else {
						check = false;
						
						try {
							check = bService.checkUsername(username);
						}catch(SQLException e) {
							dbError = true;
						}
						
						if(dbError) {
							error = "Problems connecting to the database";
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							response.getWriter().println(error);
						}
						
						else if(!check) {
							error = "Username already in use";
							response.setStatus(HttpServletResponse.SC_CONFLICT);
							response.getWriter().println(error);
						}
						
						else {
							try {
								bService.createUser(username, password, code);
							}catch(SQLException e) {
								dbError = true;
							}
							
							if(dbError) {
								error = "Problems connecting to the database";
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								response.getWriter().println(error);
							}
							
							else {
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("application/json");
								response.setCharacterEncoding("UTF-8");
							}
						}
					}
					
					
				}
			}
		}
		
	}

}
