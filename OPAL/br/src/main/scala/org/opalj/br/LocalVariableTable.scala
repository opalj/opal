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

/**
 * Representation of the local variable table.
 *
 * @author Michael Eichberg
 */
case class LocalVariableTable(localVariables: LocalVariables) extends CodeAttribute {

    override def kindId: Int = LocalVariableTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: LocalVariableTable ⇒ this.similar(that)
            case _                        ⇒ false
        }
    }

    def similar(other: LocalVariableTable): Boolean = {
        // the order of two local variable tables does not need to be identical
        this.localVariables.size == other.localVariables.size &&
            this.localVariables.forall(other.localVariables.contains)
    }

    override def remapPCs(codeSize: Int, f: PC ⇒ PC): CodeAttribute = {
        val newLocalVariables = localVariables.flatMap(_.remapPCs(codeSize, f))
        LocalVariableTable(newLocalVariables)
    }
}
object LocalVariableTable {

    final val KindId = 20

}
