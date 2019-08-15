/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.tac.common.DefinitionSite

case class ArrayEntity[ElementType](element: ElementType)
case class MethodExceptions(dm: DeclaredMethod)
case class CallExceptions(defSite: DefinitionSite)

trait AField {
    def classType: ObjectType
    def fieldType: FieldType
}

case class RealField(field: Field) extends AField {
    override def classType: ObjectType = field.classFile.thisType
    override def fieldType: FieldType = field.fieldType
}

case object UnsafeFakeField extends AField {
    override def classType: ObjectType = ObjectType.Object
    override def fieldType: FieldType = ObjectType.Object
}
