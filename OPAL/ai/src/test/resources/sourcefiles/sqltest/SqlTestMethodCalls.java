package sqltest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlTestMethodCalls {
	private final static String _driver = "oracle.jdbc.driver.OracleDriver";
	private final static String _database = "jdbc:oracle:thin:@host:1243:database;shutdown=true";
	private final static String _user = "admin";
	private final static String _pwd = "123";
	
	
	
	public static void callmain(){
		main(new String[]{""}, "05-METHOD-ParameterConstant");
	}
	
	public static void callMain(String value, String[] args){
		main(args, value);
	}
	
	public static void callMainParamSwitch(){
		callMain("06-METHOD-ParameterOfTransMethod", new String[]{""});
	}
	
	public static void main(String[] args, String s5) {
		Connection con = null;
		
		try {
			Class.forName(_driver);
			
			con = DriverManager.getConnection( _database, _user, _pwd);
			Statement stmt = con.createStatement();

			stmt.executeUpdate(getId("01-METHOD-directReturnID"));
			
			String s2 = getId("02-METHOD-ReturnIDToField");
			stmt.executeUpdate(s2);
			
			stmt.executeUpdate(getConstructorID("03-METHOD-directReturnConstructorID"));
			
			stmt.executeUpdate(getStringViaInstanceMethod());
			
			stmt.executeQuery(s5);
			
			
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
	
	private static String getId(String id){
		return id;
	}
	
	private static String getConstructorID(String id){
		return new String(id);
	}
	
	private static String getStringViaInstanceMethod() {
		class X {
			protected String getInjection(){
				return "04-METHOD-InstanceMethodReturn";
			}
		}
		return new X().getInjection();
	}
}
