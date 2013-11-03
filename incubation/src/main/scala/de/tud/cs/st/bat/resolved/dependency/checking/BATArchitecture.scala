/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package dependency
package checking

/**
 * Specification of BAT's architecture.
 *
 * @author Michael Eichberg
 */
object BATArchitecture extends Specification with App {

    println("Checking BAT's architecture")

    //
    // Architectural entities

    'Utils_Base{ "de.tud.cs.st.util.*" }

    'Utils_Debug{ "de.tud.cs.st.util.debug.**" }

    'Utils_Graph{ "de.tud.cs.st.util.graph.**" }

    'Utils{
        'Utils_Base and
            'Utils_Debug and
            'Utils_Graph
    }

    //
    // Rules

    'Utils_Debug is_only_allowed_to_use ('Utils_Base)
    
    'Utils_Graph is_only_allowed_to_use ('Utils_Graph)

    'Utils is_only_allowed_to_use empty
    
    //    
    //    
    //    
    //    'Integration_Tests{
    //        "de.tud.cs.st.bat.Architecture*" and
    //            "de.tud.cs.st.bat.BATSuite*" and
    //            "de.tud.cs.st.bat.LoadClassFilesTest*"
    //    }
    //
    //    'Demo_Code{
    //        "de.tud.cs.st.bat.resolved.AssociateUniqueIDs*" and
    //            "de.tud.cs.st.bat.resolved.ClassFileInformation*" and
    //            "de.tud.cs.st.bat.resolved.SimpleCheckers*" and
    //            "de.tud.cs.st.bat.resolved.analyses.Main*" and
    //            "de.tud.cs.st.bat.resolved.dependency.DependencyMatrix*" and
    //            "de.tud.cs.st.bat.resolved.dependency.checking.BATArchitecture*"
    //    }
    //
    //    'BAT_Commons{ "de.tud.cs.st.bat.*" except 'Integration_Tests }
    //
    //    'Generic_Reader{ "de.tud.cs.st.bat.reader.**" }
    //
    //    'Canonical_Representation{ "de.tud.cs.st.bat.canonical.**" }
    //
    //    'Canonical_Representation_Interface{ "de.tud.cs.st.bat.canonical.*" }
    //
    //    'Canonical_Representation_Reader{ "de.tud.cs.st.bat.canonical.reader.*" }
    //
    //    'Resolved_Representation{ "de.tud.cs.st.bat.resolved.**" except 'Demo_Code }
    //
    //    'Resolved_Representation_Interface{ "de.tud.cs.st.bat.resolved.*" }
    //
    //    'Resolved_Representation_Reader_Interface{
    //        "de.tud.cs.st.bat.resolved.reader.Java6Framework*"
    //    }
    //
    //    'Resolved_Representation_Reader_Implementation{
    //        "de.tud.cs.st.bat.resolved.reader.**" except 'Resolved_Representation_Reader_Interface
    //    }
    //
    //    'Resolved_Representation_Dependency{ "de.tud.cs.st.bat.resolved.dependency.**" except 'Demo_Code }
    //
    //    'Resolved_Representation_Dependency_Checking{ "de.tud.cs.st.bat.resolved.dependency.checking.**" except 'Demo_Code }
    //
    //    'Resolved_Representation_Analyses{ "de.tud.cs.st.bat.resolved.analyses.**" except 'Demo_Code }
    //
    //    'Prolog{ "de.tud.cs.st.prolog.*" }
    //
    //    //
    //    // Architectural rules:
    //
    //    'BAT_Commons is_only_allowed_to_use ('Utils, 'Prolog)
    //
    //    'Integration_Tests allows_incoming_dependencies_from empty
    //
    //    'Canonical_Representation is_only_allowed_to_use ('BAT_Commons, 'Generic_Reader)
    //
    //    'Resolved_Representation_Interface is_only_allowed_to_use (
    //        'BAT_Commons, 'Resolved_Representation_Reader_Interface, 'Utils, 'Prolog
    //    )
    //
    //    'Resolved_Representation_Reader_Implementation allows_incoming_dependencies_from (
    //        'Resolved_Representation_Reader_Interface,
    //        'Integration_Tests
    //    )
    //
    //    'Resolved_Representation_Dependency allows_incoming_dependencies_from ('Integration_Tests, 'Demo_Code)
    //
    //    'Resolved_Representation_Analyses allows_incoming_dependencies_from (
    //        'Integration_Tests, 'Demo_Code,
    //        'Resolved_Representation_Dependency_Checking
    //    )
    //
    //    'Demo_Code allows_incoming_dependencies_from empty

    //
    // Code basis that is to be analyzed

    analyze(Directory(".."))

    //println(ensembleToString('Integration_Tests))

    println("Finished checking BAT's architecture.")

}