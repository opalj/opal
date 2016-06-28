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
package analyses

import scala.collection.Set
import org.opalj.br.analyses._
import org.opalj.br.{Method, MethodDescriptor, ObjectType}
import org.opalj.fpcf.properties.CallBySignature
import org.opalj.fpcf.properties.NoCBSTargets
import org.opalj.fpcf.properties.CBSTargets
import org.opalj.fpcf.analysis.CallBySignatureTargetAnalysis

/**
 * An index that enables the efficient lookup of potential
 * call by signature resolution interface methods
 * given the method's name and the descriptor type.
 *
 * @note To get call by signature resolution information call
 * 		[[org.opalj.br.analyses.Project]]'s method and pass in the
 * 		[[CallBySignatureResolutionKey]] object.
 * @author Michael Reif
 */
class CallBySignatureResolution private (
        val project:       SomeProject,
        val propertyStore: PropertyStore
) {

    /**
     * Given the `name` and `descriptor` of a method declared by an interface and the `declaringClass`
     * where the method is declared, all those  methods are returned that have a matching name and
     * descriptor and are declared in the same package. All those methods are implemented
     * by classes (not interfaces) that '''do not inherit''' from the respective interface and
     * which may have a subclass (in the future) that may implement the interface.
     *
     * Hence, when we compute the call graph for a library the returned methods may (in general)
     * be call targets.
     *
     * @note This method assumes the closed packages assumption
     */
    def findMethods(
        name:       String,
        descriptor: MethodDescriptor,
        declClass:  ObjectType
    ): Set[Method] = {

        assert(
            project.classFile(declClass).map(_.isInterfaceDeclaration).getOrElse(true),
            s"the declaring class ${declClass.toJava} does not define an interface type"
        )

        import org.opalj.util.GlobalPerformanceEvaluation.time

        time('cbs) {

            val method = project.classFile(declClass) match {
                case Some(cf) ⇒
                    val m = cf.findMethod(name, descriptor)
                    if (m.isEmpty)
                        return Set.empty
                    else
                        m.get
                case None ⇒ return Set.empty;
            }

            val result = propertyStore(method, CallBySignature.Key)
            result match {
                case EP(_, NoCBSTargets)              ⇒ Set.empty
                case EP(_, CBSTargets(targetMethods)) ⇒ targetMethods
                case _                                ⇒ throw new AnalysisException("unsupported entity", null)
            }
        }
    }
}

/**
 * Factory to create [[CallBySignatureResolution]] information.
 *
 * @author Michael Reif
 */
object CallBySignatureResolution {

    def apply(project: SomeProject, isInterrupted: () ⇒ Boolean): CallBySignatureResolution = {
        val analysisManager = project.get(FPCFAnalysesManagerKey)
        analysisManager.runWithRecommended(CallBySignatureTargetAnalysis)(waitOnCompletion = false)

        new CallBySignatureResolution(
            project,
            project.get(SourceElementsPropertyStoreKey)
        )
    }
}
