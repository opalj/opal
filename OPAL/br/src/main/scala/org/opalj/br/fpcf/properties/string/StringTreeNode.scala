/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package string

import scala.util.Try
import scala.util.matching.Regex

/**
 * A single node that can be nested to create string trees that represent a set of possible string values. Its canonical
 * reduction is a regex of all possible strings.
 *
 * @note This trait and all its implementations should be kept immutable to allow certain values to be cached.
 * @see [[CachedHashCode]] [[CachedSimplifyNode]]
 *
 * @author Maximilian Rüsch
 */
sealed trait StringTreeNode {

    val children: Seq[StringTreeNode]

    /**
     * The depth of the string tree measured by the count of nodes on the longest path from the root to a leaf.
     */
    lazy val depth: Int = children.map(_.depth).maxOption.getOrElse(0) + 1

    /**
     * Replaces string tree nodes at the target depth if they have children. In case a [[SimpleStringTreeNode]] is given
     * as a second parameter, this effectively limits the string tree to the given target depth.
     *
     * @param targetDepth The depth at which nodes should be replaced if they have children.
     * @param replacement The replacement to set for nodes at the target depth if they have children.
     * @return The modified tree if the target depth is smaller than the current depth or the same instance if it is not.
     */
    final def replaceAtDepth(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        if (targetDepth >= depth)
            this
        else
            _replaceAtDepth(targetDepth, replacement)
    }
    protected def _replaceAtDepth(targetDepth: Int, replacement: StringTreeNode): StringTreeNode

    /**
     * @return The string tree sorted with a stable ordering over its canonical reduction.
     */
    def sorted: StringTreeNode

    private var _regex: Option[String] = None

    /**
     * @return The canonical reduction of the string tree, i.e. a regex representing the same set of string values as
     *         the tree itself.
     */
    final def toRegex: String = {
        if (_regex.isEmpty) {
            _regex = Some(_toRegex)
        }

        _regex.get
    }
    protected def _toRegex: String

    /**
     * Simplifies the string tree by e.g. flattening nested [[StringTreeOr]] instances.
     *
     * @return The simplified string tree or the same instance if nothing could be simplified.
     *
     * @see [[CachedSimplifyNode]]
     */
    def simplify: StringTreeNode

    /**
     * @return The constancy level of the string tree.
     *
     * @see [[StringConstancyLevel]]
     */
    def constancyLevel: StringConstancyLevel

    /**
     * The indices of any method parameter references using [[StringTreeParameter]] within the string tree.
     */
    lazy val parameterIndices: Set[Int] = children.flatMap(_.parameterIndices).toSet

    /**
     * Replaces all [[StringTreeParameter]] instances in the string tree that represent a parameter index defined in the
     * given map with the replacement value for that index. Keeps [[StringTreeParameter]] instances whose their index is
     * not defined in the map.
     *
     * @param parameters A map from parameter indices to replacement values
     * @return The modified string tree if something could be replaced or the same instance otherwise.
     */
    final def replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = {
        if (parameters.isEmpty ||
            parameterIndices.isEmpty ||
            parameters.keySet.intersect(parameterIndices).isEmpty
        )
            this
        else
            _replaceParameters(parameters)
    }
    protected def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode

    /**
     * @return True if this string tree node represents an empty string, false otherwise.
     */
    def isEmpty: Boolean = false

    /**
     * @return True if this string tree node represents no string, false otherwise.
     */
    def isInvalid: Boolean = false
}

object StringTreeNode {

    def lb: StringTreeNode = StringTreeDynamicString
    def ub: StringTreeNode = StringTreeInvalidElement
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

    private lazy val _hashCode = scala.util.hashing.MurmurHash3.productHash(this)
    override def hashCode(): Int = _hashCode
    override def canEqual(obj: Any): Boolean = obj.hashCode() == _hashCode
}

/**
 * Represents the concatenation of all its children.
 */
case class StringTreeConcat(override val children: Seq[StringTreeNode]) extends CachedSimplifyNode with CachedHashCode {

    override protected def _toRegex: String = {
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

    override def constancyLevel: StringConstancyLevel =
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

    def _replaceAtDepth(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        if (targetDepth == 1)
            replacement
        else
            StringTreeConcat(children.map(_.replaceAtDepth(targetDepth - 1, replacement)))
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

/**
 * Represents the free choice between all its children.
 */
trait StringTreeOr extends CachedSimplifyNode with CachedHashCode {

    protected val _children: Iterable[StringTreeNode]

    override final lazy val children: Seq[StringTreeNode] = _children.toSeq

    override protected def _toRegex: String = {
        children.size match {
            case 0 => throw new IllegalStateException("Tried to convert StringTreeOr with no children to a regex!")
            case 1 => children.head.toRegex
            case _ => s"(${children.map(_.toRegex).reduceLeft((o, n) => s"$o|$n")})"
        }
    }

    override def constancyLevel: StringConstancyLevel =
        _children.map(_.constancyLevel).reduceLeft(StringConstancyLevel.meet)

    override lazy val parameterIndices: Set[Int] = _children.flatMap(_.parameterIndices).toSet
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

/**
 * @inheritdoc
 *
 * Based on a [[Seq]] for children storage. To be used if the order of children is important for e.g. reduction to a
 * regex and subsequent comparison to another string tree.
 */
private case class SeqBasedStringTreeOr(override val _children: Seq[StringTreeNode]) extends StringTreeOr {

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

    def _replaceAtDepth(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        if (targetDepth == 1)
            replacement
        else
            SeqBasedStringTreeOr(children.map(_.replaceAtDepth(targetDepth - 1, replacement)))
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

/**
 * @inheritdoc
 *
 * Based on a [[Set]] for children storage. To be used if the order of children is NOT important.
 */
case class SetBasedStringTreeOr(override val _children: Set[StringTreeNode]) extends StringTreeOr {

    override lazy val depth: Int = _children.map(_.depth).maxOption.getOrElse(0) + 1

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

    def _replaceAtDepth(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        if (targetDepth == 1)
            replacement
        else
            SetBasedStringTreeOr(_children.map(_.replaceAtDepth(targetDepth - 1, replacement)))
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
                val newChildrenBuilder = Set.newBuilder[StringTreeNode]
                _children.foreach {
                    case setOrChild: SetBasedStringTreeOr => newChildrenBuilder.addAll(setOrChild._children)
                    case orChild: StringTreeOr            => newChildrenBuilder.addAll(orChild.children)
                    case child                            => newChildrenBuilder.addOne(child)
                }
                val newChildren = newChildrenBuilder.result()
                newChildren.size match {
                    case 1 => newChildren.head
                    case _ => SetBasedStringTreeOr(newChildren)
                }
        }
    }
}

/**
 * Represents a string tree leaf, i.e. a node having no children and can thus return itself during sorting and
 * simplification.
 */
sealed trait SimpleStringTreeNode extends StringTreeNode {

    override final val children: Seq[StringTreeNode] = Seq.empty

    override final def sorted: StringTreeNode = this
    override final def simplify: StringTreeNode = this

    override def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode = this
    override def _replaceAtDepth(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        if (targetDepth == 1)
            replacement
        else
            replaceAtDepth(targetDepth - 1, replacement)
    }
}

case class StringTreeConst(string: String) extends SimpleStringTreeNode {
    override protected def _toRegex: String = Regex.quoteReplacement(string).replaceAll("\\[", "\\\\[")

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Constant

    def isIntConst: Boolean = Try(string.toInt).isSuccess

    override def isEmpty: Boolean = string == ""
}

object StringTreeEmptyConst extends StringTreeConst("") {

    override def isEmpty: Boolean = true
}

/**
 * A placeholder for a method parameter value. Should be replaced using [[replaceParameters]] before reducing the string
 * tree to a regex.
 *
 * @param index The method parameter index that is being represented.
 */
case class StringTreeParameter(index: Int) extends SimpleStringTreeNode {
    override protected def _toRegex: String = ".*"

    override lazy val parameterIndices: Set[Int] = Set(index)

    override def _replaceParameters(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        parameters.getOrElse(index, this)

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
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
    override protected def _toRegex: String = throw new UnsupportedOperationException()

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Invalid

    override def isInvalid: Boolean = true
}

object StringTreeNull extends SimpleStringTreeNode {
    // IMPROVE Using this element nested in some other element might lead to unexpected results since it contains regex
    // matching characters for the beginning and end of a string.
    override protected def _toRegex: String = "^null$"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Constant
}

object StringTreeDynamicString extends SimpleStringTreeNode {
    override protected def _toRegex: String = ".*"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}

object StringTreeDynamicInt extends SimpleStringTreeNode {
    // IMPROVE Using this element nested in some other element might lead to unexpected results since it contains regex
    // matching characters for the beginning and end of a string.
    override protected def _toRegex: String = "^-?\\d+$"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}

object StringTreeDynamicFloat extends SimpleStringTreeNode {
    // IMPROVE Using this element nested in some other element might lead to unexpected results since it contains regex
    // matching characters for the beginning and end of a string.
    override protected def _toRegex: String = "^-?\\d*\\.{0,1}\\d+$"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}
