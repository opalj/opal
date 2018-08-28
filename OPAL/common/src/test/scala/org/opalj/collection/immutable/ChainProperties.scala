/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.{forAll, classify, BooleanOperators}
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `Chain` by creating standard Scala Lists and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object ChainProperties extends Properties("Chain") {

    // FIRST LET'S CHECK THAT WE CAN ACTUALLY CREATE SPECIALIZED CLASSES
    val nilClass = Naught.getClass
    val baseClass = new :&:[String]("1", Naught).getClass
    val specializedClass = new :&:[Int](1, Naught).getClass
    if (baseClass == specializedClass) {
        throw new UnknownError("unable to create a specialized instance of a chained list of ints")
    }

    def isNotSpecialized(cl: Chain[_]): Boolean = {
        val clClass = cl.getClass
        clClass == baseClass || clClass == nilClass
    }

    def isSpecialized(cl: Chain[_]): Boolean = {
        val clClass = cl.getClass
        clClass == specializedClass || clClass == nilClass
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             G E N E R A T O R S

    /**
     * Generates a list and an int value in the range [0,length of list ].
     */
    val listAndIndexGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[String])
        i ← Gen.choose(0, n)
    } yield (m, i)

    val listOfListGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[List[String]])
    } yield m

    val smallListsGen = for {
        m ← Gen.listOfN(5, Arbitrary.arbitrary[String])
    } yield (m)

    /**
     * Generates a list and an int value in the range [0,length of list +2 ].
     */
    val listAndIntGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[String])
        i ← Gen.choose(0, n + 2)
    } yield (m, i)

    val listsOfSingleCharStringsGen = for {
        n ← Gen.choose(0, 3)
        m ← Gen.choose(0, 3)
        l1 ← Gen.listOfN(n, Gen.oneOf("a", "b", "c"))
        l2 ← Gen.listOfN(m, Gen.oneOf("a", "b", "c"))
    } yield (l1, l2)

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create[AnyRef]") = forAll { s: String ⇒
        val fl1 = Chain(s)
        val fl2 = (Chain.newBuilder[String] += s).result
        val fl3 = s :&: Naught
        fl1.head == s && fl2.head == s && fl3.head == s
    }

    property("create[Int]") = forAll { i: Int ⇒
        val fl1 = Chain(i)
        val fl2 = new :&:[Int](i, Naught)
        val fl3 = (Chain.newBuilder[Int] += i).result
        val fl4 = Chain(List[Int](i): _*)
        val fl5 = i :&: Chain(List[Int](): _*)
        val fl6 = i :&: Chain.singleton(i)

        fl1.head == i && isSpecialized(fl1) &&
            fl2.head == i && isSpecialized(fl2) &&
            fl3.head == i && isSpecialized(fl3) &&
            fl4.head == i && isSpecialized(fl4) &&
            fl5.head == i && isSpecialized(fl5) &&
            fl6.head == i && isSpecialized(fl6)
    }

    property("==|hashCode") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        (l1 == l2) == (fl1 == fl2) && (fl1 != fl2 || fl1.hashCode() == fl2.hashCode())
    }

    property("==|hashCode[Int vs. AnyRef](I.e., a specialized list of ints should be equal to the same list containing boxed values)") = forAll { (l1: List[Int], l2: List[Int]) ⇒
        val fl1 = Chain(l1: _*)
        val l1AsAny = l1: List[Any]
        val fl1alt = Chain(l1AsAny: _*)
        val l2AsAny: List[Any] = l2
        val fl2 = Chain(l2AsAny: _*)

        isSpecialized(fl1) :| "specialization of fl1" &&
            isNotSpecialized(fl2) :| "specialization of fl2" &&
            (fl1 == fl1alt) :| "content equality" &&
            ((l1 == l2) == (fl1 == fl2)) :| "content inequality" &&
            (fl1 != fl2 || fl1.hashCode() == fl2.hashCode()) :| "hashCode (in)equality"
    }

    // METHODS DEFINED BY CHAINED LIST

    property("hasMultipleElements") = forAll { l1: List[String] ⇒
        val fl1 = Chain(l1: _*)
        (l1.size >= 2 && fl1.hasMultipleElements) || (l1.size <= 1 && !fl1.hasMultipleElements)
    }

    property("startsWith") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        classify(l1.startsWith(l2) || l2.startsWith(l1), "a list starts with the other list") {
            classify(l2.length == l1.length, "both lists have the same size") {
                l1.startsWith(l2) == fl1.startsWith(fl2) && l2.startsWith(l1) == fl2.startsWith(fl1)
            }
        }
    }

    property("sharedPrefix") = forAll(listsOfSingleCharStringsGen) { ls ⇒
        val (l1: List[String], l2: List[String]) = ls
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        val sharedPrefixList = org.opalj.collection.commonPrefix(l1, l2)
        classify(sharedPrefixList.nonEmpty, "non-empty shared prefix list") {
            val sharedPrefixChain = fl1.sharedPrefix(fl2)
            classify(sharedPrefixChain eq fl1, "the result is chain 1") {
                classify(sharedPrefixChain eq fl2, "the result is chain 2") {
                    Chain(sharedPrefixList: _*) == sharedPrefixChain && {
                        ((sharedPrefixList eq l1) && (sharedPrefixChain eq fl1)) ||
                            ((sharedPrefixList eq l2) && (sharedPrefixChain eq fl2)) ||
                            (
                                (sharedPrefixList ne l1) &&
                                (sharedPrefixChain ne fl1) &&
                                (sharedPrefixList ne l2) &&
                                (sharedPrefixChain ne fl2)
                            )
                    }
                }
            }
        }
    }

    property("WithFilter") = forAll { orig: List[String] ⇒
        def test(s: String): Boolean = s.length > 0
        val cl = Chain(orig: _*).withFilter(test).map[String, Chain[String]](s ⇒ s)
        val l = orig.withFilter(test).map[String, List[String]](s ⇒ s)
        cl == Chain(l: _*)
    }

    property("hasDefiniteSize") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.hasDefiniteSize
    }

    property("isTraversableAgain") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.isTraversableAgain
    }

    property("seq") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        (fl.seq eq fl)
    }

    property("flatMap") = forAll(listOfListGen) { l: List[List[String]] ⇒
        val fl = Chain(l: _*)
        classify(l.isEmpty, "outer list is empty") {
            classify(l.nonEmpty && l.forall(_.isEmpty), "all (at least one) inner lists are empty") {
                def t(ss: List[String]): List[Int] = ss.map(_.length)
                fl.flatMap(t) == Chain(l.flatMap(t): _*)
            }
        }
    }

    property("map") = forAll { l: List[String] ⇒
        def f(s: String): Int = s.length()
        val fl = Chain(l: _*)
        fl.map(f) == Chain(l.map(f): _*)
    }

    property("chained lists can be used in for loop") = forAll { orig: List[String] ⇒
        // In the following we, have an implicit foreach call
        var newL = List.empty[String]
        for {
            e ← Chain(orig: _*)
            if e != null
        } { newL ::= e }
        newL == orig.reverse
    }

    property("chained lists can be used in for comprehensions") = {
        forAll(listOfListGen) { orig: List[List[String]] ⇒
            // In the following we, have an implicit withFilter, map and flatMap call!
            val cl = for {
                es ← Chain(orig: _*)
                if es.length > 1
                if es.length < 10
                e ← es
                r = e.capitalize
            } yield r+":"+r.length

            val l = for {
                es ← orig
                if es.length > 1
                if es.length < 10
                e ← es
                r = e.capitalize
            } yield r+":"+r.length

            cl == Chain(l: _*)
        }
    }

    property("a for-comprehension constructs specialized lists when possible") = {
        forAll { orig: List[List[Int]] ⇒
            // In the following we, have an implicit withFilter, map and flatMap call!
            val cl = for { es ← Chain(orig: _*) } yield Chain(es: _*)
            cl.forall(icl ⇒ isSpecialized(icl))
        }
    }

    property("hasDefiniteSize") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.hasDefiniteSize
    }

    property("isTraversableAgain") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.isTraversableAgain
    }

    property("seq") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        (fl.seq eq fl)
    }

    property("head") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        (l.nonEmpty && l.head == fl.head) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.head; false } catch { case _: Throwable ⇒ true } }
    }

    property("headOption") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.headOption == l.headOption
    }

    property("tail") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        (l.nonEmpty && Chain(l.tail: _*) == fl.tail) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.tail; false } catch { case _: Throwable ⇒ true } }
    }

    property("last") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        (l.nonEmpty && l.last == fl.last) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.last; false } catch { case _: Throwable ⇒ true } }
    }

    property("(is|non)Empty") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        l.isEmpty == fl.isEmpty && l.nonEmpty == fl.nonEmpty
    }

    property("apply") = forAll(listAndIndexGen) { (listAndIndex: (List[String], Int)) ⇒
        val (l, index) = listAndIndex
        classify(index == 0, "takes first") {
            classify(index == l.length - 1, "takes last") {
                val fl = Chain(l: _*)
                (index < l.length && fl(index) == l(index)) ||
                    // if the index is not valid an exception
                    { try { fl(index); false } catch { case _: Throwable ⇒ true } }
            }
        }
    }

    property("exists") = forAll { (l: List[String], c: Int) ⇒
        val fl = Chain(l: _*)
        def test(s: String): Boolean = s.length == c
        l.exists(test) == fl.exists(test)
    }

    property("forall") = forAll { (l: List[String], c: Int) ⇒
        val fl = Chain(l: _*)
        def test(s: String): Boolean = s.length <= c
        l.forall(test) == fl.forall(test)
    }

    property("contains") = forAll { (l: List[String], s: String) ⇒
        val fl = Chain(l: _*)
        l.contains(s) == fl.contains(s) &&
            l.forall(fl.contains(_))
    }

    property("find") = forAll { (l: List[Int], i: Int) ⇒
        val fl = Chain(l: _*)
        l.find(_ == i) == fl.find(_ == i)
    }

    property("size") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        l.size == fl.size
    }

    property("isSingletonList") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        (l.size == 1) == (fl.isSingletonList)
    }

    property(":&:") = forAll { (l: List[String], es: List[String]) ⇒
        var fle = Chain(l: _*)
        var le = l
        es.forall { e ⇒
            fle :&:= e
            le ::= e
            fle.head == le.head
        }
    }

    property(":&::") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        val fl3 = fl2 :&:: fl1
        val l3 = l2 ::: l1
        fl3.toList == l3
    }

    property("++[Chain[String]]]") = forAll { (l1: List[String], l2: List[String]) ⇒
        l1.nonEmpty ==> {
            val fl1 = Chain(l1: _*)
            val fl2 = Chain(l2: _*)
            val fl3 = fl1 ++ fl2
            val l3 = l1 ++ l2
            fl3.toList == l3
        }
    }

    property("++[List[String]]") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = Chain(l1: _*)
        val fl3 = fl1 ++ l2
        val l3 = l1 ++ l2
        fl3.toList == l3
    }

    property("++[Chain[Int]]]") = forAll { (l1: List[Int], l2: List[Int]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        val fl3 = fl1 ++ fl2
        val l3 = l1 ++ l2
        isSpecialized(fl1) :| "specialization of fl1" &&
            isSpecialized(fl2) :| "specialization of fl2" &&
            //        isSpecialized(fl3) :| "specialization of fl3" &&
            fl3.toList == l3
    }

    property("++[List[Int]]]") = forAll { (l1: List[Int], l2: List[Int]) ⇒
        val fl1 = Chain(l1: _*)
        val jfl = fl1 ++ l2
        val l3 = l1 ++ l2
        isSpecialized(fl1) :| "specialization of fl1" &&
            //    isSpecialized(jfl) :| "specialization of jfl" &&
            jfl.toList == l3
    }

    property("copy") = forAll { (l1: List[Int]) ⇒
        val fl1 = Chain(l1: _*)
        val (fl1Copy, last) = fl1.copy()
        isSpecialized(fl1) :| "specialization of fl1" &&
            (fl1.isEmpty && fl1Copy.isEmpty && last == null) ||
            ((fl1 ne fl1Copy) && last != null && last.rest == Naught)
    }

    property("++!:[Chain[Int]]]") = forAll { (l1: List[Int], l2: List[Int]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        val fl3 = fl1 ++!: fl2
        val l3 = l1 ++: l2
        (fl3.toList == l3) :| "list equality" &&
            isSpecialized(fl3) :| "specialization" &&
            (fl1.isEmpty || fl2.isEmpty || (fl3 eq fl1)) :| "reference equality"
    }

    property("++![Chain[Int]]]") = forAll { (l1: List[Int], l2: List[Int]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        val fl3 = fl1 ++! fl2
        val l3 = l1 ++ l2
        fl3.toList == l3 && isSpecialized(fl3) && (fl1.isEmpty || (fl3 eq fl1))
    }

    property("foreach") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        var lRest = l
        fl.foreach { e ⇒
            if (e != lRest.head)
                throw new UnknownError;
            else
                lRest = lRest.tail
        }
        true
    }

    property("take") = forAll(listAndIntGen) { (listAndCount: (List[String], Int)) ⇒
        val (l, count) = listAndCount
        classify(l.isEmpty, "list is empty") {
            classify(count == 0, "takes no elements") {
                classify(count == l.length, "takes all elements") {
                    classify(count > l.length, "takes too many elements") {
                        val fl = Chain(l: _*)
                        (
                            count <= l.length &&
                            fl.take(count) == Chain(l.take(count): _*) && fl.size == l.size
                        ) || { try { fl.take(count); false } catch { case _: Throwable ⇒ true } }
                    }
                }
            }
        }
    }

    property("takeUpTo") = forAll(listAndIntGen) { (listAndCount: (List[String], Int)) ⇒
        val (l, count) = listAndCount
        classify(l.isEmpty, "list is empty") {
            classify(count == 0, "takes no elements") {
                classify(count == l.length, "takes all elements") {
                    classify(count > l.length, "takes too many elements") {
                        val fl = Chain(l: _*)
                        Chain(l.take(count): _*) == fl.takeUpTo(count)
                    }
                }
            }
        }
    }

    property("takeWhile") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = Chain(l: _*)
        fl.takeWhile(filter) == Chain(l.takeWhile(filter): _*)
    }

    property("foreachWhile") = forAll { (l: List[Int], c: Int) ⇒
        def filter(s: Int): Boolean = s >= c
        val fl = Chain(l: _*)
        var newL = List.empty[Int]
        val result = fl.foreachWhile(s ⇒ if (filter(s)) { newL ::= s; true } else false)
        classify(result, "processed the complete list") {
            (!result || newL.reverse == l) /* we took all values */ &&
                (l.isEmpty && newL.isEmpty) || newL.reverse == l.takeWhile(filter)
        }
    }

    property("dropWhile") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() < 2
        val fl = Chain(l: _*)
        fl.dropWhile(filter) == Chain(l.dropWhile(filter): _*)
    }

    property("filter") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = Chain(l: _*)
        fl.filter(filter) == Chain(l.filter(filter): _*)
    }

    property("filterNot") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = Chain(l: _*)
        fl.filterNot(filter) == Chain(l.filterNot(filter): _*)
    }

    property("drop") = forAll(listAndIntGen) { (listAndCount: (List[String], Int)) ⇒
        val (l, count) = listAndCount
        val fl = Chain(l: _*)
        (fl.drop(0) eq fl) && (
            (count <= l.length && fl.drop(count) == Chain(l.drop(count): _*)) ||
            { try { fl.drop(count); false } catch { case _: Throwable ⇒ true } }
        )
    }

    property("zip(GenIterable)") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = Chain(l1: _*)
        classify(l1.size == l2.size, "same length") {
            fl1.zip(l2) == Chain(l1.zip(l2): _*)
        }
    }

    property("zip(Chain)") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        classify(l1.isEmpty, "the first list is empty") {
            classify(l2.isEmpty, "the second list is empty") {
                classify(l1.size == l2.size, "same length") {
                    fl1.zip(fl2) == Chain(l1.zip(l2): _*)
                }
            }
        }
    }

    property("zipWithIndex") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        classify(l.isEmpty, "empty", "non empty") {
            fl.zipWithIndex == Chain(l.zipWithIndex: _*)
        }
    }

    property("corresponds") = forAll(listsOfSingleCharStringsGen) { ls ⇒
        val (l1: List[String], l2: List[String]) = ls
        def test(s1: String, s2: String): Boolean = s1 == s2
        val fl1 = Chain(l1: _*)
        val fl2 = Chain(l2: _*)
        classify(fl1.isEmpty && fl2.isEmpty, "both lists are empty") {
            classify(fl1.size == fl2.size, "both lists have the same length") {
                classify(l1.corresponds(l2)(test), "both lists correspond") {
                    fl1.corresponds(fl2)(test) == l1.corresponds(l2)(test)
                }
            }
        }
    }

    property("mapConserve") = forAll { (l: List[String], c: Int) ⇒
        var alwaysTrue = true
        def transform(s: String): String = {
            if (s.length < c) s else { alwaysTrue = false; s + c }
        }
        val fl = Chain(l: _*)
        classify(l.forall(s ⇒ transform(s) eq s), "all strings remain the same") {
            val mappedFL = fl.mapConserve(transform)
            (mappedFL == Chain(l.mapConserve(transform): _*)) &&
                (!alwaysTrue || (fl eq mappedFL))
        }
    }

    property("reverse") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.reverse == Chain(l.reverse: _*)
    }

    property("mkString") = forAll { (l: List[String], pre: String, sep: String, post: String) ⇒
        val fl = Chain(l: _*)
        fl.mkString(pre, sep, post) == l.mkString(pre, sep, post)
    }

    property("toIterable") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*).toIterable.toList
        fl == l
    }
    property("toIterator") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*).toIterator.toList
        fl == l
    }
    property("toIntTrieSet") = forAll { l: List[Int] ⇒
        val fl = Chain(l: _*).toIntTrieSet.iterator.toSet
        fl == l.toSet
    }
    property("toIntArraySet") = forAll { l: List[Int] ⇒
        val fl = Chain(l: _*).toIntArraySet.iterator.toSet
        fl == l.toSet
    }

    property("toTraversable") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*).toTraversable.toList
        fl == l
    }

    property("implicit toTraversable") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        val tl: Traversable[String] = fl
        tl.isInstanceOf[Traversable[String]] && tl.toList == l
    }

    property("toStream") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*).toStream
        fl == l.toStream
    }

    property("copyToArray") = forAll(listAndIntGen) { v ⇒
        val (l, i) = v
        val fl = Chain(l: _*)
        val la = new Array[String](10)
        val fla = new Array[String](10)
        l.copyToArray(la, i / 3, i * 2)
        fl.copyToArray(fla, i / 3, i * 2)
        la.zip(fla).forall(e ⇒ e._1 == e._2)
    }

    property("fusing a list with itself result in the same list") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.fuse(Chain(l: _*), (x, y) ⇒ x) eq fl
    }

    property("fuse(different lists but same elements)") = forAll(smallListsGen) { l1: List[String] ⇒
        val l2s: Iterator[List[String]] = l1.permutations
        val cl1 = Chain(l1: _*)
        l2s.forall { l2 ⇒
            val cl2 = Chain(l2: _*)
            (cl1.fuse[String](cl2, (x, y) ⇒ if (x == y) x else y)).mkString ==
                l1.zip(l2).map(v ⇒ if (v._1 == v._2) v._1 else v._2).mkString
        }
    }

    property("fuse of lists with the same elements do not call the given function") = forAll { l1: List[String] ⇒
        val cl1 = Chain(l1: _*)
        val cl2 = Chain(l1: _*)
        var failed = false
        cl1.fuse(cl2, (s1, s2) ⇒ { failed = true; s1 })
        !failed

    }

    property("fuse combines (zip and map)") = forAll { (l1: List[String]) ⇒
        val cl1 = Chain(l1: _*)
        val cl3 = cl1.fuse(cl1, (s1, s2) ⇒ s1 + s2)
        (cl3.size == cl1.size) :| "length" &&
            Chain(l1.zip(l1).map { v ⇒
                val (s1: String, s2: String) = v
                if (s1 eq s2) s1 else s1 + s2
            }: _*) == cl3
    }

    property("(self) merge") = forAll { l: List[String] ⇒
        val fl = Chain(l: _*)
        fl.merge(Chain(l: _*))((x, y) ⇒ x) eq fl
    }

    property("merge(different lists but same elements)") = forAll(smallListsGen) { l1: List[String] ⇒
        val l2s: Iterator[List[String]] = l1.permutations
        val cl1 = Chain(l1: _*)
        l2s.forall { l2 ⇒
            val cl2 = Chain(l2: _*)
            (cl1.merge[String, String](cl2)((x, y) ⇒ if (x == y) x else y).mkString ==
                l1.zip(l2).map(v ⇒ if (v._1 == v._2) v._1 else v._2).mkString)
        }
    }

    property("merge combines (zip and map)") = forAll { (l1: List[String]) ⇒
        val cl1 = Chain(l1: _*)
        val cl3 = cl1.merge(cl1)((s1, s2) ⇒ s1 + s2)
        (cl3.size == cl1.size) :| "length" &&
            Chain(l1.zip(l1).map { v ⇒ val (s1: String, s2: String) = v; s1 + s2 }: _*) == cl3
    }

    property("forFirstN") = forAll { (l1: List[String]) ⇒
        val cl1 = Chain(l1: _*)
        l1.indices.forall { i ⇒
            var result = ""
            cl1.forFirstN(i)(result += _)
            result == l1.take(i).foldLeft("")(_ + _)
        }
    }

    property("toString") = forAll { l: List[String] ⇒
        Chain(l: _*).toString.endsWith("Naught")
    }

}

/*
 * PERFORMANCE EVALUATION
 * /// USING SCALA LIST
 *
 * var l : List[Long] = Nil
 *
 * def take2MultipleAndAdd() = {
 * var rest = l
 * if (rest.nonEmpty) {
 * val first = rest.head
 * rest = rest.tail
 * if (rest.nonEmpty) {
 * rest ::= first* rest.head
 * } else {
 * rest = l
 * }
 * }
 * rest
 * }
 *
 * def sum() : Long = {
 * var sum = 0l
 * var rest = l
 * while (rest.nonEmpty) {
 * sum += rest.head
 * rest = rest.tail
 * }
 * sum
 * }
 *
 *
 * {
 * val startTime = System.nanoTime
 * for (i <- 0 to 500) {
 * var j = 0
 * while (j < i) {
 * j += 1
 * l ::= 10
 * if (j % 20 == 0) sum()
 * else if (j % 3 == 0) take2MultipleAndAdd()
 * }
 * }
 * println("Elapsed time: "+(System.nanoTime-startTime))
 * }
 *
 * sum()
 *
 *
 * ///
 * /// USING CHAINED LIST
 * ///
 * import org.opalj.collection.immutable._
 * var l : Chain[Long] = Naught
 *
 * def take2MultipleAndAdd() = {
 * var rest = l
 * if (rest.nonEmpty) {
 * val first = rest.head
 * rest = rest.tail
 * if (rest.nonEmpty) {
 * rest :&:= first* rest.head
 * } else {
 * rest = l
 * }
 * }
 * rest
 * }
 *
 * def sum() : Long = {
 * var sum = 0l
 * var rest = l
 * while (rest.nonEmpty) {
 * sum += rest.head
 * rest = rest.tail
 * }
 * sum
 * }
 *
 * {
 * val startTime = System.nanoTime
 * for (i <- 0 to 500) {
 * var j = 0
 * while (j < i) {
 * j += 1
 * l :&:= 10
 * if (j % 20 == 0) sum()
 * else if (j % 3 == 0) take2MultipleAndAdd()
 * }
 * }
 * println("Elapsed time: "+(System.nanoTime-startTime))
 * }
 *
 * sum()
 *
 */
