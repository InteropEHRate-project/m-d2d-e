package eu.interopehrate.md2ds;

import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

import eu.interopehrate.encryptedcomunication.EncryptedCommunicationFactory;
import eu.interopehrate.encryptedcomunication.api.EncryptedCommunication;

public class MD2DSecurityInterpreter {
    private static final String NEWLINE_REPLACEMENT = "##";
    private String sessionSymmetricKey;
    private String sessionPublicKey;
    private EncryptedCommunication encryptedCommunication;

    /**
     *
     * @param hcpPublicKey
     * @throws Exception
     */
    public MD2DSecurityInterpreter(String hcpPublicKey) throws Exception {
        // Alice = HCP App, Bob = S-EHR
        // Extract certificate from received HCP public key
        byte[] hcpPubKeyEnc = Base64.decode(hcpPublicKey, Base64.DEFAULT);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(hcpPubKeyEnc);
        // Diffie Helman KeyFactory
        KeyFactory sehrKeyFactory = KeyFactory.getInstance("DH");
        PublicKey hcpPubKey = sehrKeyFactory.generatePublic(x509KeySpec);

        // Creates a KeyPair starting from received HCP public key
        encryptedCommunication = EncryptedCommunicationFactory.create();
        KeyPair sehrKeyPair = encryptedCommunication.bobInitKeyPair(hcpPubKeyEnc);
        KeyAgreement sehrKeyAgreement = encryptedCommunication.bobKeyAgreement(sehrKeyPair);

        // Generate shared secret
        KeyAgreement symkeyagreement = encryptedCommunication.bobKeyAgreementFin(hcpPubKey, sehrKeyAgreement);
        byte[] sehrSharedSecret = symkeyagreement.generateSecret();
        String symkeyagreements = Base64.encodeToString(sehrSharedSecret, Base64.DEFAULT).
                replaceAll("\r", "").
                replaceAll("\n", "");
        Log.d("MD2D", "symkeyagreement: " + symkeyagreements);

        // Generate SEHR public key to sent to HCP
        byte[] sehrPubKeyEnc = encryptedCommunication.bobPubKeyEnc(sehrKeyPair);
        this.sessionPublicKey = Base64.encodeToString(sehrPubKeyEnc, Base64.DEFAULT).
                replaceAll("\r", "").
                replaceAll("\n", "");
        Log.d("MD2D", "sessionSehrPublicKey: " + sessionPublicKey);

        // Generates session symmetric key
        SecretKeySpec symkeyspec = encryptedCommunication.generateSymmtericKey(sehrSharedSecret, 32);
        this.sessionSymmetricKey = Base64.encodeToString(symkeyspec.getEncoded(), Base64.DEFAULT)
                .replaceAll("\r", "")
                .replaceAll("\n", "");

        Log.d("MD2D", "Generated session symkey: " + sessionSymmetricKey);
    }

    /**
     *
     * @return
     */
    public String getSessionPublicKey() {
        return sessionPublicKey;
    }

    /**
     *
     * @param stringToEncript
     * @return
     * @throws Exception
     */
    public String encrypt(String stringToEncript) throws Exception {
        if (sessionSymmetricKey == null)
            throw new IllegalArgumentException("Encrypting is not possible, the symmetric key is null or empty!");

        String encryptedData = encryptedCommunication.encrypt(stringToEncript, sessionSymmetricKey);
        return encryptedData.replace("\n", NEWLINE_REPLACEMENT);
    }

    public byte[] encrypt2(String stringToEncript) throws Exception {
        if (sessionSymmetricKey == null)
            throw new IllegalArgumentException("Encrypting is not possible, the symmetric key is null or empty!");

        String encryptedData = encryptedCommunication.encrypt(stringToEncript, sessionSymmetricKey);
        return null;
    }

    /**
     *
     * @param stringToDecrypt
     * @return
     * @throws Exception
     */
    public String decrypt(String stringToDecrypt) throws Exception {
        if (sessionSymmetricKey == null)
            throw new IllegalArgumentException("Decrypting is not possible, the symmetric key is null or empty!");

        EncryptedCommunication encryptedCommunication = EncryptedCommunicationFactory.create();
        stringToDecrypt = stringToDecrypt.replace(NEWLINE_REPLACEMENT, "\n");
        return encryptedCommunication.decrypt(stringToDecrypt, sessionSymmetricKey);
    }

}
