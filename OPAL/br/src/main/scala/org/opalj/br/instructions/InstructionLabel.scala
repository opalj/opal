/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

trait InstructionLabelFactory {
    final implicit def symbolToInstructionLabel(l: Symbol): InstructionLabel = InstructionLabel(l)
    final implicit def nameToInstructionLabel(l: String): InstructionLabel = InstructionLabel(l)
    final implicit def pcToInstructionLabel(l: PC): InstructionLabel = InstructionLabel(l)
}

object InstructionLabel extends InstructionLabelFactory {
    def apply(s: Symbol): NamedLabel = new NamedLabel(s.name)
    def apply(name: String): NamedLabel = new NamedLabel(name)
    def apply(pc: Int): PCLabel = new PCLabel(pc)
}

