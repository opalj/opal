package ai;

/**
 * A very large number of methods that do not contain any control-flow
 * statements.
 *
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves
 * documentation purposes. The compiled class that is used by the tests is found
 * in the test-classfiles directory.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
public class SimpleMethods {

	//
	// RETURNS
	public static void nop() {
		return;
	}

	public static int iOne() {
		return 1;
	}

	public static long lOne() {
		return 1l;
	}

	public static double dOne() {
		return 1.0d;
	}

	public static float fOne() {
		return 1.0f;
	}

	//
	// LDC
	public static String sLDC() {
		return "LDC";
	}

	//
	// PARAMETER
	public static int identity(int i) {
		return i;
	}

	public static String sOne(String s) {
		return s;
	}

	// BINARY OPERATIONS ON INT
	public static int iAdd(int i, int j) {
		return i + j;
	}

	public static int iAnd(int i, int j) {
		return i & j;
	}

	public static int iDiv(int i, int j) {
		return i / j;
	}

	public static int iMul(int i, int j) {
		return i * j;
	}

	public static int iOr(int i, int j) {
		return i | j;
	}

	public static int iShl(int i, int j) {
		return i << j;
	}

	public static int iShr(int i, int j) {
		return i >> j;
	}

	public static int iRem(int i, int j) {
		return i % j;
	}

	public static int iSub(int i, int j) {
		return i - j;
	}

	public static int iushr(int i, int j) {
		return i >>> -2;
	}

	public static int iXor(int i, int j) {
		return i ^ j;
	}

	//
	// BINARY OPERATIONS ON LONG
	public static long lAdd(long i, long j) {
		return i + j;
	}

	public static long lAnd(long i, long j) {
		return i & j;
	}

	public static long lDiv(long i, long j) {
		return i / j;
	}

	public static long lMul(long i, long j) {
		return i * j;
	}

	public static long lOr(long i, long j) {
		return i | j;
	}

	public static long lShl(long i, long j) {
		return i << j;
	}

	public static long lShr(long i, long j) {
		return i >> j;
	}

	public static long lRem(long i, long j) {
		return i % j;
	}

	public static long lSub(long i, long j) {
		return i - j;
	}

	public static long lushr(long i, long j) {
		return i >>> -2;
	}

	public static long lXor(long i, long j) {
		return i ^ j;
	}

	//
	// BINARY OPERATIONS ON DOUBLE
	public static double dAdd(double i, double j) {
		return i + j;
	}

	public static double dSub(double i, double j) {
		return i - j;
	}

	public static double dMul(double i, double j) {
		return i * j;
	}

	public static double dDiv(double i, double j) {
		return i / j;
	}

	public static double dRem(double i, double j) {
		return i % j;
	}

	//
	// BINARY OPERATIONS ON FLOAT
	public static float fAdd(float i, float j) {
		return i + j;
	}

	public static float fSub(float i, float j) {
		return i - j;
	}

	public static float fMul(float i, float j) {
		return i * j;
	}

	public static float fDiv(float i, float j) {
		return i / j;
	}

	public static float fRem(float i, float j) {
		return i % j;
	}

	//
	// INTEGER TYPE CONVERSION
	public static byte iToByte(int i) {
		return (byte) i;
	}

	public static char iToChar(int i) {
		return (char) i;
	}

	public static double iToDouble(int i) {
		return (double) i;
	}

	public static float iToFloat(int i) {
		return (float) i;
	}

	public static long iToLong(int i) {
		return (long) i;
	}

	public static short iToShort(int i) {
		return (short) i;
	}

	//
	// LONG TYPE CONVERSION
	public static double lToDouble(long i) {
		return (double) i;
	}

	public static float lToFloat(long i) {
		return (float) i;
	}

	public static int lToInt(long i) {
		return (int) i;
	}

	//
	// DOUBLE TYPE CONVERSION
	public static float dToFloat(double i) {
		return (float) i;
	}

	public static int dToInt(double i) {
		return (int) i;
	}

	public static long dToLong(double i) {
		return (long) i;
	}

	//
	// FLOAT TYPE CONVERSION
	public static double fToDouble(float i) {
		return (double) i;
	}

	public static int fToInt(float i) {
		return (int) i;
	}

	public static long fToLong(float i) {
		return (long) i;
	}

	//
	// UNARY EXPRESSIONS
	public static float fNeg(float i) {
		return -i;
	}

	public static double dNeg(double i) {
		return -i;
	}

	public static long lNeg(long i) {
		return -i;
	}

	public static int iNeg(int i) {
		return -i;
	}


	//
	// TYPE CHECKS
	public static SimpleMethods asSimpleMethods(Object o) {
		return (SimpleMethods) o; // this is deliberately not (type) safe
	}

	public static boolean asSimpleMethodsInstance(Object o) {
		return o instanceof SimpleMethods ; // this is deliberately not (type) safe
	}

	/*
	 *
	 */
	public static double twice(double i) {
		return 2 * i;
	}

	public static double square(double i) {
		return i * i;
	}

	public static String objectToString(Object o) {
		return o.toString();
	}




	// SEGMENT FIELDS (START)
	private float value;

	public void setValue(float value) {
		this.value = value;
	}

	public float getValue() {
		return value;
	}
	// SEGMENT FIELDS (END)

	// SEGMENT STATIC FIELDS (START)
	private static float sValue;

	public void setSValue(float value) {
		SimpleMethods.sValue = value;
	}

	public float getSValue() {
		return sValue;
	}
	// SEGMENT STATIC FIELDS (END)

		//
		// LOADS AND STORES
		// ILOAD 1-5  & ISTORE 1-5
		public int localInt() {
			int i = 1;
			int j = i;
			int k = j;
			int l = k;
			int m = l;
			return m;
		}
		// LLOAD 1,3,5 & LSTORE 1,3,5
		public long localLongOdd() {
			long i = 1;
			long j = i;
			long k = j;
			long l = k;
			long m = l;
			return m;
		}
		
		//LLOAD 2,4 & LSTORE 2,4
		public long localLongEven() {
			int a = 1;
			long i = 1;
			long j = i;
			long k = j;
			long l = k;
			long m = l;
			return m;
		}
		//DLOAD 1,3,5 & DSTORE 1,3,5
		public double localDoubleOdd() {
			double i = 1;
			double j = i;
			double k = j;
			double l = k;
			double m = l;
			return m;
		}
		
		//DLOAD 2,4 & DSTORE 2,4
		public double localDoubleEven() {
			int a = 1;
			double i = 1;
			double j = i;
			double k = j;
			double l = k;
			double m = l;
			return m;
		}
		
		//FLOAD 1-5 & FSTORE 1-5
		public float localFloat() {
			float i = 1;
			float j = i;
			float k = j;
			float l = k;
			float m = l;
			return m;
		}
		
		//ALOAD 1-5 & ASTORE 1-5
		public SimpleMethods localSimpleMethod(){
			SimpleMethods simpleMethods1 = new SimpleMethods();
			SimpleMethods simpleMethods2 = simpleMethods1;
			SimpleMethods simpleMethods3 = simpleMethods2;
			SimpleMethods simpleMethods4 = simpleMethods3;
			SimpleMethods simpleMethods5 = simpleMethods4;
			return simpleMethods5;
			
		}
		
		

		public static <T> T asIs(T o) {
			return o; // the type of the return value directly depends on the input
						// value
		}

		public static void multipleCalls() {
			java.util.List<Object> o = asIs(new java.util.ArrayList<Object>(100));
			o.toString();
			o.hashCode();
		}

		public static <T> T create(Class<T> clazz) throws Exception {
			return clazz.newInstance();
		}

	}


