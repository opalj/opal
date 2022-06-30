/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.jdk.CollectionConverters._

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters

case class ConfiguredMethods(nativeMethods: Array[ConfiguredMethodData])
object ConfiguredMethods {
    implicit val reader: ValueReader[ConfiguredMethods] = (config: Config, path: String) => {
        val c = config.getConfig(path)
        val configs = c.getConfigList("nativeMethods").asScala.toArray
        val data = configs.map(c => ConfiguredMethodData.reader.read(c, ""))
        ConfiguredMethods(data)
    }
}

case class ConfiguredMethodData(
        cf:                String,
        name:              String,
        desc:              String,
        pointsTo:          Option[Array[PointsToRelation]],
        methodInvocations: Option[Array[MethodDescription]]
) {
    def method(
        implicit
        declaredMethods: DeclaredMethods
    ): DeclaredMethod = {
        val classType = ObjectType(cf)
        val descriptor = MethodDescriptor(desc)
        declaredMethods(classType, classType.packageName, classType, name, descriptor)
    }
}

object ConfiguredMethodData {
    implicit val reader: ValueReader[ConfiguredMethodData] = (config: Config, path: String) => {
        val c = if (path.nonEmpty) config.getConfig(path) else config
        val cf = c.as[String]("cf")
        val name = c.getString("name")
        val desc = c.getString("desc")
        val pointsTo =
            if (c.hasPath("pointsTo"))
                Some(c.getConfigList("pointsTo").asScala.toArray.map(c => PointsToRelation.reader.read(c, "")))
            else
                None

        val methodInvocations =
            if (c.hasPath("methodInvocations"))
                Some(c.getConfigList("methodInvocations").asScala.toArray.map(c => MethodDescription.reader.read(c, "")))
            else
                None

        ConfiguredMethodData(cf, name, desc, pointsTo, methodInvocations)
    }
}

case class PointsToRelation(lhs: EntityDescription, rhs: EntityDescription)
object PointsToRelation {
    implicit val reader: ValueReader[PointsToRelation] = (config: Config, path: String) => {
        val c = if (path.nonEmpty) config.getConfig(path) else config
        val lhs = EntityDescription.reader.read(c.getConfig("lhs"), "")
        val rhs = EntityDescription.reader.read(c.getConfig("rhs"), "")
        PointsToRelation(lhs, rhs)
    }
}

sealed trait EntityDescription

object EntityDescription {
    implicit val reader: ValueReader[EntityDescription] = (c: Config, path: String) => {
        if (c.hasPath("array")) {
            val arrayType = c.getString("arrayType")
            val array = reader.read(c.getConfig("array"), "")
            ArrayDescription(array, arrayType)
        } else if (c.hasPath("fieldType")) {
            val cf = c.getString("cf")
            val name = c.getString("name")
            val fieldType = c.getString("fieldType")
            StaticFieldDescription(cf, name, fieldType)
        } else if (c.hasPath("index")) {
            val cf = c.getString("cf")
            val name = c.getString("name")
            val desc = c.getString("desc")
            val index = c.getInt("index")
            ParameterDescription(cf, name, desc, index)
        } else if (c.hasPath("instantiatedType")) {
            val cf = c.getString("cf")
            val name = c.getString("name")
            val desc = c.getString("desc")
            val instantiatedType = c.getString("instantiatedType")
            val arrayComponentTypes =
                if (c.hasPath("arrayComponentTypes"))
                    c.getStringList("arrayComponentTypes").asScala
                else
                    List.empty
            AllocationSiteDescription(cf, name, desc, instantiatedType, arrayComponentTypes)
        } else /*MethodDescription*/ {
            MethodDescription.reader.read(c, "")
        }
    }
}

case class MethodDescription(
        cf: String, name: String, desc: String
) extends EntityDescription {
    def method(declaredMethods: DeclaredMethods): DeclaredMethod = {
        val classType = ObjectType(cf)
        declaredMethods(classType, classType.packageName, classType, name, MethodDescriptor(desc))
    }
}

object MethodDescription {
    implicit val reader: ValueReader[MethodDescription] = (c: Config, path: String) => {
        val cf = c.getString("cf")
        val name = c.getString("name")
        val desc = c.getString("desc")
        MethodDescription(cf, name, desc)
    }
}

case class StaticFieldDescription(
        cf: String, name: String, fieldType: String
) extends EntityDescription {
    def fieldOption(project: SomeProject): Option[Field] = {
        project.resolveFieldReference(ObjectType(cf), name, FieldType(fieldType))
    }
}

case class ParameterDescription(cf: String, name: String, desc: String, index: Int) extends EntityDescription {
    def method(declaredMethods: DeclaredMethods): DeclaredMethod = {
        val classType = ObjectType(cf)
        declaredMethods(classType, classType.packageName, classType, name, MethodDescriptor(desc))
    }

    def fp(
        method: DeclaredMethod, virtualFormalParameters: VirtualFormalParameters
    ): VirtualFormalParameter = {
        val fps = virtualFormalParameters(method)
        if (fps eq null) null
        else fps(index)
    }
}

case class AllocationSiteDescription(
        cf:                  String,
        name:                String,
        desc:                String,
        instantiatedType:    String,
        arrayComponentTypes: scala.collection.Seq[String]
) extends EntityDescription {
    def method(declaredMethods: DeclaredMethods): DeclaredMethod = {
        val classType = ObjectType(cf)
        declaredMethods(classType, classType.packageName, classType, name, MethodDescriptor(desc))
    }
}

case class ArrayDescription(
        array:     EntityDescription,
        arrayType: String
) extends EntityDescription