/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.FieldType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.analyses.Project

/**
 * The location where a specific feature was found. In general, a feature query should always use
 * a [[LocationsContainer]] to manage the identified locations.
 *
 * @tparam S The kind of the source. E.g., `java.net.URL`.
 *
 * @author Michael Eichberg
 */
sealed abstract class Location[S] {

    /**
     * The source location.
     */
    def source: Option[S]

}

final case class PackageLocation[S](
        override val source: Option[S],
        packageName:         String
) extends Location[S] {

    override def toString: String = {
        source match {
            case Some(source) => s"$packageName\n$source"
            case None         => packageName
        }
    }
}
object PackageLocation {

    def apply[S](source: S, packageName: String): PackageLocation[S] = {
        new PackageLocation[S](Some(source), packageName)
    }

    def apply[S](packageName: String): PackageLocation[S] = {
        new PackageLocation[S](None, packageName)
    }
}

final case class ClassFileLocation[S](
        override val source: Option[S],
        classFileFQN:        String
) extends Location[S] {

    override def toString: String = {
        source match {
            case Some(source) => s"$classFileFQN\n$source"
            case None         => classFileFQN
        }
    }

}

object ClassFileLocation {

    def apply[S](classFile: ClassFile): ClassFileLocation[S] = {
        new ClassFileLocation[S](None, classFile.thisType.toJava)
    }

    def apply[S](objectType: ObjectType): ClassFileLocation[S] = {
        new ClassFileLocation[S](None, objectType.toJava)
    }

    def apply[S](source: S, classFile: ClassFile): ClassFileLocation[S] = {
        new ClassFileLocation[S](Some(source), classFile.thisType.toJava)
    }

    def apply[S](project: Project[S], objectType: ObjectType): ClassFileLocation[S] = {
        new ClassFileLocation[S](project.source(objectType), objectType.toJava)
    }

    final def apply[S](project: Project[S], classFile: ClassFile): ClassFileLocation[S] = {
        apply(project, classFile.thisType)
    }

}

final case class FieldLocation[S](
        classFileLocation: ClassFileLocation[S],
        fieldName:         String,
        fieldType:         FieldType
) extends Location[S] {

    override def source: Option[S] = classFileLocation.source

    def classFileFQN: String = classFileLocation.classFileFQN

    override def toString: String = {

        val s = s"${classFileLocation.classFileFQN}{ /*field*/ $fieldName : ${fieldType.toJava} }"
        val source = classFileLocation.source
        if (source.isDefined)
            s + s"\n${source.get}"
        else
            s
    }
}

object FieldLocation {

    def apply[S](classFileLocation: ClassFileLocation[S], field: Field): FieldLocation[S] = {
        new FieldLocation[S](classFileLocation, field.name, field.fieldType)
    }
}

final case class MethodLocation[S](
        classFileLocation: ClassFileLocation[S],
        methodName:        String,
        methodDescriptor:  MethodDescriptor
) extends Location[S] {

    override def source: Option[S] = classFileLocation.source

    def methodSignature: String = methodDescriptor.toJava(methodName)

    def classFileFQN: String = classFileLocation.classFileFQN

    override def toString: String = {
        val s = s"${classFileLocation.classFileFQN}{ /*method*/ $methodSignature }"
        val source = classFileLocation.source
        if (source.isDefined)
            s + s"\n${source.get}"
        else
            s
    }

}

object MethodLocation {

    def apply[S](source: S, method: Method): MethodLocation[S] = {
        val cf = method.classFile
        new MethodLocation(ClassFileLocation(source, cf), method.name, method.descriptor)
    }

    def apply[S](classFileLocation: ClassFileLocation[S], method: Method): MethodLocation[S] = {
        new MethodLocation(classFileLocation, method.name, method.descriptor)
    }

    final def apply[S](methodInfo: MethodInfo[S]): MethodLocation[S] = {
        MethodLocation(methodInfo.source, methodInfo.method)
    }

}

final case class InstructionLocation[S](methodLocation: MethodLocation[S], pc: Int) extends Location[S] {

    override def source: Option[S] = methodLocation.source

    def classFileFQN: String = methodLocation.classFileFQN

    def methodSignature: String = methodLocation.methodSignature

    override def toString: String = {
        val classFileLocation = methodLocation.classFileLocation
        val source = classFileLocation.source
        val s = s"${classFileLocation.classFileFQN}{ $methodSignature { $pc } }"
        if (source.isDefined)
            s + s"\n${source.get}"
        else
            s
    }
}
object InstructionLocation {

    def apply[S](source: S, method: Method, pc: Int): InstructionLocation[S] = {
        val classFileLocation = ClassFileLocation(source, method.classFile)
        val methodLocation = MethodLocation(classFileLocation, method.name, method.descriptor)
        new InstructionLocation(methodLocation, pc)
    }

}
