/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.LLVMContextCreate
import org.bytedeco.llvm.global.LLVM.LLVMCreateMemoryBufferWithContentsOfFile
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage
import org.bytedeco.llvm.global.LLVM.LLVMParseIRInContext

/**
 * This object provides an utility function to read LLVM IR files
 *
 * @author Marc Clement
 */
object Reader {

    /**
     * Parse the LLVM IR module from the given path.
     *
     * @param path the path from which to read the module from
     * @return the read Module or None if there was a problem
     */
    def readIR(path: String): Option[Module] = {
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
        if (LLVMParseIRInContext(context, file_buffer, module, out_message) != 0) {
            println("Failed to parse file: " + out_message.getString)
            LLVMDisposeMessage(out_message)
            return None
        }
        Some(Module(module))
    }
}
