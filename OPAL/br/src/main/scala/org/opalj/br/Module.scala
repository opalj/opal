/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.AccessFlagsContexts
import org.opalj.bi.AccessFlags

import scala.collection.immutable.ArraySeq

/**
 * Definition of a Java 9 module.
 *
 * @author Michael Eichberg
 */
case class Module(
        name:        String,
        moduleFlags: Int,
        versionInfo: Option[String],
        requires:    ArraySeq[Requires],
        exports:     ArraySeq[Exports],
        opens:       ArraySeq[Opens],
        uses:        ArraySeq[ObjectType],
        provides:    ArraySeq[Provides]
) extends Attribute {

    final override def kindId: Int = Module.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        // TODO make the comparisons order independent...
        this == other
    }
}

object Module {

    final val KindId = 44

}

/**
 * @param requires The name of a required module.
 */
case class Requires(requires: String, flags: Int, version: Option[String]) {

    def toJava: String = {
        var flags = AccessFlags.toString(this.flags, AccessFlagsContexts.MODULE)
        if (flags.nonEmpty) flags += " "
        val version = this.version.map(v => s" //$v").getOrElse("")
        s"requires $flags$requires;$version"
    }
}

/**
 * @param   exports Name of an exported package in internal form.
 * @param   exportsTo List of names of modules whose code can access the
 *          public types in this exported package (in internal form).
 */
case class Exports(exports: String, flags: Int, exportsTo: ArraySeq[String]) {

    def toJava: String = {
        var flags = AccessFlags.toString(this.flags, AccessFlagsContexts.MODULE)
        if (flags.nonEmpty) flags += " "
        val exportsTo =
            if (this.exportsTo.isEmpty) "" else this.exportsTo.mkString(" to ", ", ", "")

        s"exports $flags$exports$exportsTo;"
    }
}

/**
 * @param opens The name of the package.
 */
case class Opens(opens: String, flags: Int, toPackages: ArraySeq[String]) {

    def toJava: String = {
        val toPackages =
            if (this.toPackages.isEmpty) "" else this.toPackages.mkString(" to ", ", ", "")

        s"opens $opens$toPackages;"
    }

}

case class Provides(provides: ObjectType, withInterfaces: ArraySeq[ObjectType]) {

    def toJava: String = {
        val withInterfaces =
            if (this.withInterfaces.isEmpty)
                ""
            else
                this.withInterfaces.map(_.toJava).mkString(" with ", ", ", "")
        s"provides ${provides.toJava}$withInterfaces;"
    }

}
