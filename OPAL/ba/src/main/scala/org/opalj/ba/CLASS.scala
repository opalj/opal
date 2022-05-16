/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.collection.immutable.UShortPair
import org.opalj.br.MethodSignature
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.MethodTemplate

import scala.collection.immutable.ArraySeq

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
        interfaceTypes:  ArraySeq[String],
        fields:          FIELDS,
        methods:         METHODS[T],
        attributes:      ArraySeq[br.ClassFileAttributeBuilder]
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
        val interfaceTypes: ArraySeq[ObjectType] = this.interfaceTypes.map[br.ObjectType](br.ObjectType.apply)
        val brFields = fields.result()

        val brAnnotatedMethods: ArraySeq[(br.MethodTemplate, Option[T])] = {
            methods.result(version, thisType)
        }
        val annotationsMap: Map[MethodSignature, Option[T]] =
            /*
            Map.empty ++
                brAnnotatedMethods.iterator.map[(MethodSignature, Option[T])](mt =>
                    { val (m, t) = mt; (m.signature, t) })
            */
            brAnnotatedMethods.foldLeft(Map.empty[MethodSignature, Option[T]]) { (map, mt) =>
                val (m, t) = mt
                map + ((m.signature, t))
            }

        assert(annotationsMap.size == brAnnotatedMethods.size, "duplicate method signatures found")

        var brMethods = brAnnotatedMethods.map[MethodTemplate](m => m._1)
        if (!(
            bi.ACC_INTERFACE.isSet(accessFlags) ||
            brMethods.exists(_.isConstructor) ||
            // If "only" the following partial condition holds,
            // then the class file will be invalid; we can't
            // generate a default constructor, because we don't
            // know the target!
            superclassType.isEmpty
        )) {
            brMethods :+= br.Method.defaultConstructor(superclassType.get)
        }

        val attributes = this.attributes.map[br.Attribute] { attributeBuilder =>
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
                m <- classFile.methods
                Some(a) <- annotationsMap.get(m.signature).toSeq
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
        version:         UShortPair                             = CLASS.DefaultVersion,
        accessModifiers: AccessModifier                         = SUPER,
        thisType:        String,
        superclassType:  Option[String]                         = Some("java/lang/Object"),
        interfaceTypes:  ArraySeq[String]                       = ArraySeq.empty,
        fields:          FIELDS                                 = FIELDS(),
        methods:         METHODS[T]                             = METHODS[T](),
        attributes:      ArraySeq[br.ClassFileAttributeBuilder] = ArraySeq.empty
    ): CLASS[T] = {
        new CLASS(
            version, accessModifiers,
            thisType, superclassType, interfaceTypes,
            fields, methods,
            attributes
        )
    }

}
