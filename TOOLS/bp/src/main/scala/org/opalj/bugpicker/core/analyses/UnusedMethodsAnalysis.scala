/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.bi.VisibilityModifier
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.br.MethodDescriptor
import org.opalj.br.VoidType
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.IssueKind
import org.opalj.issues.IssueCategory
import org.opalj.issues.MethodLocation

/**
 * Identifies unused methods and constructors using the given call graph.
 *
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
object UnusedMethodsAnalysis {

    /**
     * Checks if the given method is used/is potentially useable. If the method is not used
     * and is also not potentially useable by future clients, then an issue is created
     * and returned.
     *
     * If any of the following conditions is true, the method is considered as being called.
     * - The method is the target of a method call in the calculated call graph.
     * - The method is a private (empty) default constructor in a final class. Such constructors
     *      are usually defined to avoid instantiations of the respective class.
     * - The method is a private constructor in a final class that always throws an exception.
     *      Such constructors are usually defined to avoid instantiations of the
     *      respective class. E.g.,
     *      `private XYZ(){throw new UnsupportedOperationException()`
     * - The method is "the finalize" method.
     */
    def apply(
        theProject:           SomeProject,
        callgraph:            ComputedCallGraph,
        callgraphEntryPoints: Set[Method],
        method:               Method
    ): Option[Issue] = {

        if (method.isSynthetic)
            return None;

        if (method.name == "finalize" && (method.descriptor eq MethodDescriptor.NoArgsAndReturnVoid))
            return None;

        if (callgraphEntryPoints.contains(method))
            return None;

        def rateMethod(): Relevance = {

            val classFile = method.classFile

            import method.{isConstructor, isPrivate, actualArgumentsCount, descriptor, name}
            import descriptor.{returnType, parametersCount => declaredParametersCount}

            //
            // Let's handle some technical artifacts related methods...
            //
            if (name == "valueOf" && classFile.isEnumDeclaration)
                return Relevance.Undetermined;

            //
            // Let's handle the standard methods...
            //
            if ((name == "equals" && descriptor == ObjectEqualsMethodDescriptor) ||
                (name == "hashCode" && descriptor == ObjectHashCodeMethodDescriptor)) {
                return Relevance.VeryLow;
            }

            //
            // Let's handle standard getter and setter methods...
            //
            if (name.length() > 3 &&
                ((name.startsWith("get") && returnType != VoidType && declaredParametersCount == 0) ||
                    (name.startsWith("set") && returnType == VoidType && declaredParametersCount == 1)) &&
                    {
                        val fieldNameCandidate = name.substring(3)
                        val fieldName = fieldNameCandidate.charAt(0).toLower + fieldNameCandidate.substring(1)
                        classFile.findField(fieldName).nonEmpty ||
                            classFile.findField('_' + fieldName).nonEmpty ||
                            classFile.findField('_' + fieldNameCandidate).nonEmpty
                    }) {
                return Relevance.VeryLow;
            }

            //
            // IN THE FOLLOWING WE DEAL WITH CONSTRUCTORS
            //

            // Let's check if it is a default constructor
            // which was defined to avoid instantiations of the
            // class (e.g., java.lang.Math)
            val isPrivateDefaultConstructor = isConstructor && isPrivate && actualArgumentsCount == 1 /*this*/
            if (!isPrivateDefaultConstructor)
                return Relevance.DefaultRelevance;

            val constructorsIterator = classFile.constructors
            constructorsIterator.next // <= we always have at least one constructor in bytecode
            if (constructorsIterator.hasNext)
                // we have (among others) a default constructor that is not used
                return Relevance.High;

            val body = method.body.get
            val instructions = body.instructions
            def justThrowsException: Boolean = {
                !body.exists { (pc, i) => /* <= it just throws exceptions */
                    i.isReturnInstruction
                }
            }
            if (instructions.size == 5 /* <= default empty constructor */ )
                Relevance.TechnicalArtifact
            else if (justThrowsException)
                Relevance.CommonIdiom
            else
                Relevance.DefaultRelevance
        }

        //
        //
        // THE ANALYSIS
        //
        //

        def unusedMethodOrConstructor: String = {
            def access(flags: Int): String =
                VisibilityModifier.get(flags) match {
                    case Some(visiblity) => visiblity.javaName.get
                    case _               => "/*default*/"
                }

            val isConstructor = method.isConstructor
            val accessFlags = access(method.accessFlags)
            s"the $accessFlags ${if (isConstructor) "constructor" else "method"} is not used"
        }

        val callers = callgraph.callGraph calledBy method

        if (callers.isEmpty) {
            val relevance: Relevance = rateMethod()
            if (relevance != Relevance.Undetermined) {
                val issue = Issue(
                    "UnusedMethodsAnalysis",
                    relevance,
                    unusedMethodOrConstructor,
                    Set(IssueCategory.Comprehensibility),
                    Set(IssueKind.UnusedMethod),
                    List(new MethodLocation(None, theProject, method))
                )
                return Some(issue);
            }
        }

        None
    }

}
