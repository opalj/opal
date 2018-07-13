/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses.cg

import scala.collection.Set
import scala.collection.Map
import scala.collection.immutable.HashSet
import scala.collection.mutable.OpenHashMap

import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.MethodSignature
import org.opalj.br.analyses.SomeProject

/**
 * @author Michael Eichberg
 */
trait CallGraphExtractor { extractor ⇒

    import CallGraphExtractor.LocalCallGraphInformation

    /**
     * This method may be executed concurrently for multiple different methods.
     */
    def extract(method: Method)(implicit project: SomeProject): LocalCallGraphInformation

    def cache: CallGraphCache[MethodSignature, Set[Method]]

    abstract protected[this] class AnalysisContext extends Callees {

        def project: SomeProject
        def method: Method

        @inline final def cache: CallGraphCache[MethodSignature, Set[Method]] = extractor.cache

        //
        //
        // Managing/Storing Call Edges
        //
        //

        var unresolvableMethodCalls = List.empty[UnresolvedMethodCall]

        @inline def addUnresolvedMethodCall(
            caller: Method, pc: PC,
            calleeClass: ReferenceType, calleeName: String, calleeDescriptor: MethodDescriptor
        ): Unit = {
            unresolvableMethodCalls ::=
                new UnresolvedMethodCall(caller, pc, calleeClass, calleeName, calleeDescriptor)
        }

        def allUnresolvableMethodCalls: List[UnresolvedMethodCall] = unresolvableMethodCalls

        private[this] val callEdgesMap = OpenHashMap.empty[PC, Set[Method]]

        @inline final def addCallEdge(
            pc:      PC,
            callees: Set[Method]
        ): Unit = {
            if (callEdgesMap.contains(pc)) {
                callEdgesMap(pc) ++= callees
            } else {
                callEdgesMap.put(pc, callees)
            }
        }

        def allCallEdges: (Method, Map[PC, Set[Method]]) = (method, callEdgesMap)

        def addCallToNullPointerExceptionConstructor(callerMethod: Method, pc: PC): Unit = {

            cache.NullPointerExceptionDefaultConstructor match {
                case Some(defaultConstructor) ⇒ addCallEdge(pc, HashSet(defaultConstructor))
                case _ ⇒
                    val defaultConstructorDescriptor = MethodDescriptor.NoArgsAndReturnVoid
                    val NullPointerException = ObjectType.NullPointerException
                    addUnresolvedMethodCall(
                        callerMethod, pc,
                        NullPointerException, "<init>", defaultConstructorDescriptor
                    )
            }
        }
    }
}
object CallGraphExtractor {

    type LocalCallGraphInformation = (( /*Caller*/ Method, Map[PC, /*Callees*/ Set[Method]]), List[UnresolvedMethodCall])

}
