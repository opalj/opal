/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.collection.Map

/**
 * Stores the information where each field is read and written. If the project
 * is incomplete the results are also necessarily incomplete. Reflective and comparable
 * accesses are not considered.
 *
 * @author Michael Eichberg
 */
class FieldAccessInformation(
        val project:          SomeProject,
        val allReadAccesses:  Map[Field, Seq[(Method, PCs)]],
        val allWriteAccesses: Map[Field, Seq[(Method, PCs)]],
        val unresolved:       Vector[(Method, PCs)]
) {

    private[this] def accesses(
        accessInformation:  Map[Field, Seq[(Method, PCs)]],
        declaringClassType: ObjectType,
        fieldName:          String
    ): Seq[(Method, PCs)] = {
        // FIX We can also use a reference to a subclass to access a field in a supertype
        accessInformation.collectFirst {
            case (field, accesses) if field.name == fieldName &&
                (field.classFile.thisType eq declaringClassType) => accesses
        }.getOrElse(Seq.empty)
    }

    def writeAccesses(declaringClassType: ObjectType, fieldName: String): Seq[(Method, PCs)] = {
        accesses(allWriteAccesses, declaringClassType, fieldName)
    }

    def readAccesses(declaringClassType: ObjectType, fieldName: String): Seq[(Method, PCs)] = {
        accesses(allReadAccesses, declaringClassType, fieldName)
    }

    final def writeAccesses(field: Field): Seq[(Method, PCs)] = {
        allWriteAccesses.getOrElse(field, Seq.empty)
    }

    final def readAccesses(field: Field): Seq[(Method, PCs)] = {
        allReadAccesses.getOrElse(field, Seq.empty)
    }

    def isRead(field: Field): Boolean = {
        allReadAccesses.get(field) match {
            case Some(accesses) => accesses.nonEmpty
            case None           => false
        }
    }

    def isWritten(field: Field): Boolean = {
        allWriteAccesses.get(field) match {
            case Some(accesses) => accesses.nonEmpty
            case None           => false
        }
    }

    final def isAccessed(field: Field): Boolean = isRead(field) || isWritten(field)

    /**
     * Returns a new iterator to iterate over all field access locations.
     */
    def allAccesses(field: Field): Iterator[(Method, PCs)] = {
        readAccesses(field).iterator ++ writeAccesses(field).iterator
    }

    /**
     * Basic statistics about the number of field reads and writes.
     */
    def statistics: Map[String, Int] = {
        Map(
            "field reads" -> allReadAccesses.values.map(_.map(_._2.size).sum).sum,
            "field writes" -> allWriteAccesses.values.map(_.map(_._2.size).sum).sum,
            "unresolved field accesses" -> unresolved.map(_._2.size).sum
        )
    }

}
