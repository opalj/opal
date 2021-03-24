/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@MutableClass("")
class ClassWithProtectedFields {

    //@Immutable
    @MutableField("the field has a mutable field reference")
    @MutableFieldReference("the field is protected")
    protected FinalEmptyClass fec1 = new FinalEmptyClass();

    //@Immutable
    @MutableField("Because of Mutable Reference")
    @MutableFieldReference("Because it is declared as protected")
    protected ClassWithNotDeepImmutableFields cwpf1 = new ClassWithNotDeepImmutableFields(new ClassWithMutableField());

    //@Immutable
    @ShallowImmutableField("field has an immutable reference and mutable type")
    @ImmutableFieldReference("Declared final Field")
    private final ClassWithNotDeepImmutableFields cwpf2 = new ClassWithNotDeepImmutableFields(new ClassWithMutableField());

    //@Immutable
    @DeepImmutableField("immutable reference and deep immutable field type")
    @ImmutableFieldReference("Declared final Field")
    private final FinalEmptyClass fec2 = new FinalEmptyClass();
}