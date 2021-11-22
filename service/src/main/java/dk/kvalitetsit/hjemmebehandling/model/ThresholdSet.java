package dk.kvalitetsit.hjemmebehandling.model;

import java.util.ArrayList;
import java.util.List;

public class ThresholdSet {
  private String questionnaireId;
  private List<Threshold> thresholdList;

  public ThresholdSet() {
    this.thresholdList = new ArrayList<>();
  }

  public String getQuestionnaireId() {
    return questionnaireId;
  }

  public void setQuestionnaireId(String questionnaireId) {
    this.questionnaireId = questionnaireId;
  }

  public List<Threshold> getThresholdList() {
    return thresholdList;
  }

  public void setThresholdList(List<Threshold> thresholdList) {
    this.thresholdList = thresholdList;
  }

  public static class Threshold {
    private String questionnaireItemLinkId;
    private String type;
    private String operator;
    private Double valueQuantity;
    private Boolean valueBoolean;

    public String getQuestionnaireItemLinkId() {
      return questionnaireItemLinkId;
    }

    public void setQuestionnaireItemLinkId(String questionnaireItemLinkId) {
      this.questionnaireItemLinkId = questionnaireItemLinkId;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getOperator() {
      return operator;
    }

    public void setOperator(String operator) {
      this.operator = operator;
    }

    public Double getValueQuantity() {
      return valueQuantity;
    }

    public void setValueQuantity(Double valueQuantity) {
      this.valueQuantity = valueQuantity;
    }

    public Boolean getValueBoolean() {
      return valueBoolean;
    }

    public void setValueBoolean(Boolean valueBoolean) {
      this.valueBoolean = valueBoolean;
    }
  }
}
