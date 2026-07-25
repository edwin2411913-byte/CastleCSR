package com.castlecsr.service;

import com.castlecsr.exception.CryptographyException;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CryptographyServiceTest {

    private static CryptographyService service;
    private static KeyPair ecKeyPair; // se genera una vez: los tests EC son rápidos

    @BeforeAll
    static void setUp() {
        service = new CryptographyService();
        ecKeyPair = service.generateEcKeyPair("secp256r1");
    }

    @Test
    void generateRsaKeyPair_con2048_generaClaveValida() {
        KeyPair kp = service.generateRsaKeyPair(2048);
        assertEquals("RSA", kp.getPrivate().getAlgorithm());
        assertNotNull(kp.getPublic());
    }

    @Test
    void generateRsaKeyPair_conTamanioInvalido_lanzaExcepcion() {
        assertThrows(CryptographyException.class, () -> service.generateRsaKeyPair(1024));
    }

    @Test
    void generateEcKeyPair_conCurvasValidas_generaClaves() {
        assertEquals("EC", ecKeyPair.getPrivate().getAlgorithm());
        KeyPair p384 = service.generateEcKeyPair("secp384r1");
        assertEquals("EC", p384.getPrivate().getAlgorithm());
    }

    @Test
    void generateEcKeyPair_conCurvaInvalida_lanzaExcepcion() {
        assertThrows(CryptographyException.class, () -> service.generateEcKeyPair("secp521r1"));
    }

    @Test
    void generateCsr_produceCsrPkcs10ConSubjectYSans() throws Exception {
        X500Name subject = new X500Name("CN=example.com,O=ACME,C=MX,ST=CDMX,L=CDMX");
        GeneralName[] sans = {
                new GeneralName(GeneralName.dNSName, "example.com"),
                new GeneralName(GeneralName.iPAddress, "192.168.1.1")
        };

        byte[] der = service.generateCsr(ecKeyPair, subject, sans);

        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(der);
        assertEquals(subject, csr.getSubject());

        // La firma del CSR debe ser verificable con su propia clave pública
        JcaPKCS10CertificationRequest jcaCsr = new JcaPKCS10CertificationRequest(csr);
        assertTrue(jcaCsr.isSignatureValid(
                new org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder()
                        .setProvider("BC").build(ecKeyPair.getPublic())));

        // Verificar extensión SAN
        Attribute[] attrs = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        assertEquals(1, attrs.length);
        Extensions extensions = Extensions.getInstance(attrs[0].getAttrValues().getObjectAt(0));
        GeneralNames names = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
        assertEquals(2, names.getNames().length);
    }

    @Test
    void encryptPrivateKey_soloSeDescifraConLaContraseniaCorrecta() throws Exception {
        char[] password = "MiContraseña123".toCharArray();
        byte[] encrypted = service.encryptPrivateKey(ecKeyPair.getPrivate(), password);

        PKCS8EncryptedPrivateKeyInfo info = new PKCS8EncryptedPrivateKeyInfo(encrypted);

        // Contraseña correcta: descifra y la clave coincide
        InputDecryptorProvider decryptor = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                .setProvider("BC").build("MiContraseña123".toCharArray());
        byte[] decrypted = info.decryptPrivateKeyInfo(decryptor).getEncoded();
        assertArrayEquals(ecKeyPair.getPrivate().getEncoded(), decrypted);

        // Contraseña incorrecta: falla
        InputDecryptorProvider wrong = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                .setProvider("BC").build("otra_password".toCharArray());
        assertThrows(Exception.class, () -> info.decryptPrivateKeyInfo(wrong));
    }

    @Test
    void encryptPrivateKey_usaSaltAleatorio() {
        char[] password = "MiContraseña123".toCharArray();
        byte[] a = service.encryptPrivateKey(ecKeyPair.getPrivate(), password);
        byte[] b = service.encryptPrivateKey(ecKeyPair.getPrivate(), password);
        // Con salt/IV aleatorios, dos cifrados de la misma clave nunca son iguales
        assertFalse(Arrays.equals(a, b));
    }

    @Test
    void convertToPem_generaHeadersFootersYLineasDe64() {
        byte[] data = new byte[200];
        String pem = service.convertToPem(data, "CERTIFICATE REQUEST");

        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE REQUEST-----\n"));
        assertTrue(pem.endsWith("-----END CERTIFICATE REQUEST-----\n"));
        for (String line : pem.split("\n")) {
            assertTrue(line.length() <= 64 || line.startsWith("-----"));
        }
    }
}