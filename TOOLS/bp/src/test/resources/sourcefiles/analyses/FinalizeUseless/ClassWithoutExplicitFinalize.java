/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FinalizeUseless;

/**
 * A class without explicit `finalize()` method. This should not be reported.
 * 
 * @author Roberts Kolosovs
 */
public class ClassWithoutExplicitFinalize {

    private String s1 = "You may not see it but it is here.";
    private String s2 = "The finalize. It can't be avoided.";

    public void foo() {
        System.out.println(s1);
        System.out.println(s2);
    }
}
