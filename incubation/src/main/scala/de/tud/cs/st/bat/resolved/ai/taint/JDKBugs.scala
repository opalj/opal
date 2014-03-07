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

import instructions._
import domain.l0._
import domain.l1._

import domain.tracing._
import debug.XHTML._

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

  import java.net.URL
  import analyses.{ Project, Analysis, AnalysisExecutor, ReportableAnalysisResult }
  import de.tud.cs.st.bat.resolved.ai.project.{ AIProject, Report }
  import de.tud.cs.st.bat.resolved.ai.domain._
  import de.tud.cs.st.util.graphs._
  import de.tud.cs.st.util.ControlAbstractions.process

  object JDKTaintAnalysis
    extends AIProject[URL, SomeDomain with Report]
    with Analysis[URL, ReportableAnalysisResult]
    with AnalysisExecutor {

    def ai = new AI[SomeDomain with Report] {}

    def description: String = "Finds unsafe< Class.forName(...) calls."

    System.out.println(System.getProperty("java.version"));

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
          map(_.trim.replace('.', '/'))
      }
      println(restrictedPackages.mkString("Restricted packages:\n\t", "\n\t", "\n"))

      super.analyze(project, parameters)
    }

    def entryPoints(project: Project[URL]): Iterable[(ClassFile, Method)] = {
      import ObjectType._
      for {
        classFile ← project.classFiles
        if !definedInRestrictedPackage(classFile.thisType.packageName)
        method ← classFile.methods
        if method.isPublic || (method.isProtected && !classFile.isFinal)
        descriptor = method.descriptor
        if (descriptor.returnType == Object) || (descriptor.returnType == Class)
        if (descriptor.parameterTypes.contains(String))
        if method.body.isDefined // let's filter native methods...
        // Select some specific method for debugging purposes...
        //                if classFile.thisType.className == "com/sun/org/apache/xalan/internal/utils/ObjectFactory"
        //                if method.name == "lookUpFactoryClass"
        //                if classFile.thisType.className == "ai/taint/PermissionCheckNotOnAllPaths"
        //                if method.name == "foo"
        //                if classFile.thisType.className == "com/sun/beans/finder/ClassFinder"
        //                if method.name == "findClass"

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
      new RootTaintAnalysisDomain[URL](project, (classFile, method))
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

    // with DefaultReturnAddressValues[CallStackEntry]
    // //with DefaultPreciseIntegerValues[CallStackEntry]
    with DefaultTypeLevelIntegerValues[CallStackEntry]
    //    with DefaultPreciseReferenceValuesWithClosedHierarchy[CallStackEntry]
    with DefaultStringValuesBinding[CallStackEntry]
    with Configuration
    //    with TypeLevelArrayInstructions
    with TypeLevelFieldAccessInstructions
    with TypeLevelInvokeInstructions
    // //with PerformInvocations[CallStackEntry, Source]
    with ProjectBasedClassHierarchy[Source]
    with Report { thisDomain ⇒

    import de.tud.cs.st.util.Unknown
    import ObjectType._

    protected def declaringClass = identifier._1.thisType
    protected def methodName = identifier._2.name
    protected def methodDescriptor = identifier._2.descriptor

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

    protected var additionalRelevantParameters: List[PC] = List.empty

    protected var taintedFields: List[String] = List.empty

    protected var callToClassForNameFound: Boolean = false;

    var objectTypesWithCreatedInstance: List[ObjectType] = List.empty

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
     * @note Initialized by a call to `postAnalysis()`!
     */
    //private var isRelevantValueReturned = false

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
    protected def postAnalysis(): Boolean = {
      relevantValuesOrigins.foreach { relevantValueOrigin ⇒
        returnedValues.foreach { returnedValue ⇒
          if (origin(returnedValue._2).exists { returnedValueOrigin ⇒
            returnedValueOrigin == relevantValueOrigin._1
          }) {
            //isRelevantValueReturned = true
            relevantValueOrigin._2.addChild(callerNode)
          }
        }
      }
      if (isRelevantValueReturned)
        callerNode.addChild(contextNode)

      isRelevantValueReturned
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
     * Checks if we want to trace the respective method call because a string or a class
     * value is passed on and the method returns either a class object or some
     * object for which we have no further type information.
     */
    private def isRelevantCall(
      pc: Int,
      name: String,
      methodDescriptor: MethodDescriptor,
      operands: List[DomainValue]): Boolean = {
      operands.exists { op ⇒
        // test if one of the relevant parameters was passed to the method
        //        op match {
        //          case AnIntegerValue => false
        //          case ABooleanValue => false
        //          case ALongValue => false
        //          case AByteValue => false
        //          case AShortValue => false
        //          case ACharValue => false
        //          case AFloatValue => false
        //          case _ => 
        origin(op).exists(pc => contextNode.identifier._1.union(additionalRelevantParameters).contains(pc))
        //}
      } && (
        methodDescriptor.returnType == Object ||
        methodDescriptor.returnType == Class)
    }

    override def areturn(pc: Int, value: DomainValue) {
      //In case a forClass instance is given to a Method and returned from it
      if (origin(value).exists(x => x == -1 || x == -2)) {
        relevantValuesOrigins = (-1, new SimpleNode("return of a relevant Parameter")) :: relevantValuesOrigins
      }

      returnedValues = (pc, value) :: returnedValues
    }

    override def invokeinterface(pc: Int,
      declaringClass: ObjectType,
      methodName: String,
      methodDescriptor: MethodDescriptor,
      operands: List[DomainValue]): MethodCallResult = {

      //      println("Interface invoked - with:")
      //      println("Declaring Class: " + declaringClass)
      //      println("methodName: " + methodName)
      //      println("operands: " + operands)
      //      println("pc: " + pc)
      //      println("")

      def doTypeLevelInvoke =
        super.invokeinterface(pc, declaringClass, methodName, methodDescriptor, operands)

      if (isIrelevantInvoke(methodDescriptor, declaringClass))
        return doTypeLevelInvoke;

      val relevantOperands = computeRelevantOperands(operands)
      if (relevantOperands.isEmpty)
        return doTypeLevelInvoke;

      println("has relevant operands")

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

      // look up every class that implements the method and was previously
      // instanciated
      val implementingMethods = classHierarchy.lookupImplementingMethods(
        declaringClass.asObjectType,
        methodName,
        methodDescriptor,
        project,
        (objectTypesWithCreatedInstance.contains(_)))

      // deepen the search for every found method implementation
      implementingMethods.foreach { (m: Method) =>
        {
          val classFile = project.classFile(m)

          if (method.isNative)
            return doTypeLevelInvoke;

          deepenSearch(pc, classFile, m, operands)

        }
      }
      return doTypeLevelInvoke

    }

    override def invokevirtual(pc: Int,
      declaringClass: ReferenceType,
      name: String,
      methodDescriptor: MethodDescriptor,
      operands: List[DomainValue]): MethodCallResult = {

      //      println("Warning: virtual invoked - with:")
      //      println("Declaring Class: " + declaringClass)
      //      println("methodName: " + name)
      //      println("operands: " + operands)
      //      println("")

      def doTypeLevelInvoke =
        super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)

      // check if we have a call to Class.newInstance...
      if (name == "newInstance") {
        if (isRelevantCall(pc, name, methodDescriptor, operands)) {
          relevantValuesOrigins = (pc, new SimpleNode("newInstance")) :: relevantValuesOrigins
        }
      }

      return doTypeLevelInvoke
    }

    override def invokespecial(
      pc: Int,
      declaringClass: ObjectType,
      methodName: String,
      methodDescriptor: MethodDescriptor,
      operands: List[DomainValue]): MethodCallResult = {

      println("Special invoked - with:")
      println("Declaring Class: " + declaringClass)
      println("methodName: " + methodName)
      println("operands: " + operands)
      println("pc: " + pc)
      println("")

      def doTypeLevelInvoke =
        super.invokespecial(pc, declaringClass, methodName, methodDescriptor, operands)

      // check if its an instance creation - in this case we need to save
      // the objectType so we can check for it in case of invokeInterface
      if (methodName == "<init>") {
        if (!objectTypesWithCreatedInstance.contains(declaringClass))
          objectTypesWithCreatedInstance = declaringClass :: objectTypesWithCreatedInstance
      }

      if (isIrelevantInvoke(methodDescriptor, declaringClass))
        return doTypeLevelInvoke;

      val relevantOperands = computeRelevantOperands(operands)
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

      val classFile = project.classFile(method)

      if (method.isNative)
        return doTypeLevelInvoke;

      deepenSearch(pc, classFile, method, operands)

      return doTypeLevelInvoke
    }

    override def invokestatic(
      pc: Int,
      declaringClass: ObjectType,
      methodName: String,
      methodDescriptor: MethodDescriptor,
      operands: List[DomainValue]): MethodCallResult = {

      //      println("Static invoked - with:")
      //      println("Declaring Class: " + declaringClass)
      //      println("methodName: " + methodName)
      //      println("operands: " + operands)
      //      println("pc: " + pc)
      //      println("")

      def doTypeLevelInvoke =
        super.invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

      if (isIrelevantInvoke(methodDescriptor, declaringClass))
        return doTypeLevelInvoke;

      val relevantOperands = computeRelevantOperands(operands)
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

      val classFile = project.classFile(method)

      if (method.isNative)
        return doTypeLevelInvoke;

      deepenSearch(pc, classFile, method, operands)

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

    override def putstatic(
      pc: PC,
      value: DomainValue,
      declaringClass: ObjectType,
      name: String,
      fieldType: FieldType) = {

      if (contextNode.identifier._1.union(additionalRelevantParameters).intersect(origin(value).toSeq).nonEmpty) {
        taintedFields = name :: taintedFields

        // it dossnt get a tainted value so check if it was tainted before and remove it from the tainted list
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

      if (taintedFields.contains(name)) {
        additionalRelevantParameters = pc :: additionalRelevantParameters
      }

      super.getstatic(pc, declaringClass, name, fieldType)
    }

    def isIrelevantInvoke(methodDescriptor: MethodDescriptor, declaringClass: ObjectType): Boolean = {
      (!declaringClass.isObjectType || (
        methodDescriptor.returnType != Class &&
        methodDescriptor.returnType != Object))
    }

    def computeRelevantOperands(operands: List[DomainValue]) = {
      operands.zipWithIndex.filter { operand_index =>
        val (operand, index) = operand_index
        origin(operand).exists { operandOrigin =>
          contextNode.identifier._1.union(additionalRelevantParameters).exists(_ == operandOrigin)
        }
      }
    }

    /*
     * check if we found a call to Class.forName
     */
    def checkForSink(declaringClass: ObjectType, methodDescriptor: MethodDescriptor, methodName: String): Boolean = {
      (declaringClass == ObjectType.Class &&
        methodName == "forName" &&
        methodDescriptor.parameterTypes == Seq(String))
    }

    /*
     * create a sinkNode into the call graph and add the pc to additionalRelevantParameters
     * and relevantValuesOrigins
     */
    def registerSink(pc: PC, operands: List[DomainValue]) = {
      val sinkNode = new SimpleNode(pc + ": Class.forName(" + operands.head + ")")
      contextNode.addChild(sinkNode)
      //TODO Continue search
      additionalRelevantParameters = pc :: additionalRelevantParameters
      relevantValuesOrigins = (pc, sinkNode) :: relevantValuesOrigins
      println("Call to class for name found...\n")
    }

    def deepenSearch(
      pc: PC,
      classFile: ClassFile,
      method: Method,
      operands: List[DomainValue]) = {

      val callerNode = new SimpleNode(pc + ": method invocation; method id: " + method.id)
      val calleeDomain = new CalledTaintAnalysisDomain(
        this,
        (classFile, method),
        callerNode,
        //relevantOperands.map(_._2))
        //relevantOperands.map(x => origin(x._1).head))
        List(-1, -2))

      val calleeParameters = operands.reverse.zipWithIndex.map { operand_index ⇒
        val (operand, index) = operand_index
        operand.adapt(calleeDomain, -(index + 1))
      }.toArray(calleeDomain.DomainValueTag)
      val parameters = DomainValues(calleeDomain)(calleeParameters)
      if (isRecursiveCall(classFile, method, parameters)) {
        //return doTypeLevelInvoke;
      } else {

        println("Relevant call found: " + method.toJava)

        // If we reach this point, we have an invocation of a relevant method 
        // with a relevant parameter that is not our final sink and which is
        // not native and which is not a recursive call

        val v = method.body.get

        val aiResult = BaseAI.perform(classFile, method, calleeDomain)(Some(calleeParameters))
        aiResult.domain.postAnalysis()
        if (!aiResult.domain.isRelevantValueReturned) {
          println("No relevant Value returned!")
          println("")
          // return doTypeLevelInvoke;
        } else {

          callToClassForNameFound = true;
          val returnNode = new SimpleNode(pc + ": returned value from : " + aiResult.domain.contextIdentifier)
          contextNode.addChild(returnNode)
          println(pc + ": returned relevant value from : " + aiResult.domain.contextIdentifier)
          contextNode.addChild(callerNode)
          if (this.callerNode.identifier == "Some user of the API") {
            println("forClass Instance returned to User! \n")
          }
          relevantValuesOrigins = (pc, returnNode) :: relevantValuesOrigins
          //          val headValue = aiResult.domain.returnedValues.head._2
          //          val mergedValue = ComputedValue(Some(
          //            (headValue /: aiResult.domain.returnedValues.tail) { (c, n) ⇒
          //              c.join(pc, n._2) match {
          //                case NoUpdate => c
          //                case SomeUpdate(value) ⇒ value
          //              }
          //            }.adapt(this, pc)))
          //          mergedValue
        }
      }
    }
  }

  class RootTaintAnalysisDomain[Source](
    val project: Project[Source],
    val identifier: (ClassFile, Method))
    extends TaintAnalysisDomain[Source] {
    val callerNode = new SimpleNode("Some user of the API")

    val contextNode: SimpleNode[(RelevantParameters, String)] = {

      objectTypesWithCreatedInstance = List.empty;

      val firstIndex = if (identifier._2.isStatic) 1 else 2
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
    val relevantParameters: RelevantParameters)

    extends TaintAnalysisDomain[Source] {

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
}