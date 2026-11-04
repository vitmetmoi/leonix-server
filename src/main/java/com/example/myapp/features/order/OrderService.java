package com.example.myapp.features.order;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.order.dto.OrderCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(OrderCreateRequest request) {
        log.info("Creating order for userId: {}, amount: {}", request.getUserId(), request.getTotalAmount());
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .totalAmount(request.getTotalAmount())
                .status("PENDING")
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created successfully with ID: {}, orderNumber: {}", saved.getId(), saved.getOrderNumber());
        return saved;
    }

    public Order getOrderById(Long id) {
        log.debug("Fetching order with ID: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {}", id);
                    return new ResourceNotFoundException("Order", "id", id);
                });
    }

    public List<Order> getOrdersByUser(Long userId) {
        log.debug("Fetching orders for userId: {}", userId);
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll();
    }
}
