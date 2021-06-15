/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.DependentImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

/**
 * Headline class
 */
public class Nested{

}

@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@TransitivelyImmutableClass(value="Class has no instance fields",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class Simple<T>{

    @MutableType(value = "class is extensible",
            analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
    @NonTransitivelyImmutableClass(value= "has shallow immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
    class Inner{

        @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
        @DependentlyImmutableField(value= "effective final with generic type T",
                analyses = L0FieldImmutabilityAnalysis.class)
        @NonTransitivelyImmutableField(value="can not handle generic types",
                analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
        @EffectivelyNonAssignableField(value = "effective final field",
                analyses = L3FieldAssignabilityAnalysis.class)
        private T t;

        public Inner(T t){
            this.t = t;
        }
    }

}
@MutableType(value = "class is extensible",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@TransitivelyImmutableClass(value="Class has no instance fields",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class Complex<T>{

    @MutableType(value = "class is extensible",
            analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
    @NonTransitivelyImmutableClass(value= "has shallow immutable fields", analyses = L1ClassImmutabilityAnalysis.class)
    class Inner {

        @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
        @DependentlyImmutableField(value = "immutable reference with generic type",
                analyses = L0FieldImmutabilityAnalysis.class)
        @NonTransitivelyImmutableField(value="can not work with generic type",
                analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
     @EffectivelyNonAssignableField(value= "effective final field", analyses = L3FieldAssignabilityAnalysis.class)
     private GenericClass<T> gc;

     public Inner(GenericClass<T> gc){
         this.gc = gc;
     }
    }
}

@DependentImmutableType(value= "class is not extensible", analyses = L1TypeImmutabilityAnalysis.class)
@NonTransitivelyImmutableType(value = "class is not extensible", analyses = L0TypeImmutabilityAnalysis.class)
@DependentlyImmutableClass(value="has only one dependent immutable field", analyses = L1ClassImmutabilityAnalysis.class)
@NonTransitivelyImmutableClass(value="can not work with dependent immutable fields",
        analyses = L0ClassImmutabilityAnalysis.class)
final class GenericClass<T> {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @DependentlyImmutableField(value = "immutable reference with generic type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value="can not work with generic type",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField(value="effective final field", analyses = L3FieldAssignabilityAnalysis.class)
    private T t;

    public GenericClass(T t){
        this.t = t;
    }
}






