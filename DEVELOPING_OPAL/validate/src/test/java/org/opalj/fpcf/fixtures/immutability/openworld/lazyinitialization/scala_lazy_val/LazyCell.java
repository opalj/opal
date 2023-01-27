/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.scala_lazy_val;

import org.opalj.tac.fpcf.analyses.fieldassignability.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.tac.fpcf.analyses.fieldassignability.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
*  This class represents the implementation of a Scala lazy val from Scala 2.12.
*  https://docs.scala-lang.org/sips/improved-lazy-val-initialization.html
*
*/
@TransitivelyImmutableClass(value = "The class has only transitive immutable fields", analyses = {})
public class LazyCell {

@TransitivelyImmutableField(value = "The field is lazily initialized and has a primitive type", analyses = {})
@LazilyInitializedField(value = "The field is only set once in a synchronized way.", analyses = {})
@AssignableField(value = "The analyses cannot recognize lazy initialization over multiple methods",
        analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                L2FieldAssignabilityAnalysis.class})
private volatile boolean bitmap_0 = false;

@TransitivelyImmutableField(value = "The field is lazily initialized and has a primitive type", analyses = {})
@LazilyInitializedField(value = "The field is only set once in a synchronized way.", analyses = {})
@AssignableField(value = "The analysis cannot recognize lazy initialization over multiple methods",
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
