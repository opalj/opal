/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports calls to `show()`, `pack()` or `setVisible()` methods on
 * `javax/swing/` objects.
 *
 * These methods should only be called from inside the Swing background thread [1], since
 * they themselves can cause events to be delivered. If they were called from outside the
 * Swing thread, there could be dead locks etc.
 *
 * The proper way to call these methods is to implement a helper `java.lang.Runnable`
 * class that does it, and pass an instance of it to `java.awt.EventQueue.invokeLater()`.
 *
 * [1]: [[http://en.wikipedia.org/wiki/Event_dispatching_thread]]
 *
 * @author Ralf Mitschke
 * @author Peter Spieler
 */
class SwingMethodInvokedInSwingThread[Source] extends FindRealBugsAnalysis[Source] {

    override def description: String =
        "Reports calls to certain Swing methods made from outside of the Swing thread."

    /**
     * Runs this analysis on the given project. Reports the method calling swing functions
     * outside of swing thread.
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

        // Look for INVOKEVIRTUAL calls to show/pack/setVisible() methods on javax/swing/
        // objects from inside public static main() or methods containing "benchmark" in
        // their name.
        for {
            classFile <- project.allProjectClassFiles
            method @ MethodWithBody(body) <- classFile.methods
            if (method.isPublic &&
                method.isStatic &&
                method.name == "main") ||
                (classFile.thisType.fqn.toLowerCase.indexOf("benchmark") >= 0)
            (idx, INVOKEVIRTUAL(targetType, name, desc)) <- body.associateWithIndex
            if targetType.isObjectType &&
                targetType.asObjectType.fqn.startsWith("javax/swing/")
            if ((name, desc) match {
                case ("show" | "pack", MethodDescriptor.NoArgsAndReturnVoid) => true
                case ("setVisible", MethodDescriptor(IndexedSeq(BooleanType),
                    VoidType)) => true
                case _ => false
            })
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Error,
                classFile.thisType,
                method.descriptor,
                method.name,
                "Calls Swing methods while outside Swing thread"
            )
        }
    }
}
