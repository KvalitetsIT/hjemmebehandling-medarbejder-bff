package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteValueSetService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.openapitools.api.ValueSetApi;
import org.openapitools.model.MeasurementTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ValueSetController extends BaseController implements ValueSetApi {
    private static final Logger logger = LoggerFactory.getLogger(ValueSetController.class);

    private final ConcreteValueSetService valueSetService;
    private final DtoMapper dtoMapper;

    public ValueSetController(ConcreteValueSetService valueSetService, DtoMapper dtoMapper) {
        this.valueSetService = valueSetService;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public ResponseEntity<List<MeasurementTypeDto>> getMeasurementTypes() {
        try {
            List<MeasurementTypeModel> measurementTypes = valueSetService.getMeasurementTypes();
            return ResponseEntity.ok(measurementTypes.stream().map(dtoMapper::mapMeasurementTypeModel).toList());

        } catch (ServiceException e) {
            logger.error("Could not get measurement types", e);
            throw toStatusCodeException(e);
        }

    }
}
