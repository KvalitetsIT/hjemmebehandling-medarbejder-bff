package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
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

    public QuestionnaireResponseService(FhirClient fhirClient, FhirMapper fhirMapper) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String cpr, List<String> questionnaireIds) throws ServiceException {
        List<QuestionnaireResponse> questionnaireResponses = fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds);
        if(questionnaireResponses.isEmpty()) {
            return List.of();
        }

        // Look up questionnaires
        Map<String, Questionnaire> questionnairesById = fhirClient.lookupQuestionnaires(questionnaireIds)
                .stream()
                .collect(Collectors.toMap(q -> q.getIdElement().toUnqualifiedVersionless().getValue(), q -> q));
        if(!new HashSet<>(questionnaireIds).equals(questionnairesById.keySet())) {
            throw new IllegalStateException("Could not look up every questionnaire when retrieving questionnaireResponses!");
        }

        // Look up patient
        Optional<Patient> patient = fhirClient.lookupPatientByCpr(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up patient for cpr %s!", cpr));
        }

        return questionnaireResponses
                .stream()
                .map(qr -> fhirMapper.mapQuestionnaireResponse(qr, questionnairesById.get(qr.getQuestionnaire()), patient.get()))
                .collect(Collectors.toList());
    }

    public void updateExaminationStatus(String questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException {
        // Look up the QuestionnaireResponse
        Optional<QuestionnaireResponse> questionnaireResponse = fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId);
        if(!questionnaireResponse.isPresent()) {
            throw new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId));
        }

        // Update it
        QuestionnaireResponseModel model = fhirMapper.mapQuestionnaireResponseForUpdate(questionnaireResponse.get());
        model.setExaminationStatus(examinationStatus);

        // Save the updated QuestionnaireResponse
        fhirClient.updateQuestionnaireResponse(fhirMapper.mapQuestionnaireResponseModelForUpdate(model));
    }
}
