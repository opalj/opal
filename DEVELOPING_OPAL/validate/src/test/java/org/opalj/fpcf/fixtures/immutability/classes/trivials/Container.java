/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

public class Container {

    @TransitivelyImmutableClass(value = "Tree has no fields",
            analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
    @MutableType(value = "Tree is extensible",
            analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
    private static abstract class Tree {

        protected abstract void write(Object o);

       @NonTransitivelyImmutableClass(value = "The body is of ImmutableContainerType",
               analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
       @NonTransitiveImmutableType(value = "The body is of ImmutableContainerType",
               analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
        private static final class Repeated extends Tree {

           @NonTransitivelyImmutableField(value="final field with mutable type",
                   analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                           L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
           @NonAssignableFieldReference(value="final field", analyses = L3FieldAssignabilityAnalysis.class)
            private final Tree body;

            private Repeated(Tree body) {
                this.body = body;
            }

            @Override
            protected void write(Object o) {
                body.write(o);
            }
        }

       @NonTransitivelyImmutableClass(value = "The body is of ImmutableContainerType",
               analyses = L0ClassImmutabilityAnalysis.class)
       @NonTransitiveImmutableType(value = "The body is of ImmutableContainerType",
               analyses = L0TypeImmutabilityAnalysis.class)
        private static final class Optional extends Tree {

           @NonTransitivelyImmutableField(value="final field with mutable type",
                   analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                           L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
           @NonAssignableFieldReference(value="final field", analyses = L3FieldAssignabilityAnalysis.class)
           private final Tree body;

            private Optional(Tree body) {
                this.body = body;
            }

            @Override
            protected void write(Object o) {
                body.write(o);
            }
        }

       @NonTransitivelyImmutableClass(value = "Arrays are treated as immutable", analyses = L0ClassImmutabilityAnalysis.class)
       @NonTransitiveImmutableType(value = "Arrays are treated as immutable", analyses = L0TypeImmutabilityAnalysis.class)
        private static final class Group extends Tree {

           @NonTransitivelyImmutableField(value=" ",
                   analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                           L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
            @NonAssignableFieldReference(value=" ", analyses={L3FieldAssignabilityAnalysis.class})
            private final Tree[] children;

            private Group(Tree[] children) {
                this.children = children;
            }

            @Override
            protected void write(Object o) {
                for (Tree child : children) {
                    child.write(o);
                }
            }
        }
    }
}



