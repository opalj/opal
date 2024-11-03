/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package parser

import org.opalj.commandlinebase.OpalCommandExternalParser
import org.opalj.tac.fpcf.analyses.purity.DomainSpecificRater
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * `RaterCommandExternalParser` is a parser responsible for resolving and loading a specified `DomainSpecificRater`.
 * It interprets a command-line argument to load a rater class.
 */
object RaterCommandExternalParser extends OpalCommandExternalParser[String, DomainSpecificRater] {
    override def parse(arg: String): DomainSpecificRater = {
        if (arg == null) SystemOutLoggingAllExceptionRater
        else {
            import scala.reflect.runtime.universe.runtimeMirror
            val mirror = runtimeMirror(getClass.getClassLoader)
            val module = mirror.staticModule(arg)
            mirror.reflectModule(module).instance.asInstanceOf[DomainSpecificRater]
        }
    }
}
