/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ref_fields;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public abstract class DepFields {

	private Object a = null; // the refined type is X

	private Object b = null; // the refined type is Y

	private Object c = null; // the refined type is Y

	private Object d = null; // the refined type is null but this depends on knowledge about the other types...

	private Object theNullValue = null;

	public void updateA(Object a) {
		if(a instanceof Y) {
			this.a = (Y)a; // the cast is currently required due to a weakness in the underlying domain
		} else {
			this.a = b;
		}
	}

	public void updateB(Object b) {
		if(b instanceof Y) {
			this.b = (Y)b; // the cast is currently required due to a weakness in the underlying domain
		} else {
			b = c;
			a = new Z();
		}

		if(this.c instanceof Z) {
			// Effectively dead code:
			d = new Z();
		}
	}

	public void updateB(Y c) {
		if(c != null)
			this.c = c;
		else
			this.c = new YY();
	}

	public void updateDWithC() {
		Z z = (Z)b; // <= this cast will ALWAYS fail!
		// Effectively dead code:
		d = z;
	}


	@Override
	public String toString() {
		return "The String: "+ a + b + c + d + theNullValue;
	}
}

class X {}

class Y extends X{}

class YY extends Y{}

class Z extends X{}

class ZZ extends Z{}
