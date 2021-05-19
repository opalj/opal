/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

/**
 * This testclass tests that different modifiers like transient, volatile or static
 * does not have an impact of mutability.
 *
 * @author Tobias Roth
 *
 */
@MutableType(value= "class has different mutable fields",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "class has different mutable fields",
        analyses =  {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
public class DifferentModifier {

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public int a = 5;

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public transient int b = 5;

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public volatile int c = 5;

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public volatile long ctl;

    DifferentModifier(long ctl){
        this.ctl = ctl;
    }

      static final class InnerClass {

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                          L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
          @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
          public static int LEFT = 1;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                          L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
          @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
        public int c = 5;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                          L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
          @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
        public transient int d = 5;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                          L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
          @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
        public volatile int e = 5;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                          L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
          @AssignableField(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
        public volatile transient int f = 5;
    }
}
