/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.open_world.lazy_initialization.scala_lazy_val;

import org.opalj.tac.fpcf.analyses.immutability.field_assignability.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.immutability.ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.L2FieldAssignabilityAnalysis;

/**
*  This class represents the implementation of Scala lazy val from Scala 2.12.
*  https://docs.scala-lang.org/sips/improved-lazy-val-initialization.html
*
*/
@MutableType("non final class")
@TransitivelyImmutableClass(value = "Class has only transitive immutable fields.", analyses = {})
public class LazyCell {

@TransitivelyImmutableField(value = "Lazy initialized field with primitive type", analyses = {})
@LazilyInitializedField(value = "The field is only set once in a synchronized way.", analyses = {})
@AssignableField(value = "The analyses do no recognize lazy initialization over multiple methods",
        analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                L2FieldAssignabilityAnalysis.class})
private volatile boolean bitmap_0 = false;

@TransitivelyImmutableField(value = "Lazy initialized field with primitive type", analyses = {})
@LazilyInitializedField(value = "The field is only set once in a synchronized way.", analyses = {})
@AssignableField(value = "The analysis is not able to recognize lazy initialization over multiple methods",
        analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                L2FieldAssignabilityAnalysis.class})
Integer value_0;

private Integer value_lzycompute() {
    synchronized (this){
        if(value_0==0) {
            value_0 = 42;
            bitmap_0 = true;
        }
    }
    return value_0;
}
public Integer getValue(){
    return bitmap_0 ? value_0 : value_lzycompute();
}
}
