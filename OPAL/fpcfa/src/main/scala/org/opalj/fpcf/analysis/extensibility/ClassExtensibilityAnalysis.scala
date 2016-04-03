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
package analysis
package extensibility

import org.opalj.br.ClassFile
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.analysis.FPCFAnalysis
import org.opalj.fpcf.analysis.FPCFAnalysisRunner
import org.opalj.br.ObjectType
import org.opalj.log.OPALLogger

/**
 * Determines if a class is extensible (i.e., can be inherited from). This property generally
 * depends on the kind of the project. If the project is an application, all classes
 * are considered to be closed; i.e., the class hierarchy is considered to be fixed; if the
 * analyzed project is a library then the result depends on the concrete assumption about the
 * openess of the library.
 *
 * @note Since the computed property is a set property, other analyses have to wait till the
 * 		property is computed.
 *
 * @author Michael Reif
 */
class ClassExtensibilityAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    final val processedObjectTypes = new Array[Boolean](ObjectType.objectTypesCount)

    /**
     * Computes the extensibility of the given class or interface and also of all supertypes of it.
     * If the analysis mode is ''application like'', no class is considered as extensible (we
     * have a closed class hierarchy).
     *
     * A type is considered as extensible if one of the criteria matches:
     *  - it is not (effectively) final and either public or the analysis mode is OPA,
     *  - one of its subtypes is extensible.
     */
    def determineExtensibility(classFile: ClassFile, subclassIsExtensible: Boolean): Unit = {
        val objectType = classFile.thisType // either a class or an interface definition
        val oid = objectType.id
        if (processedObjectTypes(oid))
            return ;

        processedObjectTypes(oid) = true

        val isExtensible =
            subclassIsExtensible ||
                ((classFile.isPublic || isOpenLibrary) && !classFile.isEffectivelyFinal)

        if (isExtensible) propertyStore.add(IsExtensible)(classFile)

        classHierarchy.directSupertypes(objectType).foreach { superType ⇒
            project.classFile(superType).foreach(determineExtensibility(_, isExtensible))
        }
    }
}

object ClassExtensibilityAnalysis extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(IsExtensible)

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        import project.logContext
        import AnalysisModes.{isDesktopApplication, isJEE6WebApplication}
        val analysis = new ClassExtensibilityAnalysis(project)
        val analysisMode = project.analysisMode
        if (isDesktopApplication(analysisMode) || isJEE6WebApplication(analysisMode)) {
            OPALLogger.info("analysis result", "all classes are not extensible")
            // application types can not be extended
            return analysis; // FIXME creating an analysis just to throw it away is stupid
        }

        val leafTypes = project.classHierarchy.leafTypes
        leafTypes.foreach(project.classFile(_).foreach(analysis.determineExtensibility(_, false)))
        analysis
    }
}
