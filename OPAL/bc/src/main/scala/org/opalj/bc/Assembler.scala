/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bc

import scala.annotation.switch
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import org.opalj.bc.MethodFilter.logContext
import org.opalj.bi.ACC_STRICT
import org.opalj.bi.{ConstantPoolTags => CPTags}
import org.opalj.da._
import org.opalj.da.ClassFileReader.LineNumberTable_attribute
import org.opalj.log.OPALLogger

/**
 * Factory to create the binary representation (that is, an array of bytes) of a given class file.
 *
 * @author Michael Eichberg
 */
object Assembler {

    private[bc] def as[T](x: AnyRef): T = x.asInstanceOf[T]

    implicit object RichCONSTANT_Class_info extends ClassFileElement[CONSTANT_Class_info] {
        def write(
            ci: CONSTANT_Class_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(name_index)
        }
    }

    implicit object RichCONSTANT_Ref extends ClassFileElement[CONSTANT_Ref] {
        def write(
            cr: CONSTANT_Ref
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import cr._
            import out._
            writeByte(tag)
            writeShort(class_index)
            writeShort(name_and_type_index)
        }
    }

    implicit object RichCONSTANT_String_info extends ClassFileElement[CONSTANT_String_info] {
        def write(
            ci: CONSTANT_String_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(string_index)
        }
    }

    implicit object RichCONSTANT_Integer_info extends ClassFileElement[CONSTANT_Integer_info] {
        def write(
            ci: CONSTANT_Integer_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeInt(value)
        }
    }

    implicit object RichCONSTANT_Float_info extends ClassFileElement[CONSTANT_Float_info] {
        def write(
            ci: CONSTANT_Float_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeFloat(value)
        }
    }

    implicit object RichCONSTANT_Long_info extends ClassFileElement[CONSTANT_Long_info] {
        def write(
            ci: CONSTANT_Long_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeLong(value)
        }
    }

    implicit object RichCONSTANT_Double_info extends ClassFileElement[CONSTANT_Double_info] {
        def write(
            ci: CONSTANT_Double_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeDouble(value)
        }
    }

    implicit object RichCONSTANT_NameAndType_info
        extends ClassFileElement[CONSTANT_NameAndType_info] {
        def write(
            ci: CONSTANT_NameAndType_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(name_index)
            writeShort(descriptor_index)

        }
    }

    implicit object RichCONSTANT_Utf8_info
        extends ClassFileElement[CONSTANT_Utf8_info] {
        def write(
            ci: CONSTANT_Utf8_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeUTF(value)
        }
    }

    implicit object RichCONSTANT_MethodHandle_info
        extends ClassFileElement[CONSTANT_MethodHandle_info] {
        def write(
            ci: CONSTANT_MethodHandle_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeByte(reference_kind)
            writeShort(reference_index)
        }
    }

    implicit object RichCONSTANT_MethodType_info
        extends ClassFileElement[CONSTANT_MethodType_info] {
        def write(
            ci: CONSTANT_MethodType_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(descriptor_index)
        }
    }

    implicit object RichCONSTANT_InvokeDynamic_info
        extends ClassFileElement[CONSTANT_InvokeDynamic_info] {
        def write(
            ci: CONSTANT_InvokeDynamic_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(bootstrap_method_attr_index)
            writeShort(name_and_type_index)
        }
    }

    implicit object RichCONSTANT_Module_info extends ClassFileElement[CONSTANT_Module_info] {
        def write(
            ci: CONSTANT_Module_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(name_index)
        }
    }

    implicit object RichCONSTANT_Package_info extends ClassFileElement[CONSTANT_Package_info] {
        def write(
            ci: CONSTANT_Package_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(name_index)
        }
    }

    implicit object RichCONSTANT_Dynamic_info extends ClassFileElement[CONSTANT_Dynamic_info] {
        def write(
            ci: CONSTANT_Dynamic_info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(bootstrap_method_attr_index)
            writeShort(name_and_type_index)
        }
    }

    implicit object RichConstant_Pool_Entry extends ClassFileElement[Constant_Pool_Entry] {
        def write(
            cpe: Constant_Pool_Entry
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            cpe.Constant_Type_Value.id match {
                case CPTags.CONSTANT_Utf8_ID        => serializeAs[CONSTANT_Utf8_info](cpe)
                case CPTags.CONSTANT_Class_ID       => serializeAs[CONSTANT_Class_info](cpe)
                case CPTags.CONSTANT_String_ID      => serializeAs[CONSTANT_String_info](cpe)
                case CPTags.CONSTANT_Integer_ID     => serializeAs[CONSTANT_Integer_info](cpe)
                case CPTags.CONSTANT_Float_ID       => serializeAs[CONSTANT_Float_info](cpe)
                case CPTags.CONSTANT_Long_ID        => serializeAs[CONSTANT_Long_info](cpe)
                case CPTags.CONSTANT_Double_ID      => serializeAs[CONSTANT_Double_info](cpe)
                case CPTags.CONSTANT_NameAndType_ID => serializeAs[CONSTANT_NameAndType_info](cpe)
                case CPTags.CONSTANT_Fieldref_ID |
                    CPTags.CONSTANT_Methodref_ID |
                    CPTags.CONSTANT_InterfaceMethodref_ID => serializeAs[CONSTANT_Ref](cpe)

                // JAVA 7
                case CPTags.CONSTANT_MethodHandle_ID =>
                    serializeAs[CONSTANT_MethodHandle_info](cpe)
                case CPTags.CONSTANT_MethodType_ID =>
                    serializeAs[CONSTANT_MethodType_info](cpe)
                case CPTags.CONSTANT_InvokeDynamic_ID =>
                    serializeAs[CONSTANT_InvokeDynamic_info](cpe)

                // JAVA 9
                case CPTags.CONSTANT_Module_ID  => serializeAs[CONSTANT_Module_info](cpe)
                case CPTags.CONSTANT_Package_ID => serializeAs[CONSTANT_Package_info](cpe)

                // JAVA 11
                case CPTags.CONSTANT_Dynamic_ID => serializeAs[CONSTANT_Dynamic_info](cpe)
            }
        }
    }

    implicit object RichElementValue extends ClassFileElement[ElementValue] {
        def write(
            ev: ElementValue
        )(
            implicit
            out:                DataOutputStream,
            segmentInformation: (String, Int) => Unit
        ): Unit = {
            import out._
            val tag = ev.tag
            writeByte(tag)
            tag match {
                case 'B' | 'C' | 'D' | 'F' | 'I' | 'J' | 'S' | 'Z' =>
                    writeShort(as[BaseElementValue](ev).const_value_index)
                case 's' =>
                    writeShort(as[StringValue](ev).const_value_index)
                case 'e' =>
                    val e = as[EnumValue](ev)
                    writeShort(e.type_name_index)
                    writeShort(e.const_name_index)
                case 'c' =>
                    writeShort(as[ClassValue](ev).class_info_index)
                case '@' =>
                    val av = as[AnnotationValue](ev)
                    serialize(av.annotation)(RichAnnotation, out, segmentInformation)
                case '[' =>
                    val av = as[ArrayValue](ev)
                    writeShort(av.values.length)
                    av.values.foreach { serialize(_) }
            }
        }
    }

    implicit object RichAnnotation extends ClassFileElement[Annotation] {
        def write(
            a: Annotation
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import a._
            import out._
            writeShort(type_index)
            writeShort(element_value_pairs.length)
            element_value_pairs.foreach { evp =>
                writeShort(evp.element_name_index)
                serialize(evp.element_value)
            }
        }
    }

    implicit object RichTypeAnnotation extends ClassFileElement[TypeAnnotation] {
        def write(
            ta: TypeAnnotation
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import out._
            val target_type = ta.target_type
            val target_typeTag = target_type.tag
            writeByte(target_typeTag)
            (target_typeTag: @switch) match {
                case 0x00 | 0x01 =>
                    val tt = as[TATTypeParameter](target_type)
                    writeByte(tt.type_parameter_index)

                case 0x10 =>
                    val tt = as[TATSupertype](target_type)
                    writeShort(tt.supertype_index)

                case 0x11 | 0x12 =>
                    val tt = as[TATTypeParameterBound](target_type)
                    writeByte(tt.type_parameter_index)
                    writeByte(tt.bound_index)

                case 0x16 =>
                    val tt = as[TATFormalParameter](target_type)
                    writeByte(tt.formal_parameter_index)

                case 0x17 =>
                    val TATThrows(throws_type_index) = target_type
                    writeShort(throws_type_index)

                case 0x40 | 0x41 =>
                    val tt = as[TATLocalvar](target_type)
                    val lvt = tt.localvarTable
                    writeShort(lvt.length)
                    lvt.foreach { lvte =>
                        writeShort(lvte.start_pc)
                        writeShort(lvte.length)
                        writeShort(lvte.index)
                    }

                case 0x42 =>
                    val tt = as[TATCatch](target_type)
                    writeShort(tt.exception_table_index)

                case 0x43 | 0x44 | 0x45 | 0x46 =>
                    val tt = as[TATWithOffset](target_type)
                    writeShort(tt.offset)

                case 0x47 | 0x48 | 0x49 | 0x4A | 0x4B =>
                    val tt = as[TATTypeArgument](target_type)
                    writeShort(tt.offset)
                    writeByte(tt.type_argument_index)

                case 0x13 | 0x14 | 0x15 =>
                // EMPTY_TARGET <=> Nothing to do

            }

            ta.target_path match {
                case TypeAnnotationDirectlyOnType =>
                    writeByte(0)

                case TypeAnnotationPathElements(elements) =>
                    writeByte(elements.length)
                    elements.foreach { tape =>
                        writeByte(tape.type_path_kind)
                        writeByte(tape.type_argument_index)
                    }
            }

            writeShort(ta.type_index)
            val evps = ta.element_value_pairs
            writeShort(evps.length)
            evps.foreach { evp =>
                writeShort(evp.element_name_index)
                serialize(evp.element_value)
            }

        }
    }

    implicit object RichVerificationTypeInfo extends ClassFileElement[VerificationTypeInfo] {
        def write(
            vti: VerificationTypeInfo
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import out._
            val tag = vti.tag
            (tag: @scala.annotation.switch) match {
                case VerificationTypeInfo.ITEM_Object =>
                    val ovi = as[ObjectVariableInfo](vti)
                    writeByte(tag)
                    writeShort(ovi.cpool_index)
                case VerificationTypeInfo.ITEM_Unitialized =>
                    val uvi = as[UninitializedVariableInfo](vti)
                    writeByte(tag)
                    writeShort(uvi.offset)
                case _ =>
                    writeByte(tag)
            }

        }
    }

    implicit object RichAttribute extends ClassFileElement[Attribute] {
        def write(
            a: Attribute
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import a._
            import out._
            writeShort(attribute_name_index)
            writeInt(attribute_length)
            a match {

                case a: TypeAnnotations_attribute =>
                    // Handles:
                    // RuntimeVisibleTypeAnnotations_attribute
                    // RuntimeInvisibleTypeAnnotations_attribute
                    writeShort(a.typeAnnotations.size)
                    a.typeAnnotations.foreach { serialize(_) }

                case a: StackMapTable_attribute =>
                    writeShort(a.stack_map_frames.length)
                    a.stack_map_frames.foreach { smf =>
                        val frame_type = smf.frame_type
                        if (frame_type <= 63) {
                            writeByte(frame_type)
                        } else if (frame_type >= 64 && frame_type <= 127) {
                            val sl1sif = as[SameLocals1StackItemFrame](smf)
                            writeByte(frame_type)
                            serialize(sl1sif.verification_type_info_stack)
                        } else if (frame_type == 247) {
                            val sl1sife = as[SameLocals1StackItemFrameExtended](smf)
                            writeByte(frame_type)
                            writeShort(sl1sife.offset_delta)
                            serialize(sl1sife.verification_type_info_stack)
                        } else if (frame_type >= 248 && frame_type <= 250) {
                            writeByte(frame_type)
                            writeShort(as[ChopFrame](smf).offset_delta)
                        } else if (frame_type == 251) {
                            writeByte(frame_type)
                            writeShort(as[SameFrameExtended](smf).offset_delta)
                        } else if (frame_type >= 252 && frame_type <= 254) {
                            writeByte(frame_type)
                            val af = as[AppendFrame](smf)
                            writeShort(af.offset_delta)
                            af.verification_type_info_locals.foreach { serialize(_) }
                        } else if (frame_type == 255) {
                            val ff = as[FullFrame](smf)
                            writeByte(frame_type)
                            writeShort(ff.offset_delta)
                            writeShort(ff.verification_type_info_locals.length)
                            ff.verification_type_info_locals.foreach { serialize(_) }
                            writeShort(ff.verification_type_info_stack.length)
                            ff.verification_type_info_stack.foreach { serialize(_) }
                        } else {
                            throw new UnknownError(s"unknown stack map frame: $smf")
                        }

                    }

                case a: ParametersAnnotations_attribute =>
                    // Handles:
                    // RuntimeVisibleParameterAnnotations_attribute
                    // RuntimeInvisibleParameterAnnotations_attribute
                    writeByte(a.parameters_annotations.length)
                    a.parameters_annotations.foreach { pas =>
                        writeShort(pas.size)
                        pas.foreach { serialize(_) }
                    }

                case a: MethodParameters_attribute =>
                    writeByte(a.parameters.length)
                    a.parameters.foreach { p =>
                        writeShort(p.name_index)
                        writeShort(p.access_flags)
                    }

                case a: BootstrapMethods_attribute =>
                    val length = a.bootstrap_methods.length
                    writeShort(length)
                    a.bootstrap_methods.foreach { bm =>
                        writeShort(bm.method_ref)
                        writeShort(bm.arguments.length)
                        bm.arguments.foreach { bma => writeShort(bma.cp_ref) }
                    }

                case as: Annotations_attribute =>
                    // Handles:
                    // RuntimeVisibleAnnotations_attribute
                    // RuntimeInvisibleAnnotations_attribute
                    writeShort(as.annotations.size)
                    as.annotations.foreach { serialize(_) }

                case a: LocalVariableTypeTable_attribute =>
                    val length = a.local_variable_type_table.size
                    writeShort(length)
                    a.local_variable_type_table.foreach { lvtte =>
                        writeShort(lvtte.start_pc)
                        writeShort(lvtte.length)
                        writeShort(lvtte.name_index)
                        writeShort(lvtte.signature_index)
                        writeShort(lvtte.index)
                    }

                case a: LocalVariableTable_attribute =>
                    val length = a.local_variable_table.size
                    writeShort(length)
                    a.local_variable_table.foreach { lvte =>
                        writeShort(lvte.start_pc)
                        writeShort(lvte.length)
                        writeShort(lvte.name_index)
                        writeShort(lvte.descriptor_index)
                        writeShort(lvte.index)
                    }

                case a: LineNumberTable_attribute =>
                    val length = a.line_number_table.size
                    writeShort(length)
                    a.line_number_table.foreach { lnte =>
                        writeShort(lnte.start_pc)
                        writeShort(lnte.line_number)
                    }

                case e: EnclosingMethod_attribute =>
                    writeShort(e.class_index)
                    writeShort(e.method_index)

                case c: Code_attribute =>
                    import c._
                    writeShort(max_stack)
                    writeShort(max_locals)
                    val code_length = code.instructions.length
                    writeInt(code_length)
                    out.write(code.instructions, 0, code_length)
                    writeShort(exceptionTable.length)
                    exceptionTable.foreach { ex =>
                        writeShort(ex.start_pc)
                        writeShort(ex.end_pc)
                        writeShort(ex.handler_pc)
                        writeShort(ex.catch_type)
                    }
                    writeShort(attributes.length)
                    attributes foreach { a => serialize(a) }

                case e: Exceptions_attribute =>
                    writeShort(e.exception_index_table.size)
                    e.exception_index_table.foreach(writeShort)

                case i: InnerClasses_attribute =>
                    import i._
                    writeShort(classes.size)
                    classes foreach { c =>
                        writeShort(c.inner_class_info_index)
                        writeShort(c.outer_class_info_index)
                        writeShort(c.inner_name_index)
                        writeShort(c.inner_class_access_flags)
                    }

                case a: SourceDebugExtension_attribute =>
                    out.write(a.debug_extension, 0, attribute_length)

                case a: AnnotationDefault_attribute => serialize(a.element_value)

                case a: SourceFile_attribute        => writeShort(a.sourceFile_index)
                case a: Signature_attribute         => writeShort(a.signature_index)
                case a: ConstantValue_attribute     => writeShort(a.constantValue_index)

                case _: Deprecated_attribute        => // nothing more to do
                case _: Synthetic_attribute         => // nothing more to do

                case a: Module_attribute =>
                    writeShort(a.module_name_index)
                    writeShort(a.module_flags)
                    writeShort(a.module_version_index)

                    writeShort(a.requires.length)
                    a.requires foreach { r =>
                        writeShort(r.requires_index)
                        writeShort(r.requires_flags)
                        writeShort(r.requires_version_index)
                    }

                    writeShort(a.exports.length)
                    a.exports foreach { e =>
                        writeShort(e.exports_index)
                        writeShort(e.exports_flags)
                        writeShort(e.exports_to_index_table.length)
                        e.exports_to_index_table.foreach(writeShort)
                    }

                    writeShort(a.opens.length)
                    a.opens foreach { o =>
                        writeShort(o.opens_index)
                        writeShort(o.opens_flags)
                        writeShort(o.opens_to_index_table.length)
                        o.opens_to_index_table.foreach(writeShort)
                    }

                    writeShort(a.uses.length)
                    a.uses.foreach(writeShort)

                    writeShort(a.provides.length)
                    a.provides foreach { p =>
                        writeShort(p.provides_index)
                        writeShort(p.provides_with_index_table.length)
                        p.provides_with_index_table.foreach(writeShort)
                    }

                case a: ModuleMainClass_attribute =>
                    writeShort(a.main_class_index)

                case a: ModulePackages_attribute =>
                    writeShort(a.package_index_table.length)
                    a.package_index_table.foreach(writeShort)

                case a: NestHost_attribute =>
                    writeShort(a.host_class_index)

                case a: NestMembers_attribute =>
                    writeShort(a.classes_array.length)
                    a.classes_array.foreach(writeShort)

                case a: Record_attribute =>
                    writeShort(a.components.length)
                    a.components foreach { c =>
                        writeShort(c.name_index)
                        writeShort(c.descriptor_index)
                        writeShort(c.attributes.length)
                        c.attributes foreach RichAttribute.write
                    }

                case a: PermittedSubclasses_attribute =>
                    writeShort(a.permitted_subclasses.length)
                    a.permitted_subclasses.foreach(writeShort)

                case a: Unknown_attribute => out.write(a.info, 0, a.info.length)
            }
        }
    }

    implicit object RichFieldInfo extends ClassFileElement[Field_Info] {
        def write(
            f: Field_Info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import f._
            import out._
            writeShort(access_flags)
            writeShort(name_index)
            writeShort(descriptor_index)
            writeShort(attributes.size)
            attributes.foreach(serializeAs[Attribute])
        }
    }

    implicit object RichMethodInfo extends ClassFileElement[Method_Info] {
        def write(
            m: Method_Info
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import m._
            import out._
            writeShort(access_flags)
            writeShort(name_index)
            writeShort(descriptor_index)
            writeShort(attributes.size)
            attributes.foreach(serializeAs[Attribute])
        }
    }

    implicit object RichClassFile extends ClassFileElement[ClassFile] {

        def write(
            classFile: ClassFile
        )(
            implicit
            out: DataOutputStream, segmentInformation: (String, Int) => Unit
        ): Unit = {
            import classFile._
            import out._
            implicit val cp: Constant_Pool = classFile.constant_pool
            writeInt(org.opalj.bi.ClassFileMagic)
            writeShort(minor_version)
            writeShort(major_version)
            segmentInformation("ClassFileMetaInformation", out.size)

            writeShort(cp.length)
            // OLD cp.tail.filter(_ ne null).foreach(serialize(_))
            val cpIt = cp.iterator
            cpIt.next()
            cpIt.filter(_ ne null).foreach(cpe => serialize(cpe))
            segmentInformation("ConstantPool", out.size)

            writeShort(access_flags)
            segmentInformation("ClassAccessFlags", out.size)

            writeShort(this_class)
            writeShort(super_class)
            writeShort(interfaces.size)
            interfaces.foreach { writeShort }
            segmentInformation("TypeInformation", out.size)

            writeShort(fields.size)
            fields.foreach { serializeAs[Field_Info] }
            segmentInformation("Fields", out.size)

            writeShort(methods.size)
            methods foreach { m =>
                serialize(m)
                if ((ACC_STRICT.mask & m.access_flags) != 0 && (classFile.major_version < 46 || classFile.major_version > 60)) {
                    OPALLogger.warn("assembler", s"Writing out ACC_STRICT flag for a method in a classfile of version ${classFile.major_version}, which is not interpreted in class files of version < 46 or > 60")
                }
                segmentInformation("Method: "+cp(m.name_index).toString, out.size)
            }
            segmentInformation("Methods", out.size)

            writeShort(attributes.size)
            attributes foreach { serializeAs[Attribute] }
            segmentInformation("ClassFileAttributes", out.size)
        }
    }

    /**
     * `serializeAs` makes it possible to specify the object type of the given parameter `t` and
     * that type will then be used to pick up the implicit class file element value.
     */
    def serializeAs[T](
        t: AnyRef
    )(
        implicit
        out:                DataOutputStream,
        segmentInformation: (String, Int) => Unit,
        cfe:                ClassFileElement[T]
    ): Unit = {
        cfe.write(as[T](t))
    }

    /**
     * @note    You should use serialize if the concrete/required type of the given parameter is
     *          available/can be automatically inferred by the Scala compiler.
     */
    def serialize[T: ClassFileElement](
        t: T
    )(
        implicit
        out:                DataOutputStream,
        segmentInformation: (String, Int) => Unit
    ): Unit = {
        implicitly[ClassFileElement[T]].write(t)
    }

    /**
     * @param   segmentInformation A function that will be called back to provide information about
     *          the segment that was just written.
     *          This is particularly useful when debugging the serializer to determine which
     *          segments were successfully/completely written.
     */
    def apply(
        classFile:          ClassFile,
        segmentInformation: (String, Int) => Unit = (segmentInformation, bytesWritten) => ()
    ): Array[Byte] = {
        val data = new ByteArrayOutputStream(classFile.size)
        val out = new DataOutputStream(data)
        serialize(classFile)(RichClassFile, out, segmentInformation)
        out.flush()
        data.toByteArray
    }

}
