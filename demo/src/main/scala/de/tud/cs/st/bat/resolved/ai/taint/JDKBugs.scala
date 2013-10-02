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

package object taint {

    type CallStackEntry = (ObjectType, String, MethodDescriptor)

}

package taint {

    import java.net.URL
    import analyses.{ Project, Analysis, AnalysisExecutor, ReportableAnalysisResult }
    import de.tud.cs.st.bat.resolved.ai.project.{ AIProject, Report }
    import de.tud.cs.st.bat.resolved.ai.domain._

    object JDKBugs extends AnalysisExecutor {

        val analysis = new JDKTaintAnalysis[URL]

    }

    class JDKTaintAnalysis[Source]
            extends analyses.Analysis[Source, ReportableAnalysisResult]
            with AIProject[Source] {

        def description: String = "Finds unsafe Class.forName(...) calls."

        def entryPoints(project: Project[Source]): Iterable[(ClassFile, Method)] = {
            import ObjectType._
            for {
                classFile ← project.classFiles
                method ← classFile.methods
                if method.isPublic || (method.isProtected && !classFile.isFinal)
                descriptor = method.descriptor
                if (descriptor.returnType == Object) || (descriptor.returnType == Class)
                if (descriptor.parameterTypes.contains(String))
                if method.body.isDefined // let's filter native methods...
            } yield (classFile, method)
        }

        def domain(
            project: analyses.Project[Source],
            classFile: ClassFile,
            method: Method): Domain[_] with Report = {
            new RootTaintAnalysisDomain[Source](project, (classFile.thisClass, method.name, method.descriptor))
        }
    }

    trait TaintAnalysisDomain[Source]
            extends Domain[CallStackEntry]
            with DefaultValueBinding[CallStackEntry]
            with DefaultTypeLevelLongValues[CallStackEntry]
            with DefaultTypeLevelFloatValues[CallStackEntry]
            with DefaultTypeLevelDoubleValues[CallStackEntry]
            with DefaultReturnAddressValues[CallStackEntry]
            with DefaultPreciseIntegerValues[CallStackEntry]
            with DefaultTypeLevelReferenceValuesWithClosedHierarchy[CallStackEntry]
            with StringValues[CallStackEntry]
            with TypeLevelArrayInstructions
            with TypeLevelFieldAccessInstructions
            with TypeLevelInvokeInstructions
            with PerformInvocations[Source]
            with DoNothingOnReturnFromMethod
            with ProjectBasedTypeHierarchyBinding[Source]
            with Report {

        import de.tud.cs.st.util.Unknown
        import ObjectType._

        private var valuePasses: List[String] = List()

        protected def declaringClass = identifier._1
        protected def methodName = identifier._2
        protected def methodDescriptor = identifier._3

        protected def contextIdentifier =
            declaringClass.className+"{"+methodDescriptor.toJava(methodName)+"}"

        def report =
            if (valuePasses.nonEmpty)
                Some(contextIdentifier+" => "+valuePasses.mkString("; "))
            else
                None

        val passedInStringValue = AReferenceValue(-1, String, Unknown, true)

        private def checkCall(
            name: String,
            methodDescriptor: MethodDescriptor,
            operands: List[DomainValue]): Boolean = {
            if (operands.contains(passedInStringValue) &&
                methodDescriptor.returnType == Object || methodDescriptor.returnType == Class) {
                valuePasses = methodDescriptor.toJava(name) :: valuePasses
                true
            } else
                false

        }

        def processResult[D <: TaintAnalysisDomain[Source]](
            aiResult: AIResult[D]): OptionalReturnValueOrExceptions = {
            //        val relevantValues = aiResult.domain.returnedValues.filter{pc_value => 
            //            val (pc,value) = pc_value
            //            
            //        }
            val values = aiResult.domain.returnedValues
            if (values.nonEmpty) {
                val headValue = values.head._2
                ComputedValue(Some(
                    (headValue /: values.tail) { (c, n) ⇒
                        c.merge(n._1, n._2) match {
                            case NoUpdate          ⇒ c
                            case SomeUpdate(value) ⇒ value
                        }
                    }.adapt(this)
                ))
            } else
                ComputationWithSideEffectOnly
        }

        private var returnedValues: Set[(Int, DomainValue)] = Set.empty

        override def areturn(pc: Int, value: DomainValue) {
            returnedValues += ((pc, value))
        }

        override def invokeinterface(pc: Int,
                                     declaringClass: ReferenceType,
                                     name: String,
                                     methodDescriptor: MethodDescriptor,
                                     operands: List[DomainValue]) = {
            checkCall(name, methodDescriptor, operands)
            super.invokeinterface(pc, declaringClass, name, methodDescriptor, operands)
        }

        override def invokevirtual(pc: Int,
                                   declaringClass: ReferenceType,
                                   name: String,
                                   methodDescriptor: MethodDescriptor,
                                   operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
            checkCall(name, methodDescriptor, operands)
            super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)
        }

        override def invokespecial(pc: Int,
                                   declaringClass: ReferenceType,
                                   methodName: String,
                                   methodDescriptor: MethodDescriptor,
                                   operands: List[DomainValue]) = {
            checkCall(methodName, methodDescriptor, operands)
            super.invokespecial(pc, declaringClass, methodName, methodDescriptor, operands)
        }

        override def invokestatic(
            pc: Int,
            declaringClass: ReferenceType,
            methodName: String,
            methodDescriptor: MethodDescriptor,
            operands: List[DomainValue]) = {
            if (declaringClass.isObjectType &&
                checkCall(methodName, methodDescriptor, operands)) {
                val domain = new ChildTaintAnalysisDomain(this, (declaringClass.asObjectType, methodName, methodDescriptor))
                println(domain.callChain)
                doInvokestatic(
                    domain,
                    pc, declaringClass, methodName, methodDescriptor, operands,
                    isRecursiveCall)(
                        processResult[domain.type])
            } else {
                super.invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)
            }
        }

        def isRecursiveCall(
            declaringClass: ReferenceType,
            methodName: String,
            methodDescriptor: MethodDescriptor,
            operands: List[DomainValue]): Boolean

        def isRecursiveCall(
            classFile: ClassFile,
            method: Method,
            operands: List[DomainValue]): Boolean = {
            isRecursiveCall(
                classFile.thisClass,
                method.name,
                method.descriptor,
                operands
            )
        }

        override def putfield(
            pc: Int,
            objectref: DomainValue,
            value: DomainValue,
            declaringClass: ObjectType,
            name: String,
            fieldType: FieldType) = {
            if (value == passedInStringValue)
                valuePasses = declaringClass.className+"."+name :: valuePasses
            super.putfield(pc, objectref, value, declaringClass, name, fieldType)
        }

        override def putstatic(
            pc: Int,
            value: DomainValue,
            declaringClass: ObjectType,
            name: String,
            fieldType: FieldType) = {
            if (value == passedInStringValue)
                valuePasses = declaringClass.className+"."+name :: valuePasses

            super.putstatic(pc, value, declaringClass, name, fieldType)
        }

        def callChain: String
    }

    class RootTaintAnalysisDomain[Source](
            val project: Project[Source],
            val identifier: CallStackEntry) extends TaintAnalysisDomain[Source] {

        def isRecursiveCall(
            declaringClass: ReferenceType,
            methodName: String,
            methodDescriptor: MethodDescriptor,
            operands: List[DomainValue]): Boolean = {
            this.declaringClass == declaringClass &&
                this.methodName == methodName &&
                this.methodDescriptor == methodDescriptor // &&
            // TODO check that the analysis would be made under the same assumption (same parameters!)
        }

        def callChain = contextIdentifier
    }

    class ChildTaintAnalysisDomain[Source](
            val previousTaintAnalysisDomain: TaintAnalysisDomain[Source],
            val identifier: CallStackEntry) extends TaintAnalysisDomain[Source] {

        def callChain: String =
            previousTaintAnalysisDomain.callChain+" => "+contextIdentifier

        def project = previousTaintAnalysisDomain.project

        def isRecursiveCall(
            declaringClass: ReferenceType,
            methodName: String,
            methodDescriptor: MethodDescriptor,
            operands: List[DomainValue]): Boolean = {
            (this.declaringClass == declaringClass &&
                this.methodName == methodName &&
                this.methodDescriptor == methodDescriptor) || (
                    declaringClass.isObjectType &&
                    previousTaintAnalysisDomain.isRecursiveCall(
                        declaringClass.asObjectType,
                        methodName,
                        methodDescriptor,
                        operands.map(_.adapt(previousTaintAnalysisDomain))))

            // TODO check that the analysis would be made under the same assumption (same parameters!)
        }
    }
}