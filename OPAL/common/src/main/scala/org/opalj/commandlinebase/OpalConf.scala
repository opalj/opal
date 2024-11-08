/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.ScallopOption
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.exceptions.Help
import org.rogach.scallop.exceptions.ScallopException

/**
 * `OpalConf` is a utility trait designed to enhance the `ScallopConf` functionality by providing additional methods
 * for handling command-line argument parsing in the OPAL framework. This trait is intended to be mixed into
 * `ScallopConf` configurations, enabling more customized parsing mechanisms with reusable methods for specific command
 * types and error handling.
 */
trait OpalConf {
    self: ScallopConf =>
    def getPlainScallopOption[T](command: OpalPlainCommand[T])(implicit conv: ValueConverter[T]): ScallopOption[T] =
        opt[T](
            name = command.name,
            argName = command.argName,
            descr = command.description,
            default = command.defaultValue,
            noshort = command.noshort
        )

    def getChoiceScallopOption(command: OpalChoiceCommand): ScallopOption[String] =
        choice(
            name = command.name,
            argName = command.argName,
            descr = command.description,
            default = command.defaultValue,
            noshort = command.noshort,
            choices = command.choices
        )

    def parseCommand[T](command: ScallopOption[T]): Option[T] = if (!command.isDefined) None else Some(command.apply())

    def parseCommandWithInternalParser[T, R](command: ScallopOption[T], internalParser: OpalCommand): Option[R] =
        if (!command.isDefined) None else Some(internalParser.parse(command.apply()).asInstanceOf[R])

    def parseCommandWithExternalParser[T, R](
        command:        ScallopOption[T],
        externalParser: OpalCommandExternalParser[T, R]
    ): Option[R] = if (!command.isDefined) None else Some(externalParser.parse(command.apply()).asInstanceOf[R])

    override def onError(e: Throwable): Unit = e match {
        case Help("") =>
            printHelp()
            sys.exit(0)
        case Help(_) =>
            subcommand.foreach(_.printHelp())
            sys.exit(0)
        case ScallopException(message) =>
            println(s"Error: $message")
            printHelp()
        case _ =>
            println(s"Unexpected error: ${e.getMessage}")
    }

}
