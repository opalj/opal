/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.log.GlobalLogContext
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.MethodDescriptor.NoArgsAndReturnVoid
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Basic tests of the class hierarchy.
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
@RunWith(classOf[JUnitRunner])
class ClassHierarchyTest extends AnyFlatSpec with Matchers {

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE SUBTYPE RELATION RELATED FUNCTIONALITY
    //
    // -----------------------------------------------------------------------------------

    //
    // Setup
    //
    val jlsCHFile = "ClassHierarchyJLS.ths"
    val jlsCHCreator = List(() => getClass.getResourceAsStream(jlsCHFile))
    val jlsCH = ClassHierarchy(Iterable.empty, jlsCHCreator)(GlobalLogContext)

    val preInitCH = ClassHierarchy.PreInitializedClassHierarchy

    val javaLangCHFile = "JavaLangClassHierarchy.ths"
    val javaLangCHCreator = List(() => getClass.getResourceAsStream(javaLangCHFile))
    val javaLangCH = ClassHierarchy(Iterable.empty, javaLangCHCreator)(GlobalLogContext)

    val Object = ObjectType.Object
    val Throwable = ObjectType.Throwable
    val Exception = ObjectType.Exception
    val Error = ObjectType.Error
    val RuntimeException = ObjectType.RuntimeException
    val ArithmeticException = ObjectType.ArithmeticException
    val Cloneable = ObjectType.Cloneable
    val Serializable = ObjectType.Serializable
    val SeriablizableArray = ArrayType(Serializable)
    val SeriablizableArrayOfArray = ArrayType(SeriablizableArray)
    val AnUnknownType = ObjectType("myTest/AnUnknownType")
    val AnUnknownTypeArray = ArrayType(AnUnknownType)
    val CloneableArray = ArrayType(Cloneable)
    val ObjectArray = ArrayType.ArrayOfObject
    val intArray = ArrayType(IntegerType)
    val arrayOfIntArray = ArrayType(ArrayType(IntegerType))
    val longArray = ArrayType(LongType)

    // Commonly used package names
    val pgk = Some("generictypes/")

    val SimpleCTS = SimpleClassTypeSignature
    val CTS = ClassTypeSignature
    def CTS(cn: String, ptas: List[TypeArgument], suffix: List[SimpleClassTypeSignature] = Nil) = {
        ClassTypeSignature(pgk, SimpleCTS(cn, ptas), suffix)
    }

    def elementType(cts: ClassTypeSignature) = ProperTypeArgument(None, cts)

    def upperBoundType(cts: ClassTypeSignature) = ProperTypeArgument(Some(CovariantIndicator), cts)

    def lowerBoundType(cts: ClassTypeSignature) =
        ProperTypeArgument(Some(ContravariantIndicator), cts)

    /*SimpleClassTypeSignatures*/
    val baseSCTS = SimpleCTS("Base", Nil)
    val extBaseSCTS = SimpleCTS("ExtendedBase", Nil)
    val lvlTwoBaseSCTS = SimpleCTS("lvlTwoBase", Nil)
    val altBaseSCTS = SimpleCTS("AlternativeBase", Nil)
    val genericSCTS = SimpleCTS("SimpleGeneric", Nil)
    val unknownSCTS = SimpleCTS("Hidden", Nil)
    /*Nested ClassTypeSignatures*/
    val unknownCTS = CTS(pgk, unknownSCTS, Nil)
    val baseCTS = CTS(pgk, baseSCTS, Nil)
    val extBaseCTS = CTS(pgk, extBaseSCTS, Nil)
    val lvlTwoBaseCTS = CTS(pgk, lvlTwoBaseSCTS, Nil)
    val altBaseCTS = CTS(pgk, altBaseSCTS, Nil)
    val genericCTS = CTS(pgk, genericSCTS, Nil)
    val altInterfaceCTS = CTS(pgk, SimpleCTS("AltInterface", Nil), Nil)

    /** UContainer<UnknownType> */
    val unknownContainer =
        CTS("UContainer", List(elementType(CTS(pgk, SimpleCTS("UnknownType", Nil), Nil))))

    /** Interface<Base> */
    val iContainerWithBase = CTS("Interface", List(elementType(baseCTS)))

    /** Interface<AlternativeBase> */
    val iContainerWithAltBase = CTS("Interface", List(elementType(altBaseCTS)))

    /** BaseWithInterface<Base> // BaseWithInterface<E> implements Interface<E> */
    val IBaseContainerWithBase = CTS("BaseWithInterface", List(elementType(baseCTS)))

    /** BaseWithInterface<AlternativeBase> */
    val IBaseContainerWithAltBase = CTS("BaseWithInterface", List(elementType(altBaseCTS)))

    /** BaseWithConcreteInterface // BaseWithConcreteInterface implements Interface<Base> */
    val concreteInterfaceWithBase = CTS("BaseWithConcreteInterface", Nil)

    /** AltBaseWithConcreteInterface implements Interface<AlternativeBase> */
    val concreteInterfaceWithAltBase = CTS("AltBaseWithConcreteInterface", Nil)

    /** SubclassWithInterface extends SubGeneric implements Interface<AlternativeBase> */
    val subClassWithInterface = CTS("SubclassWithInterface", Nil)

    /** SubGeneric // SubGeneric extends SimpleGeneric<Base> */
    val concreteSubGeneric = CTS("SubGeneric", Nil)

    /** SimpleGeneric<Base> */
    val baseContainer = CTS("SimpleGeneric", List(elementType(baseCTS)))

    /** SimpleGeneric<AlternativeBase> */
    val altContainer = CTS("SimpleGeneric", List(elementType(altBaseCTS)))

    /** SimpleGeneric<lvlTwoBase>*/
    val lvlTwoContainer = CTS("SimpleGeneric", List(elementType(lvlTwoBaseCTS)))

    /** SimpleGeneric<Base> */
    val extBaseContainer = CTS("SimpleGeneric", List(elementType(extBaseCTS)))

    /** ExtendedGeneric<Base> */
    val extGenContainer = CTS("ExtendedGeneric", List(elementType(baseCTS)))

    /** SimpleGeneric<*> */
    val wildCardContainer = CTS("SimpleGeneric", List(Wildcard))

    /**  SimpleGeneric<? extends Base>*/
    val covariantContainer = CTS("SimpleGeneric", List(upperBoundType(baseCTS)))

    /**  SimpleGeneric<? super Base>*/
    val contravariantContainer = CTS("SimpleGeneric", List(lowerBoundType(extBaseCTS)))

    /**  SimpleGeneric<? super SimpleGenericBase>*/
    val contravariantWithContainer = CTS("SimpleGeneric", List(lowerBoundType(baseContainer)))

    /**  SimpleGeneric<? super Base> */
    val contravariantBaseContainer = CTS("SimpleGeneric", List(lowerBoundType(baseCTS)))

    /** SubGenericET<SimpleGeneric<Base>, SimpleGeneric<ExtendedBase>>*/
    val doubleContainerET = CTS("SubGenericET", List(elementType(baseCTS), elementType(extBaseCTS)))

    /** SubGenericTE<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/
    val doubleContainerTE = CTS("SubGenericTE", List(elementType(extBaseCTS), elementType(baseCTS)))

    /** IndependentSubclass<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/
    val doubleContainerBase =
        CTS("IndependentSubclass", List(elementType(extBaseCTS), elementType(baseCTS)))

    /** AltIndependentSubclass<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/
    val doubleContainerAltBase =
        CTS("AltIndependentSubclass", List(elementType(extBaseCTS), elementType(baseCTS)))

    /** SubGenericET<SimpleGeneric<ExtendedBaseBase>, SimpleGeneric<Base>>*/
    val wrongDoubleContainer =
        CTS("SubGenericET", List(elementType(extBaseCTS), elementType(baseCTS)))

    /** SimpleGeneric<SimpleGeneric<Base>> */
    val nestedBase = CTS("SimpleGeneric", List(elementType(baseContainer)))

    /** SimpleGeneric<SimpleGeneric<ExtendedBase>> */
    val nestedExtBase = CTS("SimpleGeneric", List(elementType(extBaseContainer)))

    /** SimpleGeneric<SimpleGeneric<lvlTwoContainer>> */
    val nestedLvlTwoBase = CTS("SimpleGeneric", List(elementType(lvlTwoContainer)))

    /** SimpleGeneric<SimpleGeneric<AlternativeBase>> */
    val nestedAltBase = CTS("SimpleGeneric", List(elementType(altContainer)))

    /** SimpleGeneric<ExtendedGeneric<Base>> */
    val nestedSubGenBase = CTS("SimpleGeneric", List(elementType(extGenContainer)))

    /** SimpleGeneric<SimpleGeneric<? extends Base>> */
    val nestedInnerCovariantContainer = CTS("SimpleGeneric", List(elementType(covariantContainer)))

    /** SimpleGeneric<? extends SimpleGeneric<Base>> */
    val nestedOutterCovariantContainer = CTS("SimpleGeneric", List(upperBoundType(baseContainer)))

    /** SimpleGeneric<? super SimpleGeneric<Base>> */
    val nestedContravariantContainer =
        CTS("SimpleGeneric", List(elementType(contravariantBaseContainer)))

    /**
     * GenericWithSuffix<Base>.Suffix1_1<Base>
     * {{{
     * public class GenericWithSuffix<E> {
     *    public class Suffix1_1<E>{}
     * }
     * }}}
     */
    val genericWithSuffix_publicSuffix1_1 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_2<Base> where Suffix1_2 does not implement interfaces or extend classes. */
    val genericWithSuffix_publicSuffix1_2 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_2", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_3 // Suffix1_3 extends Suffix1_2<E> where E is bound to the same type as GenericWithSuffix.*/
    val genericWithSuffix_Suffix1_3 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_3", Nil)))

    /** GenericWithSuffix<AlternativeBase>.Suffix1_1<AlternativeBase>.Suffix1_3 // Suffix1_3 extends Suffix1_2<E> where E is bound to the same type as GenericWithSuffix.*/
    val genericWithSuffix_altBase_Suffix1_3 = CTS("GenericWithSuffix", List(elementType(altBaseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix1_3", Nil)))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_4 // Suffix 1_4 implements Interface<Base> */
    val genericWithSuffix_Suffix1_4 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_4", Nil)))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_5<Base> // Suffix1_5<T> where T is not the FormalTypeParamter of the prefix of Suffix1_5.*/
    val genericWithSuffix_Suffix1_5 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_5", List(elementType(baseCTS)))))

    /** GenericWithSuffix<AlternativeBase>.Suffix1_1<AlternativeBase>.Suffix1_5<Base> // Suffix1_5<T> where T is not the FormalTypeParamter of the prefix of Suffix1_5.*/
    val genericWithSuffix_altBase_Suffix1_5 = CTS("GenericWithSuffix", List(elementType(altBaseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix1_5", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_5<AlternativeBase> // Suffix1_5<T> where T is not the FormalTypeParamter of the prefix of Suffix1_5.*/
    val genericWithSuffix_Suffix1_5_altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_5", List(elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_6 // Suffix1_6 extends Suffix1_5<Base> */
    val genericWithSuffix_Suffix1_6 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_6", Nil)))

    /** GenericWithSuffix<AlterantiveBase>.Suffix1_1<AlterantiveBase>.Suffix1_6 // Suffix1_6 extends Suffix1_5<Base> */
    val genericWithSuffix_altBase_Suffix1_6 = CTS("GenericWithSuffix", List(elementType(altBaseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix1_6", Nil)))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_7<T> // Suffix1_7 extends Base */
    val genericWithSuffix_Suffix1_7 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_7", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<AlternativeBase> */
    val genericWithSuffix_publicSuffix1_1_altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(altBaseCTS)))))
    /** GenericWithSuffix<Base>.Suffix1_1<AlternativeBase> */
    val genericWithSuffix_altBase_publicSuffix1_1 = CTS("GenericWithSuffix", List(elementType(altBaseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<Base>.Suffix1_2<Base> */
    val genericWithSuffix_publicSuffix1_1_Suffix1_2 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(baseCTS))), SimpleCTS("Suffix1_2", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix4_1<Base> // Suffix4_1<E> extends Suffix1_1<E> */
    val genericWithSuffix_publicSuffix4_1 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix4_1", List(elementType(baseCTS)))))

    /** GenericWithSuffix<Base>.Suffix1_1<AlternativeBase>.Suffix1_2 */
    val genericWithSuffix_publicSuffix1_1_Suffix1_2_altBase = CTS(
        "GenericWithSuffix",
        List(elementType(baseCTS)), List(SimpleCTS("Suffix1_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix1_1", List(elementType(baseCTS))))
    )

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_2<Base, AlternativeBase> */
    val genericWithSuffix_Suffix2_2 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(baseCTS))), SimpleCTS("Suffix2_2", List(elementType(baseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_2<AlternativeBase, Base> */
    val genericWithSuffix_Suffix2_2_l2altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(baseCTS))), SimpleCTS("Suffix2_2", List(elementType(altBaseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<altBase>.Suffix2_2<Base, AlternativeBase> */
    val genericWithSuffix_Suffix2_2_l1altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix2_2", List(elementType(baseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_3<Base, AlternativeBase> // Suffix2_3<V,W> extends Suffix2_2<V,W>*/
    val genericWithSuffix_Suffix2_3 = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(baseCTS))), SimpleCTS("Suffix2_3", List(elementType(baseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_3<AlternativeBase, AlternativeBase> // Suffix2_3<V,W> extends Suffix2_2<V,W>*/
    val genericWithSuffix_Suffix2_3_l2altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(baseCTS))), SimpleCTS("Suffix2_3", List(elementType(altBaseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_3<AlternativeBase, AlternativeBase> // Suffix2_3<S1 extends E, S2 extends T> where E is bound to GenericWithSuffix and T is bound to Suffix2_1 */
    val genericWithSuffix_Suffix2_4_base_altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix2_4", List(elementType(baseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_3<AlternativeBase, AlternativeBase> // Suffix2_3<S1 extends E, S2 extends T> where E is bound to GenericWithSuffix and T is bound to Suffix2_1 */
    val genericWithSuffix_Suffix2_4_altBase_altBase = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix2_4", List(elementType(altBaseCTS), elementType(altBaseCTS)))))

    /** GenericWithSuffix<Base>.Suffix2_1<Base>.Suffix2_3<AlternativeBase, Base> // Suffix2_3<S1 extends E, S2 extends T> where E is bound to GenericWithSuffix and T is bound to Suffix2_1 */
    val genericWithSuffix_Suffix2_4_altBase_base = CTS("GenericWithSuffix", List(elementType(baseCTS)), List(SimpleCTS("Suffix2_1", List(elementType(altBaseCTS))), SimpleCTS("Suffix2_4", List(elementType(altBaseCTS), elementType(baseCTS)))))

    //
    // Verify
    //

    behavior of "the default ClassHierarchy"

    it should "be upwards closed (complete)" in {
        if (preInitCH.rootTypes.size != 1) {
            fail(
                "The default class hierarchy has unexpected root types: "+
                    preInitCH.rootTypes.mkString(", ")
            )
        }
    }

    behavior of "the default ClassHierarchy's leafTypes method"

    it should "return all leaf types" in {
        jlsCH.leafTypes should be(UIDSet(
            ObjectType("java/lang/String"),
            ObjectType("java/lang/Class"),
            ObjectType("java/lang/Cloneable"),
            ObjectType("java/lang/Comparable")
        ))
    }

    behavior of "the default ClassHierarchy's isKnown method"

    it should "return true for all known types" in {
        preInitCH.isKnown(Throwable) should be(true)
    }

    it should "return false for all unknown types" in {
        preInitCH.isKnown(AnUnknownType) should be(false)
    }

    behavior of "the default ClassHierarchy's isDirectSupertypeInformationComplete method"

    it should "return true if a type's super type information is definitive complete" in {
        javaLangCH.isDirectSuperclassTypeInformationComplete(Object) should be(true)
        javaLangCH.isDirectSuperclassTypeInformationComplete(Throwable) should be(true)
    }

    it should "return false if a type's super type information is not guaranteed to be complete" in {
        javaLangCH.isDirectSuperclassTypeInformationComplete(Serializable) should be(false)
        javaLangCH.isDirectSuperclassTypeInformationComplete(AnUnknownType) should be(false)
    }

    behavior of "the default ClassHierarchy's allSupertypesOf method w.r.t. class types"

    it should "identify the same set of class as allSupertypes if the bound contains only one element" in {
        val supertypesOfString = javaLangCH.allSupertypes(ObjectType.String, true)

        supertypesOfString should be(javaLangCH.allSupertypesOf(UIDSet(ObjectType.String), true))
    }

    behavior of "the default ClassHierarchy's leafTypes method w.r.t. class types"

    it should "correctly return a class if we give it a class and all types it inherits from" in {
        val supertypesOfString = javaLangCH.allSupertypes(ObjectType.String, true)

        javaLangCH.leafTypes(supertypesOfString) should be(UIDSet(ObjectType.String))
    }

    it should "correctly return a class' direct supertypes if we give it all types the class inherits from" in {
        val supertypesOfString = javaLangCH.allSupertypes(ObjectType.String, false)

        javaLangCH.leafTypes(supertypesOfString) should be(
            UIDSet(
                ObjectType.Serializable,
                ObjectType("java/lang/Comparable"),
                ObjectType("java/lang/CharSequence")
            )
        )
    }

    behavior of "the default ClassHierarchy's is(A)SubtypeOf method w.r.t. class types"

    it should "return false(Unknown) if the \"subtype\" is unknown" in {
        preInitCH.isASubtypeOf(AnUnknownType, Throwable) should be(Unknown)
        preInitCH.isSubtypeOf(AnUnknownType, Throwable) should be(false)
    }

    it should "return true(Yes) if a class-type indirectly inherits an interface-type" in {
        preInitCH.isASubtypeOf(ArithmeticException, Serializable) should be(Yes)
        preInitCH.isSubtypeOf(ArithmeticException, Serializable) should be(true)
    }

    it should "always return true(Yes) if both types are identical" in {
        preInitCH.isASubtypeOf(ArithmeticException, ArithmeticException) should be(Yes)
        preInitCH.isASubtypeOf(AnUnknownType, AnUnknownType) should be(Yes)
        preInitCH.isSubtypeOf(ArithmeticException, ArithmeticException) should be(true)
        preInitCH.isSubtypeOf(AnUnknownType, AnUnknownType) should be(true)
    }

    it should "return true(Yes) for interface types when the given super type is Object even if the interface type's supertypes are not known" in {
        preInitCH.isASubtypeOf(Serializable, Object) should be(Yes)
        preInitCH.isSubtypeOf(Serializable, Object) should be(true)
    }

    it should "return false(No) for a type that is not a subtype of another type and all type information is known" in {
        // "only" classes
        preInitCH.isASubtypeOf(Error, Exception) should be(No)
        preInitCH.isSubtypeOf(Error, Exception) should be(false)
        preInitCH.isASubtypeOf(Exception, Error) should be(No)
        preInitCH.isSubtypeOf(Exception, Error) should be(false)
        preInitCH.isASubtypeOf(Exception, RuntimeException) should be(No)
        preInitCH.isSubtypeOf(Exception, RuntimeException) should be(false)

        // "only" interfaces
        preInitCH.isASubtypeOf(Serializable, Cloneable) should be(No)
        preInitCH.isSubtypeOf(Serializable, Cloneable) should be(false)

        // class and interface
        preInitCH.isASubtypeOf(ArithmeticException, Cloneable) should be(No)
        preInitCH.isSubtypeOf(ArithmeticException, Cloneable) should be(false)
    }

    it should "return false(Unknown) if two types are not in an inheritance relationship but the subtype's supertypes are not guaranteed to be known" in {
        javaLangCH.isKnown(Serializable) should be(true)
        javaLangCH.isSupertypeInformationComplete(Serializable) should be(false)
        javaLangCH.isASubtypeOf(Serializable, Cloneable) should be(Unknown)
        javaLangCH.isSubtypeOf(Serializable, Cloneable) should be(false)
    }

    behavior of "the preInitialized ClassHierarchy's is(A)SubtypeOf method w.r.t. Exceptions"

    it should "correctly reflect the base exception hierarchy" in {
        preInitCH.isASubtypeOf(Throwable, Object) should be(Yes)
        preInitCH.isSubtypeOf(Throwable, Object) should be(true)
        preInitCH.isASubtypeOf(Error, Throwable) should be(Yes)
        preInitCH.isSubtypeOf(Error, Throwable) should be(true)
        preInitCH.isASubtypeOf(RuntimeException, Exception) should be(Yes)
        preInitCH.isSubtypeOf(RuntimeException, Exception) should be(true)
        preInitCH.isASubtypeOf(Exception, Throwable) should be(Yes)
        preInitCH.isSubtypeOf(Exception, Throwable) should be(true)

        preInitCH.isASubtypeOf(Object, Throwable) should be(No)
        preInitCH.isSubtypeOf(Object, Throwable) should be(false)

        preInitCH.isASubtypeOf(AnUnknownType, Object) should be(Yes)
        preInitCH.isSubtypeOf(AnUnknownType, Object) should be(true)
        preInitCH.isASubtypeOf(Object, AnUnknownType) should be(No)
        preInitCH.isSubtypeOf(Object, AnUnknownType) should be(false)

    }

    behavior of "the ClassHierarchy's is(A)SubtypeOf method w.r.t. Arrays"

    it should "correctly reflect the basic type hierarchy related to Arrays" in {
        preInitCH.isASubtypeOf(ObjectArray, Object) should be(Yes)
        preInitCH.isSubtypeOf(ObjectArray, Object) should be(true)
        preInitCH.isASubtypeOf(SeriablizableArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArray, ObjectArray) should be(true)
        preInitCH.isASubtypeOf(CloneableArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(CloneableArray, ObjectArray) should be(true)
        preInitCH.isASubtypeOf(ObjectArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(ObjectArray, ObjectArray) should be(true)
        preInitCH.isASubtypeOf(SeriablizableArray, SeriablizableArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArray, SeriablizableArray) should be(true)
        preInitCH.isASubtypeOf(AnUnknownTypeArray, AnUnknownTypeArray) should be(Yes)
        preInitCH.isSubtypeOf(AnUnknownTypeArray, AnUnknownTypeArray) should be(true)

        preInitCH.isASubtypeOf(Object, ObjectArray) should be(No)
        preInitCH.isSubtypeOf(Object, ObjectArray) should be(false)
        preInitCH.isASubtypeOf(CloneableArray, SeriablizableArray) should be(No)
        preInitCH.isSubtypeOf(CloneableArray, SeriablizableArray) should be(false)

        preInitCH.isASubtypeOf(AnUnknownTypeArray, SeriablizableArray) should be(Unknown)
        preInitCH.isSubtypeOf(AnUnknownTypeArray, SeriablizableArray) should be(false)

        preInitCH.isASubtypeOf(SeriablizableArray, AnUnknownTypeArray) should be(No)
        preInitCH.isSubtypeOf(SeriablizableArray, AnUnknownTypeArray) should be(false)
    }

    it should "correctly reflect the type hierarchy related to Arrays of primitives" in {
        preInitCH.isASubtypeOf(intArray, Object) should be(Yes)
        preInitCH.isSubtypeOf(intArray, Object) should be(true)
        preInitCH.isASubtypeOf(intArray, Serializable) should be(Yes)
        preInitCH.isSubtypeOf(intArray, Serializable) should be(true)
        preInitCH.isASubtypeOf(intArray, Cloneable) should be(Yes)
        preInitCH.isSubtypeOf(intArray, Cloneable) should be(true)
        preInitCH.isASubtypeOf(intArray, intArray) should be(Yes)
        preInitCH.isSubtypeOf(intArray, intArray) should be(true)

        preInitCH.isASubtypeOf(intArray, longArray) should be(No)
        preInitCH.isSubtypeOf(intArray, longArray) should be(false)
        preInitCH.isASubtypeOf(longArray, intArray) should be(No)
        preInitCH.isSubtypeOf(longArray, intArray) should be(false)

        preInitCH.isASubtypeOf(arrayOfIntArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(arrayOfIntArray, ObjectArray) should be(true)
        preInitCH.isASubtypeOf(arrayOfIntArray, SeriablizableArray) should be(Yes)
        preInitCH.isSubtypeOf(arrayOfIntArray, SeriablizableArray) should be(true)
    }

    it should "correctly reflect the type hierarchy related to Arrays of Arrays" in {
        preInitCH.isASubtypeOf(SeriablizableArrayOfArray, Object) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, Object) should be(true)
        preInitCH.isASubtypeOf(SeriablizableArrayOfArray, SeriablizableArrayOfArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, SeriablizableArrayOfArray) should be(true)

        preInitCH.isASubtypeOf(SeriablizableArrayOfArray, SeriablizableArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, SeriablizableArray) should be(true)
        preInitCH.isASubtypeOf(SeriablizableArrayOfArray, ObjectArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, ObjectArray) should be(true)
        preInitCH.isASubtypeOf(SeriablizableArrayOfArray, CloneableArray) should be(Yes)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, CloneableArray) should be(true)

        preInitCH.isASubtypeOf(SeriablizableArrayOfArray, AnUnknownTypeArray) should be(No)
        preInitCH.isSubtypeOf(SeriablizableArrayOfArray, AnUnknownTypeArray) should be(false)
    }

    behavior of "the ClassHierarchy's directSubtypesOf(UpperTypeBound) method"

    val typesProject =
        Project(
            ClassFiles(locateTestResources("classhierarchy.jar", "bi")),
            Iterable.empty,
            true
        )

    val cRootType = ObjectType("classhierarchy/CRoot")
    val cRootAType = ObjectType("classhierarchy/CRootA")
    val cRootAABType = ObjectType("classhierarchy/CRootAAB")
    val cRootAAABBCType = ObjectType("classhierarchy/CRootAAABBC")
    val iRootAType = ObjectType("classhierarchy/IRootA")
    val iRootBType = ObjectType("classhierarchy/IRootB")
    val iRootCType = ObjectType("classhierarchy/IRootC")

    it should "return the given upper type bound if it just contains a single type" in {
        import typesProject.classHierarchy.directSubtypesOf
        directSubtypesOf(UIDSet[ObjectType](cRootType)) should be(UIDSet(cRootType))
        directSubtypesOf(UIDSet[ObjectType](iRootAType)) should be(UIDSet(iRootAType))
        directSubtypesOf(UIDSet[ObjectType](cRootAAABBCType)) should be(UIDSet(cRootAAABBCType))
    }

    it should "return the type that is the subtype of all types of the bound" in {
        import typesProject.classHierarchy.directSubtypesOf
        directSubtypesOf(UIDSet[ObjectType](iRootAType, iRootBType)) should be(UIDSet(cRootAABType))
        directSubtypesOf(UIDSet[ObjectType](cRootAType, iRootBType)) should be(UIDSet(cRootAABType))
        directSubtypesOf(UIDSet[ObjectType](iRootAType, iRootCType)) should be(UIDSet(cRootAAABBCType))
        directSubtypesOf(UIDSet[ObjectType](iRootAType, iRootBType, iRootCType)) should be(UIDSet(cRootAAABBCType))
        directSubtypesOf(UIDSet[ObjectType](iRootBType, iRootCType)) should be(UIDSet(cRootAAABBCType))
        directSubtypesOf(UIDSet[ObjectType](cRootAType, iRootCType)) should be(UIDSet(cRootAAABBCType))
        directSubtypesOf(UIDSet[ObjectType](cRootAABType, iRootCType)) should be(UIDSet(cRootAAABBCType))
    }

    it should "not fail if no common subtype exists" in {
        import typesProject.classHierarchy.directSubtypesOf
        directSubtypesOf(UIDSet[ObjectType](cRootType, iRootBType)) should be(UIDSet.empty)
    }

    behavior of "the ClassHierarchy's allSubclassTypes method"

    it should "return the empty iterator if the type has no subclasses" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(cRootAAABBCType, false).size should be(0)
    }

    it should "return the singleton iterator if the type has no subclasses but we want the relation to be reflexive" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(cRootAAABBCType, true).size should be(1)
    }

    it should "return all subclasses of a leaf-type in the complete type hierarchy" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(cRootType, true).toSet should be(Set(cRootType))
    }

    it should "return all subclasses (non-reflexive) of a leaf-type in the complete type hierarchy" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(cRootType, false).toSet should be(Set.empty)
    }

    it should "return all subclasses in the complete type hierarchy" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(cRootAType, true).toSet should be(Set(cRootAType, cRootAABType, cRootAAABBCType))
    }

    it should "return all subclasses (non-reflexive) in the complete type hierarchy" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(cRootAType, false).toSet should be(Set(cRootAABType, cRootAAABBCType))
    }

    it should "return all subclasses (non-reflexive) of a class with multiple direct subclasses" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(ObjectType("java/lang/IndexOutOfBoundsException"), false).toSet should be(
            Set(
                ObjectType("java/lang/ArrayIndexOutOfBoundsException"),
                ObjectType("java/lang/StringIndexOutOfBoundsException")
            )
        )
    }

    it should "return all subclasses (reflexive) of a class with multiple direct subclasses" in {
        import typesProject.classHierarchy.allSubclassTypes
        allSubclassTypes(ObjectType("java/lang/IndexOutOfBoundsException"), true).toSet should be(
            Set(
                ObjectType("java/lang/IndexOutOfBoundsException"),
                ObjectType("java/lang/ArrayIndexOutOfBoundsException"),
                ObjectType("java/lang/StringIndexOutOfBoundsException")
            )
        )
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE HANDLING OF GENERICS
    //
    // -----------------------------------------------------------------------------------

    behavior of "isASubtypeOf method w.r.t. concrete generics"

    it should "return YES iff the type arguments do match considering variance indicators and wildcards" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(baseContainer, baseContainer) should be(Yes)
        isASubtypeOf(baseContainer, wildCardContainer) should be(Yes)
        isASubtypeOf(concreteSubGeneric, baseContainer) should be(Yes)
        isASubtypeOf(wildCardContainer, wildCardContainer) should be(Yes)
        isASubtypeOf(extBaseContainer, covariantContainer) should be(Yes)
        isASubtypeOf(baseContainer, covariantContainer) should be(Yes)
        isASubtypeOf(baseContainer, contravariantContainer) should be(Yes)
        isASubtypeOf(doubleContainerET, baseContainer) should be(Yes)
        isASubtypeOf(doubleContainerTE, baseContainer) should be(Yes)
        isASubtypeOf(doubleContainerBase, baseContainer) should be(Yes)
    }

    it should "return NO if the type arguments doesn't match considering variance indicators and wildcards" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(baseContainer, extBaseContainer) should be(No)
        isASubtypeOf(concreteSubGeneric, altContainer) should be(No)
        isASubtypeOf(wildCardContainer, baseContainer) should be(No)
        isASubtypeOf(altContainer, contravariantContainer) should be(No)
        isASubtypeOf(extBaseContainer, contravariantBaseContainer) should be(No)
        isASubtypeOf(altContainer, covariantContainer) should be(No)
        isASubtypeOf(baseContainer, doubleContainerET) should be(No)
        isASubtypeOf(baseContainer, doubleContainerTE) should be(No)
        isASubtypeOf(wrongDoubleContainer, baseContainer) should be(No)
        isASubtypeOf(doubleContainerAltBase, baseContainer) should be(No)
    }

    behavior of "isASubtypeOf method w.r.t. generics with interface types"

    it should "return YES iff the subtype directly implements the interface with matching type arguments" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(concreteInterfaceWithBase, iContainerWithBase) should be(Yes)
        isASubtypeOf(IBaseContainerWithBase, iContainerWithBase) should be(Yes)

    }

    it should "return NO if the subtype doesn't directly implement the interface with matching type arguments" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(IBaseContainerWithAltBase, iContainerWithBase) should be(No)
        isASubtypeOf(concreteInterfaceWithAltBase, IBaseContainerWithBase) should be(No)
        isASubtypeOf(concreteInterfaceWithBase, concreteInterfaceWithAltBase) should be(No)
    }

    it should "return YES iff the subtype implements the given interface with matching type arguments through some supertype" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(subClassWithInterface, iContainerWithAltBase) should be(Yes)
        isASubtypeOf(subClassWithInterface, concreteSubGeneric) should be(Yes)
        isASubtypeOf(subClassWithInterface, baseContainer) should be(Yes)
    }

    it should "return NO if the subtype doesn't implement the given interface with matching type arguments through some supertype" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(subClassWithInterface, iContainerWithBase) should be(No)
    }

    it should "return UNKNOWN if one of the arguments is an unknown type" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(unknownContainer, baseContainer) should be(Unknown)
    }

    behavior of "isASubtypeOf method w.r.t. generics with nested types"

    it should "return YES iff if nested type arguments of the supertype and the subtype do match" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(nestedInnerCovariantContainer, nestedInnerCovariantContainer) should be(Yes)
        isASubtypeOf(nestedExtBase, nestedInnerCovariantContainer) should be(Yes)
        isASubtypeOf(nestedBase, nestedContravariantContainer) should be(Yes)
        isASubtypeOf(nestedBase, contravariantWithContainer) should be(Yes)
        isASubtypeOf(nestedBase, nestedOutterCovariantContainer) should be(Yes)
    }

    it should "return NO if nested type arguments of the subtype and the supertype doesn't match" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(nestedBase, nestedAltBase) should be(No)
        isASubtypeOf(nestedAltBase, nestedInnerCovariantContainer) should be(No)
        isASubtypeOf(nestedLvlTwoBase, nestedContravariantContainer) should be(No)
        isASubtypeOf(nestedSubGenBase, nestedContravariantContainer) should be(No)
    }

    behavior of "isASubtypeOf method w.r.t. generics with class suffix (e.g. by inner classes)"

    it should "return YES iff the class suffixes of a ClassTypeSignature of inner classes also match when considering generic type arguments" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf

        isASubtypeOf(genericWithSuffix_Suffix1_7, baseCTS) should be(Yes)
        isASubtypeOf(genericWithSuffix_publicSuffix1_1, genericWithSuffix_publicSuffix1_1) should be(Yes)
        isASubtypeOf(genericWithSuffix_publicSuffix4_1, genericWithSuffix_publicSuffix1_1) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix1_4, iContainerWithBase) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix1_3, genericWithSuffix_publicSuffix1_2) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix1_6, genericWithSuffix_Suffix1_5) should be(Yes)
        isASubtypeOf(genericWithSuffix_altBase_Suffix1_6, genericWithSuffix_altBase_Suffix1_5) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix2_3, genericWithSuffix_Suffix2_2) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix2_4_base_altBase, genericWithSuffix_Suffix2_4_base_altBase) should be(Yes)
    }

    it should "return NO if the class suffixes of a ClassTypeSignature of inner classes doesn't match when considering generic type arguments " in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(genericWithSuffix_publicSuffix1_1, genericWithSuffix_publicSuffix1_1_altBase) should be(No)
        isASubtypeOf(genericWithSuffix_altBase_publicSuffix1_1, genericWithSuffix_publicSuffix1_1) should be(No)
        isASubtypeOf(genericWithSuffix_publicSuffix1_1_Suffix1_2, genericWithSuffix_publicSuffix1_1) should be(No)
        isASubtypeOf(genericWithSuffix_publicSuffix1_1_Suffix1_2, genericWithSuffix_publicSuffix1_1_Suffix1_2_altBase) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix1_4, iContainerWithAltBase) should be(No)
        isASubtypeOf(genericWithSuffix_altBase_Suffix1_3, iContainerWithBase) should be(No)
        isASubtypeOf(genericWithSuffix_altBase_Suffix1_3, genericWithSuffix_publicSuffix1_2) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix1_6, genericWithSuffix_altBase_Suffix1_5) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix1_6, genericWithSuffix_Suffix1_5_altBase) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix2_2, genericWithSuffix_Suffix2_2_l2altBase) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix2_2, genericWithSuffix_Suffix2_2_l1altBase) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix2_3_l2altBase, genericWithSuffix_Suffix2_2) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix2_4_base_altBase, genericWithSuffix_Suffix2_4_altBase_altBase) should be(No)
    }

    behavior of "isASubtypeOf method w.r.t. generics specified by formal type parameters"

    it should "return YES iff the subtype extends the class and implements all declared interfaces of the FormalTypeParameter" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(extBaseCTS, FormalTypeParameter("X", Some(baseCTS), Nil)) should be(Yes)
        isASubtypeOf(subClassWithInterface, FormalTypeParameter("T", Some(concreteSubGeneric), List(iContainerWithAltBase))) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix1_7, FormalTypeParameter("T", Some(baseCTS), Nil)) should be(Yes)
        isASubtypeOf(genericWithSuffix_Suffix1_4, FormalTypeParameter("T", None, List(iContainerWithBase))) should be(Yes)
    }

    it should "return NO if the subtype doesn't extends the class and implements all declared interfaces of the FormalTypeParameter" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(altBaseCTS, FormalTypeParameter("X", Some(baseCTS), Nil)) should be(No)
        isASubtypeOf(subClassWithInterface, FormalTypeParameter("T", Some(concreteSubGeneric), List(iContainerWithAltBase, altInterfaceCTS))) should be(No)
        isASubtypeOf(genericWithSuffix_Suffix1_4, FormalTypeParameter("T", None, List(iContainerWithAltBase))) should be(No)
    }

    it should "return UNKNOWN if an unknown type is encountered" in {
        implicit val genericProject = ClassHierarchyTest.genericProject
        import genericProject.classHierarchy.isASubtypeOf
        isASubtypeOf(unknownContainer, FormalTypeParameter("X", Some(baseCTS), Nil)) should be(Unknown)
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTS IF WE HAVE AN INCOMPLETE CLASS HIERARCHY
    //
    // -----------------------------------------------------------------------------------

    val apacheANTCH =
        ClassHierarchy(
            Iterable.empty,
            List(() => getClass.getResourceAsStream("ApacheANT1.7.1.ClassHierarchy.ths"))
        )(GlobalLogContext)

    it should "be possible to get all supertypes, even if not all information is available" in {

        val mi = ObjectType("org/apache/tools/ant/taskdefs/MacroInstance")
        apacheANTCH.allSupertypes(mi) should be(UIDSet(
            ObjectType("org/apache/tools/ant/Task"),
            ObjectType("org/apache/tools/ant/ProjectComponent"),
            ObjectType("org/apache/tools/ant/TaskContainer"),
            ObjectType("org/apache/tools/ant/DynamicAttribute"),
            ObjectType.Object
        ))
    }

    // -----------------------------------------------------------------------------------
    //
    // TESTING THE TRAVERSAL OF THE CLASS HIERARCHY
    //
    // -----------------------------------------------------------------------------------

    final val clusteringProject = {
        val classFiles = ClassFiles(locateTestResources("classfiles/ClusteringTestProject.jar", "bi"))
        Project(classFiles, Iterable.empty, true)
    }

    behavior of "the ClassHierarchy's method to traverse the class hierarchy"

    it should "correctly find all suptypes of an interface" in {
        import clusteringProject.classHierarchy

        val window = ObjectType("pattern/decorator/example1/Window")
        val simpleWindow = ObjectType("pattern/decorator/example1/SimpleWindow")

        classHierarchy.isKnown(window) should be(true)
        classHierarchy.isKnown(simpleWindow) should be(true)

        classHierarchy.isASubtypeOf(window, simpleWindow) should be(No)
        classHierarchy.isSubtypeOf(simpleWindow, window) should be(true)

        // check if the SimpleWindow is in the set of all subtypes of Window
        var subtypes = Set.empty[ObjectType]
        classHierarchy.foreachSubtype(window) { subtypes += _ }
        if (!subtypes.contains(simpleWindow))
            fail(s"SimpleWindow is not among the subtypes: $subtypes; "+
                s"SimpleWindow <: ${classHierarchy.allSupertypes(simpleWindow)}; "+
                s"Window >: ${classHierarchy.allSubtypes(window, false)}\n"+
                classHierarchy.asTSV)

        clusteringProject.classFile(simpleWindow).get.methods find { method =>
            method.name == "draw" && method.descriptor == NoArgsAndReturnVoid
        } should be(Symbol("defined"))

        import clusteringProject.resolveInterfaceMethodReference
        resolveInterfaceMethodReference(window, "draw", NoArgsAndReturnVoid) should be(Symbol("nonEmpty"))
    }

    val jvmFeaturesProject =
        Project(
            ClassFiles(locateTestResources("jvm_features-1.8-g-parameters-genericsignature.jar", "bi")),
            Iterable.empty,
            true
        )

    it should "correctly iterate over all suptypes of Object, even without the JDK included" in {
        var foundSomeEnumerationClass = false
        jvmFeaturesProject.classHierarchy.foreachSubtypeCF(ObjectType.Object, false) { subTypeCF =>
            val subType = subTypeCF.thisType
            if (subType == ObjectType("class_types/SomeEnumeration")) {
                foundSomeEnumerationClass = true
                false
            } else
                true
        }(jvmFeaturesProject)

        foundSomeEnumerationClass should be(true)
    }

}

object ClassHierarchyTest {

    val generics = locateTestResources("generictypes.jar", "bi")
    val genericProject = Project(ClassFiles(generics), Iterable.empty, true)

}
