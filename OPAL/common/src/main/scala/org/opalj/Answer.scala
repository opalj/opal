/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

/**
 * Models a three state answer ([[Yes]], [[No]], [[Unknown]]).
 *
 * @author Michael Eichberg
 */
sealed trait Answer {

    /**
     * Returns `true` if and only if this answer is `Yes`.
     *
     * Effectively the same as a comparison with [[Yes]].
     */
    def isYes: Boolean

    /**
     * Returns `true` if and only if this answer is `No`.
     *
     * Effectively the same as a comparison with [[No]].
     */
    def isNo: Boolean

    /**
     * Returns `true` if and only if this answer is `Unknown`.
     *
     * Effectively the same as a comparison with [[Unknown]].
     */
    def isUnknown: Boolean

    /**
     * Returns `true` if this answer is `Yes` or `Unknown`, `false` otherwise.
     */
    def isNotNo: Boolean

    /**
     * Returns `true` if this answer is `Yes` or `Unknown`, `false` otherwise.
     */
    final def isYesOrUnknown: Boolean = isNotNo

    /**
     * Returns `true` if this answer is `No` or `Unknown`, `false` otherwise.
     */
    def isNotYes: Boolean

    /**
     * Returns `true` if this answer is `No` or `Unknown`, `false` otherwise.
     */
    final def isNoOrUnknown: Boolean = isNotYes

    /**
     * Returns `true` if this answer is either `Yes` or `No`; false if this answer
     * is `Unknown`.
     */
    def isYesOrNo: Boolean

    /**
     * If this answer is unknown the given function is evaluated and that
     * result is returned, otherwise `this` answer is returned.
     */
    def ifUnknown(f: => Answer): Answer

    /**
     * The negation of this `Answer`. If the answer is `Unknown` the negation is still `Unknown`.
     */
    def negate: Answer

    /**
     * @see [[Answer#negate]]
     */
    final def unary_! : Answer = this.negate

    /**
     * The logical conjunction of this answer and the given answer.
     * In this case Unknown is considered to either represent the
     * answer Yes or No; hence, `this && other` is treated as
     * `this && (Yes || No)`.
     */
    def &&(other: Answer): Answer

    final def &&(other: Boolean): Answer = this && Answer(other)

    /**
     * The logical disjunction of this answer and the given answer.
     * In this case Unknown is considered to either represent the
     * answer Yes or No; hence, `this || other` is treated as
     * `this || (Yes || No)`.
     */
    def ||(other: Answer): Answer

    final def ||(other: Boolean): Answer = this || Answer(other)

    /**
     * Joins this answer and the given answer. In this case `Unknown` will
     * represent the case that we have both answers; that is we have
     * a set based view w.r.t. `Answer`s. Hence,
     * `this join Unknown` is considered as `this join {Yes, No}` where
     * the set `{Yes, No}` is represented by `Unknown`.
     *
     * If the other `Answer` is identical to `this` answer `this` is returned,
     * otherwise `Unknown` is returned.
     *
     */
    def join(other: Answer): Answer
}

/**
 * Factory for `Answer`s.
 *
 * @author Michael Eichberg
 */
object Answer {

    /**
     * Returns [[org.opalj.Yes]] if `value` is `true` and [[org.opalj.No]] otherwise.
     */
    def apply(value: Boolean): Answer = if (value) Yes else No

    /**
     * Returns [[org.opalj.Yes]] if `result` is `defined` and [[org.opalj.No]] otherwise.
     */
    def apply(result: Option[_]): Answer = if (result.isDefined) Yes else No
}

/**
 * Represents the answer to a question where the answer is `Yes`.
 *
 * @author Michael Eichberg
 */
case object Yes extends Answer {
    override def isYes: Boolean = true
    override def isNo: Boolean = false
    override def isUnknown: Boolean = false

    override def isNotNo: Boolean = true
    override def isNotYes: Boolean = false
    override def isYesOrNo: Boolean = true

    override def ifUnknown(f: => Answer): Answer = this

    override def negate: No.type = No
    override def &&(other: Answer): Answer = {
        other match {
            case Yes => this
            case No  => No
            case _   => Unknown
        }
    }
    override def ||(other: Answer): Yes.type = this

    override def join(other: Answer): Answer = if (other eq this) this else Unknown
}

/**
 * Represents the answer to a question where the answer is `No`.
 *
 * @author Michael Eichberg
 */
case object No extends Answer {
    override def isYes: Boolean = false
    override def isNo: Boolean = true
    override def isUnknown: Boolean = false

    override def isNotNo: Boolean = false
    override def isNotYes: Boolean = true
    override def isYesOrNo: Boolean = true

    override def ifUnknown(f: => Answer): Answer = this

    override def negate: Yes.type = Yes
    override def &&(other: Answer): No.type = this
    override def ||(other: Answer): Answer = {
        other match {
            case Yes => Yes
            case No  => this
            case _   => Unknown
        }
    }
    override def join(other: Answer): Answer = if (other eq this) this else Unknown
}

/**
 * Represents the answer to a question where the answer is either `Unknown`
 * or is actually both; that is, `Yes` and `No`.
 *
 * @author Michael Eichberg
 */
case object Unknown extends Answer {
    override def isYes: Boolean = false
    override def isNo: Boolean = false
    override def isUnknown: Boolean = true

    override def isNotNo: Boolean = true
    override def isNotYes: Boolean = true
    override def isYesOrNo: Boolean = false

    override def ifUnknown(f: => Answer): Answer = f

    override def negate: Unknown.type = this

    override def &&(other: Answer): Answer = if (other eq No) No else this
    override def ||(other: Answer): Answer = if (other eq Yes) Yes else this

    override def join(other: Answer): Unknown.type = this
}
