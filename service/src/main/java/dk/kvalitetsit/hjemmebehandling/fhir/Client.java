package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Client<CarePlan, PlanDefinition, Practitioner, Patient, Questionnaire, QuestionnaireResponse, Organization,CarePlanStatus > {

    List<CarePlan> lookupActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException;

    List<PlanDefinition> lookupActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException;

    List<CarePlan> lookupActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException;

    List<CarePlan> lookupCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException;

    List<CarePlan> lookupCarePlans(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;

    Practitioner getOrCreateUserAsPractitioner() throws ServiceException;

    List<Practitioner> lookupPractitioners(Collection<String> practitionerIds);

    FhirLookupResult lookupValueSet() throws ServiceException;

    void updatePatient(Patient patient);

    void updatePlanDefinition(PlanDefinition planDefinition);

    void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse);

    List<CarePlan> lookupCarePlans(String cpr, Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;

    Optional<CarePlan> lookupCarePlanById(String carePlanId) throws ServiceException;

    Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException;

    Optional<Patient> lookupPatientById(String patientId);

    Optional<Patient> lookupPatientByCpr(String cpr);

    List<Patient> searchPatients(List<String> searchStrings, CarePlanStatus status) throws ServiceException;

    List<Patient> getPatientsByStatus(CarePlanStatus status) throws ServiceException;

    Optional<QuestionnaireResponse> lookupQuestionnaireResponseById(String questionnaireResponseId);

    List<QuestionnaireResponse> lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds);

    List<Questionnaire> lookupVersionsOfQuestionnaireById(List<String> ids);

    List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException;

    List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException;

    List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException;

    String saveCarePlan(CarePlan carePlan, Patient patient) throws ServiceException;

    String saveCarePlan(CarePlan carePlan) throws ServiceException;

    String savePractitioner(Practitioner practitioner) throws ServiceException;

    String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) throws ServiceException;

    String saveQuestionnaire(Questionnaire questionnaire) throws ServiceException;

    String savePlanDefinition(PlanDefinition planDefinition) throws ServiceException;

    String savePatient(Patient patient) throws ServiceException;

    Optional<PlanDefinition> lookupPlanDefinition(String planDefinitionId);

    List<PlanDefinition> lookupPlanDefinitions() throws ServiceException;

    List<PlanDefinition> lookupPlanDefinitionsById(Collection<String> planDefinitionIds);

    List<PlanDefinition> lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException;

    List<Questionnaire> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException;

    List<Questionnaire> lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException;

    void updateCarePlan(CarePlan carePlan, Patient patient);

    void updateCarePlan(CarePlan carePlan);

    void updateQuestionnaire(Questionnaire questionnaire);

    String getOrganizationId() throws ServiceException;

    Organization getCurrentUsersOrganization() throws ServiceException;

    Optional<Practitioner> lookupPractitionerById(String practitionerId);

    Optional<Questionnaire> lookupQuestionnaireById(String qualifiedId) throws ServiceException;
}
