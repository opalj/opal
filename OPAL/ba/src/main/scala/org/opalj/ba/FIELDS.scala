/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

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
    def result(): IndexedSeq[br.FieldTemplate] = {
        IndexedSeq.empty ++ fields.iterator.map(f ⇒ f.result())
    }

}
