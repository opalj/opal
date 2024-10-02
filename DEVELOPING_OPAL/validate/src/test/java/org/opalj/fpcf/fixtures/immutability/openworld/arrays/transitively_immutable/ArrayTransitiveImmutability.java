/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.arrays.transitively_immutable;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.tac.fpcf.analyses.FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.immutability.ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.immutability.TypeImmutabilityAnalysis;

/**
 * This class encompasses different array-typed transitively immutable fields.
 */
@TransitivelyImmutableType(value = "Class is final", analyses = {})
@TransitivelyImmutableClass(value = "Class has only transitively immutable fields", analyses = {})
@NonTransitivelyImmutableType(value = "The analysis has only recognized non transitively immutable fields", analyses = {TypeImmutabilityAnalysis.class})
@NonTransitivelyImmutableClass(value = "The analysis has only recognized non transitively immutable fields", analyses = {ClassImmutabilityAnalysis.class})
public final class ArrayTransitiveImmutability {

    @TransitivelyImmutableField(value = "The elements of the array can not escape", analyses = {})
    @NonTransitivelyImmutableField(value = "The analysis is currently not able to recognize transitively immutable arrays", analyses = {
            FieldImmutabilityAnalysis.class})
    @NonAssignableField("Field is final")
    private final Integer[] eagerAssignedIntegerArray = new Integer[]{1, 2, 3};

    @TransitivelyImmutableField(value = "The elements of the array are never set", analyses = {})
    @NonTransitivelyImmutableField(value = "The analysis is currently not able to recognize transitively immutable arrays", analyses = {
            FieldImmutabilityAnalysis.class})
    @NonAssignableField("The field is only assigned once")
    private static final Integer[] staticTransitivelyImmutableArray = new Integer[5];

    @TransitivelyImmutableField(value = "The elements of the array can not escape", analyses = {})
    @NonTransitivelyImmutableField(value = "The analysis is currently not able to recognize transitively immutable arrays", analyses = {
            FieldImmutabilityAnalysis.class})
    @NonAssignableField("Field is initialized in the constructor")
    private final Integer[] transitivelyImmutableArrayAssignedInTheConstructor;

    public ArrayTransitiveImmutability() {
        transitivelyImmutableArrayAssignedInTheConstructor = new Integer[]{5, 6, 7, 8};
    }

    @TransitivelyImmutableField(value = "The elements of the array can not escape", analyses = {})
    @NonTransitivelyImmutableField(value = "The analysis is currently not able to recognize transitively immutable arrays", analyses = {
            FieldImmutabilityAnalysis.class})
    @EffectivelyNonAssignableField("The array is not initialized.")
    private Object[] notInitializedArray;

    @TransitivelyImmutableField(value = "The field is not assignable and only assigned with transitively immutable objects.", analyses = {})
    @NonTransitivelyImmutableField(value = "The analysis is currently not able to recognize transitively immutable arrays", analyses = {
            FieldImmutabilityAnalysis.class})
    @EffectivelyNonAssignableField("The field is effectively only assigned once.")
    private Object[] clonedArray = new Object[]{new Object(), new Object()};

    public Object[] getClonedArray(){ return  clonedArray.clone(); }
}
