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
package br
package ai
package taint

import instructions._
import domain._
import domain.l0._
import domain.l1._
import domain.tracing._
import debug.XHTML._

import analyses.{ Project, Analysis, AnalysisExecutor, ReportableAnalysisResult }
import project.{ AIProject, Report }
import de.tud.cs.st.util.graphs._
import de.tud.cs.st.util.ControlAbstractions.process

import java.net.URL

/**
 * Searches for occurrences of the Class.forName bug in the JDK
 * 
 * @author Lars Schulte
 */
object JDKTaintAnalysis
  extends AIProject[URL, SomeDomain with Report]
  with Analysis[URL, ReportableAnalysisResult]
  with AnalysisExecutor {

  def ai = new AI[SomeDomain with Report] {}

  def description: String = "Finds unsafe Class.forName(...) calls."

  println(System.getProperty("java.version"));

  val analysis = this

  val javaSecurityParameter = "-java.security="

  var javaSecurityFile: String = _

  override def analysisParametersDescription: String =
    javaSecurityParameter + "<JRE/JDK Security Policy File>"

  override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
    parameters.size == 1 && {
      javaSecurityFile = parameters.head.substring(javaSecurityParameter.length())
      new java.io.File(javaSecurityFile).exists()
    }

  override def analyze(
    project: analyses.Project[URL],
    parameters: Seq[String]): ReportableAnalysisResult = {
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
    method: Method): Domain[CallStackEntry] with Report = {
    new RootTaintAnalysisDomain[URL](project, List.empty, CallStackEntry(classFile, method), false)
  }
}

trait TaintAnalysisDomain[Source]
  extends Domain[CallStackEntry]
  with IgnoreMethodResults
  with IgnoreSynchronization
  with DefaultDomainValueBinding[CallStackEntry]
  with DefaultTypeLevelLongValues[CallStackEntry]
  with DefaultTypeLevelFloatValues[CallStackEntry]
  with DefaultTypeLevelDoubleValues[CallStackEntry]
  with DefaultTypeLevelIntegerValues[CallStackEntry]
  with DefaultStringValuesBinding[CallStackEntry]
  with Configuration
  with TypeLevelFieldAccessInstructions
  with TypeLevelInvokeInstructions
  with ProjectBasedClassHierarchy[Source]
  with Report { thisDomain ⇒

  import de.tud.cs.st.util.Unknown
  import ObjectType._

  protected def declaringClass = identifier.classFile.thisType
  protected def methodName = identifier.method.name
  protected def methodDescriptor = identifier.method.descriptor

  protected def contextIdentifier =
    declaringClass.fqn + "{ " + methodDescriptor.toJava(methodName) + " }"

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
   * predicates if the analysis is currently in the process of looking
   * for the use of a previously tainted global field. If this is true then this
   * boolean prevents the analysis from analyzing "putfield" instructions that
   * could lead the analysis to run into an endless loop
   */
  val checkForGlobalFields: Boolean

  /**
   * Stores if a call to Class.forName has occurred. No report is created if this is not true
   */
  protected var callToClassForNameFound: Boolean = false;

  /**
   * Stores all objects that are know to be instantiated.
   * This helps to reduce the number of classes that have
   * to be checked in case of invokeinterface
   */
  var objectTypesWithCreatedInstance: List[ObjectType] = List.empty

  case class CachedInterfaceCall(methodName: String, methodDescriptor: MethodDescriptor, operands: List[DomainValue], result: Boolean)
  
  protected var cachedInterfaceCalls: List[CachedInterfaceCall] = List.empty 
  
  
  /**
   * @note
   * Calling this method only makes sense when the analysis of this domain's method
   * has completed.
   */
  def isRelevantValueReturned(): Boolean = {
    relevantValuesOrigins.exists { relevantValueOrigin =>
      returnedValues.exists { returnedValue =>
        origin(returnedValue._2).exists { returnedValueOrigin =>
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
  private def postAnalysis(): Boolean = {
    relevantValuesOrigins.foreach { relevantValueOrigin =>
      returnedValues.foreach { returnedValue =>
        if (origin(returnedValue._2).exists { returnedValueOrigin =>
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
      Some(toDot.generateDot(Set(callerNode)))
    } else
      None
  }

  /**
   * Checks if we want to trace the respective method call because a relevant string or a class
   * value is passed on and the method returns either a class object or some
   * object for which we have no further type information.
   */
  private def isRelevantCall(
    pc: Int,
    name: String,
    methodDescriptor: MethodDescriptor,
    operands: List[DomainValue]): Boolean = {
    operands.exists { op =>
      origin(op).exists(pc => contextNode.identifier._1.union(taintedPCs).contains(pc))
    } && (
      methodDescriptor.returnType == Object ||
      methodDescriptor.returnType == Class)
  }

  override def areturn(pc: Int, value: DomainValue) {
    // in case a relevant parameter is returned by the method
    if (origin(value).exists(x => x == -1 || x == -2)) {
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
    
//    println(pc)
//    println(declaringClass)
//    println(methodName)
//    println(methodDescriptor)
//    println(operands)
//    println()

    def doTypeLevelInvoke =
      super.invokeinterface(pc, declaringClass, methodName, methodDescriptor, operands)
    
    if (isIrrelevantInvoke(methodDescriptor, declaringClass))
      return doTypeLevelInvoke;

    val relevantOperands = computeRelevantOperands(operands)
    if (relevantOperands.isEmpty)
      return doTypeLevelInvoke;

    var temp = cachedInterfaceCalls.filter(x => 
      x.methodDescriptor == methodDescriptor && 
      x.methodName == methodName &&
      x.operands == operands)
      
    if(temp != null){
      if (temp.exists(x => x.result == true)){
      } else {
      }
      return doTypeLevelInvoke;
    }
    
    // If we reach this point, we have an invocation of a relevant method 
    // with a relevant parameter that is not our final sink...

    val method: Method =
      classHierarchy.resolveInterfaceMethodReference(
        declaringClass.asObjectType,
        methodName,
        methodDescriptor,
        project).getOrElse {
          return doTypeLevelInvoke
        }
    
        val filter : (ObjectType => Boolean) = operands.last match {
    	case x : SObjectValue => 
    	  if(x.isPrecise){
    	    (x.upperTypeBound.contains(_))
    	  } else {
    	    (_ => true)
    	  }
    	case _ => {
    	  //println("No SObjectValue: " + operands + " " + declaringClass+ " " + methodName) 
    	  (_ => false )
    	} 
    }
    
    
    // look up every class that implements the method and was previously
    // instanced
    val implementingMethods = classHierarchy.lookupImplementingMethods(
      declaringClass.asObjectType,
      methodName,
      methodDescriptor,
      project,
      filter)
      
      
    // analyze every found method implementation
    implementingMethods.foreach { (m: Method) =>
      {
    	    val call = CachedInterfaceCall(methodName, methodDescriptor, operands, inspectMethod(pc, m, operands))
    	    cachedInterfaceCalls = call :: cachedInterfaceCalls
      }
    }
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
      if (isRelevantCall(pc, methodName, methodDescriptor, operands)) {
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

    // check if its an instance creation - in this case we need to save
    // the objectType so we can check for it in case of invokeInterface
    if (methodName == "<init>") {
      if (!objectTypesWithCreatedInstance.contains(declaringClass)) {
        objectTypesWithCreatedInstance = declaringClass :: objectTypesWithCreatedInstance
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

    // skip if we already check for global fields so we don't run into a loop
    if (!checkForGlobalFields) {
      // check if the global field is set with a relevant value
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

    // checkk if a tainted field is read
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
      methodDescriptor.returnType != Object))
    // currently disabled 
    // methodDescriptor.returnType != String

  }

  /**
   * Returns all parameters that could be tainted
   */
  def computeRelevantOperands(operands: List[DomainValue]) = {
    operands.zipWithIndex.filter { operand_index =>
      val (operand, index) = operand_index
      origin(operand).exists { operandOrigin =>
        contextNode.identifier._1.union(taintedPCs).exists(_ == operandOrigin)
      }
    }
  }

  /**
   * check if we found a relevant call to forName
   */
  def checkForSink(declaringClass: ObjectType, methodDescriptor: MethodDescriptor, methodName: String): Boolean = {
    (declaringClass == ObjectType.Class &&
      methodName == "forName" &&
      methodDescriptor.parameterTypes == Seq(String))
  }

  /**
   * create a sinkNode into the call graph and add the PC to additionalRelevantParameters
   * and relevantValuesOrigins
   */
  def registerSink(pc: PC, operands: List[DomainValue]) = {
    val sinkNode = new SimpleNode(pc + ": Class.forName(" + operands.head + ")")
    contextNode.addChild(sinkNode)
    callToClassForNameFound = true;
    taintedPCs = pc :: taintedPCs
    relevantValuesOrigins = (pc, sinkNode) :: relevantValuesOrigins
  }

  /**
   * create a new taint analysis for the specified method
   * returns true if the analysis was succesfull
   */
  def inspectMethod(
    pc: PC,
    method: Method,
    operands: List[DomainValue]) : Boolean = {

    val classFile = project.classFile(method)

    if (!method.isNative) {

      val callerNode = new SimpleNode(pc + ": method invocation; method id: " + method.id)
      val calleeDomain = new CalledTaintAnalysisDomain(
        this,
        CallStackEntry(classFile, method),
        callerNode,
        List(-1, -2),
        checkForGlobalFields)

      val calleeParameters = operands.reverse.zipWithIndex.map { operand_index ⇒
        val (operand, index) = operand_index
        operand.adapt(calleeDomain, -(index + 1))
      }.toArray(calleeDomain.DomainValueTag)
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
        	aiResult.domain.postAnalysis
          if (aiResult.domain.callToClassForNameFound) {
            callToClassForNameFound = true;
          }
          // set return nodes
          val returnNode = new SimpleNode(pc + ": returned value from : " + aiResult.domain.contextIdentifier)
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
   * finds new entry points that use a tainted field
   */
  def findAndInspectNewEntryPoint(classFile: ClassFile) {
    for (method <- classFile.methods) {

      // val callerNode = new SimpleNode(":Some user of the API after global field set " + method.id)
      if (!isRecursiveCall(classFile, method, null)) {
        if (!method.body.isEmpty) {

          val domain = new RootTaintAnalysisDomain(project, taintedFields, CallStackEntry(classFile, method), true)

          val aiResult = BaseAI.perform(classFile, method, domain)()
          //aiResult.domain.postAnalysis()
          val log: String = aiResult.domain.report.getOrElse(null)
          if (log != null)
            println(log)
        }
      }
    }
  }
}

class RootTaintAnalysisDomain[Source](
  val project: Project[Source],
  val taintedGloableFields: List[String],
  val identifier: CallStackEntry,
  val checkForGlobalFields: Boolean)
  extends TaintAnalysisDomain[Source] {
  val callerNode = new SimpleNode("Some user of the API")

  val contextNode: SimpleNode[(RelevantParameters, String)] = {

    objectTypesWithCreatedInstance = List.empty;
    taintedFields = taintedGloableFields

    val firstIndex = if (identifier.method.isStatic) 1 else 2
    val relevantParameters = {
      methodDescriptor.parameterTypes.zipWithIndex.filter { param_idx =>
        val (parameterType, _) = param_idx;
        parameterType == ObjectType.String
      }.map(param_idx => -(param_idx._2 + firstIndex))
    }
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

class CalledTaintAnalysisDomain[Source](
  val previousTaintAnalysisDomain: TaintAnalysisDomain[Source],
  val identifier: CallStackEntry,
  val callerNode: SimpleNode[_],
  val relevantParameters: RelevantParameters,
  val checkForGlobalFields: Boolean)

  extends TaintAnalysisDomain[Source] {
  
  objectTypesWithCreatedInstance = previousTaintAnalysisDomain.objectTypesWithCreatedInstance
  taintedFields = previousTaintAnalysisDomain.taintedFields
  objectTypesWithCreatedInstance = previousTaintAnalysisDomain.objectTypesWithCreatedInstance

  val contextNode = new SimpleNode((relevantParameters, contextIdentifier))

  def project = previousTaintAnalysisDomain.project

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

