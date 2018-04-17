/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package ba

import org.opalj.br.MethodSignature
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UShortPair

/**
 * Builder for [[org.opalj.br.ClassFile]] objects.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
class CLASS[T](
        version:         UShortPair,
        accessModifiers: AccessModifier,
        thisType:        String,
        superclassType:  Option[String],
        interfaceTypes:  Seq[String],
        fields:          FIELDS,
        methods:         METHODS[T],
        attributes:      Seq[br.ClassFileAttributeBuilder]
) {

    /**
     * Builds the [[org.opalj.br.ClassFile]] given the current information.
     *
     * The following conditional changes are done to ensure a correct class file is created:
     *  - For regular classes (not interface types) a default constructor will be generated
     * if no constructor was defined and the superclass type information is available.
     *
     * If the version is Java 6 or newer, `StackMapTables` are generated for methods for which
     * no explicit stack map table attribute is specified and if the method contains control
     * transfer instructions which jump to instructions or exception handlers. In this case,
     * it is necessary that a class hierarchy is given which is '''complete'''; ''the default one
     * is not sufficient for any practical purposes''.
     *
     *
     * @param classHierarchy The project's class hierarchy. Required if and only if stack map
     *                       table attributes need to be automatically computed.
     */
    def toBR(
        implicit
        classHierarchy: ClassHierarchy = br.ClassHierarchy.PreInitializedClassHierarchy
    ): (br.ClassFile, Map[br.Method, T]) = {

        val accessFlags = accessModifiers.accessFlags
        val thisType: ObjectType = br.ObjectType(this.thisType)
        val superclassType: Option[ObjectType] = this.superclassType.map(br.ObjectType.apply)
        val interfaceTypes: Seq[ObjectType] = this.interfaceTypes.map(br.ObjectType.apply)
        val brFields = fields.result()

        val brAnnotatedMethods: IndexedSeq[(br.MethodTemplate, Option[T])] = {
            methods.result(version, thisType)
        }
        val annotationsMap: Map[MethodSignature, Option[T]] =
            brAnnotatedMethods.map { mt ⇒ val (m, t) = mt; (m.signature, t) }.toMap

        assert(annotationsMap.size == brAnnotatedMethods.size, "duplicate method signatures found")

        var brMethods = brAnnotatedMethods.map(m ⇒ m._1)
        if (!(
            bi.ACC_INTERFACE.isSet(accessFlags) ||
            brMethods.exists(_.isConstructor) ||
            // If "only" the following partial condition holds,
            // then the class file will be invalid; we can't
            // generate a default constructor, because we don't
            // know the target!
            superclassType.isEmpty
        )) {
            brMethods = brMethods :+ br.Method.defaultConstructor(superclassType.get)
        }

        val attributes = this.attributes map { attributeBuilder ⇒
            attributeBuilder(
                version,
                accessFlags, thisType, superclassType, interfaceTypes,
                brFields,
                brMethods
            )
        }

        val classFile = br.ClassFile( // <= THE FACTORY METHOD ENSURES THAT THE MEMBERS ARE SORTED
            version.minor,
            version.major,
            accessFlags,
            thisType,
            superclassType,
            interfaceTypes,
            brFields,
            brMethods,
            attributes
        )

        val brAnnotations: Seq[(br.Method, T)] =
            for {
                m ← classFile.methods
                ms = m.signature
                Some(a) ← annotationsMap.get(ms)
            } yield {
                (m, a: T @unchecked)
            }
        val brAnnotationsMap = brAnnotations.toMap

        // if (annotationsMap.nonEmpty)
        //    println(annotationsMap.mkString("\n")+" ===>\n"+brAnnotations+"\n"+classFile.methods)

        (classFile, brAnnotationsMap)
    }

    /**
     * Returns the build [[org.opalj.da.ClassFile]].
     *
     * @see [[toBR]] for details - in particula regarding `classHiearchy`.
     */
    def toDA(
        implicit
        classHierarchy: ClassHierarchy = br.ClassHierarchy.PreInitializedClassHierarchy
    ): (da.ClassFile, Map[br.Method, T]) = {
        val (brClassFile, annotations) = toBR(classHierarchy)
        (ba.toDA(brClassFile), annotations)
    }

}

object CLASS {

    final val DefaultMajorVersion = bi.Java8MajorVersion

    final val DefaultMinorVersion = 0

    final val DefaultVersion = UShortPair(DefaultMinorVersion, DefaultMajorVersion)

    def apply[T](
        version:         UShortPair                        = CLASS.DefaultVersion,
        accessModifiers: AccessModifier                    = SUPER,
        thisType:        String,
        superclassType:  Option[String]                    = Some("java/lang/Object"),
        interfaceTypes:  Seq[String]                       = Seq.empty,
        fields:          FIELDS                            = FIELDS(),
        methods:         METHODS[T]                        = METHODS[T](),
        attributes:      Seq[br.ClassFileAttributeBuilder] = Seq.empty
    ): CLASS[T] = {
        new CLASS(
            version, accessModifiers,
            thisType, superclassType, interfaceTypes,
            fields, methods,
            attributes
        )
    }

}
