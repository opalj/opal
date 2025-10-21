/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br.*
import br.analyses.*
import br.instructions.*

/**
 * This analysis reports code that calls `SomeCollectionClassObject.toArray(T[])` with
 * zero-length array argument, for example:
 * {{{
 * myList.toArray(new T[0])
 * }}}
 * This is bad because this `toArray()` call will never optimize for speed by re-using the
 * array passed in as argument for returning the result. Such code should do something
 * like this instead:
 * {{{
 * myList.toArray(new T[myList.size()])
 * }}}
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class InefficientToArray[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String = "Reports inefficient toArray(T[]) calls"

    private val objectArrayType = ArrayType(ClassType.Object)
    private val toArrayDescriptor = MethodDescriptor(
        IndexedSeq(objectArrayType),
        objectArrayType
    )

    /**
     * Checks whether a type inherits from java/util/Collection or is java/util/List.
     * @param classHierarchy class hierarchy to search in
     * @param checkedType type, that is checked if it's a collection or list
     * @return true, if checkedType is a collection or list, false otherwise
     */
    private def isCollectionType(
        classHierarchy: ClassHierarchy
    )(checkedType: ReferenceType): Boolean = {
        checkedType.isClassType &&
        (classHierarchy.isSubtypeOf(
            checkedType.asClassType,
            ClassType.Collection
        ).isNoOrUnknown || checkedType == ClassType.List)
        // TODO needs more heuristic or more analysis
    }

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def doAnalyze(
        project:       Project[Source],
        parameters:    Seq[String] = List.empty,
        isInterrupted: () => Boolean
    ): Iterable[LineAndColumnBasedReport[Source]] = {

        val classHierarchy: ClassHierarchy = project.classHierarchy
        val isCollectionType = this.isCollectionType(classHierarchy) _

        // In all method bodies, look for calls to "toArray()" with "new ...[0]" argument,
        // on objects derived from the Collection classes.
        for {
            classFile <- project.allProjectClassFiles
            method @ MethodWithBody(body) <- classFile.methods
            pc <- body.matchTriple {
                case (
                        ICONST_0,
                        _: ANEWARRAY,
                        VirtualMethodInvocationInstruction(targetType, "toArray", `toArrayDescriptor`)
                    ) =>
                    isCollectionType(targetType)
                case _ => false
            }
        } yield {
            LineAndColumnBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                method.descriptor,
                method.name,
                body.lineNumber(pc),
                None,
                "Calling x.toArray(new T[0]) is inefficient, should be " +
                    "x.toArray(new T[x.size()])"
            )
        }
    }
}
