/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._
import scala.collection.mutable

import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.Domain

/**
 * ''Key'' to get the 3-address based code of a method computed using the configured
 * domain/data-flow analysis. This key performs the transformation eagerly for all methods. The
 * results of the underlying analysis are not cached.
 *
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and
 *          pass in `this` object.
 *
 * @author Michael Eichberg
 */
object EagerDetachedTACAIKey extends TACAIKey[Method => Domain with RecordDefUse] {

    /**
     * TACAI code has no special prerequisites.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Returns an factory which computes and caches the 3-address code of a method when required.
     *
     * All methods belonging to a project are converted using the same `domainFactory`. Hence,
     * the `domainFactory` needs to be set (using `setProjectInformationKeyInitializationData`)
     * before compute is called/this key is passed to a specific project.
     */
    override def compute(
        project: SomeProject
    ): Method => AITACode[TACMethodParameter, ValueInformation] = {
        val domainFactory = project.
            getProjectInformationKeyInitializationData(this).
            getOrElse((m: Method) => new DefaultDomainWithCFGAndDefUse(project, m))

        val taCodes =
            new ConcurrentLinkedQueue[(Method, AITACode[TACMethodParameter, ValueInformation])]()

        project.parForeachMethodWithBody() { mi =>
            val m = mi.method
            val domain = domainFactory(m)
            val aiResult = BaseAI(m, domain)
            val code = TACAI(project, m, aiResult)
            // well... the following cast safe is safe, because the underlying
            // data-structure is actually, conceptually immutable
            val taCode = code.asInstanceOf[AITACode[TACMethodParameter, ValueInformation]]
            taCode.detach()
            taCodes.add((m, taCode))
        }

        mutable.Map.empty[Method, AITACode[TACMethodParameter, ValueInformation]] ++
            taCodes.asScala
    }
}
