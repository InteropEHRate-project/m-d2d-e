package eu.interopehrate.md2de.api;


/*
 *		Author: UBITECH
 *		Project: InteropEHRate - www.interopehrate.eu
 *
 *	Description: Interface of security device-to-device (D2D) connection compliant compliant to D2D specifications.
 *	It allows the S-EHR application to initiate the secure Bluetooth connection between the S-EHR app and the HCP App. (M-D2D-SM)
 *
 */

import android.content.Context;

import org.bouncycastle.operator.OperatorCreationException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Patient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

@Deprecated
public interface D2DSecurityCommunication {

     /**
     *
     * Responsible for verifing the HCP's scanned signature
     *
     * @param publicKey
     * @param scannedAddress
     * @param scannedSignature
     *
     * @return boolean
     *
     */
     @Deprecated
     public boolean verifySignature(RSAPublicKey publicKey, byte[] scannedAddress, byte[] scannedSignature)
        throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException;

     /**
     *
     * Responsible for store the HCP's key to the keystore
     *
     * @param cert the certificate of HCP
     *
     * @return boolean
    */
     @Deprecated
    public boolean onCertReceivedStore(Certificate cert);

    /**
    *
    * Responsible for store the HCP's signatrue to preferences and BT address
    *
     * @param context
     * @param scannedSignature the public key of HCP
     * @param scannedAddress
     *
    */
    @Deprecated
    public void storeScanned(Context context, String scannedSignature, String scannedAddress);

    /**
     *
     * Responsible for fech credentials
     *
     */
    @Deprecated
    public void fetchCertificate() throws NoSuchProviderException, NoSuchAlgorithmException;

    /**
    *
    * Responsible for sending the Citizen's certificate
    *
    */
    @Deprecated
    public void sendSEHRCertificate()
            throws Exception;

    /**
     *
     * Responsible for sending the generating signed QR payload data
     *
     * @param payload Tha payload to signed
     */
    @Deprecated
    public String signPayload(String payload, PrivateKey privateKey) throws IOException, SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException;

    /**
     *
     * Responsible for store the Consent preferences
     *
     * @param encodedConsent
     * @param scannedConsent
     *
     */
    @Deprecated
    public void storeConsent(String encodedConsent, String scannedConsent);

    /**
     *
     * Responsible for creating the Patients's consent
     *
     */
/*
    public Consent createConsent(Consent.ConsentState theState, Patient thePatient);
*/

    /**
     *
     * Responsible for store the symmetric key to the keystore
     *
     * @param symkey the encrypted symmetric key
     *
     * @return boolean
     */
    @Deprecated
    public boolean onSymmetricKeyReceivedStore(String symkey);

    /**
     *
     * Responsible for creating and store the keyPair to the keystore
     *
     * @param keyPair the certificate and private key
     *
     * @return boolean
     */
    @Deprecated
    KeyStore createKeyStore(KeyPair keyPair);
}
