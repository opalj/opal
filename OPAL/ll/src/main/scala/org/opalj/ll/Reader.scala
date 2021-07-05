/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.{LLVMContextRef, LLVMMemoryBufferRef, LLVMModuleRef}
import org.bytedeco.llvm.global.LLVM.{LLVMContextCreate, LLVMCreateMemoryBufferWithContentsOfFile, LLVMDisposeMessage, LLVMParseIRInContext}

object Reader {
    def readIR(path: String): Option[LLVMModuleRef] = {
        val file_buffer: LLVMMemoryBufferRef = new LLVMMemoryBufferRef()
        val path_pointer: BytePointer = new BytePointer(path)
        val out_message: BytePointer = new BytePointer()
        if (LLVMCreateMemoryBufferWithContentsOfFile(path_pointer, file_buffer, out_message) != 0) {
            System.err.println("Failed to load file: " + out_message.getString)
            LLVMDisposeMessage(out_message)
            return None
        }
        val context: LLVMContextRef = LLVMContextCreate()
        val module: LLVMModuleRef = new LLVMModuleRef()
        if (LLVMParseIRInContext(context, file_buffer, module, out_message) !=0 ) {
            println("Failed to parse file: " + out_message)
            LLVMDisposeMessage(out_message)
            return None
        }
        Some(module)
    }
}