name := "Bytecode Infrastructure"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Infrastructure")
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5"

/* The following task compiles those Java files to the class files (`.class`) that are used
 * by subsequent tests. To ensure that always the same class file is generated, we use the
 * Eclipse Java compiler (4.6.1); this version is fixed!
 */
// FOR DETAILS SEE ALSO: bi/src/test/fixtures-java/Readme.md

unmanagedResourceDirectories in Test += (sourceDirectory in Test).value / "fixtures-java"
resourceGenerators in Test += Def.task {
	import org.eclipse.jdt.core.compiler.batch.BatchCompiler
	import java.io.File
	import java.io.Writer
	import java.io.PrintWriter
	import java.nio.file.SimpleFileVisitor
	import java.nio.file.Path
	import java.nio.file.Files
	import java.nio.file.FileVisitResult
	import java.nio.file.attribute.FileTime
	import java.nio.file.attribute.BasicFileAttributes
	import scala.io.Source.fromFile

	// HELPER CLASSES

	class LogWriter(println : String => Unit) extends Writer {
	    override def flush(): Unit = {}
	    override def close(): Unit = ??? // not expected to be called
	    override def write(chars: Array[Char], offset: Int, length: Int): Unit = {
			println(new String(chars,offset,length))
	    }
	}

	class WasUpdatedFileVisitor(val archiveDate : FileTime) extends SimpleFileVisitor[Path] {

		var wasUpdated : Boolean = false // will be initialized as a sideeffect

		private def checkDate(nextFileAttributes : BasicFileAttributes) : FileVisitResult = {
			if(archiveDate.compareTo(nextFileAttributes.lastModifiedTime) < 0) {
				wasUpdated = true
	            FileVisitResult.TERMINATE
	    	}
		   	else {
				FileVisitResult.CONTINUE
		   	}
		}

		override def visitFile(
			path : Path,
			fileAttributes : BasicFileAttributes
		) : FileVisitResult = {
			checkDate(fileAttributes)
		}
		override def preVisitDirectory(
			path : Path,
			fileAttributes : BasicFileAttributes
		) : FileVisitResult = {
			checkDate(fileAttributes)
	    }
	}

	// IMPLEMENTATION OF THE TASK

	val s: TaskStreams = streams.value
	val log = s.log
	val std = new PrintWriter(new LogWriter((s : String) => log.info(s)))
	val err = new PrintWriter(new LogWriter((s : String) => log.error(s)))

	val resourceManagedFolder = (resourceManaged in Test).value
	val projectsFolder = (sourceDirectory in Test).value / "fixtures-java" / "projects"
	val supportFolder = (sourceDirectory in Test).value / "fixtures-java" / "support"
	val createdJARs = for {
			sourceFolder <- projectsFolder.listFiles
			if sourceFolder.isDirectory
			configFile = sourceFolder.getAbsoluteFile / "compiler.config"
		} yield {
			val hasConfigFile = configFile.exists
			val (supportLibraries,defaultConfigurationOptions) =
				if (hasConfigFile) {
					val (requires,configurationOptions) = fromFile(configFile).getLines.
						map(_.trim).
						filter(l => l.nonEmpty && !l.startsWith("#")).toList.
						partition(_.startsWith("requires"))
					(
						requires.
							map(librarySpec => librarySpec.substring(librarySpec.indexOf('=')+1)). /* support library name */
							map(libraryName => supportFolder / libraryName). /* support library folder */
							mkString(" "),
						configurationOptions
					)
				} else {
					("", Seq("-g -8 -parameters -genericsignature"))
				}

			for {configurationOptions <- defaultConfigurationOptions} yield {
				val selectedOptionsIdentification =
					if(hasConfigFile)
						configurationOptions.replace(" ","").replace(':','=')
					else
						""
				val targetFolder =
					resourceManagedFolder.getAbsoluteFile / (
						sourceFolder.getName + selectedOptionsIdentification
				 	)
				IO.createDirectory(targetFolder)
				val targetJAR = new File(targetFolder + ".jar")

				// Let's figure out if we actually have to run the compiler
				val requiresCompilation =
					!targetJAR.exists ||
					{
						val targetJARAsPath = Files.getLastModifiedTime(targetJAR.toPath)
						val wasUpdatedVisitor = new WasUpdatedFileVisitor(targetJARAsPath)
						Files.walkFileTree(sourceFolder.toPath,wasUpdatedVisitor)
						wasUpdatedVisitor.wasUpdated
					}
				if (requiresCompilation){
					val standardConfiguration = s"$sourceFolder $supportLibraries -d $targetFolder -Xemacs -encoding utf8 "
					val commandLine = s"$standardConfiguration $configurationOptions"
					log.info(s"Compiling test fixtures: $commandLine")
					if(!BatchCompiler.compile(commandLine,std, err, null)) {
						throw new IllegalStateException("Compiling the test fixtures failed")
					}

					val targetFolderLength = targetFolder.toString.length + 1
					val classFiles : Traversable[(File,String)] =
						(targetFolder ** "*.class").get map { classFile =>
							((classFile,classFile.toString.substring(targetFolderLength)))
						}

					log.info(
						classFiles.view.map(_._2).
							mkString(
								s"Creating archive $targetJAR for compiled test fixtrues:\n\t",
								"\n\t",
								"\n"
							)
					)
					IO.zip(classFiles,targetJAR)
					Some(targetJAR)
				} else {
					None
				}
			}
		}

	val createdFiles = createdJARs.flatten.flatten.toSeq // We only collect the created JARs
	if(createdFiles.nonEmpty)
		log.info(createdFiles.mkString("Created archives:\n\t","\n\t","\n"))
	else
		log.info("The test fixtures were already compiled.")

	createdFiles
}.taskValue
// Eclipse Compiler for Java(TM) v20160829-0950, 3.12.1
// Copyright IBM Corp 2000, 2015. All rights reserved.
//
//	Usage: <options> <source files | directories>
//	If directories are specified, then their source contents are compiled.
//	Possible options are listed below. Options enabled by default are prefixed
//	with '+'.
//
//	Classpath options:
//		 -cp -classpath <directories and ZIP archives separated by :>
//											specify location for application classes and sources.
//											Each directory or file can specify access rules for
//											types between '[' and ']' (e.g. [-X] to forbid
//											access to type X, [~X] to discourage access to type X,
//											[+p/X:-p/*] to forbid access to all types in package p
//											but allow access to p/X)
//		 -bootclasspath <directories and ZIP archives separated by :>
//											specify location for system classes. Each directory or
//											file can specify access rules for types between '['
//											and ']'
//		 -sourcepath <directories and ZIP archives separated by :>
//											specify location for application sources. Each directory
//											or file can specify access rules for types between '['
//											and ']'. Each directory can further specify a specific
//											destination directory using a '-d' option between '['
//											and ']'; this overrides the general '-d' option.
//											.class files created from source files contained in a
//											jar file are put in the user.dir folder in case no
//											general '-d' option is specified. ZIP archives cannot
//											override the general '-d' option
//		 -extdirs <directories separated by :>
//											specify location for extension ZIP archives
//		 -endorseddirs <directories separated by :>
//											specify location for endorsed ZIP archives
//		 -d <dir>							destination directory (if omitted, no directory is
//											created); this option can be overridden per source
//											directory
//		 -d none							generate no .class files
//		 -encoding <enc>					specify default encoding for all source files. Each
//											file/directory can override it when suffixed with
//											'['<enc>']' (e.g. X.java[utf8]).
//											If multiple default encodings are specified, the last
//											one will be used.
//
//	Compliance options:
//		 -1.3								use 1.3 compliance (-source 1.3 -target 1.1)
//		 -1.4								+ use 1.4 compliance (-source 1.3 -target 1.2)
//		 -1.5 -5 -5.0			 			use 1.5 compliance (-source 1.5 -target 1.5)
//		 -1.6 -6 -6.0			 			use 1.6 compliance (-source 1.6 -target 1.6)
//		 -1.7 -7 -7.0			 			use 1.7 compliance (-source 1.7 -target 1.7)
//		 -1.8 -8 -8.0			 			use 1.8 compliance (-source 1.8 -target 1.8)
//		 -source <version>					set source level: 1.3 to 1.8 (or 5, 5.0, etc)
//		 -target <version>					set classfile target: 1.1 to 1.8 (or 5, 5.0, etc)
//											cldc1.1 can also be used to generate the StackMap
//											attribute
//
//	Warning options:
//		 -deprecation		 				+ deprecation outside deprecated code (equivalent to
//											-warn:+deprecation)
//		 -nowarn -warn:none disable all warnings
//		 -nowarn:[<directories separated by :>]
//											specify directories from which optional problems should
//											be ignored
//		 -?:warn -help:warn display advanced warning options
//
//	Error options:
//		 -err:<warnings separated by ,> convert exactly the listed warnings to be reported as errors
//		 -err:+<warnings separated by ,> enable additional warnings to be reported as errors
//		 -err:-<warnings separated by ,> disable specific warnings to be reported as errors
//
//	Setting warning or error options using properties file:
//		 -properties <file>	 set warnings/errors option based on the properties
//											file contents. This option can be used with -nowarn,
//											-err:.. or -warn:.. options, but the last one on the
//											command line sets the options to be used.
//
//	Debug options:
//		 -g[:lines,vars,source] custom debug info
//		 -g:lines,source	+ both lines table and source debug info
//		 -g								 all debug info
//		 -g:none						no debug info
//		 -preserveAllLocals preserve unused local vars for debug purpose
//
//	Annotation processing options:
//		These options are meaningful only in a 1.6 environment.
//		 -Akey[=value]				options that are passed to annotation processors
//		 -processorpath <directories and ZIP archives separated by :>
//											specify locations where to find annotation processors.
//											If this option is not used, the classpath will be
//											searched for processors
//		 -processor <class1[,class2,...]>
//											qualified names of the annotation processors to run.
//											This bypasses the default annotation discovery process
//		 -proc:only					 run annotation processors, but do not compile
//		 -proc:none					 perform compilation but do not run annotation
//													processors
//		 -s <dir>						 destination directory for generated source files
//		 -XprintProcessorInfo print information about which annotations and elements
//													a processor is asked to process
//		 -XprintRounds				print information about annotation processing rounds
//		 -classNames <className1[,className2,...]>
//													qualified names of binary classes to process
//
//	Advanced options:
//		 @<file>							read command line arguments from file
//		 -maxProblems <n>	 				max number of problems per compilation unit (100 by
//											default)
//		 -log <file>						log to a file. If the file extension is '.xml', then
//											the log will be a xml file.
//		 -proceedOnError[:Fatal]
//											do not stop at first error, dumping class files with
//											problem methods
//											With ":Fatal", all optional errors are treated as fatal
//		 -verbose							enable verbose output
//		 -referenceInfo		 				compute reference info
//		 -progress							show progress (only in -log mode)
//		 -time								display speed information
//		 -noExit							do not call System.exit(n) at end of compilation (n==0
//											if no error)
//		 -repeat <n>						repeat compilation process <n> times for perf analysis
//		 -inlineJSR				 			inline JSR bytecode (implicit if target >= 1.5)
//		 -enableJavadoc		 				consider references in javadoc
//		 -parameters						generate method parameters attribute (for target >= 1.8)
//		 -genericsignature					generate generic signature for lambda expressions
//		 -Xemacs							used to enable emacs-style output in the console.
//											It does not affect the xml log output
//		 -missingNullDefault	report missing default nullness annotation
//		 -annotationpath <directories and ZIP archives separated by :>
//											specify locations where to find external annotations
//											to support annotation-based null analysis.
//											The special name CLASSPATH will cause lookup of
//											external annotations from the classpath and sourcepath.
//
//		 -? -help					 print this help message
//		 -v -version				print compiler version
//		 -showversion			 print compiler version and continue
