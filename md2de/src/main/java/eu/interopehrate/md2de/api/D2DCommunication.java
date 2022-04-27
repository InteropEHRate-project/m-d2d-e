package eu.interopehrate.md2de.api;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.MedicationRequest;

/*
 *		Author: University of Piraeus Research Center
 *		Project: InteropEHRate - www.interopehrate.eu
 *
 *	Description: Interface of device-to-device (D2D) communication compliant to D2D specifications.
 *				  It allows a Patient to send data to a Health Practitioner.
 */

@Deprecated
public interface D2DCommunication {

    /**
     *
     * Responsible for sending the citizen's personal data to the HCP application
     *
     * @param patient the personal data of the citizen. (in FHIR structure)
     *
     */
    public void sendPersonalIdentity(Patient patient);

    /**
     *
     * Responsible for sending the citizen's patient summary to the HCP application
     *
     * @param patientSummary the Patient Summary of the citizen. (in FHIR structure)
     */
    public void sendPatientSummary(Bundle patientSummary) throws Exception;

    /**
     *
     * Responsible for sending the citizen's prescription to the HCP application
     *
     * @param prescription the prescription of the citizen. (in FHIR structure)
     */
    public void sendPrescription(Bundle prescription) throws Exception;

    /**
     *
     * Responsible for sending the citizen's laboratory results to the HCP application
     *
     * @param laboratoryResults the laboratory results of the citizen. (in FHIR structure)
     */
    public void sendLaboratoryResults(Bundle laboratoryResults) throws Exception;

    /**
     *
     * Responsible for sending the citizen's image report to the HCP application
     *
     * @param imageReport the image report of the citizen. (in FHIR structure)
     */
    public void sendImageReport(Bundle imageReport) throws Exception;

    /**
     *
     * Responsible for sending the citizen's pathology history information to the HCP application
     *
     * @param pathologyHistory the pathology history information of the citizen. (in FHIR structure)
     */
    public void sendPathologyHistoryInformation(Bundle pathologyHistory) throws Exception;

    /**
     *
     * Responsible for sending the citizen's medical document consultation to the HCP application
     *
     * @param medicalDocument the medical document consultation of the citizen. (in FHIR structure)
     */
    public void sendMedicalDocumentConsultation(Bundle medicalDocument) throws Exception;

    /**
     *
     * Responsible for sending the citizen's vital signs to the HCP application
     *
     * @param vitalSigns the vital signs of the citizen. (in FHIR structure)
     */
    public void sendVitalSigns(Bundle vitalSigns) throws Exception;
}
