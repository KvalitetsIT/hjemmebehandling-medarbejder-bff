package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.MeasurementTypeDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

// TODO: Update to record
public class MeasurementTypeModel implements ToDto<MeasurementTypeDto> {
  private String system;
  private String code;
  private String display;

  public String getSystem() {
    return system;
  }

  public void setSystem(String system) {
    this.system = system;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDisplay() {
    return display;
  }

  public void setDisplay(String display) {
    this.display = display;
  }

  @Override
  public MeasurementTypeDto toDto() {
    MeasurementTypeDto measurementTypeDto = new MeasurementTypeDto();

    measurementTypeDto.setSystem(this.getSystem());
    measurementTypeDto.setCode(this.getCode());
    measurementTypeDto.setDisplay(this.getDisplay());

    return measurementTypeDto;
  }
}
