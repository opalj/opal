/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.JavaConverters._

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import org.opalj.fpcf.Entity
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameters

case class ConfiguredMethods(nativeMethods: Array[ConfiguredMethodData])
object ConfiguredMethods {
    implicit val reader: ValueReader[ConfiguredMethods] = (config: Config, path: String) ⇒ {
        val c = config.getConfig(path)
        val configs = c.getConfigList("nativeMethods").asScala.toArray
        val data = configs.map(c ⇒ ConfiguredMethodData.reader.read(c, ""))
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
    implicit val reader: ValueReader[ConfiguredMethodData] = (config: Config, path: String) ⇒ {
        val c = if (path.nonEmpty) config.getConfig(path) else config
        val cf = c.as[String]("cf")
        val name = c.getString("name")
        val desc = c.getString("desc")
        val pointsTo =
            if (c.hasPath("pointsTo"))
                Some(c.getConfigList("pointsTo").asScala.toArray.map(c ⇒ PointsToRelation.reader.read(c, "")))
            else
                None

        val methodInvocations =
            if (c.hasPath("methodInvocations"))
                Some(c.getConfigList("methodInvocations").asScala.toArray.map(c ⇒ MethodDescription.reader.read(c, "")))
            else
                None

        ConfiguredMethodData(cf, name, desc, pointsTo, methodInvocations)
    }
}

case class PointsToRelation(lhs: EntityDescription, rhs: EntityDescription)
object PointsToRelation {
    implicit val reader: ValueReader[PointsToRelation] = (config: Config, path: String) ⇒ {
        val c = if (path.nonEmpty) config.getConfig(path) else config
        val lhs = EntityDescription.reader.read(c, "lhs")
        val rhs = EntityDescription.reader.read(c, "rhs")
        PointsToRelation(lhs, rhs)
    }
}

sealed trait EntityDescription {
    def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): Entity
}
object EntityDescription {
    implicit val reader: ValueReader[EntityDescription] = (config: Config, path: String) ⇒ {
        val c = config.getConfig(path)
        if (c.hasPath("fieldType")) {
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
    override def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): DeclaredMethod = {
        val classType = ObjectType(cf)
        val descriptor = MethodDescriptor(desc)
        declaredMethods(classType, classType.packageName, classType, name, descriptor)
    }
}

object MethodDescription {
    implicit val reader: ValueReader[MethodDescription] = (c: Config, path: String) ⇒ {
        val cf = c.getString("cf")
        val name = c.getString("name")
        val desc = c.getString("desc")
        MethodDescription(cf, name, desc)
    }
}

case class StaticFieldDescription(
        cf: String, name: String, fieldType: String
) extends EntityDescription {
    override def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): Field = {
        val classType = ObjectType(cf)
        val ft = FieldType(fieldType)
        val fieldOption = p.resolveFieldReference(classType, name, ft)
        if (fieldOption.isEmpty) {
            throw new RuntimeException(s"specified field $this is not part of the project.")
        }
        fieldOption.get
    }
}

case class ParameterDescription(
        cf: String, name: String, desc: String, index: Int
) extends EntityDescription {
    override def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): VirtualFormalParameter = {
        val classType = ObjectType(cf)
        val descriptor = MethodDescriptor(desc)
        val dm = declaredMethods(classType, classType.packageName, classType, name, descriptor)
        virtualFormalParameters(dm)(index)
    }
}

case class AllocationSiteDescription(
        cf: String,
        name: String,
        desc: String,
        instantiatedType: String,
        arrayComponentTypes: Seq[String]
) extends EntityDescription {
    override def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): Entity = {
        throw new RuntimeException("this should only be used as rhs and not be stored in the property store")
    }
}