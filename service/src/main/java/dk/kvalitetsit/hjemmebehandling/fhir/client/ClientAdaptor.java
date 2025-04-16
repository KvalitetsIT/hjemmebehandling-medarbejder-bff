package dk.kvalitetsit.hjemmebehandling.fhir.client;

import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Organization;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the client interface, but this might change in the future
 */
public class ClientAdaptor implements Client<
        CarePlanModel,
        PlanDefinitionModel,
        PractitionerModel,
        PatientModel,
        QuestionnaireModel,
        QuestionnaireResponseModel,
        Organization,
        CarePlanStatus> {

    private final FhirClient client;
    private final FhirMapper mapper;

    public ClientAdaptor(FhirClient client, FhirMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }



    @Override
    public List<PlanDefinitionModel> fetchActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException {
        return client.fetchActivePlanDefinitionsUsingQuestionnaireWithId(questionnaireId).stream().map(mapper::mapPlanDefinition).toList();
    }





    @Override
    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException {
        return client.lookupOrganizationBySorCode(sorCode);
    }

    @Override
    public Optional<PatientModel> lookupPatientById(String patientId) {
        return client.lookupPatientById(patientId).map(this.mapper::mapPatient);
    }

    @Override
    public Optional<PatientModel> lookupPatientByCpr(String cpr) {
        return client.lookupPatientByCpr(cpr).map(this.mapper::mapPatient);
    }

    @Override
    public List<PatientModel> searchPatients(List<String> searchStrings, CarePlanStatus carePlanStatus) throws ServiceException {
        return client.searchPatients(searchStrings, mapper.mapCarePlanStatus(carePlanStatus)).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> getPatientsByStatus(CarePlanStatus carePlanStatus) throws ServiceException {
        return client.getPatientsByStatus(mapper.mapCarePlanStatus(carePlanStatus))
                .stream()
                .map(mapper::mapPatient)
                .toList();
    }


    @Override
    public Optional<QuestionnaireResponseModel> lookupQuestionnaireResponseById(String questionnaireResponseId) {
        return client.lookupQuestionnaireResponseById(questionnaireResponseId).map(mapper::mapQuestionnaireResponse);
    }

    @Override
    public List<QuestionnaireResponseModel> lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) {
        return client.lookupQuestionnaireResponses(carePlanId, questionnaireIds).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireModel> lookupVersionsOfQuestionnaireById(List<String> ids) {
        return client.lookupVersionsOfQuestionnaireById(ids).stream().map(this.mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        return client.lookupQuestionnaireResponsesByStatus(statuses).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException {
        return client.lookupQuestionnaireResponsesByStatusAndCarePlanId(statuses, carePlanId)
                .stream()
                .map(mapper::mapQuestionnaireResponse)
                .toList();
    }

    @Override
    public List<QuestionnaireResponseModel> lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException {
        return client.lookupQuestionnaireResponsesByStatus(status)
                .stream()
                .map(mapper::mapQuestionnaireResponse)
                .toList();
    }



    @Override
    public String savePractitioner(PractitionerModel practitioner) throws ServiceException {
        return client.savePractitioner(this.mapper.mapPractitionerModel(practitioner));
    }

    @Override
    public String saveQuestionnaireResponse(QuestionnaireResponseModel questionnaireResponse) throws ServiceException {
        return client.saveQuestionnaireResponse(this.mapper.mapQuestionnaireResponseModel(questionnaireResponse));
    }

    @Override
    public String saveQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException {
        return client.saveQuestionnaire(this.mapper.mapQuestionnaireModel(questionnaire));
    }

    @Override
    public String savePlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException {
        return client.savePlanDefinition(this.mapper.mapPlanDefinitionModel(planDefinition));
    }

    @Override
    public String savePatient(PatientModel patient) throws ServiceException {
        return client.savePatient(this.mapper.mapPatientModel(patient));
    }

    @Override
    public Optional<PlanDefinitionModel> lookupPlanDefinition(String planDefinitionId) {
        return client.lookupPlanDefinition(planDefinitionId).map(mapper::mapPlanDefinition);
    }

    @Override
    public List<PlanDefinitionModel> lookupPlanDefinitions() throws ServiceException {
        return client.lookupPlanDefinitions().stream().map(mapper::mapPlanDefinition).toList();
    }

    @Override
    public List<PlanDefinitionModel> lookupPlanDefinitionsById(Collection<String> planDefinitionIds) {
        return client.lookupPlanDefinitionsById(planDefinitionIds)
                .stream()
                .map(mapper::mapPlanDefinition)
                .toList();
    }

    @Override
    public List<PlanDefinitionModel> lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException {
        return client.lookupPlanDefinitionsByStatus(statusesToInclude).stream().map(mapper::mapPlanDefinition).toList();
    }

    @Override
    public List<QuestionnaireModel> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        return client.lookupQuestionnairesByStatus(statusesToInclude).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireModel> lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException {
        return client.lookupQuestionnairesById(questionnaireIds).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public void updateCarePlan(CarePlanModel carePlan, PatientModel patient) {
        this.client.updateCarePlan(this.mapper.mapCarePlanModel(carePlan), this.mapper.mapPatientModel(patient));

    }

    @Override
    public void updatePlanDefinition(PlanDefinitionModel planDefinition) {
        this.client.updatePlanDefinition(this.mapper.mapPlanDefinitionModel(planDefinition));

    }

    @Override
    public void updateQuestionnaireResponse(QuestionnaireResponseModel questionnaireResponse) {
        this.client.updateQuestionnaireResponse(this.mapper.mapQuestionnaireResponseModel(questionnaireResponse));

    }

    @Override
    public void updateQuestionnaire(QuestionnaireModel questionnaire) {
        this.client.updateQuestionnaire(mapper.mapQuestionnaireModel(questionnaire));

    }

    @Override
    public void updatePatient(PatientModel patient) {
        this.client.updatePatient(mapper.mapPatientModel(patient));
    }

    @Override
    public String getOrganizationId() throws ServiceException {
        return client.getOrganizationId();
    }

    @Override
    public Organization getCurrentUsersOrganization() throws ServiceException {
        return client.getCurrentUsersOrganization();
    }

    @Override
    public PractitionerModel getOrCreateUserAsPractitioner() throws ServiceException {
        return this.mapper.mapPractitioner(client.getOrCreateUserAsPractitioner());
    }

    @Override
    public Optional<PractitionerModel> lookupPractitionerById(String practitionerId) {
        return client.lookupPractitionerById(practitionerId)
                .map(this.mapper::mapPractitioner);
    }

    @Override
    public Optional<QuestionnaireModel> lookupQuestionnaireById(String qualifiedId) throws ServiceException {
        return client.lookupQuestionnaireById(qualifiedId).map(mapper::mapQuestionnaire);
    }

    @Override
    public List<PractitionerModel> lookupPractitioners(Collection<String> practitionerIds) {
        return client.lookupPractitioners(practitionerIds)
                .stream()
                .map(mapper::mapPractitioner)
                .toList();
    }

    @Override
    public FhirLookupResult lookupValueSet() throws ServiceException {
        return client.lookupValueSet();
    }

    @Override
    public void update(CarePlanModel resource) {
        this.client.update(this.mapper.mapCarePlanModel(resource));
    }

    @Override
    public String save(CarePlanModel resource) throws ServiceException {
        return client.save(this.mapper.mapCarePlanModel(resource));
    }

    @Override
    public Optional<CarePlanModel> fetch(String id) throws ServiceException {
        return client.fetch(id).map(this.mapper::mapCarePlan);
    }




    @Override
    public List<CarePlanModel> fetch(String... id) {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetch() {
        throw new NotImplementedException();
    }


}
