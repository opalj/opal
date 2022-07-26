/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.collection.immutable.ArraySeq

/**
 * Builder for a sequence of [[org.opalj.br.Field]]s.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
case class FIELDS(fields: FIELD*) {

    /**
     * Returns the collection of [[org.opalj.br.FieldTemplate]] objects.
     */
    def result(): ArraySeq[br.FieldTemplate] = {
        val b = ArraySeq.newBuilder[br.FieldTemplate]
        b.sizeHint(fields.length)
        fields.foreach(b += _.result())
        b.result()
    }

}
