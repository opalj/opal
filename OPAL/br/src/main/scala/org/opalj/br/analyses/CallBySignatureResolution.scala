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
package br
package analyses

import scala.collection.Map
import scala.collection.mutable.AnyRefMap
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext

/**
 * An index that enables the efficient lookup of potential
 * call by signature resolution interface methods
 * given the method's name and the descriptor type.
 *
 * @note To get call by signature resolution information call [[Project.get]] and pass in
 * 		the [[CallBySignatureResolutionKey]] object.
 *
 * @author Michael Reif
 */
class CallBySignatureResolution private (
        val project: SomeProject,
        val methods: Map[String, Map[MethodDescriptor, Iterable[Method]]]
) {

    /**
     * Given the `name` and `descriptor` of a method declared by an interface, all those
     * methods are returned that have a matching name and descriptor and which are implemented
     * by classes (not interfaces) that '''do not inherit''' from the respective interface and
     * which may have a subclass (in the future) that may implement the interface.
     *
     * Hence, when we compute the call graph for a library the returned methods may (in general)
     * be call targets.
     */
    def findMethods(name: String, descriptor: MethodDescriptor): Iterable[Method] =
        methods.get(name).flatMap(_.get(descriptor)).getOrElse(Iterable.empty)

    def statistics(): Map[String, Any] = {
        Map(
            "number of method names" →
                methods.size,
            "number of different method name/descriptor pairs" →
                methods.view.map(kv ⇒ kv._2.size).sum,
            "number of class methods with method signatures matching non-implemented interface methods" →
                methods.view.map(kv ⇒ kv._2.map(kv ⇒ kv._2.size).sum).sum
        )
    }

    def methodReferenceStatistics(): Iterable[String] = {
        for {
            (name, descriptorToMethods) ← methods
            (descriptor, methods) ← descriptorToMethods
        } yield {
            val methodInfo = methods.map(project.classFile(_).thisType.toJava).mkString("classes={", ",", "}")
            s"${descriptor.toJava(name)} => $methodInfo"
        }
    }
}

/**
 * Factory to create [[CallBySignatureResolution]] information.
 *
 * @author Michael Reif
 */
object CallBySignatureResolution {

    def apply(project: SomeProject): CallBySignatureResolution = {
        val projectIndex = project.get(ProjectIndexKey)
        val classHierarchy = project.classHierarchy

        val methods = new AnyRefMap[String /*an interface method name*/ , AnyRefMap[MethodDescriptor /* an interface method descriptor */ , Set[Method]] /*methods defined in classes with matching signatures*/ ](project.methods.size / 3)
        def callBySignatureEvaluation(interfaceClassFile: ClassFile, interfaceMethod: Method): Unit = {
            val methodName = interfaceMethod.name
            val methodDescriptor = interfaceMethod.descriptor
            val interfaceType = interfaceClassFile.thisType

            def analyzeMethod(m: Method): Unit = {
                if (m.isAbstract)
                    return ;

                val clazzClassFile = project.classFile(m)
                if (!clazzClassFile.isClassDeclaration)
                    return ;
                if (clazzClassFile.isFinal)
                    return ;

                val clazzType = clazzClassFile.thisType

                if (!interfaceClassFile.isPublic && interfaceType.packageName != clazzType.packageName)
                    return ;

                if (classHierarchy.isSubtypeOf(clazzType, interfaceType).isYesOrUnknown)
                    return ;

                println(s"${interfaceMethod.toJava(interfaceClassFile)} => ${project.classFile(m).thisType.toJava}")

                methods.get(methodName) match {
                    case None ⇒
                        val descriptorToMethods = new AnyRefMap[MethodDescriptor, Set[Method]](2)
                        descriptorToMethods.update(methodDescriptor, Set(m))
                        methods.update(methodName, descriptorToMethods)
                    case Some(descriptorToMethods) ⇒
                        descriptorToMethods.get(methodDescriptor) match {
                            case None ⇒
                                descriptorToMethods.put(methodDescriptor, Set(m))
                            case Some(theMethods) ⇒
                                descriptorToMethods.put(methodDescriptor, theMethods + m)
                            case _ ⇒
                        }
                }
            }

            projectIndex.findMethods(methodName, methodDescriptor) foreach analyzeMethod
        }

        for {
            cf ← project.allClassFiles if cf.isInterfaceDeclaration
            m ← cf.methods if !m.isStatic // this includes (in particular) the static initializer
        } {
            callBySignatureEvaluation(cf, m)
        }

        for {
            cf ← project.allClassFiles if cf.isInterfaceDeclaration
            Method(_, "toString", MethodDescriptor.JustReturnsString) ← cf.methods
        } println(cf.thisType.toJava)

        methods.values.foreach(_.repack())
        methods.repack()
        new CallBySignatureResolution(project, methods)
    }

}