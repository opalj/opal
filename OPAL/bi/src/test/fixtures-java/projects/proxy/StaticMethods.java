/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package proxy;

/**
 * A simple test class that provides a set of static methods for the proxy test.
 * 
 * @author Arne Lottmann
 */
public class StaticMethods {
	
	/* ********************************************************
	 * 
	 * Methods without a return type.
	 * 
	 ******************************************************** */
	
	public static void noArgumentAndNoReturnValue() {}
	
	public static void intAndNoReturnValue(int i) {}
	
	public static void intLongAndNoReturnValue(int i, long l) {}
	
	public static void floatIntAndNoReturnValue(float f, int i) {}
	
	public static void doubleIntAndNoReturnValue(double d, int i) {}
	
	public static void intDoubleBooleanAndNoReturnValue(int i, double d, boolean b) {}
	
	public static void longIntLongAndNoReturnValue(long l, int i, long k) {}
	
	public static void longObjectAndNoReturnValue(long l, Object o) {}
	
	public static void stringDoubleAndNoReturnValue(String s, double d) {}
	
	public static void stringStringLongAndNoReturnValue(String s, String t, long l) {}
	
	public static void intVarintAndNoReturnValue(int i, int...is) {}
	
	public static void intVardoubleAndNoReturnValue(int i, double...ds) {}
	
	public static void doubleIntStringAndNoReturnValue(double d, int i, String s) {}
	
	public static void intIntIntIntIntAndNoReturnValue(int i, int j, int k, int l, int m) {}
	
	public static void doubleDoubleDoubleDoubleDoubleAndNoReturnValue(double d, double e, double f, double g, double h) {}
	
	public static void intDoubleIntDoubleIntAndNoReturnValue(int i, double d, int j, double e, int k) {}
	
	public static void doubleIntDoubleIntDoubleAndNoReturnValue(double d, int i, double e, int j, double f) {}
	
	public static void methodWithManyParametersAndNoReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) {}
	
	/* ********************************************************
	 * 
	 * Methods with a return type.
	 * 
	 ******************************************************** */
	
	public static int noArgumentAndIntReturnValue() { return 0; }
	
	public static double intAndDoubleReturnValue(int i) { return 0d; }
	
	public static long intLongAndLongReturnValue(int i, long l) { return 0l;}
	
	public static char floatIntAndCharReturnValue(float f, int i) { return '\0'; }
	
	public static boolean doubleIntAndBooleanReturnValue(double d, int i) { return true; }
	
	public static String intDoubleBooleanAndStringReturnValue(int i, double d, boolean b) { return ""; }
	
	public static Object[] longIntLongAndArrayReturnValue(long l, int i, long k) { return new Object[0]; }
	
	public static float longObjectAndFloatReturnValue(long l, Object o) { return 0f; }
	
	public static short stringDoubleAndShortReturnValue(String s, double d) { return (short) 0; }
	
	public static byte stringStringLongAndByteReturnValue(String s, String t, long l) { return (byte) 0; }
	
	public static int intVarintAndIntReturnValue(int i, int...is) { return 0; }
	
	public static double intVardoubleAndDoubleReturnValue(int i, double...ds) { return 0d; }
	
	public static long doubleIntStringAndLongReturnValue(double d, int i, String s) { return 0l; }
	
	public static char intIntIntIntIntAndCharReturnValue(int i, int j, int k, int l, int m) { return '\0'; }
	
	public static boolean doubleDoubleDoubleDoubleDoubleAndBooleanReturnValue(double d, double e, double f, double g, double h) { return false; }
	
	public static byte intDoubleIntDoubleIntAndStringReturnValue(int i, double d, int j, double e, int k) { return (byte) 0; }
	
	public static float doubleIntDoubleIntDoubleAndFloatReturnValue(double d, int i, double e, int j, double f) { return 0f; }
	
	public static short methodWithManyParametersAndShortReturnValue(double d, float f, long l, int i, short s, byte b, char c, boolean bo, String str, Object[] array) { return (short) 0; }
}