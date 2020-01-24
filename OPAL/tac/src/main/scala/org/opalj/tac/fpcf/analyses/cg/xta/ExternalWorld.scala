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
 *
 * @author Andreas Bauer
 */
object ExternalWorld

/**
 * Represents a field in the external world. Such fields can either not be resolved by the Project (because the
 * class file defining the field was not loaded), or they belong to a class file which was loaded as a library
 * file.
 *
 * @param declaringClass Object type of the class which declares the field (is also in the external world).
 * @param name Name of the field.
 * @param declaredFieldType Type of the field.
 */
case class ExternalField(declaringClass: ObjectType, name: String, declaredFieldType: FieldType)
