/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org

import scala.language.experimental.macros

import java.io.InputStream
import java.io.Closeable

import scala.reflect.ClassTag
import scala.reflect.api.Trees
import scala.reflect.macros.blackbox.Context

/**
 * OPAL is a Scala-based framework for the static analysis of Java bytecode
 * that is designed with performance, scalability and adaptability in mind.
 *
 * Its main components are:
 *  - a library for reading in Java bytecode and creating arbitrary representations
 *  - an easily customizable framework for the abstract interpretation of Java bytecode.
 *  - a library to extract dependencies between code elements.
 *
 * ==General Design Decisions==
 *
 * ===Thread Safety===
 * Unless explicitly noted, '''OPAL is thread safe'''. I.e., it is possible to read
 * and process class files concurrently without explicit synchronization on the
 * client side.
 *
 * ===No `null` Values===
 * OPAL generally does not make use of `null` values in its public interface. I.e.,
 * fields that are accessible  will never contain `null` values and methods will never
 * return `null`. '''If a method accepts `null` as a value for a parameter or returns
 * a `null` value it is always explicitly documented.'''
 * In general, the behavior of methods that are passed `null` values is undefined.
 *
 * ===No Typecasts for Collections===
 * For efficiency reasons OPAL sometimes uses mutable data-structures during construction
 * time. After construction time, these data-structures are generally represented using
 * their generic interfaces (e.g., `scala.collection.{Set,Map}`). However, a downcast
 * (e.g., to add/remove elements) is always forbidden as it would effectively prevent the
 * thread-safety properties. Furthermore, the concrete data-structure is always
 * considered an implementation detail and may change at any time.
 *
 * @author Michael Eichberg
 */
package object opalj {

    final val WEBPAGE = "http://www.opal-project.de"

    /**
     * A simple type alias that can be used to communicate that the respective
     * value will only take values in the range of unsigned short values.
     */
    type UShort = Int

    /**
     * A program counter identifies an instruction in the code array.
     *
     * A program counter is a value in the range `[0/*UShort.min*/, 65535/*UShort.max*/]`
     */
    type PC = UShort

    /**
     * A collection of program counters using a UShortSet as its backing collection.
     *
     * Using PCs is in particular well suited for small(er) collections.
     *
     * @note This type alias serves comprehension purposes only.
     */
    type PCs = collection.UShortSet

    /**
     * This function takes a `Closeable` resource and a function `r` that will
     * process the `Closeable` resource.
     * This function takes care of the correct handling of `Closeable` resources.
     * When `r` has finished processing the resource or throws an exception, the
     * resource is closed.
     *
     * @note If `closable` is `null`, `null` is passed to `r`.
     *
     * @param closable The `Closeable` resource.
     * @param r The function that processes the `resource`.
     */
    def process[C <: Closeable, T](closable: C)(r: C ⇒ T): T = {
        // Implementation Note
        // Creating the closeable (I) in the try block doesn't make sense, hence
        // we don't need a by-name parameter.
        try {
            r(closable)
        } finally {
            if (closable != null) closable.close()
        }
    }

    /**
     * This function takes a `Source` and a function `r` that will
     * process the source.
     * This function takes care of the correct handling of resources.
     * When `r` has finished processing the source or throws an exception,
     * the source is closed.
     *
     * @note If `source` is `null`, `null` is passed to `r`.
     */
    def processSource[C <: scala.io.Source, T](source: C)(r: C ⇒ T): T = {
        try {
            r(source)
        } finally {
            if (source != null) source.close()
        }
    }

    /**
     * Iterates over a given array `a` and calls the given function `f` for
     * each non-null value in the array.
     *
     * '''This is a macro.'''
     */
    def foreachNonNullValueOf[T <: AnyRef](
        a: Array[T])(
            f: (Int, T) ⇒ Unit): Unit = macro ControlAbstractionsImplementation.foreachNonNullValueOf[T]

    /**
     * Converts a given bit mask using an `Int` value into a bit mask using a `Long` value.
     *
     * @note This is not the same as a type conversion as the "sign-bit" is not treated
     *      as such. I.e., after conversion of the `Int` value -1, the `Long` value
     *      will be `4294967295` (both have the same bit mask:
     *      `11111111111111111111111111111111`).
     */
    final def i2lBitMask(value: Int): Long = {
        (value >>> 16).toLong << 16 | (value & 0xFFFF).toLong
    }

    /**
     * Returns the package definitions shared by both fully qualified type names.
     * If both types do not define a common package `None` is returned.
     *
     * @example
     * {{{
     * scala> org.opalj.commonPackage("a.b.T","a.c.T")
     * res: Option[String] = Some(a.)
     *
     * scala> org.opalj.commonPackage("a.b.T","a.T")
     * res: Option[String] = Some(a.)
     *
     * scala> org.opalj.commonPackage("a.b.T","a.b.T")
     * res: Option[String] = Some(a.b.)
     *
     * scala> org.opalj.commonPackage("c.b.T","a.T")
     * res: Option[String] = None
     *
     * scala> org.opalj.commonPackage("a.b.T","d.c.T")
     * res: Option[String] = None
     * }}}
     */
    def commonPackage(
        fqnA: String, fqnB: String,
        packageSeperator: Char = '.'): Option[String] = {
        val packageSeperatorIndex = fqnA.indexOf(packageSeperator) + 1
        if (packageSeperatorIndex <= 0)
            return None;

        val rootPackage = fqnA.substring(0, packageSeperatorIndex)
        if (packageSeperatorIndex == fqnB.indexOf(packageSeperator) + 1 &&
            rootPackage == fqnB.substring(0, packageSeperatorIndex)) {
            commonPackage(
                fqnA.substring(packageSeperatorIndex, fqnA.length()),
                fqnB.substring(packageSeperatorIndex, fqnB.length())
            ) match {
                    case Some(childPackage) ⇒ Some(rootPackage + childPackage)
                    case None               ⇒ Some(rootPackage)
                }
        } else {
            None
        }
    }

    /**
     * Abbreviates the given `memberTypeFQN` by abbreviating the common packages
     * (except of the last shared package) of both fully qualified type nams using '…'.
     *
     * @example
     * {{{
     * scala> org.opalj.abbreviateFQN("a.b.T","a.T") // <= no abbrev.
     * res: String = a.T
     *
     * scala> org.opalj.abbreviateFQN("a.b.T","a.b.T")
     * res: String = …b.T
     *
     * scala> org.opalj.abbreviateFQN("a.b.c.T","a.b.c.T.X")
     * res: String = …c.T.X
     * }}}
     */
    def abbreviateFQN(
        definingTypeFQN: String, memberTypeFQN: String,
        packageSeperator: Char = '.'): String = {

        commonPackage(definingTypeFQN, memberTypeFQN) match {
            case Some(commonPackages) if commonPackages.indexOf(packageSeperator) < commonPackages.length - 1 ⇒
                // we have more than one common package...
                val beforeLastCommonPackageIndex = commonPackages.dropRight(1).lastIndexOf('.')
                val length = memberTypeFQN.length
                val packagesCount = commonPackages.count(_ == '.') - 1
                val packageAbbreviation = "." * packagesCount
                packageAbbreviation +
                    memberTypeFQN.substring(beforeLastCommonPackageIndex + 1, length)
            case _ ⇒
                memberTypeFQN
        }
    }

    /**
     * Evaluates the given expression `f` with type `T` the given number of
     * `times` and stores the result in an `IndexedSeq[T]`.
     *
     * ==Example Usage==
     * {{{
     * val result = repeat(15) {
     *      System.in.read()
     * }
     * }}}
     *
     * '''This is a macro.'''
     *
     * @param times The number of times the expression `f` is evaluated. The `times`
     *      expression is evaluated exactly once.
     * @param f An expression that is evaluated the given number of times unless an
     *      exception is thrown. Hence, even though `f` is not a by-name parameter,
     *      it behaves in the same way.
     * @return The result of the evaluation of the expression `f` the given number of
     *      times stored in an `IndexedSeq`. If `times` is zero an empty sequence is
     *      returned.
     */
    def repeat[T](times: Int)(f: T): IndexedSeq[T] = macro ControlAbstractionsImplementation.repeat[T]
    // OLD IMPLEMENTATION USING HIGHER-ORDER FUNCTIONS
    // (DO NOT DELETE - TO DOCUMENT THE DESIGN DECISION FOR MACROS)
    //        def repeat[T](times: Int)(f: ⇒ T): IndexedSeq[T] = {
    //            val array = new scala.collection.mutable.ArrayBuffer[T](times)
    //            var i = 0
    //            while (i < times) {
    //                array += f
    //                i += 1
    //            }
    //            array
    //        }
    // The macro-based implementation has proven to be approx. 1,3 to 1,4 times faster 
    // when the number of times that we repeat an operation is small (e.g., 1 to 15 times)  
    // (which is very often the case when we read in Java class files)

}

/**
 * Implementation of the macros.
 *
 * @author Michael Eichberg
 */
private object ControlAbstractionsImplementation {

    def foreachNonNullValueOf[T <: AnyRef: c.WeakTypeTag](
        c: Context)(
            a: c.Expr[Array[T]])(
                f: c.Expr[(Int, T) ⇒ Unit]): c.Expr[Unit] = {
        import c.universe._

        reify {
            val array = a.splice // evaluate only once!
            val arrayLength = array.length
            var i = 0
            while (i < arrayLength) {
                val arrayEntry = array(i)
                if (arrayEntry ne null) f.splice(i, arrayEntry)
                i += 1
            }
        }
    }

    def repeat[T: c.WeakTypeTag](
        c: Context)(
            times: c.Expr[Int])(
                f: c.Expr[T]): c.Expr[IndexedSeq[T]] = {
        import c.universe._

        reify {
            val size = times.splice // => times is evaluated only once
            if (size == 0) {
                IndexedSeq.empty
            } else {
                val array = new scala.collection.mutable.ArrayBuffer[T](size)
                var i = 0
                while (i < size) {
                    val value = f.splice // => we evaluate f the given number of times
                    array += value
                    i += 1
                }
                array
            }
        }
    }
}