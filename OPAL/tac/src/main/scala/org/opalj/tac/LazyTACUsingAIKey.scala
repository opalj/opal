/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.collection.concurrent.TrieMap

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.Domain
import org.opalj.ai.AIResult
import org.opalj.ai.common.SimpleAIKey
import org.opalj.value.ValueInformation

/**
 * ''Key'' to get the 3-address based code of a method computed using the result of the
 * data-flow analysis performed by `SimpleAIKey`.
 *
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and
 *          pass in `this` object.
 * @author Michael Eichberg
 */
object LazyTACUsingAIKey extends TACAIKey[Nothing] {

    /**
     * The TACAI code is created using the results of the abstract interpretation
     * of the underlying methods using the SimpleAIKey.
     */
    override def requirements(
        project: SomeProject
    ): Seq[ProjectInformationKey[Method => AIResult { val domain: Domain with RecordDefUse }, _ <: AnyRef]] = {
        Seq(SimpleAIKey)
    }

    /**
     * Returns an object which computes and caches the 3-address code of a method when required.
     *
     * All methods belonging to a project are converted using the same `domainFactory`. Hence,
     * the `domainFactory` needs to be set before compute is called/this key is passed to a
     * specific project. If multiple projects are instead concurrently, external synchronization
     * is necessary (e.g., on the ProjectInformationKey) to ensure that each project is
     * instantiated using the desired domain.
     */
    override def compute(
        project: SomeProject
    ): Method => AITACode[TACMethodParameter, ValueInformation] = {
        val aiResults = project.get(SimpleAIKey)
        val taCodes = TrieMap.empty[Method, AITACode[TACMethodParameter, ValueInformation]]

        (m: Method) => taCodes.getOrElseUpdate(m, {
            val aiResult = aiResults(m)
            val code = TACAI(project, m, aiResult)
            // well... the following cast is safe, because the underlying
            // data structure is actually (at least conceptually) immutable
            code.asInstanceOf[AITACode[TACMethodParameter, ValueInformation]]
        })

        /*
        def computeAndCacheTAC(m: Method) = { // never executed concurrently
            val aiResult = aiResults(m)
            val code = TACAI(m, project.classHierarchy, aiResult)(Nil)
            // well... the following cast is safe, because the underlying
            // data structure is actually (at least conceptually) immutable
            val taCode = code.asInstanceOf[AITACode[TACMethodParameter, DUVar[ValueInformation]]]
            taCodes.put(m, taCode)
            taCode
        }
         (m: Method) => taCodes.get(m) match {
            case Some(taCode) => taCode
            case None =>
                val brCode = m.body.get
                // Basically, we use double checked locking; we really don't want to
                // transform the code more than once!
                brCode.synchronized {
                    taCodes.get(m) match {
                        case Some(taCode) => taCode
                        case None         => computeAndCacheTAC(m)
                    }
                }
        }
        */
    }
}
