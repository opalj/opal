/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

import java.util.List;
import java.util.ArrayList;

/**
 * Class with simple methods containing instructions for stack manipulation and
 * synchronization.
 *
 * @author Roberts Kolosovs
 */
@SuppressWarnings("all")
public class StackManipulationAndSynchronization {

	static int staticMethod(int arg1, int arg2) {
		return arg1 + arg2;
	}

	int returnInt() {
		return 1;
	}

	double returnDouble() {
		return 1.0d;
	}

	void pop() {
		returnInt();
	}

	void pop2case2() {
		returnDouble();
	}

	void dup() {
		Object o = new Object();
	}

	void monitorEnterAndExit() {
		synchronized (this) {
            pop();
		}
	}

	void invokeStatic() {
		int res = staticMethod(1, 2);
	}

	void invokeInterface() {
		List l = new ArrayList();
		l.add(new Object());
	}
}
