/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.issues.Issue
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodDescriptor
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.Relevance
import org.opalj.issues.InstructionLocation

/**
 * This analysis reports calls to `java.lang.System/Runtime.gc()` that seem to be made
 * manually in code outside the core of the JRE.
 *
 * Manual invocations of garbage collection are usually unnecessary and can lead to
 * performance problems. This heuristic tries to detect such cases.
 *
 * @author Ralf Mitschke
 * @author Peter Spieler
 * @author Michael Eichberg
 */
object ManualGarbageCollection {

    final val Runtime = ObjectType("java/lang/Runtime")

    def description: String =
        "Reports methods outside of java.lang that explicitly invoke the garbage collector."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def apply(theProject: SomeProject, classFile: ClassFile): Iterable[Issue] = {

        import MethodDescriptor.NoArgsAndReturnVoid

        if (classFile.thisType.fqn.startsWith("java/lang"))
            return Seq.empty;

        for {
            method @ MethodWithBody(body) <- classFile.methods
            (pc, gcCall) <- body.collectWithIndex {
                case (pc, INVOKESTATIC(ObjectType.System, false, "gc", NoArgsAndReturnVoid)) =>
                    (pc, "System.gc()")
                case (pc, INVOKEVIRTUAL(Runtime, "gc", NoArgsAndReturnVoid)) =>
                    (pc, "Runtime.gc()")
            }
        } yield Issue(
            "ManualGarbageCollection",
            Relevance.Low,
            s"contains dubious call to $gcCall",
            Set(IssueCategory.Performance),
            Set(IssueKind.DubiousMethodCall),
            List(new InstructionLocation(None, theProject, method, pc))
        )
    }
}
