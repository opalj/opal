clang -shared -I/usr/lib/jvm/java-11-openjdk-amd64/include/ -I/usr/lib/jvm/java-11-openjdk-amd64/include/linux/ -o libtainttest.so TaintTest.c
clang -S -I/usr/lib/jvm/java-11-openjdk-amd64/include/ -I/usr/lib/jvm/java-11-openjdk-amd64/include/linux/ TaintTest.c -emit-llvm
javac TaintTest.java