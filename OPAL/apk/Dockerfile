# based on RetDec's Dockerfile (https://github.com/avast/retdec)

# temporary build environment
FROM ubuntu:jammy AS builder

RUN useradd -m opal
WORKDIR /home/opal
ENV HOME /home/opal

RUN apt-get -y update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y   \
    build-essential                                     \
    cmake                                               \
    git                                                 \
    python3                                             \
    doxygen                                             \
    graphviz                                            \
    upx                                                 \
    openssl                                             \
    libssl-dev                                          \
    zlib1g-dev                                          \
    autoconf                                            \
    automake                                            \
    pkg-config                                          \
    m4                                                  \
    libtool                                             \
    unzip                                               \
    default-jdk


USER opal

# retdec
RUN git clone https://github.com/avast/retdec && \
    cd retdec && \
    mkdir build && \
    cd build && \
    cmake .. -DCMAKE_INSTALL_PREFIX=/home/opal/retdec-install \
        -DCMAKE_LIBRARY_PATH=/usr/lib/gcc/x86_64-linux-gnu/7/ && \
    make -j$(nproc) && \
    make install

# enjarify
RUN git clone https://github.com/Storyyeller/enjarify

# dex2jar
RUN git clone https://github.com/ThexXTURBOXx/dex2jar && \
    cd dex2jar && \
    ./gradlew distZip && \
    unzip dex-tools/build/distributions/dex-tools-2.1-SNAPSHOT.zip


# actual container contains only binaries
FROM ubuntu:jammy

RUN useradd -m opal
WORKDIR /home/opal
ENV HOME /home/opal

RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y   \
    openssl                                             \
    graphviz                                            \
    upx                                                 \
    python3                                             \
    pypy3                                               \
    default-jre

USER opal

COPY --from=builder /home/opal/retdec-install /home/opal/retdec
COPY --from=builder /home/opal/enjarify /home/opal/enjarify
COPY --from=builder /home/opal/dex2jar/dex-tools-2.1-SNAPSHOT /home/opal/dex2jar

ENV PATH /home/opal/retdec/bin:/home/opal/enjarify:/home/opal/dex2jar:$PATH

