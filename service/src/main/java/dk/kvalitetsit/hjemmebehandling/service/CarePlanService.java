package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CarePlanService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private DateProvider dateProvider;

    public CarePlanService(FhirClient fhirClient, FhirMapper fhirMapper, DateProvider dateProvider, AccessValidator accessValidator) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
    }

    public String createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException {
        // Try to look up the patient in the careplan
        String cpr = carePlan.getPatient().getCpr();
        var patient = fhirClient.lookupPatientByCpr_new(cpr);

        // TODO: More validations should be performed - possibly?
        // If the patient did exist, check that no existing careplan exists for the patient
        if(patient.isPresent()) {
            String patientId = patient.get().getIdElement().toUnqualifiedVersionless().getValue();
            var carePlanResult = fhirClient.lookupCarePlansByPatientId_new(patientId);
            for(CarePlan cp : carePlanResult.getCarePlans()) {
                var carePlanModel = fhirMapper.mapCarePlan(cp, carePlanResult);
                if(isActive(carePlanModel)) {
                    throw new IllegalStateException(String.format("Could not create careplan for cpr %s: Another active careplan already exists!", cpr));
                }
            }

            // If we already knew the patient, replace the patient reference with the resource we just retrieved (to be able to map the careplan properly.)
            carePlan.setPatient(fhirMapper.mapPatient(patient.get()));
        }

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(carePlan);

        // Mark how far into the future the careplan is 'satisfied' (a careplan is satisfied at a given point in time if it has not had its frequencies violated)
        initializeFrequencyTimestamps(carePlan);

        try {
            // If the patient did not exist, create it along with the careplan. Otherwise just create the careplan.
            if(!patient.isPresent()) {
                return fhirClient.saveCarePlan(fhirMapper.mapCarePlanModel(carePlan), fhirMapper.mapPatientModel(carePlan.getPatient()));
            }
            else {
                return fhirClient.saveCarePlan(fhirMapper.mapCarePlanModel(carePlan));
            }
        }
        catch(Exception e) {
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR);
        }
    }

    public List<CarePlanModel> getCarePlansByCpr(String cpr) throws ServiceException {
        // Look up the patient so that we may look up careplans by patientId.
        Optional<Patient> patient = fhirClient.lookupPatientByCpr_new(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up patient by cpr %s!", cpr));
        }

        // Look up the careplans along with related resources needed for mapping.
        String patientId = patient.get().getIdElement().toUnqualifiedVersionless().toString();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlansByPatientId_new(patientId);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resourecs
        List<CarePlanModel> mappedCarePlans = lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());

        return decorateCarePlans(mappedCarePlans);
    }

    public List<CarePlanModel> getCarePlansWithUnsatisfiedSchedules() throws ServiceException {
        Instant pointInTime = dateProvider.now();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlansUnsatisfiedAt_new(pointInTime);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resourecs
        List<CarePlanModel> mappedCarePlans = lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());

        return decorateCarePlans(mappedCarePlans);
    }

    public Optional<CarePlanModel> getCarePlanById(String carePlanId) throws ServiceException, AccessValidationException {
        FhirLookupResult lookupResult = fhirClient.lookupCarePlanById_new(carePlanId);

        Optional<CarePlan> carePlan = lookupResult.getCarePlan(FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan));
        if(!carePlan.isPresent()) {
            return Optional.empty();
        }

        // Validate that the user is allowed to access the careplan.
        validateAccess(carePlan.get());

        // Map the resource
        CarePlanModel mappedCarePlan = fhirMapper.mapCarePlan(carePlan.get(), lookupResult);
        return Optional.of(decorateCarePlan(mappedCarePlan));
    }

    public void resolveAlarm(String carePlanId) throws ServiceException, AccessValidationException {
        // Get the careplan

        // Validate access

        // Check that the 'satisfiedUntil'-timestamp is indeed in the past, throw an exception if not.

        // Recompute the 'satisfiedUntil'-timestamps

        // Save the updated carePlan

        throw new UnsupportedOperationException();
    }

    public void updateQuestionnaires(String carePlanId, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies) throws ServiceException, AccessValidationException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        //List<Questionnaire> questionnaires = fhirClient.lookupQuestionnaires(questionnaireIds);
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnaires_new(questionnaireIds);
        if(questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!", ErrorKind.BAD_REQUEST);
        }

        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the CarePlan, throw an exception in case it does not exist.
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult careplanResult = fhirClient.lookupCarePlanById_new(carePlanId);
        if(careplanResult.getCarePlans().size() != 1 || !careplanResult.getCarePlan(qualifiedId).isPresent()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST);
        }
        CarePlan carePlan = careplanResult.getCarePlan(qualifiedId).get();

        // Validate that the client is allowed to update the carePlan.
        validateAccess(carePlan);

        // Update the carePlan
        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, careplanResult.merge(questionnaireResult));
        Set<String> existingQuestionnaireIds = carePlanModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId()).collect(Collectors.toSet());
        for(var questionnaireId : questionnaireIds) {
            if(existingQuestionnaireIds.contains(questionnaireId)) {
                continue;
            }

            var wrapper = buildQuestionnaireWrapperModel(questionnaireId, frequencies, questionnaireResult);
            carePlanModel.getQuestionnaires().add(wrapper);
        }

        // Save the updated CarePlan
        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel));
    }

    private List<CarePlanModel> decorateCarePlans(List<CarePlanModel> carePlans) {
        // Populate 'exceededQuestionnaires' list
        for(var carePlanModel : carePlans) {
            decorateCarePlan(carePlanModel);
        }

        return carePlans;
    }

    private CarePlanModel decorateCarePlan(CarePlanModel carePlanModel) {
        if(carePlanModel.getQuestionnaires() != null) {
            carePlanModel.setQuestionnairesWithUnsatisfiedSchedule(carePlanModel.getQuestionnaires()
                    .stream()
                    .filter(qw -> qw.getSatisfiedUntil().isBefore(dateProvider.now()))
                    .map(qw -> qw.getQuestionnaire().getId())
                    .collect(Collectors.toList()));
        }

        return carePlanModel;
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId, Map<String, FrequencyModel> frequencies, FhirLookupResult lookupResult) {
        var wrapper = new QuestionnaireWrapperModel();

        // Set the questionnaire
        var questionnaire = lookupResult.getQuestionnaire(questionnaireId).orElseThrow();
        wrapper.setQuestionnaire(fhirMapper.mapQuestionnaire(questionnaire));

        // Set the frequency
        wrapper.setFrequency(frequencies.get(questionnaireId));

        // Initialize the 'satisfied-until' timestamp-
        initializeFrequencyTimestamp(wrapper);

        return wrapper;
    }

    private boolean isActive(CarePlanModel carePlan) {
        return carePlan.getEndDate() == null;
    }

    private void validateReferences(CarePlanModel carePlanModel) throws AccessValidationException {
        // Validate questionnaires
        if(carePlanModel.getQuestionnaires() != null && !carePlanModel.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires_new(carePlanModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId()).collect(Collectors.toList()));
            validateAccess(lookupResult.getQuestionnaires());
        }

        // Validate planDefinitions
        if(carePlanModel.getPlanDefinitions() != null && !carePlanModel.getPlanDefinitions().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitions_new(carePlanModel.getPlanDefinitions().stream().map(pd -> pd.getId()).collect(Collectors.toList()));
            validateAccess(lookupResult.getPlanDefinitions());
        }
    }

    private void initializeFrequencyTimestamps(CarePlanModel carePlanModel) {
        // Mark how far into the future the careplan is 'satisfied' (a careplan is satisfied at a given point in time if it has not had its frequencies violated)
        for(var questionnaireWrapper : carePlanModel.getQuestionnaires()) {
            initializeFrequencyTimestamp(questionnaireWrapper);
        }
        var carePlanSatisfiedUntil = carePlanModel.getQuestionnaires().stream().map(qw -> qw.getSatisfiedUntil()).min(Comparator.naturalOrder()).orElse(Instant.MAX);
        carePlanModel.setSatisfiedUntil(carePlanSatisfiedUntil);
    }

    private void initializeFrequencyTimestamp(QuestionnaireWrapperModel questionnaireWrapperModel) {
        var nextDeadline = new FrequencyEnumerator(dateProvider.now(), questionnaireWrapperModel.getFrequency()).next().next().getPointInTime();
        questionnaireWrapperModel.setSatisfiedUntil(nextDeadline);
    }
}
