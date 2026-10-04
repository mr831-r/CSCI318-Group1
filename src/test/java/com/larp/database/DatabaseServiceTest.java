package com.larp.database;

import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingRequestRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseServiceTest {

    @Test
    void shippingRequestsDelegatesToRepository() {
        ShippingRequestRepository shippingRepo = mock(ShippingRequestRepository.class);
        DatabaseService service = new DatabaseService(shippingRepo);

        ShippingRequest r1 = new ShippingRequest();
        when(shippingRepo.findAll()).thenReturn(List.of(r1));

        assertEquals(1, service.getShippingRequests().size());
    }
}