package sqltest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlTestConcatString {
	private final static String _driver = "oracle.jdbc.driver.OracleDriver";
	private final static String _database = "jdbc:oracle:thin:@host:1243:database;shutdown=true";
	private final static String _user = "admin";
	private final static String _pwd = "123";
	
	public static void main(String[] args) {
		Connection con = null;
		
		try {
			Class.forName(_driver);
			
			con = DriverManager.getConnection( _database, _user, _pwd);
			Statement stmt = con.createStatement();

			stmt.executeUpdate("01-CONCAT-" + "directInSink");
			
			String s2 = "02-CONCAT-" + "inField";
			stmt.executeUpdate(s2);
			
			stmt.executeUpdate("03-CONCAT-" + "directInSink" + "." + ".");
			stmt.executeUpdate("04-CONCAT-" + "directInSink" + "." + new String("lastIn<init>"));
			String s3 = new String("05-CONCAT-") + new String("inFieldWithConstructor");
			stmt.executeUpdate(s3);
			
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
