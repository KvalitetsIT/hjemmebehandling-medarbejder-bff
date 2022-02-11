package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.api.ThresholdDto;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlanDefinitionService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionService.class);

    private FhirClient fhirClient;
    private FhirMapper fhirMapper;
    private DateProvider dateProvider;

    public PlanDefinitionService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator, DateProvider dateProvider) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
    }

    public List<PlanDefinitionModel> getPlanDefinitions() throws ServiceException {
        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitions();

        return lookupResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinition(pd, lookupResult)).collect(Collectors.toList());
    }

    public String createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        initializeAttributesForNewPlanDefinition(planDefinition);

        try {
            return fhirClient.savePlanDefinition(fhirMapper.mapPlanDefinitionModel(planDefinition));
        }
        catch(Exception e) {
            throw new ServiceException("Error saving PlanDefinition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException {
        // Validate questionnaires
        if(planDefinition.getQuestionnaires() != null && !planDefinition.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires(planDefinition.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId().toString()).collect(Collectors.toList()));
            validateAccess(lookupResult.getQuestionnaires());
        }
    }

    private void initializeAttributesForNewPlanDefinition(PlanDefinitionModel planDefinition) {
        // Ensure that no id is present on the plandefinition - the FHIR server will generate that for us.
        planDefinition.setId(null);

        initializeTimestamps(planDefinition);
    }

    private void initializeTimestamps(PlanDefinitionModel planDefinition) {
        var today = dateProvider.today().toInstant();
        planDefinition.setCreated(today);
    }

    public PlanDefinitionModel updatePlanDefinition(String id, List<String> questionnaireIds, List<ThresholdDto> thresholds) throws ServiceException, AccessValidationException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnaires(questionnaireIds);
        if(questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!", ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN);
        }

        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the plan definition to verify that it exist, throw an exception in case it don't.
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinition(id);
        if(planDefinitionResult.getPlanDefinitions().isEmpty()) {
            throw new ServiceException("Could not look up plan definitions to update!", ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }
        PlanDefinition planDefinition = planDefinitionResult.getPlanDefinition(id).get();

        // Validate that the client is allowed to update the planDefinition.
        validateAccess(planDefinition);

        // Update carePlan
        PlanDefinitionModel planDefinitionModel = fhirMapper.mapPlanDefinition(planDefinition, planDefinitionResult);
        updatePlanDefinitionModel(planDefinitionModel, thresholds, questionnaireResult.getQuestionnaires() );
    }

    private void updatePlanDefinitionModel(PlanDefinitionModel planDefinitionModel, List<ThresholdDto> thresholds, List<Questionnaire> questionnaires) {
        //planDefinitionModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire()).anyMatch(q -> q.getId().equals())
        List<String> questionnaireIds = questionnaires.stream().map(q -> q.getId()).collect(Collectors.toList());

        List<QuestionnaireWrapperModel> keepList = new ArrayList<>();
        for (QuestionnaireWrapperModel wrapper : planDefinitionModel.getQuestionnaires()) {

            if (questionnaireIds.contains(wrapper.getQuestionnaire().getId().toString())) {
                keepList.add(wrapper);
            }
        }

        List<String> createList = keepList.stream()
            .map(qw -> qw.getQuestionnaire().getId().toString())
            .filter(qw -> !questionnaireIds.contains(qw))
            .collect(Collectors.toList());

        for (String questionnaireId : createList) {


        }


    }
}
