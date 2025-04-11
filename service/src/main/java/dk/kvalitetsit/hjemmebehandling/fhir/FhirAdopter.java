package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class FhirAdopter implements FhirClient<CarePlanModel, PatientModel, PlanDefinitionModel, QuestionnaireModel, QuestionnaireResponseModel, PractitionerModel> {

    private final FhirMapper fhirMapper;
    private final FhirClient<CarePlan, Patient, PlanDefinition, Questionnaire, QuestionnaireResponse, Practitioner> fhirClient;

    public FhirAdopter(FhirMapper fhirMapper, FhirClient<CarePlan, Patient, PlanDefinition, Questionnaire, QuestionnaireResponse, Practitioner> fhirClient) {
        this.fhirMapper = fhirMapper;
        this.fhirClient = fhirClient;
    }

    @Override
    public FhirLookupResult lookupActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException {
        return fhirClient.lookupActiveCarePlansWithQuestionnaire(questionnaireId);
    }

    @Override
    public FhirLookupResult lookupActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException {
        return fhirClient.lookupActivePlanDefinitionsUsingQuestionnaireWithId(questionnaireId);
    }

    @Override
    public FhirLookupResult lookupActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException {
        return fhirClient.lookupActiveCarePlansWithPlanDefinition(plandefinitionId);
    }

    @Override
    public FhirLookupResult lookupCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException {
        return fhirClient.lookupCarePlansByPatientId(patientId, onlyActiveCarePlans);
    }

    @Override
    public FhirLookupResult lookupCarePlans(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        return fhirClient.lookupCarePlans(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
    }

    @Override
    public FhirLookupResult lookupCarePlans(String cpr, Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        return fhirClient.lookupCarePlans(cpr, unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
    }

    @Override
    public FhirLookupResult lookupCarePlanById(String carePlanId) throws ServiceException {
        return fhirClient.lookupCarePlanById(carePlanId);
    }

    @Override
    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException {
        return fhirClient.lookupOrganizationBySorCode(sorCode);
    }

    @Override
    public Optional<PatientModel> lookupPatientById(String patientId) {
        return fhirClient.lookupPatientById(patientId).map(this.fhirMapper::mapPatient);
    }

    @Override
    public Optional<PatientModel> lookupPatientByCpr(String cpr) {
        return fhirClient.lookupPatientByCpr(cpr).map(this.fhirMapper::mapPatient);
    }

    @Override
    public FhirLookupResult searchPatients(List<String> searchStrings, CarePlan.CarePlanStatus status) throws ServiceException {
        return fhirClient.searchPatients(searchStrings, status);
    }

    @Override
    public FhirLookupResult getPatientsByStatus(CarePlan.CarePlanStatus status) throws ServiceException {
        return fhirClient.getPatientsByStatus(status);
    }

    @Override
    public FhirLookupResult lookupQuestionnaireResponseById(String questionnaireResponseId) {
        return fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId);
    }

    @Override
    public FhirLookupResult lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) {
        return fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds);
    }

    @Override
    public List<QuestionnaireModel> lookupVersionsOfQuestionnaireById(List<String> ids) {
        return fhirClient.lookupVersionsOfQuestionnaireById(ids).stream().map(this.fhirMapper::mapQuestionnaire).toList();
    }

    @Override
    public FhirLookupResult lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatus(statuses);
    }

    @Override
    public FhirLookupResult lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(statuses, carePlanId);
    }

    @Override
    public FhirLookupResult lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatus(status);
    }

    @Override
    public String saveCarePlan(CarePlanModel carePlan, PatientModel patient) throws ServiceException {
        return fhirClient.saveCarePlan(this.fhirMapper.mapCarePlanModel(carePlan), this.fhirMapper.mapPatientModel(patient));
    }


    @Override
    public String saveCarePlan(CarePlanModel carePlan) throws ServiceException {
        return fhirClient.saveCarePlan(this.fhirMapper.mapCarePlanModel(carePlan));
    }


    @Override
    public String savePractitioner(PractitionerModel practitioner) throws ServiceException {
        return fhirClient.savePractitioner(this.fhirMapper.mapPractitionerModel(practitioner));
    }

    @Override
    public String saveQuestionnaireResponse(QuestionnaireResponseModel questionnaireResponse) throws ServiceException {
        return fhirClient.saveQuestionnaireResponse(this.fhirMapper.mapQuestionnaireResponseModel(questionnaireResponse));
    }

    @Override
    public String saveQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException {
        return fhirClient.saveQuestionnaire(this.fhirMapper.mapQuestionnaireModel(questionnaire));
    }

    @Override
    public String savePlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException {
        return fhirClient.savePlanDefinition(this.fhirMapper.mapPlanDefinitionModel(planDefinition));
    }

    @Override
    public String savePatient(PatientModel patient) throws ServiceException {
        return fhirClient.savePatient(this.fhirMapper.mapPatientModel(patient));
    }

    @Override
    public FhirLookupResult lookupPlanDefinition(String planDefinitionId) {
        return fhirClient.lookupPlanDefinition(planDefinitionId);
    }

    @Override
    public FhirLookupResult lookupPlanDefinitions() throws ServiceException {
        return fhirClient.lookupPlanDefinitions();
    }

    @Override
    public FhirLookupResult lookupPlanDefinitionsById(Collection<String> planDefinitionIds) {
        return fhirClient.lookupPlanDefinitionsById(planDefinitionIds);
    }

    @Override
    public FhirLookupResult lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException {
        return fhirClient.lookupPlanDefinitionsByStatus(statusesToInclude);
    }

    @Override
    public FhirLookupResult lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        return fhirClient.lookupQuestionnairesByStatus(statusesToInclude);
    }

    @Override
    public FhirLookupResult lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException {
        return fhirClient.lookupQuestionnairesById(questionnaireIds);
    }

    @Override
    public void updateCarePlan(CarePlanModel carePlan, PatientModel patient) {
        this.fhirClient.updateCarePlan(this.fhirMapper.mapCarePlanModel(carePlan), this.fhirMapper.mapPatientModel(patient));

    }

    @Override
    public void updateCarePlan(CarePlanModel carePlan) {
        this.fhirClient.updateCarePlan(this.fhirMapper.mapCarePlan(carePlan));
    }

    @Override
    public void updatePlanDefinition(PlanDefinitionModel planDefinition) {
        this.fhirClient.updatePlanDefinition(this.fhirMapper.mapPlanDefinitionModel(planDefinition));

    }

    @Override
    public void updateQuestionnaireResponse(QuestionnaireResponseModel questionnaireResponse) {
        this.fhirClient.updateQuestionnaireResponse(this.fhirMapper.mapQuestionnaireResponseModel(questionnaireResponse));

    }

    @Override
    public void updateQuestionnaire(QuestionnaireModel questionnaire) {
        this.fhirClient.updateQuestionnaire(fhirMapper.mapQuestionnaireModel(questionnaire));

    }
    @Override
    public void updatePatient(PatientModel patient) {
        this.fhirClient.updatePatient(fhirMapper.mapPatientModel(patient));
    }

    @Override
    public String getOrganizationId() throws ServiceException {
        return fhirClient.getOrganizationId();
    }

    @Override
    public Organization getCurrentUsersOrganization() throws ServiceException {
        return fhirClient.getCurrentUsersOrganization();
    }

    @Override
    public PractitionerModel getOrCreateUserAsPractitioner() throws ServiceException {
        return this.fhirMapper.mapPractitioner(fhirClient.getOrCreateUserAsPractitioner());
    }

    @Override
    public Optional<PractitionerModel> lookupPractitionerById(String practitionerId) {
        return fhirClient.lookupPractitionerById(practitionerId).map(this.fhirMapper::mapPractitioner);
    }

    @Override
    public FhirLookupResult lookupPractitioners(Collection<String> practitionerIds) {
        return fhirClient.lookupPractitioners(practitionerIds);
    }

    @Override
    public FhirLookupResult lookupValueSet() throws ServiceException {
        return fhirClient.lookupValueSet();
    }


}
