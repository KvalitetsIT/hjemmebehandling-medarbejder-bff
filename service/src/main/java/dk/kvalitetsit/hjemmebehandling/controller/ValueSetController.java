package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.MeasurementTypeDto;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.service.ValueSetService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "ValueSet", description = "API for retrieving various valuesets, such as measurement types, supported by the system.")
public class ValueSetController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ValueSetController.class);

  private final ValueSetService valueSetService;
  private final DtoMapper dtoMapper;

  public ValueSetController(ValueSetService valueSetService, DtoMapper dtoMapper) {
    this.valueSetService = valueSetService;
    this.dtoMapper = dtoMapper;
  }

  @Operation(summary = "Get measurement types.", description = "Retrieves list of measurement types supported by the requestors organization.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successful operation.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MeasurementTypeDto.class))))
  })
  @GetMapping(value = "/v1/measurementtype")
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
