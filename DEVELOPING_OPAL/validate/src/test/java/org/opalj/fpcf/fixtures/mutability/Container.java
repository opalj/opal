/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.mutability;

import org.opalj.fpcf.properties.class_mutability.ImmutableContainerObject;
import org.opalj.fpcf.properties.class_mutability.ImmutableObject;
import org.opalj.fpcf.properties.type_mutability.ImmutableContainerType;


public class Container {

    @ImmutableObject("Tree has no fields")
    @ImmutableContainerType("Group is an ImmutableContainerObject")
    private static abstract class Tree {
        protected abstract void write(Object o);

        @ImmutableContainerObject("The body is of ImmutableContainerType")
        @ImmutableContainerType("The body is of ImmutableContainerType")
        private static final class Repeated extends Tree {
            private final Tree body;

            private Repeated(Tree body) {
                this.body = body;
            }

            @Override
            protected void write(Object o) {
                body.write(o);
            }
        }

        @ImmutableContainerObject("The body is of ImmutableContainerType")
        @ImmutableContainerType("The body is of ImmutableContainerType")
        private static final class Optional extends Tree {
            private final Tree body;

            private Optional(Tree body) {
                this.body = body;
            }

            @Override
            protected void write(Object o) {
                body.write(o);
            }
        }

        @ImmutableContainerObject("Arrays are treated as immutable")
        @ImmutableContainerType("Arrays are treated as immutable")
        private static final class Group extends Tree {
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



