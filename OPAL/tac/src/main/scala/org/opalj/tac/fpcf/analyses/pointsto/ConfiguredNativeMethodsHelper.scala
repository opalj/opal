/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.Entity
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.tac.common.DefinitionSite

sealed trait EntityDescription {
    def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): Entity
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

case class FieldDescription(
        cf: String, name: String, fieldType: String
) extends EntityDescription {
    override def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): Field = {
        val classType = ObjectType(cf)
        val fieldType = FieldType(fieldType)
        val fieldOption = p.resolveFieldReference(classType, name, fieldType)
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
        cf: String, name: String, desc: String, instantiatedType: String
) extends EntityDescription {
    override def entity(
        implicit
        p:                       SomeProject,
        declaredMethods:         DeclaredMethods,
        virtualFormalParameters: VirtualFormalParameters
    ): Entity = {
        val classType = ObjectType(cf)
        val descriptor = MethodDescriptor(desc)
        val dm = declaredMethods(classType, classType.packageName, classType, name, descriptor)
        val it = ObjectType(instantiatedType)
        // todo: use DefinitionSitesKey
        // todo: should be safe to use even if dm has no defined method
        AllocationSite(dm.definedMethod, -1, it)
    }
}

// todo move class
case class AllocationSite(
    override val method: Method, override val pc: Int, instantiatedType: ObjectType
) extends DefinitionSite(method, pc)

case class PointsToRelation(lhs: EntityDescription, rhs: EntityDescription)

case class NativeMethodData(
        cf:                String,
        name:              String,
        desc:              String,
        pointsTo:          Option[Seq[PointsToRelation]],
        methodInvocations: Option[Seq[MethodDescription]]
) {
    def method(
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
