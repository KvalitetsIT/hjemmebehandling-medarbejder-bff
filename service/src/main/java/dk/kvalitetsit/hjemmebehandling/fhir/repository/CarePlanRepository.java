package dk.kvalitetsit.hjemmebehandling.fhir.repository;


import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.time.Instant;
import java.util.List;

public interface CarePlanRepository<CarePlan, Patient> extends Repository<CarePlan> {

    /**
     * Updates a care plan and its associated patient.
     *
     * @param carePlan The care plan to update.
     * @param patient  The patient associated with the care plan.
     */
    void update(CarePlan carePlan, Patient patient);

    /**
     * Saves a care plan and optionally associates it with a patient.
     *
     * @param carePlan The care plan to save.
     * @param patient  The associated patient.
     * @return The saved care plan ID.
     * @throws ServiceException If the operation fails.
     */
    String save(CarePlan carePlan, Patient patient) throws ServiceException;


    /**
     * Fetches active care plans associated with a specific plan definition.
     *
     * @param plandefinitionId The ID of the plan definition.
     * @return List of matching active care plans.
     * @throws ServiceException If the operation fails.
     */
    List<CarePlan> fetchActiveCarePlansByPlanDefinitionId(QualifiedId plandefinitionId) throws ServiceException;

    /**
     * Fetches active care plans that include a specific questionnaire.
     *
     * @param questionnaireId The ID of the questionnaire.
     * @return List of matching active care plans.
     * @throws ServiceException If the operation fails.
     */
    List<CarePlan> fetchActiveCarePlansWithQuestionnaire(QualifiedId questionnaireId) throws ServiceException;

    /**
     * Fetches care plans for a specific patient.
     *
     * @param patientId            The patient ID.
     * @param onlyActiveCarePlans Whether to include only active care plans.
     * @return List of matching care plans.
     * @throws ServiceException If the operation fails.
     */
    List<CarePlan> fetchCarePlansByPatientId(QualifiedId patientId, boolean onlyActiveCarePlans) throws ServiceException;

    /**
     * Fetches care plans based on satisfaction state and creation date.
     *
     * @param unsatisfiedToDate   Filter care plans created before this date.
     * @param onlyActiveCarePlans Whether to include only active care plans.
     * @param onlyUnSatisfied     Whether to include only unsatisfied care plans.
     * @return List of matching care plans.
     * @throws ServiceException If the operation fails.
     */
    List<CarePlan> fetch(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;

    /**
     * Fetches care plans based on CPR and optional filters.
     *
     * @param patientId
     * @param unsatisfiedToDate   Date filter for unsatisfied care plans.
     * @param onlyUnSatisfied     Whether to include only unsatisfied plans.
     * @param onlyActiveCarePlans Whether to include only active plans.
     * @return List of matching care plans.
     * @throws ServiceException If the operation fails.
     */
    List<CarePlan> fetch(QualifiedId patientId, Instant unsatisfiedToDate, boolean onlyUnSatisfied, boolean onlyActiveCarePlans) throws ServiceException;

}
