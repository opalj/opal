/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

public class Container {

    @DeepImmutableClass(value = "Tree has no fields",
            analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
    @MutableType(value = "Tree is extensible",
            analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
    private static abstract class Tree {

        protected abstract void write(Object o);

       @ShallowImmutableClass(value = "The body is of ImmutableContainerType",
               analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
       @ShallowImmutableType(value = "The body is of ImmutableContainerType",
               analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
        private static final class Repeated extends Tree {

           @ShallowImmutableField(value="final field with mutable type",
                   analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                           L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
           @ImmutableFieldReference(value="final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
            private final Tree body;

            private Repeated(Tree body) {
                this.body = body;
            }

            @Override
            protected void write(Object o) {
                body.write(o);
            }
        }

       @ShallowImmutableClass(value = "The body is of ImmutableContainerType",
               analyses = L0ClassImmutabilityAnalysis.class)
       @ShallowImmutableType(value = "The body is of ImmutableContainerType",
               analyses = L0TypeImmutabilityAnalysis.class)
        private static final class Optional extends Tree {

           @ShallowImmutableField(value="final field with mutable type",
                   analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                           L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
           @ImmutableFieldReference(value="final field", analyses = L0FieldReferenceImmutabilityAnalysis.class)
           private final Tree body;

            private Optional(Tree body) {
                this.body = body;
            }

            @Override
            protected void write(Object o) {
                body.write(o);
            }
        }

       @ShallowImmutableClass(value = "Arrays are treated as immutable", analyses = L0ClassImmutabilityAnalysis.class)
       @ShallowImmutableType(value = "Arrays are treated as immutable", analyses = L0TypeImmutabilityAnalysis.class)
        private static final class Group extends Tree {

           @ShallowImmutableField(value=" ",
                   analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                           L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
            @ImmutableFieldReference(value=" ", analyses={L0FieldReferenceImmutabilityAnalysis.class})
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



