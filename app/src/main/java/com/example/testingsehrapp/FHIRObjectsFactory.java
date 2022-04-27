package com.example.testingsehrapp;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

//import org.hl7.fhir.r4.model.Bundle;

public class FHIRObjectsFactory {

    public Patient buildPatient(/*Practitioner gp*/) {
        Patient p = new Patient();
        p.setId(UUID.randomUUID().toString());

        GregorianCalendar bd = new GregorianCalendar(1963, Calendar.MARCH, 24);
        p.setBirthDate(bd.getTime());

        HumanName name = new HumanName();
        name.setFamily("Rossi").addGiven("Maria");
        p.addName(name);

        p.setGender(Enumerations.AdministrativeGender.FEMALE);

        p.addAddress().addLine("Piazza di Spagna")
                .setCity("Roma")
                .setState("Italia")
                .setPostalCode("87654")
                .setUse(Address.AddressUse.HOME);

        return p;
    }

    public org.hl7.fhir.r4.model.Bundle buildBundle(){
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        return bundle;
    }
}
