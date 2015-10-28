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
package org.opalj.br
package analyses

import scala.collection.Map
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext

/**
 * An index that enables the efficient lookup of potential
 * call by signature resolution interface methods
 * given the method's name and the descriptor type.
 *
 * To get an instance of this call by signature resolution
 * call [[Project.get]] and pass in the [[CallBySignatureResolutionKey]]
 * object.
 *
 * @author Michael Reif
 */
class CallBySignatureResolution private (
        val methods: Map[String, Map[MethodDescriptor, Iterable[Method]]]) {

    def findMethods(name: String, descriptor: MethodDescriptor): Iterable[Method] =
        methods.get(name).flatMap(_.get(descriptor)).getOrElse(Iterable.empty)

    def findMethods(name: String): Iterable[Method] =
        methods.get(name).map(_.values.flatten).getOrElse(Iterable.empty)

    def statistics(): Map[String, Any] = {

        // print a list of methods
        //        var i = -1
        //        var sum = 0
        //        methods.view foreach { kv ⇒
        //            kv._2 foreach { innerKv ⇒
        //                sum += innerKv._2.size
        //                i += 1
        //                OPALLogger.info(
        //                    s"method $i", innerKv._1.toJava(kv._1) + s"[${innerKv._2.size}][${innerKv._2.map(_.name).mkString("[", ",", "]")}]"
        //                )(GlobalLogContext)
        //            }
        //        }

        Map(
            "number of method names" ->
                methods.view.size,
            "number of different method name/descriptor pairs" ->
                methods.view.map(kv ⇒ kv._2.size).sum,
            "overall number of additionall call edges (if every method is invoked by name only once)" ->
                methods.view.map(kv ⇒ kv._2.map(kv ⇒ kv._2.size).sum * kv._2.size).sum,
            "overall number of additionall call edges (if every method is invoked by signature only once)" ->
                methods.view.map(kv ⇒ kv._2.map(kv ⇒ kv._2.size).sum).sum
        )
    }
}

/**
 * Factory for [[CallBySignatureResolution]] objects.
 *
 * @author Michael Reif
 */
object CallBySignatureResolution {

    def apply(project: SomeProject): CallBySignatureResolution = {
        import scala.collection.mutable.AnyRefMap

        val methods: AnyRefMap[String, AnyRefMap[MethodDescriptor, List[Method]]] = {
            val methods = new AnyRefMap[String, AnyRefMap[MethodDescriptor, List[Method]]](project.methods.size / 3)

            val projectIndex = project.get(ProjectIndexKey)

            def callBySignatureEvaluation(classFile: ClassFile, method: Method): Unit = {
                val methodName = method.name
                val methodDescriptor = method.descriptor
                val interfaceType = classFile.thisType

                val potentielMethods = projectIndex.findMethods(methodName, methodDescriptor)

                potentielMethods foreach { m ⇒
                    val curClassFile = project.classFile(m)
                    val classType = curClassFile.thisType
                    if (curClassFile.isClassDeclaration && !curClassFile.isFinal && (interfaceType ne classType)) {
                        if (classFile.isPublic || interfaceType.packageName == classType.packageName) {
                            val implementsInterface = project.classHierarchy.isSubtypeOf(classType, interfaceType).isYesOrUnknown
                            if (!implementsInterface) {
                                if (!implementsInterface) {
                                    methods.get(methodName) match {
                                        case None ⇒
                                            val descriptorToField = new AnyRefMap[MethodDescriptor, List[Method]](2)
                                            descriptorToField.update(methodDescriptor, List(method))
                                            methods.update(methodName, descriptorToField)
                                        case Some(descriptorToField) ⇒
                                            descriptorToField.get(methodDescriptor) match {
                                                case None ⇒
                                                    descriptorToField.put(methodDescriptor, List(method))
                                                case Some(theMethods) if theMethods.contains(method) ⇒
                                                    descriptorToField.put(methodDescriptor, method :: theMethods)
                                                case _ ⇒
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for {
                cf ← project.allClassFiles if cf.isInterfaceDeclaration
                m ← cf.methods if m.name != "<clinit>"
            } {
                callBySignatureEvaluation(cf, m)
            }
            methods
        }

        new CallBySignatureResolution(methods)
    }

}