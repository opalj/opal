/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeBuffer

import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts
import org.opalj.bi.AccessFlagsContexts.MODULE

/**
 * Representation of the ''Module'' attribute (Java 9).
 *
 * @author Michael Eichberg
 */
case class Module_attribute(
        attribute_name_index: Constant_Pool_Index,
        module_name_index:    Constant_Pool_Index,
        module_flags:         Int,
        module_version_index: Constant_Pool_Index, // can be "0"!
        requires:             Requires,
        exports:              Exports,
        opens:                Opens,
        uses:                 Uses,
        provides:             Provides
) extends Attribute {

    def attribute_length: Int = {
        2 + 2 + 2 + // <= module meta information
            2 + requires.size * 6 +
            2 + exports.iterator.map(_.attribute_length).sum +
            2 + opens.iterator.map(_.attribute_length).sum +
            2 + uses.size * 2 +
            2 + provides.iterator.map(_.attribute_length).sum
    }

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        val name = cp(module_name_index).toString(cp)
        val flags =
            AccessFlags.toStrings(module_flags, AccessFlagsContexts.MODULE).mkString("{", " ", "}")
        val version =
            if (module_version_index == 0)
                "N/A"
            else
                cp(module_version_index).toString(cp)
        val module = s"Module(name=$name, flags=$flags, version=$version)"

        <details>
            <summary class="attribute">{ module }</summary>
            <div>
                {
                    requires.view.map[String](_.toString).sorted.map[NodeBuffer] { r =>
                        <span>{ r }</span><br/>
                    }
                }
            </div>
            <div>
                {
                    exports.view.map[String](_.toString).sorted.map[NodeBuffer] { r =>
                        <span>{ r }</span><br/>
                    }
                }
            </div>
            <div>
                {
                    opens.view.map[String](_.toString).sorted.map[NodeBuffer] { r =>
                        <span>{ r }</span><br/>
                    }
                }
            </div>
            <div>
                {
                    uses.view.map(cp(_).toString).sorted.map[NodeBuffer] { r =>
                        <span>{ s"uses $r" }</span><br/>
                    }
                }
            </div>
            <div>
                {
                    provides.view.map[String](_.toString).sorted.map[NodeBuffer] { r =>
                        <span>{ r }</span><br/>
                    }
                }
            </div>
        </details>
    }

}

case class RequiresEntry(
        requires_index:         Constant_Pool_Index,
        requires_flags:         Int,
        requires_version_index: Constant_Pool_Index // can be "0" (<=> no version...)
) {

    def toString(implicit cp: Constant_Pool): String = {

        val flags = AccessFlags.toString(requires_flags, MODULE)
        val requiredPackage = cp(requires_index).toString(cp)
        val versionInfo =
            if (requires_version_index == 0)
                ""
            else
                "// "+cp(requires_version_index).toString(cp)

        s"requires $flags $requiredPackage;$versionInfo"
    }
}

case class ExportsEntry(
        exports_index:          Constant_Pool_Index,
        exports_flags:          Int,
        exports_to_index_table: ExportsToIndexTable
) {

    def attribute_length: Int = 6 + exports_to_index_table.size * 2

    def toString(implicit cp: Constant_Pool): String = {
        val flags = AccessFlags.toString(exports_flags, MODULE)
        val exports_to = {
            if (exports_to_index_table.isEmpty)
                ";"
            else
                exports_to_index_table
                    .view
                    .map(cp(_).toString)
                    .sorted
                    .mkString(" to ", ", ", ";")
        }

        s"exports $flags ${cp(exports_index).toString(cp)}$exports_to"
    }
}

case class OpensEntry(
        opens_index:          Constant_Pool_Index,
        opens_flags:          Int,
        opens_to_index_table: OpensToIndexTable
) {

    def attribute_length: Int = 6 + opens_to_index_table.size * 2

    def toString(implicit cp: Constant_Pool): String = {
        val flags = AccessFlags.toString(opens_flags, MODULE)
        val opens_to = {
            if (opens_to_index_table.isEmpty)
                ";"
            else
                opens_to_index_table.
                    view.
                    map(cp(_).toString).
                    sorted.
                    mkString(" to ", ", ", ";")
        }

        s"opens $flags ${cp(opens_index).toString(cp)}$opens_to"
    }
}

case class ProvidesEntry(
        provides_index:            Constant_Pool_Index,
        provides_with_index_table: ProvidesWithIndexTable
) {

    def attribute_length: Int = 4 + provides_with_index_table.size * 2

    def toString(implicit cp: Constant_Pool): String = {
        val provides_with =
            provides_with_index_table.
                view.
                map(cp(_).toString).
                sorted.
                mkString(" with ", ", ", ";")

        s"provides ${cp(provides_index).toString(cp)}$provides_with"
    }
}
