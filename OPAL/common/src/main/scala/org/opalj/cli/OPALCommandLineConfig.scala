/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import scala.compiletime.uninitialized

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.rogach.scallop.LazyMap
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.ScallopOption
import org.rogach.scallop.ScallopOptionBase
import org.rogach.scallop.ScallopOptionGroup
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.exceptions.Help
import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.propsConverter

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

    private var definedArgs: Set[Arg[?, ?]] = Set.empty
    def argsIterator: Iterator[Arg[?, ?]] = definedArgs.iterator

    private val runnerSpecificGroup = group("Runner-specific arguments:")
    private val generalConfigGroup = group("General configuration:")
    protected var argGroups: Map[Arg[?, ?], ScallopOptionGroup] = Map.empty

    generalArgs(NoLogsArg, ConfigFileArg, RenderConfigArg, ConfigOverrideArg)

    /**
     * Defines (additional) args for this configuration
     */
    protected def args(as: Arg[?, ?]*): Unit = {
        definedArgs ++= as
    }
    /**
     * Defines (additional) general args for this configuration
     */
    protected def generalArgs(as: Arg[?, ?]*): Unit = {
        definedArgs ++= as
        argGroups ++= as.iterator.flatMap(_.commands()).map(c => c -> generalConfigGroup)
    }

    private var required: Set[Arg[?, ?]] = Set.empty
    private var values: Map[Arg[?, ?], Any] = uninitialized

    /**
     * Gets the value of an (optional) argument or None if the argument was not supplied
     */
    def get[R](arg: Arg[?, R]): Option[R] = {
        values.get(arg).asInstanceOf[Option[R]]
    }

    /**
     * Gets the value of an (optional) argument or a default value if the argument was not supplied
     */
    def get[R](arg: Arg[?, R], default: => R): R = {
        get(arg).getOrElse(default)
    }

    /**
     * Gets the value of a (required) argument.
     *
     * @throws NoSuchElementException if the argument was not supplied (which means it was not marked as required!)
     */
    def apply[R](arg: Arg[?, R]): R = {
        values(arg).asInstanceOf[R]
    }

    protected implicit class CommandExt(a: Arg[?, ?]) {

        /**
         * Makes an argument required
         */
        def ! : Arg[?, ?] = {
            required += a
            a
        }

        /**
         * Makes an argument optional (if it was required by a super class)
         */
        def ? : Arg[?, ?] = {
            required -= a
            a
        }

        /**
         * Requires exactly one of the given arguments
         */
        def ^(a2: Arg[?, ?]): Arg[?, ?] = {
            MutuallyExclusive(a, a2)
        }
    }

    private case class MutuallyExclusive(as: Seq[Arg[?, ?]]) extends Arg[Any, Any] {
        override val name: String = ""
        override val description: String = ""

        override def commands(): IterableOnce[Arg[?, ?]] = as.iterator.flatMap(_.commands())
    }

    private object MutuallyExclusive {
        def apply(a1: Arg[?, ?], a2: Arg[?, ?]): MutuallyExclusive = (a1, a2) match {
            case (r1: MutuallyExclusive, r2: MutuallyExclusive) => new MutuallyExclusive(r1.as ++ r2.as)
            case (r1: MutuallyExclusive, _)                     => new MutuallyExclusive(r1.as :+ a2)
            case (_, r2: MutuallyExclusive)                     => new MutuallyExclusive(r2.as :+ a1)
            case _                                              => new MutuallyExclusive(Seq(a1, a2))
        }
    }

    def init(): Unit = {

        def getScallopOptionFlat(a: Arg[?, ?]): ScallopOptionBase = a match {
            case choiceArg: ChoiceArg[?]     => getChoiceScallopOption(choiceArg)
            case propertyArg: PropertyArg[?] =>
                getPropertyScallopOption(propertyArg)(using propsConverter(propertyArg.conv))
            case convertedArg: ConvertedArg[?, ?] => getRegularScallopOption(convertedArg)(using convertedArg.conv)
            case _: MutuallyExclusive             => throw new IllegalArgumentException("Cannot nest mutually exclusive arguments")
        }

        def getScallopOption(a: Arg[?, ?]): IterableOnce[(Arg[?, ?], ScallopOptionBase)] = a match {
            case me: MutuallyExclusive => {
                val options = me.as.map { c => c -> getScallopOptionFlat(c) }
                if (required.contains(a))
                    requireOne(options.map(_._2)*)
                else
                    mutuallyExclusive(options.map(_._2)*)
                options
            }
            case _ => Iterator(a -> getScallopOptionFlat(a))
        }

        val rawValues = definedArgs.iterator.flatMap(getScallopOption).toMap

        verify()

        def getValue[T, R](arg: Arg[T, R], value: T): Any = {
            arg match {
                case parsedArg: ParsedArg[T @unchecked, ?] => parsedArg.parse(value)
                case _: Arg[?, ?]                          => value
            }
        }

        def forwardedArgs(forwardedArg: Arg[?, ?]): Iterator[Arg[?, ?]] = {
            forwardedArg match {
                case forwardingArg: ForwardingArg[?, ?, ?] => Iterator(forwardingArg) ++ forwardedArgs(forwardingArg.arg)
                case _                                     => Iterator(forwardedArg)
            }
        }

        values = rawValues.flatMap {
            case (arg, scallopOpt: ScallopOption[_]) if scallopOpt.isDefined =>
                val value = getValue(arg, scallopOpt.apply())
                forwardedArgs(arg).map { arg => arg -> value }
            case (arg, properties: LazyMap[?, ?]) =>
                val values = properties.iterator.map { property => (property._1, getValue(arg, property._2)) }.toMap
                forwardedArgs(arg).map { arg => arg -> values }
            case _ => None
        }
    }

    private def getPropertyScallopOption[T](arg: PropertyArg[T])(implicit
        conv: ValueConverter[Map[String, T]]
    ): LazyMap[String, T] = {
        props[T](
            name = arg.char,
            descr = arg.description,
            keyName = arg.keyName,
            valueName = arg.valueName,
            group = argGroups.getOrElse(arg, runnerSpecificGroup)
        )
    }

    private def getRegularScallopOption[T](arg: ConvertedArg[T, ?])(implicit
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

    private def getChoiceScallopOption(arg: Arg[String, ?]): ScallopOption[String] =
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
