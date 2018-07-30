/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

import java.util.List;
import java.util.ArrayList;

/**
 * Class with simple methods containing cast and typecheck instructions.
 *
 * @author Roberts Kolosovs
 */
@SuppressWarnings("unused")
public class CastInstructions {

	void typecheckString(String s){
		boolean result = s instanceof Object;
	}

	void typecheckList(ArrayList<?> l){
		boolean result = l instanceof List<?>;
	}

	void checkcast(Object o){
		List<?> l = (List<?>) o;
	}

	void d2f(double d){
		float result = (float) d;
	}

	void d2i(double d){
		int result = (int) d;
	}

	void d2l(double d){
		long result = (long) d;
	}

	void f2d(float f){
		double result = (double) f;
	}

	void f2i(float f){
		int result = (int) f;
	}

	void f2l(float f){
		long result = (long) f;
	}

	void l2d(long l){
		double result = (double) l;
	}

	void l2f(long l){
		float result = (float) l;
	}

	void l2i(long l){
		int result = (int) l;
	}

	void i2d(int i){
		double result = (double) i;
	}

	void i2f(int i){
		float result = (float) i;
	}

	void i2l(int i){
		long result = (long) i;
	}

	void i2c(int i){
		char result = (char) i;
	}

	void i2b(int i){
		byte result = (byte) i;
	}

	void i2s(int i){
		short result = (short) i;
	}


}
