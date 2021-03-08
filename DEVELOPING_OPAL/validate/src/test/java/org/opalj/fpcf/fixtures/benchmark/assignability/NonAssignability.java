/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
public class NonAssignability {

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    public final Object publicObject = new Object();

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    protected final Object protectedObject = new Object();

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    final Object packagePrivateObject = new Object();

    @DeepImmutableField("")
    @ImmutableFieldReference("Initialized directly")
    private final int a = 1;

    @DeepImmutableField("")
    @ImmutableFieldReference("Initialized through instance initializer")
    private final int b;

    @DeepImmutableField("")
    @ImmutableFieldReference("Initialized through constructor")
    private final int c;

    public NonAssignability() {
        c=1;
    }

    // Instance initializer!
    {
        b = 1;
    }

}
