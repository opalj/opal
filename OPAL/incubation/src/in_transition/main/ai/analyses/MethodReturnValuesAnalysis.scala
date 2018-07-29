/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._
import scala.collection.mutable.AnyRefMap
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.ai.Domain
import org.opalj.ai.domain._
import org.opalj.ai.InterruptableAI

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis {

    def title: String =
        "tries to derive more precise information about the values returned by methods"

    def description: String =
        "Identifies methods where we can – statically – derive more precise return type/value information."

    def doAnalyze(
        theProject:    SomeProject,
        isInterrupted: () ⇒ Boolean,
        createDomain:  (InterruptableAI[Domain], Method) ⇒ Domain with RecordReturnedValueInfrastructure
    ): MethodReturnValueInformation = {

        val results = new ConcurrentHashMap[Method, Option[Domain#DomainValue]]
        val candidates = new AtomicInteger(0)

        theProject.parForeachMethodWithBody(isInterrupted) { methodInfo ⇒
            val method = methodInfo.method
            val originalReturnType = method.returnType

            if (originalReturnType.isObjectType &&
                // ALTERNATIVE TEST: if we are only interested in type refinements but not
                // in refinements to "null" values then we can also use the following
                // check:
                // if theProject.classHierarchy.hasSubtypes(originalReturnType.asObjectType).isYesOrUnknown

                // We may still miss some refinements to "Null" but we don't care...
                theProject.classFile(originalReturnType.asObjectType).map(!_.isFinal).getOrElse(true)) {

                candidates.incrementAndGet()
                val ai = new InterruptableAI[Domain]
                val domain = createDomain(ai, method)
                ai(method, domain)
                if (!ai.isInterrupted) {
                    results.put(method, domain.returnedValue)
                }
            }
        }

        AnyRefMap.empty[Method, Option[Domain#DomainValue]] ++ results.asScala
    }

}
