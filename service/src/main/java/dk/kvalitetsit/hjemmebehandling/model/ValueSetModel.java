package dk.kvalitetsit.hjemmebehandling.model;

import java.util.List;

public record ValueSetModel(List<MeasurementTypeModel> measurementTypes) {
    public ValueSetModel {
        measurementTypes = (measurementTypes != null) ? measurementTypes : List.of();
    }
}
