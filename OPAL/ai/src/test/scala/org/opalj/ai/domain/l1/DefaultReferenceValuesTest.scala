/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.collection.mutable.Locals
import org.opalj.collection.immutable._
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.TestSupport.biProject
import org.opalj.value.BaseReferenceValues
import org.opalj.br.ObjectType
import org.opalj.br.ArrayType
import org.opalj.br.IntegerType
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.ClassHierarchy

/**
 * Tests the `ReferenceValues` domain.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultReferenceValuesTest extends AnyFunSpec with Matchers {

    class TheDomain
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with PredefinedClassHierarchy
        with PerInstructionPostProcessing
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultReferenceValuesBinding // <- PRIMARY TEST GOAL
        with l0.DefaultTypeLevelIntegerValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators

    object ValuesDomain extends TheDomain
    import ValuesDomain._

    //
    // TESTS
    //

    describe("the DefaultReferenceValues domain") {

        describe("isValueASubtypeOf") {

            it("should be able to cast an array of objects to an array of array of ints") {
                implicit val ch: ClassHierarchy = ValuesDomain.classHierarchy
                // ASSERTION
                isSubtypeOf(
                    ArrayType(ArrayType(IntegerType)),
                    ArrayType(ObjectType.Object)
                ) should be(true)

                val v1 = ArrayValue(111, No, false, ArrayType(ObjectType.Object), 1)
                v1.isValueASubtypeOf(ArrayType(ArrayType(IntegerType))) should be(Unknown)
            }
        }

        describe("abstractsOver") {

            it("an ArrayValue should abstract over ifself (with a different timestamp)") {

                val v1 = ArrayValue(-1, Unknown, true, ArrayType(ArrayType(IntegerType)), 1)
                val v2 = ArrayValue(-1, Unknown, true, ArrayType(ArrayType(IntegerType)), 2)
                v1.abstractsOver(v2) should be(true)
                v2.abstractsOver(v1) should be(true)
            }

        }

        describe("isMorePreciseThan") {

            it("an ObjectValue should not be more precise than a second value with the same properties") {

                val v1 = ObjectValue(-1, Unknown, false, ObjectType.Object, 1)
                val v2 = ObjectValue(-1, Unknown, false, ObjectType.Object, 1)

                assert(v1.abstractsOver(v2))
                assert(v2.abstractsOver(v1))
                assert(v1.join(-1, v2).isNoUpdate)
                assert(v2.join(-1, v1).isNoUpdate)

                v1.isMorePreciseThan(v2) should be(false)
                v2.isMorePreciseThan(v1) should be(false)
            }

            it("an ObjectValue should not be more precise than a second value with the same properties, but a different timestamp") {

                val v1 = ObjectValue(-1, Unknown, true, ObjectType.Object, 1)
                val v2 = ObjectValue(-1, Unknown, true, ObjectType.Object, 2)

                assert(v1.abstractsOver(v2))
                assert(v2.abstractsOver(v1))
                assert(!v1.join(-1, v2).isStructuralUpdate)
                assert(!v2.join(-1, v1).isStructuralUpdate)

                v1.isMorePreciseThan(v2) should be(false)
                v2.isMorePreciseThan(v1) should be(false)
            }

            it("an ArrayValue should not be more precise than itself") {

                val v1 = ArrayValue(-1, Unknown, true, ArrayType(ArrayType(IntegerType)), 1)
                v1.isMorePreciseThan(v1) should be(false)
            }

            it("an ArrayValue should not be more precise than itself (with a different timestamp)") {

                val v1 = ArrayValue(-1, Unknown, true, ArrayType(ArrayType(IntegerType)), 1)
                val v2 = ArrayValue(-1, Unknown, true, ArrayType(ArrayType(IntegerType)), 2)
                v1.isMorePreciseThan(v2) should be(false)
                v2.isMorePreciseThan(v1) should be(false)
            }

            it("a NullValue should be more precise than an ObjectValue but not vice versa") {
                val v1 = NullValue(-1)
                val v2 = ObjectValue(-1, Unknown, true, ObjectType("java/lang/Object"), 1)
                v1.isMorePreciseThan(v2) should be(true)
                v2.isMorePreciseThan(v1) should be(false)
            }

            it("an ObjectValue of type java/lang/String should be more precise than "+
                "an ObjectValue of type java/lang/Object but not vice versa") {
                val v1 = ObjectValue(-1, Unknown, true, ObjectType("java/lang/String"), 1)
                val v2 = ObjectValue(-1, Unknown, true, ObjectType("java/lang/Object"), 2)
                v1.isMorePreciseThan(v2) should be(true)
                v2.isMorePreciseThan(v1) should be(false)
            }
        }

        //
        // FACTORY METHODS
        //

        describe("using the factory methods") {

            it("it should be possible to create a representation for a non-null object "+
                "with a specific type") {
                val ref = ReferenceValue(444, No, true, ObjectType.Object)
                if (!ref.isNull.isNo || ref.origin != 444 || !ref.isPrecise)
                    fail("expected a precise, non-null reference value with pc 444;"+
                        " actual: "+ref)
            }

        }

        //
        // REFINEMENT
        //

        describe("refining a DomainValue that represents a reference value") {

            it("it should be able to correctly update the upper bound of the corresponding value") {

                val File = ObjectType("java/io/File")
                val Serializable = ObjectType.Serializable

                val theObjectValue = ObjectValue(-1, No, false, ObjectType.Object)
                val theFileValue = ObjectValue(-1, No, false, File)

                {
                    val (updatedOperands, updatedLocals) =
                        theObjectValue.refineUpperTypeBound(
                            -1, Serializable,
                            List(theObjectValue),
                            Locals(IndexedSeq(theFileValue, theObjectValue))
                        )
                    updatedOperands.head.asInstanceOf[TheReferenceValue].upperTypeBound.head should be(Serializable)
                    updatedLocals(0).asInstanceOf[TheReferenceValue].upperTypeBound.head should be(File)
                    updatedLocals(1).asInstanceOf[TheReferenceValue].upperTypeBound.head should be(Serializable)
                }

                {
                    // there is nothing to refine...
                    val (updatedOperands, _) =
                        theFileValue.refineUpperTypeBound(-1, Serializable, List(theObjectValue), Locals.empty)
                    updatedOperands.head should be(theObjectValue)
                }
            }

            it("should be able to correctly handle the refinement of the nullness property of a multiple reference value that has a more precise bound than any reference value to a single value") {

                val v0 = NullValue(111)
                val v1 = ObjectValue(222, Unknown, false, ObjectType("java/lang/Cloneable"), 2)
                val v2 = ObjectValue(222, No, UIDSet(ObjectType("java/lang/Cloneable"), ObjectType("java/lang/Iterable")), 2)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v0, v1),
                        IntTrieSet(111, 222),
                        Unknown, false, UIDSet(ObjectType("java/lang/Cloneable"), ObjectType("java/lang/Iterable")),
                        3
                    )

                val (refinedOperands, _) = mv1.refineIsNull(-1, No, List(mv1), Locals.empty)
                refinedOperands.head should be(v2)
            }

            it("should be able to correctly handle the refinement of the upper type bound of a multiple reference value that has a more precise bound than any reference value to a single value") {

                val v1 = ObjectValue(111, Unknown, true, ObjectType("java/lang/Object"), 1)
                val v2 = ObjectValue(222, Unknown, false, ObjectType("java/lang/Cloneable"), 2)
                val v3 = ObjectValue(222, Unknown, UIDSet(ObjectType("java/lang/Cloneable"), ObjectType("java/lang/Iterable")), 2)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 222),
                        Unknown, true, UIDSet(ObjectType("java/lang/Cloneable"), ObjectType("java/lang/Iterable")),
                        3
                    )

                val (refinedOperands, _) = mv1.refineUpperTypeBound(-1, ObjectType("java/lang/Iterable"), List(mv1), Locals.empty)
                refinedOperands.head should be(v3)
            }

            it("should be able to correctly handle the simple subsequent refinement of the upper type bound of a single value of a multiple reference value") {
                val Member = ObjectType("java/lang/reflect/Member")
                val Field = ObjectType("java/lang/reflect/Field")
                val Constructor = ObjectType("java/lang/reflect/Constructor")

                assert(isSubtypeOf(Field, Member))
                assert(isSubtypeOf(Constructor, Member))

                val v1 = ObjectValue(111, No, true, Field, 1)
                val v2 = ObjectValue(222, Unknown, false, Member, 2)
                val v3 = ObjectValue(222, Unknown, false, Constructor, 2)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 222),
                        Unknown, false, UIDSet(Member),
                        3
                    )

                val (refinedOperands, _) = mv1.refineUpperTypeBound(-1, Constructor, List(mv1), Locals.empty)
                refinedOperands.head should be(v3)
            }

            it("should be able to correctly handle the subsequent refinement of the upper type bound of a single value of a multiple reference value") {
                val Throwable = ObjectType.Throwable
                val Error = ObjectType.Error
                val RuntimeException = ObjectType.RuntimeException

                val v1 = ObjectValue(111, No, true, Error, 1)
                val v2 = ObjectValue(222, Unknown, false, Throwable, 2)
                val v3 = ObjectValue(222, Unknown, false, RuntimeException, 2)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 222),
                        Unknown, false, UIDSet(Throwable),
                        3
                    )

                val (refinedOperands, _) = mv1.refineUpperTypeBound(-1, RuntimeException, List(mv1), Locals.empty)
                refinedOperands.head should be(v3)
            }

        }

        //
        // SUMMARIES
        //

        describe("the summarize function") {

            it("it should calculate a meaningful upper type bound given "+
                "multiple different types of reference values") {
                summarize(
                    -1,
                    List(
                        ObjectValue(444, No, true, ObjectType.Object),
                        NullValue(444),
                        ObjectValue(668, No, true, ObjectType("java/io/File"))
                    )
                ) should be(ObjectValue(-1, Unknown, false, ObjectType.Object))
            }
        }

        //
        // JOIN
        //

        describe("joining two DomainValues that represent reference values") {

            val ref1 = ObjectValue(444, No, true, ObjectType.Object)

            val ref1Alt = ObjectValue(444, No, true, ObjectType.Object)

            val ref2 = ObjectValue(668, No, true, ObjectType.String)

            val ref2Alt = ObjectValue(668, No, true, ObjectType.String)

            val ref3 = ObjectValue(732, No, true, ObjectType.String)

            val ref1MergeRef2 = ref1.join(-1, ref2).value.asDomainReferenceValue

            val ref1AltMergeRef2Alt = ref1Alt.join(-1, ref2Alt).value.asDomainReferenceValue

            val ref1MergeRef2MergeRef3 = ref1MergeRef2.join(-1, ref3).value.asDomainReferenceValue

            val ref3MergeRef1MergeRef2 = ref3.join(-1, ref1MergeRef2).value.asDomainReferenceValue

            it("it should keep the old value when we merge a value with an identical value") {
                ref1.join(-1, ref1Alt) should be(MetaInformationUpdate(ref1))
            }

            it("it should represent both values after a join of two independent values") {
                val BaseReferenceValues(values) = ref1MergeRef2
                values.exists(_ == ref1) should be(true)
                values.exists(_ == ref2) should be(true)
            }

            it("it should represent all three values when we join a MultipleReferenceValue with an ObjectValue if all three values are independent") {
                val BaseReferenceValues(values) = ref1MergeRef2MergeRef3
                values.exists(_ == ref1) should be(true)
                values.exists(_ == ref2) should be(true)
                values.exists(_ == ref3) should be(true)
            }

            it("it should be able to join two value sets that contain (reference) identical values") {
                val BaseReferenceValues(values312) = ref3MergeRef1MergeRef2
                val BaseReferenceValues(values123) = ref1MergeRef2MergeRef3
                values312.toSet should be(values123.toSet)
            }

            it("it should be able to join two value sets where the original set is a superset of the second set") {
                // the values represent different values in time...
                val update = ref1MergeRef2MergeRef3.join(-1, ref1AltMergeRef2Alt)
                if (!update.isMetaInformationUpdate)
                    fail("expected: MetaInformationUpdate; actual: "+update)
            }

            it("it should be able to join two value sets where the original set is a subset of the second set") {
                ref1AltMergeRef2Alt.join(-1, ref1MergeRef2MergeRef3) should be(StructuralUpdate(ref1MergeRef2MergeRef3))
            }

            it("should be able to join two values with the exact same properties") {
                val v1 = ObjectValue(-1, Unknown, false, ObjectType.Object, 1)
                val v2 = ObjectValue(-1, Unknown, false, ObjectType.Object, 1)
                v1.join(-1, v2) should be(NoUpdate)
            }

            it("should be able to join two refined values sets") {
                val v0 = NullValue(111)
                val v1 = ObjectValue(444, Unknown, false, ObjectType.Object)
                val v2 = ObjectValue(555, Unknown, false, ObjectType.Object)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet[DomainSingleOriginReferenceValue](v0, v1, v2),
                        IntTrieSet(111, 444, 555),
                        Yes, true, UIDSet.empty,
                        nextRefId()
                    )
                val mv2 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(444, 555),
                        No, false, UIDSet(ObjectType.Object),
                        nextRefId()
                    )

                val joinResult = mv1.join(-1, mv2)
                joinResult.updateType should be(StructuralUpdateType)
                val ValuesDomain.DomainReferenceValueTag(joinedValue) = joinResult.value
                assert(joinedValue.isPrecise === false)
                joinedValue.upperTypeBound should be(UIDSet(ObjectType.Object))
            }

            it("should be able to rejoin a value") {
                val v0 = NullValue(111)
                val v1 = ArrayValue(222, Unknown, true, ArrayType(IntegerType), 2)
                val v2 = ArrayValue(222, No, true, ArrayType(IntegerType), 2)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v0, v1),
                        IntTrieSet(111, 222),
                        Yes, true, UIDSet.empty,
                        3
                    )
                val mv2 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v0, v2),
                        IntTrieSet(111, 222),
                        Unknown, true, UIDSet1(ArrayType(IntegerType)),
                        3
                    )

                mv1.join(-1, v2) should be(StructuralUpdate(mv2))
            }

            it("should be able to rejoin a refined object value") {
                val v0 = ObjectValue(222, No, false, ObjectType.Serializable, 2)

                val v1 = NullValue(111)
                val v2 = ObjectValue(222, Unknown, false, ObjectType.Serializable, 2)
                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 222),
                        isNull = Yes, true, UIDSet.empty, refId = 3
                    )

                val mv_expected =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v0, v1),
                        IntTrieSet(111, 222),
                        Unknown, false, UIDSet(ObjectType.Serializable),
                        3
                    )

                val mv_actual = v0.join(-1, mv1)
                if (mv_actual != StructuralUpdate(mv_expected)) {
                    fail(s"the join of: $v0\n"+
                        s"with:        $mv1\n"+
                        s"is:          $mv_actual\n"+
                        s"expected:    StructuralUpdate($mv_expected)")
                }
            }

            it("should be able to rejoin a refined array value") {
                val v0 = ArrayValue(222, No, false, ArrayType(ObjectType.Serializable), 2)

                val v1 = NullValue(111)
                val v2 = ArrayValue(222, Unknown, false, ArrayType(ObjectType.Serializable), 2)
                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 222),
                        Yes, true, UIDSet.empty,
                        3
                    )

                val mv_expected =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v0, v1),
                        IntTrieSet(111, 222),
                        Unknown, false, UIDSet(ArrayType(ObjectType.Serializable)),
                        3
                    )

                val mv_actual = v0.join(-1, mv1)

                if (mv_actual != StructuralUpdate(mv_expected)) {
                    fail(s"the join of: $v0\n"+
                        s"with:        $mv1\n"+
                        s"is:          $mv_actual\n"+
                        s"expected:    $mv_expected")
                }
            }

            it("should handle an idempotent rejoin of a nullvalue") {
                val v1 = ObjectValue(111, Unknown, false, ObjectType.Object, 1)
                val v2 = ObjectValue(222, No, false, ObjectType.Object, 2)
                val v3 = NullValue(222)

                val v2_join_v3 = ObjectValue(222, Unknown, false, ObjectType.Object, 2)

                assert(v2.join(-1, v3) == StructuralUpdate(v2_join_v3))

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2_join_v3),
                        IntTrieSet(111, 222),
                        Unknown, false, UIDSet(ObjectType.Object),
                        -1
                    )
                val mv1_join_v3 = mv1.join(-1, v3)
                mv1_join_v3 should be(NoUpdate)
            }

            /*
            it("should handle an idempotent rejoin of a value") {
                val v1 = ObjectValue(111, Unknown, false, ObjectType.Object, 1)
                val v2 = ObjectValue(222, No, false, ObjectType.Object, 2)
                val v2alt = ObjectValue(222, Yes, false, ObjectType.Object, 3)

                val v2_join_v2alt = ObjectValue(222, Unknown, false, ObjectType.Object, 2)

                assert(v2.join(-1, v2alt) == StructuralUpdate(v2_join_v2alt))

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2_join_v3),
                        Unknown, false, UIDSet(ObjectType.Object),
                        -1
                    )
                val expected_mv1_join_v3 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2_join_v3),
                        Unknown, false, UIDSet(ObjectType.Object),
                        -1
                    )

                println(mv1)
                println(v3)
                val mv1_join_v3 = mv1.join(-1, v3)
                println(mv1_join_v3)
                mv1_join_v3 should be(MetaInformationUpdate(expected_mv1_join_v3))

                // StructuralUpdate        ({_ <: java.lang.Object, null}[t=-1;values=«{_ <: java.lang.Object, null}[↦111;t=1], {_ <: java.lang.Object, null}[↦222;t=2]»])
                //MetaInformationUpdate   ({_ <: java.lang.Object, null}[t=-1;values=«{_ <: java.lang.Object, null}[↦111;t=1], {_ <: java.lang.Object, null}[↦222;t=2]»])
            }
            */

            it("should handle a join of a refined ObjectValue with a MultipleReferenceValue that references the unrefined ObjectValue") {

                val SecurityException = ObjectType("java/lang/SecurityException")
                val v0 = ObjectValue(111, No, false, SecurityException, refId = 106)
                val v1 = ObjectValue(111, No, false, ObjectType.Exception, refId = 103)
                val v2 = ObjectValue(555, Unknown, false, ObjectType.Throwable, refId = 107)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 555),
                        No, true, UIDSet(SecurityException),
                        refId = 3
                    )

                val mv_expected =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 555),
                        No, false, UIDSet(SecurityException),
                        3
                    )

                val mv_actual = v0.join(-1, mv1)

                if (mv_actual != StructuralUpdate(mv_expected)) {
                    fail(s"the join of: $v0\n"+
                        s"with:        $mv1\n"+
                        s"is:          $mv_actual\n"+
                        s"expected:    $mv_expected")
                }
            }

            it("should handle a join of an ObjectValue with a MultipleReferenceValue that references the refined ObjectValue") {

                val v0 = ObjectValue(111, Unknown, false, ObjectType.Object, refId = 103)
                val v1 = ObjectValue(111, No, false, ObjectType.Object, refId = 103)
                val v2 = ObjectValue(555, No, true, ObjectType.Object, refId = 107)

                val mv1 =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v1, v2),
                        IntTrieSet(111, 555),
                        No, true, UIDSet(ObjectType.Object),
                        refId = 3
                    )

                val mv_expected =
                    MultipleReferenceValues(
                        UIDSet2[DomainSingleOriginReferenceValue](v0, v2),
                        IntTrieSet(111, 555),
                        Unknown, false, UIDSet(ObjectType.Object),
                        3
                    )

                val mv_actual = v0.join(-1, mv1)

                mv_actual.updateType should be(StructuralUpdateType)

                if (mv_actual.value != mv_expected) {
                    fail(s"the join of: $v0\n"+
                        s"with:        $mv1\n"+
                        s"is:          ${mv_actual.value}\n"+
                        s"expected:    $mv_expected")
                }
            }

        }

        //
        // USAGE
        //

        describe("using the DefaultReferenceValues domain") {

            it("it should be able to handle the case where we throw a \"null\" value or some other value") {

                val classFiles = ClassFiles(locateTestResources("classfiles/cornercases.jar", "ai"))
                val classFile = classFiles.find(_._1.thisType.fqn == "cornercases/ThrowsNullValue").get._1
                val method = classFile.methods.find(_.name == "main").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)
                val exception = result.operandsArray(20)
                theDomain.refIsNull(-1, exception.head) should be(No)
            }

            val theProject = biProject("ai.jar")
            val targetType = ObjectType("ai/domain/ReferenceValuesFrenzy")
            val ReferenceValuesFrenzy = theProject.classFile(targetType).get

            it("it should be able to handle basic aliasing (method: \"aliases\"") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "aliases").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)
                import result.operandsArray
                import result.domain.IsNull

                val IsNull(v95) = theDomain.asReferenceValue(operandsArray(14).head)
                v95 should be(Unknown)

                val IsNull(v99) = result.operandsArray(20).head
                v99 should be(No)

                val IsNull(v111) = result.operandsArray(57).head
                v111 should be(No)

                val IsNull(v113) = result.operandsArray(59).head
                v113 should be(No)

                val IsNull(v117) = result.operandsArray(61).head
                v117 should be(Yes)
            }

            it("it should be able to handle complex refinements (method: \"complexRefinement\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "complexRefinement").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val theDomain.IsNull(lastChildAtPC22) = result.operandsArray(22).head
                lastChildAtPC22 should be(Unknown)
            }

            it("it should be possible to get precise information about a method's return values (method: \"maybeNull\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "maybeNull").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val theDomain.IsNull(firstReturn) = result.operandsArray(15).head
                firstReturn should be(Yes)

                val theDomain.IsNull(secondReturn) = result.operandsArray(23).head
                secondReturn should be(No)
            }

            it("it should be possible to handle conditional assignments (method: \"simpleConditionalAssignment\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "simpleConditionalAssignment").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)
                val result.domain.DomainReferenceValueTag(head) = result.operandsArray(26).head
                val value @ BaseReferenceValues(values) = head
                value.isNull should be(No)
                value.isPrecise should be(true)
                value.upperTypeBound should be(UIDSet(ObjectType.Object))
                values.size should be(2)
                values should be(UIDSet(
                    theDomain.ObjectValue(6, No, true, ObjectType.Object),
                    theDomain.ObjectValue(17, No, true, ObjectType.Object)
                ))
            }

            it("it should be possible to handle conditional assignments (method: \"conditionalAssignment1\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "conditionalAssignment1").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)
                val result.domain.DomainReferenceValueTag(head) = result.operandsArray(46).head
                val value @ BaseReferenceValues(values) = head
                value.isNull should be(Unknown)
                value.isPrecise should be(true) // one value is null and the other is precise
                value.upperTypeBound should be(UIDSet(ObjectType.Object))
                values.size should be(3)
                values should be(UIDSet(
                    theDomain.NullValue(0),
                    theDomain.NullValue(11),
                    theDomain.ObjectValue(16, No, true, ObjectType.Object)
                ))
            }

            it("it should be able to correctly track a MultipleReferenceValue's values in the presence of aliasing (method: \"complexAliasing\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "complexAliasing").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val theDomain.IsNull(doItCallParameter) = result.operandsArray(23).head
                doItCallParameter should be(Unknown)

                val theDomain.IsNull(secondReturn) = result.operandsArray(27).head
                secondReturn should be(Unknown)

                val result.domain.DomainReferenceValueTag(head) = result.operandsArray(27).head
                val BaseReferenceValues(values) = head
                values foreach { _.isNull should be(Unknown) }
            }

            it("it should be able to correctly determine the value's properties in the presence of aliasing (method: \"iterativelyUpdated\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "iterativelyUpdated").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val result.domain.DomainReferenceValueTag(head) = result.operandsArray(5).head
                val value @ BaseReferenceValues(values) = head
                value.isNull should be(No)
                values.size should be(2)
                values foreach { _.isNull should be(Unknown) }

                val BaseReferenceValues(returnValues) = result.operandsArray(25).head.asDomainReferenceValue
                returnValues foreach { _.isNull should be(Unknown) }
            }

            it("it should be able to handle control flow dependent values (method: \"cfDependentValues\")") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "cfDependentValues").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val BaseReferenceValues(values1) = result.operandsArray(43).head.asDomainReferenceValue; {
                    // the original value is null
                    val value1 = values1.head
                    val value2 = values1.tail.head
                    assert(
                        value1.isNull != value2.isNull &&
                            (value1.isNull.isYes || value1.isNull.isUnknown) &&
                            (value2.isNull.isYes || value2.isNull.isUnknown)
                    )
                }

                val BaseReferenceValues(values2) = result.operandsArray(47).head.asDomainReferenceValue; {
                    // the original value is null
                    val value1 = values2.head
                    val value2 = values2.tail.head
                    assert(
                        value1.isNull != value2.isNull &&
                            (value1.isNull.isYes || value1.isNull.isUnknown) &&
                            (value2.isNull.isYes || value2.isNull.isUnknown)
                    )
                }

                theDomain.asReferenceValue(result.operandsArray(58).head).isNull should be(No)

                theDomain.asReferenceValue(result.operandsArray(62).head).isNull should be(No)
            }

            it("it should be able to correctly refine a MultipleReferenceValues") {
                val method = ReferenceValuesFrenzy.methods.find(_.name == "multipleReferenceValues").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)
                import result.domain.asReferenceValue

                // u != null test
                asReferenceValue(result.operandsArray(26).head).isNull should be(Unknown)

                // first "doIt" call
                asReferenceValue(result.operandsArray(30).head).isNull should be(Unknown)

                // last "doIt" call
                asReferenceValue(result.operandsArray(47).head).isNull should be(No)

                // the "last return" is not dead
                result.operandsArray(45) should not be (null)
            }

            it("it should be able to correctly refine related MultipleReferenceValues (method: relatedMultipleReferenceValues)") {
                val methods = ReferenceValuesFrenzy.methods
                val method = methods.find(_.name == "relatedMultipleReferenceValues").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val BaseReferenceValues(values1) = result.operandsArray(77).head.asDomainReferenceValue
                values1.size should be(3)
                // values1 <=>
                // Set(
                //  {_ <: java.lang.Object, null}[↦8;t=104],
                //  null[↦20],
                //  {_ <: java.lang.Object, null}[↦4;t=103]
                // )
                values1 should be(UIDSet(
                    theDomain.NullValue(20),
                    theDomain.ObjectValue(8, Unknown, false, ObjectType.Object),
                    theDomain.ObjectValue(4, Unknown, false, ObjectType.Object)
                ))

                val BaseReferenceValues(values2) = result.operandsArray(87).head.asDomainReferenceValue
                values2.size should be(3)
                // if sorted by origin: a is o , a is p, a is p
                values2.foreach(_.isNull should be(Unknown))

                val BaseReferenceValues(values3) = result.operandsArray(95).head.asDomainReferenceValue
                values3.size should be <= (3)
                values3.foreach(_.isNull should be(Unknown))

                val BaseReferenceValues(values4) = result.operandsArray(104).head.asDomainReferenceValue
                values4.size should be(3)
                // Set({_ <: java.lang.Object, null}[↦12;t=105], {_ <: java.lang.Object, null}[↦8;t=104], _ <: java.lang.Object[↦4;t=103])
                values4 should be(UIDSet(
                    theDomain.ObjectValue(4, No, false, ObjectType.Object),
                    theDomain.ObjectValue(8, Unknown, false, ObjectType.Object),
                    theDomain.ObjectValue(12, Unknown, false, ObjectType.Object)
                ))

                val BaseReferenceValues(values5) = result.operandsArray(109).head.asDomainReferenceValue
                // Set(null[↦20], _ <: java.lang.Object[↦4;t=103], {_ <: java.lang.Object, null}[↦8;t=104])
                values5 should be(UIDSet(
                    theDomain.ObjectValue(4, No, false, ObjectType.Object),
                    theDomain.ObjectValue(8, Unknown, false, ObjectType.Object),
                    theDomain.NullValue(20)
                ))
            }

            it("it should be able to correctly refine the nullness property of MultipleReferenceValues (method: refiningNullnessOfMultipleReferenceValues)") {

                val methods = ReferenceValuesFrenzy.methods
                val method = methods.find(_.name == "refiningNullnessOfMultipleReferenceValues").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                val value35 @ BaseReferenceValues(values35) = result.operandsArray(35).head.asDomainReferenceValue
                value35.isNull should be(Yes)
                values35.size should be(2)
                // Set({_ <: java.lang.Object, null}[↦4;t=102], {_ <: java.lang.Object, null}[↦8;t=103])
                values35.foreach(_.isNull should be(Unknown))

                val value51 @ BaseReferenceValues(values51) = result.operandsArray(51).head.asDomainReferenceValue
                value51.isNull should be(Yes)
                values51.size should be(1)
                values51.head.isNull should be(Yes)
            }

            it("it should be able to correctly refine the upper type bound of MultipleReferenceValues (method: refiningTypeBoundOfMultipleReferenceValues)") {
                val methods = ReferenceValuesFrenzy.methods
                val method = methods.find(_.name == "refiningTypeBoundOfMultipleReferenceValues").get
                val theDomain = new TheDomain
                val result = BaseAI(method, theDomain)

                assert(theDomain.isSubtypeOf(ObjectType.Exception, ObjectType.Throwable))

                val value78 @ BaseReferenceValues(values78) = result.operandsArray(78).head.asDomainReferenceValue
                values78.size should be(1)
                value78.upperTypeBound should be(UIDSet(ObjectType.Exception))
            }
        }
    }
}
