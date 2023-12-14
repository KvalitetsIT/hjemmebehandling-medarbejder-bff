package dk.kvalitetsit.hjemmebehandling.controller;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.api.dto.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.PlanDefinitionDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireFrequencyPairDto;
import dk.kvalitetsit.hjemmebehandling.api.request.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.api.request.UpdateCareplanRequest;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "CarePlan", description = "API for manipulating and retrieving CarePlans.")
public class CarePlanController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private final CarePlanService carePlanService;
    private final AuditLoggingService auditLoggingService;
    private final DtoMapper dtoMapper;
    private final LocationHeaderBuilder locationHeaderBuilder;

    private final PlanDefinitionService planDefinitionService;

    private enum SearchType {
        CPR, UNSATISFIED_CAREPLANS,ACTIVE
    }

    public CarePlanController(PlanDefinitionService planDefinitionService, CarePlanService carePlanService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.carePlanService = carePlanService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
        this.planDefinitionService = planDefinitionService;
    }

    @GetMapping(value = "/v1/careplan")
    public ResponseEntity<List<CarePlanDto>> searchCarePlans(
                @RequestParam("cpr")
                Optional<String> cpr,
                @RequestParam("only_unsatisfied_schedules")
                Optional<Boolean> onlyUnsatisfiedSchedules,
                @RequestParam("only_active_careplans")
                Optional<Boolean> onlyActiveCarePlans,
                @RequestParam("page_number")
                Optional<Integer> pageNumber,
                @RequestParam("page_size")
                Optional<Integer> pageSize
    ) {

        // TODO: Validate the request params
        try {
            Pagination pagination = null;
            if (pageNumber.isPresent() && pageSize.isPresent()) {
                pagination = new Pagination(pageNumber.get(), pageSize.get());
            }

            List<CarePlanModel> carePlans = carePlanService.getCarePlansWithFilters(cpr,onlyActiveCarePlans.orElse(false),onlyUnsatisfiedSchedules.orElse(false), pagination);

            auditLoggingService.log("GET /v1/careplan", carePlans.stream().map(CarePlanModel::getPatient).collect(Collectors.toList()));
            return ResponseEntity.ok(carePlans.stream().map(CarePlanModel::toDto).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up careplans by cpr", e);
            throw toStatusCodeException(e);
        }
    }

    @Operation(summary = "Get CarePlan by id.", description = "Retrieves a CarePlan by its id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = @Content(schema = @Schema(implementation = CarePlanDto.class))),
            @ApiResponse(responseCode = "404", description = "CarePlan not found.", content = @Content)
    })
    @GetMapping(value = "/v1/careplan/{id}", produces = { "application/json" })
    public ResponseEntity<CarePlanDto> getCarePlanById(
            @PathVariable
            @Parameter(description = "Id of the CarePlan to be retrieved.")
            String id
    ) {
        // Look up the CarePlan
        Optional<CarePlanModel> carePlan = Optional.empty();

        try {
            carePlan = carePlanService.getCarePlanById(id);
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }

        if(carePlan.isEmpty()) {
            throw new ResourceNotFoundException(String.format("CarePlan with id %s not found.", id), ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }
        auditLoggingService.log("GET /v1/careplan/"+id, carePlan.get().getPatient());
        return ResponseEntity.ok(carePlan.get().toDto());
    }

    @Operation(summary = "Create a new CarePlan for a patient.", description = "Create a CarePlan for a patient, based on a PlanDefinition.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successful operation.", headers = { @Header(name = "Location", description = "URL pointing to the created CarePlan.")}, content = @Content),
            @ApiResponse(responseCode = "500", description = "Error during creation of CarePlan.", content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping(value = "/v1/careplan", consumes = { "application/json" })
    public ResponseEntity<Void> createCarePlan(@RequestBody CreateCarePlanRequest request) {
        String carePlanId = null;
        try {
            CarePlanModel carePlan = request.getCarePlan().toModel();
            carePlanId = carePlanService.createCarePlan(carePlan);
            auditLoggingService.log("POST /v1/careplan", carePlan.getPatient());
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Error creating CarePlan", e);
            throw toStatusCodeException(e);
        }

        URI location = locationHeaderBuilder.buildLocationHeader(carePlanId);
        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/v1/careplan")
    public void updateCarePlan(CarePlanDto carePlanDto) {
        throw new UnsupportedOperationException();
    }

    @PatchMapping(value = "/v1/careplan/{id}")
    public ResponseEntity<Void> patchCarePlan(@PathVariable String id, @RequestBody UpdateCareplanRequest request) {
        if(request.getPlanDefinitionIds() == null || request.getQuestionnaires() == null ) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }
        try {
            List<String> questionnaireIds = getQuestionnaireIds(request.getQuestionnaires());
            Map<String, FrequencyModel> frequencies = getQuestionnaireFrequencies(request.getQuestionnaires());
            PatientDetails patientDetails = getPatientDetails(request);

            CarePlanModel carePlanModel = carePlanService.updateCarePlan(
                    id,
                    request.getPlanDefinitionIds(),
                    questionnaireIds,
                    frequencies,
                    patientDetails
            );

            auditLoggingService.log("PATCH /v1/careplan/"+id, carePlanModel.getPatient());
        }
        catch(AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }


    /**
     * Returns unresolved questionnaires
     * The questionnaires which still have unanswered questions
     */
    @GetMapping(value = "/v1/careplan/{id}/questionnaires/unresolved")
    public ResponseEntity<List<String>> getUnresolvedQuestionnaires(@PathVariable String id){
        try {
            List<QuestionnaireModel> questionnaires = carePlanService.getUnresolvedQuestionnaires(id);
            List<String> ids = questionnaires.stream().map(questionnaire -> questionnaire.getId().getId()).collect(Collectors.toList());
            return ResponseEntity.ok(ids);
        }catch (ServiceException e){
            logger.error(String.format("Unresolved questionnaires could not be fetched due to:  %s", e ));
            throw toStatusCodeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping(value = "/v1/careplan/{id}/resolve-alarm/{questionnaireId}")
    public ResponseEntity<Void> resolveAlarm(@PathVariable String id, @PathVariable String questionnaireId) {
        try {
            CarePlanModel carePlan = carePlanService.resolveAlarm(id, questionnaireId);
            auditLoggingService.log("PUT /v1/careplan/"+id+"/resolve-alarm", carePlan.getPatient());
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error(String.format("resolveAlarm(%s, %s) error:", id, questionnaireId), e);
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/v1/careplan/{id}/complete")
    public ResponseEntity<Void> completeCarePlan(@PathVariable String id) {
        try {
            CarePlanModel carePlan = carePlanService.completeCarePlan(id);
            auditLoggingService.log("PUT /v1/careplan/"+id+"/complete", carePlan.getPatient());
        }
        catch(ServiceException e) {
            logger.error(String.format("completeCarePlan(%s) error:", id), e);
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/v1/careplan/plandefinition")
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions(@RequestParam("statusesToInclude") Optional<Collection<String>> statusesToInclude) {
        try {
            if(statusesToInclude.isPresent() && statusesToInclude.get().isEmpty()){
                var details = ErrorDetails.PARAMETERS_INCOMPLETE;
                details.setDetails("Statusliste blev sendt med, men indeholder ingen elementer");
                throw new BadRequestException(details);
            }

            Collection<String> nonOptionalStatusesToInclude = statusesToInclude.orElseGet(List::of);
            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions(nonOptionalStatusesToInclude);


            return ResponseEntity.ok(planDefinitions.stream()
                    .map(PlanDefinitionModel::toDto)
                    .sorted(Comparator.comparing(PlanDefinitionDto::getLastUpdated, Comparator.nullsFirst(Instant::compareTo).reversed()))
                    .collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up plandefinitions", e);
            throw toStatusCodeException(e);
        }
    }

    private List<String> getQuestionnaireIds(List<QuestionnaireFrequencyPairDto> questionnaireFrequencyPairs) {
        return questionnaireFrequencyPairs
                .stream()
                .map(pair -> FhirUtils.qualifyId(pair.getId(), ResourceType.Questionnaire))
                .collect(Collectors.toList());
    }

    private Map<String, FrequencyModel> getQuestionnaireFrequencies(List<QuestionnaireFrequencyPairDto> questionnaireFrequencyPairs) {
        return questionnaireFrequencyPairs
                .stream()
                .collect(Collectors.toMap(
                        pair -> FhirUtils.qualifyId(pair.getId(), ResourceType.Questionnaire),
                        pair -> dtoMapper.mapFrequencyDto(pair.getFrequency()))
                );
    }

    private PatientDetails getPatientDetails(UpdateCareplanRequest request) {
        PatientDetails patientDetails = new PatientDetails();

        patientDetails.setPatientPrimaryPhone(request.getPatientPrimaryPhone());
        patientDetails.setPatientSecondaryPhone(request.getPatientSecondaryPhone());
        patientDetails.setPrimaryRelativeName(request.getPrimaryRelativeName());
        patientDetails.setPrimaryRelativeAffiliation(request.getPrimaryRelativeAffiliation());
        patientDetails.setPrimaryRelativePrimaryPhone(request.getPrimaryRelativePrimaryPhone());
        patientDetails.setPrimaryRelativeSecondaryPhone(request.getPrimaryRelativeSecondaryPhone());

        return patientDetails;
    }
}
