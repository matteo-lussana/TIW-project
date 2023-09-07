package dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import beans.User;

/**
 * This class checks the credentials of the users
 *
 */
public class UserDAO {
	private Connection con;
	
	public UserDAO(Connection con) {
		this.con = con;
	}
	
	public User checkCredential(String username, String password) throws SQLException {
		String query = "SELECT Username FROM users WHERE Username = ? and Password = ?";
		
		try(PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setString(1, username);
			pstatement.setString(2, password);
			
			try(ResultSet result = pstatement.executeQuery();){
				if(!result.isBeforeFirst())
					return null; 
				else {
					result.next();
					User user = new User();
					user.setUsername(result.getString("username"));
					return user;
				}
			}
		}
		
	}
	
	public boolean checkCode(int code) throws SQLException {
		String query = "SELECT * FROM employee WHERE code = ?";
		
		try(PreparedStatement pstatement = con.prepareStatement(query);){
			pstatement.setInt(1, code);
			
			try(ResultSet result = pstatement.executeQuery();){
				if(!result.isBeforeFirst())
					return false; 
				else 
					return true;
			}
		}
	}
	
	public int checkNameAndLastName(int code, String name, String lastname)throws SQLException {
		String query = "SELECT * FROM employee WHERE code = ? and name = ? and lastname = ?";

		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setInt(1, code);
			pstatement.setString(2, name);
			pstatement.setString(3, lastname);

			try (ResultSet result = pstatement.executeQuery();) {
				if (!result.isBeforeFirst())
					return 0;
				else {
					result.next();
					if(result.getString("nickname") != null)
						return 1;
					else
						return 2;
				}
			}
		}
	}
	
	public boolean checkUsername(String username)throws SQLException {
		String query = "SELECT * FROM users WHERE username = ?";

		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setString(1, username);

			try (ResultSet result = pstatement.executeQuery();) {
				if (!result.isBeforeFirst())
					return true;
				else 
					return false;
			}
		}
	}
	
	public void createUser(String username, String password, int code)throws SQLException{
		String query = "INSERT into users (username, password) VALUES (?, ?)";

		try (PreparedStatement pstatement = con.prepareStatement(query);) {
			pstatement.setString(1, username);
			pstatement.setString(2, password);
			pstatement.executeUpdate();
		}
		
		query = "UPDATE employee SET nickname = ? WHERE code = ?";
		try(PreparedStatement pstatement = con.prepareStatement(query);){
			pstatement.setString(1, username);
			pstatement.setInt(2, code);
			pstatement.executeUpdate();
		}
	}
}
