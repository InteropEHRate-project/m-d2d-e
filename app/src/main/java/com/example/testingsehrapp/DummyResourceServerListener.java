package com.example.testingsehrapp;

import android.util.Log;

import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;

import java.util.Date;

import eu.interopehrate.md2de.api.MD2DListener;
import eu.interopehrate.protocols.common.ResourceCategory;

public class DummyResourceServerListener implements MD2DListener {
    @Override
    public Bundle onResourcesRequested(String... ids) {
        Log.d("MD2D","MD2DListener.onResourcesRequested(ids)");
        return new Bundle();
    }

    @Override
    public Bundle onResourcesRequested(Date from, boolean isSummary) {
        Log.d("MD2D","MD2DListener.onResourcesRequested(from, isSummary)");
        return new Bundle();
    }

    @Override
    public Bundle onResourcesRequested(Date from, boolean isSummary, ResourceCategory... categories) {
        Log.d("MD2D","MD2DListener.onResourcesRequested(from, isSummary, categories)");
        return new Bundle();
    }

    @Override
    public Bundle onResourcesRequested(ResourceCategory category, String subCategory, String type, Date from, boolean isSummary) {
        Log.d("MD2D","MD2DListener.onResourcesRequested(category, subCategory, type, from, isSummary)");
        int numObs = 2000;
        Log.d("MD2D","Creating " + numObs + " fakes observations...");

        Bundle b = new Bundle();
        b.setType(Bundle.BundleType.SEARCHSET);

        Observation obs;
        Coding code;
        Identifier id;
        for (int i = 0; i < numObs; i++) {
            obs = new Observation();
            id = new Identifier();
            id.setValue(String.valueOf(i));
            obs.addIdentifier(id);
            obs.addCategory(new CodeableConcept(new Coding(
                    "http://terminology.hl7.org/CodeSystem/observation-category",
                    "vital-signs", "")));
            obs.setStatus(Observation.ObservationStatus.FINAL);
            obs.setEffective(new DateTimeType());
            obs.addNote(new Annotation().setText("This is the a dummy annotation for observation " +
                    "number " + (i + 1) + " created by Charis and Alessio."));

            code = new Coding("http://loinc.org", "29463-7", "Body Weight");
            obs.setCode(new CodeableConcept(code));

            Quantity q = new Quantity();
            q.setValue(100);
            q.setUnit("kg");
            q.setSystem("http://unitsofmeasure.org");
            q.setCode("kg");
            obs.setValue(q);


            b.addEntry().setResource(obs);
        }

        return b;
    }

    @Override
    public Bundle onResourcesRequested(ResourceCategory category, String subCategory, String type, int mostRecentSize, boolean isSummary) {
        Log.d("MD2D","MD2DListener.onResourcesRequested(category, subCategory, type, mostRecentSize, isSummary)");
        return new Bundle();
    }

    @Override
    public void onResourcesReceived(Bundle healthDataBundle) {
        Log.d("MD2D","MD2DListener.onResourcesReceived(healthDataBundle)");
    }

    @Override
    public boolean onHealthOrganizationIdentityReceived(Practitioner practitioner) {
        Log.d("MD2D","MD2DListener.onHealthOrganizationIdentityReceived(practitioner)");
        return true;
    }
}
