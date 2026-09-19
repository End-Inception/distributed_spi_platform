package com.distributedplatform.order;

import com.distributedplatform.exception.OrderNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(
            UUID userId,
            BigDecimal totalAmount) {

        Order order = new Order(
                userId,
                totalAmount,
                OrderStatus.CREATED
        );

        return orderRepository.save(order);
    }

    public Order getOrderById(UUID id) {

        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found"));
    }

    public List<Order> getAllOrders() {

        return orderRepository.findAll();
    }
}