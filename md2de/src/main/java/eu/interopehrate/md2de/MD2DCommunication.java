package eu.interopehrate.md2de;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.hl7.fhir.r4.model.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.d2d.D2DParameter;
import eu.interopehrate.d2d.D2DParameterConverter;
import eu.interopehrate.d2d.D2DParameterName;
import eu.interopehrate.d2d.D2DRequest;
import eu.interopehrate.d2d.D2DResponse;
import eu.interopehrate.d2d.D2DStatusCodes;
import eu.interopehrate.md2de.api.MD2DConnectionListener;
import eu.interopehrate.md2de.api.MD2D;
import eu.interopehrate.md2ds.MD2DSecurityInterpreter;
import eu.interopehrate.protocols.common.ResourceCategory;
import eu.interopehrate.protocols.server.ResourceServerListener;
import eu.interopehrate.d2d.D2DOperation;

class MD2DCommunication extends Thread implements MD2D {

    private static final String UTF8_NAME = "UTF-8";
    private final Context context;
    public BufferedReader inputChannel;
    public OutputStream outputChannel;
    private ResourceServerListener listener;
    private final MD2DSecurityInterpreter interpreter;
    private final MD2DConnectionListener connectionListener;
    private final Gson gson;
    private final IParser fhirParser;
    private String lineRead;

    public MD2DCommunication(BufferedReader input, OutputStream output, ResourceServerListener listener,
                             MD2DConnectionListener connectionListener,
                             MD2DSecurityInterpreter interpreter, Context context, IParser fhirParser) {
        setResourceServerListener(listener);
        this.connectionListener = connectionListener;
        this.context = context;
        this.interpreter = interpreter;
        this.inputChannel = input;
        this.outputChannel = output;
        this.gson = new GsonBuilder().
                registerTypeAdapter(D2DParameter.class, new D2DParameterConverter())
                .create();
        this.fhirParser = fhirParser;
    }

    @Override
    public void setResourceServerListener(ResourceServerListener listener) {
        this.listener = listener;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void run() {
        D2DRequest request;
        Log.d("MD2D", "Starting loop for reception of requests from HCP...");
        while (true) {
            // Step 1 reception of messages from HCP App
            try {
                lineRead = inputChannel.readLine();
                Log.d("MD2D", "Received message: " + lineRead);
                if (lineRead == null || lineRead.trim().isEmpty()) {
                    Log.d("MD2D", "Warning: received empty line from HCP, line has been ignored.");
                    continue;
                }
            } catch (IOException ioe) {
                Log.d("MD2D", "Lost connection with HCP App, closing communication channels.");
                closeCommunicationChannels();
                break;
            }

            // Step 2 Deserialization of the message to check if it is a valid D2DRequest
            try {
                Log.d("MD2D", "Deserializing received string into a D2DRequest...");
                request = gson.fromJson(lineRead, D2DRequest.class);
            } catch (Exception e) {
                D2DResponse response = new D2DResponse();
                response.setMessage("Error while deserializing request: " + e.getMessage());
                response.setStatus(D2DStatusCodes.BAD_REQUEST);
                try {
                    sendErrorResponse(response);
                } catch (Exception exception) {
                    Log.e("MD2D", "Error while sending error response", exception);
                }
                continue;
            }

            // Step 3 management of the request
            try {
                if (request.getOperation() == D2DOperation.WRITE) {
                    handleWriteOperation(request);
                } else if (request.getOperation() == D2DOperation.READ) {
                    handleReadOperation(request);
                } else if (request.getOperation() == D2DOperation.SEARCH) {
                    handleSearchOperation(request);
                } else if (request.getOperation() == D2DOperation.CLOSE_CONNECTION) {
                    Log.d("MD2D", "Received request for closing connection, closing communication channels.");
                    closeCommunicationChannels();
                    break;
                }
            } catch (Exception e) {
                D2DResponse response = new D2DResponse(request);
                response.setMessage("Error during request handling: " + e.getMessage());
                response.setStatus(D2DStatusCodes.SEHR_APP_ERROR);
                try {
                    sendErrorResponse(response);
                } catch (Exception exception) {
                    Log.e("MD2D", "Error while sending error response", exception);
                }
            }
        }

        Log.d("MD2D", "Ended loop for reception of requests from HCP.");
    }


    private void handleReadOperation(D2DRequest request) throws Exception {
        Log.d("MD2D", "Received a READ request.");
        List<Object> idsValues = request.getAllOccurencesValuesByName(D2DParameterName.ID);
        // TODO: surround with try / catch and handle error response
        Log.d("MD2D", "Invoking listener.onResourcesRequested().");
        Bundle bundle = listener.onResourcesRequested(idsValues.toArray(new String[0]));
        // sends data to S-EHR
        sendEncryptedD2DResponse(request, bundle);
    }


    private void handleWriteOperation(D2DRequest request) throws Exception {
        Log.d("MD2D", "Received a WRITE request.");
        if (request.getBody() != null) {
            Log.d("MD2D", "Decrypting received request body...");
            String decryptedBundle = interpreter.decrypt(request.getBody());
            Bundle bundle = fhirParser.parseResource(Bundle.class, decryptedBundle);
            Log.d("MD2D", "Invoking listener.onResourcesReceived().");
            // may be called in a new Thread
            listener.onResourcesReceived(bundle);
        } else {
            Log.d("MD2D", "Received WRITE request with empty body. Request is ignored.");
        }
    }

    private void handleSearchOperation(D2DRequest request) throws Exception {
        Log.d("MD2D", "Received a SEARCH request.");
        List<Object> categoriesValues = request.getAllOccurencesValuesByName(D2DParameterName.CATEGORY);
        ResourceCategory[] categoriesValuesArray = categoriesValues.toArray(new ResourceCategory[0]);
        String subCategory = (String) request.getFirstOccurenceValueByName(D2DParameterName.SUB_CATEGORY);
        String type = (String) request.getFirstOccurenceValueByName(D2DParameterName.TYPE);
        Date from = (Date) request.getFirstOccurenceValueByName(D2DParameterName.DATE);
        Boolean isSummary = (Boolean) request.getFirstOccurenceValueByName(D2DParameterName.SUMMARY);
        Integer mostRecentSize = (Integer) request.getFirstOccurenceValueByName(D2DParameterName.MOST_RECENT);

        Log.d("MD2D", "category      :" + categoriesValues);
        Log.d("MD2D", "subCategory   :" + subCategory);
        Log.d("MD2D", "type          :" + type);
        Log.d("MD2D", "from          :" + from);
        Log.d("MD2D", "isSummary     :" + isSummary);
        Log.d("MD2D", "mostRecentSize:" + mostRecentSize);

        Bundle bundle;
        Log.d("MD2D", "Invoking listener.onResourcesRequested().");
        if (categoriesValues.size() == 0) {
            bundle = listener.onResourcesRequested(from, isSummary); //all
        } else if (categoriesValues.size() == 1) {
            if (mostRecentSize == null)
                bundle = listener.onResourcesRequested(categoriesValuesArray[0], subCategory, type, from, isSummary); //ps and category
            else
                bundle = listener.onResourcesRequested(categoriesValuesArray[0], subCategory, type, mostRecentSize, isSummary); //mr_image and mr_lab
        } else {
            Log.d("MD2D", "cnt");
            bundle = listener.onResourcesRequested(from, isSummary, categoriesValuesArray); //categories
        }
        if (bundle == null) bundle = new Bundle();
        // sends data to HCP
        sendEncryptedD2DResponse(request, bundle);
    }

    /**
     * Used to send a single response, mainly used for sending UNSUCCESSFUL responses
     * without a body
     * @param response
     */
    private void sendErrorResponse(D2DResponse response) throws Exception {
        Log.d("MD2D", "Sending error response");
        outputChannel.write(gson.toJson(response).getBytes(UTF8_NAME));
        outputChannel.write("\n".getBytes(UTF8_NAME));
        outputChannel.flush();
    }

    /**
     * Used in case of a SUCCESSFUL request execution to send multiple responses due to the large
     * amount of data that overcomes UTF String dimensions
     *
     * @param request
     * @param healthData
     * @throws Exception
     */
    private void sendEncryptedD2DResponse(D2DRequest request, Bundle healthData) throws Exception {
        long methodStart = System.currentTimeMillis();

        final int bundleSize = healthData.getEntry().size();
        int itemsPerPage = request.getHeader().getItemsPerPage();
        if (itemsPerPage <= 0 || bundleSize < itemsPerPage)
            itemsPerPage = bundleSize;

        int totalBytes = 0;
        Bundle transportBundle = new Bundle();
        List<Bundle.BundleEntryComponent> healtDataList = healthData.getEntry();

        // all data can be sent in one page, it includes the case when bundle is 0 size
        if(bundleSize <= itemsPerPage) {
            Log.d("MD2D", "Receive a bundle with " + bundleSize + " items, " +
                    " data will be sent to HCP in 1 page.");
            totalBytes = sendEncryptedPage(request, healthData, 1, 1);
        } else {
            // calculates needed pages
            int totalPages = bundleSize / itemsPerPage;
            if (bundleSize % itemsPerPage != 0)
                totalPages++;

            Log.d("MD2D", "Receive a bundle with " + bundleSize + " items, " +
                    " data will be sent to HCP partitioned into " + totalPages + " pages.");

            int pageCounter = 1;
            for (int i = 0; i < bundleSize; i++) {
                transportBundle.addEntry(healtDataList.get(i));

                if (transportBundle.getEntry().size() == itemsPerPage || i == bundleSize - 1) {
                    Log.d("MD2D", "Sending page number: " + pageCounter);
                    totalBytes += sendEncryptedPage(request, transportBundle, pageCounter, totalPages);
                    pageCounter++;
                    transportBundle = new Bundle();
                }
            }
        }

        Log.d("MD2D", "Total bytes sent " + totalBytes + " sent in "
                + ((System.currentTimeMillis() - methodStart) / 1000F) + " seconds");
    }

    private int sendEncryptedPage(D2DRequest request, Bundle healthData, int page, int pages) throws Exception {
        healthData.setType(Bundle.BundleType.SEARCHSET);
        final String serializedBundle = fhirParser.encodeResourceToString(healthData);
        final String encryptedBundle = interpreter.encrypt(serializedBundle);
        D2DResponse response = new D2DResponse(request);
        response.setStatus(D2DStatusCodes.SUCCESSFULL);
        response.setBody(encryptedBundle);
        response.getHeader().setTotalPages(pages);
        response.getHeader().setPage(page);
        // serializing overall response
        final String serializedResponse = gson.toJson(response);
        byte[] responseBytes = serializedResponse.getBytes(UTF8_NAME);
        // totalBytes += responseBytes.length;
        long transmissionStart = System.currentTimeMillis();
        outputChannel.write(responseBytes);
        outputChannel.write("\n".getBytes(UTF8_NAME));
        outputChannel.flush();
        long end = System.currentTimeMillis();
        Log.d("MD2D", "Sent " + responseBytes.length + " bytes in "
                + ((System.currentTimeMillis() - transmissionStart) / 1000F) + " seconds");

        return responseBytes.length;
    }

    private void closeCommunicationChannels() {
        try {
            inputChannel.close();
        } catch (IOException ioException) {
            Log.w("MD2D", "Warning closing inputChannel.", ioException);
        }

        try {
            outputChannel.close();
        } catch (IOException ioException) {
            Log.w("MD2D", "Warning closing outputChannel.", ioException);
        }
        if(connectionListener != null) {
            connectionListener.onConnectionClosure();
        }
    }

    /*
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void oldrun() {
        Bundle bundle;
        D2DRequest request;
        D2DResponse response;
        Log.d("MD2D", "Starting loop for reception of request from HCP...");
        while (true) {
            try {
                lineRead = inputChannel.readLine();
                Log.d("MD2D", "lineRead: " + lineRead);

                // Checking of the
                if (lineRead == null || lineRead.trim().isEmpty()) {
                    Log.d("MD2D", "Warning: received empty line from HCP, line has been ignored.");
                    continue;
                }

                // remove characters added by BT transmission
                // lineRead = lineRead.startsWith("?enc") ? lineRead.substring(1) : lineRead.substring(2);

                try {
                    Log.d("MD2D", "Deserializing received string into a D2DRequest...");
                    request = gson.fromJson(lineRead, D2DRequest.class);
                } catch (Exception e) {
                    response = new D2DResponse();
                    response.setMessage("Error while deserializing request: " + e.getMessage());
                    response.setStatus(D2DStatusCodes.BAD_REQUEST);
                    sendErrorResponse(response);
                    continue;
                }

                // Analyse request and invokes corresponding listener operation
                if (request.getOperation() == D2DOperation.WRITE) {
                    Log.d("MD2D", "Received a WRITE request.");
                    if (request.getBody() != null) {
                        Log.d("MD2D", "Decrypting received request body...");
                        String decryptedBundle = interpreter.decrypt(request.getBody());
                        bundle = fhirParser.parseResource(Bundle.class, decryptedBundle);
                        Log.d("MD2D", "Invoking listener.onResourcesReceived().");
                        listener.onResourcesReceived(bundle);
                    } else {
                        Log.d("MD2D", "Received WRITE request with empty body. Request is ignored.");
                        continue;
                    }
                } else if (request.getOperation() == D2DOperation.READ) {
                    Log.d("MD2D", "Received a READ request.");
                    List<Object> idsValues = request.getAllOccurencesValuesByName(D2DParameterName.ID);
                    // TODO: surround with try / catch and handle error response
                    Log.d("MD2D", "Invoking listener.onResourcesRequested().");
                    bundle = listener.onResourcesRequested(idsValues.toArray(new String[0]));
                    // creates response and load it with results
                    bundle.setType(Bundle.BundleType.SEARCHSET);
                    // sends data to S-EHR
                    sendEncryptedD2DResponse(request, bundle);
                    // sendEncryptedD2DResponse(response);
                } else if (request.getOperation() == D2DOperation.SEARCH) {
                    Log.d("MD2D", "Received a SEARCH request.");
                    List<Object> categoriesValues = request.getAllOccurencesValuesByName(D2DParameterName.CATEGORY);
                    ResourceCategory[] categoriesValuesArray = categoriesValues.toArray(new ResourceCategory[0]);
                    String subCategory = (String) request.getFirstOccurenceValueByName(D2DParameterName.SUB_CATEGORY);
                    String type = (String) request.getFirstOccurenceValueByName(D2DParameterName.TYPE);
                    Date from = (Date) request.getFirstOccurenceValueByName(D2DParameterName.DATE);
                    Boolean isSummary = (Boolean) request.getFirstOccurenceValueByName(D2DParameterName.SUMMARY);
                    Integer mostRecentSize = (Integer) request.getFirstOccurenceValueByName(D2DParameterName.MOST_RECENT);

                    Log.d("MD2D", "category      :" + categoriesValues);
                    Log.d("MD2D", "subCategory   :" + subCategory);
                    Log.d("MD2D", "type          :" + type);
                    Log.d("MD2D", "from          :" + from);
                    Log.d("MD2D", "isSummary     :" + isSummary);
                    Log.d("MD2D", "mostRecentSize:" + mostRecentSize);

                    bundle = null;
                    Log.d("MD2D", "Invoking listener.onResourcesRequested().");
                    // TODO: surround with try / catch and handle error response
                    if (categoriesValues.size() == 0) {
                        bundle = listener.onResourcesRequested(from, isSummary);
                    } else if (categoriesValues.size() == 1) {
                        if (mostRecentSize == null)
                            bundle = listener.onResourcesRequested(categoriesValuesArray[0], subCategory, type, from, isSummary);
                        else
                            bundle = listener.onResourcesRequested(categoriesValuesArray[0], subCategory, type, mostRecentSize, isSummary);
                    } else {
                        Log.d("MD2D", "cnt");
                        bundle = listener.onResourcesRequested(from, isSummary, categoriesValuesArray);
                    }
                    if (bundle == null) bundle = new Bundle();
                    // send data to S-EHR
                    sendEncryptedD2DResponse(request, bundle);
                }
            } catch (Exception e) {
                Log.e("MD2D", e.getMessage(), e);
                response = new D2DResponse();
                response.setMessage("Error while handling request: " + e.getMessage());
                response.setStatus(D2DStatusCodes.D2D_ERROR);
                try {
                    sendErrorResponse(response);
                } catch (Exception exception) {
                    Log.e("MD2D", exception.getMessage(), exception);
                }
            }
        }
   }
*/

}
