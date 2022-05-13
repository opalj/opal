/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

import org.opalj.bi.reader.PermittedSubclasses_attributeReader
import org.opalj.br.PermittedSubclasses

import scala.collection.immutable.ArraySeq

/**
 * Implements the factory methods to create the `PermittedSubclasses` attribute (Java 17).
 *
 * @author Julius Naeumann
 */
trait PermittedSubclasses_attributeBinding
    extends PermittedSubclasses_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type PermittedSubclasses_attribute = PermittedSubclasses

    def PermittedSubclasses_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        classes:              PermittedSubclassesArray
    ): PermittedSubclasses_attribute = {
        new PermittedSubclasses(
            ArraySeq.from(classes).map { p => cp(p).asObjectType(cp) }
        )
    }
}
