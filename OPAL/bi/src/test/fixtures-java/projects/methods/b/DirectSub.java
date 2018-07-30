/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package methods.b;

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
public class DirectSub extends methods.a.Super {

  public void publicMethod() {
    // empty
  }

  void defaultVisibilityMethod() {
    System.out.println("DirectSub.defaultVisibilityMethod()");
  }

  private void privateMethod() {
    // empty
  }

  static void staticDefaultVisibilityMethod() {
    System.out.println("Super.staticDefaultVisibilityMethod()");
  }
}
