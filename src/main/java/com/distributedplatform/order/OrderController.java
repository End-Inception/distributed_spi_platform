package com.distributedplatform.order;

import com.distributedplatform.order.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        return orderService.createOrder(
                request.getUserId(),
                request.getTotalAmount()
        );
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable UUID id) {
        return orderService.getOrderById(id);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }
}