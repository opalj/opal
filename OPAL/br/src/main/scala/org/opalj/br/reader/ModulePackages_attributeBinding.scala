/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.ModuleMainClass_attributeReader

/**
 * The factory method to create the `ModulePackages` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait ModulePackages_attributeBinding
    extends ModuleMainClass_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type ModulePackages_attribute = ModulePackages

    type PackageIndexTableEntry = String
    implicit val PackageIndexTableEntryManifest: ClassTag[PackageIndexTableEntry] = implicitly

    type PackageIndexTable = IndexedSeq[PackageIndexTableEntry]

    def PackageIndexTableEntry(
        cp:            Constant_Pool,
        package_index: Constant_Pool_Index // CONSTANT_Package_info
    ): PackageIndexTableEntry = {
        cp(package_index).asPackageIdentifier(cp)
    }

    def ModulePackages_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        package_index_table:  PackageIndexTable
    ): ModulePackages_attribute = {
        new ModulePackages(package_index_table)
    }

}

