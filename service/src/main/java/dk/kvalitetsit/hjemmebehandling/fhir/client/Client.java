package dk.kvalitetsit.hjemmebehandling.fhir.client;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Generic client interface for managing FHIR-based entities such as CarePlans, PlanDefinitions,
 * Practitioners, Patients, Questionnaires, and related resources.
 *
 * @param <CarePlan> Type representing a care plan.
 * @param <PlanDefinition> Type representing a plan definition.
 * @param <Practitioner> Type representing a practitioner.
 * @param <Patient> Type representing a patient.
 * @param <Questionnaire> Type representing a questionnaire.
 * @param <QuestionnaireResponse> Type representing a questionnaire response.
 * @param <Organization> Type representing an organization.
 * @param <CarePlanStatus> Type representing a care plan status enum.
 */
public interface Client<CarePlan, PlanDefinition, Practitioner, Patient, Questionnaire, QuestionnaireResponse, Organization, CarePlanStatus>  {



    /**
     * Fetches active plan definitions that reference the specified questionnaire.
     *
     * @param questionnaireId The ID of the questionnaire.
     * @return List of plan definitions using the questionnaire.
     * @throws ServiceException If the operation fails.
     */
    List<PlanDefinition> fetchActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException;


    /**
     * Retrieves the current user as a practitioner, creating one if not existing.
     *
     * @return The practitioner instance.
     * @throws ServiceException If the operation fails.
     */
    Practitioner getOrCreateUserAsPractitioner() throws ServiceException;

    /**
     * Looks up multiple practitioners by their IDs.
     *
     * @param practitionerIds Collection of practitioner IDs.
     * @return List of matching practitioners.
     */
    List<Practitioner> lookupPractitioners(Collection<String> practitionerIds);

    /**
     * Performs a value set lookup (e.g. for coding systems or terminology).
     *
     * @return The lookup result.
     * @throws ServiceException If the operation fails.
     */
    FhirLookupResult lookupValueSet() throws ServiceException;

    /**
     * Updates a patient resource.
     *
     * @param patient The patient to update.
     */
    void updatePatient(Patient patient);

    /**
     * Updates a plan definition resource.
     *
     * @param planDefinition The plan definition to update.
     */
    void updatePlanDefinition(PlanDefinition planDefinition);

    /**
     * Updates a questionnaire response resource.
     *
     * @param questionnaireResponse The questionnaire response to update.
     */
    void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse);


    /**
     * Looks up an organization by its SOR code.
     *
     * @param sorCode The SOR code.
     * @return An optional organization.
     * @throws ServiceException If the operation fails.
     */
    Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException;

    /**
     * Looks up a patient by ID.
     *
     * @param patientId The patient ID.
     * @return An optional patient.
     */
    Optional<Patient> lookupPatientById(String patientId);

    /**
     * Looks up a patient by CPR number.
     *
     * @param cpr The CPR number.
     * @return An optional patient.
     */
    Optional<Patient> lookupPatientByCpr(String cpr);

    /**
     * Searches patients by given terms and care plan status.
     *
     * @param searchStrings List of search terms.
     * @param status        Care plan status filter.
     * @return List of matching patients.
     * @throws ServiceException If the operation fails.
     */
    List<Patient> searchPatients(List<String> searchStrings, CarePlanStatus status) throws ServiceException;

    /**
     * Retrieves patients by care plan status.
     *
     * @param status The care plan status.
     * @return List of matching patients.
     * @throws ServiceException If the operation fails.
     */
    List<Patient> getPatientsByStatus(CarePlanStatus status) throws ServiceException;

    /**
     * Looks up a questionnaire response by ID.
     *
     * @param questionnaireResponseId The response ID.
     * @return An optional questionnaire response.
     */
    Optional<QuestionnaireResponse> lookupQuestionnaireResponseById(String questionnaireResponseId);

    /**
     * Looks up questionnaire responses for a care plan and specific questionnaires.
     *
     * @param carePlanId       The care plan ID.
     * @param questionnaireIds The questionnaire IDs.
     * @return List of matching responses.
     */
    List<QuestionnaireResponse> lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds);

    /**
     * Looks up all versions of questionnaires by their IDs.
     *
     * @param ids List of questionnaire IDs.
     * @return List of questionnaires.
     */
    List<Questionnaire> lookupVersionsOfQuestionnaireById(List<String> ids);

    /**
     * Looks up questionnaire responses filtered by status.
     *
     * @param statuses List of statuses to filter by.
     * @return List of responses.
     * @throws ServiceException If the operation fails.
     */
    List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException;

    /**
     * Looks up questionnaire responses by status and care plan ID.
     *
     * @param statuses   List of statuses.
     * @param carePlanId The care plan ID.
     * @return List of responses.
     * @throws ServiceException If the operation fails.
     */
    List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException;

    /**
     * Looks up questionnaire responses by a single status.
     *
     * @param status The status.
     * @return List of responses.
     * @throws ServiceException If the operation fails.
     */
    List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException;


    /**
     * Saves a practitioner.
     *
     * @param practitioner The practitioner to save.
     * @return The saved practitioner ID.
     * @throws ServiceException If the operation fails.
     */
    String savePractitioner(Practitioner practitioner) throws ServiceException;

    /**
     * Saves a questionnaire response.
     *
     * @param questionnaireResponse The response to save.
     * @return The saved response ID.
     * @throws ServiceException If the operation fails.
     */
    String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) throws ServiceException;

    /**
     * Saves a questionnaire.
     *
     * @param questionnaire The questionnaire to save.
     * @return The saved questionnaire ID.
     * @throws ServiceException If the operation fails.
     */
    String saveQuestionnaire(Questionnaire questionnaire) throws ServiceException;

    /**
     * Saves a plan definition.
     *
     * @param planDefinition The plan definition to save.
     * @return The saved plan definition ID.
     * @throws ServiceException If the operation fails.
     */
    String savePlanDefinition(PlanDefinition planDefinition) throws ServiceException;

    /**
     * Saves a patient.
     *
     * @param patient The patient to save.
     * @return The saved patient ID.
     * @throws ServiceException If the operation fails.
     */
    String savePatient(Patient patient) throws ServiceException;

    /**
     * Looks up a single plan definition by ID.
     *
     * @param planDefinitionId The ID of the plan definition.
     * @return An optional plan definition.
     */
    Optional<PlanDefinition> lookupPlanDefinition(String planDefinitionId);

    /**
     * Retrieves all plan definitions.
     *
     * @return List of plan definitions.
     * @throws ServiceException If the operation fails.
     */
    List<PlanDefinition> lookupPlanDefinitions() throws ServiceException;

    /**
     * Looks up multiple plan definitions by their IDs.
     *
     * @param planDefinitionIds Collection of plan definition IDs.
     * @return List of matching plan definitions.
     */
    List<PlanDefinition> lookupPlanDefinitionsById(Collection<String> planDefinitionIds);

    /**
     * Looks up plan definitions filtered by status values.
     *
     * @param statusesToInclude Statuses to include.
     * @return List of matching plan definitions.
     * @throws ServiceException If the operation fails.
     */
    List<PlanDefinition> lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException;

    /**
     * Looks up questionnaires filtered by their status values.
     *
     * @param statusesToInclude Statuses to include.
     * @return List of matching questionnaires.
     * @throws ServiceException If the operation fails.
     */
    List<Questionnaire> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException;

    /**
     * Looks up multiple questionnaires by their IDs.
     *
     * @param questionnaireIds The IDs of the questionnaires.
     * @return List of matching questionnaires.
     * @throws ServiceException If the operation fails.
     */
    List<Questionnaire> lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException;


    /**
     * Updates a questionnaire.
     *
     * @param questionnaire The questionnaire to update.
     */
    void updateQuestionnaire(Questionnaire questionnaire);

    /**
     * Retrieves the organization ID for the current context.
     *
     * @return The organization ID.
     * @throws ServiceException If the operation fails.
     */
    String getOrganizationId() throws ServiceException;

    /**
     * Gets the organization of the currently authenticated user.
     *
     * @return The user's organization.
     * @throws ServiceException If the operation fails.
     */
    Organization getCurrentUsersOrganization() throws ServiceException;

    /**
     * Looks up a practitioner by their ID.
     *
     * @param practitionerId The ID of the practitioner.
     * @return An optional practitioner.
     */
    Optional<Practitioner> lookupPractitionerById(String practitionerId);

    /**
     * Looks up a questionnaire by its qualified ID.
     *
     * @param qualifiedId The qualified ID of the questionnaire.
     * @return An optional questionnaire.
     * @throws ServiceException If the operation fails.
     */
    Optional<Questionnaire> lookupQuestionnaireById(String qualifiedId) throws ServiceException;


    CarePlanClient<CarePlan, Patient> carePlan();
}

