/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Classes that traverse a class file can extend this trait to facilitate
 * reporting the traversed source file elements.
 *
 * @author Michael Eichberg
 */
trait SourceElementsVisitor[T] {

    def visit(classFile: ClassFile): T

    def visit(classFile: ClassFile, method: Method): T

    def visit(classFile: ClassFile, field: Field): T
}

