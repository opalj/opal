/*
 * License (BSD Style License):
 * Copyright (c) 2012
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
package de.tud.cs.st.bat
package resolved
package ai

/**
 * @author Michael Eichberg
 */
object AI {

   /**
     * @param classFile Some class file.
     * @param method A non-abstract,non-native method of the given class file.
     * @param domain The abstract domain that is used during the interpretation.
     */
   def apply(classFile : ClassFile, method : Method)(implicit domain : Domain) : Array[MemoryLayout] = {
      val code = method.body.get.instructions
      val initialLocals = {
         var locals : IndexedSeq[Value] = new Array[Value](method.body.get.maxLocals)
         var localVariableIndex = 0

         if (!method.isStatic) {
            val thisType = classFile.thisClass
            locals = locals.updated(localVariableIndex, AReferenceTypeValue(thisType))
            localVariableIndex += 1 /*==thisType.computationalType.operandSize*/
         }

         for (parameterType ← method.descriptor.parameterTypes) {
            val ct = parameterType.computationalType
            locals = locals.updated(localVariableIndex, TypedValue(parameterType))
            localVariableIndex += ct.operandSize
         }
         locals
      }
      apply(code, initialLocals)
   }

   def apply(code : Array[Instruction], initialLocals : IndexedSeq[Value])(implicit domain : Domain) : Array[MemoryLayout] = {
      // true if the instruction with the respective program counter is already transformed
      val memoryLayouts = new Array[MemoryLayout](code.length)
      memoryLayouts(0) = new MemoryLayout(Nil, initialLocals)

      var worklist : List[Int /*program counter*/ ] = List(0)
      while (worklist.nonEmpty) {
         var pc = worklist.head
         worklist = worklist.tail
         val instruction = code(pc)
         val memoryLayout = memoryLayouts(pc)

         val newMemoryLayout = memoryLayout.update(instruction)

         // Go to the next instruction and store the memory layout 
         pc += 1
         while (pc < code.length && (code(pc) eq null)) pc += 1
         if (pc < code.length) {
            worklist = pc :: worklist
            memoryLayouts(pc) = newMemoryLayout
         }
      }

      memoryLayouts
   }
}
