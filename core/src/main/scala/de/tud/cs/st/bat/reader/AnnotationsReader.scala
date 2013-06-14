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
package de.tud.cs.st.bat.reader

import java.io.DataInputStream

import scala.reflect.ClassTag

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
 * Defines a template method to read in a set of annotations. This
 * reader is intended to be used in conjunction with the
 * Runtime(In)Visible(Parameter)Annotations_attributeReaders.
 *
 * ''' From the Specification'''
 *
 * <pre>
 * annotation {
 * 	u2 type_index;
 * 	u2 num_element_value_pairs;
 * 	{	u2 element_name_index;
 * 		element_value value;
 * 	}	element_value_pairs[num_element_value_pairs]
 * }
 * </pre>
 *
 * @author Michael Eichberg
 */
trait AnnotationsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type Annotation
    implicit val AnnotationManifest: ClassTag[Annotation]

    type ElementValuePairs

    def ElementValuePairs(in: DataInputStream, cp: Constant_Pool): ElementValuePairs

    def Annotation(type_index: Constant_Pool_Index,
                   element_value_pairs: ElementValuePairs)(
                       implicit constant_pool: Constant_Pool): Annotation

    //
    // IMPLEMENTATION
    //

    type Annotations = IndexedSeq[Annotation]

    def Annotations(in: DataInputStream, cp: Constant_Pool): Annotations = {
        /*
		repeat(in.readUnsignedShort) {
			Annotation(in,cp)
		}

		The code given below (Scala 2.8) is much faster than the code seen above,
		if we have a loop with few repetitions.
		*/
        val count = in.readUnsignedShort
        val annotations = new Array[Annotation](count)
        var i = 0
        while (i < count) {
            annotations(i) = Annotation(in, cp)
            i += 1
        }
        annotations
    }

    def Annotation(in: DataInputStream, cp: Constant_Pool): Annotation = {
        Annotation(in.readUnsignedShort, ElementValuePairs(in, cp))(cp)
    }
}
