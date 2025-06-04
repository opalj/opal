/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk

import org.opalj.apk.parser.ApkParser
import org.opalj.si.ProjectInformationKey
import org.opalj.si.Project

/**
 * Key for a list of [[ApkComponent]] to link them to a [[org.opalj.br.analyses.Project]].
 *
 * @author Nicolas Gross
 */
object ApkComponentsKey extends ProjectInformationKey[Project, Seq[ApkComponent], ApkParser] {

    /**
     * The computation of the APK's components / entry points has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: Project): Seq[ProjectInformationKey[Project, Nothing, Nothing]] = Nil

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
    override def compute(project: Project): Seq[ApkComponent] = {
        project.getOrCreateProjectInformationKeyInitializationData(
            this,
            new ApkParser("")(project.config)
        )
            .parseComponents
    }
}
