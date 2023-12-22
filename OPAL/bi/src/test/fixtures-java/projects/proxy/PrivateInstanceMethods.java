/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package proxy;

/**
 * A simple test class that provides a set of instance methods for the proxy test.
 * 
 * @author Arne Lottmann
 */
@SuppressWarnings("all") public class PrivateInstanceMethods {
	
	/* ********************************************************
	 * 
	 * Methods without a return type.
	 * 
	 ******************************************************** */
	
	private void noArgumentAndNoReturnValue() {}
	
	private void intAndNoReturnValue(int i) {}
	
	private void intLongAndNoReturnValue(int i, long l) {}
	
	private void floatIntAndNoReturnValue(float f, int i) {}
	
	private void doubleIntAndNoReturnValue(double d, int i) {}
	
	private void intDoubleBooleanAndNoReturnValue(int i, double d, boolean b) {}
	
	private void longIntLongAndNoReturnValue(long l, int i, long k) {}
	
	private void longObjectAndNoReturnValue(long l, Object o) {}
	
	private void stringDoubleAndNoReturnValue(String s, double d) {}
	
	private void stringStringLongAndNoReturnValue(String s, String t, long l) {}
	
	private void intVarintAndNoReturnValue(int i, int...is) {}
	
	private void intVardoubleAndNoReturnValue(int i, double...ds) {}
	
	private void doubleIntStringAndNoReturnValue(double d, int i, String s) {}
	
	private void intIntIntIntIntAndNoReturnValue(int i, int j, int k, int l, int m) {}
	
	private void doubleDoubleDoubleDoubleDoubleAndNoReturnValue(double d, double e, double f, double g, double h) {}
	
	private void intDoubleIntDoubleIntAndNoReturnValue(int i, double d, int j, double e, int k) {}
	
	private void doubleIntDoubleIntDoubleAndNoReturnValue(double d, int i, double e, int j, double f) {}
	
	private void methodWithManyParametersAndNoReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) {}
	
	/* ********************************************************
	 * 
	 * Methods with a return type.
	 * 
	 ******************************************************** */
	
	private int noArgumentAndIntReturnValue() { return 0; }
	
	private double intAndDoubleReturnValue(int i) { return 0d; }
	
	private long intLongAndLongReturnValue(int i, long l) { return 0l;}
	
	private char floatIntAndCharReturnValue(float f, int i) { return '\0'; }
	
	private boolean doubleIntAndBooleanReturnValue(double d, int i) { return true; }
	
	private String intDoubleBooleanAndStringReturnValue(int i, double d, boolean b) { return ""; }
	
	private Object[] longIntLongAndArrayReturnValue(long l, int i, long k) { return new Object[0]; }
	
	private float longObjectAndFloatReturnValue(long l, Object o) { return 0f; }
	
	private short stringDoubleAndShortReturnValue(String s, double d) { return (short) 0; }
	
	private byte stringStringLongAndByteReturnValue(String s, String t, long l) { return (byte) 0; }
	
	private int intVarintAndIntReturnValue(int i, int...is) { return 0; }
	
	private double intVardoubleAndDoubleReturnValue(int i, double...ds) { return 0d; }
	
	private long doubleIntStringAndLongReturnValue(double d, int i, String s) { return 0l; }
	
	private char intIntIntIntIntAndCharReturnValue(int i, int j, int k, int l, int m) { return '\0'; }
	
	private boolean doubleDoubleDoubleDoubleDoubleAndBooleanReturnValue(double d, double e, double f, double g, double h) { return false; }
	
	private byte intDoubleIntDoubleIntAndStringReturnValue(int i, double d, int j, double e, int k) { return (byte) 0; }
	
	private float doubleIntDoubleIntDoubleAndFloatReturnValue(double d, int i, double e, int j, double f) { return 0f; }
	
	private short methodWithManyParametersAndShortReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) { return (short) 0; }
}