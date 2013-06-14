/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.resolved
package reader

import scala.reflect.ClassTag

import de.tud.cs.st.bat.reader.InnerClasses_attributeReader

/**
  * The factory methods to create inner classes attributes and entries.
  *
  * @author Michael Eichberg
  */
trait InnerClasses_attributeBinding
        extends InnerClasses_attributeReader
        with ConstantPoolBinding
        with AttributeBinding {

    type InnerClasses_attribute = de.tud.cs.st.bat.resolved.InnerClassTable
    type InnerClassesEntry = de.tud.cs.st.bat.resolved.InnerClass
    val InnerClassesEntryManifest: ClassTag[InnerClassesEntry] = implicitly

    def InnerClasses_attribute(attribute_name_index: Constant_Pool_Index,
                               inner_classes: InnerClasses)(
                                   implicit constant_pool: Constant_Pool): InnerClasses_attribute =
        new InnerClasses_attribute(inner_classes)

    def InnerClassesEntry(inner_class_info_index: Constant_Pool_Index,
                          outer_class_info_index: Constant_Pool_Index,
                          inner_name_index: Constant_Pool_Index,
                          inner_class_access_flags: Int)(implicit cp: Constant_Pool) = {
        new InnerClassesEntry(
            inner_class_info_index.asObjectType,
            if (outer_class_info_index == 0) None else Some(outer_class_info_index.asObjectType),
            if (inner_name_index == 0) None else Some(inner_name_index.asString),
            inner_class_access_flags
        )
    }

}


