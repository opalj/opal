/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import FinalizeUseless.ClassWithoutExplicitFinalize;

/**
 * Class for testing handling of classes with missing class file in immutability analysis.
 * 
 * @author Roberts Kolosovs
 */
public class MutableClassWithUnknownField {

    // The .jar containing this class is not loaded during testing.
    public final ClassWithoutExplicitFinalize foo;

    public MutableClassWithUnknownField(ClassWithoutExplicitFinalize arg0) {
        foo = arg0;
    }
}
