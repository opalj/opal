package sqltest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlTestConstants {
	
	private final static String _driver = "oracle.jdbc.driver.OracleDriver";
	private final static String _database = "jdbc:oracle:thin:@host:1243:database;shutdown=true";
	private final static String _user = "admin";
	private final static String _pwd = "123";
	
	private final static String _executeMe = "03-CONSTANT-staticField";
	
	public static void main(String[] args) {
		Connection con = null;
		
		try {
			Class.forName(_driver);
			
			con = DriverManager.getConnection( _database, _user, _pwd);
			Statement stmt = con.createStatement();

			String s1 = "01-CONSTANT-Field";
			stmt.executeUpdate(s1);

			ResultSet rs = stmt.executeQuery("02-CONSTANT-directParam");
			
			stmt.executeQuery(_executeMe);
			
			char[] _04 = "04-CONSTANT-StringOfCharArray".toCharArray();
			stmt.executeUpdate(_04.toString());
			
			rs.close();
			stmt.close();
		}catch (ClassNotFoundException c) {
			System.err.println("Driver not found!");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
