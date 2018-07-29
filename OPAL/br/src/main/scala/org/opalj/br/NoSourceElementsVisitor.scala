/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Simple implementation of the [[SourceElementsVisitor]] trait where all methods
 * do nothing.
 *
 * @author Michael Eichberg
 */
trait NoSourceElementsVisitor extends SourceElementsVisitor[Unit] {

    override def visit(classFile: ClassFile): Unit = { /* EMPTY */ }

    override def visit(classFile: ClassFile, method: Method): Unit = { /* EMPTY */ }

    override def visit(classFile: ClassFile, field: Field): Unit = { /* EMPTY */ }
}

