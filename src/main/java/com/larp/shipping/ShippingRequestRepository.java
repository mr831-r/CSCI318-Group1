package com.larp.shipping;

import org.springframework.data.jpa.repository.JpaRepository;

// TODO: Remove this, use DatabaseService
public interface ShippingRequestRepository extends JpaRepository<ShippingRequest, Long> {
}
