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

    val children: Iterable[StringTreeNode]

    /**
     * The depth of the string tree measured by the count of nodes on the longest path from the root to a leaf.
     */
    lazy val depth: Int = children.maxByOption(_.depth).map(_.depth).getOrElse(0) + 1

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
        else if (targetDepth == 1)
            replacement
        else
            replaceInChildren(targetDepth - 1, replacement)
    }

    protected def replaceInChildren(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        createNew(children.map(_.replaceAtDepth(targetDepth, replacement)))
    }

    /**
     * @return The string tree sorted with a stable ordering over its canonical reduction.
     */
    def sorted: StringTreeNode

    /**
     * @return The canonical reduction of the string tree, i.e. a regex representing the same set of string values as
     *         the tree itself.
     */
    final lazy val regex: String = toRegex
    protected def toRegex: String

    /**
     * Simplifies the string tree by e.g. flattening nested [[StringTreeOr]] instances.
     *
     * @return The simplified string tree or the same instance if nothing could be simplified.
     *
     * @see [[CachedSimplifyNode]]
     */
    def simplified: StringTreeNode

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
        assert(parameters.nonEmpty)
        if (parameterIndices.isEmpty || parameters.keySet.intersect(parameterIndices).isEmpty)
            this
        else
            replaceParametersInChildren(parameters)
    }

    protected def replaceParametersInChildren(parameters: Map[Int, StringTreeNode]): StringTreeNode = {
        var hasChanges = false
        val newChildren = children.map { c =>
            val nc = c.replaceParameters(parameters)
            hasChanges |= c ne nc
            nc
        }

        if (hasChanges) createNew(newChildren)
        else this
    }

    /**
     * @return True if this string tree node represents an empty string, false otherwise.
     */
    def isEmpty: Boolean = false

    /**
     * @return True if this string tree node represents no string, false otherwise.
     */
    def isInvalid: Boolean = false

    /**
     * @return A new StringTreeNode (of the same type if applicable) with the given children
     */
    protected def createNew(children: Iterable[StringTreeNode]): StringTreeNode
}

object StringTreeNode {
    def lb: StringTreeNode = StringTreeDynamicString
    def ub: StringTreeNode = StringTreeInvalidElement
}

sealed trait CachedSimplifyNode extends StringTreeNode {

    private var isSimplified = false

    final def simplified: StringTreeNode = {
        if (isSimplified) {
            this
        } else {
            simplify match {
                case cr: CachedSimplifyNode =>
                    cr.isSimplified = true
                    cr
                case r => r
            }
        }
    }

    protected def simplify: StringTreeNode
}

sealed trait CachedHashCode extends Product {
    override lazy val hashCode: Int = scala.util.hashing.MurmurHash3.caseClassHash(this)
    override def canEqual(obj: Any): Boolean = obj.hashCode() == hashCode
}

/**
 * Represents the concatenation of all its children.
 */
case class StringTreeConcat(override val children: Seq[StringTreeNode]) extends CachedSimplifyNode with CachedHashCode {

    override protected def toRegex: String = children.tail.foldLeft(children.head.regex) { (regex, child) =>
        s"$regex${child.regex}"
    }

    override def sorted: StringTreeNode = StringTreeConcat(children.map(_.sorted))

    override protected def simplify: StringTreeNode = {
        var hasInvalidChildren = false
        val newChildren = children.flatMap { c =>
            c.simplified match {
                case nc if nc.isInvalid =>
                    hasInvalidChildren |= true
                    None
                case nc if nc.isEmpty              => None
                case concatChild: StringTreeConcat => concatChild.children
                case nc                            => Some(nc)
            }
        }

        if (hasInvalidChildren) StringTreeInvalidElement
        else newChildren.size match {
            case 0 => StringTreeEmptyConst
            case 1 => newChildren.head
            case _ => StringTreeConcat(newChildren)
        }
    }

    override lazy val constancyLevel: StringConstancyLevel =
        children.foldLeft(children.head.constancyLevel) {
            (level, child) => StringConstancyLevel.determineForConcat(level, child.constancyLevel)
        }

    override protected def createNew(children: Iterable[StringTreeNode]): StringTreeNode =
        StringTreeConcat(children.toSeq)
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

    override def sorted: StringTreeNode = SeqBasedStringTreeOr(children.iterator.map(_.sorted).toSeq.sortBy(_.regex))

    override protected def toRegex: String = {
        children.size match {
            case 0 => throw new IllegalStateException("Tried to convert StringTreeOr with no children to a regex!")
            case 1 => children.head.regex
            case _ => s"(${children.tail.foldLeft(children.head.regex) { (regex, child) => s"$regex|${child.regex}" }})"
        }
    }

    override lazy val constancyLevel: StringConstancyLevel =
        children.foldLeft[StringConstancyLevel](StringConstancyLevel.Invalid) { (level, child) =>
            StringConstancyLevel.meet(level, child.constancyLevel)
        }
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
private case class SeqBasedStringTreeOr(override val children: Seq[StringTreeNode]) extends StringTreeOr {

    override protected def simplify: StringTreeNode = {
        val newChildren = children.flatMap { c =>
            c.simplified match {
                case nc if nc.isInvalid => None
                case or: StringTreeOr   => or.children
                case nc                 => Some(nc)
            }
        }.distinct

        SeqBasedStringTreeOr(newChildren)
    }

    override protected def createNew(children: Iterable[StringTreeNode]): StringTreeNode =
        SeqBasedStringTreeOr(children.toSeq)
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
case class SetBasedStringTreeOr(override val children: Set[StringTreeNode]) extends StringTreeOr {

    override protected def simplify: StringTreeNode = SetBasedStringTreeOr.createWithSimplify(children.map(_.simplified))

    override protected def createNew(children: Iterable[StringTreeNode]): StringTreeNode =
        SetBasedStringTreeOr(children.toSet)
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

    def createWithSimplify(children: Set[StringTreeNode]): StringTreeNode = {
        children.size match {
            case 0 => StringTreeInvalidElement
            case 1 => children.head
            case _ =>
                val newChildrenBuilder = Set.newBuilder[StringTreeNode]
                children.foreach {
                    case orChild: StringTreeOr    => newChildrenBuilder.addAll(orChild.children)
                    case child if child.isInvalid =>
                    case child                    => newChildrenBuilder.addOne(child)
                }
                SetBasedStringTreeOr(newChildrenBuilder.result())
        }
    }
}

/**
 * Represents a string tree leaf, i.e. a node having no children and can thus return itself during sorting and
 * simplification.
 */
sealed trait SimpleStringTreeNode extends StringTreeNode {

    override final val children: Iterable[StringTreeNode] = Iterable.empty

    override final lazy val depth = 1

    override final def sorted: StringTreeNode = this
    override final def simplified: StringTreeNode = this

    override protected def replaceParametersInChildren(parameters: Map[Int, StringTreeNode]): StringTreeNode = this
    override protected def replaceInChildren(targetDepth: Int, replacement: StringTreeNode): StringTreeNode = {
        throw new UnsupportedOperationException()
    }

    override protected def createNew(children: Iterable[StringTreeNode]): StringTreeNode =
        throw new UnsupportedOperationException()
}

case class StringTreeConst(string: String) extends SimpleStringTreeNode {
    override protected def toRegex: String = Regex.quoteReplacement(string).replaceAll("\\[", "\\\\[")

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
    override protected def toRegex: String = ".*"

    override lazy val parameterIndices: Set[Int] = Set(index)

    override protected def replaceParametersInChildren(parameters: Map[Int, StringTreeNode]): StringTreeNode =
        parameters.getOrElse(index, this)

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}

object StringTreeParameter {
    def forParameterPC(paramPC: Int): StringTreeParameter = {
        if (paramPC >= -1) {
            throw new IllegalArgumentException(s"Invalid parameter pc given: $paramPC")
        }
        // Parameters start at PC -2 downwards
        StringTreeParameter(-2 - paramPC)
    }
}

object StringTreeInvalidElement extends SimpleStringTreeNode {
    override protected def toRegex: String = throw new UnsupportedOperationException()

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Invalid

    override def isInvalid: Boolean = true
}

object StringTreeNull extends SimpleStringTreeNode {
    // IMPROVE Using this element nested in some other element might lead to unexpected results since it contains regex
    // matching characters for the beginning and end of a string.
    override protected def toRegex: String = "null"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Constant
}

object StringTreeDynamicString extends SimpleStringTreeNode {
    override protected def toRegex: String = ".*"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}

object StringTreeDynamicInt extends SimpleStringTreeNode {
    // IMPROVE Using this element nested in some other element might lead to unexpected results since it contains regex
    // matching characters for the beginning and end of a string.
    override protected def toRegex: String = "-?\\d+"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}

object StringTreeDynamicFloat extends SimpleStringTreeNode {
    // IMPROVE Using this element nested in some other element might lead to unexpected results since it contains regex
    // matching characters for the beginning and end of a string.
    override protected def toRegex: String = "-?\\d*\\.{0,1}\\d+"

    override def constancyLevel: StringConstancyLevel = StringConstancyLevel.Dynamic
}
