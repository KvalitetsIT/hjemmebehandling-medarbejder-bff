package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BundleBuilderTest {
    private BundleBuilder subject = new BundleBuilder();

    private static final String CAREPLAN_ID = "careplan-1";
    private static final String PATIENT_ID = "patient-1";

    //@Test
    public void skhTest() {
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://localhost:7070/fhir");
        Bundle result = client.search().forResource(Questionnaire.class).where(Questionnaire.RES_ID.exactly().codes("66")).returnBundle(Bundle.class).execute();

         Questionnaire questionnaire = (Questionnaire) result.getEntryFirstRep().getResource();

        Questionnaire.QuestionnaireItemComponent item = questionnaire.addItem();
        item.setLinkId(IdType.newRandomUuid().getValueAsString());
        item.setType(Questionnaire.QuestionnaireItemType.GROUP);
        item.setText("Hvad er dit blodtryk?");

        Questionnaire.QuestionnaireItemComponent display = questionnaire.addItem();
        display.setLinkId(IdType.newRandomUuid().getValueAsString());
        display.setType(Questionnaire.QuestionnaireItemType.DISPLAY);
        display.setText("SYS er det øverste tal på blodtryksapparatet, DIA er det mellemste tal og PUL er det nederste.");

        Questionnaire.QuestionnaireItemComponent sys = item.addItem();
        sys.setLinkId(IdType.newRandomUuid().getValueAsString());
        sys.setText("SYS");
        sys.getCodeFirstRep().setCode("DNK05472").setDisplay("Blodtryk systolisk;Arm");
        sys.setType(Questionnaire.QuestionnaireItemType.QUANTITY);

        Questionnaire.QuestionnaireItemComponent dia = item.addItem();
        dia.setLinkId(IdType.newRandomUuid().getValueAsString());
        dia.setText("DIA");
        dia.getCodeFirstRep().setCode("DNK05473").setDisplay("Blodtryk diastolisk;Arm");
        dia.setType(Questionnaire.QuestionnaireItemType.QUANTITY);

        Questionnaire.QuestionnaireItemComponent pul = item.addItem();
        pul.setLinkId(IdType.newRandomUuid().getValueAsString());
        pul.setText("PUL");
        pul.getCodeFirstRep().setCode("NPU21692").setDisplay("Puls;Hjerte");
        pul.setType(Questionnaire.QuestionnaireItemType.QUANTITY);




//        Questionnaire questionnaire = new Questionnaire();
//        questionnaire.setId("questionnaire-infektionsmedicinsk-2");
//        questionnaire.addExtension(ExtensionMapper.mapOrganizationId("Organization/organization-infektionsmedicinsk"));
//        questionnaire.setTitle("Infektionsmedicinsk spørgeskema");

        System.out.println( FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(questionnaire) );
    }
    @Test
    public void buildCreateCarePlanBundle_mapsArgumentsToEntries() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID, PATIENT_ID);
        Patient patient = buildPatient(PATIENT_ID);

        // Act
        Bundle result = subject.buildCreateCarePlanBundle(carePlan, patient);

        // Assert
        assertEquals(2, result.getEntry().size());
    }

    @Test
    public void buildCreateCarePlanBundle_updatesSubjectReference() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID, PATIENT_ID);
        Patient patient = buildPatient(PATIENT_ID);

        // Act
        Bundle result = subject.buildCreateCarePlanBundle(carePlan, patient);

        // Assert
        assertEquals(patient, result.getEntry().get(1).getResource());
        assertEquals(carePlan.getSubject().getReference(), result.getEntry().get(1).getFullUrl());
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(carePlanId);
        carePlan.setSubject(new Reference(patientId));

        return carePlan;
    }

    private Patient buildPatient(String patientId) {
        Patient patient = new Patient();

        patient.setId(patientId);

        return patient;
    }
}