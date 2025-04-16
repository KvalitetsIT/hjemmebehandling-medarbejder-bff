package dk.kvalitetsit.hjemmebehandling.fhir.client;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.fhir.BundleBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.fhir.client.FhirClient.getQuestionnaireIdsFromCarePlan;
import static dk.kvalitetsit.hjemmebehandling.fhir.client.FhirClient.getQuestionnaireIdsFromPlanDefinition;

public class ConcreteCarePlanClient implements CarePlanClient<CarePlan, Patient> {

    private final FhirClient client;

    public ConcreteCarePlanClient(FhirClient client) {
        this.client = client;
    }

    @Override
    public void update(CarePlan carePlan, Patient patient) {
        var bundle = new BundleBuilder().buildUpdateCarePlanBundle(carePlan, patient);
        saveInTransaction(bundle, ResourceType.CarePlan);
    }

    @Override
    public String save(CarePlan carePlan, Patient patient) throws ServiceException {
        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCreateCarePlanBundle(carePlan, patient);
        addOrganizationTag(bundle);

        var id = saveInTransaction(bundle, ResourceType.CarePlan);

        return id.orElseThrow(() -> new IllegalStateException("Could not locate location-header in response when executing transaction."));
    }

    @Override
    public List<CarePlan> lookupCarePlans(String cpr, Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {

        var criteria = createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);

        Optional<Patient> patient = lookupPatientByCpr(cpr);
        if (patient.isEmpty()) {
            return List.of();
        }
        String patientId = patient.get().getIdElement().toUnqualifiedVersionless().toString();
        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        criteria.add(patientCriterion);

        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);

        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec)).getCarePlans();
    }

    @Override
    public List<CarePlan> fetchActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var plandefinitionCriterion = CarePlan.INSTANTIATES_CANONICAL.hasId(plandefinitionId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, plandefinitionCriterion, organizationCriterion));
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }

    @Override
    public List<CarePlan> fetchCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException {
        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(patientCriterion, organizationCriterion));
        if (onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }

    @Override
    public List<CarePlan> fetchCarePlans(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        var criteria = createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);
        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec)).getCarePlans();
    }

    public List<CarePlan> fetchActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var questionnaireCriterion = CarePlan.INSTANTIATES_CANONICAL.hasChainedProperty(PlanDefinition.DEFINITION.hasId(questionnaireId));
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion, organizationCriterion));
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }


    @Override
    public void update(CarePlan resource) {

    }

    @Override
    public String save(CarePlan resource) throws ServiceException {
        return "";
    }

    @Override
    public Optional<CarePlan> fetch(String id) throws ServiceException {
        var idCriterion = CarePlan.RES_ID.exactly().code(id);

        var result = this.lookupCarePlansByCriteria(List.of(idCriterion));

        if (result.getCarePlans().isEmpty()) {
            return Optional.empty();
        }
        if (result.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", CarePlan.class));
        }

        return Optional.of(result.getCarePlans().getFirst());
    }

    @Override
    public List<CarePlan> fetch(String... id) {
        return List.of();
    }

    @Override
    public List<CarePlan> fetch() {
        return List.of();
    }


    public FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return lookupCarePlansByCriteria(criteria, Optional.empty());
    }

    public  FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria, Optional<SortSpec> sortSpec) throws ServiceException {
        boolean withOrganizations = true;
        var carePlanResult = client.lookupByCriteria(CarePlan.class, criteria, List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL), withOrganizations, sortSpec, Optional.empty(), Optional.empty());

        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server, not the questionnaire-resources. Se wo look up those in a separate call.
        if(carePlanResult.getCarePlans().isEmpty()) {
            return carePlanResult;
        }

        // Get the related questionnaire-resources
        List<String> questionnaireIds = new ArrayList<>();
        questionnaireIds.addAll(getQuestionnaireIdsFromCarePlan(carePlanResult.getCarePlans()));
        questionnaireIds.addAll(getQuestionnaireIdsFromPlanDefinition(carePlanResult.getPlanDefinitions()));
        List<Questionnaire> questionnaireResult = client.lookupQuestionnairesById(questionnaireIds);

        // Merge the results
        return carePlanResult.merge(questionnaireResult);
    }

}
