package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;


import org.hl7.fhir.r4.model.ValueSet;

import java.util.List;
import java.util.Optional;

public class ValueSetRepository implements Repository<ValueSet, QualifiedId.ValueSetId>{

    private final FhirClient fhirClient;

    public ValueSetRepository(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public List<ValueSet> lookupValueSet() throws ServiceException {
        var organizationCriterion = dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils.buildOrganizationCriterion();
        return fhirClient.lookupByCriteria(ValueSet.class, List.of(organizationCriterion)).getValueSets();
    }

}
