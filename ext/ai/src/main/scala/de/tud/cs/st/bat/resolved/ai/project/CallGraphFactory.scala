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
package project

import bat.resolved.analyses.{ Project, ReportableAnalysisResult }

/**
 * Creates a call graph by analyzing each entry point on its own. The call
 * graph is calculated under a specific assumption about a programs/libraries/framework's
 * entry methods.
 *
 * @author Michael Eichberg
 */
class CallGraphFactory {

    /**
     * The set of all entry points consists of:
     * - all static initializers,
     * - every non-private static method,
     * - every non-private constructor,
     * - every non-private method.
     */
    def defaultEntryPointsForCHA(project: Project[_]): Set[(ClassFile, Method)] = {
        for {
            classFile ← project.classFiles.toSet[ClassFile]
            method ← classFile.methods
            if !(method.isNative || method.isPrivate)
        } yield (classFile, method)
    }

    //    def defaultEntryPoints(project: Project[_]): Set[(ClassFile, Method)] = {
    //    	// We assume that we are analyzing a library. Hence, 
    //        // all static initializers are considered entry points,
    //        // every non-private static method is an entry point and
    //        // every non-private constructor is an entry point
    //        // Secondary entry points are all non-private, non-native 
    //        // instance methods.        
    //    }

    import language.existentials

    import bat.resolved.ai.domain._

    def performCHA[Source](
        theProject: Project[Source])(
            entryPoints: Set[(ClassFile, Method)] = defaultEntryPointsForCHA(theProject)): (CHACallGraph, Set[UnresolvedMethodCall]) = {

        type DomainContext = (ClassFile,Method)

        //        val fieldTypes: Map[Field, UpperBound] =
        //            Map.empty
        //        var writtenBy: Map[Field, Set[Method]] =
        //            Map.empty.withDefaultValue(Set.empty)
        //        var readBy: Map[Field, Set[Method]] =
        //            Map.empty.withDefaultValue(Set.empty)
        var calledBy: Map[Method, Map[Method, Set[PC]]] =
            Map.empty.withDefaultValue(Map.empty.withDefaultValue(Set.empty))
        var calls: Map[Method, Map[PC, Set[Method]]] =
            Map.empty.withDefaultValue(Map.empty.withDefaultValue(Set.empty))

        var analyzedMethods = Set.empty[Method]
        var methodsToAnalyze = entryPoints
        var unresolvedMethodCalls = Set.empty[UnresolvedMethodCall]
        def unresolvedMethodCall(
            declaringClass: ReferenceType,
            name: String,
            methodDescriptor: MethodDescriptor) {
            unresolvedMethodCalls += UnresolvedMethodCall(declaringClass, name, methodDescriptor)
        }

        // This domain does not have any associated state. 
        case class MethodDomain(
                val identifier : (ClassFile,Method)) 
        extends Domain[DomainContext]
                with DefaultDomainValueBinding[DomainContext]
                with DefaultTypeLevelIntegerValues[DomainContext]
                with DefaultTypeLevelLongValues[DomainContext]
                with DefaultTypeLevelFloatValues[DomainContext]
                with DefaultTypeLevelDoubleValues[DomainContext]
                with DefaultPreciseReferenceValues[DomainContext]
                with StringValues[DomainContext]
                with TypeLevelArrayInstructions
                with TypeLevelFieldAccessInstructions
                with TypeLevelInvokeInstructions
                with DoNothingOnReturnFromMethod
                with ProjectBasedClassHierarchy[Source] {

            def project: Project[Source] = theProject

            override def invokevirtual(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions =
                super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)

            override def invokeinterface(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                val parameters = operands.reverse
                val baseType = typeOfValue(parameters.head) match {
                    case IsReferenceValue(upperBound) ⇒ upperBound
                    case _                            ⇒ declaringClass
                }
                super.invokeinterface(pc, declaringClass, name, methodDescriptor, operands)
            }

            override def invokespecial(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                // invocation of private, constructor and super methods 
                classHierarchy.lookupMethodDefinition(
                    declaringClass.asObjectType, // the dynamic type is not "relevant"
                    name,
                    methodDescriptor,
                    project) match {
                        case Some(nextMethod @ (classFile, method)) ⇒
                            assume(method.isNative || method.body.isDefined,
                                "unexpected INVOKESPECIAL of an abstract method found")
                            
//                            calledBy: Map[Method, Map[Method, Set[PC]]] +=
//                                calledBy(nextMethod)()
//                                
//            Map.empty.withDefaultValue(Map.empty.withDefaultValue(Set.empty))
//        var calls: Map[Method, Map[PC, Set[Method]]] =
                                
                                if (!analyzedMethods.contains(method)) {
                                methodsToAnalyze += nextMethod
                            }
                        
                        case None ⇒
                            unresolvedMethodCall(
                                declaringClass,
                                name,
                                methodDescriptor
                            )
                    }
                super.invokespecial(pc, declaringClass, name, methodDescriptor, operands)
            }

            override def invokestatic(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                methodDescriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions =
                super.invokestatic(pc, declaringClass, name, methodDescriptor, operands)
        }

       

        while (methodsToAnalyze.nonEmpty) {
            val currentMethod @ (classFile, method) = methodsToAnalyze.head
            analyzedMethods += method
            methodsToAnalyze = methodsToAnalyze.tail
            BaseAI(classFile, method, MethodDomain((classFile,method)))
        }

        (new CHACallGraph(calledBy, calls), unresolvedMethodCalls)
    }

}
class CHACallGraph(
        val calledBy: Map[Method, Map[Method, Set[PC]]],
        val calls: Map[Method, Map[PC, Set[Method]]]) {
}

/**
 * @param pc the program counter of the instruction that is responsible for the call
 * 	of a method. In general, the "pc" refers to an invoke instruction. However,
 *   	a static initializer may also be called due the access of static field of a
 *    	class that was not previously loaded.
 */
class CallSite(
        val method: Method,
        val pc: PC) {

    override def equals(other: Any): Boolean =
        other match {
            case that: CallSite ⇒ this.pc == that.pc && this.method == that.method
            case _              ⇒ false
        }

    override def hashCode: Int = pc << 17 | method.id // collisions will happen, but are unlikely
}

object CallSite {
    def apply(method: Method, pc: PC): CallSite = {
        new CallSite(method, pc)
    }
}

case class CallGraph(
        val fieldTypes: Map[Field, UpperBound],
        val written: Map[Field, Set[Method]],
        val read: Map[Field, Set[Method]],
        val calledBy: Map[Method, Set[CallSite]],
        val calls: Map[Method, Map[PC, Set[Method]]]) {
}

case class UnresolvedMethodCall(
    val declaringClass: ReferenceType,
    val name: String,
    val descriptor: MethodDescriptor)



/*
Things that complicate matters:
class A {

    private A a = this;

    public m() {    
        a.foo() // here, a refers to an object of type B if bar was called before m()
        a.foo() // here, a "always" refers to an object of type B and not this!
    }

    private foo() {
        a = new B();
    }

    public bar() {
        a = new B();
    }
} 
*/ 

// For each entry point we start calculating the Call Graph. 

 


