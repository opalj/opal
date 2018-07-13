/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.collection.Set

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodSignature
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.analyses.CallBySignatureResolutionKey

/**
 * Domain object that can be used to calculate a call graph using CHA. This domain
 * basically collects – for all invoke instructions of a method – the potential target
 * methods that may be invoked at runtime.
 *
 * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to the
 * respective methods of `java.lang.Object`.
 *
 * Signature polymorphic methods are correctly resolved (done by the method
 * `lookupImplementingMethod` defined in `ClassHierarchy`.)
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
class CHACallGraphExtractorWithCBS(
        cache: CallGraphCache[MethodSignature, Set[Method]]
) extends CHACallGraphExtractor(cache) {

    protected[this] class AnalysisContext(method: Method)(implicit project: SomeProject)
        extends super.AnalysisContext(method) {

        final val cbsIndex = project.get(CallBySignatureResolutionKey)

        private[AnalysisContext] def callBySignature(
            declaringClassType: ObjectType,
            name:               String,
            descriptor:         MethodDescriptor
        ): Set[Method] = {
            cbsIndex.findMethods(name, descriptor, declaringClassType)
        }

        override def interfaceCall(
            caller:             Method,
            pc:                 PC,
            declaringClassType: ObjectType,
            name:               String,
            descriptor:         MethodDescriptor
        ): Unit = {

            addCallToNullPointerExceptionConstructor(method, pc)

            val cbsCallees = callBySignature(declaringClassType, name, descriptor)
            val callees = this.callees(caller, declaringClassType, isInterface = true, name, descriptor)

            assert(
                (callees & cbsCallees).isEmpty,
                s"CHACallGraphExtractor: call by signature calls for $name on ${declaringClassType.toJava} \n\n"+
                    s"${cbsCallees.map { _.classFile.thisType.toJava }.mkString(", ")}}\n\n"+
                    s"are not disjunct with normal callees: ${callees.map { _.classFile.thisType.toJava }.mkString(", ")}}\n\n common:"+
                    (callees & cbsCallees).map { m ⇒ m.toJava }.mkString("\n")
            )

            val allCallees = cbsCallees ++ callees
            if (callees.isEmpty) {
                addUnresolvedMethodCall(method, pc, declaringClassType, name, descriptor)
            } else {
                addCallEdge(pc, allCallees)
            }
        }
    }

}
