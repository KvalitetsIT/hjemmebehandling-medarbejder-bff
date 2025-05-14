package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;

public class FhirUtils {

    public static ICriterion<?> buildOrganizationCriterion(QualifiedId.OrganizationId organizationId) throws ServiceException {
        return new ReferenceClientParam(SearchParameters.ORGANIZATION).hasId(organizationId.unqualified());
    }

    public static ICriterion<ReferenceClientParam> buildPlanDefinitionCriterion(QualifiedId.PlanDefinitionId plandefinitionId) {
        return CarePlan.INSTANTIATES_CANONICAL.hasId(plandefinitionId.unqualified());
    }
}
