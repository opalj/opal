#Overview
The folder `projects` contains Java projects (to be precise: the root packages of the projects) that serve as ***test fixtures*** for tests and therefore have to have a very specific bytecode layout! _The java files are compiled with the bundled Eclipse JDT 3.26.0 compiler automatically by the build script._

The fixtures are compiled by a sbt plugin that is part of the developer tools of OPAL. It resides in `DEVELOPING_OPAL/plugins/sbt-java-fixture-compiler`. The
(fixed) Eclipse JDT compiler version is configured over there. For more information, please refer to `DEVELOPING_OPAL/plugins/sbt-java-fixture-compiler/Readme.md`.

The generated files are found in:

    /OPAL/OPAL/<Subproject>/target/scala-2.11/resource_managed/test/<name of the project/package><compiler parameters>

If a project should be compiled using different compiler settings or requires an additional (shared) library put a `compiler.config` file in the project's folder. Each line starting with a dash "-" in that file (unless it starts with a `#`) is then used as a compiler configuration setting. For example, the following config file configures two compiler configurations.

	# We compile the sources once including all and once with no debug information:
	-g -8 -parameters -genericsignature
	-g:none -5

For the generated JAR archive the compiler parameters are concatenated (" " => "") and colons are replaced by equals signs: (':' => '='), unless the default options are used. In the latter case all parameters are omitted.

 > The target folder, the enconding (UTF-8) and the error messages style (emacs) are however
 > automatically configured and most not be specified.

 If the project requires some shared support library add a line with a `requires` statement that lists the name of the root package; e.g.:

    requires=annotations
