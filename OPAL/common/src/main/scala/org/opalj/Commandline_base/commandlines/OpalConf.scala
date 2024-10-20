package org.opalj.Commandline_base.commandlines

import org.rogach.scallop.{ScallopConf, ScallopOption, ValueConverter}

trait OpalConf {self: ScallopConf =>
    def getPlainScallopOption[T](command: OpalPlainCommand[T])(implicit conv: ValueConverter[T]) : ScallopOption[T] = {
        opt[T](name = command.name, argName = command.argName, descr = command.description, default = command.defaultValue, noshort = command.noshort)
    }

    def getChoiceScallopOption(command: OpalChoiceCommand) : ScallopOption[String] = {
        choice(name = command.name, argName = command.argName, descr = command.description, default = command.defaultValue, noshort = command.noshort, choices = command.choices)
    }
}
