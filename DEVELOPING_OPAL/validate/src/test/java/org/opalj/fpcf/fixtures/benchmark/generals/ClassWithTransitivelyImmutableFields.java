/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

//@Immutable
@TransitivelyImmutableType("")
@TransitivelyImmutableClass("")
public final class ClassWithTransitivelyImmutableFields {

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable field type")
    @NonAssignableField("Declared final Field")
    private final FinalClassWithNoFields fec2 = new FinalClassWithNoFields();

    //@Immutable
    @TransitivelyImmutableField("Immutable Reference and Immutable Field Type")
    @NonAssignableField("declared final field")
    private final FinalClassWithNoFields fec = new FinalClassWithNoFields();

    public FinalClassWithNoFields getFec() {
        return fec;
    }

    //@Immutable
    @TransitivelyImmutableField("Immutable Reference and Immutable Field Type")
    @NonAssignableField("effective immutable field")
    private FinalClassWithNoFields name = new FinalClassWithNoFields();

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable field type")
    @NonAssignableField(value = "declared final field reference")
    private final FinalClassWithNoFields fec1;

    //@Immutable
    @TransitivelyImmutableField("")
    @NonAssignableField("")
    private static String deepImmutableString = "string";

    public ClassWithTransitivelyImmutableFields(FinalClassWithNoFields fec) {
        this.fec1 = fec;
    }

}





