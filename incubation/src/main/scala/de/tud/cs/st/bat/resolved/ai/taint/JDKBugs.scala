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

import de.tud.cs.st.bat.resolved.ClassFile
import de.tud.cs.st.bat.resolved.Method

import de.tud.cs.st.bat.resolved.ClassFile
import de.tud.cs.st.bat.resolved.Method
package object taint {

    type CallStackEntry = (ClassFile, Method)

    /**
     * Set of ids (integer values) associated with the relevant parameters passed
     * to a method.
     */
    type RelevantParameters = Seq[Int]

    // Initialized (exactly once) by the "analyze" method of the main analysis class.
    protected[taint] var restrictedPackages: Seq[String] = null

    def definedInRestrictedPackage(packageName: String): Boolean =
        restrictedPackages.exists(packageName.startsWith(_))
}

package taint {

//    import java.net.URL
//    import analyses.{ Project, Analysis, AnalysisExecutor, ReportableAnalysisResult }
//    import de.tud.cs.st.bat.resolved.ai.project.{ AIProject, Report }
//    import de.tud.cs.st.bat.resolved.ai.domain._
//    import de.tud.cs.st.util.graphs._
//    import de.tud.cs.st.util.ControlAbstractions.process
//
//    object JDKTaintAnalysis
//            extends AIProject[URL,SomeDomain with Report]
//            with Analysis[URL, ReportableAnalysisResult]
//            with AnalysisExecutor {
//
//        def ai = BaseAI
//        
//        def description: String = "Finds unsafe Class.forName(...) calls."
//
//        val analysis = this
//
//        val javaSecurityParameter = "-java.security="
//
//        var javaSecurityFile: String = _
//
//        override def analysisParametersDescription: String =
//            javaSecurityParameter+"<JRE/JDK Security Policy File>"
//
//        override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
//            parameters.size == 1 && {
//                javaSecurityFile = parameters.head.substring(javaSecurityParameter.length())
//                new java.io.File(javaSecurityFile).exists()
//            }
//
//        override def analyze(
//            project: analyses.Project[URL],
//            parameters: Seq[String]): ReportableAnalysisResult = {
//            restrictedPackages = process(new java.io.FileInputStream(javaSecurityFile)) { in ⇒
//                val properties = new java.util.Properties()
//                properties.load(in)
//                properties.getProperty("package.access", "").
//                    split(",").
//                    map(_.trim.replace('.', '/'))
//            }
//            println(restrictedPackages.mkString("Restricted packages:\n\t", "\n\t", "\n"))
//
//            super.analyze(project, parameters)
//        }
//
//        def entryPoints(project: Project[URL]): Iterable[(ClassFile, Method)] = {
//            import ObjectType._
//            for {
//                classFile ← project.classFiles
//                if !definedInRestrictedPackage(classFile.thisClass.packageName)
//                method ← classFile.methods
//                if method.isPublic || (method.isProtected && !classFile.isFinal)
//                descriptor = method.descriptor
//                if (descriptor.returnType == Object) || (descriptor.returnType == Class)
//                if (descriptor.parameterTypes.contains(String))
//                if method.body.isDefined // let's filter native methods...
//                // Select some specific method for debugging purposes...
//                //                if classFile.thisClass.className == "com/sun/org/apache/xalan/internal/utils/ObjectFactory"
//                //                if method.name == "lookUpFactoryClass"
//                //                if classFile.thisClass.className == "ai/taint/PermissionCheckNotOnAllPaths"
//                //                if method.name == "foo"
//                //                if classFile.thisClass.className == "com/sun/beans/finder/ClassFinder"
//                //                if method.name == "findClass"
//            } yield (classFile, method)
//        }
//
//        /**
//         * Basically, each entry point is analyzed on its own and is associated with
//         * a unique domain instance.
//         */
//        def domain(
//            project: analyses.Project[URL],
//            classFile: ClassFile,
//            method: Method): Domain[CallStackEntry] with Report = {
//            new RootTaintAnalysisDomain[URL](project, (classFile, method))
//        }
//    }
//
//    trait TaintAnalysisDomain[Source]
//            extends Domain[CallStackEntry]
//            with DefaultValueBinding[CallStackEntry]
//            with DefaultTypeLevelLongValues[CallStackEntry]
//            with DefaultTypeLevelFloatValues[CallStackEntry]
//            with DefaultTypeLevelDoubleValues[CallStackEntry]
//            with DefaultReturnAddressValues[CallStackEntry]
//            //with DefaultPreciseIntegerValues[CallStackEntry]
//            with DefaultTypeLevelIntegerValues[CallStackEntry]
//            with DefaultPreciseReferenceValuesWithClosedHierarchy[CallStackEntry]
//            with StringValues[CallStackEntry]
//            with TypeLevelArrayInstructions
//            with TypeLevelFieldAccessInstructions
//            with TypeLevelInvokeInstructions
//            //with PerformInvocations[CallStackEntry, Source]
//            with DoNothingOnReturnFromMethod
//            with ProjectBasedTypeHierarchy[Source]
//            with Report { thisDomain ⇒
//
//        import de.tud.cs.st.util.Unknown
//        import ObjectType._
//
//        protected def declaringClass = identifier._1.thisClass
//        protected def methodName = identifier._2.name
//        protected def methodDescriptor = identifier._2.descriptor
//
//        protected def contextIdentifier =
//            declaringClass.className+"{ "+methodDescriptor.toJava(methodName)+" }"
//
//        /**
//         * Identifies the node in the analysis graph that represents the call
//         * of this domain's method.
//         *
//         * ==Control Flow==
//         * We only attach a child node (the `contextNode`) to the caller node if
//         * the analysis of this domain's method returns a relevant result.
//         * This is determined when the `postAnalysis` method is called.
//         */
//        protected val callerNode: SimpleNode[_]
//
//        /**
//         * Represents the node in the analysis graph that models the entry point
//         * to this method.
//         */
//        protected val contextNode: SimpleNode[(RelevantParameters, String)]
//
//        /**
//         * Stores the program counters of those invoke instructions that return either
//         * a class object or an object with the greatest lower bound `java.lang.Object`
//         * and which were originally passed a relevant value (in particular a relevant
//         * parameter).
//         */
//        protected var relevantValuesOrigins: List[(PC, SimpleNode[String])] = List.empty
//
//        /**
//         * Stores the values that are returned by this method. When the analysis
//         * of this method has completed the '''caller''' can then ask whether a
//         * a relevant value was returned.
//         */
//        protected var returnedValues: List[(PC, DomainValue)] = List.empty
//
//        //        /**
//        //         * @note
//        //         * Calling this method only makes sense when the analysis of this domain's method
//        //         * has completed.
//        //         */
//        //        def isRelevantValueReturned(): Boolean = {
//        //            relevantValuesOrigins.exists { relevantValueOrigin ⇒
//        //                returnedValues.exists { returnedValue ⇒
//        //                    origin(returnedValue._2).exists { returnedValueOrigin ⇒
//        //                        returnedValueOrigin == relevantValueOrigin._1
//        //                    }
//        //                }
//        //            }
//        //        }
//
//        /**
//         * @note Initialized by a call to `postAnalysis()`!
//         */
//        private var isRelevantValueReturned = false
//
//        /**
//         * When the analysis of this domain's method has completed a post analysis
//         * can be performed to construct the analysis graph.
//         *
//         * @note
//         * Calling this method only makes sense when the analysis of this domain's method
//         * has completed.
//         *
//         * @note
//         * This method must be called at most once otherwise the constructed analysis graph
//         * may contain duplicate information.
//         */
//        protected def postAnalysis(): Boolean = {
//            relevantValuesOrigins.foreach { relevantValueOrigin ⇒
//                returnedValues.foreach { returnedValue ⇒
//                    if (origin(returnedValue._2).exists { returnedValueOrigin ⇒
//                        returnedValueOrigin == relevantValueOrigin._1
//                    }) {
//                        isRelevantValueReturned = true
//                        relevantValueOrigin._2.addChild(callerNode)
//                    }
//                }
//            }
//            if (isRelevantValueReturned)
//                callerNode.addChild(contextNode)
//
//            isRelevantValueReturned
//        }
//
//        /**
//         * @note
//         * Calling this method only makes sense when the analysis of this domain's method
//         * has completed.
//         */
//        lazy val report = {
//            postAnalysis();
//            if (isRelevantValueReturned)
//                Some(toDot.generateDot(Set(callerNode)))
//            else
//                None
//        }
//
//        /**
//         * Checks if we want to trace the respective method call because a string
//         * value is passed on and the method returns either a class object or some
//         * object for which we have no further type information.
//         */
//        private def isRelevantCall(
//            pc: Int,
//            name: String,
//            methodDescriptor: MethodDescriptor,
//            operands: List[DomainValue]): Boolean = {
//            operands.exists { op ⇒
//                // test if one of the relevant parameters was passed to the method
//                origin(op).exists(pc ⇒ contextNode.identifier._1.contains(pc))
//            } && (
//                methodDescriptor.returnType == Object ||
//                methodDescriptor.returnType == Class)
//        }
//
//        override def areturn(pc: Int, value: DomainValue) {
//            returnedValues = (pc, value) :: returnedValues
//        }
//
//        override def invokeinterface(pc: Int,
//                                     declaringClass: ReferenceType,
//                                     name: String,
//                                     methodDescriptor: MethodDescriptor,
//                                     operands: List[DomainValue]) = {
//            isRelevantCall(pc, name, methodDescriptor, operands)
//            // TODO ...
//            super.invokeinterface(pc, declaringClass, name, methodDescriptor, operands)
//        }
//
//        override def invokevirtual(pc: Int,
//                                   declaringClass: ReferenceType,
//                                   name: String,
//                                   methodDescriptor: MethodDescriptor,
//                                   operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
//            isRelevantCall(pc, name, methodDescriptor, operands)
//            // TODO ...
//            // check if we have a call to Class.newInstance...
//            super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)
//        }
//
//        override def invokespecial(pc: Int,
//                                   declaringClass: ReferenceType,
//                                   methodName: String,
//                                   methodDescriptor: MethodDescriptor,
//                                   operands: List[DomainValue]) = {
//            isRelevantCall(pc, methodName, methodDescriptor, operands)
//            // TODO ...
//            super.invokespecial(pc, declaringClass, methodName, methodDescriptor, operands)
//        }
//
//        override def invokestatic(
//            pc: Int,
//            declaringClass: ReferenceType,
//            methodName: String,
//            methodDescriptor: MethodDescriptor,
//            operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
//
//            def doTypeLevelInvoke =
//                super.invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)
//
//            if (!declaringClass.isObjectType || (
//                methodDescriptor.returnType != Class &&
//                methodDescriptor.returnType != Object))
//                return doTypeLevelInvoke;
//
//            val relevantOperands = operands.zipWithIndex.filter { operand_index ⇒
//                val (operand, index) = operand_index
//                origin(operand).exists { operandOrigin ⇒
//                    contextNode.identifier._1.exists(_ == operandOrigin)
//                }
//            }
//            if (relevantOperands.isEmpty)
//                return doTypeLevelInvoke;
//
//            // If we reach this point, we have an invocation of a relevant method 
//            // with a relevant parameter...
//            if (declaringClass == ObjectType.Class &&
//                methodName == "forName" &&
//                methodDescriptor.parameterTypes == Seq(String)) {
//                // found final  sink ...
//                val sinkNode = new SimpleNode(pc+": Class.forName("+operands.head+")")
//                contextNode.addChild(sinkNode)
//                relevantValuesOrigins = (pc, sinkNode) :: relevantValuesOrigins
//                println("Call to class for name found...")
//                return doTypeLevelInvoke;
//            }
//
//            // If we reach this point, we have an invocation of a relevant method 
//            // with a relevant parameter that is not our final sink...
//            val (classFile: ClassFile, method: Method) =
//                classHierarchy.resolveMethodReference(
//                    declaringClass.asObjectType,
//                    methodName,
//                    methodDescriptor,
//                    project).getOrElse {
//                        return doTypeLevelInvoke
//                    }
//
//            if (method.isNative)
//                return doTypeLevelInvoke;
//
//            val callerNode = new SimpleNode(pc+": method invocation")
//            val calleeDomain = new CalledTaintAnalysisDomain(
//                this,
//                (classFile, method),
//                callerNode,
//                relevantOperands.map(_._2)
//            )
//            val calleeParameters = operands.reverse.zipWithIndex.map { operand_index ⇒
//                val (operand, index) = operand_index
//                operand.adapt(calleeDomain, -(index + 1))
//            }.toArray(calleeDomain.DomainValueTag)
//            val parameters = DomainValues(calleeDomain)(calleeParameters)
//            if (isRecursiveCall(classFile, method, parameters))
//                return doTypeLevelInvoke;
//
//            println("Relevant call found: "+method.toJava)
//
//            // If we reach this point, we have an invocation of a relevant method 
//            // with a relevant parameter that is not our final sink and which is
//            // not native and which is not a recursive call
//            val aiResult = BaseAI.perform(classFile, method, calleeDomain)(Some(calleeParameters))
//            aiResult.domain.postAnalysis()
//            if (!aiResult.domain.isRelevantValueReturned)
//                return doTypeLevelInvoke;
//
//            val returnNode = new SimpleNode(pc+": returned value from : "+aiResult.domain.contextIdentifier)
//            contextNode.addChild(callerNode)
//            relevantValuesOrigins = (pc, returnNode) :: relevantValuesOrigins
//            val headValue = aiResult.domain.returnedValues.head._2
//            val mergedValue = ComputedValue(Some(
//                (headValue /: aiResult.domain.returnedValues.tail) { (c, n) ⇒
//                    c.join(pc, n._2) match {
//                        case NoUpdate          ⇒ c
//                        case SomeUpdate(value) ⇒ value
//                    }
//                }.adapt(this, pc)
//            ))
//            mergedValue
//        }
//
//        /**
//         * Checks if the call to the specified method represents a (mutually) recursive
//         * call.
//         */
//        def isRecursiveCall(
//            declaringClass: ReferenceType,
//            methodName: String,
//            methodDescriptor: MethodDescriptor,
//            parameters: DomainValues[_ <: Domain[CallStackEntry]]): Boolean
//
//        final def isRecursiveCall(
//            classFile: ClassFile,
//            method: Method,
//            parameters: DomainValues[_ <: Domain[CallStackEntry]]): Boolean = {
//            isRecursiveCall(
//                classFile.thisClass,
//                method.name,
//                method.descriptor,
//                parameters)
//        }
//
//        //        override def putfield(
//        //            pc: Int,
//        //            objectref: DomainValue,
//        //            value: DomainValue,
//        //            declaringClass: ObjectType,
//        //            name: String,
//        //            fieldType: FieldType) = {
//        //            if (origin and type fit)
//        //                valuePasses = declaringClass.className+"."+name :: valuePasses
//        //            super.putfield(pc, objectref, value, declaringClass, name, fieldType)
//        //        }
//        //
//        //        override def putstatic(
//        //            pc: Int,
//        //            value: DomainValue,
//        //            declaringClass: ObjectType,
//        //            name: String,
//        //            fieldType: FieldType) = {
//        //            if (origin and type fit)
//        //                valuePasses = declaringClass.className+"."+name :: valuePasses
//        //
//        //            super.putstatic(pc, value, declaringClass, name, fieldType)
//        //        }
//
//    }
//
//    class RootTaintAnalysisDomain[Source](
//        val project: Project[Source],
//        val identifier: (ClassFile, Method))
//            extends TaintAnalysisDomain[Source] {
//
//        val callerNode = new SimpleNode("Some user of the API")
//
//        val contextNode = {
//            val firstIndex = if (identifier._2.isStatic) 1 else 2
//            val relevantParameters =
//                methodDescriptor.parameterTypes.zipWithIndex.filter { param_idx ⇒
//                    val (parameterType, _) = param_idx;
//                    parameterType == ObjectType.String
//                }.map(param_idx ⇒ -(param_idx._2 + firstIndex))
//            new SimpleNode((relevantParameters, contextIdentifier))
//        }
//
//        def isRecursiveCall(
//            declaringClass: ReferenceType,
//            methodName: String,
//            methodDescriptor: MethodDescriptor,
//            parameters: DomainValues[_ <: Domain[CallStackEntry]]): Boolean = {
//            this.declaringClass == declaringClass &&
//                this.methodName == methodName &&
//                this.methodDescriptor == methodDescriptor // &&
//            // TODO check that the analysis would be made under the same assumption (same parameters!)
//        }
//    }
//
//    class CalledTaintAnalysisDomain[Source](
//        val previousTaintAnalysisDomain: TaintAnalysisDomain[Source],
//        val identifier: CallStackEntry,
//        val callerNode: SimpleNode[_],
//        val relevantParameters: RelevantParameters)
//            extends TaintAnalysisDomain[Source] {
//
//        val contextNode = new SimpleNode((relevantParameters, contextIdentifier))
//
//        def project = previousTaintAnalysisDomain.project
//
//        def isRecursiveCall(
//            declaringClass: ReferenceType,
//            methodName: String,
//            methodDescriptor: MethodDescriptor,
//            parameters: DomainValues[_ <: Domain[CallStackEntry]]): Boolean = {
//            (this.declaringClass == declaringClass &&
//                this.methodName == methodName &&
//                this.methodDescriptor == methodDescriptor
//            // && // TODO check that the analysis would be made under the same assumption (same parameters!)    
//            ) || (
//                    declaringClass.isObjectType &&
//                    previousTaintAnalysisDomain.isRecursiveCall(
//                        declaringClass.asObjectType,
//                        methodName,
//                        methodDescriptor,
//                        parameters))
//
//        }
//    }
}