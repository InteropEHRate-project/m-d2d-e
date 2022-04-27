package eu.interopehrate.md2de.api;


/*
 *		Author: University of Piraeus Research Center
 *		Project: InteropEHRate - www.interopehrate.eu
 *
 *	Description: Interface of device-to-device (D2D) connection listeners compliant to D2D specifications.
 *			 It allows a patient to have a connection closure message when the connection is closed.
 */

public interface MD2DConnectionListener {

    /**
     *
     * Responsible for informing the HCP app that the connection has been closed.
     *
     */
    public void onConnectionClosure();
}
