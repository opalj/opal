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
package de.tud.cs.st
package bat
package resolved
package dependency
package checking

import reader.Java6Framework

/**
 * Demonstrates how to check BAT's fundamental architectural properties.
 *
 * @author Michael Eichberg
 */
object BATArchitecture extends Specification with App {

    println("Checking BAT's architecture")

    'Root{ "de.tud.cs.st.bat.*" }

    'canonical{ "de.tud.cs.st.bat.canonical.*" }

    'canonical_reader{ "de.tud.cs.st.bat.canonical.reader.*" }

    'reader{ "de.tud.cs.st.bat.reader.*" }

    'resolved_representation{ "de.tud.cs.st.bat.resolved.**" }

    'resolved_representation_reader_implementation {
        "de.tud.cs.st.bat.resolved.reader.**" except "de.tud.cs.st.bat.resolved.reader.Java6Framework*"
        }

   // 'support{ "de.tud.cs.st.util.**" union "de.tud.cs.st.prolog.*" }

    'util{ "de.tud.cs.st.util.**" }

    'prolog{ "de.tud.cs.st.prolog.*" }

    'empty{ Nothing }

    'demo_code{ "de.tud.cs.st.bat.LoadClassFilesTest*" }

    'resolved_representation_reader_implementation allows_incoming_dependencies_from 'demo_code

    //only('empty) is_allowed_to_depend_on 'prolog

    //only('util) is_allowed_to_depend_on 'reader

    analyze(List(Directory("build/class")))

    println("Finished checking BAT's architecture.")
}