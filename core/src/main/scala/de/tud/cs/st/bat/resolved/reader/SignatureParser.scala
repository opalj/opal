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
package de.tud.cs.st
package bat
package resolved
package reader

import scala.util.parsing.combinator._

// TODO [Improvement] consider making the signature parser abstract and use the factory pattern as in the case of all other major structures

/**
 * Parses Java class file signature strings.
 *
 * ==Thread-Safety==
 * Using this object is thread-safe.
 */
object SignatureParser {

    /**
     * Parses Java class file signature strings.
     *
     * ==Thread-Safety==
     * As of Scala 2.10 classes that inherit from `(Regex)Parsers` are not thread-safe.
     * However, the only class that can create instances of a `SignatureParsers` is
     * its companion object and that one implements the necessary abstractions for the
     * thread-safe use of `SignatureParsers`.
     *
     * @author Michael Eichberg
     */
    // TODO [Scala 2.11 - Improvement] investigate if Combinator Parsers are now thread-safe; if so the wrapper object which currently implements the necessary logic for thread safety can be removed 
    class SignatureParsers private[SignatureParser] () extends RegexParsers {

        def parseClassSignature(signature: String): ClassSignature = {
            parseAll(classSignatureParser, signature).get
        }

        def parseFieldTypeSignature(signature: String): FieldTypeSignature = {
            parseAll(fieldTypeSignatureParser, signature).get
        }

        def parseMethodTypeSignature(signature: String): MethodTypeSignature = {
            parseAll(methodTypeSignatureParser, signature).get
        }

        //
        // The methods to parse signatures. The methods which create the parsers
        // start with an underscore to make them easily distinguishable from
        // the data structure they parse/create.
        //

        protected val classSignatureParser: Parser[ClassSignature] =
            opt(formalTypeParametersParser) ~
                superclassSignatureParser ~
                rep(superinterfaceSignatureParser) ^^ {
                    case ftps ~ scs ~ siss ⇒ ClassSignature(ftps, scs, siss)
                }

        protected val fieldTypeSignatureParser: Parser[FieldTypeSignature] =
            classTypeSignatureParser |
                typeVariableSignatureParser |
                arrayTypeSignatureParser

        protected val methodTypeSignatureParser: Parser[MethodTypeSignature] =
            opt(formalTypeParametersParser) ~
                ('(' ~> rep(typeSignatureParser) <~ ')') ~
                returnTypeParser ~
                rep(throwsSignatureParser) ^^ {
                    case ftps ~ psts ~ rt ~ tss ⇒ MethodTypeSignature(ftps, psts, rt, tss)
                }

        protected val identifierParser: Parser[String] = """[^.;\[\]/\<>\:]*+""".r

        protected def formalTypeParametersParser: Parser[List[FormalTypeParameter]] =
            '<' ~> rep1(formalTypeParameterParser) <~ '>'

        protected def formalTypeParameterParser: Parser[FormalTypeParameter] =
            identifierParser ~ classBoundParser ~ opt(interfaceBoundParser) ^^ {
                case id ~ cb ~ ib ⇒ FormalTypeParameter(id, cb, ib)
            }

        protected def classBoundParser: Parser[Option[FieldTypeSignature]] =
            ':' ~> opt(fieldTypeSignatureParser)

        protected def interfaceBoundParser: Parser[FieldTypeSignature] =
            ':' ~> fieldTypeSignatureParser

        protected def superclassSignatureParser: Parser[ClassTypeSignature] =
            classTypeSignatureParser

        protected def superinterfaceSignatureParser: Parser[ClassTypeSignature] =
            classTypeSignatureParser

        /**
         * '''From the JVM Specification'''
         *
         * A class type signature gives complete type information for a class or
         * interface type. The class type signature must be formulated such that
         * it can be reliably mapped to the binary name of the class it denotes
         * by erasing any type arguments and converting each ‘.’ character in
         * the signature to a ‘$’ character.
         */
        protected def classTypeSignatureParser: Parser[ClassTypeSignature] =
            'L' ~>
                opt(packageSpecifierParser) ~
                simpleClassTypeSignatureParser ~
                rep(classTypeSignatureSuffixParser) <~ ';' ^^ {
                    case ps ~ scts ~ ctsss ⇒ ClassTypeSignature(ps, scts, ctsss)
                }

        protected def packageSpecifierParser: Parser[String] =
            (identifierParser ~ ('/' ~> opt(packageSpecifierParser))) ^^ {
                case id ~ rest ⇒ id+"/"+rest.getOrElse("")
            }

        protected def simpleClassTypeSignatureParser: Parser[SimpleClassTypeSignature] =
            identifierParser ~ opt(typeArgumentsParser) ^^ {
                case id ~ tas ⇒ SimpleClassTypeSignature(id, tas)
            }

        protected def classTypeSignatureSuffixParser: Parser[SimpleClassTypeSignature] =
            '.' ~> simpleClassTypeSignatureParser

        protected def typeVariableSignatureParser: Parser[TypeVariableSignature] =
            ('T' ~> identifierParser <~ ';') ^^ { TypeVariableSignature(_) }

        protected def typeArgumentsParser: Parser[List[TypeArgument]] =
            '<' ~> rep1(typeArgumentParser) <~ '>'

        protected def typeArgumentParser: Parser[TypeArgument] =
            (opt(wildcardIndicatorParser) ~ fieldTypeSignatureParser) ^^ {
                case wi ~ fts ⇒ ProperTypeArgument(wi, fts)
            } |
                ('*' ^^ { _ ⇒ Wildcard })

        protected def wildcardIndicatorParser: Parser[VarianceIndicator] =
            // Conceptually, we do the following:
            // '+' ^^ { _ ⇒ CovariantIndicator } | '-' ^^ { _ ⇒ ContravariantIndicator }
            new Parser[VarianceIndicator] {
                def apply(in: Input): ParseResult[VarianceIndicator] = {
                    if (in.atEnd) Failure("signature incomplete", in);
                    else (in.first: @scala.annotation.switch) match {
                        case '+' ⇒ Success(CovariantIndicator, in.rest)
                        case '-' ⇒ Success(ContravariantIndicator, in.rest)
                        case x   ⇒ Failure("unknown wildcard indicator", in.rest)
                    }
                }
            }

        protected def arrayTypeSignatureParser: Parser[ArrayTypeSignature] =
            '[' ~> typeSignatureParser ^^ { ArrayTypeSignature(_) }

        protected def typeSignatureParser: Parser[TypeSignature] =
            fieldTypeSignatureParser | baseTypeParser

        protected def throwsSignatureParser: Parser[ThrowsSignature] =
            '^' ~> (classTypeSignatureParser | typeVariableSignatureParser)

        protected def baseTypeParser: Parser[BaseType] =
            // This is what is conceptually done: 
            // 'B' ^^ (_ ⇒ ByteType) | 'C' ^^ (_ ⇒ CharType) | 'D' ^^ (_ ⇒ DoubleType) | 
            // 'F' ^^ (_ ⇒ FloatType) | 'I' ^^ (_ ⇒ IntegerType) | 'J' ^^ (_ ⇒ LongType) | 
            // 'S' ^^ (_ ⇒ ShortType) | 'Z' ^^ (_ ⇒ BooleanType)
            // This is a way more efficient implementation:
            new Parser[BaseType] {
                def apply(in: Input): ParseResult[BaseType] = {
                    if (in.atEnd) {
                        Failure("signature is incomplete, base type identifier expected", in);
                    } else {
                        (in.first: @scala.annotation.switch) match {
                            case 'B' ⇒ Success(ByteType, in.rest)
                            case 'C' ⇒ Success(CharType, in.rest)
                            case 'D' ⇒ Success(DoubleType, in.rest)
                            case 'F' ⇒ Success(FloatType, in.rest)
                            case 'I' ⇒ Success(IntegerType, in.rest)
                            case 'J' ⇒ Success(LongType, in.rest)
                            case 'S' ⇒ Success(ShortType, in.rest)
                            case 'Z' ⇒ Success(BooleanType, in.rest)
                            case x   ⇒ Failure("unknown base type identifier: "+x, in.rest)
                        }
                    }
                }
            }

        protected def returnTypeParser: Parser[ReturnTypeSignature] =
            typeSignatureParser | 'V' ^^ (_ ⇒ VoidType)
    }

    private def createSignatureParsers() = new SignatureParsers()

    private val signatureParsers: ThreadLocal[SignatureParsers] =
        new ThreadLocal[SignatureParsers] {
            override protected def initialValue() = createSignatureParsers()
        }

    def parseClassSignature(signature: String): ClassSignature = {
        signatureParsers.get.parseClassSignature(signature)
    }

    def parseFieldTypeSignature(signature: String): FieldTypeSignature = {
        signatureParsers.get.parseFieldTypeSignature(signature)
    }

    def parseMethodTypeSignature(signature: String): MethodTypeSignature = {
        signatureParsers.get.parseMethodTypeSignature(signature)
    }
}



