# Overview

The ***APK*** module provides tools to do cross language (Dex bytecode and native code) analyses on Android APKs.

## Future work
- Entry points analysis is not complete, focus only on Activities, Services, Broadcast Receivers and Content Providers,
  from AndroidManifest.xml and from registrations in code. Entry points of UI events are missing, e.g. button onClick, gestures, ...

TODO:
- [x] add to build
- [ ] dex parser
- [ ] so parser
- [x] manifest entry point parser
- [x] unzip in scala, remove unzip dependency
- [ ] dynamic entry points analysis
- [ ] dynamic entry points key
- [ ] create naive example xlang apk
- [ ] simple proof of concept analysis
- [ ] add this poc as test
- [ ] docker dependencies installation bash script
