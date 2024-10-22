package org.opalj.support.parser

import org.opalj.tac.fpcf.analyses.purity.{DomainSpecificRater, SystemOutLoggingAllExceptionRater}

object RaterCommandParser {
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
