# Creating an OPAL Release

## Necessary Steps
 1. integrate everything into the develop branch
 1. run tests and integration tests
 1. migrate everything to the master branch
 1. update version number in `build.sbt`
 1. update version information in `src/site/index.md`
 1. turn off assertions in `scalac.options.local`
 1. run tests and integration tests (to ensure that everything works if we have no assertions)
 1. publish to maven (`sbt publishedSigned`)
 1. go to Sonatype to release the build
 1. upload the new webpage to www.opal-project.de 
 1. upload the generated ScalaDoc
 1. upload the latest version of the OPAL-Disassembler to BitBucket
 1. update *MyOPALProject* using the latest released version of OPAL
 1. force the recreation of the OPAL Docker Container
 
## Optional Steps
 1. update BugPicker
 1. update OPAL-Disassembler ATOM Plug-in 
 1. update OPAL-Integration IntelliJ Plug-in 
 
# Preparing the next release 
 1. merge changes from master back to develop (in particular version information)
 1. update version information (`build.sbt`) (x.y.z-SNAPSHOT)
 1. turn on assertions (`scalac.options.local`)
 1. release a new snapshot build to ensure that the snapshot is always younger than the last release (`sbt publishSigned`)
