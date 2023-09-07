package controllers;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
 */
@WebServlet("/CheckLogin")
@MultipartConfig
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
    public CheckLogin() {
        super();
    }
    
    /**
     * doGet only used to redirect the invalid url requests to the login page
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
		
		String error = null;
		boolean badRequest = false;
		
		//get the parameters
		String usrn = null, pwd = null;
		try {
			usrn = request.getParameter("username");
			pwd = request.getParameter("password");
		}catch(NullPointerException e) {
			badRequest = true;
		}
		if(badRequest || usrn == null || usrn.isEmpty() || pwd == null || pwd.isEmpty() ) {
			error = "missing parameters";
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println(error);
			return;
		}
		
		else{
			UserDAO usr = new UserDAO(connection); 
			User u = null;
			
			//check if the credential are correct
			try {
				u = usr.checkCredential(usrn, pwd);
			}catch(SQLException e) {	
				error = "Fail in credential checking";
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println(error);
				return;
			}
			
			
			if(u == null) {
				error = "Username and/or password not valid";
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println(error);
				return;
			}
			
			else {
				request.getSession().setAttribute("username", usrn);
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
			}
			
		}
	}
	
	public void destroy() {
		try {
			if(connection != null)
				connection.close();
		}catch(SQLException sqle) {}
		
	}

}
