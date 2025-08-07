/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.ScallopOption
import org.rogach.scallop.ScallopOptionGroup
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.exceptions.Help
import org.rogach.scallop.exceptions.ScallopException

import org.opalj.log.DevNullLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * `OpalConf` is a utility trait designed to enhance the `ScallopConf` functionality by providing additional methods
 * for handling command-line argument parsing in the OPAL framework. This trait is intended to be mixed into
 * `ScallopConf` configurations, enabling more customized parsing mechanisms with reusable methods for specific command
 * types and error handling.
 */
trait OPALCommandLineConfig {
    self: ScallopConf =>

    val description: String
    banner(description + "\n")

    private var definedArgs: Set[Arg[_, _]] = Set.empty
    def argsIterator: Iterator[Arg[_, _]] = definedArgs.iterator

    private val runnerSpecificGroup = group("Runner-specific arguments:")
    private val generalConfigGroup = group("General configuration:")
    protected var argGroups: Map[Arg[_, _], ScallopOptionGroup] = Map.empty

    generalArgs(NoLogsArg, ConfigFileArg, RenderConfigArg)

    /**
     * Defines (additional) args for this configuration
     */
    protected def args(as: Arg[_, _]*): Unit = {
        definedArgs ++= as
    }
    /**
     * Defines (additional) general args for this configuration
     */
    protected def generalArgs(as: Arg[_, _]*): Unit = {
        definedArgs ++= as
        argGroups ++= as.iterator.flatMap(_.commands()).map(c => c -> generalConfigGroup)
    }

    private var required: Set[Arg[_, _]] = Set.empty
    private var values: Map[Arg[_, _], Any] = _

    /**
     * Gets the value of an (optional) argument or None if the argument was not supplied
     */
    def get[R](arg: Arg[_, R]): Option[R] = {
        values.get(arg).asInstanceOf[Option[R]]
    }

    /**
     * Gets the value of an (optional) argument or a default value if the argument was not supplied
     */
    def get[R](arg: Arg[_, R], default: => R): R = {
        get(arg).getOrElse(default)
    }

    /**
     * Gets the value of a (required) argument.
     *
     * @throws NoSuchElementException if the argument was not supplied (which means it was not marked as required!)
     */
    def apply[R](arg: Arg[_, R]): R = {
        values(arg).asInstanceOf[R]
    }

    protected implicit class CommandExt(a: Arg[_, _]) {

        /**
         * Makes an argument required
         */
        def ! : Arg[_, _] = {
            required += a
            a
        }

        /**
         * Makes an argument optional (if it was required by a super class)
         */
        def ? : Arg[_, _] = {
            required -= a
            a
        }

        /**
         * Requires exactly one of the given arguments
         */
        def ^(a2: Arg[_, _]): Arg[_, _] = {
            MutuallyExclusive(a, a2)
        }
    }

    private case class MutuallyExclusive(as: Seq[Arg[_, _]]) extends Arg[Any, Any] {
        override val name: String = ""
        override val description: String = ""

        override def commands(): IterableOnce[Arg[_, _]] = as.iterator.flatMap(_.commands())
    }

    private object MutuallyExclusive {
        def apply(a1: Arg[_, _], a2: Arg[_, _]): MutuallyExclusive = (a1, a2) match {
            case (r1: MutuallyExclusive, r2: MutuallyExclusive) => new MutuallyExclusive(r1.as ++ r2.as)
            case (r1: MutuallyExclusive, _)                     => new MutuallyExclusive(r1.as :+ a2)
            case (_, r2: MutuallyExclusive)                     => new MutuallyExclusive(r2.as :+ a1)
            case _                                              => new MutuallyExclusive(Seq(a1, a2))
        }
    }

    def init(): Unit = {

        def getScallopOptionFlat(a: Arg[_, _]): ScallopOption[_] = a match {
            case choiceArg: ChoiceArg[_] => getChoiceScallopOption(choiceArg)
            case convertedArg: ConvertedArg[_, _] =>
                getRegularScallopOption(convertedArg)(convertedArg.conv)
            case _: MutuallyExclusive => throw new IllegalArgumentException("Cannot nest mutually exclusive arguments")
        }

        def getScallopOption(a: Arg[_, _]): IterableOnce[(Arg[_, _], ScallopOption[_])] = a match {
            case me: MutuallyExclusive => {
                val options = me.as.map { c => c -> getScallopOptionFlat(c) }
                if (required.contains(a))
                    requireOne(options.map(_._2): _*)
                else
                    mutuallyExclusive(options.map(_._2): _*)
                options
            }
            case _ => Iterator(a -> getScallopOptionFlat(a))
        }

        val rawValues = definedArgs.iterator.flatMap(getScallopOption).toMap

        verify()

        def forwardedArgs(forwardedArg: Arg[_, _]): Iterator[Arg[_, _]] = {
            forwardedArg match {
                case forwardingArg: ForwardingArg[_, _, _] => Iterator(forwardingArg) ++ forwardedArgs(forwardingArg.arg)
                case _                                     => Iterator(forwardedArg)
            }
        }

        values = rawValues.flatMap {
            case (arg, scallopOpt) if scallopOpt.isDefined =>
                val value = arg match {
                    case parsedArg: ParsedArg[_, _] => parseArgWithParser(scallopOpt, parsedArg.parse)
                    case _: Arg[_, _]               => scallopOpt()
                }
                forwardedArgs(arg).map { arg => arg -> value }
            case _ => None
        }
    }

    private def getRegularScallopOption[T](arg: ConvertedArg[T, _])(implicit
        conv: ValueConverter[T]
    ): ScallopOption[T] =
        opt[T](
            name = arg.name,
            argName = arg.argName,
            descr = arg.description,
            default = arg.defaultValue,
            short = arg.short,
            noshort = arg.noshort,
            required = required.contains(arg),
            group = argGroups.getOrElse(arg, runnerSpecificGroup)
        )

    private def getChoiceScallopOption(arg: Arg[String, _]): ScallopOption[String] =
        choice(
            name = arg.name,
            argName = arg.argName,
            descr = arg.description,
            default = arg.defaultValue,
            short = arg.short,
            noshort = arg.noshort,
            choices = arg.choices,
            required = required.contains(arg),
            group = argGroups.getOrElse(arg, runnerSpecificGroup)
        )

    private def parseArgWithParser[T, R](value: ScallopOption[_], parse: T => R): R =
        parse(value.apply().asInstanceOf[T])

    def setupConfig(isLibrary: Boolean): Config = {
        if (get(NoLogsArg, false))
            OPALLogger.updateLogger(GlobalLogContext, DevNullLogger)

        var config: Config =
            if (get(ConfigFileArg).isDefined)
                ConfigFactory.load(apply(ConfigFileArg))
            else if (isLibrary)
                ConfigFactory.load("LibraryProject.conf")
            else
                ConfigFactory.load("CommandLineProject.conf")

        for (arg <- definedArgs)
            config = arg(config, this)

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
