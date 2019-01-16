/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import scala.collection.immutable.IntMap
import scala.collection.mutable

import org.scalatest.FunSuite

import org.opalj.fpcf.fixtures.InitializedPropertyStore
import org.opalj.fpcf.fixtures.Marker

class EOptionPSetTest extends FunSuite {

    test("we can iterate over the singleton value stored in an EOptionPSet") {
        val e1 = InterimEUBP(new Object, Marker.IsMarked)
        val set = EOptionPSet[Entity, Property](e1)
        var count = 0
        set.foreach(e ⇒ { count += 1; assert(e == e1) })
        assert(count == 1)
    }

    test("the same EOptionP is returned unless explicitly updated") {
        val e1 = "e1"
        val e2 = "e2"
        val ieupb1 = InterimEUBP(e1, Marker.NotMarked)
        val ieupb2 = InterimEUBP(e2, Marker.NotMarked)
        val fep1 = FinalEP(e1, Marker.IsMarked)
        val fep2 = FinalEP(e2, Marker.IsMarked)
        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id → Map(
                e1 → mutable.Queue(ieupb1, fep1, fep1),
                e2 → mutable.Queue(ieupb2, fep2)
            )
        ))

        val set = EOptionPSet.empty[Entity, Property]
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ieupb1)
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == ieupb1)

        set.updateAll // updates both objects to final values..

        assert(set.isEmpty,ps.data)

        // the following query is passed through to the property store...
        assert(set.getOrQueryAndUpdate(e1, Marker.Key) == fep1)
    }

    test("we can iterate over the values stored in an EOptionPSet") {
        val e1 = new Object
        val e2 = new Object
        val ieupb1 = InterimEUBP(e1, Marker.NotMarked)
        val ieupb2 = InterimEUBP(e2, Marker.NotMarked)
        implicit val ps = new InitializedPropertyStore(IntMap(
            Marker.Key.id →
                Map(
                    e1 → mutable.Queue(ieupb1),
                    e2 → mutable.Queue(ieupb2)
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

    test("remove can be called on an empty EOptionPSet") {
        val set = EOptionPSet.empty[Entity, Property]

        assert(set.isEmpty)

        set.remove(new Object)
        set.remove(Marker.Key)
        set.remove(FinalEP(new Object, Marker.IsMarked))

        assert(set.isEmpty)
    }

}
