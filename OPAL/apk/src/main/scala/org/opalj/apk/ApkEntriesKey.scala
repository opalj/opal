/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

/**
 * Key for a list of ApkEntry to link them to a [[Project]].
 *
 * @author Nicolas Gross
 */
object ApkEntriesKey extends ProjectInformationKey[Seq[ApkEntryPoint], ApkParser] {

    /**
     * The computation of the APK's entry points has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the APK's entry points.
     *
     * @return a Seq of [[ApkEntryPoint]].
     */
    override def compute(project: SomeProject): Seq[ApkEntryPoint] = {
        project.getOrCreateProjectInformationKeyInitializationData(this, new ApkParser("")).parseEntryPoints
    }

}
