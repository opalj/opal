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
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.ai.DefinitionSite
import org.opalj.ai.DefinitionSiteLike
import org.opalj.br.Field
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.LocalField

class FieldLocalityState(val field: Field) {
    private[this] var declaredMethodsDependees: Set[EOptionP[DeclaredMethod, Property]] = Set.empty
    private[this] var definitionSitesDependees: Set[EOptionP[DefinitionSiteLike, EscapeProperty]] = Set.empty

    private[this] var clonedDependees: Set[DefinitionSiteLike] = Set.empty

    private[this] var temporary: FieldLocality = LocalField

    def dependees: Set[EOptionP[Entity, Property]] = {
        declaredMethodsDependees ++ definitionSitesDependees
    }

    def hasNoDependees: Boolean = {
        declaredMethodsDependees.isEmpty &&
            definitionSitesDependees.isEmpty
    }
    def isDefinitionSiteOfClone(e: DefinitionSite): Boolean =
        clonedDependees.contains(e)

    def addMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit =
        declaredMethodsDependees += ep

    def removeMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit =
        declaredMethodsDependees = declaredMethodsDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

    def updateMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit = {
        removeMethodDependee(ep)
        addMethodDependee(ep)
    }

    def addClonedDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit = {
        addDefinitionSiteDependee(ep)
        clonedDependees += ep.e
    }

    def addDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit =
        definitionSitesDependees += ep

    def removeDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit =
        definitionSitesDependees = definitionSitesDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

    def updateAllocationSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit = {
        removeDefinitionSiteDependee(ep)
        addDefinitionSiteDependee(ep)
    }

    def updateWithMeet(f: FieldLocality): Unit = temporary = temporary.meet(f)
    def temporaryState: FieldLocality = temporary
}
