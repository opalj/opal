/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker

import scala.io.Source
import org.opalj.io.process

/**
 * Common constants used by the BugPicker.
 *
 * @author Michael Eichberg
 */
package object core {

    def getAsset(path: String): String = {
        process(getClass.getResourceAsStream(path)) { in => Source.fromInputStream(in).mkString }
    }

    lazy val HTMLCSS: String = getAsset("html.css")

    lazy val HTMLJS: String = getAsset("html.js")

    lazy val SearchJS: String = getAsset("search.js")

    lazy val ReportCSS: String = getAsset("report.css")

    lazy val ReportJS: String = getAsset("report.js")

}
