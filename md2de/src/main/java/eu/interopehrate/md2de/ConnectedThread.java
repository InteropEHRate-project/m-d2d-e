package eu.interopehrate.md2de;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.encryptedcomunication.EncryptedCommunicationFactory;
import eu.interopehrate.encryptedcomunication.api.EncryptedCommunication;
import eu.interopehrate.md2de.api.D2DCommunication;
import ca.uhn.fhir.context.FhirContext;
import eu.interopehrate.md2de.api.MD2DConnectionListener;
import eu.interopehrate.md2de.api.D2DHRExchangeListeners;
import eu.interopehrate.md2de.api.D2DSecurityCommunication;

import static java.nio.charset.StandardCharsets.UTF_8;

@Deprecated
public class ConnectedThread extends Thread implements D2DCommunication, D2DSecurityCommunication {

    public final BluetoothSocket mmSocket;
    public final InputStream mmInStream;
    public final DataOutputStream mmOutStream;
    public BufferedWriter bWriter = null;
    public static Context context;
    String lineRead;
    String lineReadBp;
    String lineReadSubBp;
    Practitioner practitioner;
    Consent consent;
    D2DHRExchangeListeners listeners;
    MD2DConnectionListener listenersConnection;
    RSAPublicKey publicKey;
    KeyStore keystore;

    /**
     * Keystore name
     */
    public final static String KEYSTORE_NAME = "keystore.bks";

    /**
     * Keystore password
     */
    public final static String KEYSTORE_PASSWORD = "android";

    /**
     * Keystore type
     */
    public final static String KEYSTORE_TYPE = KeyStore.getDefaultType();

    /**
     * Keystore password
     */
    public final static String KEYSTORE_ALIAS = "androidkey";

    /**
     * Session key
     */
    public String symkey = null; // = "Bos0HSxY4HWrVwEZaoywbAnP8a0BWExEfl5pyHULEXQ=";


    public ConnectedThread(BluetoothSocket socket, D2DHRExchangeListeners listeners, MD2DConnectionListener listenersConnection, Context context) {
        mmSocket = socket;
        InputStream tmpIn = null;
        DataOutputStream tmpOut = null;
        this.listeners = listeners;
        this.listenersConnection = listenersConnection;
        this.context = context;

        try {
            tmpIn = mmSocket.getInputStream();
            tmpOut = new DataOutputStream(mmSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void run() {
        //Keep listening to the InputStream until an exception occurs
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new InputStreamReader(mmInStream, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            bWriter = new BufferedWriter(new OutputStreamWriter(mmOutStream, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        while (true) {
            try {

                lineRead = bReader.readLine();
                lineReadBp = lineRead;

                //comment if use ASCII characters
                if(!lineRead.equals("")) {
                    if(lineRead.startsWith("�enc")){
                        lineRead = lineRead.substring(1);
                    }
                    else {
                        lineRead = lineRead.substring(2);
                    }
                }
//                if(!lineRead.equals("")) {
//
//                    // strips off all non-ASCII characters
//                    lineRead = lineRead.replaceAll("[^\\x00-\\x7F]", "");
//
//                    // erases all the ASCII control characters
//                    lineRead = lineRead.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
//
//                    // removes non-printable characters from Unicode
//                    lineRead = lineRead.replaceAll("\\p{C}", "");
//
//                    // remove the + for the Pubkey
//                    if (lineRead.startsWith("+")) {
//                        lineRead = lineRead.substring(1);
//                    }
//                    // remove the 7 for the symkey
//                    if (lineRead.startsWith("7")) {
//                        lineRead = lineRead.substring(1);
//                    }
//                    // remove the + for the Practitioner
//                    if (lineRead.startsWith(" ")) {
//                        lineRead = lineRead.substring(1);
//                    }
//                    // remove the D for the prescrpr
//                    if (lineRead.startsWith("D")) {
//                        lineRead = lineRead.substring(1);
//                    }
//                    // remove the "\" for the med_doc
//                    if (lineRead.startsWith("\\")) {
//                        lineRead = lineRead.substring(1);
//                    }
//                    // vital do not have additional char


                lineReadSubBp = lineRead;

                if (lineRead.startsWith("enc") && symkey != null) {
                    lineRead = lineRead.substring(3);
                    EncryptedCommunication encryptedCommunication = EncryptedCommunicationFactory.create();
                    System.out.println("Symmteric: " + symkey);
                    lineRead = lineRead.replace("##", "\n");
                    System.out.println("Encrypted: " + lineRead);
                    lineRead = encryptedCommunication.decrypt(lineRead, symkey);
                    System.out.println("Decrypted: " + lineRead);
                }

                Log.d("MD2D", "onrun()lineRead\n" + lineRead);

                //if (lineRead != null) {
                if(!lineRead.equals("")) {
                    if (lineRead.substring(0, 22).equals("ConsentDetailsDocument")) {
                        lineRead = lineRead.substring(23);
                        Log.d("MSSG CONSENT", lineRead);

                        String[] parts = lineRead.split("#");
                        String rconsent = parts[0];
                        String rsignature = parts[1];

                        JSONParser jsonParser = new JSONParser();
                        JSONObject json = (JSONObject) jsonParser.parse(rconsent);
                        String resourceType = (String) json.get("resourceType");

                        if (resourceType.equals("Consent")) {
                            PrivateKey privateKey = (PrivateKey) keystore.getKey(KEYSTORE_ALIAS, null);
                            KeyPair keyPair = getKeyPair(KEYSTORE_ALIAS, null);

                            IParser parser = FhirContext.forR4().newJsonParser();
                            consent = parser.parseResource(Consent.class, rconsent);

                            boolean result = verifySignature(publicKey, rconsent.getBytes(), rsignature.getBytes());
                            listeners.onVerify(result);
                            Log.d("MSSG Check Consent Sign", String.valueOf(result));

                            if (result) {
                                //Resign the consent
                                String signedConsent = signPayload(rconsent, privateKey);
                                storeConsent(rconsent, signedConsent);
                            } else {
                                Log.d("Signature not verified", String.valueOf(result));
                                listeners.onConsentRequested(consent, "");
                            }
                        }
                    } else if (lineRead.contains("#ACK#")) {
                        String[] typeAndValue = lineRead.split("#ACK#");
                        if (typeAndValue[0].equals("practitioner")) {
                            IParser parser = FhirContext.forR4().newJsonParser();
                            practitioner = parser.parseResource(Practitioner.class, typeAndValue[1]);
                            listeners.onHealthOrganizationIdentityReceived(practitioner);
                        } else if (typeAndValue[0].equals("medicationRequest")) {
                            IParser parser = FhirContext.forR4().newJsonParser();
                            Bundle medicationRequest = parser.parseResource(Bundle.class, typeAndValue[1]);
                            listeners.onPrescriptionReceived(medicationRequest);
                        } else if (typeAndValue[0].equals("vitalSigns")) {
                            IParser parser = FhirContext.forR4().newJsonParser();
                            Bundle vitalSigns = parser.parseResource(Bundle.class, typeAndValue[1]);
                            listeners.onVitalSignsReceived(vitalSigns);
                        } else if (typeAndValue[0].equals("medicalDocumentRequest")) {
                            String[] request = typeAndValue[1].split("#");
                            //Date startingDate = new SimpleDateFormat("yyyy-MM-dd").parse(request[0]);
                            //Date endingDate = new SimpleDateFormat("yyyy-MM-dd").parse(request[1]);
                            String startingDate = request[0];
                            String endingDate = request[1];
                            String type = request[2];
                            listeners.onMedicalDocumentRequestReceived(startingDate, endingDate, type);

                        }

//                        else if(typeAndValue[0].equals("medicalDocumentRequest")){
//                            String[] request =  typeAndValue[1].split("#");
//                            //Date startingDate = new SimpleDateFormat("yyyy-MM-dd").parse(request[0]);
//                            //Date endingDate = new SimpleDateFormat("yyyy-MM-dd").parse(request[1]);
//                            String type = request[2];
//                            if (type.equals("cardiology") || type.equals("psychiatry") || type.equals("all")) {
//                                String startingDate = request[0];
//                                String endingDate = request[1];
//                                listeners.onMedicalDocumentRequestReceived(startingDate, endingDate, type);
//                            }
//                            else{
//                                listenersConnection.onConnectionClosure();
//                            }
//                        }
                    } else if (lineRead.equals("closingConnection")) {
                        listenersConnection.onConnectionClosure();
                    } else if (lineRead.contains("pubkey")) {
                        lineRead = lineRead.substring(6);

                        byte[] tempCert = Base64.decode(lineRead, Base64.DEFAULT);
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(tempCert));
                        publicKey = (RSAPublicKey) cert.getPublicKey();

                        System.out.println("Public: " + publicKey.getModulus() + "|" + publicKey.getPublicExponent());

                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                        String sAddress = settings.getString("address", "");
                        String sSignature = settings.getString("signature", "");

                        byte[] scannedAddress = sAddress.getBytes("UTF-8");
                        byte[] scannedSignature = sSignature.getBytes("UTF-8");

                        onCertReceivedStore(cert);
                        Log.d("MSSG SOFIANNA sAddress", sAddress);
                        Log.d("MSSG SOFIANNA sSign", sSignature);
                        String certs = Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");

                        Log.d("MSSG SOFIANNA publicss", certs);
                        boolean result = verifySignature(publicKey, scannedAddress, scannedSignature);
                        listeners.onVerify(result);
                        Log.d("MD2D", String.valueOf(result));
                    } else if (lineRead.contains("symkey")) {
                        symkey = lineRead.substring(6);
                        Log.d("MSSG symkey", symkey);
                        byte[] alicePubKeyEnc = Base64.decode(symkey, Base64.DEFAULT);

                        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
                        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);
                        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

                        EncryptedCommunication encryptedCommunication = EncryptedCommunicationFactory.create();
                        KeyPair bobkeypair = encryptedCommunication.bobInitKeyPair(alicePubKeyEnc);
                        KeyAgreement bobKeyAgreement = encryptedCommunication.bobKeyAgreement(bobkeypair);

                        KeyAgreement symkeyagreement = encryptedCommunication.bobKeyAgreementFin(alicePubKey, bobKeyAgreement);
                        byte[] bobSharedSecret = symkeyagreement.generateSecret();
                        String symkeyagreements = Base64.encodeToString(bobSharedSecret, Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");
                        Log.d("MSSG symkeyagreement", symkeyagreements);

                        byte[] bobPubKeyEnc = encryptedCommunication.bobPubKeyEnc(bobkeypair);
                        String bobPubKeyEncb = Base64.encodeToString(bobPubKeyEnc, Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");
                        Log.d("MSSG bobPubKeyEncb", bobPubKeyEncb);

                        this.write("bobPubKeyEncb#ACK#" + bobPubKeyEncb); ////////////////
                        SecretKeySpec symkeyspec = encryptedCommunication.generateSymmtericKey(bobSharedSecret, 32);
                        String symkeys = Base64.encodeToString(symkeyspec.getEncoded(), Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");
                        symkey = symkeys;
                        Log.d("MSSG symkey", symkey);
                    }
                }
                //}//close the replaceAll
            } catch (IOException | StringIndexOutOfBoundsException | NullPointerException | ParseException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | CertificateException | KeyStoreException | UnrecoverableKeyException | InvalidKeySpecException e) {

                Log.d("MD2D", "run() stopped.");
                e.printStackTrace();
//                Log.d("MD2D","Connection before closed.btSocket:"+String.valueOf(mmSocket.isConnected()));
//                Log.d("MD2D",e.getStackTrace().toString()+","+e.getMessage()+","+e.getLocalizedMessage()+ ","+e.toString());
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //----------API methods
    public void sendPersonalIdentity(Patient patient) {
        String patientJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(patient);
        this.write("patient#ACK#" + patientJSON);
    }

    public void sendPatientSummary(Bundle patientSummary) throws Exception {
        String patientSummaryJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(patientSummary);
        patientSummaryJSON = patientSummaryJSON.replaceAll("\n", "");
        patientSummaryJSON = patientSummaryJSON.replaceAll("\r", "");
        this.encryptedwrite("patientSummary#ACK#" + patientSummaryJSON);
        //this.write("patientSummary#ACK#"+patientSummaryJSON);
    }

    public void sendPrescription(Bundle prescription) throws Exception {
        String prescriptionJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(prescription);
        prescriptionJSON = prescriptionJSON.replaceAll("\n", "");
        prescriptionJSON = prescriptionJSON.replaceAll("\r", "");
        this.encryptedwrite("prescription#ACK#" + prescriptionJSON);
        //this.write("prescription#ACK#"+prescriptionJSON);
    }

    public void sendLaboratoryResults(Bundle laboratoryResults) throws Exception {
        String laboratoryResultsJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(laboratoryResults);
        laboratoryResultsJSON = laboratoryResultsJSON.replaceAll("\n", "");
        laboratoryResultsJSON = laboratoryResultsJSON.replaceAll("\r", "");
        this.encryptedwrite("laboratoryResults#ACK#" + laboratoryResultsJSON);
        //this.write("laboratoryResults#ACK#"+laboratoryResultsJSON);
    }

    public void sendImageReport(Bundle imageReport) throws Exception {
        String imageReportJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(imageReport);
        imageReportJSON = imageReportJSON.replaceAll("\n", "");
        imageReportJSON = imageReportJSON.replaceAll("\r", "");
        this.encryptedwrite("imageReport#ACK#" + imageReportJSON);
        //this.write("imageReport#ACK#"+imageReportJSON);
    }

    public void sendPathologyHistoryInformation(Bundle pathologyHistory) throws Exception {
        String pathologyHistoryJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(pathologyHistory);
        pathologyHistoryJSON = pathologyHistoryJSON.replaceAll("\n", "");
        pathologyHistoryJSON = pathologyHistoryJSON.replaceAll("\r", "");
        this.encryptedwrite("pathologyHistoryInformation#ACK#" + pathologyHistoryJSON);
        //this.write("pathologyHistoryInformation#ACK#"+pathologyHistoryJSON);
    }

    public void sendMedicalDocumentConsultation(Bundle medicalDocument) throws Exception {
        String medicalDocumentJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(medicalDocument);
        medicalDocumentJSON = medicalDocumentJSON.replaceAll("\n", "");
        medicalDocumentJSON = medicalDocumentJSON.replaceAll("\r", "");
        this.encryptedwrite("medicalDocumentConsultation#ACK#" + medicalDocumentJSON);
        //this.write("medicalDocumentConsultation#ACK#"+medicalDocumentJSON);
    }

    public void sendVitalSigns(Bundle vitalSigns) throws Exception {
        String vitalSignsJSON = FhirContext.forR4().newJsonParser().encodeResourceToString(vitalSigns);
        vitalSignsJSON = vitalSignsJSON.replaceAll("\n", "");
        vitalSignsJSON = vitalSignsJSON.replaceAll("\r", "");
        this.encryptedwrite("vitalSigns#ACK#" + vitalSignsJSON);
        //this.write("vitalSigns#ACK#"+vitalSignsJSON);
    }

    //---------NOT API methods
    //write data to the output stream
    public void write(String data) {

        try {
//            mmOutStream.writeUTF(data+"\n");
            bWriter.write(data);
            bWriter.newLine();
            bWriter.flush();
        } catch (IOException e) {
            String msg = "An exception occurred during write: " + e.getMessage();
            Log.d("MD2D", msg);
        }
    }

    public void encryptedwrite(String data) throws Exception {
        // Encrypted Communication
        EncryptedCommunication encryptedCommunication = EncryptedCommunicationFactory.create();
        //String symkey = "+LNxt0f/2wYtB2i7GjqtpSh3f2NAhE8vEePpO2jV22U=";////"Bos0HSxY4HWrVwEZaoywbAnP8a0BWExEfl5pyHULEXQ=";//TODO
        String encryptedData = encryptedCommunication.encrypt(data, symkey);
        //System.out.println("ENCRYPTED: " + encryptedData);
        String decyrpted = encryptedCommunication.decrypt(encryptedData, symkey);
        //System.out.println("DATA: " + data);
        //System.out.println("DECRYPTED: " + decyrpted);
        encryptedData = encryptedData.replace("\n", "##");

        try {
            String toSent = "enc" + encryptedData + "\n";
            byte[] b = toSent.getBytes("utf-8");
            mmOutStream.writeInt(b.length);
            mmOutStream.write(b);
            //mmOutStream.writeUTF("enc"+encryptedData+"\n");
            System.out.println("SENT:" + "enc" + encryptedData + "\n");
            mmOutStream.flush();
        } catch (IOException e) {
            String msg = "An exception occurred during write: " + e.getMessage();
            Log.d("MD2D", msg);
        }
    }

    public void closeStreams() {

        this.write("closingConnection");
        listenersConnection.onConnectionClosure();
    }


    //---SECURITY-UBIT---START
    //------------------------
    public KeyPair getKeyPair(final String alias, final String password) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        final Key key = (PrivateKey) keystore.getKey(alias, null);

        final Certificate cert = keystore.getCertificate(alias);
        final PublicKey publicKey = cert.getPublicKey();

        return new KeyPair(publicKey, (PrivateKey) key);
    }

    /*
     * Verify a signature previously made by a PrivateKey in our
     * KeyStore. This uses the X.509 certificate attached to our
     * private key in the KeyStore to validate a previously
     * generated signature.
     */

    @Override
    public boolean verifySignature(RSAPublicKey publicKey, byte[] data, byte[] scannedSignature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        byte[] signedPayloadContent = Base64.decode(scannedSignature, Base64.DEFAULT);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);

        signature.update(data);
        boolean result = signature.verify(signedPayloadContent);

        Log.d("MSSG ", "================================");
        String certs = Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");
        Log.d("MSSG pubkey", certs);
        Log.d("MSSG verifySignature->", String.valueOf(result));
        return result;
    }

    @Override
    public void storeScanned(Context context, String signature, String address) {
        Log.d("MSSG storeScanned", signature + " " + address);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("signature", signature);
        editor.putString("address", address);
        editor.commit();
        editor.apply();
    }

    @Override
    public void fetchCertificate() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        KeyStore ks = createKeyStore(keyPair);
        Log.d("MD2D", "fetchCertificate");
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.initialize(1024, random);
        return keyGen.generateKeyPair();
    }

    @Override
    public KeyStore createKeyStore(KeyPair keyPair) {
        KeyStore ks = null;
        //if ((file == null) || (!file.exists())) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getKeystorePath(this.context));

            // Build empty keystore
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            X509Certificate certificate = generateCertificate(keyPair);
            ks.load(null, KEYSTORE_PASSWORD.toCharArray());
            ks.setKeyEntry(KEYSTORE_ALIAS,
                    keyPair.getPrivate(),
                    null,
                    new X509Certificate[]{
                            certificate
                    });
            // Export keystore in a file
            ks.store(fos, KEYSTORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                // Intentionally blank
            }
        }
        //}
        return ks;
    }

    public String getKeystorePath(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/"
                + KEYSTORE_NAME;
    }

    private static KeyStore loadKeystore(FileInputStream keystoreBytes)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks;
        ks = KeyStore.getInstance(KeyStore.getDefaultType());

        if (keystoreBytes != null) {
            ks.load(keystoreBytes, KEYSTORE_PASSWORD.toCharArray());
        } else {
            ks.load(null, KEYSTORE_PASSWORD.toCharArray());
        }
        Log.d("MD2D", "loadKeystore:\n");
        return ks;
    }

    private static X509Certificate generateCertificate(KeyPair keyPair)
            throws OperatorCreationException, CertificateException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void sendSEHRCertificate() throws Exception {
        FileInputStream fis = new FileInputStream(getKeystorePath(this.context));
        KeyStore keyStore = loadKeystore(fis);
        //Reload the keystore
        keystore = keyStore;

        java.security.cert.Certificate cert = keyStore.getCertificate(KEYSTORE_ALIAS);

        String certs = Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");
        this.write("cert" + certs);
        Log.d("MD2D", "sendSEHRCertificate:\n" + "cert" + certs);
    }

    public static PublicKey loadPublicKey(String alias, char[] password, KeyStore keyStore) throws Exception {

        Key key = keyStore.getKey(alias, password);
        java.security.cert.Certificate[] certs = new java.security.cert.Certificate[0];
        if (key instanceof PrivateKey) {
            System.out.println("Get private key : ");
            System.out.println(key.toString());

            certs = keyStore.getCertificateChain(alias);
            System.out.println("Certificate chain length : " + certs.length);
            for (java.security.cert.Certificate cert : certs) {
                System.out.println(cert.toString());
            }
        } else {
            System.out.println("Key is not private key");
        }
        return (PublicKey) certs[0].getPublicKey();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public String signPayload(String payload, PrivateKey privateKey) throws SignatureException, InvalidKeyException {
        Signature privateSignature = null;
        try {
            privateSignature = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        privateSignature.initSign(privateKey);
        privateSignature.update(payload.getBytes(UTF_8));

        byte[] signature = privateSignature.sign();

        Log.d("MSSG signature", Base64.encodeToString(signature, Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", ""));

        return Base64.encodeToString(signature, Base64.DEFAULT).replaceAll("\r", "").replaceAll("\n", "");
    }

    @Override
    public void storeConsent(String encodedConsent, String signedConsent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        Log.d("MSSG storeConsent", encodedConsent + " " + signedConsent);
        editor.putString("consent", encodedConsent);
        editor.putString("signedConsent", signedConsent);
        editor.commit();
        editor.apply();
    }

    @Override
    public boolean onSymmetricKeyReceivedStore(String symkey) {
        //TODO
        Log.d("MD2D", "onSymmetricKeyRecivedStore" + symkey);

        return false;
    }

    @Override
    public boolean onCertReceivedStore(Certificate cert) {

        Log.d("MD2D", "onCertRecivedStore");

        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null);

            String alias = "mykey";

            // Add the certificate
            keystore.setCertificateEntry(alias, cert);

            Log.d("My App Cert: ", "true");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendConsentAnswer(String consent) {
        this.write(consent);
    }
    //-----------------------
    //---SECURITY-UBIT----END
}
