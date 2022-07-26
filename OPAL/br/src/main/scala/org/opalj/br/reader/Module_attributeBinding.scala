/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Module_attributeReader

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * The factory method to create the "class level" `Module` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait Module_attributeBinding
    extends Module_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Module_attribute = br.Module

    type RequiresEntry = br.Requires
    override implicit val requiresEntryType: ClassTag[RequiresEntry] = ClassTag(classOf[br.Requires])

    type ExportsEntry = br.Exports
    override implicit val exportsEntryType: ClassTag[ExportsEntry] = ClassTag(classOf[br.Exports])
    type ExportsToIndexEntry = String

    type OpensEntry = br.Opens
    override implicit val opensEntryType: ClassTag[OpensEntry] = ClassTag(classOf[br.Opens])

    type OpensToIndexEntry = String // module name

    type UsesEntry = ObjectType

    type ProvidesEntry = br.Provides
    override implicit val providesEntryType: ClassTag[ProvidesEntry] = ClassTag(classOf[br.Provides])

    type ProvidesWithIndexEntry = ObjectType

    override def Module_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
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
            requires,
            exports,
            opens,
            ArraySeq.from(uses).map(cp(_).asObjectType(cp)),
            provides
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
        cp:                     Constant_Pool,
        exports_index:          Constant_Pool_Index,
        exports_flags:          Int,
        exports_to_index_table: ExportsToIndexTable
    ): ExportsEntry = {
        br.Exports(
            cp(exports_index).asPackageIdentifier(cp),
            exports_flags,
            ArraySeq.from(exports_to_index_table).map(cp(_).asModuleIdentifier(cp))
        )
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
            ArraySeq.from(opens_to_index_table).map(cp(_).asModuleIdentifier(cp))
        )
    }

    override def ProvidesEntry(
        cp:                        Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable
    ): ProvidesEntry = {
        br.Provides(
            cp(provides_index).asObjectType(cp),
            ArraySeq.from(provides_with_index_table).map(cp(_).asObjectType(cp))
        )
    }

}

