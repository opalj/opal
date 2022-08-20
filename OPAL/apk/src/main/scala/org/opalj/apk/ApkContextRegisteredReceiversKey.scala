/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

import org.opalj.apk.analyses.ContextRegisteredReceiversAnalysis
import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}

/**
 * Key for a list of [[ApkContextRegisteredReceiver]] to link them to a [[org.opalj.br.analyses.Project]].
 *
 * @author Nicolas Gross
 */
object ApkContextRegisteredReceiversKey extends ProjectInformationKey[Seq[ApkContextRegisteredReceiver], SomeProject] {

    /**
     * The computation of context-registered Broadcast Receivers has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the APK's context-registered Broadcast Receivers.
     *
     * @return a Seq of [[ApkContextRegisteredReceiver]].
     */
    override def compute(project: SomeProject): Seq[ApkContextRegisteredReceiver] = {
        ContextRegisteredReceiversAnalysis.analyze(project)
    }
}
