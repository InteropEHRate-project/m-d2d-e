package eu.interopehrate.md2de.api;

import org.hl7.fhir.r4.model.Practitioner;

import eu.interopehrate.protocols.server.ResourceServerListener;

public interface MD2DListener extends ResourceServerListener {

    /**
     * Method invoked during the security handshake to let the citizen identify the Practitioner.
     *
     * @param practitioner
     * @return: true if Practitioner has been recognized, otherwise false
     */
    boolean onHealthOrganizationIdentityReceived(Practitioner practitioner);

}
