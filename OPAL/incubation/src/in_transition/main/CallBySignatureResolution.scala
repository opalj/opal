/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import scala.collection.Set
import org.opalj.br.analyses._
import org.opalj.br.{Method, MethodDescriptor, ObjectType}
import org.opalj.fpcf.properties.CallBySignature
import org.opalj.fpcf.properties.NoCBSTargets
import org.opalj.fpcf.properties.CBSTargets

/**
 * An index that enables the efficient lookup of potential
 * call by signature resolution interface methods
 * given the method's name and the descriptor type.
 *
 * @note To get call by signature resolution information call
 *      [[org.opalj.br.analyses.Project]]'s method and pass in the
 *      [[CallBySignatureResolutionKey]] object.
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
            project.classFile(declClass).forall(_.isInterfaceDeclaration),
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

    def apply(project: SomeProject): CallBySignatureResolution = {
        new CallBySignatureResolution(project, project.get(PropertyStoreKey))
    }
}
