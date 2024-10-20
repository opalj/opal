package org.opalj.Commandline_base.commandlines

import org.opalj.tac.fpcf.analyses.purity.{DomainSpecificRater, SystemOutLoggingAllExceptionRater}

object RaterCommand extends OpalPlainCommand[String] {
    override var name: String = "rater"
    override var argName: String = "rater"
    override var description: String = "class name of the rater for domain-specific actions"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    def parse(rater: String) : DomainSpecificRater = {
        if (rater == null) {
            SystemOutLoggingAllExceptionRater
        } else {
            import scala.reflect.runtime.universe.runtimeMirror
            val mirror = runtimeMirror(getClass.getClassLoader)
            val module = mirror.staticModule(rater)
            mirror.reflectModule(module).instance.asInstanceOf[DomainSpecificRater]
        }
    }
}
