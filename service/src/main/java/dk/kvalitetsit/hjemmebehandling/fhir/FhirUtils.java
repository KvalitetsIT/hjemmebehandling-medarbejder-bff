package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.ResourceType;

import java.util.regex.Pattern;

public class FhirUtils {
    private static final Pattern plainIdPattern = Pattern.compile("^[a-z0-9\\-]+$");

    public static String qualifyId(String id, ResourceType qualifier) {
        if (isQualifiedId(id, qualifier)) {
            return id;
        }
        if (!isPlainId(id)) {
            throw new IllegalArgumentException(String.format("Cannot qualify id: %s", id));
        }
        return qualifier + "/" + id;
    }

    public static boolean isPlainId(String id) {
        return plainIdPattern.matcher(id).matches();
    }

    public static boolean isQualifiedId(String id, ResourceType qualifier) {
        String prefix = qualifier.toString() + "/";
        return id.startsWith(prefix) && isPlainId(id.substring(prefix.length()));
    }

    public static ICriterion<?> buildOrganizationCriterion(QualifiedId.OrganizationId organizationId) throws ServiceException {
        return new ReferenceClientParam(SearchParameters.ORGANIZATION).hasId(organizationId.unqualified());
    }

    public static ICriterion<ReferenceClientParam> buildPlanDefinitionCriterion(QualifiedId.PlanDefinitionId plandefinitionId) {
        return CarePlan.INSTANTIATES_CANONICAL.hasId(plandefinitionId.unqualified());
    }
}
