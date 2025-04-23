package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.fhir.BundleBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ConcreteCarePlanRepository implements CarePlanRepository<CarePlan, Patient> {

    private final FhirClient client;

    public ConcreteCarePlanRepository(FhirClient client) {
        this.client = client;
    }

    @Override
    public void update(CarePlan carePlan, Patient patient) {
        var bundle = new BundleBuilder().buildUpdateCarePlanBundle(carePlan, patient);
        client.saveInTransaction(bundle, ResourceType.CarePlan);
    }

    @Override
    public String save(CarePlan carePlan, Patient patient) throws ServiceException {
        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCreateCarePlanBundle(carePlan, patient);
        client.addOrganizationTag(bundle);

        var id = client.saveInTransaction(bundle, ResourceType.CarePlan);

        return id.orElseThrow(() -> new IllegalStateException("Could not locate location-header in response when executing transaction."));
    }

    @Override
    public List<CarePlan> fetch(QualifiedId patientId, Instant unsatisfiedToDate, boolean onlyUnSatisfied, boolean onlyActiveCarePlans) throws ServiceException {

        var criteria = client.createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);

        var patientCriterion = CarePlan.PATIENT.hasId(patientId.id());
        criteria.add(patientCriterion);

        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);

        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec)).getCarePlans();
    }

    @Override
    public List<CarePlan> fetchActiveCarePlansByPlanDefinitionId(QualifiedId plandefinitionId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var plandefinitionCriterion = CarePlan.INSTANTIATES_CANONICAL.hasId(plandefinitionId.id());
        var organizationCriterion = client.buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, plandefinitionCriterion, organizationCriterion));
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }

    @Override
    public List<CarePlan> fetchCarePlansByPatientId(QualifiedId patientId, boolean onlyActiveCarePlans) throws ServiceException {
        var patientCriterion = CarePlan.PATIENT.hasId(patientId.id());
        var organizationCriterion = client.buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(patientCriterion, organizationCriterion));
        if (onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }

    @Override
    public List<CarePlan> fetch(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        var criteria = client.createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);
        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec)).getCarePlans();
    }

    public List<CarePlan> fetchActiveCarePlansWithQuestionnaire(QualifiedId questionnaireId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var questionnaireCriterion = CarePlan.INSTANTIATES_CANONICAL.hasChainedProperty(PlanDefinition.DEFINITION.hasId(questionnaireId.id()));
        var organizationCriterion = client.buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion, organizationCriterion));
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }


    @Override
    public void update(CarePlan resource) {
        throw new NotImplementedException();
    }

    @Override
    public String save(CarePlan resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<CarePlan> fetch(QualifiedId id) throws ServiceException {
        var idCriterion = CarePlan.RES_ID.exactly().code(id.id());

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
    public List<CarePlan> fetch(List<QualifiedId> id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlan> fetch() {
        throw new NotImplementedException();
    }

    private FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return lookupCarePlansByCriteria(criteria, Optional.empty());
    }

    private  FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria, Optional<SortSpec> sortSpec) throws ServiceException {
        boolean withOrganizations = true;
        var carePlanResult = client.lookupByCriteria(
                CarePlan.class, criteria,
                List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL),
                withOrganizations,
                sortSpec,
                Optional.empty(),
                Optional.empty()
        );

        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server, not the questionnaire-resources. Se wo look up those in a separate call.
        if(carePlanResult.getCarePlans().isEmpty()) {
            return carePlanResult;
        }

        // Get the related questionnaire-resources
        List<String> questionnaireIds = new ArrayList<>();
        questionnaireIds.addAll(extractQuestionnaireIdsFromCarePlan(carePlanResult.getCarePlans()));
        questionnaireIds.addAll(extractQuestionnaireIdsFromPlanDefinition(carePlanResult.getPlanDefinitions()));
        List<Questionnaire> questionnaireResult = questionnaireRepository.lookupQuestionnairesById(questionnaireIds);

        // Merge the results½
        return carePlanResult.merge(questionnaireResult);
    }

    private static List<String> extractQuestionnaireIdsFromCarePlan(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getActivity().stream().map(a -> getQuestionnaireId(a.getDetail())))
                .toList();
    }

    private static String getQuestionnaireId(CarePlan.CarePlanActivityDetailComponent detail) {
        if (detail.getInstantiatesCanonical() == null || detail.getInstantiatesCanonical().size() != 1) {
            throw new IllegalStateException("Expected InstantiatesCanonical to be present, and to contain exactly one value!");
        }
        return detail.getInstantiatesCanonical().getFirst().getValue();
    }

    private static List<String> extractQuestionnaireIdsFromPlanDefinition(List<PlanDefinition> planDefinitions) {
        return planDefinitions
                .stream()
                .flatMap(pd -> pd.getAction().stream().map(a -> a.getDefinitionCanonicalType().getValue()))
                .toList();
    }
}
