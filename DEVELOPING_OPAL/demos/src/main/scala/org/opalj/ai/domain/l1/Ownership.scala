/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.net.URL
import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.AccessFlagsMatcher
import org.opalj.br.ArrayType
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.analyses.ProjectAnalysisApplication

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Find methods that return an internal (private) array to callers of the class.
 *
 * @author Michael Eichberg
 */
object OwnershipAnalysis extends ProjectAnalysisApplication {

    override def title: String =
        "basic ownership analysis for arrays"

    override def description: String =
        "a very basic ownership analysis for arrays"

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val Private___Not_Static = (AccessFlagsMatcher.NOT_STATIC && ACC_PRIVATE)

        val classes = for {
            classFile <- theProject.allProjectClassFiles.par
            classType = classFile.thisType
            arrayFields = classFile.fields.collect {
                case Field(Private___Not_Static(), name, ArrayType(_)) => name
            }
            if arrayFields.nonEmpty
            publicAndProtectedMethods = classFile.methods.filter(m => m.body.isDefined && (m.isPublic || m.isProtected))
            ownershipViolatingMethods = publicAndProtectedMethods.collect {
                case method if {
                    var isOwner = true
                    val aiResult =
                        BaseAI(
                            method,
                            new DefaultDomain(theProject, method) with RecordLastReturnedValues
                        )
                    import aiResult.domain
                    if (method.returnType.isReferenceType) {
                        isOwner =
                            domain.allReturnedValues.forall { kv =>
                                val (_ /*pc*/ , returnedValue) = kv

                                def checkOrigin(pc: Int): Boolean = {
                                    if (pc < 0 || pc >= method.body.get.instructions.length)
                                        return true;

                                    method.body.get.instructions(pc) match {
                                        // we don't want to give back a reference to the
                                        // array of this value or another value
                                        // that has the same type as this value!
                                        case GETFIELD(`classType`, name, _) =>
                                            !arrayFields.contains(name)
                                        case AALOAD =>
                                            // only needs to be handled if we also want
                                            // to enforce deep ownership relations
                                            true
                                        case GETFIELD(_, _, _) /* <= somebody else's array */ |
                                            GETSTATIC(_, _, _) |
                                            NEWARRAY(_) |
                                            ANEWARRAY(_) |
                                            MULTIANEWARRAY(_, _) =>
                                            true
                                        case _ /*invoke*/ : MethodInvocationInstruction =>
                                            // here we need to call this analysis
                                            // again... we may call a private method that
                                            // returns the array...
                                            true
                                    }
                                }

                                def checkValue(returnValue: domain.DomainSingleOriginReferenceValue): Boolean = {
                                    returnValue match {
                                        case domain.DomainArrayValueTag(av) =>
                                            checkOrigin(av.origin)
                                        case _ =>
                                            true
                                    }

                                }

                                returnedValue match {
                                    case domain.DomainSingleOriginReferenceValueTag(sorv) =>
                                        checkValue(sorv)
                                    case domain.DomainMultipleReferenceValuesTag(morv) =>
                                        morv.values.forall(checkValue(_))
                                }
                            }
                    }
                    !isOwner
                } => method
            }
            if ownershipViolatingMethods.nonEmpty
        } yield {
            (
                classType.toJava,
                ownershipViolatingMethods.map(m => m.name + m.descriptor.toUMLNotation).mkString(", ")
            )
        }

        BasicReport(
            classes.toList.sortWith((v1, v2) => v1._1 < v2._1).mkString(
                "Class files with no ownership protection for arrays:\n\t", "\n\t", "\n"
            )
        )

    }

}
