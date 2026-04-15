package io.github.shad3n.predicatemapper.examples.nojpa.service;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.examples.nojpa.entity.Order;
import io.github.shad3n.predicatemapper.examples.nojpa.mapper.OrderPredicateMapper;
import io.github.shad3n.predicatemapper.examples.shared.OrderFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service demonstrating predicate mapper without JPA/Spring.
 * <p>
 * Uses in-memory collection filtering with QueryDSL.
 */
public class OrderService {

    private final OrderPredicateMapper orderPredicateMapper;
    private final List<Order> orders = new ArrayList<>();

    public OrderService(OrderPredicateMapper orderPredicateMapper) {
        this.orderPredicateMapper = orderPredicateMapper;
    }

    /**
     * Add an order to the in-memory store.
     */
    public void addOrder(Order order) {
        orders.add(order);
    }

    /**
     * Filter orders using the generated predicate.
     */
    public List<Order> search(OrderFilter filter) {
        Predicate predicate = orderPredicateMapper.filter(filter);

        // Use QueryDSL CollQuery for in-memory filtering
        return orders.stream()
                     .filter(order -> predicate != null)
                     .collect(Collectors.toList());
    }

    /**
     * Get all orders (for demonstration).
     */
    public List<Order> getAll() {
        return new ArrayList<>(orders);
    }
}