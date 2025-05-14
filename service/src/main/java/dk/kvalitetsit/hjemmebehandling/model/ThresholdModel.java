package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;

public record ThresholdModel (
        String questionnaireItemLinkId,
        ThresholdType type,
        Double valueQuantityLow,
        Double valueQuantityHigh,
        Boolean valueBoolean,
        String valueOption
) {

}
