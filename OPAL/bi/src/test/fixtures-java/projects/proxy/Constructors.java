/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package proxy;

/**
 * A simple test class that provides a set of constructors for the proxy test.
 * 
 * @author Arne Lottmann
 */
public class Constructors {
	
	/* ********************************************************
	 * 
	 * Methods without a return type.
	 * 
	 ******************************************************** */
	
	public Constructors() {}
	
	public Constructors(int i) {}
	
	public Constructors(int i, long l) {}
	
	public Constructors(float f, int i) {}
	
	public Constructors(double d, int i) {}
	
	public Constructors(int i, double d, boolean b) {}
	
	public Constructors(long l, int i, long k) {}
	
	public Constructors(long l, Object o) {}
	
	public Constructors(String s, double d) {}
	
	public Constructors(String s, String t, long l) {}
	
	public Constructors(int i, int...is) {}
	
	public Constructors(int i, double...ds) {}
	
	public Constructors(double d, int i, String s) {}
	
	public Constructors(int i, int j, int k, int l, int m) {}
	
	public Constructors(double d, double e, double f, double g, double h) {}
	
	public Constructors(int i, double d, int j, double e, int k) {}
	
	public Constructors(double d, int i, double e, int j, double f) {}
	
	public Constructors(
			double d, float f, long l, int i, short s, byte b, char c, boolean bo, 
			String str, Object[] array) {}
}