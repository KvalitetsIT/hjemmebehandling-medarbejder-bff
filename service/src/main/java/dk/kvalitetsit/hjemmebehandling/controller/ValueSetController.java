package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.service.ValueSetService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openapitools.api.ValueSetApi;
import org.openapitools.model.MeasurementTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "ValueSet", description = "API for retrieving various valuesets, such as measurement types, supported by the system.")
public class ValueSetController extends BaseController implements ValueSetApi {
  private static final Logger logger = LoggerFactory.getLogger(ValueSetController.class);

  private final ValueSetService valueSetService;
  private final DtoMapper dtoMapper;

  public ValueSetController(ValueSetService valueSetService, DtoMapper dtoMapper) {
    this.valueSetService = valueSetService;
    this.dtoMapper = dtoMapper;
  }

  @Override
  public ResponseEntity<List<MeasurementTypeDto>> getMeasurementTypes() {
    try {
      List<MeasurementTypeModel> measurementTypes = valueSetService.getMeasurementTypes();
      return ResponseEntity.ok(measurementTypes.stream().map(dtoMapper::mapMeasurementTypeModel).collect(Collectors.toList()));

    } catch(ServiceException e) {
      logger.error("Could not get measurement types", e);
      throw toStatusCodeException(e);
    }

  }
}
