/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@MutableClass("")
public class ClassWithNotDeepImmutableFields {

    //@Immutable
    @ShallowImmutableField("field has an immutable field reference and mutable type")
    @ImmutableFieldReference("declared final reference")
    private final ClassWithNotDeepImmutableFields cwpf = new ClassWithNotDeepImmutableFields(new ClassWithMutableField());

    public ClassWithNotDeepImmutableFields getTmc() {
        return cwpf;
    }

    //@Immutable
    @ShallowImmutableField("immutable field reference and mutable type ClassWithPublicFields")
    @ImmutableFieldReference("declared final field")
    private final ClassWithMutableField tmc;

    public ClassWithNotDeepImmutableFields(ClassWithMutableField tmc){
        this.tmc = tmc;
    }

    //@Immutable
    @MutableField(value = "field is public")
    @MutableFieldReference(value = "field is public")
    public int n = 0;

    //@Immutable
    @MutableField(value = "field is public")
    @MutableFieldReference(value = "field is public")
    public String name = "name";
}