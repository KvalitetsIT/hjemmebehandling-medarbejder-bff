package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;

public class MeasurementTypeDto implements Dto<MeasurementTypeModel> {
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
  public MeasurementTypeModel toModel() {
    return null;
  }
}
