/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpcf
package analysis

import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.log.OPALLogger
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodDescriptor._
import org.opalj.br.BooleanType
import org.opalj.br.VoidType
import org.opalj.br.FieldType
import org.opalj.br.LongType
import org.opalj.br.IntegerType

/**
 * This property expresses the leakage of methods to the client such that
 * the method can be called by a client. A method does only leak if it gets accessible
 * though inheritance where a immediate non-abstract subclass inherits the target method.
 */
sealed trait LibraryLeakage extends Property {
    final def key = LibraryLeakage.Key
}

object LibraryLeakage {
    final val Key = PropertyKey.create("Leakage", Leakage)
}

// Leakage => CallableFromClassesInOtherPackages
case object Leakage extends LibraryLeakage { final val isRefineable = false }

case object PotentialLeakage extends LibraryLeakage { final val isRefineable = true }

case object NoLeakage extends LibraryLeakage { final val isRefineable = false }

/**
 * This Analysis determines the ´LibraryLeakage´ property of a method. A method is considered as leaked
 * if it is overridden in every immediate non-abstract subclass.
 *
 * In the following scenario, m defined by B overrides m in C and (in this specific scenario) m in C is
 * also always overridden.
 * {{{
 * 		/*package visible*/ class C { public Object m() }
 * 		/*package visible*/ abstract class A extends C { /*empty*/ }
 * 		public class B extends A { public Object m() }
 * }}}
 *
 *  @author Michael Reif
 */
class LibraryLeakageAnalysis private (
    project: SomeProject
)
        extends DefaultFPCFAnalysis[Method](
            project,
            entitySelector = LibraryLeakageAnalysis.entitySelector
        ) {

    /**
     * Determines the [[LibraryLeakage]] property of non-static methods. It is tailored to entry point
     *  set computation where we have to consider different kind of program/library usage scenarios.
     * Computational differences regarding static methods are :
     *  - private methods can be handled equal in every context
     *  - if OPA is met, all package visible classes are visible which implies that all non-private methods are
     *    visible too
     *  - if CPA is met, methods in package visible classes are not visible by default.
     *
     */
    def determineProperty(method: Method): PropertyComputationResult = {

        if (method.isPrivate)
            /* private methods are only visible in the scope of the class */
            return ImmediateResult(method, NoLeakage);

        val classFile = project.classFile(method)
        if (classFile.isFinal)
            return ImmediateResult(method, NoLeakage);

        if (isOpenLibrary)
            return ImmediateResult(method, Leakage);

        //we are now either analyzing a library under CPA or an application.
        if (method.isPackagePrivate || method.isConstructor)
            /* a package private method can not leak to the client under CPA */
            return ImmediateResult(method, NoLeakage);

        // When we reach this point:
        // - the method is public or protected
        // - the class is not final
        if (classFile.isPublic)
            return ImmediateResult(method, Leakage);

        val classHierarchy = project.classHierarchy

        val classType = classFile.thisType
        val methodName = method.name
        val methodDescriptor = method.descriptor

        classHierarchy.foreachSubtype(classType) { subtype ⇒
            project.classFile(subtype) match {
                case Some(subclass) ⇒
                    if (subclass.isPublic &&
                        method.isPackagePrivate &&
                        subclass.findMethod(methodName, methodDescriptor).isEmpty)
                        // the original method is visible and can be called by clients
                        return ImmediateResult(method, Leakage);
                case None ⇒
                    return ImmediateResult(method, Leakage);
            }
        }

        //Now: A method does not leak through a subclass, but we also have to check the superclasses

        classHierarchy.foreachSupertype(classType) { supertype ⇒
            project.classFile(supertype) match {
                case Some(superclass) ⇒ {
                    val declMethod = superclass.findMethod(methodName, methodDescriptor)
                    if (declMethod.isDefined) {
                        val m = declMethod.get
                        if ((m.isPublic || m.isProtected) && superclass.isPublic)
                            return ImmediateResult(method, Leakage);
                    }
                }
                case None if supertype eq ObjectType.Object ⇒
                    (methodName, methodDescriptor) match {
                        case ("toString", JustReturnsString) |
                            ("hashCode", JustReturnsInteger) |
                            ("equals", MethodDescriptor(IndexedSeq(ObjectType.Object), BooleanType)) |
                            ("clone", JustReturnsObject) |
                            ("getClass", _) |
                            ("finalize", NoArgsAndReturnVoid) |
                            ("notify", NoArgsAndReturnVoid) |
                            ("notifyAll", NoArgsAndReturnVoid) |
                            ("wait", NoArgsAndReturnVoid) |
                            ("wait", MethodDescriptor(IndexedSeq(LongType), VoidType)) |
                            ("wait", MethodDescriptor(IndexedSeq(LongType, IntegerType), VoidType)) ⇒
                            return ImmediateResult(method, Leakage);
                        case _ ⇒ /* nothing leaks */
                    }
                case _ ⇒
                    return ImmediateResult(method, Leakage);
            }
        }

        ImmediateResult(method, NoLeakage)
    }
}

object LibraryLeakageAnalysis extends FPCFAnalysisRunner[LibraryLeakageAnalysis] {
    private[LibraryLeakageAnalysis] def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isStatic && !m.isAbstract ⇒ m
    }

    private[LibraryLeakageAnalysis] def apply(
        project: SomeProject
    ): LibraryLeakageAnalysis = {
        new LibraryLeakageAnalysis(project)
    }

    protected def start(project: SomeProject): Unit = {
        LibraryLeakageAnalysis(project)
    }
}