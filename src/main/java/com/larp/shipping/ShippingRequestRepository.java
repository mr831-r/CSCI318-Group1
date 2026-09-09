package com.larp.shipping;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingRequestRepository extends JpaRepository<ShippingRequest, Long> {
}
