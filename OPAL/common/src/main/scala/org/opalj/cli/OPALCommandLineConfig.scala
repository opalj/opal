/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

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
trait OPALCommandLineConfig {
    self: ScallopConf =>

    private var commands: Set[Command[_, _]] = Set.empty
    def commandsIterator: Iterator[Command[_, _]] = commands.iterator

    /**
     * Defines (additional) commands for this configuration
     */
    protected def commands(cs: Command[_, _]*): Unit = {
        commands ++= cs
    }

    private var required: Set[Command[_, _]] = Set.empty
    private var values: Map[Command[_, _], Any] = _

    /**
     * Gets the value of an (optional) argument or None if the argument was not supplied
     */
    def get[R](command: Command[_, R]): Option[R] = {
        values.get(command).asInstanceOf[Option[R]]
    }

    /**
     * Gets the value of a (required) argument.
     *
     * @throws NoSuchElementException if the argument was not supplied (which means it was not marked as required!)
     */
    def apply[R](command: Command[_, R]): R = {
        values(command).asInstanceOf[R]
    }

    protected implicit class CommandExt(c: Command[_, _]) {

        /**
         * Makes an argument required
         */
        def ! : Command[_, _] = {
            required += c
            c
        }

        /**
         * Makes an argument optional (if it was required by a super class)
         */
        def ? : Command[_, _] = {
            required -= c
            c
        }

        /**
         * Requires exactly one of the given arguments
         */
        def ^(c2: Command[_, _]): Command[_, _] = {
            MutuallyExclusive(c, c2)
        }
    }

    private case class MutuallyExclusive(cs: Command[_, _]*) extends Command[Any, Any] {
        override def name: String = ???
        override def description: String = ???
    }

    private object MutuallyExclusive {
        def apply(c1: Command[_, _], c2: Command[_, _]): MutuallyExclusive = (c1, c2) match {
            case (r1: MutuallyExclusive, r2: MutuallyExclusive) => new MutuallyExclusive((r1.cs ++ r2.cs) *)
            case (r1: MutuallyExclusive, _)                     => new MutuallyExclusive((r1.cs :+ c2) *)
            case (_, r2: MutuallyExclusive)                     => new MutuallyExclusive((r2.cs :+ c1) *)
            case _                                              => new MutuallyExclusive(Seq(c1, c2) *)
        }
    }

    protected def init(): Unit = {

        def getScallopOptionFlat(c: Command[_, _]): ScallopOption[_] = c match {
            case choiceCommand: ChoiceCommand[_] => getChoiceScallopOption(choiceCommand)
            case convertedCommand: ConvertedCommand[_, _] =>
                getRegularScallopOption(convertedCommand)(convertedCommand.conv)
            case _: MutuallyExclusive => throw new IllegalArgumentException("Cannot nest mutually exclusive arguments")
        }

        def getScallopOption(c: Command[_, _]): IterableOnce[(Command[_, _], ScallopOption[_])] = c match {
            case me: MutuallyExclusive => {
                val options = me.cs.map { c => c -> getScallopOptionFlat(c) }
                if (required.contains(c))
                    requireOne(options.map(_._2) *)
                else
                    mutuallyExclusive(options.map(_._2) *)
                options
            }
            case _ => Iterator(c -> getScallopOptionFlat(c))
        }

        val rawValues = commands.iterator.flatMap(getScallopOption).toMap

        verify()

        values = rawValues.collect {
            case (command, value) if value.isDefined =>
                (
                    command,
                    command match {
                        case parsedCommand: ParsedCommand[_, _] => parseCommandWithParser(value, parsedCommand.parse)
                        case _: Command[_, _]                   => value()
                    }
                )
        }
    }

    private def getRegularScallopOption[T](command: ConvertedCommand[T, _])(implicit
        conv: ValueConverter[T]
    ): ScallopOption[T] =
        opt[T](
            name = command.name,
            argName = command.argName,
            descr = command.description,
            default = command.defaultValue,
            noshort = command.noshort,
            required = required.contains(command)
        )

    private def getChoiceScallopOption(command: Command[String, _]): ScallopOption[String] =
        choice(
            name = command.name,
            argName = command.argName,
            descr = command.description,
            default = command.defaultValue,
            noshort = command.noshort,
            choices = command.choices,
            required = required.contains(command)
        )

    private def parseCommandWithParser[T, R](value: ScallopOption[_], parse: T => R): R =
        parse(value.apply().asInstanceOf[T])

    def setupConfig(isLibrary: Boolean): Config = {
        var config: Config =
            if (isLibrary)
                ConfigFactory.load("LibraryProject.conf")
            else
                ConfigFactory.load("CommandLineProject.conf")

        for (command <- commands)
            config = command(config, this)

        config
    }

    helpWidth(120)
    appendDefaultToDescription = true

    override protected def onError(e: Throwable): Unit = {
        implicit val logContext: LogContext = GlobalLogContext
        e match {
            case Help("") =>
                printHelp()
            case Help(_) =>
                subcommand.foreach(_.printHelp())
            case ScallopException(message) =>
                OPALLogger.error("analysis configuration", message)
                printHelp()
            case throwable =>
                OPALLogger.error("analysis configuration", "", throwable)
        }
        sys.exit(0)
    }
}
