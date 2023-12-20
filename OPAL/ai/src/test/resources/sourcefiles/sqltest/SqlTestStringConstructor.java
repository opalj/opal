package sqltest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlTestStringConstructor {
	
	private final static String _driver = "oracle.jdbc.driver.OracleDriver";
	private final static String _database = "jdbc:oracle:thin:@host:1243:database;shutdown=true";
	private final static String _user = "admin";
	private final static String _pwd = "123";
	
	private final static String _executeMe = new String("03-CONSTRUCTOR-staticField");
	
	public static void main(String[] args) {
		Connection con = null;
		
		try {
			Class.forName(_driver);
			
			con = DriverManager.getConnection( _database, _user, _pwd);
			Statement stmt = con.createStatement();

			String s1 = new String("01-CONSTRUCTOR-Field");
			stmt.executeUpdate(s1);
			
			ResultSet rs = stmt.executeQuery(new String("02-CONSTRUCTOR-directParam"));
			
			stmt.executeQuery(_executeMe);
			
			char[] _04 = "04-CONSTRUCTOR-OfCharArray".toCharArray();
			String s4 = new String(_04);
			stmt.executeUpdate(s4);
			
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
