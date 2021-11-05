package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class QuestionnaireResponseService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private FhirObjectBuilder fhirObjectBuilder;

    public QuestionnaireResponseService(FhirClient fhirClient, FhirMapper fhirMapper, FhirObjectBuilder fhirObjectBuilder) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.fhirObjectBuilder = fhirObjectBuilder;
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String cpr, List<String> questionnaireIds) throws ServiceException {
        List<QuestionnaireResponse> responses = fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds);
        if(responses.isEmpty()) {
            return List.of();
        }

        // Look up questionnaires
        Map<String, Questionnaire> questionnairesById = getQuestionnairesById(questionnaireIds);

        // Look up patient
        Optional<Patient> patient = fhirClient.lookupPatientByCpr(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up patient for cpr %s!", cpr));
        }

        return constructResult(responses, questionnairesById, Map.of(patient.get().getId(), patient.get()));
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        // Get the questionnaires by status
        List<QuestionnaireResponse> responses = fhirClient.lookupQuestionnaireResponsesByStatus(statuses);
        if(responses.isEmpty()) {
            return List.of();
        }

        // Extract the questionnaireIds, get the questionnaires
        Set<String> questionnaireIds = responses.stream().map(qr -> qr.getQuestionnaire()).collect(Collectors.toSet());
        Map<String, Questionnaire> questionnairesById = getQuestionnairesById(questionnaireIds);

        // Extract the patientIds, get the patients
        Set<String> patientIds = responses.stream().map(qr -> qr.getSubject().getReference()).collect(Collectors.toSet());
        Map<String, Patient> patientsById = getPatientsById(patientIds);

        // Return the result
        return constructResult(responses, questionnairesById, patientsById);
    }

    public void updateExaminationStatus(String questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException {
        // Look up the QuestionnaireResponse
        Optional<QuestionnaireResponse> questionnaireResponse = fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId);
        if(!questionnaireResponse.isPresent()) {
            throw new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId));
        }

        // Update the Questionnaireresponse
        fhirObjectBuilder.updateExaminationStatusForQuestionnaireResponse(questionnaireResponse.get(), examinationStatus);

        // Save the updated QuestionnaireResponse
        fhirClient.updateQuestionnaireResponse(questionnaireResponse.get());
    }

    private Map<String, Questionnaire> getQuestionnairesById(Collection<String> questionnaireIds) {
        Set<String> distinctIds = questionnaireIds.stream().collect(Collectors.toSet());

        Map<String, Questionnaire> questionnairesById = fhirClient.lookupQuestionnaires(distinctIds)
                .stream()
                .collect(Collectors.toMap(q -> q.getIdElement().toUnqualifiedVersionless().getValue(), q -> q));

        if(!distinctIds.equals(questionnairesById.keySet())) {
            throw new IllegalStateException("Could not look up every questionnaire when retrieving questionnaireResponses!");
        }
        return questionnairesById;
    }

    private Map<String, Patient> getPatientsById(Collection<String> patientIds) {
        Set<String> distinctIds = patientIds.stream().collect(Collectors.toSet());

        Map<String, Patient> patientsById = fhirClient.lookupPatientsById(distinctIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p));

        if(!distinctIds.equals(patientsById.keySet())) {
            throw new IllegalStateException("Could not look up every patient when retrieving questionnaireResponses!");
        }
        return patientsById;
    }

    private List<QuestionnaireResponseModel> constructResult(List<QuestionnaireResponse> responses, Map<String, Questionnaire> questionnairesById, Map<String, Patient> patientsById) {
        return responses
                .stream()
                .map(qr -> fhirMapper.mapQuestionnaireResponse(qr, questionnairesById.get(qr.getQuestionnaire()), patientsById.get(qr.getSubject().getReference())))
                .collect(Collectors.toList());
    }
}
