package dk.kvalitetsit.hjemmebehandling.constants;

import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;
import org.hl7.fhir.r4.model.Questionnaire;

public enum EnableWhenOperator implements ToDto<Questionnaire.QuestionnaireItemOperator> {
  EQUAL("="), GREATER_THAN(">"), LESS_THAN("<"), GREATER_OR_EQUAL(">="), LESS_OR_EQUAL("<=");

  private final String code;

  EnableWhenOperator(String code) {
    this.code = code;
  }

  @Override
  public Questionnaire.QuestionnaireItemOperator toDto() {
      return switch (this) {
          case EQUAL -> Questionnaire.QuestionnaireItemOperator.EQUAL;
          case LESS_THAN -> Questionnaire.QuestionnaireItemOperator.LESS_THAN;
          case LESS_OR_EQUAL -> Questionnaire.QuestionnaireItemOperator.LESS_OR_EQUAL;
          case GREATER_THAN -> Questionnaire.QuestionnaireItemOperator.GREATER_THAN;
          case GREATER_OR_EQUAL -> Questionnaire.QuestionnaireItemOperator.GREATER_OR_EQUAL;
          default ->
                  throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.QuestionnaireItemOperator %s", this));
      };

  }
}
