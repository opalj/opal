/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with simple methods containing control sequences like if-then.
 * 
 * @author Roberts Kolosovs
 */
public class ControlSequences {

	int icmpne(int a, int b) {
		if (a == b) {
			return a;
		}
		return b;
	}

	int icmpeq(int a, int b) {
		if (a != b) {
			return a;
		}
		return b;
	}

	int icmpge(int a, int b) {
		if (a < b) {
			return a;
		}
		return b;
	}

	int icmplt(int a, int b) {
		if (a >= b) {
			return a;
		}
		return b;
	}

	int icmple(int a, int b) {
		if (a > b) {
			return a;
		}
		return b;
	}

	int icmpgt(int a, int b) {
		if (a <= b) {
			return a;
		}
		return b;
	}

	int ifne(int a) {
		if (a == 0) {
			return a;
		}
		return 0;
	}

	int ifeq(int a) {
		if (a != 0) {
			return a;
		}
		return 0;
	}

	int ifge(int a) {
		if (a < 0) {
			return a;
		}
		return 0;
	}

	int iflt(int a) {
		if (a >= 0) {
			return a;
		}
		return 0;
	}

	int ifle(int a) {
		if (a > 0) {
			return a;
		}
		return 0;
	}

	int ifgt(int a) {
		if (a <= 0) {
			return a;
		}
		return 0;
	}
	
	Object ifacmpeq(Object a, Object b){
		if(a != b){
			return a;
		}
		return b;
	}
	
	Object ifacmpne(Object a, Object b){
		if(a == b){
			return a;
		}
		return b;
	}
	
	Object ifnonnull(Object a){
		if(a == null){
			return a;
		}
		return null;
	}
	
	Object ifnull(Object a){
		if(a != null){
			return a;
		}
		return null;
	}

	int ifTest(int a, int b) {
		int result = 0;
		if (a == b) {
			result = a;

			if (a != b) {
				result = a;
			} else if (a < b) {
				result = a;
			} else {
				result = 1;
			}
		} else if (a >= b) {
			result = a;
		} else if (a > b) {
			result = a;
		} else if (a <= b) {
			result = a;
		} else if (a == 0) {
			result = a;
		} else if (a != 0) {
			result = a;
		} else if (a < 0) {
			result = a;
		} else if (a >= 0) {
			result = a;
		} else if (a > 0) {
			result = a;
		} else {
			result = a;
		}
		return result;
	}
}
