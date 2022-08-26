/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

import org.opalj.apk.parser.ApkParser
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

/**
 * Key for a list of [[ApkComponent]] to link them to a [[org.opalj.br.analyses.Project]].
 *
 * @author Nicolas Gross
 */
object ApkComponentsKey extends ProjectInformationKey[Seq[ApkComponent], ApkParser] {

    /**
     * The computation of the APK's components / entry points has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the APK's components / static entry points.
     * Context registered intents for Broadcast Receivers are missing, see [[analyses.ContextRegisteredReceiversAnalysis]].
     *
     * Expects an [[ApkParser]] to be already set as project key initialization data:
     * <pre>
     * project.updateProjectInformationKeyInitializationData(ApkComponentsKey)(
     *     current => apkParser
     * )
     * </pre>
     *
     * @return a Seq of [[ApkComponent]].
     */
    override def compute(project: SomeProject): Seq[ApkComponent] = {
        project.getOrCreateProjectInformationKeyInitializationData(this, new ApkParser("")).parseComponents
    }
}
