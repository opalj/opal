/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj
package hermes
package queries

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.InstanceAPIMethod
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 * Captures the usage of the Java Crypto Architecture.
 *
 * @author Michael Reif
 */
class JavaCryptoArchitectureUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: Chain[APIFeature] = {

        // java.security
        val SecureRandom = ObjectType("java/security/SecureRandom")
        val MessageDigest = ObjectType("java/security/MessageDigest")
        val Signature = ObjectType("java/security/Signature")
        val KeyFactory = ObjectType("java/security/KeyFactory")
        val KeyPairGenerator = ObjectType("java/security/KeyPairGenerator")
        val KeyStore = ObjectType("java/security/KeyStore")

        // java.security.cert

        val CertificateFactory = ObjectType("java/security/cert/CertificateFactory")
        val CertPathBuilder = ObjectType("java/security/cert/CertPathBuilder")
        val CertPathValidator = ObjectType("java/security/cert/CertPathValidator")
        val CertStore = ObjectType("java/security/cert/CertStore")

        // javax/crypto
        val Cipher = ObjectType("javax/crypto/Cipher")
        val Mac = ObjectType("javax/crypto/Mac")
        val SecretKeyFactory = ObjectType("javax/crypto/SecretKeyFactory")
        val KeyGenerator = ObjectType("javax/crypto/KeyGenerator")
        val KeyAgreement = ObjectType("javax/crypto/KeyAgreement")

        // common methods
        val init = "<init>"
        val getInstance = "getInstance"

        Chain(

            StaticAPIMethod(Cipher, getInstance),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(SecureRandom, init),
                    StaticAPIMethod(SecureRandom, getInstance),
                    StaticAPIMethod(SecureRandom, "getInstanceStrong")
                ),
                s"using SecureRandom"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(MessageDigest, init),
                    StaticAPIMethod(MessageDigest, getInstance)
                ),
                s"using MessageDigest"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Signature, init),
                    StaticAPIMethod(Signature, getInstance)
                ),
                s"using Signature"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Mac, init),
                    StaticAPIMethod(Mac, getInstance)
                ),
                s"using Mac"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(KeyFactory, init),
                    StaticAPIMethod(KeyFactory, getInstance),
                    InstanceAPIMethod(SecretKeyFactory, init),
                    StaticAPIMethod(SecretKeyFactory, getInstance),
                    InstanceAPIMethod(KeyPairGenerator, init),
                    StaticAPIMethod(KeyPairGenerator, getInstance),
                    InstanceAPIMethod(KeyGenerator, init),
                    StaticAPIMethod(KeyGenerator, getInstance),
                    InstanceAPIMethod(KeyAgreement, init),
                    StaticAPIMethod(KeyAgreement, getInstance)
                ),
                s"cryptographic key handling"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(KeyStore, init),
                    StaticAPIMethod(KeyStore, getInstance)
                ),
                s"using KeyStore"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(CertificateFactory, init),
                    StaticAPIMethod(CertificateFactory, getInstance),
                    InstanceAPIMethod(CertPathBuilder, init),
                    StaticAPIMethod(CertPathBuilder, getInstance),
                    InstanceAPIMethod(CertPathValidator, init),
                    StaticAPIMethod(CertPathValidator, getInstance),
                    InstanceAPIMethod(CertStore, init),
                    StaticAPIMethod(CertStore, getInstance)
                ),
                s"using Certificates"
            )
        )
    }
}
