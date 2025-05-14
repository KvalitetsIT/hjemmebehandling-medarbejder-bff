package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

/**
 * Generic client interface for managing FHIR-based entities such as CarePlans, PlanDefinitions,
 * Practitioners, Patients, Questionnaires, and related resources.
 */
public interface Client  {

    /**
     * Performs a value set lookup (e.g. for coding systems or terminology).
     *
     * @return The lookup result.
     * @throws ServiceException If the operation fails.
     */
    FhirLookupResult lookupValueSet() throws ServiceException;


}

