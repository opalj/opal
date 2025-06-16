/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.ClassType
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

    override val apiFeatures: List[APIFeature] = {

        // java.security
        val SecureRandom = ClassType("java/security/SecureRandom")
        val MessageDigest = ClassType("java/security/MessageDigest")
        val Signature = ClassType("java/security/Signature")
        val KeyFactory = ClassType("java/security/KeyFactory")
        val KeyPairGenerator = ClassType("java/security/KeyPairGenerator")
        val KeyStore = ClassType("java/security/KeyStore")

        // java.security.cert

        val CertificateFactory = ClassType("java/security/cert/CertificateFactory")
        val CertPathBuilder = ClassType("java/security/cert/CertPathBuilder")
        val CertPathValidator = ClassType("java/security/cert/CertPathValidator")
        val CertStore = ClassType("java/security/cert/CertStore")

        // javax/crypto
        val Cipher = ClassType("javax/crypto/Cipher")
        val Mac = ClassType("javax/crypto/Mac")
        val SecretKeyFactory = ClassType("javax/crypto/SecretKeyFactory")
        val KeyGenerator = ClassType("javax/crypto/KeyGenerator")
        val KeyAgreement = ClassType("javax/crypto/KeyAgreement")

        // common methods
        val init = "<init>"
        val getInstance = "getInstance"

        List(
            StaticAPIMethod(Cipher, getInstance),
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(SecureRandom, init),
                    StaticAPIMethod(SecureRandom, getInstance),
                    StaticAPIMethod(SecureRandom, "getInstanceStrong")
                ),
                s"using SecureRandom"
            ),
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(MessageDigest, init),
                    StaticAPIMethod(MessageDigest, getInstance)
                ),
                s"using MessageDigest"
            ),
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Signature, init),
                    StaticAPIMethod(Signature, getInstance)
                ),
                s"using Signature"
            ),
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Mac, init),
                    StaticAPIMethod(Mac, getInstance)
                ),
                s"using Mac"
            ),
            APIFeatureGroup(
                List(
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
                List(
                    InstanceAPIMethod(KeyStore, init),
                    StaticAPIMethod(KeyStore, getInstance)
                ),
                s"using KeyStore"
            ),
            APIFeatureGroup(
                List(
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
