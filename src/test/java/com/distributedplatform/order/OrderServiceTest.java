package com.distributedplatform.order;

import com.distributedplatform.exception.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void shouldCreateOrder() {

        BigDecimal totalAmount = new BigDecimal("50000.00");

        Order savedOrder = new Order(
                userId,
                totalAmount,
                OrderStatus.CREATED
        );

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        Order result = orderService.createOrder(userId, totalAmount);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(totalAmount, result.getTotalAmount());
        assertEquals(OrderStatus.CREATED, result.getStatus());

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldGetOrderById() {

        Order order = new Order(
                userId,
                new BigDecimal("50000.00"),
                OrderStatus.CREATED
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(new BigDecimal("50000.00"), result.getTotalAmount());

        verify(orderRepository).findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        verify(orderRepository).findById(orderId);
    }

    @Test
    void shouldGetAllOrders() {

        Order order1 = new Order(
                UUID.randomUUID(),
                new BigDecimal("1000.00"),
                OrderStatus.CREATED
        );

        Order order2 = new Order(
                UUID.randomUUID(),
                new BigDecimal("2000.00"),
                OrderStatus.CONFIRMED
        );

        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(orderRepository).findAll();
    }
}