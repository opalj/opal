/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import scala.collection.mutable

import org.opalj.br.instructions.INCOMPLETE_LDC
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.LoadInt
import org.opalj.br.instructions.LoadFloat
import org.opalj.br.instructions.LoadString
import org.opalj.br.instructions.LoadClass
import org.opalj.br.instructions.LoadDynamic
import org.opalj.br.instructions.LoadMethodHandle
import org.opalj.br.instructions.LoadMethodType

/**
 * This class can be used to (re)build a [[org.opalj.br.ClassFile]]'s constant pool.
 *
 * @note    The builder will try its best to create a valid constant pool (w.r.t. the overall size
 *          and size of the indexes). Issues will be reported.
 *          Use the factory method defined by the companion object [[ConstantsBuffer$]] to
 *          create an instance and to get information about the requirements.
 * @author  Andre Pacak
 * @author  Michael Eichberg
 */
class ConstantsBuffer private (
        private var nextIndex:    Int,
        private val constantPool: mutable.Map[Constant_Pool_Entry, Constant_Pool_Index] // IMPROVE[L3] Use ObjectToIntMap
) extends ConstantsPoolLike {

    private[this] val bootstrapMethods = new BootstrapMethodsBuffer()
    private[this] var bootstrapMethodAttributeNameIndex: Int = _

    private[this] def getOrElseUpdate(cpEntry: Constant_Pool_Entry, entry_size: Int): Int = {
        constantPool.getOrElseUpdate(
            cpEntry,
            {
                val index = nextIndex
                nextIndex += entry_size
                index
            }
        )
    }

    @throws[ConstantPoolException]
    private[this] def validateIndex(index: Int, requiresUByteIndex: Boolean): Int = {
        if (requiresUByteIndex && index > UByte.MaxValue) {
            val message = s"the constant pool index $index is larger than  ${UByte.MaxValue}"
            throw new ConstantPoolException(message)
        }

        validateUShortIndex(index)
    }

    @throws[ConstantPoolException]
    private[this] def validateUShortIndex(index: Int): Int = {
        if (index > UShort.MaxValue) {
            val message = s"the constant pool index $index is larger than ${UShort.MaxValue}"
            throw new ConstantPoolException(message)
        }
        index
    }

    //
    // CPEntries (also) related to constant values / the LDC instruction.
    // (These entries may be referenced using an unsigned byte or unsigned short value.)
    //

    @throws[ConstantPoolException]
    def CPEClass(referenceType: ReferenceType, requiresUByteIndex: Boolean): Int = {
        val cpeUtf8 = CPEUtf8OfCPEClass(referenceType)
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Class_info(cpeUtf8), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEFloat(value: Float, requiresUByteIndex: Boolean): Int = {
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Float_info(ConstantFloat(value)), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEInteger(value: Int, requiresUByteIndex: Boolean): Int = {
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Integer_info(ConstantInteger(value)), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEString(value: String, requiresUByteIndex: Boolean): Int = {
        val cpEntryIndex = getOrElseUpdate(CONSTANT_String_info(CPEUtf8(value)), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEMethodHandle(methodHandle: MethodHandle, requiresUByteIndex: Boolean): Int = {
        val (tag, cpRefIndex) = CPERefOfCPEMethodHandle(methodHandle)
        val cpEntryIndex = getOrElseUpdate(CONSTANT_MethodHandle_info(tag, cpRefIndex), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex)
    }

    @throws[ConstantPoolException]
    def CPEMethodType(descriptor: MethodDescriptor, requiresUByteIndex: Boolean): Int = {
        val jvmDescriptor = descriptor.toJVMDescriptor
        val cpEntry = CONSTANT_MethodType_info(getOrElseUpdate(CONSTANT_Utf8_info(jvmDescriptor), 1))
        validateIndex(getOrElseUpdate(cpEntry, 1), requiresUByteIndex)
    }

    //
    // OTHER CPEntries
    // (These entries are always referenced using an unsigned short value.)
    //

    @throws[ConstantPoolException]
    def CPEDouble(value: Double): Int = {
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Double_info(ConstantDouble(value)), 2)
        validateUShortIndex(cpEntryIndex)
    }

    @throws[ConstantPoolException]
    def CPELong(value: Long): Int = {
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Long_info(ConstantLong(value)), 2)
        validateUShortIndex(cpEntryIndex)
    }

    @throws[ConstantPoolException]
    def CPEUtf8(value: String): Int = {
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Utf8_info(value), 1)
        validateUShortIndex(cpEntryIndex)
    }

    @throws[ConstantPoolException]
    def CPENameAndType(name: String, tpe: String): Int = {
        val nameIndex = CPEUtf8(name)
        val typeIndex = CPEUtf8(tpe)
        validateUShortIndex(getOrElseUpdate(CONSTANT_NameAndType_info(nameIndex, typeIndex), 1))
    }

    @throws[ConstantPoolException]
    def CPEFieldRef(
        objectType: ObjectType,
        fieldName:  String,
        fieldType:  String
    ): Int = {
        val nameAndTypeRef = CPENameAndType(fieldName, fieldType)
        val cpeClass = CPEClass(objectType, requiresUByteIndex = false)
        val cpFieldRef = CONSTANT_Fieldref_info(cpeClass, nameAndTypeRef)
        validateUShortIndex(getOrElseUpdate(cpFieldRef, 1))
    }

    @throws[ConstantPoolException]
    def CPEMethodRef(
        referenceType: ReferenceType,
        methodName:    String,
        descriptor:    MethodDescriptor
    ): Int = {
        val jvmDescriptor = descriptor.toJVMDescriptor
        val class_index = CPEClass(referenceType, requiresUByteIndex = false)
        val name_and_type_index = CPENameAndType(methodName, jvmDescriptor)
        val cpIndex = getOrElseUpdate(CONSTANT_Methodref_info(class_index, name_and_type_index), 1)
        validateUShortIndex(cpIndex)
    }

    @throws[ConstantPoolException]
    def CPEInterfaceMethodRef(
        objectType: ReferenceType,
        methodName: String,
        descriptor: MethodDescriptor
    ): Int = {
        val jvmDescriptor = descriptor.toJVMDescriptor
        val class_index = CPEClass(objectType, requiresUByteIndex = false)
        val name_and_type_index = CPENameAndType(methodName, jvmDescriptor)
        val cpMethodRef = CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)
        validateUShortIndex(getOrElseUpdate(cpMethodRef, 1))
    }

    @throws[ConstantPoolException]
    def CPEInvokeDynamic(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor
    ): Int = {
        val jvmDescriptor = descriptor.toJVMDescriptor

        if (bootstrapMethodAttributeNameIndex == 0)
            bootstrapMethodAttributeNameIndex = CPEUtf8(bi.BootstrapMethodsAttribute.Name)

        //need to build up bootstrap_methods
        var indexOfBootstrapMethod = bootstrapMethods.indexOf(bootstrapMethod)
        if (indexOfBootstrapMethod == -1) {
            bootstrapMethods += bootstrapMethod
            CPEMethodHandle(bootstrapMethod.handle, requiresUByteIndex = false)
            bootstrapMethod.arguments.foreach(CPEntryForBootstrapArgument)
            indexOfBootstrapMethod = bootstrapMethods.size - 1
        }
        val cpNameAndTypeIndex = CPENameAndType(name, jvmDescriptor)
        getOrElseUpdate(CONSTANT_InvokeDynamic_info(indexOfBootstrapMethod, cpNameAndTypeIndex), 1)
    }

    //
    // Java9+ CPEntries
    //

    @throws[ConstantPoolException]
    def CPEModule(name: String): Int = {
        val cpeUtf8 = CPEUtf8(name)
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Module_info(cpeUtf8), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex = false)
    }

    @throws[ConstantPoolException]
    def CPEPackage(name: String): Int = {
        val cpeUtf8 = CPEUtf8(name)
        val cpEntryIndex = getOrElseUpdate(CONSTANT_Package_info(cpeUtf8), 1)
        validateIndex(cpEntryIndex, requiresUByteIndex = false)
    }

    //
    // Java11+ CPEntries
    //

    @throws[ConstantPoolException]
    def CPEDynamic(
        bootstrapMethod:    BootstrapMethod,
        name:               String,
        descriptor:         FieldType,
        requiresUByteIndex: Boolean
    ): Int = {
        CPEDynamic(
            bootstrapMethod,
            name,
            descriptor,
            requiresUByteIndex,
            createBootstrapArgumentEntries = true
        )
    }

    @throws[ConstantPoolException]
    def CPEDynamic(
        bootstrapMethod:                BootstrapMethod,
        name:                           String,
        descriptor:                     FieldType,
        requiresUByteIndex:             Boolean,
        createBootstrapArgumentEntries: Boolean
    ): Int = {
        val jvmDescriptor = descriptor.toJVMTypeName

        if (bootstrapMethodAttributeNameIndex == 0)
            bootstrapMethodAttributeNameIndex = CPEUtf8(bi.BootstrapMethodsAttribute.Name)

        //need to build up bootstrap_methods
        var indexOfBootstrapMethod = bootstrapMethods.indexOf(bootstrapMethod)
        if (indexOfBootstrapMethod == -1) {
            bootstrapMethods += bootstrapMethod
            CPEMethodHandle(bootstrapMethod.handle, requiresUByteIndex = false)
            if (createBootstrapArgumentEntries)
                bootstrapMethod.arguments.foreach(CPEntryForBootstrapArgument)
            indexOfBootstrapMethod = bootstrapMethods.size - 1
        }
        val cpNameAndTypeIndex = CPENameAndType(name, jvmDescriptor)

        validateIndex(
            getOrElseUpdate(CONSTANT_Dynamic_info(indexOfBootstrapMethod, cpNameAndTypeIndex), 1),
            requiresUByteIndex
        )
    }

    /**
     * Converts this constant pool buffer to an array and also returns an immutable view of the
     * current state of the constants pool. This in particular enables the creation of the
     * `BootstrapMethodTable` attribute - iff the table is not empty! If the table is empty,
     * it is not guaranteed that the name of the `BootstrapMethodTable` attribute is defined by
     * the constant pool, but there is also no need to add the attribute.
     */
    def build: (Array[Constant_Pool_Entry], ConstantsPool) = {
        val cp = new Array[Constant_Pool_Entry](nextIndex)
        constantPool foreach { e =>
            val (cpe, index) = e
            cp(index) = cpe
        }
        (cp, new ConstantsPool(constantPool.toMap, bootstrapMethods.toIndexedSeq))
    }
}

/**
 * Factory methods and helper methods to create a valid [[ConstantsBuffer]].
 *
 * @author  Michael Eichberg
 */
object ConstantsBuffer {

    def collectLDCs(classFile: ClassFile): Set[LDC[_]] = {
        val allLDC = for {
            method <- classFile.methods.iterator
            if method.body.isDefined
            ldc <- method.body.get.instructionIterator.collect { case ldc: LDC[_] => ldc }
        } yield {
            ldc
        }
        allLDC.toSet
    }

    @throws[ConstantPoolException]
    def getOrCreateCPEntry(ldc: LDC[_])(implicit constantsBuffer: ConstantsBuffer): Int = {
        import constantsBuffer._
        ldc match {
            case LoadInt(value) =>
                CPEInteger(value, requiresUByteIndex = true)
            case LoadFloat(value) =>
                CPEFloat(value, requiresUByteIndex = true)
            case LoadString(value) =>
                CPEString(value, requiresUByteIndex = true)
            case LoadClass(value) =>
                CPEClass(value, requiresUByteIndex = true)
            // Java 7+
            case LoadMethodHandle(value) =>
                CPEMethodHandle(value, requiresUByteIndex = true)
            case LoadMethodType(value) =>
                CPEMethodType(value, requiresUByteIndex = true)
            // JAVA 11+
            case LoadDynamic(bootstrapMethod, name, descriptor) =>
                CPEDynamic(
                    bootstrapMethod,
                    name,
                    descriptor,
                    requiresUByteIndex = true,
                    createBootstrapArgumentEntries = false
                )
            case INCOMPLETE_LDC =>
                throw ConstantPoolException("incomplete LDC")
        }
    }

    /**
     * Creates a new [[ConstantsBuffer]] which is already preinitialized to contain the
     * constants defined by the respective [[org.opalj.br.instructions.LDC]] instructions.
     *
     * This is necessary to ensure that these entries are assigned values less than 255, because
     * the constant pool reference used by LDC instructions is just one unsigned byte.
     *
     * @note    If a class has more than 254 unique constants and all of them use simple `LDC` (not
     *          LDC_W) instructions, a [[ConstantPoolException]] will be thrown.
     *          Furthermore, a [[ConstantPoolException]] is thrown if the maximum size of the pool
     *          (65535 entries) is exceeded.
     *
     * @param   ldcs The set of unique LDC instructions. For each constant referred to by an LDC
     *          instruction we (need to) create the required `ConstantPool` entry right away to
     *          ensure the index is an unsigned byte value.
     *          To collect a [[org.opalj.br.ClassFile]]'s ldc instructions use [[collectLDCs]].
     */
    @throws[ConstantPoolException]("if it is impossible to create a valid constant pool")
    def apply(ldcs: Set[LDC[_]]): ConstantsBuffer = {
        // IMPROVE Use Object2IntMap..
        val buffer = mutable.HashMap.empty[Constant_Pool_Entry, Constant_Pool_Index]
        //the first item is null because the constant_pool starts with the index 1
        buffer(null) = 0

        /*
        The basic idea is to first add the referenced constant pool entries (which always use two
        byte references) and afterwards create the LDC related constant pool entries.
        For the first phase the pool's nextIndex is set to the first index that is required by the
        referenced entries. After that, nextIndex is set to 1 and all LDC related entries
        are created.

        One exception are the CPClass entries: they strictly need to be processed first
        to ensure that – indirect references (e.g., due to a method handle) - never lead to invalid
        indexes!

        The second exception are bootstrap method attributes for dynamic constants. They can refer
        to dynamic constants (using two byte references), but the same constant pool entries might
        be used by a one byte reference from an LDC. Thus, we first create the LDC related entries,
        then create the bootstrap argument entries at the end.
        */
        val (ldClasses, ldOtherConstants) = ldcs partition { ldc => ldc.isInstanceOf[LoadClass] }

        // 1. let's add the referenced CONSTANT_UTF8 entries required by LoadClass instructions
        var nextIndexAfterLDCRelatedEntries = 1 + ldcs.size
        implicit val constantsBuffer = new ConstantsBuffer(nextIndexAfterLDCRelatedEntries, buffer)
        import constantsBuffer._
        ldClasses foreach { ldc => CPEUtf8OfCPEClass(ldc.asInstanceOf[LoadClass].value) }
        nextIndexAfterLDCRelatedEntries = constantsBuffer.nextIndex

        // 2. let's add the CONSTANT_Class_Info entries
        constantsBuffer.nextIndex = 1
        ldClasses.foreach(ldc => CPEClass(ldc.asInstanceOf[LoadClass].value, true))
        val nextLDCIndex = constantsBuffer.nextIndex

        // 3.  process all referenced constant pool entries (UTF8, NAME_AND_TYPE, FIELDREF,...)
        constantsBuffer.nextIndex = nextIndexAfterLDCRelatedEntries
        val bootstrapMethods = mutable.Buffer.empty[BootstrapMethod]
        ldOtherConstants foreach {
            case LoadMethodType(value)   => CPEUtf8(value.toJVMDescriptor)
            case LoadMethodHandle(value) => CPERefOfCPEMethodHandle(value)
            case LoadString(value)       => CPEUtf8(value)
            case LoadDynamic(bootstrapMethod, name, descriptor) =>
                CPEUtf8(bi.BootstrapMethodsAttribute.Name)
                CPEMethodHandle(bootstrapMethod.handle, requiresUByteIndex = false)
                CPENameAndType(name, descriptor.toJVMTypeName)
                bootstrapMethods += bootstrapMethod
            case _: LoadFloat => // does not reference other entries
            case _: LoadInt   => // does not reference other entries
            case that         => throw new UnknownError(s"unknown LDC: $that")
        }
        nextIndexAfterLDCRelatedEntries = constantsBuffer.nextIndex

        // 4.   Add all other CONSTANT_(INTEGER|FLOAT|STRING|METHODHANDLE|METHODTYPE) entries
        constantsBuffer.nextIndex = nextLDCIndex
        ldOtherConstants.foreach(getOrCreateCPEntry)

        // 5.   Correct nextIndex to point to the first not used index; all previous indexes
        //      are now used! Then create the constant pool entries for bootstrap arguments
        constantsBuffer.nextIndex = nextIndexAfterLDCRelatedEntries
        bootstrapMethods.foreach(_.arguments.foreach(CPEntryForBootstrapArgument))

        assert(
            buffer.size == constantsBuffer.nextIndex,
            "constant pool contains holes:\n\t"+
                ldcs.mkString("LDCs={", ", ", "}\n\t") +
                buffer.toList.map(_.swap).sortBy(e => e._1).mkString("Buffer=[\n\t", "\n\t", "]\n")
        )
        constantsBuffer
    }
}
