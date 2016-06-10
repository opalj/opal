/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package properties

/**
 * Determines for each method if it belongs to the public interface, hence, can be inherited by a '''future''' - yet unknown - subtype. Since
 * this property is defined w.r.t. future subtyping it is '''only''' relevant when libraries are analyzed.
 *
 * It specifies in particular whether a method can be inherited by a subtype that is created by some client of the library.
 * When a method cannot be inherited by a future subtype is discussed in the following:
 *
 * == Inheritance w.r.t. Applications ==
 *
 * If an application is analyzed, this analysis does not make any sense since all used types are already known. Therefore,
 * all applications methods should have the property [[NotInheritableByNewTypes]].
 *
 * == Inheritance w.r.t. Open Packages Assumption ==
 *
 * inheritance is possible if:
 * $ - if the method's visibility modifier is either public, protected or package visible
 * $ - if the method's declaring class is not (effectively) final
 * $ - CPA has to be applied to classes which are in the package "java.*"
 *
 * == Inheritance w.r.t. Closed Packages Assumption ==
 *
 * Inheritance is possible if:
 * $ - if the method's visibility modifier is public or protected
 * $ - if the method's declaring class not is (effectively) final
 * $ - if the method's declaring class is package visible and does not have a public subtype
 *     within the same package that inherits the method where the method is not overridden on
 *    the path from the declaring type to the public subtype
 *
 * == Special Cases ==
 *
 * Notice that the packages "java.*" are closed, hence, nobody can contribute to this packages. Therefore, only
 * public or protected methods where the declaring class is either public or the declaring class has a public subtype that does
 * not override the method can have the property [[IsInheritableByNewTypes]]. Therefore, the closed packages assumption can always
 * be applied to these packages.
 *
 * == Fallback ==
 *
 * 	The sound fallback of this property depends on the actual analysis mode under which the property is computed.
 *
 *  If the analysis mode is some application analysis mode (Desktop, J2EE etc.), the entity [[NotInheritableByNewTypes]].
 *  If the analysis mode is some library analysis mode (CPA, OPA), the sound approximation is [[IsInheritableByNewTypes]].
 *
 *
 * == Cycle Resolution Strategy ==
 *
 * None.
 *
 * @author Michael Reif
 */
sealed trait InheritableByNewTypes extends Property {

    final type Self = InheritableByNewTypes

    final def key = InheritableByNewTypes.Key

    final def isRefineable = false

    def isInheritable(analysisMode: AnalysisMode): Boolean
}

object InheritableByNewTypes {

    final val cycleResolutionStrategy: PropertyKey.CycleResolutionStrategy = (
        ps: PropertyStore,
        epks: PropertyKey.SomeEPKs
    ) ⇒ {
        throw new Error("there should be no cycles")
    }

    final val Key = {
        PropertyKey.create[InheritableByNewTypes](
            "InheritableByNewTypes",
            fallbackProperty = (ps: PropertyStore, e: Entity) ⇒ AnalysisModeSpecific,
            cycleResolutionStrategy = cycleResolutionStrategy
        )
    }
}

case object AnalysisModeSpecific extends InheritableByNewTypes {

    def inheritability(am: AnalysisMode): InterfaceForNewSubtypes = {
        import AnalysisModes._
        am match {
            case DesktopApplication                  ⇒ NotInheritableByNewTypes
            case JEE6WebApplication                  ⇒ NotInheritableByNewTypes
            case LibraryWithClosedPackagesAssumption ⇒ IsInheritableByNewTypes
            case LibraryWithOpenPackagesAssumption   ⇒ IsInheritableByNewTypes
            case _                                   ⇒ throw new UnknownError(s"Not supported AnalysisMode $am. Please make sure that your analysisMode is supported by this property.")
        }
    }

    final def isInheritable(analysisMode: AnalysisMode): Boolean = {
        inheritability(analysisMode).isInheritable(analysisMode)
    }
}

sealed trait InterfaceForNewSubtypes extends InheritableByNewTypes

case object IsInheritableByNewTypes extends InterfaceForNewSubtypes {
    final def isInheritable(analysisMode: AnalysisMode): Boolean = true
}

case object NotInheritableByNewTypes extends InterfaceForNewSubtypes {
    final def isInheritable(analysisMode: AnalysisMode): Boolean = false
}