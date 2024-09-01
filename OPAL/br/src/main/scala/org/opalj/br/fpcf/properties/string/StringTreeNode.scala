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

    def sorted: StringTreeNode

    private var _regex: Option[String] = None
    final def toRegex: String = {
        if (_regex.isEmpty) {
            _regex = Some(_toRegex)
        }

        _regex.get
    }
    def _toRegex: String

    def simplify: StringTreeNode

    def constancyLevel: StringConstancyLevel.Value

    def collectParameterIndices: Set[Int] = children.flatMap(_.collectParameterIndices).toSet
    final def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = {
        if (parameters.isEmpty) this
        else _replaceParameters(parameters)
    }
    protected def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode

    def isEmpty: Boolean = false
    def isInvalid: Boolean = false
}

sealed trait CachedSimplifyNode extends StringTreeNode {

    private var _simplified = false
    final def simplify: StringTreeNode = {
        if (_simplified) {
            this
        } else {
            _simplify match {
                case cr: CachedSimplifyNode =>
                    cr._simplified = true
                    cr
                case r => r
            }
        }
    }

    protected def _simplify: StringTreeNode
}

sealed trait CachedHashCode extends Product {

    // Performance optimizations
    private lazy val _hashCode = scala.util.hashing.MurmurHash3.productHash(this)
    override def hashCode(): Int = _hashCode
    override def canEqual(obj: Any): Boolean = obj.hashCode() == _hashCode
}

object StringTreeNode {

    def lb: StringTreeNode = StringTreeDynamicString
    def ub: StringTreeNode = StringTreeInvalidElement
}

case class StringTreeRepetition(child: StringTreeNode) extends CachedSimplifyNode with CachedHashCode {

    override val children: Seq[StringTreeNode] = Seq(child)

    override def _toRegex: String = s"(${child.toRegex})*"

    override def sorted: StringTreeNode = this

    override def _simplify: StringTreeNode = {
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

    def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        StringTreeRepetition(child.replaceParameters(parameters))
}

case class StringTreeConcat(override val children: Seq[StringTreeNode]) extends CachedSimplifyNode with CachedHashCode {

    override def _toRegex: String = {
        children.size match {
            case 0 => throw new IllegalStateException("Tried to convert StringTreeConcat with no children to a regex!")
            case 1 => children.head.toRegex
            case _ => s"${children.map(_.toRegex).reduceLeft((o, n) => s"$o$n")}"
        }
    }

    override def sorted: StringTreeNode = StringTreeConcat(children.map(_.sorted))

    override def _simplify: StringTreeNode = {
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

    def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = {
        val childrenWithChange = children.map { c =>
            val nc = c.replaceParameters(parameters)
            (nc, c ne nc)
        }

        if (childrenWithChange.exists(_._2)) {
            StringTreeConcat(childrenWithChange.map(_._1))
        } else {
            this
        }
    }
}

object StringTreeConcat {
    def fromNodes(children: StringTreeNode*): StringTreeNode = {
        if (children.isEmpty || children.exists(_.isInvalid)) {
            StringTreeInvalidElement
        } else if (children.forall(_.isEmpty)) {
            StringTreeEmptyConst
        } else {
            new StringTreeConcat(children.filterNot(_.isEmpty))
        }
    }
}

trait StringTreeOr extends CachedSimplifyNode with CachedHashCode {

    protected val _children: Iterable[StringTreeNode]

    override final val children: Seq[StringTreeNode] = _children.toSeq

    override def _toRegex: String = {
        children.size match {
            case 0 => throw new IllegalStateException("Tried to convert StringTreeOr with no children to a regex!")
            case 1 => children.head.toRegex
            case _ => s"(${children.map(_.toRegex).reduceLeft((o, n) => s"$o|$n")})"
        }
    }

    override def constancyLevel: StringConstancyLevel.Value =
        _children.map(_.constancyLevel).reduceLeft(StringConstancyLevel.determineMoreGeneral)

    override def collectParameterIndices: Set[Int] = _children.flatMap(_.collectParameterIndices).toSet
}

object StringTreeOr {

    def apply(children: Seq[StringTreeNode]): StringTreeNode = apply(children.toSet)

    def apply(children: Set[StringTreeNode]): StringTreeNode = {
        if (children.isEmpty) {
            StringTreeInvalidElement
        } else if (children.size == 1) {
            children.head
        } else {
            new SetBasedStringTreeOr(children)
        }
    }

    def fromNodes(children: StringTreeNode*): StringTreeNode = SetBasedStringTreeOr.createWithSimplify(children.toSet)
}

case class SeqBasedStringTreeOr(override val _children: Seq[StringTreeNode]) extends StringTreeOr {

    override def sorted: StringTreeNode = SeqBasedStringTreeOr(children.map(_.sorted).sortBy(_.toRegex))

    override def _simplify: StringTreeNode = {
        val validChildren = _children.map(_.simplify).filterNot(_.isInvalid)
        validChildren.size match {
            case 0 => StringTreeInvalidElement
            case 1 => validChildren.head
            case _ =>
                val newChildren = validChildren.flatMap {
                    case orChild: StringTreeOr => orChild.children
                    case child                 => Set(child)
                }
                val distinctNewChildren = newChildren.distinct
                distinctNewChildren.size match {
                    case 1 => distinctNewChildren.head
                    case _ => SeqBasedStringTreeOr(distinctNewChildren)
                }
        }
    }

    def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = {
        val childrenWithChange = _children.map { c =>
            val nc = c.replaceParameters(parameters)
            (nc, c ne nc)
        }

        if (childrenWithChange.exists(_._2)) {
            SeqBasedStringTreeOr(childrenWithChange.map(_._1))
        } else {
            this
        }
    }
}

object SeqBasedStringTreeOr {

    def apply(children: Seq[StringTreeNode]): StringTreeNode = {
        if (children.isEmpty) {
            StringTreeInvalidElement
        } else if (children.size == 1) {
            children.head
        } else {
            new SeqBasedStringTreeOr(children)
        }
    }
}

case class SetBasedStringTreeOr(override val _children: Set[StringTreeNode]) extends StringTreeOr {

    override def sorted: StringTreeNode = SeqBasedStringTreeOr(children.map(_.sorted).sortBy(_.toRegex))

    override def _simplify: StringTreeNode = SetBasedStringTreeOr._simplifySelf {
        _children.map(_.simplify).filterNot(_.isInvalid)
    }

    def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = {
        val childrenWithChange = _children.map { c =>
            val nc = c.replaceParameters(parameters)
            (nc, c ne nc)
        }

        if (childrenWithChange.exists(_._2)) {
            SetBasedStringTreeOr(childrenWithChange.map(_._1))
        } else {
            this
        }
    }
}

object SetBasedStringTreeOr {

    def apply(children: Set[StringTreeNode]): StringTreeNode = {
        if (children.isEmpty) {
            StringTreeInvalidElement
        } else if (children.size == 1) {
            children.head
        } else {
            new SetBasedStringTreeOr(children)
        }
    }

    def createWithSimplify(children: Set[StringTreeNode]): StringTreeNode =
        _simplifySelf(children.filterNot(_.isInvalid))

    private def _simplifySelf(_children: Set[StringTreeNode]): StringTreeNode = {
        _children.size match {
            case 0 => StringTreeInvalidElement
            case 1 => _children.head
            case _ =>
                val newChildren = _children.flatMap {
                    case orChild: StringTreeOr => orChild.children
                    case child                 => Set(child)
                }
                newChildren.size match {
                    case 1 => newChildren.head
                    case _ => SetBasedStringTreeOr(newChildren)
                }
        }
    }
}

sealed trait SimpleStringTreeNode extends StringTreeNode {

    override final val children: Seq[StringTreeNode] = Seq.empty

    override final def sorted: StringTreeNode = this
    override final def simplify: StringTreeNode = this

    override def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = this
}

case class StringTreeConst(string: String) extends SimpleStringTreeNode {
    override def _toRegex: String = Regex.quoteReplacement(string)

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    def isIntConst: Boolean = Try(string.toInt).isSuccess

    override def isEmpty: Boolean = string == ""
}

object StringTreeEmptyConst extends StringTreeConst("") {

    override def isEmpty: Boolean = true
}

case class StringTreeParameter(index: Int) extends SimpleStringTreeNode {
    override def _toRegex: String = ".*"

    override def collectParameterIndices: Set[Int] = Set(index)

    override def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
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
    override def _toRegex: String = throw new UnsupportedOperationException()

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT

    override def isInvalid: Boolean = true
}

object StringTreeNull extends SimpleStringTreeNode {
    // Using this element nested in some other element might lead to unexpected results...
    override def _toRegex: String = "^null$"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.CONSTANT
}

object StringTreeDynamicString extends SimpleStringTreeNode {
    override def _toRegex: String = ".*"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}

object StringTreeDynamicInt extends SimpleStringTreeNode {
    override def _toRegex: String = "^-?\\d+$"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}

object StringTreeDynamicFloat extends SimpleStringTreeNode {
    override def _toRegex: String = "^-?\\d*\\.{0,1}\\d+$"

    override def constancyLevel: StringConstancyLevel.Value = StringConstancyLevel.DYNAMIC
}
