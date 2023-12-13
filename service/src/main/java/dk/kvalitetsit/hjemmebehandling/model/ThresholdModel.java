package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.ThresholdDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;

public class ThresholdModel implements ToDto<ThresholdDto> {
  private String questionnaireItemLinkId;
  private ThresholdType type;
  private Double valueQuantityLow;
  private Double valueQuantityHigh;
  private Boolean valueBoolean;

  public String getQuestionnaireItemLinkId() {
    return questionnaireItemLinkId;
  }

  public void setQuestionnaireItemLinkId(String questionnaireItemLinkId) {
    this.questionnaireItemLinkId = questionnaireItemLinkId;
  }

  public ThresholdType getType() {
    return type;
  }

  public void setType(ThresholdType type) {
    this.type = type;
  }

  public Double getValueQuantityLow() {
    return valueQuantityLow;
  }

  public void setValueQuantityLow(Double valueQuantityLow) {
    this.valueQuantityLow = valueQuantityLow;
  }

  public Double getValueQuantityHigh() {
    return valueQuantityHigh;
  }

  public void setValueQuantityHigh(Double valueQuantityHigh) {
    this.valueQuantityHigh = valueQuantityHigh;
  }

  public Boolean getValueBoolean() {
    return valueBoolean;
  }

  public void setValueBoolean(Boolean valueBoolean) {
    this.valueBoolean = valueBoolean;
  }

  @Override
  public ThresholdDto toDto() {
    ThresholdDto thresholdDto = new ThresholdDto();
    thresholdDto.setQuestionId(this.getQuestionnaireItemLinkId());
    thresholdDto.setType(this.getType());
    thresholdDto.setValueBoolean(this.getValueBoolean());
    thresholdDto.setValueQuantityLow(this.getValueQuantityLow());
    thresholdDto.setValueQuantityHigh(this.getValueQuantityHigh());
    return thresholdDto;
  }
}
