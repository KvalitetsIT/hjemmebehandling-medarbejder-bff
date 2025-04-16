package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.client.Client;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValueSetService {
    private static final Logger logger = LoggerFactory.getLogger(ValueSetService.class);


    private final Client<
            CarePlanModel,
            PlanDefinitionModel,
            PractitionerModel,
            PatientModel,
            QuestionnaireModel,
            QuestionnaireResponseModel,
            Organization,
            CarePlanStatus> fhirClient;


    public ValueSetService(
            Client<
                    CarePlanModel,
                    PlanDefinitionModel,
                    PractitionerModel,
                    PatientModel,
                    QuestionnaireModel,
                    QuestionnaireResponseModel,
                    Organization,
                    CarePlanStatus> fhirClient) {
        this.fhirClient = fhirClient;
    }

    public List<MeasurementTypeModel> getMeasurementTypes() throws ServiceException {
        // as of now we only have one ValueSet in the system which holds the measurement type codes, so no special search handling is needed.
        FhirLookupResult lookupResult = fhirClient.lookupValueSet();

        List<MeasurementTypeModel> result = new ArrayList<>();
        lookupResult.getValueSets()
                .forEach(vs -> {
                    var list = extractMeasurementTypes(vs);
                    result.addAll(list);
                });

        return result;
    }

    private List<MeasurementTypeModel> extractMeasurementTypes(ValueSet valueSet) {
        return valueSet.getCompose().getInclude()
                .stream()
                .flatMap(csc -> csc.getConcept()
                        .stream()
                        .map(crc -> mapCodingConcept(csc.getSystem(), crc))).toList();


    }

    private MeasurementTypeModel mapCodingConcept(String system, ValueSet.ConceptReferenceComponent concept) {
        return new MeasurementTypeModel(system, concept.getCode(), concept.getDisplay());
    }

}
