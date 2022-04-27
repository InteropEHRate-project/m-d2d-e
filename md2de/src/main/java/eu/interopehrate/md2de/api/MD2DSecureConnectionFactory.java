package eu.interopehrate.md2de.api;

import android.content.Context;

import org.hl7.fhir.r4.model.Patient;

public interface MD2DSecureConnectionFactory {

    MD2D createSecureConnection(Patient patient, Context context) throws Exception;

}
