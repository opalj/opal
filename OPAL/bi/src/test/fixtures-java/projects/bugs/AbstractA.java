/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package bugs;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public abstract class AbstractA implements Comparable<Integer> {

	public boolean equals(SuperA obj) {
		return super.equals(obj);
	}

	public abstract boolean equals(AbstractA obj);

	
	public int compareTo(Integer o) {
		return 0;
	}

}
