/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with simple methods containing instructions loading constants.
 *
 * @author Roberts Kolosovs
 */
 @SuppressWarnings("unused")
public class Constants {

	void intConsts(){
		int zero = 0;
		int one = 1;
		int two = 2;
		int three = 3;
		int four = 4;
		int five = 5;
		int mone = -1;
	}

	void longConsts(){
		long zero = 0L;
		long one = 1L;
	}

	void floatConsts(){
		float zero = 0.0f;
		float one = 1.0f;
		float two = 2.0f;
	}

	void doubleConsts(){
		double zero = 0.0d;
		double one = 1.0d;
	}

	void nullReferenceConst(){
		Object nil = null;
	}

	void loadConstants(){
		int intTen = 10;
		float floatTen = 10.0f;
		long longTen = 10L;
		double doubleTen = 10.0d;
	}
}
