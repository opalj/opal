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
package org.opalj
package br
package reader

import org.scalatest.FunSuite
import org.scalatest.ParallelTestExecution

/**
 * Tests the parsing of signatures.
 *
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SignaturesTest extends FunSuite with ParallelTestExecution {

    import SignatureParser.parseClassSignature

    test("traversing a minimal class type signature") {
        var types: Set[Type] = Set()
        val visitor = new TypesVisitor(t ⇒ { types = types + t })
        visitor.visit(parseClassSignature("<E:Ljava/Object;>Lde/Iterator<TE;>;"))

        assert(types == Set(ObjectType("java/Object"), ObjectType("de/Iterator")))
    }

    test("traversing a class type signature") {
        var types: Set[Type] = Set()
        val visitor = new TypesVisitor(t ⇒ { types = types + t })
        visitor.visit(parseClassSignature("LDefault;Lde/Collection<Lde/Type;>;LAnotherDefault;Lde/MyObject;"))

        assert(
            types == Set(
                ObjectType("Default"),
                ObjectType("de/Collection"),
                ObjectType("de/Type"),
                ObjectType("AnotherDefault"),
                ObjectType("de/MyObject")))
    }

    import Java8Framework.ClassFile
    private val classA = ClassFile(TestSupport.locateTestResources("classfiles/Signatures.jar","bi"), "signatures/A.class")
    assert(classA ne null)

    private val classB = ClassFile(TestSupport.locateTestResources("classfiles/Signatures.jar","bi"), "signatures/B.class")
    assert(classB ne null)

    test("parsing the class signatures") {
        val classASignature = classA.classSignature.get
        assert(classASignature ne null)

        val classBSignature = classB.classSignature.get
        assert(classBSignature ne null)
    }

    test("parsing the field type signatures") {
        classA.fields.foreach(x ⇒
            x match {
                case Field(_, "b", _) ⇒ {
                    val signature = x.fieldTypeSignature;
                    assert(signature ne null)
                }
                case Field(_, "bs", _) ⇒ {
                    val signature = x.fieldTypeSignature
                    assert(signature ne null)
                }
                case _ ⇒ ;
            }
        )
    }

    //
    // BRUTE FORCE TESTS (WE ARE JUST TRYING TO PARSE A VERY LARGE NUMBER OF SIGNATURES)
    //

    test("parse various class file signatures in parallel") {
        def parse(s: String) {
            val r = SignatureParser.parseClassSignature(s)
            assert(r ne null)
        }
        ClassFileSignatures.par.foreach(parse _)
    }

    test("parse various field type signatures in parallel") {
        def parse(s: String) {
            val r = SignatureParser.parseFieldTypeSignature(s)
            assert(r ne null)
        }
        FieldTypeSignatures.par.foreach(parse _)
    }

    test("parse various method type signatures in parallel") {
        def parse(s: String) {
            val r = SignatureParser.parseMethodTypeSignature(s)
            assert(r ne null)
        }
        MethodTypeSignatures.par.foreach(parse _)
    }

    //
    // DATA
    //
    // The following signatures are just thrown to the parser as is.
    //

    val ClassFileSignatures: Array[String] =
        """
	<A:Ljava/lang/Object;>Ljava/lang/Object;Lde/tud/cs/st/util/collection/Store<TA;>;Lscala/ScalaObject;
	<A:Ljava/lang/Object;>Ljava/lang/Object;Lde/tud/cs/st/util/collection/WorkList<TA;>;Lscala/ScalaObject;
	<A:Ljava/lang/Object;>Ljava/lang/Object;Lscala/Iterable<TA;>;Lscala/ScalaObject;
	<A:Ljava/lang/Object;>Ljava/lang/Object;Lscala/ScalaObject;
	<E:Ljava/lang/Object;>Lde/tud/bat/util/SimpleListIterator<TE;>;
	<E:Ljava/lang/Object;>Ljava/lang/Object;Lde/tud/bat/util/BATIterator<TE;>;
	<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Iterator<TE;>;
	<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Iterator<TE;>;Ljava/lang/Iterable<TE;>;
	<T::Lde/tud/cs/st/sae/GroundTerm;>Lde/tud/cs/st/sae/GroundTerms<TT;>;Lscala/ScalaObject;
	<T::Lde/tud/cs/st/sae/GroundTerm;>Ljava/lang/Object;Lde/tud/cs/st/sae/GroundTerm;Lscala/ScalaObject;
	<T::Lde/tud/cs/st/sae/ValueAtom;>Ljava/lang/Object;Lde/tud/cs/st/bat/prolog/ValueTerm;Lscala/ScalaObject;
	<T:Ljava/lang/Object;>Ljava/lang/Object;Lde/tud/bat/classfile/structure/MemberValueConstant<TT;>;
	<T:Ljava/lang/Object;>Ljava/lang/Object;Lscala/ScalaObject;
	<T:Ljava/lang/Object;>Ljava/util/ArrayList<TT;>;
	<This::Lde/tud/cs/st/util/OrderedTreeSetElement<TThis;>;>Ljava/lang/Object;Lscala/ScalaObject;
	Lde/tud/cs/st/sae/GroundTerms<Lscala/Nothing;>;Lscala/ScalaObject;
	Lde/tud/cs/st/sae/GroundTerms<TT;>;
	Ljava/lang/Enum<Lde/tud/bat/io/xml/writer/XMLClassFileWriterConfiguration$DocumentType;>;
	Ljava/lang/Enum<Lde/tud/bat/quadruples/Use$RefType;>;
	Ljava/lang/Enum<Lde/tud/cs/se/flashcards/model/learning/LearningStrategies;>;
	Ljava/lang/Object;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/ClassFile;>;
	Ljava/lang/Object;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/ExceptionHandler;>;
	Ljava/lang/Object;Lde/tud/bat/util/BATIterator<Lde/tud/bat/instruction/Instruction;>;
	Ljava/lang/Object;Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/Type;>;
	Ljava/lang/Object;Lde/tud/bat/util/BATIterator<Ljava/lang/String;>;
	Ljava/lang/Object;Lde/tud/cs/st/bat/prolog/SimpleValueTerm<Lde/tud/cs/st/sae/FloatAtom;>;Lde/tud/cs/st/bat/prolog/FieldValueTerm;Lscala/ScalaObject;
	Ljava/lang/Object;Lde/tud/cs/st/bat/prolog/SimpleValueTerm<Lde/tud/cs/st/sae/IntegerAtom;>;Lde/tud/cs/st/bat/prolog/FieldValueTerm;Lscala/ScalaObject;
	Ljava/lang/Object;Lde/tud/cs/st/bat/prolog/SimpleValueTerm<Lde/tud/cs/st/sae/IntegerAtom;>;Lscala/ScalaObject;
	Ljava/lang/Object;Lde/tud/cs/st/bat/prolog/SimpleValueTerm<Lde/tud/cs/st/sae/StringAtom;>;Lde/tud/cs/st/bat/prolog/FieldValueTerm;Lscala/ScalaObject;
	Ljava/lang/Object;Lde/tud/cs/st/bat/prolog/SimpleValueTerm<Lde/tud/cs/st/sae/TruthValueAtom;>;Lscala/ScalaObject;
	Ljava/lang/Object;Lde/tud/cs/st/bat/resolved/ConstantValue<Lde/tud/cs/st/bat/resolved/ReferenceType;>;Lscala/ScalaObject;Lscala/Product;
	Ljava/lang/Object;Lde/tud/cs/st/bat/resolved/ConstantValue<Ljava/lang/Double;>;Lscala/ScalaObject;Lscala/Product;
	Ljava/lang/Object;Lde/tud/cs/st/bat/resolved/ConstantValue<Ljava/lang/Float;>;Lscala/ScalaObject;Lscala/Product;
	Ljava/lang/Object;Lde/tud/cs/st/bat/resolved/ConstantValue<Ljava/lang/Integer;>;Lscala/ScalaObject;Lscala/Product;
	Ljava/lang/Object;Lde/tud/cs/st/bat/resolved/ConstantValue<Ljava/lang/Long;>;Lscala/ScalaObject;Lscala/Product;
	Ljava/lang/Object;Lde/tud/cs/st/bat/resolved/ConstantValue<Ljava/lang/String;>;Lscala/ScalaObject;Lscala/Product;
	Ljava/lang/Object;Lde/tud/cs/st/util/collection/Store<Lscala/Nothing;>;Lscala/ScalaObject;
	Ljava/lang/Object;Lde/tud/cs/st/util/OrderedTreeSetElement<Lde/tud/cs/st/util/OrderedTreeSet;>;Lscala/ScalaObject;
	Ljava/lang/Object;Ljava/util/Comparator<Ljava/lang/Integer;>;
	Ljava/lang/Object;Ljava/util/Enumeration<Lde/tud/cs/st/columbus/ServiceSpecification;>;
	Ljava/lang/Object;Lscala/Iterator<Lde/tud/cs/st/bat/native/AccessFlag;>;Lscala/ScalaObject;
	Ljava/lang/Object;Lscala/Iterator<TA;>;
	""".trim.split("\n").map(_.trim())

    val FieldTypeSignatures: Array[String] =
        """
	Lde/tud/bat/instruction/executiongraph/StackLayout$ShrinkableArrayList<Lde/tud/bat/type/ValueType;>;
	Lde/tud/cs/st/bat/resolved/ConstantValue<*>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ElementValuePairTerm;>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ExceptionTableEntryTerm;>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/KeyValueTerm;>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/LineNumberTableEntryTerm;>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ValueTerm;>;
	Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/sae/IntegerAtom;>;
	Lde/tud/cs/st/sae/GroundTerms<TT;>;
	Lde/tud/cs/st/util/collection/LinkedListWorkList<TA;>.Element<TA;>;
	Lde/tud/cs/st/util/collection/Store<TA;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Annotation;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Attribute;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/ExceptionHandler;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Field;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/InnerClass;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/LocalVariable;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/MemberValue;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/MemberValuePair;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Method;>;
	Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/ParameterAnnotation;>;
	Ljava/util/ArrayList<Lde/tud/bat/instruction/executiongraph/BasicBlock;>;
	Ljava/util/ArrayList<Lde/tud/bat/instruction/executiongraph/CatchBlock;>;
	Ljava/util/ArrayList<Lde/tud/bat/quadruples/ExceptionHandler;>;
	Ljava/util/ArrayList<Lde/tud/bat/quadruples/MethodParameter;>;
	Ljava/util/ArrayList<Lde/tud/bat/quadruples/Statement;>;
	Ljava/util/ArrayList<Lde/tud/bat/quadruples/Switch$Case;>;
	Ljava/util/ArrayList<Lde/tud/bat/type/FormalTypeParameter;>;
	Ljava/util/ArrayList<Lde/tud/bat/type/ObjectType;>;
	Ljava/util/ArrayList<Lde/tud/cs/se/flashcards/model/Flashcard;>;
	Ljava/util/ArrayList<Ljava/lang/Integer;>;
	Ljava/util/ArrayList<Ljava/lang/String;>;
	Ljava/util/ArrayList<Ljava/util/ArrayList<Lde/tud/cs/se/flashcards/model/Flashcard;>;>;
	Ljava/util/ArrayList<Lorg/jdom/Namespace;>;
	Ljava/util/Comparator<Ljava/lang/Integer;>;
	Ljava/util/HashMap<Lde/tud/bat/type/ReferenceType;Ljava/lang/Integer;>;
	Ljava/util/HashMap<Ljava/lang/Object;Ljava/lang/Integer;>;
	Ljava/util/HashMap<Ljava/lang/String;Lde/tud/bat/type/ObjectType;>;
	Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Integer;>;
	Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Object;>;
	Ljava/util/Hashtable<Lde/tud/bat/instruction/executiongraph/BasicBlock;Ljava/lang/String;>;
	Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/String;>;
	Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/util/Vector<Lorg/jdom/Element;>;>;
	Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Lorg/jdom/Element;>;
	Ljava/util/Hashtable<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;>;
	Ljava/util/Hashtable<Lde/tud/bat/quadruples/CodeElement;Ljava/lang/String;>;
	Ljava/util/Hashtable<Lde/tud/bat/quadruples/CodeElement;Lorg/jdom/Element;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/classfile/structure/Attribute;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/classfile/structure/ClassFile;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/instruction/Instruction;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/io/reader/AttributeReader;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/io/xml/reader/exceptionhandler/CatchBlock;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/io/xml/reader/exceptionhandler/TryBlock;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/io/xml/reader/instruction/InstructionReader;>;
	Ljava/util/Hashtable<Ljava/lang/String;Ljava/lang/Integer;>;
	Ljava/util/Hashtable<Ljava/lang/String;Ljava/util/List<Lde/tud/bat/instruction/JumpTarget;>;>;
	Ljava/util/Hashtable<Ljava/lang/String;Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;>;
	Ljava/util/Hashtable<Ljava/lang/String;Lorg/jdom/Document;>;
	Ljava/util/Hashtable<Lorg/jdom/Element;Lde/tud/bat/io/xml/reader/instructionlayout/InstructionLayout;>;
	Ljava/util/Hashtable<Lorg/jdom/Namespace;Ljava/lang/Integer;>;
	Ljava/util/Iterator<Lde/tud/cs/st/columbus/Bundle;>;
	Ljava/util/Iterator<Lde/tud/cs/st/columbus/ServiceSpecification;>;
	Ljava/util/LinkedList<Lde/tud/bat/classfile/structure/ClassFile;>;
	Ljava/util/LinkedList<Lde/tud/cs/se/flashcards/model/FlashcardObserver;>;
	Ljava/util/List<Lalice/tuprolog/event/WarningEvent;>;
	Ljava/util/List<Lde/michaeleichberg/multihtreadedprogramming/v2Beta4Thread/Calculation;>;
	Ljava/util/List<Lde/tud/bat/io/constantPool/ConstantPoolEntry;>;
	Ljava/util/List<Lde/tud/bat/io/xml/reader/exceptionhandler/CatchBlock;>;
	Ljava/util/List<Lde/tud/bat/io/xml/reader/exceptionhandler/TryBlock;>;
	Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/BasicBlock;>;
	Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;
	Ljava/util/List<Lde/tud/cs/se/flashcards/model/Flashcard;>;
	Ljava/util/List<Lde/tud/cs/st/columbus/ProvidedService;>;
	Ljava/util/List<Lde/tud/cs/st/columbus/RequiredService;>;
	Ljava/util/List<Lorg/apache/commons/vfs/FileObject;>;
	Ljava/util/List<Lorg/jdom/Namespace;>;
	Ljava/util/List<TE;>;
	Ljava/util/Map<Ljava/lang/String;Lde/tud/bat/io/writer/AttributeWriter;>;
	Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;
	Ljava/util/Set<Lde/tud/cs/st/columbus/Bundle;>;
	Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;
	Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;
	Ljava/util/Set<Ljava/lang/ClassLoader;>;
	Ljava/util/Stack<Lde/tud/bat/classfile/impl/CodeImpl$Code_JumpTarget;>;
	Ljava/util/Stack<Lde/tud/bat/io/xml/reader/exceptionhandler/TryBlock;>;
	Ljava/util/Stack<Lde/tud/bat/type/Type;>;
	Ljava/util/Stack<Ljavax/swing/event/ListDataEvent;>;
	Ljava/util/Vector<Lde/tud/bat/classfile/impl/CodeImpl$Code_JumpTarget;>;
	Ljava/util/Vector<Lde/tud/bat/type/Type;>;
	Ljava/util/Vector<Lde/tud/cs/se/flashcards/model/Command;>;
	Lscala/collection/immutable/Map<Ljava/lang/String;Lscala/collection/immutable/Set<Ljava/lang/String;>;>;
	Lscala/collection/immutable/Map<Ljava/lang/String;Lscala/Tuple2<Ljava/util/zip/ZipFile;Ljava/util/zip/ZipEntry;>;>;
	Lscala/collection/immutable/Set<Ljava/lang/String;>;
	Lscala/collection/jcl/WeakHashMap<Ljava/lang/Object;Ljava/lang/String;>;
	Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/FieldRefTerm;Lde/tud/cs/st/bat/prolog/FieldRefTerm;>;
	Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/MethodDescriptorTerm;Lde/tud/cs/st/bat/prolog/MethodDescriptorTerm;>;
	Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/MethodRefTerm;Lde/tud/cs/st/bat/prolog/MethodRefTerm;>;
	Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/TypeTerm;Lde/tud/cs/st/bat/prolog/ArrayTypeTerm;>;
	Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/resolved/FieldType;Lde/tud/cs/st/bat/resolved/ArrayType;>;
	Lscala/collection/mutable/Map<Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;>;
	Lscala/collection/mutable/Map<Ljava/lang/String;Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;
	Lscala/collection/mutable/Map<Ljava/lang/String;Lde/tud/cs/st/bat/resolved/ObjectType;>;
	Lscala/collection/mutable/Map<Lscala/Tuple2<Ljava/lang/Object;Ljava/lang/Integer;>;Lscala/List<Lde/tud/cs/st/sae/Fact;>;>;
	Lscala/collection/mutable/Map<Lscala/Tuple2<Ljava/lang/Object;Ljava/lang/Integer;>;Lscala/List<Lde/tud/cs/st/sae/Rule;>;>;
	Lscala/Function1<Lde/tud/cs/st/prolog/ISOProlog;Ljava/lang/Object;>;
	Lscala/List<Lde/tud/cs/st/bat/resolved/ObjectType;>;
	Lscala/List<Lde/tud/cs/st/sae/Fact;>;
	Lscala/List<Lde/tud/cs/st/sae/Query;>;
	Lscala/List<Lde/tud/cs/st/sae/Rule;>;
	Lscala/List<[Lde/tud/cs/st/sae/Term;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/AccessFlag;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ElementValuePair;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ExceptionTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Field_Info;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$InnerClassesEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LineNumberTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTypeTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Method_Info;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapFrame;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ElementValue;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ElementValuePair;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ExceptionTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Field_Info;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LineNumberTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LocalVariableTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LocalVariableTypeTableEntry;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Method_Info;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/StackMapFrame;>;
	Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;
	Lscala/RandomAccessSeq<Ljava/lang/Integer;>;
	Lscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;>;
	Lscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;>;
	Lscala/RandomAccessSeq<Lscala/Tuple2<Ljava/lang/Integer;Ljava/lang/Integer;>;>;
	Lscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;
	Lscala/Seq<Lde/tud/cs/st/bat/native/ElementValue;>;
	Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;
	Lscala/Seq<Lde/tud/cs/st/bat/resolved/FieldType;>;
	Lscala/Seq<Lde/tud/cs/st/bat/resolved/ObjectType;>;
	Lscala/Seq<Lde/tud/cs/st/sae/GroundTerm;>;
	Lscala/Seq<Lde/tud/cs/st/sae/Term;>;
	TA;
	TT;
	[TE;
  	""".trim.split("\n").map(_.trim())

    val MethodTypeSignatures: Array[String] =
        """
	()Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/ParameterAnnotation;>;
	()Lde/tud/bat/util/BATIterator<Lde/tud/bat/instruction/Instruction;>;
	()Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/FormalTypeParameter;>;
	()Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/ObjectType;>;
	()Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/Type;>;
	()Lde/tud/bat/util/BATIterator<Ljava/lang/String;>;
	()Lde/tud/cs/st/bat/resolved/ConstantValue<*>;
	()Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;
	()Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ValueTerm;>;
	()Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/sae/IntegerAtom;>;
	()Lde/tud/cs/st/sae/GroundTerms<TT;>;
	()Lde/tud/cs/st/util/collection/LinkedListWorkList<TA;>.Element<TA;>;
	()Lde/tud/cs/st/util/collection/Store<TA;>;
	()Ljava/lang/Class<*>;
	()Ljava/lang/Class<Ljava/lang/Short;>;
	()Ljava/lang/Class<Ljava/lang/Void;>;
	()Ljava/lang/Iterable<Lde/tud/bat/quadruples/Statement;>;
	()Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Annotation;>;
	()Ljava/util/ArrayList<Lde/tud/bat/quadruples/Statement;>;
	()Ljava/util/Collection<Lde/tud/bat/quadruples/Statement;>;
	()Ljava/util/Enumeration<Lde/tud/cs/st/columbus/ServiceSpecification;>;
	()Ljava/util/Enumeration<Lorg/jdom/Document;>;
	()Ljava/util/Hashtable<Ljava/lang/String;Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;
	()Ljava/util/Hashtable<Ljava/lang/String;Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;>;
	()Ljava/util/Hashtable<Lorg/jdom/Element;Lde/tud/bat/io/xml/reader/instructionlayout/InstructionLayout;>;
	()Ljava/util/Iterator<Lde/tud/bat/io/constantPool/ConstantPoolEntry;>;
	()Ljava/util/Iterator<Lde/tud/bat/type/Type;>;
	()Ljava/util/Iterator<Ljava/lang/String;>;
	()Ljava/util/Iterator<TE;>;
	()Ljava/util/List<Lalice/tuprolog/event/WarningEvent;>;
	()Ljava/util/List<Lde/tud/bat/classfile/structure/LocalVariable;>;
	()Ljava/util/List<Lde/tud/bat/instruction/executiongraph/BasicBlock;>;
	()Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;
	()Ljava/util/List<Lde/tud/bat/quadruples/Statement;>;
	()Ljava/util/List<Lde/tud/cs/st/columbus/ProvidedService;>;
	()Ljava/util/List<Lde/tud/cs/st/columbus/RequiredService;>;
	()Ljava/util/List<Lorg/apache/commons/vfs/FileObject;>;
	()Ljava/util/Map<Lde/tud/bat/instruction/executiongraph/BasicBlock;Ljava/lang/String;>;
	()Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;
	()Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;
	()Ljava/util/Stack<Lde/tud/bat/type/Type;>;
	()Ljava/util/Vector<Lde/tud/bat/type/Type;>;
	()Lscala/collection/immutable/Map<Ljava/lang/String;Lscala/collection/immutable/Set<Ljava/lang/String;>;>;
	()Lscala/collection/immutable/Map<Ljava/lang/String;Lscala/Tuple2<Ljava/util/zip/ZipFile;Ljava/util/zip/ZipEntry;>;>;
	()Lscala/collection/immutable/Set<Ljava/lang/String;>;
	()Lscala/collection/jcl/WeakHashMap<Ljava/lang/Object;Ljava/lang/String;>;
	()Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/FieldRefTerm;Lde/tud/cs/st/bat/prolog/FieldRefTerm;>;
	()Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/MethodDescriptorTerm;Lde/tud/cs/st/bat/prolog/MethodDescriptorTerm;>;
	()Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/MethodRefTerm;Lde/tud/cs/st/bat/prolog/MethodRefTerm;>;
	()Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/prolog/TypeTerm;Lde/tud/cs/st/bat/prolog/ArrayTypeTerm;>;
	()Lscala/collection/mutable/Map<Lde/tud/cs/st/bat/resolved/FieldType;Lde/tud/cs/st/bat/resolved/ArrayType;>;
	()Lscala/collection/mutable/Map<Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;>;
	()Lscala/collection/mutable/Map<Ljava/lang/String;Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;
	()Lscala/collection/mutable/Map<Ljava/lang/String;Lde/tud/cs/st/bat/resolved/ObjectType;>;
	()Lscala/collection/mutable/Map<Lscala/Tuple2<Ljava/lang/Object;Ljava/lang/Integer;>;Lscala/List<Lde/tud/cs/st/sae/Fact;>;>;
	()Lscala/collection/mutable/Map<Lscala/Tuple2<Ljava/lang/Object;Ljava/lang/Integer;>;Lscala/List<Lde/tud/cs/st/sae/Rule;>;>;
	()Lscala/Collection<Lde/tud/cs/st/util/trees/TreeNode;>;
	()Lscala/Function1<Lde/tud/cs/st/prolog/ISOProlog;Ljava/lang/Object;>;
	()Lscala/Function3<Ljava/io/DataInputStream;Lscala/RandomAccessSeq<TA;>;Ljava/lang/Integer;Ljava/lang/Object;>;
	()Lscala/Iterator<Lscala/Enumeration$Value;>;
	()Lscala/Iterator<TA;>;
	()Lscala/List<Lde/tud/cs/st/bat/resolved/ObjectType;>;
	()Lscala/List<Lde/tud/cs/st/sae/Fact;>;
	()Lscala/List<Lde/tud/cs/st/sae/Query;>;
	()Lscala/List<Lde/tud/cs/st/sae/Rule;>;
	()Lscala/List<Lscala/Nothing;>;
	()Lscala/List<[Lde/tud/cs/st/sae/Term;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/AccessFlag;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapFrame;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Method_Info;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/StackMapFrame;>;
	()Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;
	()Lscala/RandomAccessSeq<Ljava/lang/Integer;>;
	()Lscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;>;
	()Lscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;>;
	()Lscala/RandomAccessSeq<Lscala/Tuple2<Ljava/lang/Integer;Ljava/lang/Integer;>;>;
	()Lscala/Seq<Lde/tud/cs/st/bat/resolved/ObjectType;>;
	()Lscala/Seq<Lde/tud/cs/st/sae/Fact;>;
	()Lscala/Seq<Lde/tud/cs/st/sae/GroundTerm;>;
	()Lscala/Seq<Lde/tud/cs/st/sae/Term;>;
	()Lscala/Seq<Ljava/lang/Integer;>;
	()Lscala/Seq<Ljava/lang/Object;>;
	()Lscala/Seq<Lscala/xml/Elem;>;
	()Lscala/Tuple2<Ljava/lang/Integer;Ljava/lang/Integer;>;
	()Lscala/Tuple2<TA;Lde/tud/cs/st/util/collection/Store<TA;>;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Atom;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Fact;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/FloatAtom;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/GroundTerm;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/sae/GroundTerm;>;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/GroundTerms<Lscala/Nothing;>;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/parser/Program;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Rule;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Term;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Variable;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Ljava/lang/Float;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Ljava/lang/Long;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Ljava/lang/Object;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Ljava/lang/String;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lscala/List<Lde/tud/cs/st/sae/GroundTerm;>;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lscala/List<Lde/tud/cs/st/sae/Term;>;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<Lscala/List<TT;>;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<[Lde/tud/cs/st/sae/GroundTerm;>;
	()Lscala/util/parsing/combinator/Parsers$Parser<[Lde/tud/cs/st/sae/Term;>;
	()TA;
	()TE;
	()TT;
	()TThis;
	()V
	(I)Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ValueTerm;>;
	(I)TA;
	(I)TT;
	(I)TThis;
	(IIIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTableEntry;
	(IIIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTypeTableEntry;
	(IIIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/LocalVariableTableEntry;
	(IIIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/LocalVariableTypeTableEntry;
	(IIIIILscala/RandomAccessSeq<Ljava/lang/Integer;>;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
	(IIIIILscala/RandomAccessSeq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Field_Info;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Method_Info;>;Lscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ClassFile;
	(IIIIILscala/RandomAccessSeq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<TA;>;Lscala/RandomAccessSeq<TA;>;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/prolog/ClassFileFact;
	(IIIIILscala/RandomAccessSeq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<TA;>;Lscala/RandomAccessSeq<TA;>;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ClassFile;
	(IIIILde/tud/cs/st/bat/native/reader/BasicJava6Framework$Code;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ExceptionTableEntry;>;Lscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;)V
	(IIIILde/tud/cs/st/bat/native/reader/BasicJava6Framework$Code;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ExceptionTableEntry;>;Lscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Code_attribute;
	(IIIILjava/lang/Object;Lscala/RandomAccessSeq<Ljava/lang/Object;>;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
	(IIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ExceptionTableEntry;
	(IIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$InnerClassesEntry;
	(IIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ExceptionTableEntry;
	(IIIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/InnerClassesEntry;
	(IIII[Lde/tud/cs/st/bat/resolved/Instruction;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ExceptionTableEntry;>;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Code_attribute;
	(IIILde/tud/cs/st/bat/resolved/ObjectType;Lde/tud/cs/st/bat/resolved/ObjectType;Lscala/Seq<Lde/tud/cs/st/bat/resolved/ObjectType;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Field_Info;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Method_Info;>;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)V
	(IIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$EnclosingMethod_attribute;
	(IIILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/EnclosingMethod_attribute;
	(IIILscala/RandomAccessSeq<Ljava/lang/Integer;>;)V
	(IIILscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;)V
	(IIILscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Field_Info;
	(IIILscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Method_Info;
	(IIILscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/prolog/FieldFact;
	(IIILscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/prolog/MethodFact;
	(IIILscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Field_Info;
	(IIILscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Method_Info;
	(IILde/tud/cs/st/bat/native/ElementValue;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$AnnotationDefault_attribute;
	(IILde/tud/cs/st/bat/resolved/ElementValue;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/AnnotationDefault_attribute;
	(IILjava/lang/String;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$SourceDebugExtension_attribute;
	(IILjava/lang/String;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/SourceDebugExtension_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/ElementValue;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ConstantValue_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LineNumberTableEntry;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Signature_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$SourceFile_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/SourceFile_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;)V
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$RuntimeInvisibleAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$RuntimeVisibleAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LineNumberTableEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LineNumberTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTableEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTypeTableEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTypeTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapFrame;>;)V
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapFrame;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapFrame;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;)V
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$StackMapFrame;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/VerificationTypeInfo;>;)V
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/RuntimeInvisibleAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/RuntimeVisibleAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LineNumberTableEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/LineNumberTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LocalVariableTableEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/LocalVariableTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LocalVariableTypeTableEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/LocalVariableTypeTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/StackMapFrame;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/StackMapTable_attribute;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;)Lde/tud/cs/st/bat/resolved/StackMapFrame;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;)V
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;)Lde/tud/cs/st/bat/resolved/StackMapFrame;
	(IILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/VerificationTypeInfo;>;)V
	(IILscala/RandomAccessSeq<Ljava/lang/Integer;>;Ljava/lang/Object;)Ljava/lang/Object;
	(IILscala/RandomAccessSeq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Exceptions_attribute;
	(IILscala/RandomAccessSeq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Exceptions_attribute;
	(IILscala/RandomAccessSeq<Ljava/lang/Object;>;)Ljava/lang/Object;
	(IILscala/RandomAccessSeq<Ljava/lang/Object;>;Ljava/lang/Object;)Ljava/lang/Object;
	(IILscala/RandomAccessSeq<Ljava/lang/Object;>;Lscala/RandomAccessSeq<Ljava/lang/Object;>;)Ljava/lang/Object;
	(IILscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;>;)V
	(IILscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$RuntimeInvisibleParameterAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;>;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$RuntimeVisibleParameterAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/RuntimeInvisibleParameterAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/RuntimeVisibleParameterAnnotations_attribute;
	(IILscala/RandomAccessSeq<Lscala/Tuple2<Ljava/lang/Integer;Ljava/lang/Integer;>;>;)V
	(II[Lde/tud/cs/st/bat/resolved/Instruction;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ExceptionTableEntry;>;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)V
	(ILde/tud/cs/st/bat/native/ElementValue;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ElementValuePair;
	(ILde/tud/cs/st/bat/resolved/ElementValue;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ElementValuePair;
	(ILde/tud/cs/st/sae/GroundTerms<TT;>;)V
	(ILjava/lang/String;Lde/tud/cs/st/bat/resolved/FieldDescriptor;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)V
	(ILjava/lang/String;Lde/tud/cs/st/bat/resolved/MethodDescriptor;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)V
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/ElementValue;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Deprecated_attribute;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Synthetic_attribute;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/VerificationTypeInfo;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/prolog/TypeTerm;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ConstantValue<*>;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Deprecated_attribute;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ElementValue;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/FieldDescriptor;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/MethodDescriptor;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ObjectType;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ObjectVariableInfo;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Synthetic_attribute;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/sae/StringAtom;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Ljava/lang/String;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lscala/Tuple2<Ljava/lang/String;Lde/tud/cs/st/bat/resolved/FieldType;>;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lscala/Tuple2<Ljava/lang/String;Lde/tud/cs/st/bat/resolved/MethodDescriptor;>;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lscala/Tuple3<Lde/tud/cs/st/bat/resolved/ObjectType;Ljava/lang/String;Lde/tud/cs/st/bat/resolved/FieldType;>;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lscala/Tuple3<Lde/tud/cs/st/bat/resolved/ObjectType;Ljava/lang/String;Lde/tud/cs/st/bat/resolved/MethodDescriptor;>;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ElementValuePair;>;)V
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$ElementValuePair;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$InnerClassesEntry;>;)V
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$InnerClassesEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$InnerClasses_attribute;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LineNumberTableEntry;>;)V
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTableEntry;>;)V
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$LocalVariableTypeTableEntry;>;)V
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ElementValuePair;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Annotation;
	(ILscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/InnerClassesEntry;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/InnerClasses_attribute;
	(ILscala/RandomAccessSeq<Ljava/lang/Integer;>;)V
	(ILscala/RandomAccessSeq<Ljava/lang/Object;>;Ljava/lang/Object;)Ljava/lang/Object;
	(ILscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)Lde/tud/cs/st/bat/prolog/SyntheticTerm;
	(ILscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)Z
	(I[BLscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Unknown_attribute;
	(I[BLscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/Unknown_attribute;
	(Lalice/tuprolog/Prolog;)Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;
	(Lalice/tuprolog/Prolog;Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;)V
	(Lalice/tuprolog/Prolog;Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;)V
	(Lde/tud/bat/classfile/structure/Attribute;Ljava/io/DataOutputStream;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;)V
	(Lde/tud/bat/classfile/structure/Attributes;Ljava/io/DataOutputStream;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;I)V
	(Lde/tud/bat/classfile/structure/ClassFileElement;Ljava/lang/Class<*>;[Ljava/lang/Class<*>;)V
	(Lde/tud/bat/classfile/structure/Code;Lde/tud/bat/instruction/Instruction;IIZLjava/io/DataInputStream;Ljava/util/ArrayList<Lde/tud/bat/io/reader/instruction/UninitializedJumpTarget;>;Lde/tud/bat/io/reader/ConstantPoolResolver;)I
	(Lde/tud/bat/classfile/structure/Code;Ljava/io/DataOutputStream;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/LineNumber;>;)V
	(Lde/tud/bat/classfile/structure/ExceptionHandler;)Ljava/util/List<Lde/tud/bat/instruction/executiongraph/BasicBlock;>;
	(Lde/tud/bat/classfile/structure/ExceptionHandler;Ljava/util/HashMap<Lde/tud/bat/instruction/Instruction;Lde/tud/bat/quadruples/CodeElement;>;)V
	(Lde/tud/bat/classfile/structure/MemberValueConstant<*>;)V
	(Lde/tud/bat/classfile/structure/Method;Ljava/lang/Class<*>;[Ljava/lang/Class<*>;)Lde/tud/bat/classfile/structure/MethodSignature;
	(Lde/tud/bat/classfile/structure/MethodRef;Ljava/lang/Class<*>;[Ljava/lang/Class<*>;)Lde/tud/bat/classfile/structure/MethodSignature;
	(Lde/tud/bat/classfile/structure/MethodSignature;Lde/tud/bat/type/Type;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/Annotation;>;)Lde/tud/bat/classfile/structure/MethodParameter;
	(Lde/tud/bat/classfile/structure/MethodSignature;Lde/tud/bat/type/Type;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/Annotation;>;)V
	(Lde/tud/bat/classfile/structure/MethodSignature;Lde/tud/bat/type/Type;Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Annotation;>;)Lde/tud/bat/classfile/structure/MethodParameter;
	(Lde/tud/bat/classfile/structure/MethodSignature;Lde/tud/bat/type/Type;Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Annotation;>;)V
	(Lde/tud/bat/instruction/Instruction;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;)[B
	(Lde/tud/bat/io/xml/writer/bytecodeinstructions/WrapperHandler;Lde/tud/bat/instruction/Instruction;Ljava/util/Vector<Lorg/jdom/Element;>;)V
	(Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/Annotation;>;)I
	(Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/Type;>;)Z
	(Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Annotation;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/ElementValue;
	(Lde/tud/cs/st/bat/prolog/ClassCategoryAtom;Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;Lde/tud/cs/st/bat/prolog/VisibilityAtom;Lde/tud/cs/st/bat/prolog/FinalTerm;Lde/tud/cs/st/bat/prolog/AbstractTerm;Lde/tud/cs/st/bat/prolog/SyntheticTerm;Lde/tud/cs/st/bat/prolog/DeprecatedTerm;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/prolog/FieldFact;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/prolog/MethodFact;>;)V
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ExceptionTableEntryTerm;>;)V
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/LineNumberTableEntryTerm;>;)Lde/tud/cs/st/bat/prolog/LineNumbersTableFact;
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/LineNumberTableEntryTerm;>;)V
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;)Lde/tud/cs/st/bat/prolog/MethodExceptionsFact;
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ObjectTypeTerm;>;)V
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/KeyValueTerm;>;)V
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/IntegerAtom;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/sae/IntegerAtom;>;)V
	(Lde/tud/cs/st/bat/prolog/MethodKeyAtom;Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)V
	(Lde/tud/cs/st/bat/prolog/TypeTerm;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ElementValuePairTerm;>;)Lde/tud/cs/st/bat/prolog/AnnotationValueTerm;
	(Lde/tud/cs/st/bat/prolog/TypeTerm;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ElementValuePairTerm;>;)V
	(Lde/tud/cs/st/bat/resolved/Annotation;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ElementValue;
	(Lde/tud/cs/st/bat/resolved/ArrayType;)Lscala/Option<Lde/tud/cs/st/bat/resolved/FieldType;>;
	(Lde/tud/cs/st/bat/resolved/ConstantValue<*>;)Lde/tud/cs/st/bat/prolog/FieldValueTerm;
	(Lde/tud/cs/st/bat/resolved/ConstantValue<*>;)Lde/tud/cs/st/bat/prolog/ValueTerm;
	(Lde/tud/cs/st/bat/resolved/ConstantValue<*>;)V
	(Lde/tud/cs/st/bat/resolved/FieldDescriptor;)Lscala/Option<Lde/tud/cs/st/bat/resolved/FieldType;>;
	(Lde/tud/cs/st/bat/resolved/FieldType;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ElementValuePair;>;)V
	(Lde/tud/cs/st/bat/resolved/Instruction;ILjava/io/DataInputStream;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/AALOAD;
	(Lde/tud/cs/st/bat/resolved/ObjectType;)Lscala/Option<Ljava/lang/String;>;
	(Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/TypeTerm;>;Lde/tud/cs/st/bat/prolog/TypeTerm;)Lde/tud/cs/st/bat/prolog/MethodDescriptorTerm;
	(Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/TypeTerm;>;Lde/tud/cs/st/bat/prolog/TypeTerm;)V
	(Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ValueTerm;>;)Lde/tud/cs/st/bat/prolog/ArrayValueTerm;
	(Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ValueTerm;>;)V
	(Lde/tud/cs/st/sae/KeyAtom;Lde/tud/cs/st/bat/prolog/RuntimeVisibleTerm;Lde/tud/cs/st/bat/prolog/TypeTerm;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ElementValuePairTerm;>;)Lde/tud/cs/st/bat/prolog/AnnotationFact;
	(Lde/tud/cs/st/sae/KeyAtom;Lde/tud/cs/st/bat/prolog/RuntimeVisibleTerm;Lde/tud/cs/st/bat/prolog/TypeTerm;Lde/tud/cs/st/sae/GroundTerms<Lde/tud/cs/st/bat/prolog/ElementValuePairTerm;>;)V
	(Lde/tud/cs/st/sae/Term;)Lscala/Tuple2<Ljava/lang/Object;Ljava/lang/Integer;>;
	(Lde/tud/cs/st/sae/Term;Lscala/Seq<Lde/tud/cs/st/sae/Term;>;)V
	(Lde/tud/cs/st/util/collection/LinkedListStore<TA;>;)V
	(Lde/tud/cs/st/util/collection/LinkedListWorkList<TA;>.Element<TA;>;)V
	(Lde/tud/cs/st/util/collection/LinkedListWorkList<TA;>;)V
	(Lde/tud/cs/st/util/collection/LinkedListWorkList<TA;>;TA;)V
	(Ljava/io/DataInputStream;)Lscala/RandomAccessSeq<Ljava/lang/Object;>;
	(Ljava/io/DataInputStream;Ljava/lang/Object;)Lscala/RandomAccessSeq<Ljava/lang/Object;>;
	(Ljava/io/DataInputStream;Ljava/lang/Object;)Lscala/RandomAccessSeq<Lscala/RandomAccessSeq<Ljava/lang/Object;>;>;
	(Ljava/io/DataInputStream;Lscala/RandomAccessSeq<TA;>;)Lscala/Seq<Ljava/lang/Object;>;
	(Ljava/io/DataOutputStream;ILde/tud/bat/instruction/Instruction;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/List<Lde/tud/bat/io/writer/UnresolvedJumpOffset;>;)I
	(Ljava/io/DataOutputStream;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/InnerClass;>;Lde/tud/bat/io/writer/ConstantPoolCreator;)V
	(Ljava/io/DataOutputStream;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/LineNumber;>;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;)V
	(Ljava/io/DataOutputStream;Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/LocalVariable;>;Lde/tud/bat/io/writer/ConstantPoolCreator;Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;)V
	(Ljava/io/DataOutputStream;Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/ObjectType;>;Lde/tud/bat/io/writer/ConstantPoolCreator;)V
	(Ljava/io/DataOutputStream;Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Annotation;>;Lde/tud/bat/io/writer/ConstantPoolCreator;)V
	(Ljava/lang/Class<*>;[Ljava/lang/Class<*>;)V
	(Ljava/lang/Object;Ljava/util/Map<Lde/tud/bat/instruction/Instruction;Lde/tud/bat/quadruples/CodeElement;>;)Ljava/lang/Object;
	(Ljava/lang/reflect/Constructor<*>;)V
	(Ljava/lang/String;)Lde/tud/bat/util/BATIterator<Lde/tud/bat/classfile/structure/Attribute;>;
	(Ljava/lang/String;)Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;
	(Ljava/lang/String;)Lscala/Option<Lscala/Enumeration$Value;>;
	(Ljava/lang/String;I)Lscala/Tuple2<Lde/tud/cs/st/bat/resolved/FieldType;Ljava/lang/Integer;>;
	(Ljava/lang/String;Lde/tud/cs/st/columbus/Container;Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;Ljava/util/List<Lorg/apache/commons/vfs/FileObject;>;Lorg/apache/commons/vfs/FileObject;)V
	(Ljava/lang/String;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;)Ljava/lang/String;
	(Ljava/lang/String;Lorg/scalatest/Reporter;Lorg/scalatest/Stopper;Lscala/collection/immutable/Map<Ljava/lang/String;Ljava/lang/Object;>;)V
	(Ljava/lang/String;Lscala/Function0<Ljava/io/InputStream;>;)Ljava/lang/String;
	(Ljava/lang/String;Lscala/Seq<Lde/tud/cs/st/sae/GroundTerm;>;)V
	(Ljava/lang/String;Lscala/Seq<Lde/tud/cs/st/sae/Term;>;)V
	(Ljava/lang/String;Z)Ljava/lang/Class<*>;
	(Ljava/util/Collection<+Lde/tud/cs/st/columbus/AbstractComponentService;>;)V
	(Ljava/util/Collection<+Lde/tud/cs/st/columbus/IBundleEntry;>;)Ljava/util/Set<Lde/tud/cs/st/columbus/Bundle;>;
	(Ljava/util/Collection<+TT;>;)V
	(Ljava/util/Comparator<Ljava/lang/Integer;>;)V
	(Ljava/util/HashSet<Lde/tud/bat/instruction/executiongraph/BasicBlock;>;Lde/tud/bat/instruction/executiongraph/BasicBlock;Lde/tud/bat/instruction/executiongraph/BasicBlockIterationListener;)V
	(Ljava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;)Z
	(Ljava/util/List<*>;I)Lde/tud/bat/io/xml/reader/executiongraph/BasicBlock;
	(Ljava/util/List<Lde/michaeleichberg/multihtreadedprogramming/v2Beta4Thread/Calculation;>;)V
	(Ljava/util/List<Lde/tud/bat/instruction/executiongraph/BasicBlock;>;)V
	(Ljava/util/List<Lde/tud/bat/io/xml/reader/executiongraph/Instruction;>;)V
	(Ljava/util/List<Lde/tud/cs/st/columbus/RequiredService;>;Ljava/util/List<Lde/tud/cs/st/columbus/ProvidedService;>;Ljava/lang/String;Lde/tud/cs/st/columbus/Version;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
	(Ljava/util/List<TE;>;)V
	(Ljava/util/Map<Lde/tud/bat/instruction/Instruction;Lde/tud/bat/quadruples/CodeElement;>;)V
	(Ljava/util/Set<+Lde/tud/cs/st/columbus/AbstractBundleEntry;>;)V
	(Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;)Ljava/util/Map<Lde/tud/cs/st/columbus/Component;Ljava/lang/Object;>;
	(Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;)Ljava/util/Map<Lde/tud/cs/st/columbus/IBundleEntry;Ljava/lang/ClassLoader;>;
	(Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;)Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;
	(Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;)V
	(Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;Lde/tud/cs/st/columbus/Component;)V
	(Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;Ljava/util/Map<Lde/tud/cs/st/columbus/Component;Ljava/util/List<Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;>;>;)Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;
	(Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;Lde/tud/cs/st/columbus/ServiceSpecification;)V
	(Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;Ljava/lang/String;)Lde/tud/cs/st/columbus/ServiceSpecification;
	(Ljava/util/Set<Lde/tud/cs/st/columbus/ServiceSpecification;>;Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;)V
	(Ljava/util/Set<Ljava/lang/ClassLoader;>;)V
	(Ljava/util/Set<Ljava/lang/String;>;)Ljava/util/Set<Lde/tud/cs/st/columbus/Component;>;
	(Lnet/sf/saxon/Configuration;Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V
	(Lorg/jdom/Element;Lde/tud/bat/classfile/structure/ClassFileElement;)Ljava/util/ArrayList<Lde/tud/bat/classfile/structure/Annotation;>;
	(Lscala/collection/immutable/Set<Lde/tud/cs/st/util/graphs/Node;>;)Ljava/lang/String;
	(Lscala/collection/immutable/Set<Ljava/lang/String;>;Ljava/lang/String;)Lscala/collection/immutable/Set<Ljava/lang/String;>;
	(Lscala/collection/mutable/Map<Ljava/lang/String;Lde/tud/cs/st/sae/Variable;>;)Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Term;>;
	(Lscala/collection/mutable/Map<Ljava/lang/String;Lde/tud/cs/st/sae/Variable;>;)Lscala/util/parsing/combinator/Parsers$Parser<Lde/tud/cs/st/sae/Variable;>;
	(Lscala/Function0<Ljava/io/InputStream;>;)Ljava/lang/Object;
	(Lscala/Function0<TT;>;)[TT;
	(Lscala/Function1<Lde/tud/cs/st/sae/Term;Ljava/lang/Object;>;)V
	(Lscala/Function1<Lde/tud/cs/st/util/graphs/Node;Ljava/lang/Object;>;)V
	(Lscala/Function1<Lde/tud/cs/st/util/trees/TreeNode;Ljava/lang/Object;>;)V
	(Lscala/Function1<Lscala/Enumeration$Value;Ljava/lang/Boolean;>;)Lscala/Iterator<Lscala/Enumeration$Value;>;
	(Lscala/Function1<Lscala/Enumeration$Value;Ljava/lang/Boolean;>;)Z
	(Lscala/Function1<Lscala/Enumeration$Value;Ljava/lang/Object;>;)V
	(Lscala/Function1<Lscala/Nothing;Ljava/lang/Object;>;)V
	(Lscala/Function1<TA;Ljava/lang/Object;>;)V
	(Lscala/Function1<TA;Ljava/lang/String;>;Ljava/lang/String;)Ljava/lang/String;
	(Lscala/List<Lde/tud/cs/st/sae/Fact;>;)V
	(Lscala/List<Lde/tud/cs/st/sae/GroundTerm;>;)Lde/tud/cs/st/sae/cons<Lde/tud/cs/st/sae/GroundTerm;>;
	(Lscala/List<Lde/tud/cs/st/sae/Query;>;)V
	(Lscala/List<Lde/tud/cs/st/sae/Rule;>;)V
	(Lscala/List<Lde/tud/cs/st/sae/Rule;>;Lscala/List<Lde/tud/cs/st/sae/Fact;>;Lscala/List<[Lde/tud/cs/st/sae/Term;>;)V
	(Lscala/List<Lde/tud/cs/st/sae/Term;>;)[Lde/tud/cs/st/sae/Term;
	(Lscala/List<Ljava/lang/Object;>;)Lde/tud/cs/st/sae/parser/Program;
	(Lscala/List<TT;>;)[TT;
	(Lscala/List<[Lde/tud/cs/st/sae/Term;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;IIIIILscala/RandomAccessSeq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Field_Info;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Method_Info;>;Lscala/Seq<Lde/tud/cs/st/bat/native/Attribute;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/ElementValue;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/ElementValue;
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;)Lscala/xml/Elem;
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ElementValue;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/ElementValue;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/resolved/ElementValue;
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LineNumberTableEntry;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LocalVariableTableEntry;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/LocalVariableTypeTableEntry;>;)V
	(Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/StackMapFrame;>;)V
	(Lscala/RandomAccessSeq<Ljava/lang/Object;>;Ljava/lang/Object;)Ljava/lang/Object;
	(Lscala/RandomAccessSeq<Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/resolved/Annotation;>;>;)V
	(Lscala/Seq<Lde/tud/cs/st/bat/native/ElementValue;>;)V
	(Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)Lde/tud/cs/st/bat/prolog/DeprecatedTerm;
	(Lscala/Seq<Lde/tud/cs/st/bat/resolved/Attribute;>;)Z
	(Lscala/Seq<Lde/tud/cs/st/bat/resolved/FieldType;>;Lde/tud/cs/st/bat/resolved/Type;)V
	(Lscala/Seq<Lde/tud/cs/st/bat/resolved/ObjectType;>;)V
	(Lscala/Seq<Lde/tud/cs/st/sae/Fact;>;)V
	(Lscala/Seq<Lde/tud/cs/st/sae/Rule;>;)V
	(Lscala/Seq<Ljava/lang/Integer;>;Lscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lscala/Seq<Lde/tud/cs/st/bat/resolved/ObjectType;>;
	(Lscala/Seq<[Lde/tud/cs/st/sae/Term;>;)V
	(Lscala/Tuple2<Ljava/lang/Integer;Ljava/lang/Integer;>;)Lde/tud/cs/st/bat/prolog/KeyValueTerm;
	(Lscala/Tuple2<Ljava/lang/Integer;Ljava/lang/Integer;>;)Lscala/xml/Elem;
	(Lscala/Tuple2<Ljava/lang/Object;Ljava/lang/Object;>;)V
	(Lscala/Tuple2<Ljava/lang/String;Lscala/Function3<Ljava/io/DataInputStream;Ljava/lang/Object;Ljava/lang/Integer;Ljava/lang/Object;>;>;)V
	(Lscala/Tuple2<Ljava/lang/String;Lscala/Function3<Ljava/io/DataInputStream;Lscala/RandomAccessSeq<TA;>;Ljava/lang/Integer;Ljava/lang/Object;>;>;)V
	(Lscala/util/parsing/combinator/Parsers$$tilde<Ljava/lang/String;[Lde/tud/cs/st/sae/GroundTerm;>;)Lde/tud/cs/st/sae/Fact;
	(Lscala/util/parsing/combinator/Parsers$$tilde<Ljava/lang/String;[Lde/tud/cs/st/sae/GroundTerm;>;)Lde/tud/cs/st/sae/GroundTerm;
	(Lscala/util/parsing/combinator/Parsers$$tilde<Ljava/lang/String;[Lde/tud/cs/st/sae/Term;>;)Lde/tud/cs/st/sae/Term;
	(Lscala/util/parsing/combinator/Parsers$$tilde<Lscala/util/parsing/combinator/Parsers$$tilde<Lde/tud/cs/st/sae/Term;Ljava/lang/String;>;[Lde/tud/cs/st/sae/Term;>;)Lde/tud/cs/st/sae/Rule;
	(TA;)V
	(TA;Lde/tud/cs/st/util/collection/Store<TA;>;)V
	(TT;)V
	(TThis;)Z
	(TThis;TThis;I)TThis;
	([BLjava/util/Hashtable<Lde/tud/bat/instruction/Instruction;Ljava/lang/Integer;>;)V
	([BLscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)Lde/tud/cs/st/bat/native/reader/BasicJava6Framework$Code;
	([BLscala/RandomAccessSeq<Lde/tud/cs/st/bat/native/Constant_Pool_Entry;>;)[Lde/tud/cs/st/bat/resolved/Instruction;
	([Lde/tud/cs/st/columbus/Component;)Ljava/util/Map<Lde/tud/cs/st/columbus/Component;Ljava/lang/Object;>;
	([Ljava/lang/Class<*>;)Lde/tud/bat/util/BATIterator<Lde/tud/bat/type/Type;>;
	([Ljava/lang/Class<*>;)Lde/tud/bat/util/BATIterator<Ljava/lang/String;>;
	([TE;)V
	<A:Ljava/lang/Object;>()Lde/tud/cs/st/util/collection/WorkList<TA;>;
	<A:Ljava/lang/Object;>(Lde/tud/cs/st/util/collection/Indexed<TA;>;)Lde/tud/cs/st/util/collection/LinkedListWorkList<TA;>;
	<A:Ljava/lang/Object;>(Lde/tud/cs/st/util/collection/WorkList<TA;>;Lscala/Function1<TA;Ljava/lang/Object;>;)V
	<A:Ljava/lang/Object;>(Lscala/Seq<TA;>;)Lde/tud/cs/st/util/collection/Store<TA;>;
	<A:Ljava/lang/Object;>(TA;)Lde/tud/cs/st/util/collection/WorkList<TA;>;
	<B:Ljava/lang/Object;>(Lscala/Function1<Lscala/Enumeration$Value;Lscala/Iterator<TB;>;>;)Lscala/Iterator<TB;>;
	<B:Ljava/lang/Object;>(Lscala/Function1<Lscala/Enumeration$Value;TB;>;)Lscala/Iterator<TB;>;
	<B:Ljava/lang/Object;>(TB;)Lde/tud/cs/st/util/collection/LinkedListStore<TB;>;
	<B:Ljava/lang/Object;>(TB;)Lde/tud/cs/st/util/collection/Store<TB;>;
	<I:Ljava/io/InputStream;T:Ljava/lang/Object;>(Lscala/Function0<TI;>;Lscala/Function1<TI;TT;>;)TT;
	<T::Lde/tud/cs/st/sae/GroundTerm;>([TT;)Lde/tud/cs/st/sae/GroundTerms<TT;>;
	<T::Lde/tud/cs/st/sae/Term;>(Lscala/Function0<Lscala/util/parsing/combinator/Parsers$Parser<TT;>;>;)Lscala/util/parsing/combinator/Parsers$Parser<[TT;>;
	<T:Ljava/lang/Object;>()Lscala/Function1<Lscala/Function0<TT;>;[TT;>;
	<T:Ljava/lang/Object;>(ILscala/Function0<TT;>;)[TT;
	<T:Ljava/lang/Object;>(Lde/tud/bat/classfile/structure/ClassFileElement;)Lde/tud/bat/classfile/structure/MemberValueConstant<TT;>;
	<T:Ljava/lang/Object;>(Ljava/lang/String;Lscala/Function0<TT;>;)TT;
	<T:Ljava/lang/Object;>(Lscala/Function0<TT;>;I)[TT;
	<T:Ljava/lang/Object;>(Lscala/Function1<Ljava/lang/Double;Ljava/lang/Object;>;Lscala/Function0<TT;>;)TT;
	<T:Ljava/lang/Object;>(Lscala/Symbol;Lscala/Function0<TT;>;)TT;
	<T:Ljava/lang/Object;>([TT;ITT;Lscala/Function1<TT;Ljava/lang/Integer;>;)I
	<T:Ljava/lang/Object;>([TT;I[TT;)[TT;
	<T:Ljava/lang/Object;>([TT;TT;)I
	<T:Ljava/lang/Object;>([TT;TT;)Z
	<T:Ljava/lang/Object;>([TT;TT;)[TT;
	<T:Ljava/lang/Object;>([TT;TT;I)[TT;
	<T:Ljava/lang/Object;>([TT;TT;[TT;)[TT;
	<T:Ljava/lang/Object;>([TT;[TT;)[TT;
	<T:Ljava/lang/Object;G::Lde/tud/cs/st/sae/GroundTerm;>(Lscala/RandomAccessSeq<TT;>;Lscala/Function1<TT;TG;>;)Lde/tud/cs/st/sae/GroundTerms<TG;>;
	<T:Ljava/lang/Object;G::Lde/tud/cs/st/sae/GroundTerm;>(Lscala/Seq<TT;>;Lscala/Function1<TT;TG;>;)Lde/tud/cs/st/sae/GroundTerms<TG;>;
	<X:Ljava/lang/Object;Y:Ljava/lang/Object;>(Lde/tud/cs/st/util/collection/Indexed<TX;>;Lde/tud/cs/st/util/collection/Indexed<TY;>;Lscala/Function2<TX;TY;Ljava/lang/Boolean;>;)Z
  	""".trim.split("\n").map(_.trim())
}
