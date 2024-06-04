/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string

import scala.collection.immutable.Seq
import scala.util.Try
import scala.util.matching.Regex

/**
 * @author Maximilian Rüsch
 */
sealed trait StringTreeNode {

    val children: Seq[StringTreeNode]

    def toRegex: String

    def simplify: StringTreeNode

    def constancyLevel: StringConstancyLevel.Value

    def collectParameterIndices: Set[Int] = children.flatMap(_.collectParameterIndices).toSet
    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode

    def isNeutralElement: Boolean = false
    def isInvalidElement: Boolean = false
}

object StringTreeNode {

    def reduceMultiple(trees: Seq[StringTreeNode]): StringTreeNode = {
        if (trees.size == 1) trees.head
        else if (trees.exists(_.isInvalidElement)) StringTreeInvalidElement
        else StringTreeOr(trees)
    }

    def lb: StringTreeNode = StringTreeDynamicString
}

case class StringTreeRepetition(child: StringTreeNode) extends StringTreeNode {

    override val children: Seq[StringTreeNode] = Seq(child)

    override def toRegex: String = s"(${child.toRegex})*"

    override def simplify: StringTreeNode = {
        val simplifiedChild = child.simplify
        if (simplifiedChild.isInvalidElement)
            StringTreeInvalidElement
        else if (simplifiedChild.isNeutralElement)
            StringTreeNeutralElement
        else
            StringTreeRepetition(simplifiedChild)
    }

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC

    override def collectParameterIndices: Set[Int] = child.collectParameterIndices

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeRepetition(child.replaceParameters(parameters))
}

case class StringTreeConcat(override val children: Seq[StringTreeNode]) extends StringTreeNode {

    override def toRegex: String = {
        children.size match {
            case 0 => StringTreeNeutralElement.toRegex
            case 1 => children.head.toRegex
            case _ => s"${children.map(_.toRegex).reduceLeft((o, n) => s"$o$n")}"
        }
    }

    override def simplify: StringTreeNode = {
        // TODO neutral concat something is always neutral
        val nonNeutralChildren = children.map(_.simplify).filterNot(_.isNeutralElement)
        if (nonNeutralChildren.isEmpty)
            StringTreeNeutralElement
        else
            StringTreeConcat(nonNeutralChildren)
    }

    override def constancyLevel: StringConstancyLevel.Value =
        children.map(_.constancyLevel).reduceLeft(StringConstancyLevel.determineForConcat)

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeConcat(children.map(_.replaceParameters(parameters)))
}

object StringTreeConcat {
    def fromNodes(children: StringTreeNode*): StringTreeConcat = new StringTreeConcat(children)
}

case class StringTreeOr private (override val children: Seq[StringTreeNode]) extends StringTreeNode {

    override def toRegex: String = {
        children.size match {
            case 0 => throw new IllegalStateException("Tried to convert StringTreeOr with no children to a regex!")
            case 1 => children.head.toRegex
            case _ => s"(${children.map(_.toRegex).reduceLeft((o, n) => s"$o|$n")})"
        }
    }

    override def simplify: StringTreeNode = {
        val nonNeutralChildren = children.map(_.simplify).filterNot(_.isNeutralElement)
        nonNeutralChildren.size match {
            case 0 => StringTreeNeutralElement
            case 1 => nonNeutralChildren.head
            case _ =>
                var newChildren = Seq.empty[StringTreeNode]
                nonNeutralChildren.foreach {
                    case orChild: StringTreeOr => newChildren :++= orChild.children
                    case child                 => newChildren :+= child
                }
                val distinctNewChildren = newChildren.distinct
                distinctNewChildren.size match {
                    case 1 => distinctNewChildren.head
                    case _ => StringTreeOr(distinctNewChildren)
                }
        }
    }

    override def constancyLevel: StringConstancyLevel.Value =
        children.map(_.constancyLevel).reduceLeft(StringConstancyLevel.determineMoreGeneral)

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeOr(children.map(_.replaceParameters(parameters)))
}

object StringTreeOr {

    def apply(children: Seq[StringTreeNode]): StringTreeNode = {
        if (children.isEmpty) {
            StringTreeNeutralElement
        } else {
            new StringTreeOr(children)
        }
    }

    def fromNodes(children: StringTreeNode*): StringTreeNode = {
        val nonNeutralChildren = children.filterNot(_.isNeutralElement)
        nonNeutralChildren.size match {
            case 0 => StringTreeNeutralElement
            case 1 => nonNeutralChildren.head
            case _ =>
                var newChildren = Seq.empty[StringTreeNode]
                nonNeutralChildren.foreach {
                    case orChild: StringTreeOr => newChildren :++= orChild.children
                    case child                 => newChildren :+= child
                }
                val distinctNewChildren = newChildren.distinct
                distinctNewChildren.size match {
                    case 1 => distinctNewChildren.head
                    case _ => StringTreeOr(distinctNewChildren)
                }
        }
    }
}

sealed trait SimpleStringTreeNode extends StringTreeNode {

    override final val children: Seq[StringTreeNode] = Seq.empty

    override final def simplify: StringTreeNode = this

    override def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = this
}

case class StringTreeConst(string: String) extends SimpleStringTreeNode {
    override def toRegex: String = Regex.quoteReplacement(string)

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    def isIntConst: Boolean = Try(string.toInt).isSuccess

    override def isNeutralElement: Boolean = string == ""
}

case class StringTreeParameter(index: Int) extends SimpleStringTreeNode {
    override def toRegex: String = ".*"

    override def collectParameterIndices: Set[Int] = Set(index)

    override def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        parameters.getOrElse(index, this)

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}

object StringTreeParameter {

    def forParameterPC(paramPC: Int): StringTreeParameter = {
        if (paramPC >= -1) {
            throw new IllegalArgumentException(s"Invalid parameter pc given: $paramPC")
        }
        // Parameters start at PC -2 downwards
        StringTreeParameter(Math.abs(paramPC + 2))
    }
}

object StringTreeNeutralElement extends SimpleStringTreeNode {
    override def toRegex: String = ""

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    override def isNeutralElement: Boolean = true
}

object StringTreeInvalidElement extends SimpleStringTreeNode {
    override def toRegex: String = throw new UnsupportedOperationException()

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    override def isInvalidElement: Boolean = true
}

object StringTreeNull extends SimpleStringTreeNode {
    // Using this element nested in some other element might lead to unexpected results...
    override def toRegex: String = "^null$"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT
}

object StringTreeDynamicString extends SimpleStringTreeNode {
    override def toRegex: String = ".*"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}

object StringTreeDynamicInt extends SimpleStringTreeNode {
    override def toRegex: String = "^-?\\d+$"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}

object StringTreeDynamicFloat extends SimpleStringTreeNode {
    override def toRegex: String = "^-?\\d*\\.{0,1}\\d+$"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}
