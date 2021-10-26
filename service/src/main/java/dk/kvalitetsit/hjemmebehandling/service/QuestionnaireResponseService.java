package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

        return questionnaireResponses
                .stream()
                .map(qr -> fhirMapper.mapQuestionnaireResponse(qr))
                .collect(Collectors.toList());
    }
}
