/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

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
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public int a = 5;

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public transient int b = 5;

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public volatile int c = 5;

    @MutableField(value = "field has a mutable field reference",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
    public volatile long ctl;

    DifferentModifier(long ctl){
        this.ctl = ctl;
    }

      static final class InnerClass {

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                          L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
          @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
          public static int LEFT = 1;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                          L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
          @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
        public int c = 5;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                          L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
          @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
        public transient int d = 5;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                          L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
          @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
        public volatile int e = 5;

          @MutableField(value = "field has a mutable field reference",
                  analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                          L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
          @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
        public volatile transient int f = 5;
    }
}
