package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;

public interface ValueSetService {
    List<MeasurementTypeModel> getMeasurementTypes() throws ServiceException, AccessValidationException;
}
