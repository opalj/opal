/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports invocations of `java.lang.reflect.Field|Method.setAccessible()`
 * outside of doPrivileged blocks.
 *
 * @author Ralf Mitschke
 * @author Roberts Kolosovs
 */
class DoInsideDoPrivileged[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String =
        "Detects calls to setAccessible() outside of doPrivileged blocks."

    private val ReflectFieldType =
        ObjectType("java/lang/reflect/Field")
    private val ReflectMethodType =
        ObjectType("java/lang/reflect/Method")
    private val PriviledgedActionType =
        ObjectType("java/security/PrivilegedAction")
    private val PriviledgedExceptionActionType =
        ObjectType("java/security/PrivilegedExceptionAction")

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
    ): Iterable[MethodBasedReport[Source]] = {

        // For all classes referencing neither privilegedAction nor
        // privilegedExceptionAction, look for methods that call setAccessible() on
        // java/lang/reflect/{Field|Method}.
        for {
            classFile <- project.allProjectClassFiles
            if !classFile.interfaceTypes.contains(PriviledgedActionType) &&
                !classFile.interfaceTypes.contains(PriviledgedExceptionActionType)
            method @ MethodWithBody(body) <- classFile.methods
            (_, INVOKEVIRTUAL(ReflectFieldType | ReflectMethodType,
                "setAccessible", _)) <- body.associateWithIndex
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                method,
                "Calls java.lang.reflect.Field|Method.setAccessible() outside of "+
                    "doPrivileged block"
            )
        }
    }
}
