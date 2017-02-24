# Usage of the Java Crypto Architecture (JCA)

Extracts the information about the usage of the core classes and interfaces of the Java Crypto
Architecture. Those classes have been selected according to the official JCA reference guide. [1]

The analysis checks in particular for calls to constructor or the `getInstance` method of a given
class/interface. 

## Algorithm kinds

The analysis supports the following cryptographic primitives:

- `Cipher`
- `Mac`
- `MessageDigest`
- `Signature`
- `SecureRandom`

> Note: Each of these types represents a single feature.

## Key Handling

The _Key Handling_ group represents whether some of the following classes/interfaces are used:

- `KeyFactory`
- `SecretKeyFactory`
- `KeyGenerator`
- `KeyPairGenerator`
- `KeyAgreement`

> Note: Subclasses are not supported yet, however, to track this calls gives a first intuition about
> the usage of cryptographic keys. Especially the factories are often used. 
## Key Store

The _Key Store_ class checks whether a `KeyStore` is used.

## Certificate Handling

The _Certificate Handling_ group checks whether calls to the following certificate classes/interfaces
appear within the codebase:

- `CertificateFactory`
- `CertPathBuilder`
- `CertPathValidator`
- `CertStore`
 
> [1] JCA Reference Guide - http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#CoreClasses