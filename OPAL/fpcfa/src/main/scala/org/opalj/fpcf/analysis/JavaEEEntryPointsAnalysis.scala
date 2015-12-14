/**
 * BSD 2-Clause License:
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

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.br.ClassFile
import scala.collection.mutable.ListBuffer
import org.opalj.br.analyses.InjectedClassesInformationKey
import org.opalj.br.Method

class JavaEEEntryPointsAnalysis private (
    val project: SomeProject
) extends {
    private[this] final val AccessKey = ProjectAccessibility.key
    private[this] final val InstantiabilityKey = Instantiability.key
    private[this] final val CallableFromClassesInOtherPackagesKey = CallableFromClassesInOtherPackages.key
    private[this] final val SerializableType = ObjectType.Serializable
    private[this] final val InjectedClasses = project.get(InjectedClassesInformationKey)
    private[this] final val ExceptionHandlerFactory = ObjectType("javax.faces.context.ExceptionHandlerFactory")
} with FPCFAnalysis {

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineProperty(classFile: ClassFile): PropertyComputationResult = {

        if (project.isLibraryType(classFile))
            //we are not interested in library classFiles
            return NoResult;

        val isAnnotated = classFile.annotations.size > 0
        val willBeInjected = InjectedClasses.isInjected(classFile)
        val result = ListBuffer.empty[(Entity, Property)]
        val isWebFactory = project.classHierarchy.isSubtypeOf(classFile.thisType, ExceptionHandlerFactory).isYesOrUnknown
        classFile.methods.filter { m ⇒ !m.isAbstract && !m.isNative }.foreach { method ⇒

            if (CallGraphFactory.isPotentiallySerializationRelated(classFile, method)(project.classHierarchy)) {
                result += ((method, IsEntryPoint))
            } else if (method.isConstructor && willBeInjected) {
                result += ((method, IsEntryPoint))
            } else if (method.isPrivate) {
                result += ((method, NoEntryPoint))
            } else if (method.isStaticInitializer) {
                result += ((method, IsEntryPoint))
            } else if (isAnnotated) {
                result += ((method, IsEntryPoint))
            } else if (hasAnnotatedSubtypeAndInheritsMethod(classFile, method)) {
                result += ((method, IsEntryPoint))
            } else if (isWebFactory && !method.isPrivate) {
                result += ((method, IsEntryPoint))
            }
        }

        ImmediateMultiResult(result.toSet)
    }

    def hasAnnotatedSubtypeAndInheritsMethod(classFile: ClassFile, method: Method): Boolean = {

        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        val subtypes = ListBuffer.empty ++= classHierarchy.directSubtypesOf(classFile.thisType)
        while (subtypes.nonEmpty) {
            val subtype = subtypes.head
            project.classFile(subtype) match {
                case Some(subclass) if subclass.isClassDeclaration || subclass.isEnumDeclaration ⇒
                    val isAnnotated = subclass.annotations.size > 0
                    val inheritsMethod = subclass.findMethod(methodName, methodDescriptor).isEmpty
                    if (inheritsMethod) {
                        if (isAnnotated)
                            return true;
                        else
                            subtypes ++= classHierarchy.directSubtypesOf(subtype)
                    }
                case _ ⇒
            }
            subtypes -= subtype
        }

        false
    }
}

object JavaEEEntryPointsAnalysis extends FPCFAnalysisRunner {

    val injectAnnotation = ObjectType("javax.inject.Inject")

    final def entitySelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }

    override def derivedProperties: Set[PropertyKind] = Set(EntryPoint)

    /*
     * This recommendations are not transitive. All (even indirect) dependencies are listed here.
     */
    override def recommendations: Set[FPCFAnalysisRunner] = {
        Set()
    }

    def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new JavaEEEntryPointsAnalysis(project)
        propertyStore <||< (entitySelector, analysis.determineProperty)
        analysis
    }
}
