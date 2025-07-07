/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cli

import org.opalj.cli.PlainArg

import org.rogach.scallop.stringConverter

object ClassNameArg extends PlainArg[String] {

    override val name: String = "class"
    override val description: String = "Fully-qualified class name of class to analyze"

}
