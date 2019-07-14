/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.FieldType
import org.opalj.br.ObjectType

/**
 * Entity which is used for types flow from/to the "external world".
 */
object ExternalWorld

/**
 * Represents a field in the external world. Such fields can not be resolved by the Project.
 *
 * @param declaringClass Object type of the class which declares the field (is also in the external world).
 * @param name Name of the field.
 * @param declaredFieldType Type of the field.
 */
case class ExternalField(declaringClass: ObjectType, name: String, declaredFieldType: FieldType)
