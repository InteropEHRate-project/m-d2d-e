package eu.interopehrate.md2de;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.d2d.D2DSecurityMessage;
import eu.interopehrate.d2d.D2DSecurityOperation;
import eu.interopehrate.md2de.api.MD2DListener;
import eu.interopehrate.md2de.api.MD2DSecureConnectionFactory;
import eu.interopehrate.md2de.api.MD2D;
import eu.interopehrate.md2ds.MD2DSecurityInterpreter;
import eu.interopehrate.md2ds.MD2DSecurityUtilities;

class MD2DSecureConnectionFactoryImpl implements MD2DSecureConnectionFactory {

    private static final String UTF8_NAME = "UTF-8";
    private final MD2DListener d2DListener;
    private final OutputStream outputChannel;
    private final BufferedReader inputChannel;
    private MD2DSecurityInterpreter securityInterpreter;
    private final Gson gson = new Gson();

    public MD2DSecureConnectionFactoryImpl(MD2DListener d2DListener,
                                           InputStream in, OutputStream out) throws UnsupportedEncodingException {
        // opens input channel
        this.inputChannel = new BufferedReader(new InputStreamReader(in, UTF8_NAME));
        // opens output channel;
        this.outputChannel = new BufferedOutputStream(out);
        this.d2DListener = d2DListener;
    }

    @Override
    public MD2D createSecureConnection(Patient patient, Context context) throws Exception {
        Log.d("MD2D", "Starting secure connection protocol, waiting for HELLO_SEHR message...");
        // TODO: initialize security
        MD2DSecurityUtilities.initialize(context);

        // Creates the only instance of parser for FHIR resources
        IParser fhirParser = FhirContext.forR4().newJsonParser();

        // Step 1: Receive the HELLO_SEHR msg and reply with HELLO_HCP message
        D2DSecurityMessage incomingMsg = waitForMsg();
        if (incomingMsg.getOperation() != D2DSecurityOperation.HELLO_SEHR)
            throw new IllegalStateException("Received " + incomingMsg.getOperation()
                    + " message while expecting HELLO_SEHR message.");

        if (incomingMsg.getBody() == null || incomingMsg.getBody().isEmpty())
            throw new IllegalStateException("Received empty message, secure connection protocol aborted.");

        Practitioner hcpUser = (Practitioner)fhirParser.parseResource(incomingMsg.getBody());
        if (!d2DListener.onHealthOrganizationIdentityReceived(hcpUser)) {
            Log.i("MD2D", "D2DSecureConnection not established, " +
                    "practitioner identity not matched by Citizen");
            throw new IllegalStateException();
        }

        D2DSecurityMessage replyMsg = new D2DSecurityMessage();
        replyMsg.setOperation(D2DSecurityOperation.HELLO_HCP);
        replyMsg.setBody(fhirParser.encodeResourceToString(patient));
        sendReply(replyMsg);
        // sends hello

        // Step 2: receive consent, sign it and send it back

        // Step 3: Receive the HCP_CERTIFICATE msg and reply with SEHR_CERTIFICATE message
//        Log.d("MD2D", "Waiting for HCP_CERTIFICATE message...");
//        incomingMsg = waitForMsg();
//        if (incomingMsg.getOperation() != D2DSecurityOperation.HCP_CERTIFICATE)
//            throw new IllegalStateException("Received " + incomingMsg.getOperation()
//                    + " message while expecting HCP_CERTIFICATE message.");
//
//        if (incomingMsg.getBody() == null || incomingMsg.getBody().isEmpty())
//            throw new IllegalStateException("Received empty message, secure connection protocol aborted.");
//
//        // TODO: store somewhere the certificate
//        replyMsg = new D2DSecurityMessage();
//        replyMsg.setOperation(D2DSecurityOperation.SEHR_CERTIFICATE);
//        // retrieve the SEHR certificate from someone
//        replyMsg.setBody(MD2DSecurityUtilities.getSEHRCertificate(context));
//        sendReply(replyMsg);

        // Step 4: Receive the HCP_PUBLIC_KEY msg and reply with SEHR_PUBLIC_KEY message
        Log.d("MD2D", "Waiting for HCP_PUBLIC_KEY message...");
        incomingMsg = waitForMsg();
        if (incomingMsg.getOperation() != D2DSecurityOperation.HCP_PUBLIC_KEY)
            throw new IllegalStateException("Received " + incomingMsg.getOperation()
                    + " message while expecting HCP_PUBLIC_KEY message.");

        if (incomingMsg.getBody() == null || incomingMsg.getBody().isEmpty())
            throw new IllegalStateException("Received empty message, secure connection protocol aborted.");

        securityInterpreter = new MD2DSecurityInterpreter(incomingMsg.getBody());

        replyMsg = new D2DSecurityMessage();
        replyMsg.setOperation(D2DSecurityOperation.SEHR_PUBLIC_KEY);
        // retrieve the Public Key from someone and generates the symmetric key
        replyMsg.setBody(securityInterpreter.getSessionPublicKey());
        sendReply(replyMsg);
        Log.d("MD2D", "Generating Symmetric Key...");

        // Step 4
        Log.d("MD2D", "Secure connection successfully established");

        MD2DCommunication md2d = new MD2DCommunication(this.inputChannel, this.outputChannel,
                this.d2DListener, null,
                securityInterpreter, context, fhirParser);

        // starts infinite loop for reception of messages
        md2d.start();

        return md2d;
    }

    private D2DSecurityMessage waitForMsg() throws Exception {
        String incomingMsg = inputChannel.readLine();
        Log.d("MD2D", incomingMsg);
        //incomingMsg = incomingMsg.substring(2);
        return gson.fromJson(incomingMsg, D2DSecurityMessage.class);
    }

    private void sendReply(D2DSecurityMessage outMsg) throws Exception {
        outputChannel.write(gson.toJson(outMsg).getBytes(UTF8_NAME));
        outputChannel.write("\n".getBytes(UTF8_NAME));
        outputChannel.flush();
    }

}
