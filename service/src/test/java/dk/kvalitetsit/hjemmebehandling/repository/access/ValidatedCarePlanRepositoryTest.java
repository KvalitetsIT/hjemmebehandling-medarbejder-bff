package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidatedCarePlanRepositoryTest {
    @Mock
    private AccessValidator<CarePlanModel> validator;

    @Mock
    private CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository;

    @InjectMocks
    private ValidatedCarePlanRepository subject;



}