/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import bi.AccessFlagsMatcher

import br._
import br.analyses._

/**
 * This analysis reports outer classes that have (non-`static`) inner `Serializable`
 * classes without themselves being `Serializable`.
 *
 * This situation is problematic, because the serialization of the inner class would
 * require – due to the link to its outer class – always the serialization of the outer
 * class which will, however, fail.
 *
 * ==Implementation Note==
 * This analysis is implemented using the traditional approach where each analysis
 * analyzes the project's resources on its own and fully controls the process.
 *
 * @author Michael Eichberg
 */
class NonSerializableClassHasASerializableInnerClass[Source]
    extends FindRealBugsAnalysis[Source] {

    override def description: String =
        "Identifies (non-static) inner classes that are serializable, "+
            "but where the outer class is not."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def doAnalyze(
        project:       Project[Source],
        parameters:    Seq[String]     = List.empty,
        isInterrupted: () => Boolean
    ): Iterable[ClassBasedReport[Source]] = {

        import project.classHierarchy.isSubtypeOf

        val Serializable = ObjectType.Serializable

        // If it's unknown, it's neither possible nor necessary to collect subtypes
        if (project.classHierarchy.isUnknown(Serializable)) {
            return Iterable.empty
        }

        for {
            serializableType <- project.classHierarchy.allSubtypes(Serializable, false)
            classFile <- project.classFile(serializableType)
            if !project.isLibraryType(classFile)
            (outerType, AccessFlagsMatcher.NOT_STATIC()) <- classFile.outerType
            /* if we know nothing about the class, then we never generate a warning */
            if isSubtypeOf(outerType, Serializable).isNo
        } yield {
            ClassBasedReport(
                project.source(outerType),
                Severity.Error,
                outerType,
                "Has a serializable non-static inner class ("+serializableType.toJava+
                    "), but is not serializable itself"
            )
        }
    }
}
