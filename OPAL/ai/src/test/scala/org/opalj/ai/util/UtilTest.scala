/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Tests the utility methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UtilTest extends AnyFlatSpec with Matchers {

    behavior of "the function removeFirstWhile"

    val shortList = List(1, 5, 4)
    val longList = List(1, 4, 5, 6, 4848, 34, 35, 35, 37)

    it should ("return the given list if no element matches") in {
        val newList = removeFirstUnless(longList, -1)(_ <= 0)
        newList should be(longList)
        System.identityHashCode(newList) should be(System.identityHashCode(longList))
    }

    it should ("return the given list if the matching element is not considered") in {
        val newList = removeFirstUnless(longList, 34)(_ >= 1000)
        newList should be(longList)
        System.identityHashCode(newList) should be(System.identityHashCode(longList))
    }

    it should ("return the list even if the head matches but it does not pass the filter") in {
        val newList = removeFirstUnless(longList, 1)(_ >= -1)
        newList should be(longList)
    }

    it should ("return the list without it's head if the head matches") in {
        val newList = removeFirstUnless(longList, 1)(_ >= 1000)
        newList should be(longList.tail)
    }

    it should ("return the list without it's second element if that element matches") in {
        val newList = removeFirstUnless(longList, 4)(_ >= 1000)
        newList should be(longList.head :: longList.tail.tail)
    }

    it should ("return the list without it's last element if that element matches") in {
        val newList = removeFirstUnless(shortList, 4)(_ >= 1000)
        newList should be(List(1, 5))
    }
}
