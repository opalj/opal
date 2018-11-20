/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.collection.immutable.RefArray
import org.opalj.bi.reader.ModulePackages_attributeReader

/**
 * The factory method to create the `ModulePackages` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait ModulePackages_attributeBinding
    extends ModulePackages_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type ModulePackages_attribute = ModulePackages

    def ModulePackages_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        package_index_table:  PackageIndexTable
    ): ModulePackages_attribute = {
        new ModulePackages(
            RefArray.mapFrom(package_index_table) { p ⇒ cp(p).asPackageIdentifier(cp) }
        )
    }

}

