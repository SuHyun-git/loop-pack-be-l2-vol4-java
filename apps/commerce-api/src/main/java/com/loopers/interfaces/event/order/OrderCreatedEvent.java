package com.loopers.interfaces.event.order;

import com.loopers.domain.order.OrderModel;

public record OrderCreatedEvent(
    Long orderId,
    Long memberId,
    Long totalPrice
) {
    public static OrderCreatedEvent from(OrderModel order) {
        return new OrderCreatedEvent(order.getId(), order.getMemberId(), order.getTotalPrice());
    }
}
