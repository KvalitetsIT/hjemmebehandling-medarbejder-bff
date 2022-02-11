package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.controller.BaseController;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionnaireService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    public QuestionnaireService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator) {
        super(accessValidator);
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }
    public Optional<QuestionnaireModel> getQuestionnaireById(String questionnaireId) throws ServiceException, AccessValidationException {
        String qualifiedId = FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire);
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires(List.of(qualifiedId));

        Optional<Questionnaire> questionnaire = lookupResult.getQuestionnaire(qualifiedId);
        if(!questionnaire.isPresent()) {
            return Optional.empty();
        }

        // Validate that the user is allowed to access the careplan.
        validateAccess(questionnaire.get());

        // Map the resource
        QuestionnaireModel mappedCarePlan = fhirMapper.mapQuestionnaire(questionnaire.get());
        return Optional.of(mappedCarePlan);
    }

    public List<QuestionnaireModel> getQuestionnaires() {
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires();

        return lookupResult.getQuestionnaires().stream().map(q -> fhirMapper.mapQuestionnaire(q)).collect(Collectors.toList());
    }

    public String createQuestionnaire(QuestionnaireModel questionnaire) {
        // Initialize basic attributes for a new CarePlan: Id, status and so on.
        initializeAttributesForNewQuestionnaire(questionnaire);

        return fhirClient.saveQuestionnaire(fhirMapper.mapQuestionnaire(questionnaire));
    }

    private void initializeAttributesForNewQuestionnaire(QuestionnaireModel questionnaire) {
        // Ensure that no id is present on the careplan - the FHIR server will generate that for us.
        questionnaire.setId(null);

        questionnaire.setStatus(QuestionnaireStatus.DRAFT);
    }
}
