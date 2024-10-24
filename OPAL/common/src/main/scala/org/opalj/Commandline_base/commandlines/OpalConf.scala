package org.opalj.Commandline_base.commandlines

import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption, ValueConverter}

trait OpalConf {self: ScallopConf =>
    def getPlainScallopOption[T](command: OpalPlainCommand[T])(implicit conv: ValueConverter[T]) : ScallopOption[T] = {
        opt[T](name = command.name, argName = command.argName, descr = command.description, default = command.defaultValue, noshort = command.noshort)
    }

    def getChoiceScallopOption(command: OpalChoiceCommand) : ScallopOption[String] = {
        choice(name = command.name, argName = command.argName, descr = command.description, default = command.defaultValue, noshort = command.noshort, choices = command.choices)
    }

    def parseCommand[T](command: ScallopOption[T]): Option[T] = {
        if (!command.isDefined) None else Some(command.apply())
    }

    def parseCommandWithInternalParser[T, R](command: ScallopOption[T], internalParser: OpalCommand): Option[R] = {
        if (!command.isDefined) None else Some(internalParser.parse(command.apply()).asInstanceOf[R])
    }

    def parseCommandWithExternalParser[T, R](command: ScallopOption[T], externalParser: OpalCommandExternalParser): Option[R] = {
        if (!command.isDefined) None else Some(externalParser.parse(command.apply()).asInstanceOf[R])
    }

    override def onError(e: Throwable): Unit = e match {
        case ScallopException(message) =>
            println(s"Error: $message")
            printHelp()
        case _ =>
            println(s"Unexpected error: ${e.getMessage}")
    }
}
