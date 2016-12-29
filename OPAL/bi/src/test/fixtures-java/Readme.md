#Overview
This folder contains Java projects (to be precise: the root packages of the projects) that serve as ***Test fixtures*** for tests and therefore have to have a very specific bytecode layout! _Hence, the files are compiled with the bundled Eclipse 4.6.1 compiler automatically by the build script._

The generated files are found in:

    /OPAL/OPAL/<Subproject>/target/scala-2.11/resource_managed/test/<name of the project/package><compiler parameters>
    
The compiler parameters are concatenated (" " ⇒ "") and colons are replaced by equals signs: (':' ⇒ '='), unless the default options are used. In the latter case the parameters are omitted.

If a project should be compiled using different compiler settings put a `compiler.config` file in the project's folder. Each line in that file (unless it starts with a `#`) is then used as a compiler configuration. For example, the following config file configures two compiler configurations.

	# We compile the sources once including all and once with no debug information:
	-g -8 -parameters -genericsignature
	-g:none -5 
    
    