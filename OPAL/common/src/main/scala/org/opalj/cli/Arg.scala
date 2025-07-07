/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config

import org.rogach.scallop.ValueConverter

trait Arg[T, R] {
    def name: String
    def argName: String = name
    def description: String
    def defaultValue: Option[T] = None
    def short: Char = '\u0000'
    def noshort: Boolean = true
    def choices: Seq[String] = Seq.empty

    type valueType = R

    final def apply(opalConfig: Config, cliConfig: OPALCommandLineConfig): Config = {
        this(opalConfig, cliConfig.get(this))
    }

    def apply(opalConfig: Config, value: Option[R]): Config = opalConfig

    def commands(): IterableOnce[Arg[_, _]] = Iterator(this)
}

abstract class ConvertedArg[T: ValueConverter, R] extends Arg[T, R] {
    val conv: ValueConverter[T] = implicitly[ValueConverter[T]]
}

abstract class PlainArg[T: ValueConverter] extends ConvertedArg[T, T] {
    override final val choices = Seq.empty
}

abstract class ParsedArg[T: ValueConverter, R] extends ConvertedArg[T, R] {
    def parse(arg: T): R
}

trait ChoiceArg[R] extends Arg[String, R] {
    override def choices: Seq[String] = ???
}

trait ForwardingArg[T, S, R] extends Arg[T, R] {
    val command: Arg[T, S]
    def name: String = command.name
    override def argName: String = command.argName
    def description: String = command.description
    override def defaultValue: Option[T] = command.defaultValue
    override def short: Char = command.short
    override def noshort: Boolean = command.noshort
    override def choices: Seq[String] = command.choices
}

abstract class ExternalParser[T: ValueConverter, R](val command: Arg[T, T]) extends ParsedArg[T, R]
    with ForwardingArg[T, T, R]
