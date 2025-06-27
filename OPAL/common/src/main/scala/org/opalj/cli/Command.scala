/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config

import org.rogach.scallop.ValueConverter

trait Command[T, R] {
    def name: String
    def argName: String = name
    def description: String
    def defaultValue: Option[T] = None
    def noshort: Boolean = true
    def choices: Seq[String] = Seq.empty

    type valueType = R

    final def apply(opalConfig: Config, cliConfig: OPALCommandLineConfig): Config = {
        this(opalConfig, cliConfig.get(this))
    }

    def apply(opalConfig: Config, value: Option[R]): Config = opalConfig
}

abstract class ConvertedCommand[T: ValueConverter, R] extends Command[T, R] {
    val conv: ValueConverter[T] = implicitly[ValueConverter[T]]
}

abstract class PlainCommand[T: ValueConverter] extends ConvertedCommand[T, T] {
    override final val choices = Seq.empty
}

abstract class ParsedCommand[T: ValueConverter, R] extends ConvertedCommand[T, R] {
    def parse(arg: T): R
}

trait ChoiceCommand[R] extends Command[String, R]

trait ForwardingCommand[T, S, R] extends Command[T, R] {
    val command: Command[T, S]
    def name: String = command.name
    override def argName: String = command.argName
    def description: String = command.description
    override def defaultValue: Option[T] = command.defaultValue
    override def noshort: Boolean = command.noshort
    override def choices: Seq[String] = command.choices
}

abstract class ExternalParser[T: ValueConverter, R](val command: Command[T, T]) extends ParsedCommand[T, R]
    with ForwardingCommand[T, T, R]
