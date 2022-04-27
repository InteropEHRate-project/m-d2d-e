package eu.interopehrate.md2ds;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

public class MD2DSecurityUtilities {
    private final static String KEYSTORE_NAME = "keystore.bks";
    private final static String KEYSTORE_TYPE = KeyStore.getDefaultType();
    private final static String KEYSTORE_PASSWORD = "android";
    private final static String SEHR_CERTIFICATE_ALIAS = "androidkey";

    private final static String ADDRESS_ENRTY = "ADDRESS";
    private final static String SIGNATURE_ENRTY = "SIGNATURE";

    /**
     * Checks if KeyStore exists, otherwise creates and empty one and loads it
     * with a new X509 certificate containing a pair of private and public key.
     *
     * @param context
     * @throws Exception
     */
    public static void initialize(Context context) throws Exception {
        // checks if keystore exists
        Log.d("MD2D", "Initializing security...");

        String keyStorePath = getKeystorePath(context);
        Log.d("MD2D", "keyStorePath: "+ keyStorePath);
        File f = new File(keyStorePath);
        if (f == null || !f.isFile() || !f.canRead()) {
            Log.d("MD2D", "KeyStore does not exist. Creation of keystore in progress...");
            generateKeyStore(keyStorePath);
            Log.d("MD2D","KeyStore created and stored successfully");
        } else{
            Log.d("MD2D", "keyStore already exists");
        }
    }

    /**
     * retrieves the SEHR certificate from the sehr keystore
     *
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String getSEHRCertificate(Context context) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException {
        Log.d("MD2D", "retrieving SEHR certificate...:");
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        String keyStorePath = getKeystorePath(context);
        keyStore.load(new FileInputStream(keyStorePath), KEYSTORE_PASSWORD.toCharArray());

        Certificate cert = keyStore.getCertificate(SEHR_CERTIFICATE_ALIAS);
        return Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT)
                .replaceAll("\r", "")
                .replaceAll("\n", "");
    }

    private static String getKeystorePath(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/"
                + KEYSTORE_NAME;
    }

    /**
     * Method used to creates the KeyStore from scratch.
     *
     * @param keyStoreFileName
     * @return
     * @throws Exception
     */
    private static KeyStore generateKeyStore(String keyStoreFileName) throws Exception {
        // Creates KeyPair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.initialize(1024, random);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Creates an X509 certificate
        X509Certificate certificate = generateCertificate(keyPair);

        // Creates an empty KeyStore
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        ks.load(null, KEYSTORE_PASSWORD.toCharArray());
        ks.setKeyEntry(SEHR_CERTIFICATE_ALIAS,
                keyPair.getPrivate(),
                null,
                new X509Certificate[]{certificate});

        // Export keystore in a file
        FileOutputStream fos = new FileOutputStream(keyStoreFileName);
        ks.store(fos, KEYSTORE_PASSWORD.toCharArray());
        fos.close();

        return ks;
    }

    /**
     * Method used to create an X509 certificate with a KeyPair
     *
     * @param keyPair
     * @return
     * @throws OperatorCreationException
     * @throws CertificateException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     */
    private static X509Certificate generateCertificate(KeyPair keyPair)
            throws OperatorCreationException, CertificateException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {
        // TODO: ALESSIO: Mario Rossi hard coded?
        String issuerString = "C=IT, O=InteropEHRate, OU=InteropEHRate Certificate, CN=Mario Rossi, UID=0f3e03e0-b4ca-4a76-821d-bdef16267ed0";
        // subjects name - the same as we are self signed.
        String subjectString = "C=IT, O=InteropEHRate, OU=InteropEHRate Certificate, CN=Mario Rossi, UID=0f3e03e0-b4ca-4a76-821d-bdef16267ed0";
        // String issuerString = "C=DE, O=datenkollektiv, OU=Planets Debug Certificate";
        // subjects name - the same as we are self signed.
        // String subjectString = "C=DE, O=datenkollekitv, OU=Planets Debug Certificate";
        X500Name issuer = new X500Name(issuerString);
        BigInteger serial = BigInteger.ONE;
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (365 * 24 * 60 * 60));
        X500Name subject = new X500Name(subjectString);
        PublicKey publicKey = keyPair.getPublic();
        JcaX509v3CertificateBuilder v3Bldr = new JcaX509v3CertificateBuilder(issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                publicKey);
        X509CertificateHolder certHldr = v3Bldr
                .build(new JcaContentSignerBuilder("SHA1WithRSA").build(keyPair.getPrivate()));
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHldr);
        cert.checkValidity(new Date());
        cert.verify(keyPair.getPublic());
        return cert;
    }


    public static void storeScannedCertificate(Context context, String signature, String address) {
        Log.d("MD2D", "Saving scanned data: " + signature + ", " + address);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SIGNATURE_ENRTY, signature);
        editor.putString(ADDRESS_ENRTY, address);
        editor.commit();
        editor.apply();
    }


    public static void verifyHCPCertificate(Context context, String hcpCertificate) throws Exception {
        // Exctract publickey from received certificate
        byte[] tempCert = Base64.decode(hcpCertificate, Base64.DEFAULT);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(tempCert));
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

        Log.d("MD2D", "Public: " + publicKey.getModulus() + "|" + publicKey.getPublicExponent());

        // store certificate locally
        // onCertReceivedStore(cert);

        String certs = Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT)
                .replaceAll("\r", "")
                .replaceAll("\n", "");

        Log.d("MD2D SOFIANNA publicss", certs);

        // Retrieve SharedPreferences from context
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String sAddress = settings.getString(ADDRESS_ENRTY, "");
        Log.d("MD2D SOFIANNA sAddress", sAddress);
        byte[] scannedAddress = sAddress.getBytes("UTF-8");

        String sSignature = settings.getString(SIGNATURE_ENRTY, "");
        Log.d("MD2D SOFIANNA sSign", sSignature);
        byte[] scannedSignature = sSignature.getBytes("UTF-8");

        boolean result = verifySignature(publicKey, scannedAddress, scannedSignature);
        //TODO: fix onVerify
        //onVerify(result);
        Log.d("MD2D", String.valueOf(result));
    }

    /**
     * What does this method do?
     */
    public static void storeHCPCertificate(Certificate certificate) {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null);

            String alias = "mykey";

            // Add the certificate
            keystore.setCertificateEntry(alias, certificate);

            Log.d("My App Cert: ", "true");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * VerifySignature
     */
    public static boolean verifySignature(RSAPublicKey publicKey, byte[] data, byte[] scannedSignature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] signedPayloadContent = Base64.decode(scannedSignature, Base64.DEFAULT);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);

        signature.update(data);
        boolean result = signature.verify(signedPayloadContent);

        String certs = Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT)
                .replaceAll("\r", "")
                .replaceAll("\n", "");

        Log.d("MD2D", certs);
        Log.d("MD2D", "verifySignature-> " + String.valueOf(result));
        return result;
    }

    /**
     * SignPayLoad
     */

}
