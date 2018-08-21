/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.Module_attributeReader

/**
 * The factory method to create the class level `Module` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait Module_attributeBinding
    extends Module_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Module_attribute = br.Module

    type RequiresEntry = br.Requires
    implicit val RequiresEntryManifest: ClassTag[RequiresEntry] = implicitly

    type ExportsEntry = br.Exports
    implicit val ExportsEntryManifest: ClassTag[ExportsEntry] = implicitly
    type ExportsToIndexEntry = String
    implicit val ExportsToIndexEntryManifest: ClassTag[ExportsToIndexEntry] = implicitly

    type OpensEntry = br.Opens
    implicit val OpensEntryManifest: ClassTag[OpensEntry] = implicitly
    type OpensToIndexEntry = String // module name
    implicit val OpensToIndexEntryManifest: ClassTag[OpensToIndexEntry] = implicitly

    type UsesEntry = ObjectType
    implicit val UsesEntryManifest: ClassTag[UsesEntry] = implicitly

    type ProvidesEntry = br.Provides
    implicit val ProvidesEntryManifest: ClassTag[ProvidesEntry] = implicitly
    type ProvidesWithIndexEntry = ObjectType
    implicit val ProvidesWithIndexEntryManifest: ClassTag[ProvidesWithIndexEntry] = implicitly

    override def Module_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        as_name_index:        Constant_Pool_Index,
        as_descriptor_index:  Constant_Pool_Index,
        module_name_index:    Constant_Pool_Index, // CONSTANT_Module_info
        module_flags:         Int,
        module_version_index: Constant_Pool_Index, // CONSTANT_UTF8
        requires:             Requires,
        exports:              Exports,
        opens:                Opens,
        uses:                 Uses,
        provides:             Provides
    ): Module_attribute = {
        Module(
            cp(module_name_index).asModuleIdentifier(cp),
            module_flags,
            if (module_version_index == 0) None else Some(cp(module_version_index).asString),
            requires, exports, opens, uses, provides
        )
    }

    override def RequiresEntry(
        cp:                    Constant_Pool,
        requires_index:        Constant_Pool_Index,
        requires_flags:        Int,
        require_version_index: Constant_Pool_Index // Optional: CONSTANT_UTF8
    ): RequiresEntry = {
        br.Requires(
            cp(requires_index).asModuleIdentifier(cp),
            requires_flags,
            if (require_version_index != 0) Some(cp(require_version_index).asString) else None
        )
    }

    override def ExportsEntry(
        cp:            Constant_Pool,
        exports_index: Constant_Pool_Index,
        exportsFlags:  Int,
        exportsTo:     ExportsToIndexTable
    ): ExportsEntry = {
        br.Exports(
            cp(exports_index).asPackageIdentifier(cp),
            exportsFlags,
            exportsTo
        )
    }

    override def ExportsToIndexEntry(
        cp:               Constant_Pool,
        exports_to_index: Constant_Pool_Index // CONSTANT_UTF8
    ): ExportsToIndexEntry = {
        cp(exports_to_index).asModuleIdentifier(cp)
    }

    override def OpensToIndexEntry(
        cp:             Constant_Pool,
        opens_to_index: Constant_Pool_Index
    ): OpensToIndexEntry = {
        cp(opens_to_index).asModuleIdentifier(cp)
    }

    override def OpensEntry(
        cp:                   Constant_Pool,
        opens_index:          Constant_Pool_Index, // CONSTANT_Package_info
        opens_flags:          Int,
        opens_to_index_table: OpensToIndexTable
    ): OpensEntry = {
        br.Opens(
            cp(opens_index).asPackageIdentifier(cp),
            opens_flags,
            opens_to_index_table
        )
    }

    override def UsesEntry(
        cp:         Constant_Pool,
        uses_index: Constant_Pool_Index // CONSTANT_Class
    ): UsesEntry = {
        cp(uses_index).asObjectType(cp)
    }

    override def ProvidesWithIndexEntry(
        cp:                  Constant_Pool,
        provides_with_index: Constant_Pool_Index // CONSTANT_Class
    ): ProvidesWithIndexEntry = {
        cp(provides_with_index).asObjectType(cp)
    }

    override def ProvidesEntry(
        cp:                        Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable
    ): ProvidesEntry = {
        br.Provides(cp(provides_index).asObjectType(cp), provides_with_index_table)
    }

}

