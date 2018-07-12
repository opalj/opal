/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package methods.a;

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
public class Super {

  // instance methods

  public void publicMethod() {
    // empty
  }

  void defaultVisibilityMethod() {
    System.out.println("Super.defaultVisibilityMethod()");
  }

  private void privateMethod() {
    // empty
  }

  // static methods

  static void staticDefaultVisibilityMethod() {
    System.out.println("Super.staticDefaultVisibilityMethod()");
  }

  public static void staticPublicVisibilityMethod() {
    System.out.println("Super.staticPublicVisibilityMethod()");
  }
}
