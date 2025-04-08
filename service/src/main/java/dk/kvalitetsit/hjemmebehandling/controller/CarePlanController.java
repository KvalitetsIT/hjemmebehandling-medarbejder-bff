package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.TimeType;
import org.openapitools.api.CarePlanApi;
import org.openapitools.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class CarePlanController extends BaseController implements CarePlanApi {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private final CarePlanService carePlanService;
    private final AuditLoggingService auditLoggingService;
    private final DtoMapper dtoMapper;
    private final LocationHeaderBuilder locationHeaderBuilder;
    private final PlanDefinitionService planDefinitionService;

    public CarePlanController(PlanDefinitionService planDefinitionService, CarePlanService carePlanService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.carePlanService = carePlanService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
        this.planDefinitionService = planDefinitionService;
    }

    @Override
    public ResponseEntity<Void> completeCarePlan(String id) {
        try {
            CarePlanModel carePlan = carePlanService.completeCarePlan(id);
            auditLoggingService.log("PUT /v1/careplan/" + id + "/complete", carePlan.patient());
        } catch (ServiceException e) {
            logger.error("completeCarePlan({}) error:", id, e);
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> createCarePlan(CreateCarePlanRequest request) {
        String carePlanId;
        try {
            // force time for deadline to organization configured default.
            if (request.getCarePlan().getQuestionnaires() != null) {
                TimeType defaultDeadlineTime = carePlanService.getDefaultDeadlineTime();
                // todo: 'Optional.get()' without 'isPresent()' check
                request.getCarePlan().getQuestionnaires().forEach(q -> q.getFrequency().get().setTimeOfDay(Optional.of(defaultDeadlineTime.getValue())));
            }

            CarePlanModel carePlan = dtoMapper.mapCarePlanDto(request.getCarePlan());

            carePlanId = carePlanService.createCarePlan(carePlan);

            auditLoggingService.log("POST /v1/careplan", carePlan.patient());
        } catch (AccessValidationException | ServiceException e) {
            logger.error("Error creating CarePlan", e);
            throw toStatusCodeException(e);
        }

        URI location = locationHeaderBuilder.buildLocationHeader(carePlanId);
        return ResponseEntity.created(location).build();
    }

    @Override
    public ResponseEntity<CarePlanDto> getCarePlanById(String id) {
        try {
            Optional<CarePlanModel> carePlan = carePlanService.getCarePlanById(id);

            if (carePlan.isEmpty()) {
                throw new ResourceNotFoundException(String.format("CarePlan with id %s not found.", id), ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
            }
            auditLoggingService.log("GET /v1/careplan/" + id, carePlan.get().patient());
            return ResponseEntity.ok(dtoMapper.mapCarePlanModel(carePlan.get()));
        } catch (AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions1(Optional<List<String>> statusesToInclude) {
        try {
            if (statusesToInclude.isPresent() && statusesToInclude.get().isEmpty()) {
                var details = ErrorDetails.PARAMETERS_INCOMPLETE;
                details.setDetails("Statusliste blev sendt med, men indeholder ingen elementer");
                throw new BadRequestException(details);
            }
            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions(statusesToInclude.orElse(List.of()));

            // todo: 'Optional.get()' without 'isPresent()' check
            return ResponseEntity.ok(planDefinitions.stream().map(dtoMapper::mapPlanDefinitionModel).sorted(Comparator.comparing(x -> x.getLastUpdated().get().toInstant(), Comparator.nullsFirst(Instant::compareTo).reversed())).toList());
        } catch (ServiceException e) {
            logger.error("Could not look up plandefinitions", e);
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<List<String>> getUnresolvedQuestionnaires(String id) {
        try {
            List<QuestionnaireModel> questionnaires = carePlanService.getUnresolvedQuestionnaires(id);
            List<String> ids = questionnaires.stream().map(questionnaire -> questionnaire.id().id()).toList();
            return ResponseEntity.ok(ids);
        } catch (ServiceException e) {
            logger.error("Unresolved questionnaires could not be fetched due to:  %s", e);
            throw toStatusCodeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<Void> patchCarePlan(String id, UpdateCareplanRequest request) {
        if (request.getPlanDefinitionIds() == null || request.getQuestionnaires() == null) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }
        try {
            List<String> questionnaireIds = getQuestionnaireIds(request.getQuestionnaires());
            Map<String, FrequencyModel> frequencies = getQuestionnaireFrequencies(request.getQuestionnaires());
            PatientDetails patientDetails = getPatientDetails(request);

            CarePlanModel carePlanModel = carePlanService.updateCarePlan(id, request.getPlanDefinitionIds(), questionnaireIds, frequencies, patientDetails);

            auditLoggingService.log("PATCH /v1/careplan/" + id, carePlanModel.patient());
        } catch (AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> resolveAlarm(String id, String questionnaireId) {
        try {
            CarePlanModel carePlan = carePlanService.resolveAlarm(id, questionnaireId);
            auditLoggingService.log("PUT /v1/careplan/" + id + "/resolve-alarm", carePlan.patient());
        } catch (AccessValidationException | ServiceException e) {
            logger.error(String.format("resolveAlarm(%s, %s) error:", id, questionnaireId), e);
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<CarePlanDto>> searchCarePlans(Optional<String> cpr, Optional<Boolean> onlyUnsatisfiedSchedules, Optional<Boolean> onlyActiveCareplans, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        try {
            List<CarePlanModel> carePlans;

            if (pageNumber.isPresent() && pageSize.isPresent()) {
                Pagination pagination = new Pagination(pageNumber.get(), pageSize.get());

                if (cpr.isPresent()) {
                    carePlans = carePlanService.getCarePlansWithFilters(cpr.get(), onlyActiveCareplans.orElse(false), onlyUnsatisfiedSchedules.orElse(false), pagination);
                } else {
                    carePlans = carePlanService.getCarePlansWithFilters(onlyActiveCareplans.orElse(false), onlyUnsatisfiedSchedules.orElse(false), pagination);
                }

            } else {
                if (cpr.isPresent()) {
                    carePlans = carePlanService.getCarePlansWithFilters(cpr.get(), onlyActiveCareplans.orElse(false), onlyUnsatisfiedSchedules.orElse(false));
                } else {
                    carePlans = carePlanService.getCarePlansWithFilters(onlyActiveCareplans.orElse(false), onlyUnsatisfiedSchedules.orElse(false));
                }
            }
            auditLoggingService.log("GET /v1/careplan", carePlans.stream().map(CarePlanModel::patient).toList());
            return ResponseEntity.ok(carePlans.stream().map(dtoMapper::mapCarePlanModel).toList());
        } catch (ServiceException e) {
            logger.error("Could not look up careplans by cpr", e);
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<Void> updateCarePlan(CarePlanDto carePlanDto) {
        throw new UnsupportedOperationException();
    }

    private List<String> getQuestionnaireIds(List<QuestionnaireFrequencyPairDto> questionnaireFrequencyPairs) {
        // todo: 'Optional.get()' without 'isPresent()' check
        return questionnaireFrequencyPairs.stream().map(pair -> FhirUtils.qualifyId(pair.getId().get(), ResourceType.Questionnaire)).toList();
    }

    private Map<String, FrequencyModel> getQuestionnaireFrequencies(List<QuestionnaireFrequencyPairDto> questionnaireFrequencyPairs) throws ServiceException {
        // force time for deadline to organization configured default.
        TimeType defaultDeadlineTime = carePlanService.getDefaultDeadlineTime();
        // todo: 'Optional.get()' without 'isPresent()' check
        questionnaireFrequencyPairs.forEach(pair -> pair.getFrequency().get().setTimeOfDay(Optional.of(defaultDeadlineTime.getValue())));

        // todo: 'Optional.get()' without 'isPresent()' check
        return questionnaireFrequencyPairs.stream().collect(Collectors.toMap(pair -> FhirUtils.qualifyId(pair.getId().get(), ResourceType.Questionnaire), pair -> dtoMapper.mapFrequencyDto(pair.getFrequency().get())));
    }

    private PatientDetails getPatientDetails(UpdateCareplanRequest request) {
        PatientDetails patientDetails = new PatientDetails(
                request.getPatientPrimaryPhone().orElse(null),
                request.getPatientSecondaryPhone().orElse(null),
                request.getPrimaryRelativeName().orElse(null),
                request.getPrimaryRelativeAffiliation().orElse(null),
                request.getPrimaryRelativePrimaryPhone().orElse(null),
                request.getPrimaryRelativeSecondaryPhone().orElse(null)
        );
        return patientDetails;
    }

}
