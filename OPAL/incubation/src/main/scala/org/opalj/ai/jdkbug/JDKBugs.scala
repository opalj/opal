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
package jdkbug

import java.net.URL
import org.opalj.graphs._
import br._
import org.opalj.br.instructions._
import org.opalj.br.analyses.{ Project, OneStepAnalysis, AnalysisExecutor, ReportableAnalysisResult }
import project.{ AIProject, OptionalReport }
import domain._
import domain.l0._
import domain.l1._
import domain.tracing._
import debug.XHTML._
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.l1.DefaultStringValuesBinding
import org.opalj.ai.BaseAI
import org.opalj.ai.project.OptionalReport
import org.opalj.ai.domain.DefaultDomainValueBinding
import org.opalj.ai.domain.DomainId
import org.opalj.ai.project.AIProject
import org.opalj.ai.domain.l0.TypeLevelInvokeInstructions
import org.opalj.ai.domain.TheCode
import org.opalj.ai.domain.l0.DefaultTypeLevelLongValues
import org.opalj.ai.domain.l0.DefaultTypeLevelIntegerValues
import org.opalj.ai.domain.ProjectBasedClassHierarchy
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.l0.DefaultTypeLevelDoubleValues
import org.opalj.ai.domain.l0.DefaultTypeLevelFloatValues
import org.opalj.ai.Domain
import org.opalj.ai.domain.l0.TypeLevelReferenceValues
import org.opalj.ai.domain.l0.DefaultPrimitiveValuesConversions
import org.opalj.ai.domain.l0.TypeLevelFieldAccessInstructions

/**
 * Searches for occurrences of the Class.forName bug in the JDK
 *
 * @author Lars Schulte
 */
object JDKTaintAnalysis
        extends AIProject[URL, Domain with OptionalReport]
        with OneStepAnalysis[URL, ReportableAnalysisResult]
        with AnalysisExecutor {

    def ai = new AI[Domain with OptionalReport] {}

    override def description: String = "Finds unsafe Class.forName(...) calls."

    println(System.getProperty("java.version"));

    val analysis = this

    val javaSecurityParameter = "-java.security="

    var javaSecurityFile: String = _

    override def analysisSpecificParametersDescription: String =
        javaSecurityParameter+"<JRE/JDK Security Policy File>"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.size == 1 && {
            javaSecurityFile = parameters.head.substring(javaSecurityParameter.length())
            new java.io.File(javaSecurityFile).exists()
        }

    override def doAnalyze(
        project: analyses.Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean): ReportableAnalysisResult = {
        restrictedPackages = process(new java.io.FileInputStream(javaSecurityFile)) { in ⇒
            val properties = new java.util.Properties()
            properties.load(in)
            properties.getProperty("package.access", "").
                split(",").
                map(_.trim.replace('.', '/')).toSet
        }
        println(restrictedPackages.mkString("Restricted packages:\n\t", "\n\t", "\n"))

        super.analyze(project, parameters)
    }

    /**
     * This method finds all possible entry points.
     * An entry point has to be public or protected and not final.
     * Also it needs to take a String as argument and return an Object or Class
     */
    def entryPoints(project: Project[URL]): Iterable[(ClassFile, Method)] = {
        import ObjectType._
        for {
            classFile ← project.classFiles
            if !definedInRestrictedPackage(classFile.thisType.packageName)
            method ← classFile.methods
            if method.body.isDefined
            if method.isPublic || (method.isProtected && !classFile.isFinal)
            descriptor = method.descriptor
            if (descriptor.parameterTypes.contains(String))
        } yield (classFile, method)
    }

    /**
     * Basically, each entry point is analyzed on its own and is associated with
     * a unique domain instance.
     */
    def domain(
        project: analyses.Project[URL],
        classFile: ClassFile,
        method: Method): Domain with OptionalReport = {
        new RootTaintAnalysisDomain[URL](project, List.empty, CallStackEntry(classFile, method), false)
    }
}

// used to cache interface calls - currently disabled

//object TaintAnalysisDomain
//  extends TypeLevelDomain
//  with IgnoreMethodResults
//  with IgnoreSynchronization {
//
//  type Id = String
//
//  def id = "CachedInterfaceCallsDomain"
//
//  /**
//   * contains all cached interface calls
//   */
//  var cachedInterfaceCalls: List[CachedInterfaceCall] = List.empty
//
//  case class CachedInterfaceCall(methodName: String, methodDescriptor: MethodDescriptor, operands: List[DomainValue]) {
//    override def equals(obj: Any) = {
//      obj match {
//        case that: CachedInterfaceCall => {
//
//          methodName == that.methodName &&
//            methodDescriptor == that.methodDescriptor &&
//            operands.forall(p => that.operands.contains(p))
//        }
//        case _ => false
//      }
//
//    }
//  }
//}

/**
 * This companion object saves some values during the analysis. It can be used for testing purpose
 */
object TaintAnalysisDomain {

    var numberOfReports = new java.util.concurrent.atomic.AtomicInteger(0)

}

/**
 * This is the analysis domain it provides all need functionality to find Class.forName bugs.
 */
trait TaintAnalysisDomain[Source]
        extends Domain
        with DomainId
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with DefaultPrimitiveValuesConversions
        with DefaultDomainValueBinding
        with DefaultTypeLevelLongValues
        with DefaultTypeLevelFloatValues
        with DefaultTypeLevelDoubleValues
        with DefaultTypeLevelIntegerValues
        with DefaultStringValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with TypeLevelFieldAccessInstructions
        with TypeLevelInvokeInstructions
        with TypeLevelReferenceValues
        with TheProject[Source]
        with TheCode
        with ProjectBasedClassHierarchy
        with OptionalReport { thisDomain ⇒

    type Id = CallStackEntry

    import ObjectType._

    protected def declaringClass = id.classFile.thisType
    def method = id.method
    protected def methodName = id.method.name
    protected def methodDescriptor = id.method.descriptor
    def code = method.body.get

    protected def contextIdentifier =
        declaringClass.fqn+"{ "+methodDescriptor.toJava(methodName)+" }"

    /**
     * Identifies the node in the analysis graph that represents the call
     * of this domain's method.
     *
     * ==Control Flow==
     * We only attach a child node (the `contextNode`) to the caller node if
     * the analysis of this domain's method returns a relevant result.
     * This is determined when the `postAnalysis` method is called.
     */
    protected val callerNode: SimpleNode[_]

    /**
     * Represents the node in the analysis graph that models the entry point
     * to this method.
     */
    protected val contextNode: SimpleNode[(RelevantParameters, String)]

    /**
     * Stores the program counters of those invoke instructions that return either
     * a class object or an object with the greatest lower bound `java.lang.Object`
     * and which were originally passed a relevant value (in particular a relevant
     * parameter).
     */
    protected var relevantValuesOrigins: List[(PC, SimpleNode[String])] = List.empty

    /**
     * Stores the values that are returned by this method. When the analysis
     * of this method has completed the '''caller''' can then ask whether a
     * a relevant value was returned.
     */
    protected var returnedValues: List[(PC, DomainValue)] = List.empty

    /**
     * Stores relevant PCs that occur during the analysis of a method.
     * For examples calls to getstatic or Class.forName
     */
    protected var taintedPCs: List[PC] = List.empty

    /**
     * Stores a list of all global fields that contain a tainted value
     */
    var taintedFields: List[String] = List.empty

    /**
     * Predicates if the analysis is currently in the process of looking
     * for the use of a previously tainted field. If this is true then this
     * boolean prevents the analysis from analyzing "putfield" instructions that
     * could lead the analysis to run into an endless loop
     */
    val checkForFields: Boolean

    /**
     * Stores if a call to Class.forName has occurred. No report is created if this is not true
     */
    protected var callToClassForNameFound: Boolean = false;

    /**
     * @note
     * Calling this method only makes sense when the analysis of this domain's method
     * has completed.
     */
    def isRelevantValueReturned(): Boolean = {
        relevantValuesOrigins.exists { relevantValueOrigin ⇒
            returnedValues.exists { returnedValue ⇒
                origin(returnedValue._2).exists { returnedValueOrigin ⇒
                    returnedValueOrigin == relevantValueOrigin._1
                }
            }
        }
    }

    /**
     * When the analysis of this domain's method has completed a post analysis
     * can be performed to construct the analysis graph.
     *
     * @note
     * Calling this method only makes sense when the analysis of this domain's method
     * has completed.
     *
     * @note
     * This method must be called at most once otherwise the constructed analysis graph
     * may contain duplicate information.
     */
    def postAnalysis(): Boolean = {
        relevantValuesOrigins.foreach { relevantValueOrigin ⇒
            returnedValues.foreach { returnedValue ⇒
                if (origin(returnedValue._2).exists { returnedValueOrigin ⇒
                    returnedValueOrigin == relevantValueOrigin._1
                }) {
                    // adds a return path to every relevant value that is returned to the caller
                    relevantValueOrigin._2.addChild(callerNode)
                }
            }
        }
        if (isRelevantValueReturned) {
            // a relevant value is returned so we add this node into the analysis graph
            callerNode.addChild(contextNode)
            true
        } else
            false
    }

    /**
     * @note
     * Calling this method only makes sense when the analysis of this domain's method
     * has completed.
     */
    lazy val report = {
        postAnalysis();
        if (isRelevantValueReturned && callToClassForNameFound) {
            val createdDot = toDot.generateDot(Set(callerNode))
            TaintAnalysisDomain.numberOfReports.incrementAndGet()
            Some(createdDot)
        } else
            None
    }

    /**
     * Checks if we want to trace the respective method call because a relevant string or a class
     * value is passed on and the method returns either a class object or some
     * object for which we have no further type information.
     */
    private def isRelevantCall(
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): Boolean = {
        operands.exists { op ⇒
            origin(op).exists(pc ⇒ contextNode.identifier._1.union(taintedPCs).contains(pc))
        } && (
            methodDescriptor.returnType == Object ||
            methodDescriptor.returnType == Class)
    }

    override def areturn(pc: Int, value: DomainValue) {
        // in case a relevant parameter is returned by the method
        if (origin(value).exists(orig ⇒ contextNode.identifier._1.union(taintedPCs).contains(orig))) {
            relevantValuesOrigins = (-1, new SimpleNode("return of a relevant Parameter")) :: relevantValuesOrigins
        }
        returnedValues = (pc, value) :: returnedValues
    }

    //TODO check compatibility with Java8 Extension Methods
    override def invokeinterface(pc: Int,
                                 declaringClass: ObjectType,
                                 methodName: String,
                                 methodDescriptor: MethodDescriptor,
                                 operands: List[DomainValue]): MethodCallResult = {

        def doTypeLevelInvoke =
            super.invokeinterface(pc, declaringClass, methodName, methodDescriptor, operands)

        /*
       *invokeinterface is currently disabled due to the following reasons
       *  1) caching interfaces does not work properly:
       *  	- problem with adapting some values the cached domain
       *   	- similar operands are not matched as equal due to different instance of RefValue
       *  2) invokeinterface does not find and Class.forName bugs only more pathes to it.
       */

        // invokeinterface is currently due to the following reasons
        //  - caching interfaces does not work properly:
        //problem with adepting some values the cached domain

        //    if (isIrrelevantInvoke(methodDescriptor, declaringClass))
        //      return doTypeLevelInvoke;
        //
        //    val relevantOperands = computeRelevantOperands(operands)
        //    if (relevantOperands.isEmpty)
        //      return doTypeLevelInvoke;
        //
        //    // If we reach this point, we have an invocation of a relevant method 
        //    // with a relevant parameter that is not our final sink...
        //
        //    val method: Method =
        //      classHierarchy.resolveInterfaceMethodReference(
        //        declaringClass.asObjectType,
        //        methodName,
        //        methodDescriptor,
        //        project).getOrElse {
        //          return doTypeLevelInvoke
        //        }
        //
        //    var cachedInterfaceCall = TaintAnalysisDomain.CachedInterfaceCall(
        //      methodName,
        //      methodDescriptor,
        //      operands.reverse.tail.map(operand => operand.adapt(TaintAnalysisDomain, 0)))
        //
        //
        //    // filters cachedInterfaceCalls to see if there has already been a call to the
        //    // same implementing class with the same parameters
        //    // if that is the case the analysis stops  
        //    if (TaintAnalysisDomain.cachedInterfaceCalls.contains(cachedInterfaceCall)) {
        //      return doTypeLevelInvoke
        //    }
        //
        //    TaintAnalysisDomain.cachedInterfaceCalls = cachedInterfaceCall :: TaintAnalysisDomain.cachedInterfaceCalls
        //
        //    // define a filter for the call to lookupImplementing methods
        //    // if the ObjectType is precise it's upper bound is used.
        //    // else every implementing method is analyzed
        //    val filter: (ObjectType => Boolean) = operands.last match {
        //      case x: SObjectValue =>
        //        if (x.isPrecise) {
        //          (x.upperTypeBound.contains(_))
        //        } else {
        //          (_ => true)
        //        }
        //      case _ => {
        //        (_ => false)
        //      }
        //    }
        //
        //    // look up every class that implements the method and was previously
        //    // instantiated
        //    val implementingMethods = classHierarchy.lookupImplementingMethods(
        //      declaringClass.asObjectType,
        //      methodName,
        //      methodDescriptor,
        //      project,
        //      filter)
        //
        //    // analyze every found method implementation
        //    implementingMethods.foreach { (m: Method) =>
        //      {
        //        inspectMethod(pc, m, operands)
        //      }
        //    }
        return doTypeLevelInvoke
    }

    override def invokevirtual(
        pc: Int,
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {

        def doTypeLevelInvoke =
            super.invokevirtual(pc, declaringClass, methodName, methodDescriptor, operands)

        // check if we have a call to Class.newInstance...
        if (methodName == "newInstance") {
            if (isRelevantCall(methodDescriptor, operands)) {
                relevantValuesOrigins = (pc, new SimpleNode("newInstance")) :: relevantValuesOrigins
            }
        }

        if (isIrrelevantInvoke(methodDescriptor, declaringClass))
            return doTypeLevelInvoke;

        val relevantOperands = computeRelevantOperands(operands)
        if (relevantOperands.isEmpty)
            return doTypeLevelInvoke;

        // If we reach this point, we have an invocation of a relevant method 
        // with a relevant parameter that is not our final sink...

        val method: Method =
            classHierarchy.lookupMethodDefinition(
                declaringClass.asObjectType,
                methodName,
                methodDescriptor,
                project).getOrElse {
                    return doTypeLevelInvoke
                }

        if (method.body.isEmpty) {
            return doTypeLevelInvoke;
        }

        inspectMethod(pc, method, operands)

        return doTypeLevelInvoke
    }

    override def invokespecial(
        pc: Int,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {

        def doTypeLevelInvoke =
            super.invokespecial(pc, declaringClass, methodName, methodDescriptor, operands)

        if (isIrrelevantInvoke(methodDescriptor, declaringClass))
            return doTypeLevelInvoke;

        val relevantOperands = computeRelevantOperands(operands)
        if (relevantOperands.isEmpty)
            return doTypeLevelInvoke;

        // If we reach this point, we have an invocation of a relevant method 
        // with a relevant parameter that is not our final sink...
        val method: Method =
            classHierarchy.resolveMethodReference(
                declaringClass.asObjectType,
                methodName,
                methodDescriptor,
                project).getOrElse {
                    return doTypeLevelInvoke
                }

        inspectMethod(pc, method, operands)

        return doTypeLevelInvoke
    }

    override def invokestatic(
        pc: Int,
        declaringClass: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {

        def doTypeLevelInvoke =
            super.invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

        if (isIrrelevantInvoke(methodDescriptor, declaringClass))
            return doTypeLevelInvoke;

        val relevantOperands = computeRelevantOperands(operands)
        if (taintedFields.isEmpty)
            if (relevantOperands.isEmpty)
                return doTypeLevelInvoke;

        // If we reach this point, we have an invocation of a relevant method 
        // with a relevant parameter...
        if (checkForSink(declaringClass, methodDescriptor, methodName)) {
            registerSink(pc, operands)
            return doTypeLevelInvoke;
        }

        // If we reach this point, we have an invocation of a relevant method 
        // with a relevant parameter that is not our final sink...
        val method: Method =
            classHierarchy.resolveMethodReference(
                declaringClass.asObjectType,
                methodName,
                methodDescriptor,
                project).getOrElse {
                    return doTypeLevelInvoke
                }

        inspectMethod(pc, method, operands)

        return doTypeLevelInvoke
    }

    /**
     * Checks if the call to the specified method represents a (mutually) recursive
     * call.
     */
    def isRecursiveCall(
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        parameters: DomainValues): Boolean

    final def isRecursiveCall(
        classFile: ClassFile,
        method: Method,
        parameters: DomainValues): Boolean = {
        isRecursiveCall(
            classFile.thisType,
            method.name,
            method.descriptor,
            parameters)
    }

    override def putfield(
        pc: PC,
        objectref: DomainValue,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType) = {

        // skip if we already check for fields so we don't run into a loop
        if (!checkForFields) {
            // check if the field is set with a relevant value
            if (contextNode.identifier._1.union(taintedPCs).intersect(origin(value).toSeq).nonEmpty) {

                taintedFields = name :: taintedFields

                val thisField: Field = classHierarchy.resolveFieldReference(
                    declaringClass,
                    name,
                    fieldType,
                    project).get

                val classFile: ClassFile = project.classFile(thisField)

                findAndInspectNewEntryPoint(classFile)
            }
        }

        super.putfield(pc, objectref, value, declaringClass, name, fieldType)
    }

    override def getfield(
        pc: PC,
        objectref: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType) = {

        // check if a tainted field is read
        if (taintedFields.contains(name)) {
            // set the value at this pc as tainted
            taintedPCs = pc :: taintedPCs
        }

        super.getfield(pc, objectref, declaringClass, name, fieldType)
    }

    override def putstatic(
        pc: PC,
        value: DomainValue,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType) = {

        // check if a tainted value is put in the field
        if (contextNode.identifier._1.union(taintedPCs).intersect(origin(value).toSeq).nonEmpty) {
            taintedFields = name :: taintedFields
            // else if it doesn't get a tainted value:
            // check if it was marked as tainted and remove it from the list
        } else if (taintedFields.contains(name)) {
            taintedFields = taintedFields.filter(_ != name)
        }

        super.putstatic(pc, value, declaringClass, name, fieldType)
    }

    override def getstatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        fieldType: FieldType) = {

        // check if a tainted field is read
        if (taintedFields.contains(name)) {
            // store the PC of this instruction
            taintedPCs = pc :: taintedPCs
        }
        super.getstatic(pc, declaringClass, name, fieldType)
    }

    /**
     * checks if the invoke is irrelevant
     * A relevant call needs to return a Class or an Object
     * and its declaring Class must be an ObjectType
     */
    def isIrrelevantInvoke(methodDescriptor: MethodDescriptor, declaringClass: ReferenceType): Boolean = {
        (!declaringClass.isObjectType || (
            methodDescriptor.returnType != Class &&
            methodDescriptor.returnType != Object &&
            methodDescriptor.returnType != String))
    }

    /**
     * Compares the list of possibly tainted values with the operands
     * and returns all parameters that could be tainted.
     */
    def computeRelevantOperands(operands: List[DomainValue]) = {
        operands.zipWithIndex.filter { operand_index ⇒
            val (operand, index) = operand_index
            origin(operand).exists { operandOrigin ⇒
                contextNode.identifier._1.union(taintedPCs).exists(_ == operandOrigin)
            }
        }
    }

    /**
     * Check if the method call resembles a call to Class.forName. In this
     * case the analysis has found a sink and this method returns true.
     */
    def checkForSink(declaringClass: ObjectType, methodDescriptor: MethodDescriptor, methodName: String): Boolean = {
        (declaringClass == ObjectType.Class &&
            methodName == "forName" &&
            methodDescriptor.parameterTypes == Seq(String))
    }

    /**
     * Creates a sinkNode into the call graph and add the PC to additionalRelevantParameters
     * and relevantValuesOrigins
     */
    def registerSink(pc: PC, operands: List[DomainValue]) = {
        val sinkNode = new SimpleNode(pc+": Class.forName("+operands.head+")")
        contextNode.addChild(sinkNode)
        callToClassForNameFound = true;
        taintedPCs = pc :: taintedPCs
        relevantValuesOrigins = (pc, sinkNode) :: relevantValuesOrigins
    }

    /**
     * This method allows for the complete analysis of a given method. It
     * creates a new unique CalledTaintAnalysisDomain and evaluates the
     * returned result of that analysis. It returns true if a relevant
     * Value was returned.
     */
    def inspectMethod(
        pc: PC,
        method: Method,
        operands: List[DomainValue]): Boolean = {
        val classFile = project.classFile(method)

        if (!method.isNative && !definedInRestrictedPackage(classFile.thisType.packageName) && method.body.nonEmpty) {
            val callerNode = new SimpleNode(pc+": method invocation; method id: "+method)

            // compute the new pc of relevant parameters that the analysis
            // wants to keep track of
            var relevantParameters: List[Int] = List.empty
            for ((operand, index) ← operands.view.reverse.zipWithIndex) {
                if ((origin(operand)).exists(orig ⇒ contextNode.identifier._1.union(taintedPCs).contains(orig))) {
                    relevantParameters = -(index + 1) :: relevantParameters
                }
            }

            val calleeDomain = new CalledTaintAnalysisDomain(
                this,
                CallStackEntry(classFile, method),
                callerNode,
                relevantParameters,
                checkForFields)

            val calleeParameters = calleeDomain.DomainValueTag.newArray(method.body.get.maxLocals)
            var localVariableIndex = 0
            for ((operand, index) ← operands.view.reverse.zipWithIndex) {
                calleeParameters(localVariableIndex) =
                    operand.adapt(calleeDomain, -(index + 1))
                localVariableIndex += operand.computationalType.operandSize
            }

            val parameters = DomainValues(calleeDomain)(calleeParameters)
            if (isRecursiveCall(classFile, method, parameters)) {
                false
            } else {
                // If we reach this point, we have an invocation of a relevant method 
                // with a relevant parameter that is not our final sink and which is
                // not native and which is not a recursive call
                val v = method.body.get
                // Analyze the method
                val aiResult = BaseAI.perform(classFile, method, calleeDomain)(Some(calleeParameters))
                if (!aiResult.domain.isRelevantValueReturned) {
                    false
                } else {
                    aiResult.domain.postAnalysis()
                    if (aiResult.domain.callToClassForNameFound) {
                        callToClassForNameFound = true;
                    }
                    // set return nodes
                    val returnNode = new SimpleNode(pc+": returned value from : "+aiResult.domain.contextIdentifier)
                    contextNode.addChild(returnNode)
                    taintedPCs = pc :: taintedPCs
                    contextNode.addChild(callerNode)

                    relevantValuesOrigins = (pc, returnNode) :: relevantValuesOrigins
                    true
                }
            }
        }
        false
    }

    /**
     * This method tries to find new entry points that use a tainted field.
     * For each found entry point a new RootTaintAnalysisDomain is created.
     * If the analyzed method found a bug (created a report) this report is printed.
     */
    def findAndInspectNewEntryPoint(classFile: ClassFile) {
        for (method ← classFile.methods) {
            if (!isRecursiveCall(classFile, method, null)) {
                if (!method.body.isEmpty) {
                    val domain = new RootTaintAnalysisDomain(project, taintedFields, CallStackEntry(classFile, method), true)
                    val aiResult = BaseAI.perform(classFile, method, domain)()
                    val log: String = aiResult.domain.report.getOrElse(null)
                    if (log != null)
                        println(log)
                }
            }
        }
    }
}

/**
 * The root domain that one is instanciated for every entry point found
 * as well as when searching for the use of a field
 */
class RootTaintAnalysisDomain[Source](
    val project: Project[Source],
    val taintedGloableFields: List[String],
    val id: CallStackEntry,
    val checkForFields: Boolean)
        extends TaintAnalysisDomain[Source] {
    val callerNode = new SimpleNode("Some user of the API")

    val contextNode: SimpleNode[(RelevantParameters, String)] = {

        taintedFields = taintedGloableFields

        var nextIndex = if (id.method.isStatic) 1 else 2
        var relevantParameters =
            //compute correct index (double, long take two slots) 
            methodDescriptor.parameterTypes.zipWithIndex.map { param_idx ⇒
                val (parameterType, index) = param_idx;
                val currentIndex = nextIndex
                nextIndex += parameterType.computationalType.operandSize
                (parameterType, currentIndex)
                // filter for Strings
            }.filter { param_idx ⇒ param_idx._1 == ObjectType.String
                // map on correct index
            }.map(param_idx ⇒ -(param_idx._2))

        new SimpleNode((relevantParameters, contextIdentifier))
    }

    def isRecursiveCall(
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        parameters: DomainValues): Boolean = {
        this.declaringClass == declaringClass &&
            this.methodName == methodName &&
            this.methodDescriptor == methodDescriptor // &&
        // TODO check that the analysis would be made under the same assumption (same parameters!)
    }

}

/**
 * This class is  instantiated when a Root- or CalledTaintAnalysisDomain decide to further analyse
 * a method resulting.
 */
class CalledTaintAnalysisDomain[Source](
    val previousTaintAnalysisDomain: TaintAnalysisDomain[Source],
    val id: CallStackEntry,
    val callerNode: SimpleNode[_],
    val relevantParameters: RelevantParameters,
    val checkForFields: Boolean)

        extends TaintAnalysisDomain[Source] {

    taintedFields = previousTaintAnalysisDomain.taintedFields
    //cachedInterfaceCalls = previousTaintAnalysisDomain.cachedInterfaceCalls

    val contextNode = new SimpleNode((relevantParameters, contextIdentifier))

    def project = previousTaintAnalysisDomain.project

    /**
     * Checks against all previous instantiated AnalysisDomains in the respective call graph
     * up to the RootTaintAnalysisDomain
     */
    def isRecursiveCall(
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        parameters: DomainValues): Boolean = {
        (this.declaringClass == declaringClass &&
            this.methodName == methodName &&
            this.methodDescriptor == methodDescriptor // && // TODO check that the analysis would be made under the same assumption (same parameters!)    
        ) || (
                declaringClass.isObjectType &&
                previousTaintAnalysisDomain.isRecursiveCall(
                    declaringClass.asObjectType,
                    methodName,
                    methodDescriptor,
                    parameters))

    }
}

