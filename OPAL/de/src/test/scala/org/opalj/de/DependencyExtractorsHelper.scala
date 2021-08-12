/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.VirtualClass
import org.opalj.br.VirtualMethod
import org.opalj.br.VirtualField
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.VirtualSourceElement
import org.opalj.br.Type

/**
 * Functionality useful when testing a dependency extractor.
 *
 * @author Michael Eichberg
 */
object DependencyExtractorsHelper {

    val FIELD_AND_METHOD_SEPARATOR = "."

    def sourceElementName(t: Type): String = {
        if (t.isArrayType) t.asArrayType.elementType.toJava else t.toJava
    }

    def sourceElementName(vClass: VirtualClass): String = sourceElementName(vClass.thisType)

    def sourceElementName(vField: VirtualField): String = {
        sourceElementName(vField.declaringClassType) + FIELD_AND_METHOD_SEPARATOR + vField.name
    }

    def sourceElementName(vMethod: VirtualMethod): String = {
        sourceElementName(vMethod.declaringClassType) +
            FIELD_AND_METHOD_SEPARATOR +
            methodDescriptorToString(vMethod.name, vMethod.descriptor)
    }

    def methodDescriptorToString(name: String, descriptor: MethodDescriptor): String = {
        descriptor.parameterTypes.map { sourceElementName(_) }.mkString(name+"(", ", ", ")")
    }

    def vseToString(vse: VirtualSourceElement): String = {
        vse match {
            case vc: VirtualClass  => sourceElementName(vc)
            case vm: VirtualMethod => sourceElementName(vm)
            case vf: VirtualField  => sourceElementName(vf)
        }
    }

    def extractDependencies(
        folder:                    String,
        jarFile:                   String,
        createDependencyExtractor: (DependencyProcessor) => DependencyExtractor
    ): Map[(String, String, DependencyType), Int] = {
        var dependencies: Map[(String, String, DependencyType), Int] = Map.empty

        val dependencyExtractor =
            createDependencyExtractor(
                new DependencyProcessorAdapter() {

                    override def processDependency(
                        source: VirtualSourceElement,
                        target: VirtualSourceElement,
                        dType:  DependencyType
                    ): Unit = {
                        val key = ((vseToString(source), vseToString(target), dType))
                        dependencies = dependencies.updated(
                            key,
                            dependencies.getOrElse(key, 0) + 1
                        )
                    }
                    override def processDependency(
                        source: VirtualSourceElement,
                        target: ArrayType,
                        dType:  DependencyType
                    ): Unit = {
                        if (target.elementType.isObjectType) {
                            processDependency(
                                source, VirtualClass(target.elementType.asObjectType), dType
                            )
                        }
                    }
                }
            )
        def resources() = locateTestResources(jarFile, folder)
        for ((classFile, _) <- ClassFiles(resources())) {
            dependencyExtractor.process(classFile)
        }
        dependencies
    }

}
