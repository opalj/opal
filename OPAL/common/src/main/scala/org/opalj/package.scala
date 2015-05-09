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
import scala.reflect.ClassTag
import scala.reflect.api.Trees
import scala.reflect.macros.blackbox.Context
import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger

/**
 * OPAL is a Scala-based framework for the static analysis of Java bytecode
 * that is designed with performance, scalability and adaptability in mind.
 *
 * Its main components are:
 *  - a library (`Common`) that defines functionality that is generally useful
 *    in the context of static analyses.
 *  - a library for parsing Java bytecode (`Bytecode Infrastructure`).
 *  - a library to create a one-to-one in-memory representation of Java bytecode
 *    (`Bytecode Disassembler`).
 *  - a library to create a representation of Java bytecode that facilitates writing
 *    simple static analyses.
 *  - a scalable, easily customizable framework for the abstract interpretation of
 *    Java bytecode.
 *  - a library to extract dependencies between code elements.
 *
 * ==General Design Decisions==
 *
 * ===Thread Safety===
 * Unless explicitly noted, '''OPAL is thread safe'''. I.e., the classes defined by
 * OPAL can be considered to be thread safe unless otherwise stated.
 * (For example, it is possible to read and process class files concurrently without
 * explicit synchronization on the client side.)
 *
 * ===No `null` Values===
 * Unless explicitly noted, '''OPAL does not `null` values'''
 * I.e., fields that are accessible will never contain `null` values and methods will
 * never return `null`.  '''If a method accepts `null` as a value for a parameter or
 * returns a `null` value it is always explicitly documented.'''
 * In general, the behavior of methods that are passed `null` values is undefined unless
 * explicitly documented.
 *
 * ===No Typecasts for Collections===
 * For efficiency reasons, OPAL sometimes uses mutable data-structures internally.
 * After construction time, these data-structures are generally represented using
 * their generic interfaces (e.g., `scala.collection.{Set,Map}`). However, a downcast
 * (e.g., to add/remove elements) is always forbidden as it would effectively prevent the
 * thread-safety properties. Furthermore, the concrete data-structure is always
 * considered an implementation detail and may change at any time.
 *
 * ===Assertions===
 * OPAL makes heavy use of Scala's '''Assertion Facility''' to facilitate writing correct
 * code. Hence, for production builds (after thorough testing(!)) it is
 * highly recommend to build OPAL again using `-Xdisable-assertions`.
 *
 * @author Michael Eichberg
 */
package object opalj {

    type RelationalOperator = RelationalOperators.Value

    type BinaryArithmeticOperator = BinaryArithmeticOperators.Value

    type UnaryArithmeticOperator = UnaryArithmeticOperators.Value

    {
        implicit val logContext = GlobalLogContext
        try {
            scala.Predef.assert(false)
            OPALLogger.info("OPAL", "Common - Production Build")
        } catch {
            case ae: AssertionError ⇒
                OPALLogger.info("OPAL", "Common - Assertions are enabled - Development Build.")
        }
    }

    final val WEBPAGE = "http://www.opal-project.de"

    /**
     * A simple type alias that can be used to communicate that the respective
     * value will/should only take values in the range of unsigned short values.
     */
    final type UShort = Int

    /**
     * Iterates over a given array `a` and calls the given function `f` for
     * each non-null value in the array.
     *
     * @note '''This is a macro.'''
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
     *      `11111111111111111111111111111111`); in other words, the long's sign bit will
     *      still be `0`.
     */
    final def i2lBitMask(value: Int): Long = {
        (value >>> 16).toLong << 16 | (value & 0xFFFF).toLong
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
