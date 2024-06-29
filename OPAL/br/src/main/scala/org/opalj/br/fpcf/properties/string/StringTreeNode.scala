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

    def isEmpty: Boolean = false
    def isInvalid: Boolean = false
}

object StringTreeNode {

    def lb: StringTreeNode = StringTreeDynamicString
    def ub: StringTreeNode = StringTreeInvalidElement
}

case class StringTreeRepetition(child: StringTreeNode) extends StringTreeNode {

    override val children: Seq[StringTreeNode] = Seq(child)

    override def toRegex: String = s"(${child.toRegex})*"

    override def simplify: StringTreeNode = {
        val simplifiedChild = child.simplify
        if (simplifiedChild.isInvalid)
            StringTreeInvalidElement
        else if (simplifiedChild.isEmpty)
            StringTreeEmptyConst
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
            case 0 => throw new IllegalStateException("Tried to convert StringTreeConcat with no children to a regex!")
            case 1 => children.head.toRegex
            case _ => s"${children.map(_.toRegex).reduceLeft((o, n) => s"$o$n")}"
        }
    }

    override def simplify: StringTreeNode = {
        val nonEmptyChildren = children.map(_.simplify).filterNot(_.isEmpty)
        if (nonEmptyChildren.exists(_.isInvalid)) {
            StringTreeInvalidElement
        } else {
            nonEmptyChildren.size match {
                case 0 => StringTreeEmptyConst
                case 1 => nonEmptyChildren.head
                case _ =>
                    var newChildren = Seq.empty[StringTreeNode]
                    nonEmptyChildren.foreach {
                        case concatChild: StringTreeConcat => newChildren :++= concatChild.children
                        case child                         => newChildren :+= child
                    }
                    StringTreeConcat(newChildren)
            }
        }
    }

    override def constancyLevel: StringConstancyLevel.Value =
        children.map(_.constancyLevel).reduceLeft(StringConstancyLevel.determineForConcat)

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeConcat(children.map(_.replaceParameters(parameters)))
}

object StringTreeConcat {
    def fromNodes(children: StringTreeNode*): StringTreeNode = {
        if (children.isEmpty || children.exists(_.isInvalid)) {
            StringTreeInvalidElement
        } else {
            new StringTreeConcat(children)
        }
    }
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
        val nonEmptyChildren = children.map(_.simplify).filterNot(_.isEmpty).filterNot(_.isInvalid)
        nonEmptyChildren.size match {
            case 0 => StringTreeInvalidElement
            case 1 => nonEmptyChildren.head
            case _ =>
                var newChildren = Seq.empty[StringTreeNode]
                nonEmptyChildren.foreach {
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
            StringTreeInvalidElement
        } else if (children.take(2).size == 1) {
            children.head
        } else {
            new StringTreeOr(children)
        }
    }

    def fromNodes(children: StringTreeNode*): StringTreeNode = {
        val nonNeutralDistinctChildren = children.distinct.filterNot(_.isEmpty)
        nonNeutralDistinctChildren.size match {
            case 0 => StringTreeInvalidElement
            case 1 => nonNeutralDistinctChildren.head
            case _ =>
                var newChildren = Seq.empty[StringTreeNode]
                nonNeutralDistinctChildren.foreach {
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

    override def isEmpty: Boolean = string == ""
}

object StringTreeEmptyConst extends StringTreeConst("") {

    override def isEmpty: Boolean = true
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

object StringTreeInvalidElement extends SimpleStringTreeNode {
    override def toRegex: String = throw new UnsupportedOperationException()

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    override def isInvalid: Boolean = true
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
