/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses

import org.opalj.log.OPALLogger
import org.opalj.br.ClassFile
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

/**
 * The ''key'' object to get information about the "more precise" field types.
 *
 * @author Michael Eichberg
 */
object FieldValuesKey extends ProjectInformationKey[FieldValueInformation, Nothing] {

    /**
     * The FieldTypesKey has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the field type information.
     */
    override protected def compute(project: SomeProject): FieldValueInformation = {
        implicit val logContext = project.logContext

        OPALLogger.info("progress", "computing field value information")

        val result = FieldValuesAnalysis.doAnalyze(
            project,
            (project: SomeProject, classFile: ClassFile) ⇒ {

                // TODO Make the analyses which computes the information configurable.
                new BaseFieldValuesAnalysisDomain(project, classFile)
            },
            () ⇒ false // make it configurable
        )

        OPALLogger.info(
            "progress",
            s"computed the field value information; refined the type of ${result.size} fields"
        )

        result
    }
}

