/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package methods.a;

import methods.b.B;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
public class User {

	public static void main(String[] args) {
		first();
		second();
		third();
		
		Super.staticDefaultVisibilityMethod();
		IndirectSub.staticPublicVisibilityMethod();
	}

	private static void third() {
		System.out.println("3");
		DirectSub adsub = new DirectSub();
		IndirectSub aidsub = new IndirectSub();
		((Super) aidsub).defaultVisibilityMethod();
		new SubIndirectSub().defaultVisibilityMethod();
	}

	private static void second() {
		System.out.println("2");
		B b = new B();
		methods.b.DirectSub bdsub = new methods.b.DirectSub();
		((Super)b).defaultVisibilityMethod(); // calls "Super's default visibility method using invokevirtual
	}

	private static void first() {
		System.out.println("1");
		Super asuper = new Super();
		asuper.defaultVisibilityMethod();
	}

}
