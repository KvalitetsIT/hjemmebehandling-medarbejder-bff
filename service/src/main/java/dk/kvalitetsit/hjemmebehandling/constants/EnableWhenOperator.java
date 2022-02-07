package dk.kvalitetsit.hjemmebehandling.constants;

public enum EnableWhenOperator {
  EQUAL("="), GREATER_THAN(">"), LESS_THAN("<"), GREATER_OR_EQUAL(">="), LESS_OR_EQUAL("<=");

  private String code;

  EnableWhenOperator(String code) {
    this.code = code;
  }
}
