/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package graphs

import org.junit.runner.RunWith
import org.opalj.util.PerformanceEvaluation
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.ArraySeq

/**
 * Tests the SCC algorithm.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ClosedSCCTest extends AnyFlatSpec with Matchers {

    "an empty graph" should "not contain any cSCCs" in {
        val g = Graph.empty[String]
        closedSCCs(g) should be(List.empty)
    }

    "a graph with just one node" should "not contain any cSCCs" in {
        val g = Graph.empty[String] addVertice ("a")
        closedSCCs(g) should be(List.empty)
    }

    "a graph with a single path of three elements" should "not contain any cSCCs" in {
        val g = Graph.empty[String] addEdge ("a" -> "b") addEdge ("b" -> "c")
        closedSCCs(g) should be(List.empty)
    }

    "a graph with a single path of 5 elements" should "not contain any cSCCs" in {
        val g = Graph.empty[String] addEdge ("a" -> "b") addEdge ("b" -> "c") addEdge ("c" -> "d") addEdge ("d" -> "e")
        closedSCCs(g) should be(List.empty)
    }

    "a graph with a single path of 5 elements, specified in mixed order" should "not contain any cSCCs" in {
        val g = Graph.empty[String] addEdge ("d" -> "e") addEdge ("a" -> "b") addEdge ("c" -> "d") addEdge ("b" -> "c")
        closedSCCs(g) should be(List.empty)
    }

    "a graph with multiple nodes, but no edges" should "not contain any cSCCs" in {
        val g = Graph.empty[String] addVertice ("a") addVertice "b"
        closedSCCs(g) should be(List.empty)
    }

    "a graph with one node with a self dependency" should "contain one cSCC with the node" in {
        val g = Graph.empty[String] addEdge ("a", "a")
        closedSCCs(g).map(_.toList) should be(List(List("a")))
    }

    "a graph with three nodes which has two cycles and one mega-cycle" should "contain one cSCC" in {
        // models a specific failing test
        val g =
            Graph.empty[String] addEdge ("n1", "n3") addEdge ("n1", "n2") addEdge ("n2", "n1") addEdge ("n2", "n3") addEdge ("n3", "n1")
        closedSCCs(g).head.toSet should be(Set("n1", "n2", "n3"))
    }

    "a graph with four nodes with two nodes with a self dependency" should
        "contain two cSCCs with the respective nodes" in {
            val g = Graph.empty[String] addEdge ("a", "a") addVertice ("b") addEdge ("c" -> "c") addVertice ("d")
            closedSCCs(g).map(_.toList.sorted).toSet should be(Set(List("a"), List("c")))
        }

    "a graph with two nodes which form a cSCCs" should "contain the cSCCs" in {
        val g = Graph.empty[String] addEdge ("a" -> "b") addEdge ("b" -> "a")
        closedSCCs(g).map(_.toList.sorted) should be(List(List("a", "b")))
    }

    "a graph with four nodes which form a cSCCs" should "contain the cSCCs" in {
        val g = Graph.empty[String] addEdge ("a" -> "b") addEdge ("b" -> "c") addEdge ("c" -> "d") addEdge ("d" -> "a")
        closedSCCs(g).map(_.toList.sorted) should be(List(List("a", "b", "c", "d")))
    }

    "a graph with four nodes which form a cSCCs with multiple edges between pairs of nodes" should
        "contain the cSCCs" in {
            val g = Graph.empty[String] addEdge
                ("a" -> "b") addEdge ("b" -> "c") addEdge ("c" -> "d") addEdge ("d" -> "a") addEdge
                ("a" -> "b") addEdge ("b" -> "c") addEdge ("c" -> "d") addEdge ("d" -> "a")
            assert(g.successors("a").size == 2)
            closedSCCs(g).map(_.toList.sorted) should be(List(List("a", "b", "c", "d")))
        }

    "a large tree-like graph " should "not contain a cSCC" in {
        val g = Graph.empty[String] addEdge
            ("a", "b") addEdge ("a" -> "c") addEdge ("a" -> "d") addEdge
            ("c" -> "e") addEdge ("d" -> "e") addEdge ("e" -> "f")
        closedSCCs(g) should be(List.empty)
    }

    "a graph with three nodes with a cSCC and an incoming dependency" should "contain one cSCC" in {
        val g = Graph.empty[String] addEdge ("a" -> "b") addEdge ("b" -> "a") addEdge ("c" -> "a")
        g.successors.size should be(3)
        val cSCCs = closedSCCs(g).map(_.toList.sorted)
        val expected = List(List("a", "b"))
        if (cSCCs != expected) {
            fail(s"the graph $g contains one closed SCCs $expected, but found $cSCCs")
        }
    }

    "a graph with three nodes with a connected component but an outgoing dependency" should
        "not contain a cSCC" in {
            val g = Graph.empty[String] addEdge ("a" -> "b") addEdge ("b" -> "a") addEdge ("b" -> "c")
            g("a").size should be(1)
            g("b").size should be(2)
            val cSCCs = closedSCCs(g)
            if (cSCCs.nonEmpty) {
                fail(s"the graph $g contains no closed SCCs, but found $cSCCs")
            }
        }

    "a graph with five nodes with two cSCCs and an incoming dependency" should
        "contain two cSCCs" in {
            val data = List(
                ("a" -> "b"),
                ("b" -> "a"),
                ("c" -> "a"), ("c" -> "d"),
                ("d" -> "e"),
                ("e" -> "d")
            )
            var permutationCount = 0
            data.permutations.foreach { aPermutation =>
                permutationCount += 1
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(5)

                val cSCCs = closedSCCs(g).map(_.toList.sorted).toSet
                val expected = Set(List("a", "b"), List("d", "e"))
                if (cSCCs != expected) {
                    fail(
                        s"the graph $g\ncontains two closed SCCs $expected,\n but found $cSCCs\n"+
                            s"permutation $permutationCount: $aPermutation"
                    )
                }

            }
        }

    "a graph with one SCC and once cSCCs" should "contain one cSCCs" in {
        val g = Graph.empty[String] addEdge
            ("a" -> "b") addEdge ("f" -> "b") addEdge ("f" -> "a") addEdge ("b" -> "f") addEdge
            ("a" -> "e") addEdge ("e" -> "d") addEdge ("d" -> "c") addEdge ("c" -> "e")
        g.vertices.size should be(6)
        g.successors.map(_._2.size).sum should be(8)
        val cSCCs = closedSCCs(g).map(_.toList.sorted)
        val expected = List(List("c", "d", "e"))
        if (cSCCs != expected) {
            fail(s"the graph $g contains one closed SCCs $expected, but found $cSCCs")
        }
    }

    "a large totally connected graph" should "contain one cSCCs" in {
        val g = Graph.empty[String] addEdge
            ("a" -> "b") addEdge ("a" -> "c") addEdge ("a" -> "d") addEdge ("a" -> "e") addEdge
            ("b" -> "a") addEdge ("b" -> "c") addEdge ("b" -> "d") addEdge ("b" -> "e") addEdge
            ("c" -> "b") addEdge ("c" -> "a") addEdge ("c" -> "d") addEdge ("c" -> "e") addEdge
            ("d" -> "a") addEdge ("d" -> "b") addEdge ("d" -> "c") addEdge ("d" -> "e") addEdge
            ("e" -> "a") addEdge ("e" -> "b") addEdge ("e" -> "c") addEdge ("e" -> "d")
        g.vertices.size should be(5)
        g.successors.map(_._2.size).sum should be(20)
        val cSCCs = closedSCCs(g).map(_.toList.sorted)
        val expected = List(List("a", "b", "c", "d", "e"))
        if (cSCCs != expected) {
            fail(s"the graph $g contains one closed SCCs $expected, but found $cSCCs")
        }
    }

    "a large totally connected graph with self dependencies" should "contain one cSCCs" in {
        val g = Graph.empty[String] addEdge
            ("a" -> "a") addEdge ("a" -> "b") addEdge ("a" -> "c") addEdge ("a" -> "d") addEdge ("a" -> "e") addEdge
            ("b" -> "a") addEdge ("b" -> "b") addEdge ("b" -> "c") addEdge ("b" -> "d") addEdge ("b" -> "e") addEdge
            ("c" -> "b") addEdge ("c" -> "a") addEdge ("c" -> "c") addEdge ("c" -> "d") addEdge ("c" -> "e") addEdge
            ("d" -> "a") addEdge ("d" -> "b") addEdge ("d" -> "c") addEdge ("d" -> "d") addEdge ("d" -> "e") addEdge
            ("e" -> "a") addEdge ("e" -> "b") addEdge ("e" -> "c") addEdge ("e" -> "d") addEdge ("e" -> "e")
        g.vertices.size should be(5)
        g.successors.map(_._2.size).sum should be(25)
        val cSCCs = closedSCCs(g).map(_.toList.sorted)
        val expected = List(List("a", "b", "c", "d", "e"))
        if (cSCCs != expected) {
            fail(s"the graph $g contains one closed SCCs $expected, but found $cSCCs")
        }
    }

    "a complex graph with six nodes which creates one cSCC" should "contain one cSCCs" in {
        val data = List(
            ("a" -> "b"), ("f" -> "b"), ("f" -> "a"), ("b" -> "f"), ("a" -> "e"),
            ("e" -> "d"), ("d" -> "c"), ("c" -> "e"), ("c" -> "b")
        )
        var permutationCount = 0
        var testedCount = 0
        val random = new java.util.Random // testing all permutations takes too long...
        data.permutations.foreach { aPermutation =>
            permutationCount += 1
            if (random.nextInt(2500) == 1) {
                testedCount += 1
                // info(s"tested permutation: $aPermutation")
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(6)

                val cSCCs = closedSCCs(g).map(_.toList.sorted)
                val expected = List(List("a", "b", "c", "d", "e", "f"))
                if (cSCCs != expected) {
                    fail(s"the graph $g (created with permutation $permutationCount: $aPermutation) "+
                        s"contains one closed SCCs $expected, but found $cSCCs")
                }
            }
        }
        info(s"tested $testedCount permutations")
    }

    "a complex graph with several cSCCs and connected components" should "contain all cSCCs" in {
        val data = List(
            ("a" -> "b"), ("b" -> "c"), ("c" -> "a"),
            ("g" -> "f"),
            ("b" -> "d"),
            ("a" -> "h"), ("h" -> "j"), ("j" -> "i"), ("i" -> "j"), ("i" -> "k"), ("k" -> "h")
        )
        var permutationCount = 0
        var testedCount = 0
        val random = new java.util.Random // testing all permutations takes FAR too long...
        data.permutations.foreach { aPermutation =>
            permutationCount += 1
            if (random.nextInt(250000) == 1) {
                val data = List(
                    ("f" -> "c"), ("f" -> "g"), ("d" -> "e"), ("e" -> "d"), ("l" -> "m"), ("m" -> "l")
                ) ::: aPermutation
                testedCount += 1
                val g = data.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(13)

                val cSCCs = closedSCCs(g).map(_.toList.sorted).toSet
                val expected = Set(List("l", "m"), List("h", "i", "j", "k"), List("d", "e"))
                if (cSCCs != expected) {
                    fail(s"the graph $g (created with permutation $permutationCount: $data) "+
                        s"contains three closed SCCs $expected, but found $cSCCs")
                }
            }
        }
        info(s"tested $testedCount permutations")
    }

    "a graph with four nodes with a path which has a connection to one cSCCs" should
        "contain one cSCCs" in {
            val data = List(("a" -> "b"), ("b" -> "c"), ("c" -> "a"), ("b" -> "d"), ("d" -> "d"))
            data.permutations.foreach { aPermutation =>
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(4)
                val cSCCs = closedSCCs(g)
                val expected = List(ArraySeq("d"))
                if (cSCCs != expected) {
                    fail(s"the graph $g contains one closed SCCs $expected, but found $cSCCs")
                }
            }
        }

    "a graph with two connected sccs which are connected to one cSCC (requires the correct setting of the non-cSCC node)" should "contain one cSCCs" in {
        val g = Graph.empty[String] addEdge
            ("a", "b") addEdge ("b", "c") addEdge ("c", "d") addEdge ("d", "e") addEdge ("c", "g") addEdge ("g", "a") addEdge
            ("e", "d") addEdge ("g", "h") addEdge ("h", "j") addEdge ("j", "g")

        val cSCCs = closedSCCs(g).map(_.toList.sorted)
        val expected = List(List("d", "e"))
        if (cSCCs != expected) {
            fail(s"$g: cscc with $expected expected, but found $cSCCs")
        }
    }

    "a graph with two cSCCs which are connected by one SCC" should "contain two cSCCs" in {
        val g = Graph.empty[String] addEdge ("a", "b") addEdge ("b", "c") addEdge
            ("c", "d") addEdge ("d", "e") addEdge ("c", "g") addEdge ("g", "a") addEdge ("e", "d") addEdge
            ("g", "h") addEdge ("h", "j") addEdge ("j", "g") addEdge ("b", "x") addEdge ("y", "x") addEdge
            ("x", "z") addEdge ("z", "y")
        val cSCCs = closedSCCs(g).map(_.toList.sorted).toSet
        val expected = Set(List("d", "e"), List("x", "y", "z"))
        if (cSCCs != expected) {
            fail(s"$g: cscc with $expected expected, but found $cSCCs")
        }
    }

    "a graph with one cSCC which has multiple incoming edges" should "contain one cSCCs" in {
        val g = Graph.empty[String] addEdge
            ("u", "v") addEdge ("v", "a") addEdge ("w", "e") addEdge ("x", "c") addEdge ("x", "b") addEdge ("u", "d") addEdge
            ("a", "b") addEdge ("b", "c") addEdge ("c", "a") addEdge
            ("a", "e") addEdge ("e", "d") addEdge ("d", "c")
        val cSCCs = closedSCCs(g).map(_.toList.sorted)
        val expected = List(List("a", "b", "c", "d", "e"))
        if (cSCCs != expected) {
            fail(s"$g: cscc with $expected expected, but found $cSCCs")
        }
    }

    "a graph with three cSCC and a SCC which has multiple incoming edges" should
        "contain three cSCCs" in {
            val g = Graph.empty[String] addEdge
                ("u", "a") addEdge ("v", "c") addEdge ("w", "c") addEdge ("w", "e") addEdge ("w", "g") addEdge ("x", "g") addEdge
                ("h", "z") addEdge ("y", "b") addEdge ("y", "d") addEdge ("y", "f") addEdge ("y", "h") addEdge
                ("a", "b") addEdge ("b", "a") addEdge
                ("c", "d") addEdge ("d", "c") addEdge
                ("e", "f") addEdge ("f", "e") addEdge
                ("g", "h") addEdge ("h", "g")
            val cSCCs = closedSCCs(g).map(_.toList.sorted).toSet
            val expected = Set(List("a", "b"), List("c", "d"), List("e", "f"))
            if (cSCCs != expected) {
                fail(s"$g: cscc with $expected expected, but found $cSCCs")
            }
        }

    "a multi-graph with cSCCs and a SCCs" should "contain the correct number of cSCCs" in {
        val seed = System.currentTimeMillis
        val random = new java.util.Random(seed) // testing all permutations takes FAR too long...

        val edges = Array(
            ("c" -> "b"), /*REDUNDANT:*/ "c" -> "b",
            ("d" -> "e"),
            ("e" -> "e"),
            ("f" -> "g"),
            ("g" -> "f"), /*REDUNDANT:*/ "c" -> "b", /*REDUNDANT:*/ "d" -> "e",
            ("h" -> "i"),
            ("j" -> "k"),
            ("k" -> "l"),
            ("l" -> "k"), ("l" -> "n"), /*REDUNDANT:*/ "c" -> "b", /*REDUNDANT:*/ "d" -> "e",
            ("m" -> "l"),
            ("o" -> "b"), ("o" -> "e"), ("o" -> "g"), /*REDUNDANT:*/ "o" -> "e",
            ("p" -> "r"), ("p" -> "q"),
            ("q" -> "p"),
            ("r" -> "q"), ("r" -> "s"),
            ("s" -> "t"), ("s" -> "q"), /*REDUNDANT:*/ "o" -> "e",
            ("t" -> "p"),
            ("u" -> "p"), ("u" -> "v"), ("u" -> "w"),
            ("v" -> "w"),
            ("w" -> "v"),
            ("x" -> "w"), /*REDUNDANT:*/ "e" -> "e",
            ("y" -> "t"), /*REDUNDANT:*/ "p" -> "q"
        )
        val expectedCSCCs = Set(
            List("e"),
            List("v", "w"),
            List("f", "g"),
            List("p", "q", "r", "s", "t")
        )

        var run = 0

        do {
            val g = {
                val g = Graph.empty[String] addVertice ("a") addVertice ("b") addVertice ("i") addVertice ("n") addVertice ("z")
                // permutate the edges
                var swaps = 10
                while (swaps > 0) {
                    val cellA = random.nextInt(edges.length)
                    val cellB = random.nextInt(edges.length)
                    val temp = edges(cellA)
                    edges(cellA) = edges(cellB)
                    edges(cellB) = temp
                    swaps -= 1
                }
                // add the edges
                edges.foreach(g.addEdge)
                g
            }
            val cSCCs = closedSCCs(g).map(_.toList.sorted).toSet
            if (cSCCs != expectedCSCCs) {
                fail(s"$g: cscc with $expectedCSCCs expected, but found $cSCCs (run=$run)")
            }
            run += 1
        } while (System.currentTimeMillis - seed < 1000)
        info(s"tested $run permutations of the graph (initial seed: $seed)")
    }

    "a graph with a long path leading to a simple cSCC consisting of two nodes" should
        "contain one cSCC" in {
            val data = List(("a" -> "b"), ("b" -> "c"), ("c" -> "d"), ("d" -> "c"), ("d" -> "d"))
            data.permutations.foreach { aPermutation =>
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(4)

                val cSCCs = closedSCCs(g).map(_.toSet).toSet
                cSCCs.size should be(1)
                val expected = Set("c", "d")
                if (cSCCs.head != expected) {
                    fail(s"the graph $g contains one closed SCC $expected, but found $cSCCs")
                }
            }
        }

    "a graph with multiple paths leading to a simple cSCC" should
        "contain one cSCC" in {
            val data = List(("a" -> "c"), ("b" -> "c"), ("c" -> "c"))
            data.permutations.foreach { aPermutation =>
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(3)

                val cSCCs = closedSCCs(g).map(_.toSet).toSet
                cSCCs.size should be(1)
                val expected = Set("c")
                if (cSCCs.head != expected) {
                    fail(s"the graph $g contains one closed SCC $expected, but found $cSCCs")
                }
            }
        }

    "a graph with a long path leading to two simple cSCCs" should
        "contain two cSCCs" in {
            val data = List(("a" -> "b"), ("b" -> "c"), ("b" -> "d"), ("c" -> "c"), ("d" -> "d"))
            data.permutations.foreach { aPermutation =>
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(4)

                val cSCCs = closedSCCs(g).map(_.toSet).toSet
                cSCCs.size should be(2)
                val expected = Set(Set("d"), Set("c"))
                if (cSCCs != expected) {
                    fail(s"the graph $g contains two closed SCCs $expected, but found $cSCCs")
                }
            }
        }

    "a graph with one node with multiple outgoing edges leading to a complex cSCCs" should
        "contain one cSCC" in {
            val data = List(("a" -> "b"), ("a" -> "c"), ("a" -> "d"), ("b" -> "c"), ("c" -> "d"), ("d" -> "b"))
            data.permutations.foreach { aPermutation =>
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                g.vertices.size should be(4)

                val cSCCs = closedSCCs(g).map(_.toSet).toSet
                cSCCs.size should be(1)
                val expected = Set("b", "c", "d")
                if (cSCCs.head != expected) {
                    fail(s"the graph $g contains one closed SCCs $expected, but found $cSCCs")
                }
            }
        }

    "a graph with two SCCs and two cSCCs" should "contain two cSCCs" in {
        val data = List(
            ("a" -> "c"), ("b" -> "c"), ("c" -> "d"), ("d" -> "e"), ("c" -> "f"), ("f" -> "g"),
            ("d" -> "d"), ("e" -> "e"), ("f" -> "f"), ("g" -> "g")
        )
        var permutationCounter = 1
        PerformanceEvaluation.time {
            data.permutations.take(20000).foreach { aPermutation =>
                val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                val cSCCs = closedSCCs(g).map(_.toSet).toSet
                val expected = Set(Set("e"), Set("g"))
                if (cSCCs != expected) {
                    fail(s"the graph $g\n"+
                        s"created using permutation: $aPermutation\n"+
                        s"contains two closed SCCs : $expected\n"+
                        s"found: $cSCCs")
                } else {
                    // info(s"successfully tested permutation $permutationCounter: $aPermutation")
                    permutationCounter += 1
                }
            }
        }(t => info(s"analyzing ${permutationCounter - 1} permutations took: ${t.toSeconds}"))
    }

    "a graph with two cSCCs where one is a chain " should "contain two cSCCs" in {
        List(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).permutations.take(50).foreach { lis =>
            val a = "n"+lis(0)
            val b = "n"+lis(1)
            val d = "n"+lis(2)
            val e = "n"+lis(3)
            val f = "n"+lis(4)
            val g = "n"+lis(5)
            val h = "n"+lis(6)
            val i = "n"+lis(7)
            val j = "n"+lis(8)
            val r = "n"+lis(9)
            val data = List(
                a -> f, f -> h, f -> j, f -> i,
                h -> j, j -> h, i -> j, j -> i,
                a -> g, g -> h,
                a -> h,
                a -> b,
                b -> d, d -> d, d -> e, e -> r, r -> b
            )
            var permutationCounter = 1
            PerformanceEvaluation.time {
                data.permutations.take(50).foreach { aPermutation =>
                    val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                    val cSCCs = closedSCCs(g).map(_.toSet).toSet
                    val expected = Set(Set(h, j, i), Set(b, d, e, r))
                    if (cSCCs != expected) {
                        fail(s"the graph $g\n"+
                            s"created using permutation: $aPermutation\n"+
                            s"contains two closed SCCs : $expected\n"+
                            s"found: $cSCCs")
                    } else {
                        // info(s"successfully tested permutation $permutationCounter: $aPermutation")
                        permutationCounter += 1
                    }
                }
            }(t => info(s"analyzing ${permutationCounter - 1} permutations took: ${t.toSeconds}"))
        }
    }

    "a graph with multiple cycles embedded in a larger cycle " should "contain one cSCC" in {
        List(1, 2, 3).permutations.foreach { lis =>
            val a = "n"+lis(0)
            val b = "n"+lis(1)
            val c = "n"+lis(2)
            val data = List(a -> b, a -> c, b -> a, b -> c, c -> a)
            var permutationCounter = 1
            PerformanceEvaluation.time {
                data.permutations.foreach { aPermutation =>
                    val g = aPermutation.foldLeft(Graph.empty[String])(_ addEdge _)
                    val rawCSCCs = closedSCCs(g)
                    val cSCCs = rawCSCCs.map(_.toSet).toSet
                    val expected = Set(Set(a, b, c))
                    if (cSCCs != expected) {
                        fail(s"the graph (with the nodes $lis) $g\n"+
                            s"created using permutation: $aPermutation\n"+
                            s"contains one closed SCC : $expected\n"+
                            s"found: $cSCCs")
                    } else {
                        info(s"successfully tested permutation $permutationCounter: $aPermutation")
                        permutationCounter += 1
                    }
                }
            }(t => info(s"analyzing ${permutationCounter - 1} permutations took: ${t.toSeconds}"))
        }
    }
}
