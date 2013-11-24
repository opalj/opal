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

import collection.Set
import collection.Map

/**
 * Creates a call graph by analyzing each entry point on its own. The call
 * graph is calculated under a specific assumption about a programs/libraries/framework's
 * entry methods.
 *
 * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to
 * `java.lang.Object`.
 *
 * @author Michael Eichberg
 */
object CallGraphFactory {

    import language.existentials

    // internally, we use mutable data structures due to their better performance 
    // characteristics, but they are not exposed to the outside
    import collection.mutable.HashSet
    import collection.mutable.HashMap

    import bat.resolved.ai.domain._

    /**
     * The set of all entry points consists of:
     * - all static initializers,
     * - every non-private static method,
     * - every non-private constructor,
     * - every non-private method.
     */
    def defaultEntryPointsForCHA(project: Project[_]): List[Method] = {
        var entryPoints = List.empty[Method]
        project.foreachMethod { method: Method ⇒
            if (!method.isPrivate && method.body.isDefined)
                entryPoints = method :: entryPoints
        }
        entryPoints
    }

    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method]): (CHACallGraph[Source], Seq[UnresolvedMethodCall], Seq[CallGraphConstructionException]) = {

        var exceptions = List.empty[CallGraphConstructionException]
        def handleException(classFile: ClassFile, method: Method, exception: Exception) {
            exceptions =
                CallGraphConstructionException(classFile, method, exception) ::
                    exceptions
        }

        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        def handleUnresolvedMethodCall(
            callerClass: ReferenceType,
            caller: Method,
            pc: PC,
            calleeClass: ReferenceType,
            calleeName: String,
            calleeDescriptor: MethodDescriptor) {
            unresolvedMethodCalls =
                UnresolvedMethodCall(
                    callerClass, caller, pc,
                    calleeClass, calleeName, calleeDescriptor) :: unresolvedMethodCalls
        }

        (
            performCHA(theProject, entryPoints, handleUnresolvedMethodCall _, handleException _),
            unresolvedMethodCalls,
            exceptions
        )
    }

    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method],
        handleUnresolvedMethodCall: ( /*callerClass: */ ReferenceType, /*caller:*/ Method, /*pc:*/ PC, /*calleeClass:*/ ReferenceType, /*calleeName:*/ String, /*calleeDescriptor: */ MethodDescriptor) ⇒ _,
        handleException: (ClassFile, Method, Exception) ⇒ _): CHACallGraph[Source] = {

        type DomainContext = Int

        var methodsToAnalyze = entryPoints
        val methodAnalyzed = new Array[Boolean](Method.methodsCount)

        val calledBy =
            new HashMap[Method, HashMap[Method, Set[PC]]] {
                override def initialSize = Method.methodsCount
            }
        val calls =
            new HashMap[Method, HashMap[PC, Iterable[Method]]] {
                override def initialSize = Method.methodsCount
            }

        //        val implementingMethodsCache =
        //            new HashMap[(ReferenceType, String, MethodDescriptor), Iterable[Method]] {
        //                override def initialSize = Method.methodsCount
        //            }
        val resolvedTargets = {
            // the index is the id of the ReferenceType of the receiver
            new Array[HashMap[MethodSignature, Iterable[Method]]](theProject.objectTypesCount)
        }

        // This domain does not have any associated state. 
        class MethodDomain(
            val callerClassFile: ClassFile,
            val caller: Method)
                extends Domain[DomainContext]
                with DefaultDomainValueBinding[DomainContext]
                with DefaultTypeLevelIntegerValues[DomainContext]
                with DefaultTypeLevelLongValues[DomainContext]
                with DefaultTypeLevelFloatValues[DomainContext]
                with DefaultTypeLevelDoubleValues[DomainContext]
                with DefaultTypeLevelReferenceValues[DomainContext]
                //with DefaultPreciseReferenceValues[DomainContext]
                //with StringValues[DomainContext]
                with TypeLevelArrayInstructions
                with TypeLevelFieldAccessInstructions
                with TypeLevelInvokeInstructions
                with DoNothingOnReturnFromMethod
                with ProjectBasedClassHierarchy[Source] {

            def identifier = caller.id

            val project: Project[Source] = theProject

            @inline private[this] def addCallEdge(
                caller: Method,
                pc: PC,
                callees: Iterable[Method]): Unit = {
                // calledBy: Map[Method, Map[Method, Set[PC]]]
                for (callee ← callees) {
                    val callers = calledBy.getOrElseUpdate(callee, HashMap.empty)
                    callers.get(caller) match {
                        case Some(pcs) ⇒
                            val newPCs = pcs + pc
                            if (pcs ne newPCs)
                                callers.update(caller, newPCs)
                        case None ⇒
                            val newPCs = collection.immutable.Set.empty + pc
                            callers.update(caller, newPCs)
                    }
                    if (!callee.isNative) {
                        methodsToAnalyze = callee :: methodsToAnalyze
                    }
                }

                // calls : Map[Method, Map[PC, Iterable[Method]]]          
                val callSites = calls.getOrElseUpdate(caller, HashMap.empty)
                callSites.update(pc, callees)
            }

            @inline private[this] def dynamicallyBoundInvocation(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor) {
                // PLAIN CHA - we do not consider any data-flow information 
                // that is available
                //                val callerSignature = (declaringClass, name, descriptor)
                //                val callees =
                //                    implementingMethodsCache.getOrElseUpdate(
                //                        callerSignature,
                //                        classHierarchy.lookupImplementingMethods(
                //                            declaringClass, name, descriptor, project
                //                        )
                //                    )

                val callees = {
                    //resolvedTargets : Array[Map[MethodSignature, Iterable[Method]]]
                    val resolvedTargetsForClass = resolvedTargets(declaringClass.id)
                    val callerSignature = new MethodSignature(name, descriptor)
                    //                    if (resolvedTargetsForClass eq null) {
                    //                        val targets =
                    //                            classHierarchy.lookupImplementingMethods(
                    //                                declaringClass, name, descriptor, project
                    //                            )
                    //                        resolvedTargets(declaringClass.id) =
                    //                            new collection.immutable.Map.Map1(callerSignature, targets)
                    //                        targets
                    //                    } else {
                    //                        resolvedTargetsForClass.get(callerSignature) match {
                    //                            case Some(targets) ⇒
                    //                                targets
                    //                            case None ⇒
                    //                                val targets = classHierarchy.lookupImplementingMethods(
                    //                                    declaringClass, name, descriptor, project
                    //                                )
                    //                                resolvedTargets(declaringClass.id) =
                    //                                    resolvedTargetsForClass + ((callerSignature, targets))
                    //                                targets
                    //                        }
                    //                    }
                    if (resolvedTargetsForClass eq null) {
                        val targets =
                            classHierarchy.lookupImplementingMethods(
                                declaringClass, name, descriptor, project
                            )
                        resolvedTargets(declaringClass.id) =
                            HashMap((callerSignature, targets))
                        targets
                    } else {
                        resolvedTargetsForClass.getOrElseUpdate(
                            callerSignature,
                            classHierarchy.lookupImplementingMethods(
                                declaringClass, name, descriptor, project))
                    }
                }
                //                implementingMethodsCache.getOrElseUpdate(
                //                    callerSignature,
                //                    classHierarchy.lookupImplementingMethods(
                //                        declaringClass, name, descriptor, project
                //                    )
                //                )

                if (callees.isEmpty)
                    handleUnresolvedMethodCall(
                        callerClassFile.thisClass, caller, pc,
                        declaringClass, name, descriptor)
                else
                    addCallEdge(caller, pc, callees)
            }

            override def invokevirtual(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {

                if (declaringClass.isArrayType) {
                    staticallyBoundInvocation(pc, ObjectType.Object, name, descriptor)
                } else {
                    dynamicallyBoundInvocation(pc, declaringClass.asObjectType, name, descriptor)
                }
                super.invokevirtual(pc, declaringClass, name, descriptor, operands)
            }

            override def invokeinterface(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                dynamicallyBoundInvocation(pc, declaringClass.asObjectType, name, descriptor)
                super.invokeinterface(pc, declaringClass, name, descriptor, operands)
            }

            @inline private[this] def staticallyBoundInvocation(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor) {
                classHierarchy.lookupMethodDefinition(
                    declaringClass, name, descriptor, project
                ) match {
                        case Some(callee) ⇒
                            addCallEdge(caller, pc, Iterable(callee))
                        case None ⇒
                            handleUnresolvedMethodCall(
                                callerClassFile.thisClass, caller, pc,
                                declaringClass, name, descriptor
                            )
                    }
            }

            /**
             * Invocation of private, constructor and super methods.
             */
            override def invokespecial(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                // for invokespecial the dynamic type is not "relevant" and the
                // first method that we find is the one that needs to be concrete 
                staticallyBoundInvocation(pc, declaringClass, name, descriptor)
                super.invokespecial(pc, declaringClass, name, descriptor, operands)
            }

            /**
             * Invocation of static methods.
             */
            override def invokestatic(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                staticallyBoundInvocation(pc, declaringClass, name, descriptor)
                super.invokestatic(pc, declaringClass, name, descriptor, operands)
            }
        }

        while (methodsToAnalyze.nonEmpty) {
            val method = methodsToAnalyze.head
            methodsToAnalyze = methodsToAnalyze.tail
            if (!methodAnalyzed(method.id)) {
                methodAnalyzed(method.id) = true
                val classFile = theProject.classFile(method)
                try {
                    BaseAI(classFile, method, new MethodDomain(classFile, method))
                } catch {
                    case e: Exception ⇒ handleException(classFile, method, e)
                }
            }
        }

        CHACallGraph(theProject, calledBy, calls)
    }
}
/**
 * Basic representation of a call graph.
 *
 * @author Michael Eichberg
 */
class CHACallGraph[Source] private (
        val project: Project[Source],
        val calledBy: Map[Method, Map[Method, Set[PC]]],
        val calls: Map[Method, Map[PC, Iterable[Method]]]) {

    /**
     * Statistics about the number of potential targets per call site.
     * (TSV format (tab-separated file) - can easily read by most spreadsheet
     * applications).
     */
    def callsStatistics: String = {
        var result: List[List[String]] = List.empty
        project foreachMethod { (method: Method) ⇒
            calls.get(method) foreach { callSites ⇒
                callSites foreach { callSite ⇒
                    val (pc, targets) = callSite
                    result = List(
                        method.id.toString,
                        "\""+method.toJava+"\"",
                        pc.toString,
                        targets.size.toString
                    ) :: result
                }
            }
        }
        result = List("\"Method ID\"", "\"Method Signature\"", "\"Callsite (PC)\"", "\"Targets\"") :: result
        result.map(_.mkString("\t")).mkString("\n")
    }

    /**
     * Statistics about the number of methods that potentially call a specific method.
     * (TSV format (tab-separated file) - can easily read by most spreadsheet
     * applications).
     */
    def calledByStatistics: String = {
        var result: List[List[String]] = List.empty
        project foreachMethod { (method: Method) ⇒
            calledBy.get(method) foreach { callingSites ⇒
                //val callingSitesCount = (0 /: callingSites.values) { _ + _.size }
                callingSites foreach { callingSite ⇒
                    val (callerMethod, callingInstructions) = callingSite
                    result =
                        List(
                            method.id.toString,
                            method.toJava,
                            callerMethod.id.toString,
                            callerMethod.toJava,
                            callingInstructions.size.toString
                        ) :: result
                }
            }
        }
        result = List("Method ID", "Method Signature", "Calling Method ID", "Calling Method", "Calling Sites") :: result
        result.map(_.mkString("\t")).mkString("\n")
    }
}
/**
 * Factory method to create a CHACallGraph object.
 */
object CHACallGraph {

    def apply[Source](
        project: Project[Source],
        calledBy: Map[Method, Map[Method, Set[PC]]],
        calls: Map[Method, Map[PC, Iterable[Method]]]) =
        new CHACallGraph(project, calledBy, calls)
}

/**
 * Represents a method signature.
 *
 *  @author Michael Eichberg
 */
final class MethodSignature(
        val name: String,
        val descriptor: MethodDescriptor) {

    override def equals(other: Any): Boolean = {
        other match {
            case that: MethodSignature ⇒
                name == that.name && descriptor == that.descriptor
            case _ ⇒ false
        }
    }
    override def hashCode: Int = name.hashCode * 13 + descriptor.hashCode
}

/**
 * Represents a method call that could not be resolved. This information is primarily
 * interesting during the development of static analyses.
 *
 * @author Michael Eichberg
 */
case class UnresolvedMethodCall(
        callerClass: ReferenceType,
        caller: Method,
        pc: PC,
        calleeClass: ReferenceType,
        calleeName: String,
        calleeDescriptor: MethodDescriptor) {

    import Console._

    override def toString: String = {
        callerClass.toJava+"{ "+
            BOLD + caller.toJava + RESET+":"+pc+" } => "+
            calleeClass.toJava+"{ "+
            BOLD + calleeDescriptor.toJava(calleeName) + RESET+
            " }"
    }
}

/**
 * Encapsulates an exception that is thrown during the creation of the call graph.
 *
 * @author Michael Eichberg
 */
case class CallGraphConstructionException(
        classFile: ClassFile,
        method: Method,
        underlyingException: Exception) {

    import Console._

    override def toString: String = {
        classFile.thisClass.toJava+"{ "+
            method.toJava+" ⚡ "+
            RED +
            underlyingException.getClass().getSimpleName()+": "+
            underlyingException.getMessage() +
            RESET+
            " }"
    }
}




/*
Things that complicate matters for more complex call graph analyses:
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
class B extends A {
    private foo() {
        bar()
    }

    public bar() {
        // do nothing
    }
}
*/ 

 


