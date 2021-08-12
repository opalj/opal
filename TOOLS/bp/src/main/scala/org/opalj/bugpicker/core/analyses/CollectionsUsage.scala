/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.TheCode
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.fpcf.PropertyStore
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.instructions.NEW
import org.opalj.issues.Issue
import org.opalj.issues.InstructionLocation
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.Relevance

/**
 * Identifies cases where the collections API is not used as intended.
 *
 * @author Michael Eichberg
 */
object CollectionsUsage {

    final val Collection = ObjectType("java/util/Collection")
    final val UnmodifiableCollectionMethodDescriptor = MethodDescriptor(Collection, Collection)
    final val Collections = ObjectType("java/util/Collections")

    def apply(
        theProject:    SomeProject,
        propertyStore: PropertyStore,
        callGraph:     CallGraph,
        method:        Method,
        result:        AIResult { val domain: Domain with TheCode with RecordDefUse }
    ): Seq[Issue] = {

        if (method.isSynthetic)
            return Nil;
        //
        //
        // IDENTIFYING RAW ISSUES
        //
        //
        var issues = List.empty[Issue]
        val domain = result.domain
        val code = domain.code
        val instructions = code.instructions
        code iterate { (pc, instruction) =>
            instruction match {

                case INVOKESTATIC(
                    Collections,
                    false,
                    "unmodifiableCollection",
                    UnmodifiableCollectionMethodDescriptor
                    ) =>
                    val origins = domain.operandOrigin(pc, 0)
                    if ((origins ne null) && // the instruction is not dead
                        origins.size == 1 &&
                        origins.head >= 0 && // the origin is not a parameter
                        instructions(origins.head).opcode == NEW.opcode) {
                        // FIXME Add check if something is done in a loop
                        // there is just one path on which the value is initialized
                        val usages = domain.usedBy(origins.head)
                        if (usages.size == 2) {
                            // one for the call of the initializer and for the call to Coll...
                            instructions(usages.withFilter(_ != pc).head) match {

                                // TODO Support the matching of other constructors... (e.g., which take a size hint)
                                case INVOKESPECIAL(_, false, _, MethodDescriptor.NoArgsAndReturnVoid) =>
                                    issues ::= Issue(
                                        "CollectionsUsage",
                                        Relevance.DefaultRelevance,
                                        "useless creation of standard collection class",
                                        Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                                        Set(IssueKind.JavaCollectionAPIMisusage),
                                        List(
                                            new InstructionLocation(
                                                Some("directly use Collections.emptyList/Collections.emptySet"),
                                                theProject, method, pc
                                            ),
                                            new InstructionLocation(
                                                Some("useless"),
                                                theProject, method, origins.head
                                            )

                                        )
                                    )

                                case _ => // other constructors are ignored
                            }

                        } else if (usages.size == 3) {
                            var foundConstructorCall = false
                            var foundAddCall = false
                            val previousUsages = usages.withFilter(_ != pc)
                            previousUsages.foreach { pc =>
                                instructions(pc) match {

                                    // TODO Support the matching of other constructors... (e.g., which take a size hint)
                                    case INVOKESPECIAL(_, false, _, MethodDescriptor.NoArgsAndReturnVoid) =>
                                        foundConstructorCall = true

                                    // TODO Support the case of a call to addElement
                                    case INVOKEVIRTUAL(_, "add", MethodDescriptor(IndexedSeq(ObjectType.Object), _)) |
                                        INVOKEINTERFACE(_, "add", MethodDescriptor(IndexedSeq(ObjectType.Object), _)) =>
                                        // is it the receiver or the parameter (in relation to a different collection?
                                        if (domain.operandOrigin(pc, 1) == origins) {
                                            foundAddCall = true
                                        }

                                    case i => // other calls are ignored
                                        println("let's see"+i)
                                }
                            }
                            if (foundAddCall && foundConstructorCall) {
                                issues ::= Issue(
                                    "CollectionsUsage",
                                    Relevance.DefaultRelevance,
                                    "useless creation of standard collection class",
                                    Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                                    Set(IssueKind.JavaCollectionAPIMisusage),
                                    List(
                                        new InstructionLocation(
                                            Some("directly use Collections.singletonList/Collections.singletonSet"),
                                            theProject, method, pc
                                        ),
                                        new InstructionLocation(
                                            Some("useless"),
                                            theProject, method, origins.head
                                        )
                                    )
                                )
                            }
                        }
                    }
                case _ => // don't care
            }
        }

        issues
    }
}
