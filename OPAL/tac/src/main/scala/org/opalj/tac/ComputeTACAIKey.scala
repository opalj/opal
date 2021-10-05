/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.Domain

/**
 * ''Key'' to compute the 3-address based code of a method computed using the configured
 * domain/data-flow analysis. The result is __not cached__ and the 3-address code is still linked
 * to the results of the underlying data-flow analysis. In general, this key is only appropriate
 * if you want to transform every method at most once.
 *
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and
 *          pass in `this` object.
 *
 * @author Michael Eichberg
 */
object ComputeTACAIKey extends TACAIKey[Method => Domain with RecordDefUse] {

    /**
     * TACAI code has no special prerequisites.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Returns an factory which computes the 3-address code of a method anew when called.
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

        (m: Method) => {
            val domain = domainFactory(m)
            val aiResult = BaseAI(m, domain)
            val code = TACAI(project, m, aiResult)
            code.asInstanceOf[AITACode[TACMethodParameter, ValueInformation]]
        }
    }
}
