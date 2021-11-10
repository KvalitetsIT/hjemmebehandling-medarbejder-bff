package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlanDefinitionService {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    public PlanDefinitionService(FhirClient fhirClient, FhirMapper fhirMapper) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }

    public List<PlanDefinitionModel> getPlanDefinitions() throws ServiceException {
        List<PlanDefinition> planDefinitions = fhirClient.lookupPlanDefinitions();

        return planDefinitions.stream().map(pd -> fhirMapper.mapPlanDefinition(pd)).collect(Collectors.toList());
    }
}
