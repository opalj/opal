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
import de.tud.cs.st.bat.resolved._
import de.tud.cs.st.bat.resolved.instructions._
import de.tud.cs.st.bat.resolved.reader.Java8Framework
import de.tud.cs.st.util.debug.PerformanceEvaluation._

import scala.collection.parallel.ParSeq

object SlidingCollect {

    val project = Java8Framework.ClassFiles(new java.io.File("/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home/jre/lib"))

    def pcsBeforePullRequest =
        time(1, 3, 5, {
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                Seq((_, INVOKESPECIAL(receiver1, _, MethodDescriptor(Seq(paramType), _))),
                    (pc, INVOKEVIRTUAL(receiver2, name, MethodDescriptor(Seq(), returnType)))) ← body.associateWithIndex.sliding(2)
                if (!paramType.isReferenceType &&
                    receiver1.asObjectType.fqn.startsWith("java/lang") &&
                    receiver1 == receiver2 &&
                    name.endsWith("Value") &&
                    returnType != paramType // coercion to another type performed
                )
            } yield ((classFile.fqn, method.toJava, pc))
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsBeforePullRequest

    def pcsAfterPullRequest =
        time(1, 3, 5, {
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.slidingCollect(2)({
                    case (pc,
                        Seq(INVOKESPECIAL(receiver1, _, MethodDescriptor(Seq(paramType), _)),
                            INVOKEVIRTUAL(receiver2, name, MethodDescriptor(Seq(), returnType)))) if (
                        !paramType.isReferenceType &&
                        receiver1.asObjectType.fqn.startsWith("java/lang") &&
                        receiver1 == receiver2 &&
                        name.endsWith("Value") &&
                        returnType != paramType // coercion to another type performed
                    ) ⇒ pc
                })
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsAfterPullRequest

    def pcsWithNewMethodeDescriptorMatcher =
        time(1, 3, 5, {
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.slidingCollect(2) {
                    case (
                        pc,
                        Seq(INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType: BaseType, _))),
                            INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType)))) if (
                        (receiver1 eq receiver2) &&
                        (returnType ne paramType) && // coercion to another type performed
                        receiver1.asObjectType.fqn.startsWith("java/lang/") &&
                        name.endsWith("Value")
                    ) ⇒ pc
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodeDescriptorMatcher

    def pcsWithNewMethodeDescriptorMatcherAndSet =
        time(1, 3, 5, {
            val theTypes = scala.collection.mutable.HashSet(
                "java/lang/Boolean",
                "java/lang/Byte",
                "java/lang/Character",
                "java/lang/Short",
                "java/lang/Integer",
                "java/lang/Long",
                "java/lang/Float",
                "java/lang/Double")
            val theMethods = scala.collection.mutable.HashSet(
                "booleanValue",
                "byteValue",
                "charValue",
                "shortValue",
                "integerValue",
                "longValue",
                "floatValue",
                "doubleValue")
            for {
                classFile ← project.view.map(_._1).par
                method @ MethodWithBody(body) ← classFile.methods
                pc ← body.slidingCollect(2) {
                    case (
                        pc,
                        Seq(INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType, _))),
                            INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType)))) if (
                        paramType.isBaseType && // implicitly: returnType.isBaseType &&
                        (receiver1 eq receiver2) &&
                        (returnType ne paramType) && // coercion to another type performed
                        theTypes.contains(receiver1.fqn) &&
                        theMethods.contains(name)
                    ) ⇒ pc
                }
            } yield (classFile.fqn, method.toJava, pc)
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    pcsWithNewMethodeDescriptorMatcherAndSet
}