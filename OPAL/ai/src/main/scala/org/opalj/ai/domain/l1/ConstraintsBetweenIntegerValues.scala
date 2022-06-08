/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.util.{IdentityHashMap => IDMap}

import scala.jdk.CollectionConverters._

import org.opalj.constraints.NumericConstraints
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.instructions.Instruction
import org.opalj.br.LiveVariables

/**
 * Domain that traces the relationship between integer values; currently, the domain only
 * works in an unbelievable small number of cases... it is basically useless at the moment.
 *
 * @author Michael Eichberg
 */
trait ConstraintsBetweenIntegerValues
    extends CoreDomainFunctionality
    with IntegerRangeValues // IMRPOVE Define a common trait that specifies that the values support aliasing analyses
    with TheCodeStructure {
    domain: CorrelationalDomainSupport with Configuration with ExceptionsFactory =>

    type Constraint = NumericConstraints.Value

    type ConstraintsStore = IDMap[IntegerLikeValue, IDMap[IntegerLikeValue, Constraint]]

    //
    //
    // INITIALIZATION (TIME)
    //
    //

    // We store the constraints that are in effect for each instruction
    private[this] var constraints: Array[ConstraintsStore] = null

    abstract override def setCodeStructure(
        theInstructions: Array[Instruction],
        theCFJoins:      IntTrieSet,
        liveVariables:   LiveVariables
    ): Unit = {
        super.setCodeStructure(theInstructions, theCFJoins, liveVariables)

        constraints = new Array[ConstraintsStore](theInstructions.length)
    }

    private[this] var lastConstraint: Option[(Int /*PC*/ , IntegerLikeValue, IntegerLikeValue, Constraint)] = {
        None
    }

    //
    //
    // IMPLEMENTATION
    //
    //

    def putConstraintInStore(
        store: ConstraintsStore,
        v1:    IntegerLikeValue, v2: IntegerLikeValue, c: Constraint
    ): ConstraintsStore = {

        assert(v1 ne v2)

        var m = store.get(v1)
        if (m == null) {
            m = new IDMap()
            store.put(v1, m)
            m.put(v2, c)
        } else {
            val old_c = m.get(v2)
            if (old_c == null)
                m.put(v2, c)
            else
                m.put(v2, NumericConstraints.combine(old_c, c))
        }
        store
    }

    def establishConstraint(
        pc: Int,
        v1: IntegerLikeValue, v2: IntegerLikeValue, c: Constraint
    ): ConstraintsStore = {

        val store = {
            val store = this.constraints(pc)
            if (store == null) {
                val store = new ConstraintsStore()
                this.constraints(pc) = store
                store
            } else {
                store
            }
        }
        putConstraintInStore(store, v1, v2, c)
    }

    private[this] def addConstraint(
        pc: Int,
        v1: IntegerLikeValue,
        v2: IntegerLikeValue,
        c:  Constraint
    ): Unit = {
        // let's collect the constraint(s)
        this.lastConstraint = Some((pc, v1, v2, c))
    }

    private[this] def addConstraint(
        pc: Int,
        v1: DomainValue,
        v2: DomainValue,
        c:  Constraint
    ): Unit = {
        addConstraint(
            pc,
            v1.asInstanceOf[IntegerLikeValue], v2.asInstanceOf[IntegerLikeValue], c
        )
    }

    private[this] def getConstraint(
        pc: Int,
        v1: IntegerLikeValue,
        v2: IntegerLikeValue
    ): Option[Constraint] = {
        val constraints = this.constraints(pc)
        if (constraints == null)
            return None

        val m = constraints.get(v1)
        if (m == null)
            None
        else {
            Option(m.get(v2))
        }
    }

    private[this] def getConstraint(
        pc: Int,
        v1: DomainValue,
        v2: DomainValue
    ): Option[Constraint] = {
        getConstraint(
            pc,
            v1.asInstanceOf[IntegerLikeValue], v2.asInstanceOf[IntegerLikeValue]
        )
    }

    def cloneConstraintsStore(store: ConstraintsStore): ConstraintsStore = {
        val newStore = new ConstraintsStore()
        val it = store.entrySet().iterator
        while (it.hasNext) {
            val e = it.next()
            newStore.put(e.getKey(), e.getValue().clone().asInstanceOf[IDMap[IntegerLikeValue, Constraint]])
        }
        newStore
    }

    abstract override def flow(
        currentPC:                        Int,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      Int,
        isSuccessorScheduled:             Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[Int /*PC*/ ],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[Int /*PC*/ ] = {

        def clone(store: ConstraintsStore): ConstraintsStore = {
            def stillExists(value: IntegerLikeValue): Boolean = {
                operandsArray(successorPC).exists(_ eq value) ||
                    localsArray(successorPC).exists(_ eq value)
            }

            val newStore = new ConstraintsStore()
            val it = store.entrySet().iterator
            while (it.hasNext) {
                val e = it.next()
                if (stillExists(e.getKey)) {
                    val inner_newStore = new IDMap[IntegerLikeValue, Constraint]()
                    val inner_it = e.getValue().entrySet().iterator
                    while (inner_it.hasNext) {
                        val inner_e = inner_it.next()
                        if (stillExists(inner_e.getKey)) {
                            inner_newStore.put(inner_e.getKey, inner_e.getValue)
                        }
                    }
                    if (!inner_newStore.isEmpty()) {
                        newStore.put(e.getKey, inner_newStore)
                    }
                }
            }
            if (newStore.isEmpty())
                null
            else
                newStore
        }

        val constraints = this.constraints

        if (!wasJoinPerformed) {
            if (constraints(currentPC) != null)
                constraints(successorPC) = clone(constraints(currentPC))
            val lastConstraintOption = this.lastConstraint
            if (lastConstraintOption.isDefined) {
                val (_, v1, v2, c) = lastConstraintOption.get
                val constraintsStore = establishConstraint(successorPC, v1, v2, c)
                putConstraintInStore(constraintsStore, v2, v1, NumericConstraints.inverse(c))
            }
        } else {
            // We only keep constraints for values where we have constraints on
            // both paths (including a newly established constraint)

            // IMPROVE The join of inter-integer-value constraints
            constraints(successorPC) = null
        }

        this.lastConstraint = None

        super.flow(
            currentPC, currentOperands, currentLocals,
            successorPC, isSuccessorScheduled,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer
        )

    }

    private[this] val updatedValues = new IDMap[DomainValue, DomainValue]

    abstract override def updateMemoryLayout(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        oldValue match {
            case _: IntegerLikeValue => updatedValues.put(oldValue, newValue)
            case _                   => /*nothing special to do*/
        }

        super.updateMemoryLayout(oldValue, newValue, operands, locals)
    }

    private[this] def currentValue(value: DomainValue): DomainValue = {
        val updatedValue = this.updatedValues.get(value)
        if (updatedValue != null)
            updatedValue
        else
            value
    }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO INTEGER VALUES
    //
    // -----------------------------------------------------------------------------------

    abstract override def intAreEqual(
        pc:     Int,
        value1: DomainValue, value2: DomainValue
    ): Answer = {
        super.intAreEqual(pc, value1, value2) match {
            case Unknown =>
                val constraint = getConstraint(pc, value1, value2)
                if (constraint.isDefined)
                    constraint.get match {
                        case NumericConstraints.!= => No
                        case NumericConstraints.>  => No
                        case NumericConstraints.<  => No
                        case NumericConstraints.== => Yes
                        case _                     => Unknown
                    }
                else
                    Unknown
            case answer =>
                answer
        }
    }

    override def intIsLessThan(
        pc:    Int,
        left:  DomainValue,
        right: DomainValue
    ): Answer = {
        super.intIsLessThan(pc, left, right) match {
            case Unknown =>
                val constraint = getConstraint(pc, left, right)
                if (constraint.isDefined)
                    constraint.get match {
                        case NumericConstraints.>  => No
                        case NumericConstraints.>= => No
                        case NumericConstraints.<  => Yes
                        case NumericConstraints.== => No
                        case _                     => Unknown
                    }
                else
                    Unknown
            case answer =>
                answer
        }
    }

    override def intIsLessThanOrEqualTo(pc: Int, left: DomainValue, right: DomainValue): Answer = {
        super.intIsLessThanOrEqualTo(pc, left, right) match {
            case Unknown =>
                val constraint = getConstraint(pc, left, right)
                if (constraint.isDefined)
                    constraint.get match {
                        case NumericConstraints.>  => No
                        case NumericConstraints.<  => Yes
                        case NumericConstraints.<= => Yes
                        case NumericConstraints.== => Yes
                        case _                     => Unknown
                    }
                else
                    Unknown
            case answer =>
                answer
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    abstract override def intEstablishValue(
        pc:       Int,
        theValue: Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val result = super.intEstablishValue(pc, theValue, value, operands, locals)

        // we do not need to add a constraint

        updatedValues.clear
        result
    }

    abstract override def intEstablishAreEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val result = super.intEstablishAreEqual(pc, value1, value2, operands, locals)

        // we do not need to add a constraint; this situation is handled by the domain

        updatedValues.clear
        result
    }

    override def intEstablishAreNotEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val result = super.intEstablishAreNotEqual(pc, value1, value2, operands, locals)

        addConstraint(pc, currentValue(value1), currentValue(value2), NumericConstraints.!=)

        updatedValues.clear
        result
    }

    override def intEstablishIsLessThan(
        pc:       Int,
        left:     DomainValue,
        right:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val result = super.intEstablishIsLessThan(pc, left, right, operands, locals)

        addConstraint(pc, currentValue(left), currentValue(right), NumericConstraints.<)

        updatedValues.clear
        result
    }

    override def intEstablishIsLessThanOrEqualTo(
        pc:       Int,
        left:     DomainValue,
        right:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = {

        val result = super.intEstablishIsLessThanOrEqualTo(pc, left, right, operands, locals)

        addConstraint(pc, currentValue(left), currentValue(right), NumericConstraints.<=)

        updatedValues.clear
        result
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //    override def iadd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
    //        (value1, value2) match {
    //            case (IntegerRange(lb1, ub1), IntegerRange(lb2, ub2)) =>
    //                // to identify overflows we simply do the "add" on long values
    //                // and check afterwards
    //                val lb = lb1.toLong + lb2.toLong
    //                val ub = ub1.toLong + ub2.toLong
    //                if (lb < Int.MinValue || ub > Int.MaxValue)
    //                    IntegerValue(pc)
    //                else
    //                    IntegerRange(lb.toInt, ub.toInt)
    //            case _ =>
    //                // we have to create a new instance... even if we just add "0"
    //                IntegerValue(pc)
    //        }
    //    }

    //
    //    override def iinc(pc: Int, value: DomainValue, increment: Int): DomainValue = {
    //        value match {
    //            case IntegerRange(lb, ub) =>
    //                val newLB = lb.toLong + increment.toLong
    //                val newUB = ub.toLong + increment.toLong
    //                if (newLB < Int.MinValue || newUB > Int.MaxValue)
    //                    IntegerValue(pc)
    //                else
    //                    IntegerRange(newLB.toInt, newUB.toInt)
    //            case _ =>
    //                IntegerValue(pc)
    //        }
    //    }
    //
    //    override def isub(pc: Int, left: DomainValue, right: DomainValue): DomainValue = {
    //        if (left eq right)
    //            return IntegerRange(0, 0)
    //
    //        (left, right) match {
    //            case (IntegerRange(llb, lub), IntegerRange(rlb, rub)) =>
    //                // to identify overflows we simply do the "add" on long values
    //                // and check afterwards
    //                val lb = llb.toLong - rub.toLong
    //                val ub = lub.toLong - rlb.toLong
    //                if (lb < Int.MinValue || ub > Int.MaxValue)
    //                    IntegerValue(pc)
    //                else
    //                    IntegerRange(lb.toInt, ub.toInt)
    //            case _ =>
    //                // we have to create a new instance... even if we just subtract "0"
    //                IntegerValue(pc)
    //        }
    //    }
    //
    //    override def idiv(
    //        pc: Int,
    //        numerator: DomainValue,
    //        denominator: DomainValue): IntegerValueOrArithmeticException = {
    //        denominator match {
    //            case IntegerRange(1, 1) =>
    //                ComputedValue(numerator.asInstanceOf[IntegerLikeValue].newInstance)
    //            case IntegerRange(lb, ub) if lb > 0 || ub < 0 =>
    //                // no div by "0"
    //                ComputedValue(IntegerValue(pc))
    //            case _ =>
    //                if (throwArithmeticExceptions)
    //                    ComputedValueOrException(
    //                        IntegerValue(pc), ArithmeticException(pc))
    //                else
    //                    ComputedValue(IntegerValue(pc))
    //        }
    //    }
    //
    //    override def imul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
    //        value1 match {
    //            case IntegerRange(lb1, ub1) =>
    //                if (lb1 == 0 && ub1 == 0) IntegerRange(0, 0)
    //                else value2 match {
    //                    case IntegerRange(lb2, ub2) =>
    //                        // to identify overflows we simply do the "mul" on long values
    //                        // and check afterwards
    //                        val lb1l = lb1.toLong
    //                        val ub1l = ub1.toLong
    //                        val lb2l = lb2.toLong
    //                        val ub2l = ub2.toLong
    //                        val ub =
    //                            Math.max(lb1l * lb2l, ub1l * ub2l)
    //                        val lb =
    //                            Math.min(
    //                                Math.min(
    //                                    Math.min(lb1l * lb2l, ub1l * ub2l),
    //                                    ub1l * lb2l),
    //                                lb1l * ub2l)
    //
    //                        if (lb < Int.MinValue || ub > Int.MaxValue)
    //                            IntegerValue(pc)
    //                        else
    //                            IntegerRange(lb.toInt, ub.toInt)
    //                    case _ =>
    //                        IntegerValue(pc)
    //                }
    //
    //            case _ =>
    //                value2 match {
    //                    case IntegerRange(0, 0) => IntegerRange(0, 0)
    //                    case _ =>
    //                        IntegerValue(pc)
    //                }
    //        }
    //    }
    //
    //    override def irem(
    //        pc: Int,
    //        left: DomainValue,
    //        right: DomainValue): IntegerValueOrArithmeticException = {
    //        right match {
    //            case IntegerRange(rightLB, rightUB) if rightLB > 0 || rightUB < 0 =>
    //                // no div by "0"
    //                ComputedValue(IntegerValue(pc))
    //            case _ =>
    //                if (throwArithmeticExceptions)
    //                    ComputedValueOrException(
    //                        IntegerValue(pc), ArithmeticException(pc))
    //                else
    //                    ComputedValue(IntegerValue(pc))
    //        }
    //    }
    //
    //    override def iand(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ior(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ishl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ishr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def iushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ixor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //

    // -----------------------------------------------------------------------------------
    //
    // "DEBUGGING"
    //
    // -----------------------------------------------------------------------------------

    protected[this] def constraintsToText(
        pc:            Int,
        valueToString: AnyRef => String
    ): String = {
        if (constraints(pc) == null)
            return "No constraints found."

        val cs = (constraints(pc).asScala.map { e =>
            val (v1, v2c) = e
            val jv2c = v2c.asScala
            for ((v2, c) <- jv2c)
                yield s"${valueToString(v1)} $c ${valueToString(v2)}"
        }).flatten
        cs.mkString("Constraints:\n\t", "\n\t", "")
    }

    abstract override def properties(
        pc:            Int,
        valueToString: AnyRef => String
    ): Option[String] = {
        val superProperties = super.properties(pc)
        if (constraints(pc) != null) {
            val otherProperties = superProperties.map(_+"\n").getOrElse("")
            Some(otherProperties + constraintsToText(pc, valueToString))
        } else {
            superProperties
        }
    }
}
