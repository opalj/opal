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
package org.opalj
package br

/**
 * An element of a [[Signature]] used to encode generic type information.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
trait SignatureElement { // RECALL: Void and the BaseTypes are also subtypes!

    def accept[T](sv: SignatureVisitor[T]): T

    /**
     * Converts this signature into its JVM representation. (See the JVM 5 or later
     * specification for further details.)
     */
    def toJVMSignature: String

}

trait ReturnTypeSignature extends SignatureElement

trait TypeSignature extends ReturnTypeSignature

sealed trait ThrowsSignature extends SignatureElement

/**
 * An attribute-level signature as defined in the JVM specification.
 *
 * To match `Signature` objects the predefined matchers/extractors can be used.
 * * @example
 * ==Example 1==
 * {{{
 * interface Processor extends Function<Object, Void> { /*empty*/ }
 * }}}
 * '''ClassSignature''':
 * `Ljava/lang/Object;Ljava/util/function/Function<Ljava/lang/Object;Ljava/lang/Void;>;`
 * {{{
 *  ClassSignature(
 *    typeParameters=List(),
 *    superClass=ClassTypeSignature(Some(java/lang/),SimpleClassTypeSignature(Object,List()),List()),
 *    superInterfaces=List(
 *        ClassTypeSignature(
 *            Some(java/util/function/),
 *            SimpleClassTypeSignature(Function,
 *                  List(ProperTypeArgument(
 *                         None,
 *                         ClassTypeSignature(
 *                            Some(java/lang/),
 *                            SimpleClassTypeSignature(Object,List()),
 *                            List())),
 *                     ProperTypeArgument(
 *                        None,
 *                        ClassTypeSignature(
 *                            Some(java/lang/),
 *                            SimpleClassTypeSignature(Void,List()),
 *                            List())))),
 *            List())))
 * }}}
 *
 * ==Example 2==
 * {{{
 * interface Col<C> { /*empty*/ }
 * }}}
 * '''ClassSignature''':
 * `<C:Ljava/lang/Object;>Ljava/lang/Object;`
 * {{{
 *  ClassSignature(
 *      typeParameters=
 *          List(
 *              FormalTypeParameter(
 *                  C,
 *                  Some(ClassTypeSignature(
 *                          Some(java/lang/),SimpleClassTypeSignature(Object,List()),List())),
 *                  List())),
 *       superClass=ClassTypeSignature(
 *              Some(java/lang/),SimpleClassTypeSignature(Object,List()),List()),
 *       superInterfaces=List())
 * }}}
 *
 * ==Example 3==
 * {{{
 * interface ColObject extends Col<Object> { /*empty*/ }
 * }}}
 * '''ClassSignature''':
 * `Ljava/lang/Object;LCol<Ljava/lang/Object;>;`
 * {{{
 *  ClassSignature(
 *      typeParameters=List(),
 *      superClass=ClassTypeSignature(Some(java/lang/),SimpleClassTypeSignature(Object,List()),List()),
 *      superInterfaces=List(
 *              ClassTypeSignature(
 *                  None,
 *                  SimpleClassTypeSignature(
 *                      Col,
 *                      List(ProperTypeArgument(
 *                              variance=None,
 *                              signature=ClassTypeSignature(Some(java/lang/),SimpleClassTypeSignature(Object,List()),List()))
 *                   )),
 *                   List())))
 * }}}
 *
 * ==Example 4==
 * {{{
 * interface ColError<E extends Error> extends Col<E>{/*empty*/}
 * }}}
 * '''ClassSignature''':
 * `<E:Ljava/lang/Error;>Ljava/lang/Object;LCol<TE;>;`
 * {{{
 *  ClassSignature(
 *      typeParameters=List(
 *              FormalTypeParameter(
 *                  E,
 *                  Some(ClassTypeSignature(Some(java/lang/),SimpleClassTypeSignature(Error,List()),List())),List())),
 *      superClass=ClassTypeSignature(Some(java/lang/),SimpleClassTypeSignature(Object,List()),List()),
 *      superInterfaces=List(
 *              ClassTypeSignature(
 *                  None,
 *                  SimpleClassTypeSignature(
 *                      Col,
 *                      List(ProperTypeArgument(variance=None,signature=TypeVariableSignature(E)))),
 *                  List())))
 * }}}
 *
 * ==Example 5==
 * {{{
 * class Use {
 *   // The following fields all have "ClassTypeSignatures"
 *   Col<?> ce = null; // Signature: LCol<*>;
 *   Col<Object> co = null; // Signature: LCol<Ljava/lang/Object;>;
 *   Col<? super Serializable> cs = null; // Signature: LCol<-Ljava/io/Serializable;>;
 *   Col<? extends Comparable<?>> cc = null; // Signature: LCol<+Ljava/lang/Comparable<*>;>;
 *
 *   MyCol<java.util.List<Object>> mco = new MyCol<>();
 *   MyCol<java.util.List<Object>>.MyInnerCol<Comparable<java.util.List<Object>>> mico = this.mco.new MyInnerCol<Comparable<java.util.List<Object>>>();
 *   // Signature: LMyCol<Ljava/util/List<Ljava/lang/Object;>;>.MyInnerCol<Ljava/lang/Comparable<Ljava/util/List<Ljava/lang/Object;>;>;>;
 * }
 * }}}
 *
 * AST of `mico`:
 * {{{
 *  ClassSignature(
 *      typeParameters=List(),
 *      superClass=ClassTypeSignature(
 *              None,
 *              SimpleClassTypeSignature(
 *                  MyCol,
 *                  List(ProperTypeArgument(
 *                          variance=None,
 *                          signature=ClassTypeSignature(
 *                                  Some(java/util/),
 *                                  SimpleClassTypeSignature(
 *                                      List,
 *                                      List(ProperTypeArgument(
 *                                              variance=None,
 *                                              signature=ClassTypeSignature(
 *                                                      Some(java/lang/),
 *                                                      SimpleClassTypeSignature(Object,List()),
 *                                                      List())))),
 *                                   List())))),
 *              /*suffic=*/List(SimpleClassTypeSignature(
 *                      MyInnerCol,
 *                      List(ProperTypeArgument(
 *                              variance=None,
 *                              signature=ClassTypeSignature(
 *                                  Some(java/lang/),
 *                                  SimpleClassTypeSignature(
 *                                      Comparable,
 *                                      List(ProperTypeArgument(
 *                                              variance=None,
 *                                              signature=ClassTypeSignature(
 *                                                      Some(java/util/),
 *                                                      SimpleClassTypeSignature(
 *                                                          List,
 *                                                          List(ProperTypeArgument(
 *                                                                  variance=None,
 *                                                                  signature=ClassTypeSignature(
 *                                                                          Some(java/lang/),
 *                                                                          SimpleClassTypeSignature(Object,List()),
 *                                                                          List())))),
 *                                                      List())))),
 *                                  List())))))),
 *      superInterfaces=List())
 * }}}
 *
 * ==Matching Signatures==
 * '''Scala REPL''':
 * {{{
 * val SignatureParser = org.opalj.br.reader.SignatureParser
 * val GenericType = org.opalj.br.GenericType
 * val SimpleGenericType = org.opalj.br.SimpleGenericType
 * val BasicClassTypeSignature = org.opalj.br.BasicClassTypeSignature
 *
 * SignatureParser.parseClassSignature("<E:Ljava/lang/Error;>Ljava/lang/Object;LCol<TE;>;").superInterfacesSignature.head match { case BasicClassTypeSignature(ot) => ot.toJava; case _ => null}
 * // res: String = Col
 *
 * SignatureParser.parseClassSignature("<E:Ljava/lang/Error;>Ljava/lang/Object;LCol<TE;>;").superInterfacesSignature.head match { case SimpleGenericType(bt,gt) => bt.toJava+"<"+gt.toJava+">"; case _ => null}
 * //res11: String = null
 *
 * scala> SignatureParser.parseFieldTypeSignature("LCol<Ljava/lang/Object;>;") match { case SimpleGenericType(bt,ta) => bt.toJava+"<"+ta+">"; case _ => null}
 * res1: String = Col<ObjectType(java/lang/Object)>
 *
 * scala> SignatureParser.parseFieldTypeSignature("LCol<Ljava/lang/Object;>;") match { case GenericType(bt,ta) => bt.toJava+"<"+ta+">"; case _ => null}
 * res2: String = Col<List(ProperTypeArgument(variance=None,signature=ClassTypeSignature(Some(java/lang/),SimpleClassTypeSignature(Object,List()),List())))>
 * }}}
 */
sealed abstract class Signature extends SignatureElement with Attribute {

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

}

object Signature {

    private[br] def formalTypeParametersToJVMSignature(
        formalTypeParameters: List[FormalTypeParameter]
    ): String = {
        if (formalTypeParameters.isEmpty) {
            ""
        } else {
            formalTypeParameters.map(_.toJVMSignature).mkString("<", "", ">")
        }
    }

    def unapply(s: Signature): Some[String] = Some(s.toJVMSignature)

}
import Signature.formalTypeParametersToJVMSignature

/**
 * @see For matching signatures see [[Signature]].
 */
case class ClassSignature(
        formalTypeParameters:     List[FormalTypeParameter],
        superClassSignature:      ClassTypeSignature,
        superInterfacesSignature: List[ClassTypeSignature]
) extends Signature {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def kindId: Int = ClassSignature.KindId

    override def toJVMSignature: String = {
        formalTypeParametersToJVMSignature(formalTypeParameters) +
            superClassSignature.toJVMSignature +
            superInterfacesSignature.map(_.toJVMSignature).mkString("")
    }

    override def toString: String = {
        "ClassSignature("+
            formalTypeParameters.mkString("typeParameters=List(", ",", ")")+
            ",superClass="+superClassSignature.toString +
            superInterfacesSignature.mkString(",superInterfaces=List(", ",", "))")
    }
}
object ClassSignature {

    final val KindId = 12

}

/**
 * @see For matching signatures see [[Signature]].
 */
case class MethodTypeSignature(
        formalTypeParameters:     List[FormalTypeParameter],
        parametersTypeSignatures: List[TypeSignature],
        returnTypeSignature:      ReturnTypeSignature,
        throwsSignature:          List[ThrowsSignature]
) extends Signature {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def kindId: Int = MethodTypeSignature.KindId

    override def toJVMSignature: String =
        formalTypeParametersToJVMSignature(formalTypeParameters) +
            parametersTypeSignatures.map(_.toJVMSignature).mkString("(", "", ")") +
            returnTypeSignature.toJVMSignature +
            throwsSignature.map('^' + _.toJVMSignature).mkString("")
}
object MethodTypeSignature {

    final val KindId = 13

}

/**
 * @see For matching signatures see [[Signature]].
 */
sealed trait FieldTypeSignature extends Signature with TypeSignature

object FieldTypeSignature {

    def unapply(signature: AnyRef): Boolean = signature.isInstanceOf[FieldTypeSignature]

}

object FieldTypeJVMSignature {

    def unapply(signature: FieldTypeSignature): Some[String] = Some(signature.toJVMSignature)

}

/**
 * @see For matching signatures see [[Signature]].
 */
case class ArrayTypeSignature(typeSignature: TypeSignature) extends FieldTypeSignature {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def kindId: Int = ArrayTypeSignature.KindId

    override def toJVMSignature: String = "["+typeSignature.toJVMSignature
}
object ArrayTypeSignature {

    final val KindId = 14

}

/**
 * @see For matching signatures see [[Signature]].
 */
case class ClassTypeSignature(
        packageIdentifier:        Option[String],
        simpleClassTypeSignature: SimpleClassTypeSignature,
        classTypeSignatureSuffix: List[SimpleClassTypeSignature]
) extends FieldTypeSignature with ThrowsSignature {

    def objectType: ObjectType = {
        val className =
            if (packageIdentifier.isDefined)
                new java.lang.StringBuilder(packageIdentifier.get)
            else
                new java.lang.StringBuilder()
        className.append(simpleClassTypeSignature.simpleName)
        classTypeSignatureSuffix foreach { ctss ⇒
            className.append('$')
            className.append(ctss.simpleName)
        }

        ObjectType(className.toString)
    }

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)

    override def kindId: Int = ClassTypeSignature.KindId

    override def toJVMSignature: String = {
        val packageName = packageIdentifier.getOrElse("")

        "L"+
            packageName +
            simpleClassTypeSignature.toJVMSignature +
            (classTypeSignatureSuffix match {
                case Nil ⇒ ""
                case l   ⇒ l.map(_.toJVMSignature).mkString(".", ".", "")
            })+
            ";"
    }

}
object ClassTypeSignature {

    final val KindId = 15

}

/**
 * @see For matching signatures see [[Signature]].
 */
case class TypeVariableSignature(
        identifier: String
) extends FieldTypeSignature with ThrowsSignature {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def kindId: Int = TypeVariableSignature.KindId

    override def toJVMSignature: String = "T"+identifier+";"

}
object TypeVariableSignature {

    final val KindId = 16

}

/**
 * @see For matching signatures see [[Signature]].
 */
case class SimpleClassTypeSignature(
        simpleName:    String,
        typeArguments: List[TypeArgument]
) {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    def toJVMSignature: String = {
        simpleName +
            (typeArguments match {
                case Nil ⇒ ""
                case l   ⇒ l.map(_.toJVMSignature).mkString("<", "", ">")
            })
    }
}

/**
 * @see For matching signatures see [[Signature]].
 */
case class FormalTypeParameter(
        identifier:     String,
        classBound:     Option[FieldTypeSignature],
        interfaceBound: List[FieldTypeSignature]
) {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    def toJVMSignature: String = {
        identifier +
            (classBound match {
                case Some(x) ⇒ ":"+x.toJVMSignature
                case None    ⇒ ":"
            }) +
            (interfaceBound match {
                case Nil ⇒ ""
                case l   ⇒ ":"+l.map(_.toJVMSignature).mkString(":")
            })
    }
}

/**
 * @see For matching signatures see [[Signature]].
 */
sealed abstract class TypeArgument extends SignatureElement

/**
 * @see For matching signatures see [[Signature]].
 */
case class ProperTypeArgument(
        varianceIndicator:  Option[VarianceIndicator],
        fieldTypeSignature: FieldTypeSignature
) extends TypeArgument {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJVMSignature: String = {
        (varianceIndicator match {
            case Some(x) ⇒ x.toJVMSignature
            case None    ⇒ ""
        }) +
            fieldTypeSignature.toJVMSignature
    }

    override def toString: String = {
        "ProperTypeArgument"+
            "(variance="+varianceIndicator+
            ",signature="+fieldTypeSignature+
            ")"
    }
}

/**
 * Indicates a TypeArgument's variance.
 */
sealed abstract class VarianceIndicator extends SignatureElement

/**
 * If you have a declaration such as &lt;? extends Entry&gt; then the "? extends" part
 * is represented by the `CovariantIndicator`.
 *
 * @see For matching signatures see [[Signature]].
 */
sealed abstract class CovariantIndicator extends VarianceIndicator {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJVMSignature: String = "+"

}
case object CovariantIndicator extends CovariantIndicator

/**
 * A declaration such as <? super Entry> is represented in class file signatures
 * by the ContravariantIndicator ("? super") and a FieldTypeSignature.
 *
 * @see For matching signatures see [[Signature]].
 */
sealed abstract class ContravariantIndicator extends VarianceIndicator {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJVMSignature: String = "-"

}
case object ContravariantIndicator extends ContravariantIndicator

/**
 * If a type argument is not further specified (e.g., List<?> l = …), then the
 * type argument "?" is represented by this object.
 *
 *
 * @see For matching signatures see [[Signature]].
 */
sealed abstract class Wildcard extends TypeArgument {

    def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJVMSignature: String = "*"

}
case object Wildcard extends Wildcard

/**
 * Extractor/Matcher of the (potentially erased) `ObjectType` that is defined by a
 * `ClassTypeSignature`; ignores all further potential type parameters.
 *
 * @see For matching signatures see [[Signature]].
 */
object BasicClassTypeSignature {

    def unapply(cts: ClassTypeSignature): Option[ObjectType] = {
        Some(cts.objectType)
    }
}

/**
 * Matches a [[ClassTypeSignature]] with a [[SimpleClassTypeSignature]] that does not define
 * a generic type. For example, `java.lang.Object`.
 *
 * @see For matching signatures see [[Signature]].
 */
object ConcreteType {

    def unapply(cts: ClassTypeSignature): Option[ObjectType] = {
        cts match {

            case ClassTypeSignature(cpn, SimpleClassTypeSignature(csn, Nil), Nil) ⇒
                Some(ObjectType(cpn.getOrElse("") + csn))

            case _ ⇒
                None
        }
    }
}

/**
 * Facilitates matching [[ProperTypeArgument]]s that define a single concrete/invariant
 * type that is not a generic type on its own. E.g., it can be used to match the
 * type argument of `List<Integer>` and to extract the concrete type `Integer`. It
 * cannot be used to match, e.g., `List<List<Integer>>`.
 *
 * @see For matching signatures see [[Signature]].
 */
object ConcreteTypeArgument {

    def unapply(pta: ProperTypeArgument): Option[ObjectType] = {
        pta match {
            case ProperTypeArgument(None, ConcreteType(ot)) ⇒ Some(ot)
            case _                                          ⇒ None
        }
    }
}

/**
 * Facilitates matching [[ProperTypeArgument]]s that define an upper type bound. E.g.,
 * a type bound which uses a CovarianceIndicator (`? extends`) such as in
 * `List<? extends Number>`.
 *
 * @example
 * {{{
 *  val scts : SimpleClassTypeSignature = ...
 *  scts.typeArguments.head match {
 *      case UpperTypeBound(objectType) => ...
 *      case _ => ...
 *  }
 * }}}
 *
 *
 * @see For matching signatures see [[Signature]].
 */
object UpperTypeBound {

    def unapply(pta: ProperTypeArgument): Option[ObjectType] = pta match {
        case ProperTypeArgument(Some(CovariantIndicator), ConcreteType(ot)) ⇒ Some(ot)
        case _ ⇒ None
    }
}

/**
 * Facilitates matching [[ProperTypeArgument]]s that define a lower type bound. E.g.,
 * a type bound which uses a ContravarianceIndicator (`? super`) such as in
 * `List<? super Number>`.
 *
 * @example
 *  matches, e.g., `List<? super Integer>`
 *  {{{
 *  val scts : SimpleClassTypeSignature = ...
 *  scts.typeArguments.head match {
 *      case LowerTypeBound(objectType) => ...
 *      case _ => ...
 *  }
 * }}}
 *
 * @see For matching signatures see [[Signature]].
 */
object LowerTypeBound {

    def unapply(pta: ProperTypeArgument): Option[ObjectType] = pta match {
        case ProperTypeArgument(Some(ContravariantIndicator), ConcreteType(ot)) ⇒ Some(ot)
        case _ ⇒ None
    }
}

/**
 * Facilitates matching the (`VarianceIndicator`, `ObjectType`) that is defined
 * within a `ProperTypeArgument`. It matches ProperTypeArguments which define
 * `TypeArgument`s in the inner ClassTypeSignature.
 *
 * @example
 *      matches e.g.: `List<List<Integer>>`
 * {{{
 *  val scts : SimpleClassTypeSignature = ...
 *  scts.typeArguments match {
 *      case GenericTypeArgument(varInd, objectType) => ...
 *      case _ => ...
 *  }
 * }}}
 *
 * @see For matching signatures see [[Signature]].
 */
object GenericTypeArgument {

    def unapply(
        pta: ProperTypeArgument
    ): Option[(Option[VarianceIndicator], ClassTypeSignature)] = {
        pta match {
            case ProperTypeArgument(variance, cts: ClassTypeSignature) ⇒ Some((variance, cts))
            case _ ⇒ None
        }
    }
}

/**
 * Matches all [[ClassTypeSignature]]s which consists of
 * a [[SimpleClassTypeSignature]] with a non-empty List of TypeArguments (
 * which consists of [[Wildcard]]s or [[ProperTypeArgument]]s)
 *
 * @see For matching signatures see [[Signature]].
 */
object GenericType {

    def unapply(cts: ClassTypeSignature): Option[(ObjectType, List[TypeArgument])] = {
        cts match {

            case ClassTypeSignature(
                _,
                SimpleClassTypeSignature(_, typeArgs),
                Nil) if typeArgs.nonEmpty ⇒
                Some((cts.objectType, typeArgs))

            case _ ⇒
                None
        }
    }
}

/**
 * Matches all [[ClassTypeSignature]]s which consists of
 * a [[SimpleClassTypeSignature]] with an optional list of TypeArguments (
 * which consists of [[Wildcard]]s or [[ProperTypeArgument]]s) and a non-empty list of
 * [[SimpleClassTypeSignature]] (which encodes the suffix of the [[ClassTypeSignature]] for
 * inner classes)
 *
 * @see For matching signatures see [[Signature]].
 */
object GenericTypeWithClassSuffix {

    def unapply(
        cts: ClassTypeSignature
    ): Option[(ObjectType, List[TypeArgument], List[SimpleClassTypeSignature])] = {
        cts match {

            case ClassTypeSignature(
                _,
                SimpleClassTypeSignature(_, typeArgs),
                suffix) if suffix.nonEmpty ⇒
                Some((cts.objectType, typeArgs, suffix))

            case _ ⇒
                None
        }
    }
}

/**
 * Facilitates matching [[ClassTypeSignature]]s that define a simple generic type that
 * has a single type argument with a concrete type.
 *
 * @example
 * The following can be used to match, e.g., `List<Object>`.
 * {{{
 *  val f : Field = ...
 *  f.fieldTypeSignature match {
 *      case SimpleGenericType(ContainerType,ElementType) => ...
 *      case _ => ...
 *  }
 * }}}
 *
 * @author Michael Eichberg
 */
object SimpleGenericType {

    def unapply(cts: ClassTypeSignature): Option[(ObjectType, ObjectType)] = {
        cts match {

            case ClassTypeSignature(
                cpn,
                SimpleClassTypeSignature(
                    csn,
                    List(ProperTypeArgument(None, ConcreteType(tp)))),
                Nil
                ) ⇒
                Some((ObjectType(cpn.getOrElse("") + csn), tp))

            case _ ⇒
                None
        }
    }
}
