package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.tac.fpcf.analyses.purity.{DomainSpecificRater, SystemOutLoggingAllExceptionRater}

object RaterCommandExternalParser extends OpalCommandExternalParser{
    override def parse[T](arg: T) : DomainSpecificRater = {
        val rater = arg.asInstanceOf[String]

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
