/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with simple methods containing arithmetic expressions.
 *
 * @author Roberts Kolosovs
 *
 */
public class ArithmeticExpressions {

    // Boolean operations **************************************************************************

    boolean directNegate(boolean b) {
        return !b;
    }

    boolean indirectNegate(boolean b) {
        return !b ? false : true;
    }

    int embeddedNegate(boolean b) {
        int i = !b ? 1 : 0;
        return i + 1;
    }


	// Integer operations **************************************************************************
	int integerAdd(int a, int b){
		return a + b;
	}

	int integerAnd(int a, int b){
		return a & b;
	}

	int integerDiv(int a, int b){
		return a / b;
	}

	int integerInc(int a){
		return a++;
	}

	int integerNeg(int a){
		return -a;
	}

	int integerMul(int a, int b){
		return a * b;
	}

	int integerOr(int a, int b){
		return a | b;
	}

	int integerRem(int a, int b){
		return a % b;
	}

	int integerShR(int a, int b){
		return a >> b;
	}

	int integerShL(int a, int b){
		return a << b;
	}

	int integerSub(int a, int b){
		return a - b;
	}

	int integerASh(int a, int b){
		return a >>> b;
	}

	int integerXOr(int a, int b){
		return a ^ b;
	}

	int integerTest(int a, int b){
		int c = 0;
		c = a + b;
		c = a & b;
		c = a / b;
		a++;
		c = -b;
		c = a * b;
		c = a | b;
		c = a % b;
		c = a >> b;
		c = a << b;
		c = a - b;
		c = a >>> b;
		c = a ^ b;
		return c;
	}

	// Double operations *****************************************************************************
	double doubleAdd(double a, double b){
		return a + b;
	}

	double doubleDiv(double a, double b){
		return a / b;
	}

	boolean doubleCmp(double a, double b){
		return a < b;
	}

	double doubleNeg(double a){
		return -a;
	}

	double doubleMul(double a, double b){
		return a * b;
	}

	double doubleRem(double a, double b){
		return a % b;
	}

	double doubleSub(double a, double b){
		return a - b;
	}

	boolean doubleTest(double a, double b){
		return a < b;
	}

	// Float operations ******************************************************************************
	float floatAdd(float a, float b){
		return a + b;
	}

	float floatDiv(float a, float b){
		return a / b;
	}

	boolean floatCmp(float a, float b){
		return a < b;
	}

	float floatNeg(float a){
		return -a;
	}

	float floatMul(float a, float b){
		return a * b;
	}

	float floatRem(float a, float b){
		return a % b;
	}

	float floatSub(float a, float b){
		return a - b;
	}

	// Long operations *******************************************************************************
	long longAdd(long a, long b){
		return a + b;
	}

	long longAnd(long a, long b){
		return a & b;
	}

	long longDiv(long a, long b){
		return a / b;
	}

	long longNeg(long a){
		return -a;
	}

	long longMul(long a, long b){
		return a * b;
	}

	long longOr(long a, long b){
		return a | b;
	}

	long longRem(long a, long b){
		return a % b;
	}

	long longShR(long a, int b){
		return a >> b;
	}

	long longShL(long a, int b){
		return a << b;
	}

	long longSub(long a, long b){
		return a - b;
	}

	long longASh(long a, int b){
		return a >>> b;
	}

	long longXOr(long a, long b){
		return a ^ b;
	}
}
