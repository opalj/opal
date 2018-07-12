/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with simple methods containing array creation and manipulation instructions.
 *
 * @author Roberts Kolosovs
 */
@SuppressWarnings("unused")
public class ArrayCreationAndManipulation {

	void refArray(){
		Object[] oa = new Object[5];
		oa[4] = new Object();
		Object o = oa[4];
	}

	void multidimArray(){
		int[][] mdia = new int[4][2];
		int lngth = mdia.length;
	}

	void doubleArray(){
		double[] da = new double[5];
		da[4] = 1.0d;
		double d = da[4];
	}

	void floatArray(){
		float[] fa = new float[5];
		fa[4] = 2.0f;
		float f = fa[4];
	}

	void intArray(){
		int[] ia = new int[5];
		ia[4] = 2;
		int i = ia[4];
	}

	void longArray(){
		long[] la = new long[5];
		la[4] = 1L;
		long l = la[4];
	}

	void shortArray(){
		short[] sa = new short[5];
		sa[4] = 2;
		short s = sa[4];
	}

	void byteArray(){
		byte[] ba = new byte[5];
		ba[4] = 2;
		byte b = ba[4];
	}

	void charArray(){
		char[] ca = new char[5];
		ca[4] = 2;
		char c = ca[4];
	}
}
