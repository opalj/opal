/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.ObjectType
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
