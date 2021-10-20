package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.model.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {
    @InjectMocks
    private CarePlanService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private FhirObjectBuilder fhirObjectBuilder;

    @Test
    public void getCarePlan_carePlanPresent_returnsCarePlan() {
        // Arrange
        String carePlanId = "careplan-1";
        String patientId = "Patient/patient-1";

        CarePlanModel carePlanModel = setupCarePlan(carePlanId, patientId);
        PatientModel patientModel = setupPatient(patientId);

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertEquals(carePlanModel, result.get());
        assertEquals(patientModel, result.get().getPatient());
    }

    @Test
    public void getCarePlan_patientMissing_throwsException() {
        // Arrange
        String carePlanId = "careplan-1";
        String patientId = "Patient/patient-1";

        CarePlanModel carePlanModel = setupCarePlan(carePlanId, patientId);
        Mockito.when(fhirClient.lookupPatientById(patientId)).thenReturn(Optional.empty());

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.getCarePlan(carePlanId));
    }

    @Test
    public void getCarePlan_carePlanMissing_returnsEmpty() {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.when(fhirClient.lookupCarePlan(carePlanId)).thenReturn(Optional.empty());

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void getCarePlan_carePlanPresent_includesQuestionnaires() {
        // Arrange
        String carePlanId = "careplan-1";
        String patientId = "Patient/patient-1";
        String questionnaireId = "questionnaire-1";

        setupPatient(patientId);

        CarePlanModel carePlanModel = setupCarePlan(carePlanId, patientId, questionnaireId);
        QuestionnaireModel questionnaireModel = setupQuestionnaire(questionnaireId);

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertEquals(carePlanModel, result.get());
        assertEquals(1, result.get().getQuestionnaires().size());
        assertEquals(questionnaireModel, result.get().getQuestionnaires().get(0).getQuestionnaire());
    }

    private CarePlanModel setupCarePlan(String carePlanId, String patientId) {
        return setupCarePlan(carePlanId, patientId, null);
    }

    private CarePlanModel setupCarePlan(String carePlanId, String patientId, String questionnaireId) {
        CarePlan carePlan = buildCarePlan(carePlanId, patientId, questionnaireId);
        Mockito.when(fhirClient.lookupCarePlan(carePlanId)).thenReturn(Optional.of(carePlan));

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan)).thenReturn(carePlanModel);

        return carePlanModel;
    }

    private PatientModel setupPatient(String patientId) {
        Patient patient = new Patient();
        Mockito.when(fhirClient.lookupPatientById(patientId)).thenReturn(Optional.of(patient));

        PatientModel patientModel = new PatientModel();
        Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

        return patientModel;
    }

    private QuestionnaireModel setupQuestionnaire(String questionnaireId) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setIdElement(new IdType(questionnaireId));
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(questionnaireId))).thenReturn(List.of(questionnaire));

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        FrequencyModel frequencyModel = new FrequencyModel();
        Mockito.when(fhirMapper.mapTiming(Mockito.any())).thenReturn(frequencyModel);

        return questionnaireModel;
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId, String questionnaireId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(carePlanId);
        carePlan.setSubject(new Reference(patientId));

        if(questionnaireId != null) {
            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();
            detail.setInstantiatesCanonical(List.of(new CanonicalType(questionnaireId)));
            detail.setScheduled(new Timing());
            carePlan.addActivity().setDetail(detail);
        }

        return carePlan;
    }
}