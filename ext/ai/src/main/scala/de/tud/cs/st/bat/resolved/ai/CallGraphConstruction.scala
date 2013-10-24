/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

import bat.resolved.analyses.{ Project, ReportableAnalysisResult }

/**
 * Creates a simple call graph by analyzing each method on its own.
 *
 * It ignores exceptions for now.
 *
 * @author Michael Eichberg
 */
trait CallGraphConstruction {
        
    protected def analyzeInParallel: Boolean = true

    def ai: AI[Domain[_]] = BaseAI

    def domain[Source](
        theProject: Project[Source],
        classFile: ClassFile,
        method: Method): Domain[_] = {

        import bat.resolved.ai.domain._
        class MethodDomain
                extends Domain[(ClassFile, Method)]
                with DefaultValueBinding[(ClassFile, Method)]
                with DefaultTypeLevelIntegerValues[(ClassFile, Method)]
                with DefaultTypeLevelLongValues[(ClassFile, Method)]
                with DefaultTypeLevelFloatValues[(ClassFile, Method)]
                with DefaultTypeLevelDoubleValues[(ClassFile, Method)]
                with DefaultReturnAddressValues[(ClassFile, Method)]
                with DefaultPreciseReferenceValues[(ClassFile, Method)]
                with StringValues[(ClassFile, Method)]
                with TypeLevelArrayInstructions
                with TypeLevelFieldAccessInstructions
                with TypeLevelInvokeInstructions
                with DoNothingOnReturnFromMethod
                with ProjectBasedClassHierarchy[Source] {

            def identifier = (classFile, method)

            def project: Project[Source] = theProject

            override def invokeinterface(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions =
                    
                    //project.classHierarchy.
                super.invokeinterface(pc, declaringClass, name, methodDescriptor, operands)

            override def invokevirtual(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions =
                super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)

            override def invokespecial(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions =
                super.invokespecial(pc, declaringClass, name, methodDescriptor, operands)

            override def invokestatic(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions =
                super.invokespecial(pc, declaringClass, name, methodDescriptor, operands)
        }

        new MethodDomain
    }

    def analyze(
        project: Project[_]) {

        var privateMethods = List.empty[Method]
        for {
            classFile ← project.classes.values.par
            method ← classFile.methods
        } {
            if (method.isPrivate) {
                privateMethods = method :: privateMethods
            } else {
                ai(classFile, method, domain(project, classFile, method))
            }
        }
    }

    private[this] var calledBy: Map[Method, Set[Method]] = Map.empty

}

case class CalledByGraph(
        val calledBy: Map[Method, Set[Method]]) {

}


