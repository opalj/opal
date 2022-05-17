/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package collection
package mutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.log.GlobalLogContext
import org.opalj.br.analyses.Project

/**
 * Basic tests of the TypesSet class.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TypesSetTest extends AnyFlatSpec with Matchers {

    //
    // Setup
    //
    val jlsCHFile = "ClassHierarchyJLS.ths"
    val jlsCHCreator = List(() => classOf[Project[_]].getResourceAsStream(jlsCHFile))
    val jlsCH = ClassHierarchy(Iterable.empty, jlsCHCreator)(GlobalLogContext)

    val preInitCH = ClassHierarchy.PreInitializedClassHierarchy

    val javaLangCHFile = "JavaLangClassHierarchy.ths"
    val javaLangCHCreator = List(() => classOf[Project[_]].getResourceAsStream(javaLangCHFile))
    val javaLangCH = ClassHierarchy(Iterable.empty, javaLangCHCreator)(GlobalLogContext)

    val Object = ObjectType.Object
    val Class = ObjectType.Class
    val Throwable = ObjectType.Throwable
    val Exception = ObjectType.Exception
    val Error = ObjectType.Error
    val RuntimeException = ObjectType.RuntimeException
    val ArithmeticException = ObjectType.ArithmeticException
    val Cloneable = ObjectType.Cloneable
    val Serializable = ObjectType.Serializable
    val AnUnknownType = ObjectType("myTest/AnUnknownType")

    //
    // Verify
    //

    behavior of "the TypesSet"

    it should "be empty empty upon creation" in {
        new TypesSet(jlsCH) should be(Symbol("Empty"))
    }

    behavior of "the add type method +="

    it should "add the type if it is not stored in the set" in {
        val ts = new TypesSet(jlsCH)
        ts += Cloneable
        ts += Serializable
        assert(ts.size === 2)
        assert(ts.nonEmpty)
        ts.types should be((Set(Cloneable, Serializable), Set.empty))
    }

    it should "add the type if it is not stored in the set and only subtypes are added as upper type bounds" in {
        val ts = new TypesSet(jlsCH)
        ts +<:= Class
        ts += Serializable
        assert(ts.size === 2)
        assert(ts.nonEmpty)
        ts.types should be((Set(Serializable), Set(Class)))
    }

    it should "not add the type if the type is already added as an upper type bound" in {
        val ts = new TypesSet(jlsCH)
        ts +<:= Cloneable
        ts += Serializable
        ts += Cloneable
        assert(ts.size === 2)
        assert(ts.nonEmpty)
        ts.types should be((Set(Serializable), Set(Cloneable)))
    }

    it should "not add a type if a super type is already added as an upper type bound" in {
        val ts = new TypesSet(jlsCH)
        ts +<:= Object
        ts += Object
        ts += Cloneable
        assert(ts.size === 1)
        assert(ts.nonEmpty)
        ts.types should be((Set.empty, Set(Object)))
    }

    behavior of "the add upper type bound method +<:="

    it should "not remove unrelated concrete types if a super type of some types is added as an upper type bound" in {
        val ts = new TypesSet(jlsCH)
        ts += Cloneable
        ts += Class
        ts +<:= Serializable
        assert(ts.size === 2)
        assert(ts.nonEmpty)
        ts.types should be((Set(Cloneable), Set(Serializable)))
    }

    it should "remove the type if the type is (later) added as an upper type bound" in {
        val ts = new TypesSet(jlsCH)
        ts += Cloneable
        ts += Serializable
        ts +<:= Cloneable
        assert(ts.size === 2)
        assert(ts.nonEmpty)
        ts.types should be((Set(Serializable), Set(Cloneable)))
    }

    it should "remove all concrete types if a super type is added as an upper type bound" in {
        val ts = new TypesSet(jlsCH)
        ts += Cloneable
        ts += Serializable
        ts +<:= Object
        assert(ts.size === 1)
        assert(ts.nonEmpty)
        ts.types should be((Set.empty, Set(Object)))
    }

    it should "add the type if it is not stored in the set" in {
        val ts = new TypesSet(jlsCH)
        ts +<:= Cloneable
        ts +<:= Serializable
        assert(ts.size === 2)
        assert(ts.nonEmpty)
        ts.types should be((Set.empty, Set(Cloneable, Serializable)))
    }

    it should "not a add the type if a super type is already in the set as an upper type bound" in {
        val ts = new TypesSet(jlsCH)
        ts +<:= Serializable
        ts +<:= Class
        assert(ts.size === 1)
        assert(ts.nonEmpty)
        ts.types should be((Set.empty, Set(Serializable)))
    }

    it should "replace a type if a super type is added" in {
        val ts = new TypesSet(jlsCH)
        ts +<:= Class
        ts +<:= Serializable
        assert(ts.size === 1)
        assert(ts.nonEmpty)
        ts.types should be((Set.empty, Set(Serializable)))
    }
}
