/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Implements immutable class correctly with private constructors, and a factory method.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPCorrectyImplementationWithFactoryMethod {

    private String foo;

    private JCIPCorrectyImplementationWithFactoryMethod() {
        // Implemented to override the default construcor, so that this class cann now
        // only be build with the factory method.
    }

    private JCIPCorrectyImplementationWithFactoryMethod(String arg0) {
        foo = arg0;
    }

    public String getFoo() {
        return foo;
    }

    // This is the factory method to call the private construcor.
    // The only way to get an instance of this class.
    public static JCIPCorrectyImplementationWithFactoryMethod makeJCIPCorrectyImplementationWithFactoryMethod(
            String arg0) {
        return new JCIPCorrectyImplementationWithFactoryMethod(arg0);
    }

}
