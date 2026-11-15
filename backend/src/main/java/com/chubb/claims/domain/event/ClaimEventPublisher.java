package com.chubb.claims.domain.event;

/** Port for handing claim events to the outside world. Must be called inside the transaction that changed the claim. */
public interface ClaimEventPublisher {

    void publish(ClaimStatusChanged event);
}
