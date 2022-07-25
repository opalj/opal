package org.opalj
package apk

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

/**
 * Key for a list of APKEntry to link them to a [[org.opalj.br.analyses.Project]].
 * 
 * @author Nicolas Gross
 */
object APKEntriesKey extends ProjectInformationKey[Seq[APKEntryPoint], APKParser] {

  /**
   * The computation of the APK's entry points has no special prerequisites.
   *
   * @return `Nil`.
   */
  override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

  /**
   * Computes the APK's entry points.
   *
   * @return a Seq of [[APKEntryPoint]].
   */
  override def compute(project: SomeProject): Seq[APKEntryPoint] = {
    project.getOrCreateProjectInformationKeyInitializationData(this, new APKParser("")).parseEntryPoints
  }

}
