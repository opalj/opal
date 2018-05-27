/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger

/**
 * OPAL is a Scala-based framework for the static analysis, manipulation and creation of
 * Java bytecode. OPAL is designed with performance, scalability and adaptability in mind.
 *
 * Its main components are:
 *  - a library (`Common`) which provides generally useful data-structures and algorithms
 *    for static analyses.
 *  - a framework for implementing lattice based static analyses (`Static Analysis Infrastructure`)
 *  - a framework for parsing Java bytecode (Bytecode Infrastructure`) that can be used to
 *    create arbitrary representations.
 *  - a library to create a one-to-one in-memory representation of Java bytecode
 *    (`Bytecode Disassembler`).
 *  - a library to create a representation of Java bytecode that facilitates writing
 *    simple static analyses (`Bytecode Representation` - [[org.opalj.br]]).
 *  - a scalable, easily customizable framework for the abstract interpretation of
 *    Java bytecode (`Abstract Interpretation Framework` - [[org.opalj.ai]]).
 *  - a library to extract dependencies between code elements and to facilitate checking
 *    architecture definitions.
 *  - a library for the lightweight manipulation and creation of Java bytecode (Bytecode Assembler).
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
 * (e.g., to add/remove elements) is always forbidden as it would effectively prevent
 * thread-safety. Furthermore, the concrete data-structure is always
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

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext = GlobalLogContext
        import OPALLogger.info
        try {
            assert(false)
            // when we reach this point assertions are turned off
            info("OPAL Common", "Production Build")
        } catch {
            case _: AssertionError ⇒ info("OPAL Common", "Development Build with Assertions")
        }
    }

    val BaseConfig: Config = ConfigFactory.load(this.getClass.getClassLoader())

    /** Non-elidable version of `assert`; only to be used in a guarded context. */
    def check(condition: Boolean) = {
        if (!condition) throw new AssertionError();
    }

    /** Non-elidable version of `assert`; only to be used in a guarded context. */
    def check(condition: Boolean, message: ⇒ String) = {
        if (!condition) throw new AssertionError(message);
    }

    /**
     * The URL of the webpage of the opal project.
     */
    final val WEBPAGE = "http://www.opal-project.de"

    /**
     * The type of a project.
     */
    final type ProjectType = ProjectTypes.Value

    /**
     * The type of the predefined relational operators.
     *
     * See [[org.opalj.RelationalOperators]] for the list of all defined operators.
     */
    final type RelationalOperator = RelationalOperators.Value

    /**
     * The type of the predefined binary arithmetic operators.
     *
     * See [[org.opalj.BinaryArithmeticOperators]] for the list of all defined operators.
     */
    final type BinaryArithmeticOperator = BinaryArithmeticOperators.Value

    /**
     * The type of the predefined unary arithmetic operators.
     *
     * See [[org.opalj.UnaryArithmeticOperators]] for the list of all defined operators.
     */
    final type UnaryArithmeticOperator = UnaryArithmeticOperators.Value

    /**
     * A simple type alias that can be used to communicate that the respective
     * value will/should only take values in the range of unsigned short values.
     */
    final type UShort = Int

    /**
     * A simple type alias that can be used to communicate that the respective
     * value will/should only take values in the range of unsigned byte values.
     */
    final type UByte = Int

    /**
     * Converts a given bit mask using an `Int` value into a bit mask using a `Long` value.
     *
     * @note This is not the same as a type conversion as the "sign-bit" is not treated
     *      as such. I.e., after conversion of the `Int` value -1, the `Long` value
     *      will be `4294967295` (both have the same bit mask:
     *      `11111111111111111111111111111111`); in other words, the long's sign bit will
     *      still be `0`.
     */
    @inline final def i2lBitMask(value: Int): Long = {
        (value >>> 16).toLong << 16 | (value & 0xFFFF).toLong
    }

    final def notRequired(): Nothing = {
        throw new UnknownError("providing an implementation was not expected to be required")
    }

    /**
     * A method that takes an arbitrary parameter and throws an `UnknownError` that states
     * that an implementation was not required.
     */
    final val NotRequired: Any ⇒ Nothing = (a: Any) ⇒ { notRequired() }

}
