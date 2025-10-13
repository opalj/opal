/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config

import org.rogach.scallop.ValueConverter

trait Arg[+T, R] {
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
    override final val choices: Seq[String] = Seq.empty
}

abstract class ParsedArg[T: ValueConverter, R] extends ConvertedArg[T, R] {
    def parse(arg: T): R
}

abstract class PropertyArg[T: ValueConverter] extends ConvertedArg[T, Map[String, T]] {
    def char: Char
    def keyName: String = "key"
    def valueName: String = "value"

    override final val name = char.toString
    override final val argName: String = null
    override final val defaultValue: Option[T] = None
    override final val short: Char = '\u0000'
    override final val noshort: Boolean = true
    override final val choices: Seq[String] = Seq.empty
}

trait ChoiceArg[R] extends Arg[String, R] {
    override def choices: Seq[String] = ???
}

trait ForwardingArg[T, S, R] extends Arg[T, R] {
    val arg: Arg[T, S]
    def name: String = arg.name
    override def argName: String = arg.argName
    def description: String = arg.description
    override def defaultValue: Option[T] = arg.defaultValue
    override def short: Char = arg.short
    override def noshort: Boolean = arg.noshort
    override def choices: Seq[String] = arg.choices
}

abstract class ExternalParser[T: ValueConverter, R](val arg: Arg[T, T]) extends ParsedArg[T, R]
    with ForwardingArg[T, T, R]
