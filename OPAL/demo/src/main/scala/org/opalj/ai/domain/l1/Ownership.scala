/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package domain
package l1

import scala.language.existentials
import java.net.URL
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.Iterable
import scala.collection.{ Set, Map }
import org.opalj.collection.mutable.Locals
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.domain
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.No
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

/**
 * Find methods that return an internal (private) array to callers of the class.
 *
 * @author Michael Eichberg
 */
object OwnershipAnalysis
        extends OneStepAnalysis[URL, BasicReport]
        with AnalysisExecutor {

    val analysis = this

    override def title: String =
        "basic ownership analysis for arrays"

    override def description: String =
        "a very basic ownership analysis for arrays"

    override def doAnalyze(
        theProject: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {
        import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

        val Private___Not_Static = (AccessFlagsMatcher.NOT_STATIC && ACC_PRIVATE)

        val classes = for {
            classFile ← theProject.classFiles.par
            classType = classFile.thisType
            arrayFields = classFile.fields.collect {
                case f @ Field(Private___Not_Static(), name, ArrayType(_)) ⇒ name
            }
            if arrayFields.nonEmpty
            publicAndProtectedMethods = classFile.methods.filter(m ⇒ m.body.isDefined && (m.isPublic || m.isProtected))
            ownershipViolatingMethods = publicAndProtectedMethods.collect {
                case method if {
                    var isOwner = true
                    val aiResult =
                        BaseAI(
                            classFile, method,
                            new DefaultDomain(
                                theProject, classFile, method
                            ) with RecordLastReturnedValues
                        )
                    import aiResult.domain
                    if (method.returnType.isReferenceType) {
                        isOwner =
                            domain.allReturnedValues.forall { kv ⇒
                                val (_ /*pc*/ , returnedValue) = kv

                                def checkOrigin(pc: PC): Boolean = {
                                    if (pc < 0 || pc >= method.body.get.instructions.size)
                                        return true;

                                    method.body.get.instructions(pc) match {
                                        // we don't want to give back a reference to the 
                                        // array of this value or another value
                                        // that has the same type as this value! 
                                        case GETFIELD(`classType`, name, _) ⇒
                                            !arrayFields.contains(name)
                                        case AALOAD ⇒
                                            // only needs to be handled if we also want
                                            // to enforce deep ownership relations
                                            true
                                        case GETFIELD(_, _, _) /* <= somebody else's array */ |
                                            GETSTATIC(_, _, _) |
                                            NEWARRAY(_) |
                                            ANEWARRAY(_) |
                                            MULTIANEWARRAY(_, _) ⇒
                                            true
                                        case invoke: MethodInvocationInstruction ⇒
                                            // here we need to call this analysis
                                            // again... we may call a private method that
                                            // returns the array... 
                                            true
                                    }
                                }

                                def checkValue(returnValue: domain.DomainSingleOriginReferenceValue): Boolean = {
                                    returnValue match {
                                        case domain.DomainArrayValue(av) ⇒
                                            checkOrigin(av.origin)
                                        case _ ⇒
                                            true
                                    }

                                }

                                returnedValue match {
                                    case domain.DomainSingleOriginReferenceValue(sorv) ⇒
                                        checkValue(sorv)
                                    case domain.DomainMultipleReferenceValues(morv) ⇒
                                        morv.values.forall(checkValue(_))
                                }
                            }
                    }
                    !isOwner
                } ⇒ method
            }
            if ownershipViolatingMethods.nonEmpty
        } yield {
            (
                classType.toJava,
                ownershipViolatingMethods.map(m ⇒ m.name + m.descriptor.toUMLNotation).mkString(", ")
            )
        }

        BasicReport(
            classes.toList.sortWith((v1, v2) ⇒ v1._1 < v2._1).mkString(
                "Class files with no ownership protection for arrays:\n\t",
                "\n\t",
                "\n"))

    }

}

