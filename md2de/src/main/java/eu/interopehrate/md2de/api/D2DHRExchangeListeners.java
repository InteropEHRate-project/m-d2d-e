package eu.interopehrate.md2de.api;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Practitioner;
import java.util.Date;

/*
 *		Author: University of Piraeus Research Center
 *		Project: InteropEHRate - www.interopehrate.eu
 *
 *	Description: Interface of device-to-device (D2D) HR exchange listeners compliant to D2D specifications.
 *			 It allows a citizen to send the requested data and to know when incoming data from the Health Practitioner has been received.
 */
@Deprecated
public interface D2DHRExchangeListeners {


        /**
         *
         * Responsible for informing the S-EHR app that the Healthcare Organization personal identity has been received from the side of the HCP app.
         *
         * @param practitioner r: the HCP’s demographic data, with a reference to the data related to the HCP’s organization in the form of a FHIR object.
         *
         */
        public void onHealthOrganizationIdentityReceived(Practitioner practitioner);

        /**
         *
         * Responsible for showing the consent details from the HCP .
         *
         * @param consent a boolean that represents the answer regarding the case that the citizen has approved the consent to provide her data or not.
         * @param signedConsent
         *
         */
        public void onConsentRequested(Consent consent, String signedConsent);

        /**
         *
         * Responsible for informing the S-EHR app that the prescription of the patient summary has been received from the side of the HCP app.
         *
         * @param medicationRequest prescription data in a form of MedicationRequest (i.e. FHIR Resource Bundle).
         *
         */
        public void onPrescriptionReceived(Bundle medicationRequest);

        /**
         *
         * Responsible for informing the S-EHR app that the singature and hence identity is verified.
         *
         * @param result
         *
         */
        public void onVerify(boolean result);

        /**
         *
         * Responsible for informing the S-EHR app that the vital signs of the patient has been received from the side of the HCP app.
         *
         * @param vitalSigns prescription data in a form of MedicationRequest (i.e. FHIR Resource Bundle).
         *
         */
        public void onVitalSignsReceived(Bundle vitalSigns);

        /**
         *
         * Responsible for informing the S-EHR app that the request for the medical document history of the patient has been received from the side of the HCP app.
         *
         * @param startingDate The starting date
         *
         * @param endingDate The ending date
         *
         * @param type The type of the documents
         */
        public void onMedicalDocumentRequestReceived(String startingDate, String endingDate, String type);

}
