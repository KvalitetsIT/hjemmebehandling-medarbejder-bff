package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FhirClient<CarePlan, Patient, PlanDefinition, Questionnaire, QuestionnaireResponse, Practitioner> {

    FhirLookupResult lookupActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException;
    FhirLookupResult lookupActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException;
    FhirLookupResult lookupActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException;
    FhirLookupResult lookupCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException;
    FhirLookupResult lookupCarePlans(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;


    FhirLookupResult lookupCarePlans(String cpr, Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;

    FhirLookupResult lookupCarePlanById(String carePlanId) throws ServiceException;

    Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException;

    Optional<Patient> lookupPatientById(String patientId);

    Optional<Patient> lookupPatientByCpr(String cpr);

    FhirLookupResult searchPatients(List<String> searchStrings, org.hl7.fhir.r4.model.CarePlan.CarePlanStatus status) throws ServiceException;

    FhirLookupResult getPatientsByStatus(org.hl7.fhir.r4.model.CarePlan.CarePlanStatus status) throws ServiceException;

    FhirLookupResult lookupQuestionnaireResponseById(String questionnaireResponseId);
    FhirLookupResult lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds);
    List<Questionnaire> lookupVersionsOfQuestionnaireById(List<String> ids);
    FhirLookupResult lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException;
    FhirLookupResult lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException;
    FhirLookupResult lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException;

    String saveCarePlan(CarePlan carePlan, Patient patient) throws ServiceException;
    String saveCarePlan(CarePlan carePlan) throws ServiceException;
    String savePractitioner(Practitioner practitioner) throws ServiceException;
    String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) throws ServiceException;
    String saveQuestionnaire(Questionnaire questionnaire) throws ServiceException;
    String savePlanDefinition(PlanDefinition planDefinition) throws ServiceException;
    String savePatient(Patient patient) throws ServiceException;

    FhirLookupResult lookupPlanDefinition(String planDefinitionId);
    FhirLookupResult lookupPlanDefinitions() throws ServiceException;
    FhirLookupResult lookupPlanDefinitionsById(Collection<String> planDefinitionIds);
    FhirLookupResult lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException;
    FhirLookupResult lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException;
    FhirLookupResult lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException;

    void updateCarePlan(CarePlan carePlan, Patient patient);
    void updateCarePlan(CarePlan carePlan);
    void updatePlanDefinition(PlanDefinition planDefinition);
    void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse);
    void updateQuestionnaire(Questionnaire questionnaire);

    String getOrganizationId() throws ServiceException;

    Organization getCurrentUsersOrganization() throws ServiceException;

    Practitioner getOrCreateUserAsPractitioner() throws ServiceException;

    Optional<Practitioner> lookupPractitionerById(String practitionerId);

    FhirLookupResult lookupPractitioners(Collection<String> practitionerIds);

    FhirLookupResult lookupValueSet() throws ServiceException;

    void updatePatient(Patient patient);
}
