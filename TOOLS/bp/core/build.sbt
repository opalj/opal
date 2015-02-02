name := "BugPicker - Core"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors if we publish the projects; hence, we do not use the 
// standard compiler settings!
scalacOptions in (Compile, doc) := Opts.doc.title("BugPicker - Core")

