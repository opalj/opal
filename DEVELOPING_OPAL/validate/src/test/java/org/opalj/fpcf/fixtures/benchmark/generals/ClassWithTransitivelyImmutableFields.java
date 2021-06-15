/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

//@Immutable
@TransitivelyImmutableType("Class is transitively immutable and final and as a result not extensible")
@TransitivelyImmutableClass("Class has only transitively immutable fields")
public final class ClassWithTransitivelyImmutableFields {

    //@Immutable
    @TransitivelyImmutableField("Field is non assignable and has a transitively immutable type")
    @NonAssignableField("Field is final")
    private final FinalClassWithNoFields transitivelyImmutableFieldWithGetter = new FinalClassWithNoFields();

    public FinalClassWithNoFields getTransitivelyImmutableFieldWithGetter() {
        return transitivelyImmutableFieldWithGetter;
    }

    //@Immutable
    @TransitivelyImmutableField("Field is non assignable and has a transitively immutable type")
    @NonAssignableField("Field is final")
    private final FinalClassWithNoFields eagerAssignedFinalTransitivelyImmutableField =
            new FinalClassWithNoFields();

    //@Immutable
    @TransitivelyImmutableField("Field is non assignable and has a transitively immutable type")
    @NonAssignableField("Field is final")
    private final FinalClassWithNoFields inConstructorAssignedFinalFieldWithTransitivelyImmutableType;

    //@Immutable
    @TransitivelyImmutableField("Field is non assignable and has a transitively immutable type")
    @NonAssignableField("Field is final")
    private final static String transitivelyImmutableFieldWithStringType = "string";

    public ClassWithTransitivelyImmutableFields(FinalClassWithNoFields fec) {
        this.inConstructorAssignedFinalFieldWithTransitivelyImmutableType = fec;
    }

}





