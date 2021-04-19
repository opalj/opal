/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
public class NonAssignability {

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    public final Object publicObject = new Object();

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    protected final Object protectedObject = new Object();

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    final Object packagePrivateObject = new Object();

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("Initialized directly")
    private final int a = 1;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("Initialized through instance initializer")
    private final int b;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("Initialized through constructor")
    private final int c;

    final int n = 5;



    // Instance initializer!
    {
        b = 1;
    }

    //@Immutable
    @DeepImmutableField("non-assignable field with primitive type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    public static final int publicStaticFinalInt = 5;

    //@Immutable
    @DeepImmutableField("non-assignable field with primitive type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    protected static final int protectedStaticFinalInt = 5;

    //@Immutable
    @DeepImmutableField("non-assignable field with primitive type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    private static final int privateStaticFinalInt = 5;

    //@Immutable
    @DeepImmutableField("non-assignable field with primitive type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    static final int packagePrivateStaticFinalInt = 5;

    //@Immutable
    @ShallowImmutableField("non-assignable field with mutable type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    public static final Object publicStaticFinalObject = new MutableClass();

    //@Immutable
    @ShallowImmutableField("non-assignable field with mutable type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    protected static final Object protectedStaticFinalObject = new MutableClass();

    //@Immutable
    @ShallowImmutableField("non-assignable field with mutable type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    private static final Object privateStaticFinalObject = new MutableClass();

    //@Immutable
    @ShallowImmutableField("non-assignable field with mutable type")
    @ImmutableFieldReference("non-assignable due to final modifier")
    static final Object packagePrivateStaticFinalObject = new MutableClass();

    public NonAssignability() {
        c=1;
    }
}

class MutableClass{
    public int n = 8;
}
