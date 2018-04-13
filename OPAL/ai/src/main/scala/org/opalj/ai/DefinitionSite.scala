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
package ai

import org.opalj.br.Method

sealed class DefinitionSite(val method: Method, val pc: PC, val uses: PCs) {

    def canEqual(other: Any): Boolean = other.isInstanceOf[DefinitionSite]

    override def equals(other: Any): Boolean = other match {
        case that: DefinitionSite ⇒
            (that canEqual this) &&
                method == that.method &&
                pc == that.pc
        case _ ⇒ false
    }

    override def hashCode(): Int = {
        val state: Seq[Any] = Seq(method, pc)
        state.map(_.hashCode()).foldLeft(0)((a, b) ⇒ 31 * a + b)
    }

    override def toString = s"DefinitionSite($method, $pc)"
}

object DefinitionSite {
    def unapply(ds: DefinitionSite): Option[(Method, PC, PCs)] = Some((ds.method, ds.pc, ds.uses))
}
final class DefinitionSiteWithFilteredUses(method: Method, pc: PC, uses: PCs)
    extends DefinitionSite(method, pc, uses) {

    override def canEqual(other: Any): Boolean = other.isInstanceOf[DefinitionSiteWithFilteredUses]

    override def equals(other: Any): Boolean = other match {
        case that: DefinitionSiteWithFilteredUses ⇒
            super.equals(that) &&
                uses == that.uses
        case _ ⇒ false
    }

    override def hashCode(): Int = {
        val state: Seq[Any] = Seq(super.hashCode(), uses)
        state.map(_.hashCode()).foldLeft(0)((a, b) ⇒ 31 * a + b)
    }

    override def toString = s"DefinitionSiteWithFilteredUses($method, $pc, $uses)"
}
