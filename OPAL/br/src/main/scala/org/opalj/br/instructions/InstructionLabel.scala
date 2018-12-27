/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import java.util.concurrent.atomic.AtomicLong

import scala.language.implicitConversions

/**
 * A label that identifies an instruction.
 *
 * InstructionLabels have to support structural equality (They are used as keys in maps).
 *
 * @author Michael Eichberg
 */
sealed trait InstructionLabel {
    def isPCLabel: Boolean
    def pc: Int
}

/**
 * A `Label` used to specify the "original" pc of an instruction. `PCLabel`s are assigned to
 * all (labeled) instructions when we create the labeled code based on a method's bytecode.
 * (I.e., when we instrument existing code.)
 */
case class PCLabel(pc: Int) extends InstructionLabel {
    def isPCLabel: Boolean = true
    override def toString: String = s"PC($pc)"
}

/**
 *
 * @param id A globally unique id.
 */
case class RewriteLabel private (id: Long) extends InstructionLabel {
    def isPCLabel: Boolean = false
    def pc: Int = throw new UnsupportedOperationException();
    override def toString: String = s"Rewrite($id)"
}

object RewriteLabel {
    private final val idGenerator = new AtomicLong(0L)
    def apply(): RewriteLabel = {
        val newID = idGenerator.getAndIncrement()
        if (newID == -1) {
            throw new IllegalStateException("out of labels; contact the OPAL developers");
        }
        new RewriteLabel(newID)
    }
}

case class NamedLabel(name: String) extends InstructionLabel {
    def isPCLabel: Boolean = false
    def pc: Int = throw new UnsupportedOperationException();
    override def toString: String = s"'$name"
}

object InstructionLabel {

    final implicit def symbolToInstructionLabel(l: Symbol): InstructionLabel = InstructionLabel(l)
    final implicit def nameToInstructionLabel(l: String): InstructionLabel = InstructionLabel(l)
    final implicit def pcToInstructionLabel(l: PC): InstructionLabel = InstructionLabel(l)

    def apply(s: Symbol): NamedLabel = new NamedLabel(s.name)
    def apply(name: String): NamedLabel = new NamedLabel(name)
    def apply(pc: Int): PCLabel = new PCLabel(pc)
}

