package com.castlecsr.service;

import com.castlecsr.exception.CryptographyException;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Service
public class CryptographyService {

    static {
        // Necesario también fuera del contexto de Spring (tests unitarios)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    private static final int RSA_KEY_SIZE_2048 = 2048;
    private static final int RSA_KEY_SIZE_4096 = 4096;
    private static final String CURVE_P256 = "secp256r1";
    private static final String CURVE_P384 = "secp384r1";

    private static final String SIGNATURE_ALGORITHM_RSA = "SHA256WithRSA";
    private static final String SIGNATURE_ALGORITHM_EC = "SHA256WithECDSA";

    private static final int PBKDF2_ITERATIONS = 100_000;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Genera par de claves RSA (2048 o 4096 bits). */
    public KeyPair generateRsaKeyPair(int keySize) {
        if (keySize != RSA_KEY_SIZE_2048 && keySize != RSA_KEY_SIZE_4096) {
            throw new CryptographyException("keySize debe ser 2048 o 4096");
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", PROVIDER);
            kpg.initialize(keySize, secureRandom);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new CryptographyException("Error generando claves RSA", e);
        }
    }

    /** Genera par de claves EC (secp256r1 o secp384r1). */
    public KeyPair generateEcKeyPair(String curve) {
        if (!CURVE_P256.equals(curve) && !CURVE_P384.equals(curve)) {
            throw new CryptographyException("curve debe ser secp256r1 o secp384r1");
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", PROVIDER);
            kpg.initialize(new ECGenParameterSpec(curve), secureRandom);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new CryptographyException("Error generando claves EC para curva " + curve, e);
        }
    }

    /** Genera un CSR PKCS#10 (DER) firmado con la clave privada, con extensión SAN. */
    public byte[] generateCsr(KeyPair keyPair, X500Name subject, GeneralName[] sans) {
        try {
            PKCS10CertificationRequestBuilder builder =
                    new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

            if (sans != null && sans.length > 0) {
                ExtensionsGenerator extGen = new ExtensionsGenerator();
                extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(sans));
                builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            }

            String signAlg = "RSA".equals(keyPair.getPrivate().getAlgorithm())
                    ? SIGNATURE_ALGORITHM_RSA
                    : SIGNATURE_ALGORITHM_EC;

            PKCS10CertificationRequest csr = builder.build(
                    new JcaContentSignerBuilder(signAlg)
                            .setProvider(PROVIDER)
                            .build(keyPair.getPrivate()));

            return csr.getEncoded();
        } catch (Exception e) {
            throw new CryptographyException("Error generando CSR", e);
        }
    }

    /**
     * Cifra la clave privada como PKCS#8 EncryptedPrivateKeyInfo (DER):
     * AES-256-CBC con clave derivada por PBKDF2-HMAC-SHA256 (100,000 iteraciones,
     * salt aleatorio). Compatible con `openssl pkey`.
     */
    public byte[] encryptPrivateKey(PrivateKey privateKey, char[] password) {
        try {
            OutputEncryptor encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                    .setProvider(PROVIDER)
                    .setRandom(secureRandom)
                    .setIterationCount(PBKDF2_ITERATIONS)
                    .setPRF(PKCS8Generator.PRF_HMACSHA256)
                    .setPasssword(password) // (sic) nombre del método en BouncyCastle
                    .build();

            PemObject pem = new JcaPKCS8Generator(privateKey, encryptor).generate();
            return pem.getContent(); // DER EncryptedPrivateKeyInfo
        } catch (Exception e) {
            throw new CryptographyException("Error cifrando clave privada", e);
        }
    }

    /** Convierte datos DER a PEM (Base64 en líneas de 64 caracteres). */
    public String convertToPem(byte[] derData, String type) {
        String base64 = Base64.getEncoder().encodeToString(derData);

        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        sb.append("-----END ").append(type).append("-----\n");

        return sb.toString();
    }
}