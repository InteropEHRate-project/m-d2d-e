package eu.interopehrate.md2de.api;

import android.content.Context;

import java.io.IOException;

/*
 *		Author: University of Piraeus Research Center
 *		Project: InteropEHRate - www.interopehrate.eu
 *
 *	Description: Interface of device-to-device (D2D) connection compliant compliant to D2D specifications.
 *				 It allows the S-EHR application to initiate the Bluetooth connection between the S-EHR app and the HCP App.
 *				 Also it allows the closure of the connection between the two devices.
 */

public interface MD2DConnectionFactory {

    /**
     *
     * Responsible for connecting with the HCP who advertises a specified Bluetooth connection.
     *
     * @param address the MAC address of the Bluetooth adapter that the HCP is using.
     * @param listeners
     * @param listenersConnection
     *
     * @return the new thread that was opened for listening for incoming messages.
     */
    public MD2DSecureConnectionFactory broadcastConnection(String address,
                                                           MD2DListener listeners,
                                                           MD2DConnectionListener listenersConnection,
                                                           Context context) throws Exception;

    /**
     *
     * Responsbile for closing the Bluetooth Connection.
     */
    public void closeConnection() throws IOException;

}
