/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.collection.Map

import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.fieldaccess
import org.opalj.br.fpcf.properties.fieldaccess.AccessParameter
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.SomeInterimEP

/**
 * Acts as a proxy for the propertyStore, accessing [[FieldReadAccessInformation]] and [[FieldWriteAccessInformation]].
 * Should be computed outside of any FPCF phases as it cannot handle intermediate values.
 *
 * @author Maximilian Rüsch
 */
case class FieldAccessInformation(project: SomeProject) {

    private[this] val propertyStore = project.get(PropertyStoreKey)

    private[this] def getFieldAccessInformation[S <: fieldaccess.FieldAccessInformation[S]](
        field: DeclaredField,
        key:   PropertyKey[fieldaccess.FieldAccessInformation[S]]
    ): fieldaccess.FieldAccessInformation[S] = {
        propertyStore(field, key) match {
            case FinalP(fai) => fai
            case _: SomeInterimEP =>
                throw new IllegalStateException("FieldAccessInformationKey should not be called during an FPCF phase!")
            case r =>
                throw new IllegalStateException(s"Unexpected property found: $r")
        }
    }

    def readAccesses(
        field: Field
    )(implicit declaredFields: DeclaredFields): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        getFieldAccessInformation(declaredFields(field), FieldReadAccessInformation.key).accesses

    def writeAccesses(
        field: Field
    )(implicit declaredFields: DeclaredFields): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        getFieldAccessInformation(declaredFields(field), FieldWriteAccessInformation.key).accesses

    def isRead(field: Field)(implicit declaredFields: DeclaredFields): Boolean = readAccesses(field).nonEmpty
    def isWritten(field: Field)(implicit declaredFields: DeclaredFields): Boolean = writeAccesses(field).nonEmpty
    def isAccessed(field: Field)(implicit declaredFields: DeclaredFields): Boolean = isRead(field) || isWritten(field)

    /**
     * Returns a new iterator to iterate over all field access locations.
     */
    def allAccesses(
        field: Field
    )(implicit declaredFields: DeclaredFields): Iterator[(Int, PC, AccessReceiver, AccessParameter)] =
        readAccesses(field) ++ writeAccesses(field)

    /**
     * Basic statistics about the number of field reads and writes.
     */
    def statistics(implicit declaredFields: DeclaredFields): Map[String, Int] = {
        Map(
            "field reads" -> project.allFields.flatMap(readAccesses).size,
            "field writes" -> project.allFields.flatMap(writeAccesses).size
        )
    }
}
