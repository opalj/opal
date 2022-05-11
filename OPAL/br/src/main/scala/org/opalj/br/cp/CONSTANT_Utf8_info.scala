/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.AttributeParent
import org.opalj.bi.AttributesParent
import org.opalj.br.reader.SignatureParser
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant string value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Utf8_info(value: String) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Utf8_ID

    override def asString = value

    private[this] var methodDescriptor: MethodDescriptor = null // to cache the result
    override def asMethodDescriptor = {
        if (methodDescriptor eq null) { methodDescriptor = MethodDescriptor(value) };
        methodDescriptor
    }

    private[this] var fieldType: FieldType = null // to cache the result
    override def asFieldType = {
        if (fieldType eq null) { fieldType = FieldType(value) };
        fieldType
    }

    override def asFieldTypeSignature = {
        // should be called at most once => caching doesn't make sense
        SignatureParser.parseFieldTypeSignature(value)
    }

    override def asSignature(ap: AttributeParent): Signature = {
        // should be called at most once => caching doesn't make sense
        ap match {
            case AttributesParent.Field | AttributesParent.RecordComponent =>
                SignatureParser.parseFieldTypeSignature(value)
            case AttributesParent.ClassFile => SignatureParser.parseClassSignature(value)
            case AttributesParent.Method    => SignatureParser.parseMethodTypeSignature(value)
            case AttributesParent.Code =>
                val message = s"code attribute has an unexpected signature attribute: $value"
                throw new BytecodeProcessingFailedException(message)
        }
    }

    override def asConstantValue(cp: Constant_Pool): ConstantString = {
        // required to support annotations; should be called at most once
        // => caching doesn't make sense
        ConstantString(value)
    }
}
