package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class PlanDefinitionService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionService.class);

    private final FhirClient<
            CarePlanModel,
            PlanDefinitionModel,
            PractitionerModel,
            PatientModel,
            QuestionnaireModel,
            QuestionnaireResponseModel,
            Organization,
            CarePlanStatus> fhirClient;

    private final FhirMapper fhirMapper;
    private final DateProvider dateProvider;

    public PlanDefinitionService(
            FhirClient<
                    CarePlanModel,
                    PlanDefinitionModel,
                    PractitionerModel,
                    PatientModel,
                    QuestionnaireModel,
                    QuestionnaireResponseModel,
                    Organization,
                    CarePlanStatus> fhirClient,
            FhirMapper fhirMapper,
            AccessValidator accessValidator,
            DateProvider dateProvider
    ) {
        super(accessValidator);
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
    }

    @NotNull
    private static List<QuestionnaireWrapperModel> getQuestionnaireWrapperModels(List<ThresholdModel> thresholds, CarePlanModel model, List<QuestionnaireModel> newQuestionnaireModels) {
        var newQuestionnaires = newQuestionnaireModels.stream().map(qModel -> new QuestionnaireWrapperModel(
                qModel,
                new FrequencyModel(List.of(), LocalTime.parse("11:00")),
                Instant.MAX,
                thresholds // or pass new threshold list if applicable
        )).toList();

        return Stream.of(model.questionnaires(), newQuestionnaires)
                .flatMap(Collection::stream)
                .toList();
    }

    public List<PlanDefinitionModel> getPlanDefinitions(Collection<String> statusesToInclude) throws ServiceException {
        return fhirClient.lookupPlanDefinitionsByStatus(statusesToInclude);
    }

    public String createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        PlanDefinitionModel.Builder
                .from(planDefinition)
                .created(dateProvider.today().toInstant())
                .build();

        try {
            return fhirClient.savePlanDefinition(planDefinition);
        } catch (Exception e) {
            throw new ServiceException("Error saving PlanDefinition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException, ServiceException {
        // Validate questionnaires
        if (planDefinition.questionnaires() != null && !planDefinition.questionnaires().isEmpty()) {

            var questionnaireWrappers = planDefinition.questionnaires()
                    .stream().map(qw -> qw.questionnaire().id().toString()).toList();

            fhirClient.lookupQuestionnairesById(questionnaireWrappers);
        }
    }

    public void updatePlanDefinition(String id, String name, PlanDefinitionStatus status, List<String> questionnaireIds, List<ThresholdModel> thresholds) throws ServiceException, AccessValidationException {

        List<QuestionnaireModel> questionnaires = fhirClient.lookupQuestionnairesById(questionnaireIds);

        if (questionnaires.size() != questionnaireIds.size()) {
            throw new ServiceException(
                    "Could not look up questionnaires to update!",
                    ErrorKind.BAD_REQUEST,
                    ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
            );
        }



        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.PlanDefinition);


        PlanDefinitionModel planDefinition = fhirClient.lookupPlanDefinition(qualifiedId).orElseThrow(() -> new ServiceException(
                "Could not look up plan definitions to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST
        ));


        List<String> currentQuestionnaires = planDefinition.questionnaires().stream()
                .map(q -> q.questionnaire().id().toString())
                .toList();

        List<String> removedQuestionnaireIds = currentQuestionnaires.stream()
                .filter(qid -> !questionnaireIds.contains(qid))
                .toList();

        if (!removedQuestionnaireIds.isEmpty()) {
            List<CarePlanModel> activeCarePlans = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            for (CarePlanModel carePlan : activeCarePlans) {
                String carePlanId = carePlan.id().toString();

                if (questionnaireHasExceededDeadline(fhirMapper.mapCarePlanModel(carePlan), removedQuestionnaireIds)) {
                    throw new ServiceException(
                            String.format("Careplan with id %s has missing scheduled questionnaire-responses!", carePlanId),
                            ErrorKind.BAD_REQUEST,
                            ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES
                    );
                }

                if (questionnaireHasUnexaminedResponses(carePlanId, removedQuestionnaireIds)) {
                    throw new ServiceException(
                            String.format("Careplan with id %s still has unhandled questionnaire-responses!", carePlanId),
                            ErrorKind.BAD_REQUEST,
                            ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES
                    );
                }
            }
        }

        List<String> newQuestionnaires = questionnaireIds.stream()
                .filter(qid -> !currentQuestionnaires.contains(qid))
                .toList();

        if (!newQuestionnaires.isEmpty()) {
            List<CarePlanModel> result = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            String orgId = fhirClient.getOrganizationId();

            boolean inUse = result.stream()
                    .flatMap(model -> model.questionnaires().stream())
                    .map(qw -> qw.questionnaire().id().toString())
                    .anyMatch(newQuestionnaires::contains);

            if (inUse) {
                throw new ServiceException(
                        String.format("A questionnaire with id %s is used by active careplans!", newQuestionnaires),
                        ErrorKind.BAD_REQUEST,
                        ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN
                );
            }
        }

        List<QuestionnaireWrapperModel> updatedWrappers = questionnaires.stream()
                .map(q -> new QuestionnaireWrapperModel(q, null, null, thresholds)) // use appropriate defaults/nulls if needed
                .toList();

        planDefinition = new PlanDefinitionModel(
                planDefinition.id(),
                planDefinition.organizationId(),
                name,
                planDefinition.title(),
                status,
                planDefinition.created(),
                Instant.now(),
                updatedWrappers
        );

        fhirClient.updatePlanDefinition(planDefinition);

        // if questionnaire(s) has been removed, remove them from appropriate careplans
        if (!removedQuestionnaireIds.isEmpty()) {
            // get careplans we are removing the questionnaire(s) to
            List<CarePlanModel> carePlans = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            var orgId = fhirClient.getOrganizationId();
            carePlans.forEach(carePlan -> {
                var result = carePlan.questionnaires().stream().filter(qw -> removedQuestionnaireIds.contains(qw.questionnaire().id().toString())).toList();
                fhirClient.updateCarePlan(CarePlanModel.Builder.from(carePlan).questionnaires(result).build());
            });
        }

        // if new questionnaire(s) has been added, add them to appropriate careplans with an empty schedule
        if (!newQuestionnaires.isEmpty()) {
            List<QuestionnaireModel> newQuestionnaireModels = questionnaires.stream()
                    .filter(q -> newQuestionnaires.contains(q.id().toString()))
                    .toList();

            List<CarePlanModel> carePlans = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            String orgId = fhirClient.getOrganizationId();

            carePlans.forEach(model -> {

                List<QuestionnaireWrapperModel> updatedQuestionnaires = getQuestionnaireWrapperModels(thresholds, model, newQuestionnaireModels);

                CarePlanModel updatedModel = new CarePlanModel(
                        model.id(),
                        model.organizationId(),
                        model.title(),
                        model.status(),
                        model.created(),
                        model.startDate(),
                        model.endDate(),
                        model.patient(),
                        updatedQuestionnaires,
                        model.planDefinitions(),
                        model.departmentName(),
                        model.satisfiedUntil()
                );

                fhirClient.updateCarePlan(updatedModel);
            });
        }
    }


    public void retirePlanDefinition(String id) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.PlanDefinition);
        Optional<PlanDefinitionModel> result = fhirClient.lookupPlanDefinition(qualifiedId);


        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup plandefinition with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        var activeCarePlansWithPlanDefinition = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
        if (!activeCarePlansWithPlanDefinition.isEmpty()) {
            throw new ServiceException(String.format("Plandefinition with id %s if used by active careplans!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        PlanDefinitionModel retiredPlanDefinition = PlanDefinitionModel.Builder.from(result.get()).status(PlanDefinitionStatus.RETIRED).build();;
        fhirClient.updatePlanDefinition(retiredPlanDefinition);
    }

    public List<CarePlanModel> getCarePlansThatIncludes(String planDefinitionId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(planDefinitionId, ResourceType.PlanDefinition);

        Optional<PlanDefinitionModel> result = fhirClient.lookupPlanDefinition(qualifiedId);

        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not find plandefinition with tht requested id: %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        List<CarePlanModel> activeCarePlans = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);


        var orgId = fhirClient.getOrganizationId();
        return activeCarePlans;
    }



    private boolean questionnaireHasExceededDeadline(CarePlan carePlan, List<String> questionnaireIds) {
        return carePlan.getActivity()
                .stream()
                .filter(carePlanActivityComponent -> questionnaireIds.contains(carePlanActivityComponent.getDetail().getInstantiatesCanonical().getFirst().getValue()))
                .anyMatch(carePlanActivityComponent -> ExtensionMapper.extractActivitySatisfiedUntil(carePlanActivityComponent.getDetail().getExtension()).isBefore(dateProvider.now()));
    }

    private boolean questionnaireHasUnexaminedResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), carePlanId)
                .stream()
                .anyMatch(questionnaireIds::contains);
    }
}
