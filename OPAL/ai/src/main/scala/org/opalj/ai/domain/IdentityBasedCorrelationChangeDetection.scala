/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.util.IdentityHashMap

/**
 * Identifies situations (based on a '''reference comparison of the domain values''')
 * in which the memory layout changes such that a correlation between two values, which
 * existed before a join was performed, no longer exists.
 * In this case the [[UpdateType]] is lifted from [[MetaInformationUpdate]] to
 * [[StructuralUpdateType]].
 * For example, imagine that the old stack layout (before the join was executed)
 * is as follows:
 *
 *  `AnIntegerValue[#1]` <- `AnIntegerValue[#1]` <- `IntegerRange(lb=0,ub=10)[#2]` <- ...
 *
 *  and that the stack after the join is:
 *
 *  `AnIntegerValue[#2]` <- `AnIntegerValue[#3]` <- `IntegerRange(lb=0,ub=10)[#2]` <- ...
 *
 * Hence, the two top-most stack values are now different values and – if the result of an
 * analysis/domain is influenced by correlation information – the continuation of the
 * abstract interpretation is enforced.
 *
 * ==Concrete Example==
 * {{{
 * static void cfDependentValues(int i) {
 *  Object b = null;
 *  Object c = null;
 *  int j = i; // <--- j is just an alias for i
 *  while (j < 2) {
 *      Object a = maybeNull(); // returns "null" or a new instance of Object
 *      if (i == 1)
 *          b = a; // <--- b is just an alias for a
 *      else
 *          c = a; // <--- c is just an alias for a
 *      i++;
 *      j = i;
 *  }
 *  // b and c are never referring to the same object; hence a constraint related to
 *  // c does not affect b and vice versa
 *  if (c == null) { // this just constraints "c" (not "b")
 *      doIt(b); // we know nothing special about b
 *      doIt(c); // c is null
 *  } else if (b != null) {
 *      doIt(b); // b is non-null
 *      doIt(c); // we know nothing special  about c
 *  }
 * }
 * }}}
 *
 * This trait requires that updates to a value that do not influence the represented
 * value as such, but which may influence its ''correlation information'', have to create a
 * [[MetaInformationUpdate]]. Here, correlation means:
 *  - two reference values that refer to the '''same object''' are considered aliases
 *  - two local variables that are guaranteed to be identical in all cases, and, hence
 *    are subject to the same constraints are also correlated.
 *
 * @note   Mixing in this trait is strictly necessary when aliases are traced using a
 *         DomainValue's reference.
 *
 * @author Michael Eichberg
 */
trait IdentityBasedCorrelationChangeDetection extends CoreDomainFunctionality {

    /* NOT abstract override [this trait is by purpose NOT stackable] */
    protected[this] override def joinPostProcessing(
        updateType:  UpdateType,
        pc:          Int,
        oldOperands: Operands,
        oldLocals:   Locals,
        newOperands: Operands,
        newLocals:   Locals
    ): Update[(Operands, Locals)] = {

        if (updateType.isMetaInformationUpdate) {
            val aliasInformation = new IdentityHashMap[DomainValue, Integer]()

            var opi = -1;
            oldOperands.foreach { op =>
                val previousLocation = aliasInformation.get(op)
                if (previousLocation == null)
                    aliasInformation.put(op, opi)
                else {
                    // let's check if we can no-longer find the same alias
                    // relation in the new operands
                    if (newOperands(-previousLocation - 1) ne newOperands(-opi - 1))
                        return StructuralUpdate((newOperands, newLocals));
                }
                opi -= 1
            }

            var li = 0;
            oldLocals.foreach { l =>
                if ((l ne null) && (l ne TheIllegalValue)) {
                    val previousLocation = aliasInformation.get(l)
                    if (previousLocation == null)
                        aliasInformation.put(l, li)
                    else {
                        // let's check if we can find the same alias relation
                        if (previousLocation < 0) {
                            val v2 = newLocals(li)
                            if ((newOperands(-previousLocation - 1) ne v2) &&
                                (v2 ne TheIllegalValue))
                                return StructuralUpdate((newOperands, newLocals));
                        } else /*previousLocation >= 0*/ {
                            val v1 = newLocals(previousLocation)
                            val v2 = newLocals(li)
                            if ((v1 ne v2) /* <=> the alias no longer exists */ &&
                                // but, does it matter?
                                (v1 ne TheIllegalValue) && (v2 ne TheIllegalValue))
                                return StructuralUpdate((newOperands, newLocals));
                        }
                    }
                }
                li += 1
            }
        }

        updateType((newOperands, newLocals))
    }
}
