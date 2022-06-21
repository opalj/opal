/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import scala.collection.immutable.IntMap
import scala.collection.mutable

import org.scalatest.funsuite.AnyFunSuite

import org.opalj.fpcf.fixtures.InitializedPropertyStore
import org.opalj.fpcf.fixtures.Marker
import org.opalj.fpcf.fixtures.Palindromes

class EOptionPSetTest extends AnyFunSuite {

    test("always has a definitive size") {
        val e1 = InterimEUBP(new Object, Marker.IsMarked)

        val set = EOptionPSet[Entity, Property](e1)
        assert(set.hasDefiniteSize)
    }

    test("we can iterate over the singleton value stored in an EOptionPSet") {
        val e1 = InterimEUBP(new Object, Marker.IsMarked)
        val set = EOptionPSet[Entity, Property](e1)
        var count = 0
        set.foreach(e => { count += 1; assert(e == e1) })
        assert(count == 1)
    }

    test("the same EOptionP is returned unless explicitly updated (using updateAll)") {
        val e1 = "e1"
        val e2 = "e2"
        val ie1Marker = InterimEUBP(e1, Marker.NotMarked)
        val ie2Marker = InterimEUBP(e2, Marker.NotMarked)
        val ie2Pal = InterimEUBP(e2, Palindromes.NoPalindrome)

        val fep1Marker = FinalEP(e1, Marker.IsMarked)
        val fep2Marker = FinalEP(e2, Marker.IsMarked)
        val fe2Pal = FinalEP(e2, Palindromes.Palindrome)

        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id -> Map(
                e1 -> mutable.Queue(ie1Marker, fep1Marker, fep1Marker),
                e2 -> mutable.Queue(ie2Marker, fep2Marker)
            ),
            Palindromes.PalindromeKey.id -> Map(
                e2 -> mutable.Queue(ie2Pal, fe2Pal)
            )
        ))

        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker) // the same value is returned...
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == ie2Pal) // the same value is returned...

        set.updateAll() // updates both objects to final values..

        assert(set.isEmpty, ps.data) // all updates result in final values - hence, they are deleted

        // the following query is passed through to the property store...
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == fep1Marker)
    }

    test("the same EOptionP is returned unless explicitly updated (using update)") {
        val e1 = "e1"
        val e2 = "e2"

        val ie1Marker = InterimEUBP(e1, Marker.NotMarked)
        val ie2Marker = InterimEUBP(e2, Marker.NotMarked)
        val ie2Pal = InterimEUBP(e2, Palindromes.NoPalindrome)

        val fep1Marker = FinalEP(e1, Marker.IsMarked)
        val fe2Pal = FinalEP(e2, Palindromes.Palindrome)

        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id -> Map(
                e1 -> mutable.Queue(ie1Marker, fep1Marker),
                e2 -> mutable.Queue(ie2Marker)
            ),
            Palindromes.PalindromeKey.id -> Map(
                e2 -> mutable.Queue(ie2Pal, fe2Pal)
            )
        ))

        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
        assert(set.getOrQueryAndUpdate(e2, Marker.Key) == ie2Marker)
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == ie2Pal)

        set.update(fep1Marker)
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == fep1Marker)
        assert(set.size == 2, ps.data) // we "implicitly removed e1/Marker"

        set.update(fe2Pal)
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == fe2Pal)
        assert(set.size == 1, ps.data) // we "implicitly removed e1/PalindromesKey"
    }

    test("we can iterate (using foreach) over the values stored in an EOptionPSet") {
        val e1 = new Object
        val e2 = new Object
        val ieupb1 = InterimEUBP(e1, Marker.NotMarked)
        val ieupb2 = InterimEUBP(e2, Marker.NotMarked)
        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id ->
                Map(
                    e1 -> mutable.Queue(ieupb1),
                    e2 -> mutable.Queue(ieupb2)
                )
        ))

        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ieupb1)
        assert(set.getOrQueryAndUpdate(e2, Marker.Key) == ieupb2)
        assert(set.size == 2)
        var iteratedOverValues = List.empty[SomeEOptionP]
        set.foreach(iteratedOverValues ::= _)
        assert(iteratedOverValues.contains(ieupb1))
        assert(iteratedOverValues.contains(ieupb2))
        assert(iteratedOverValues.size == 2)
    }

    test("creating an EOptionPSet with a final value results in an empty set") {
        val set = EOptionPSet[Entity, Property](FinalEP(new Object, Marker.IsMarked))
        assert(set.isEmpty)
    }
    /*
    test("remove can be called on an empty EOptionPSet") {
        val set = EOptionPSet.empty[Entity, Property]

        assert(set.isEmpty)

        set.remove(new Object)
        set.remove(Marker.Key)
        set.remove(FinalEP(new Object, Marker.IsMarked))

        assert(set.isEmpty)
    }

    test("remove of an entity works across PropertyKeys") {
        val e1 = "e1"
        val e2 = "e2"
        val ie1Marker = InterimEUBP(e1, Marker.NotMarked)
        val ie2Marker = InterimEUBP(e2, Marker.NotMarked)
        val ie2Pal = InterimEUBP(e2, Palindromes.NoPalindrome)

        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id -> Map(
                e1 -> mutable.Queue(ie1Marker),
                e2 -> mutable.Queue(ie2Marker)
            ),
            Palindromes.PalindromeKey.id -> Map(
                e2 -> mutable.Queue(ie2Pal)
            )
        ))

        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
        assert(set.getOrQueryAndUpdate(e2, Marker.Key) == ie2Marker)
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == ie2Pal)
        assert(set.size == 3, ps.data)

        set.remove(e2)
        assert(set.size == 1, ps.data)

        // the following query is passed through to the property store...
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
    }

    test("remove of a property kind removes all values") {
        val e1 = "e1"
        val e2 = "e2"
        val ie1Marker = InterimEUBP(e1, Marker.NotMarked)
        val ie2Marker = InterimEUBP(e2, Marker.NotMarked)
        val ie2Pal = InterimEUBP(e2, Palindromes.NoPalindrome)

        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id -> Map(
                e1 -> mutable.Queue(ie1Marker),
                e2 -> mutable.Queue(ie2Marker)
            ),
            Palindromes.PalindromeKey.id -> Map(
                e2 -> mutable.Queue(ie2Pal)
            )
        ))

        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
        assert(set.getOrQueryAndUpdate(e2, Marker.Key) == ie2Marker)
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == ie2Pal)
        assert(set.size == 3, ps.data)

        set.remove(Marker.Key)
        assert(set.size == 1, ps.data)

        // the following query is passed through to the property store...
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == ie2Pal)
    }
    */

    test("filters filters the respective value") {
        val e1 = "e1"
        val e2 = "e2"
        val ie1Marker = InterimEUBP(e1, Marker.NotMarked)
        val ie2Marker = InterimEUBP(e2, Marker.NotMarked)
        val ie2Pal = InterimEUBP(e2, Palindromes.NoPalindrome)

        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id -> Map(
                e1 -> mutable.Queue(ie1Marker),
                e2 -> mutable.Queue(ie2Marker)
            ),
            Palindromes.PalindromeKey.id -> Map(
                e2 -> mutable.Queue(ie2Pal)
            )
        ))
        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
        assert(set.getOrQueryAndUpdate(e2, Marker.Key) == ie2Marker)
        assert(set.getOrQueryAndUpdate(e2, Palindromes.PalindromeKey) == ie2Pal)
        assert(set.size == 3, ps.data)

        val newSet = set.filter(_.e == e1)
        assert(newSet.size == 1)
        assert(!newSet.isEmpty)
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ie1Marker)
        assert(newSet.size == 1)

        val newSet2 = newSet.filter(_.e == e2)
        assert(newSet2.size == 0)
        assert(newSet2.isEmpty)

        // old sets are unchanged...:
        assert(set.size == 3)
        assert(newSet.size == 1)
    }
}
