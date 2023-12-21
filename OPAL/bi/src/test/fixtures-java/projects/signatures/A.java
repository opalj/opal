/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package signatures;

import java.io.Serializable;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("serial")
public abstract class A extends java.util.ArrayList<B<java.util.HashSet<?>>> {

	public java.util.List<B<? super java.util.HashSet<?>>>[] bs;

	public B<?> b;

	public <T, X extends Serializable & Comparable<T>, Y extends java.util.Map<T, X>> void foo(T t,
			X x, Y y, java.util.List<? super Comparable<T>> list) {
		// we do nothing...
	}

	public abstract <T extends RuntimeException & Serializable> void bar() throws T;
}
