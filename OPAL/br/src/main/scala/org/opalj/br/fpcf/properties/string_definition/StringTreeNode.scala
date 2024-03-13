/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string_definition

import scala.collection.immutable.Seq
import scala.collection.mutable
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

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode

    def isNeutralElement: Boolean = false
}

case class StringTreeRepetition(
    child:      StringTreeNode,
    lowerBound: Option[Int] = None,
    upperBound: Option[Int] = None
) extends StringTreeNode {

    override val children: Seq[StringTreeNode] = Seq(child)

    override def toRegex: String = {
        (lowerBound, upperBound) match {
            case (Some(lb), Some(ub)) => s"(${child.toRegex}){$lb,$ub}"
            case (Some(lb), None)     => s"(${child.toRegex}){$lb,}"
            case _                    => s"(${child.toRegex})*"
        }
    }

    override def simplify: StringTreeNode = {
        val simplifiedChild = child.simplify
        if (simplifiedChild.isNeutralElement)
            StringTreeNeutralElement
        else
            this.copy(child = simplifiedChild)
    }

    override def constancyLevel: StringConstancyLevel.Value = child.constancyLevel

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        this.copy(child = child.replaceParameters(parameters))
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

case class StringTreeOr(override val children: Seq[StringTreeNode]) extends StringTreeNode {

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
                StringTreeOr(removeDuplicates(newChildren))
        }
    }

    private def removeDuplicates(someChildren: Seq[StringTreeNode]): Seq[StringTreeNode] = {
        val seen = mutable.HashSet.empty[String]
        someChildren.flatMap {
            case next @ StringTreeConst(string) =>
                if (!seen.contains(string)) {
                    seen += string
                    Some(next)
                } else None
            case other => Some(other)
        }
    }

    override def constancyLevel: StringConstancyLevel.Value =
        children.map(_.constancyLevel).reduceLeft(StringConstancyLevel.determineMoreGeneral)

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeOr(children.map(_.replaceParameters(parameters)))
}

object StringTreeOr {
    def fromNodes(children: StringTreeNode*): StringTreeOr = new StringTreeOr(children)
}

case class StringTreeCond(child: StringTreeNode) extends StringTreeNode {

    override val children: Seq[StringTreeNode] = Seq(child)

    override def toRegex: String = {
        val childRegex = child.toRegex

        // IMPROVE dont wrap and immediately unwrap in ()
        val resultingRegex = if (childRegex.startsWith("(") && childRegex.endsWith(")")) {
            s"(${childRegex.substring(1, childRegex.length - 1)})?"
        } else {
            s"($childRegex)?"
        }

        resultingRegex
    }

    override def simplify: StringTreeNode = {
        child.simplify match {
            case condNode: StringTreeCond             => condNode
            case repetitionNode: StringTreeRepetition => repetitionNode
            case node if node.isNeutralElement        => StringTreeNeutralElement
            case node                                 => StringTreeCond(node)
        }
    }

    override def constancyLevel: StringConstancyLevel.Value = child.constancyLevel

    def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeCond(child.replaceParameters(parameters))
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
}

case class StringTreeParameter(index: Int) extends SimpleStringTreeNode {
    override def toRegex: String = ".*"

    override def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        parameters.getOrElse(index, this)

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}

object StringTreeNeutralElement extends SimpleStringTreeNode {
    override def toRegex: String = ""

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    override def isNeutralElement: Boolean = true
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
