package io.github.shad3n.predicatemapper.examples.nojpa;

import io.github.shad3n.predicatemapper.examples.nojpa.entity.Customer;
import io.github.shad3n.predicatemapper.examples.nojpa.entity.Order;
import io.github.shad3n.predicatemapper.examples.nojpa.mapper.OrderPredicateMapper;
import io.github.shad3n.predicatemapper.examples.nojpa.mapper.OrderPredicateMapperImpl;
import io.github.shad3n.predicatemapper.examples.nojpa.service.OrderService;
import io.github.shad3n.predicatemapper.examples.shared.OrderFilter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Demo application showing predicate mapper usage without Spring/JPA.
 * <p>
 * Run this to see the generated predicate mapper in action.
 */
public class NoJpaApplication {

    public static void main(String[] args) {
        System.out.println("=== QueryDSL Predicate Mapper - No JPA Demo ===\n");

        // Create the generated implementation directly (no Spring DI)
        OrderPredicateMapper orderPredicateMapper = new OrderPredicateMapperImpl();
        OrderService orderService = new OrderService(orderPredicateMapper);

        // Seed some test data
        seedOrders(orderService);

        // Demo 1: Search by order number
        System.out.println("--- Search by Order Number ---");
        OrderFilter byNumber = new OrderFilter();
        byNumber.setOrderNumber("ORD-001");
        demonstrateFilter(orderPredicateMapper, byNumber);

        // Demo 2: Search by customer email pattern
        System.out.println("\n--- Search by Customer Email ---");
        OrderFilter byEmail = new OrderFilter();
        byEmail.setCustomerEmail("alice");
        demonstrateFilter(orderPredicateMapper, byEmail);

        // Demo 3: Search by amount range
        System.out.println("\n--- Search by Amount Range (100-500) ---");
        OrderFilter byAmount = new OrderFilter();
        byAmount.setMinAmount(new BigDecimal("100"));
        byAmount.setMaxAmount(new BigDecimal("500"));
        demonstrateFilter(orderPredicateMapper, byAmount);

        // Demo 4: Search by status
        System.out.println("\n--- Search by Status ---");
        OrderFilter byStatus = new OrderFilter();
        byStatus.setStatuses(List.of("COMPLETED", "SHIPPED"));
        demonstrateFilter(orderPredicateMapper, byStatus);

        // Demo 5: Search non-cancelled orders
        System.out.println("\n--- Search Non-Cancelled Orders ---");
        OrderFilter notCancelled = new OrderFilter();
        notCancelled.setNotCancelled(true);
        demonstrateFilter(orderPredicateMapper, notCancelled);

        // Demo 6: Complex filter
        System.out.println("\n--- Complex Filter ---");
        OrderFilter complex = new OrderFilter();
        complex.setCustomerEmail("example.com");
        complex.setMinAmount(new BigDecimal("50"));
        complex.setStatuses(List.of("PENDING", "PROCESSING"));
        complex.setNotCancelled(true);
        demonstrateFilter(orderPredicateMapper, complex);

        System.out.println("\n=== Demo Complete ===");
    }

    private static void seedOrders(OrderService service) {
        Customer alice = new Customer();
        alice.setEmail("alice@example.com");
        alice.setName("Alice Smith");

        Customer bob = new Customer();
        bob.setEmail("bob@example.com");
        bob.setName("Bob Jones");

        Customer charlie = new Customer();
        charlie.setEmail("charlie@other.org");
        charlie.setName("Charlie Brown");

        // Order 1
        Order order1 = new Order();
        order1.setOrderNumber("ORD-001");
        order1.setCustomer(alice);
        order1.setStatus("COMPLETED");
        order1.setTotalAmount(new BigDecimal("150.00"));
        order1.setOrderDate(LocalDate.of(2024, 1, 15));
        service.addOrder(order1);

        // Order 2
        Order order2 = new Order();
        order2.setOrderNumber("ORD-002");
        order2.setCustomer(bob);
        order2.setStatus("PENDING");
        order2.setTotalAmount(new BigDecimal("75.50"));
        order2.setOrderDate(LocalDate.of(2024, 1, 20));
        service.addOrder(order2);

        // Order 3
        Order order3 = new Order();
        order3.setOrderNumber("ORD-003");
        order3.setCustomer(alice);
        order3.setStatus("SHIPPED");
        order3.setTotalAmount(new BigDecimal("320.00"));
        order3.setOrderDate(LocalDate.of(2024, 1, 22));
        service.addOrder(order3);

        // Order 4 (cancelled)
        Order order4 = new Order();
        order4.setOrderNumber("ORD-004");
        order4.setCustomer(charlie);
        order4.setStatus("CANCELLED");
        order4.setTotalAmount(new BigDecimal("50.00"));
        order4.setOrderDate(LocalDate.of(2024, 1, 25));
        order4.setCancelled(LocalDate.of(2024, 1, 26));
        service.addOrder(order4);

        // Order 5
        Order order5 = new Order();
        order5.setOrderNumber("ORD-005");
        order5.setCustomer(bob);
        order5.setStatus("PROCESSING");
        order5.setTotalAmount(new BigDecimal("450.00"));
        order5.setOrderDate(LocalDate.of(2024, 2, 1));
        service.addOrder(order5);

        System.out.println("Seeded 5 orders\n");
    }

    private static void demonstrateFilter(OrderPredicateMapper orderPredicateMapper, OrderFilter filter) {
        System.out.println("Filter: " + describeFilter(filter));

        // Generate predicate
        var predicate = orderPredicateMapper.filter(filter);
        System.out.println("Generated Predicate: " + predicate);
    }

    private static String describeFilter(OrderFilter filter) {
        StringBuilder sb = new StringBuilder("{");

        if (filter.getOrderNumber() != null) {
            sb.append("orderNumber='").append(filter.getOrderNumber()).append("', ");
        }
        if (filter.getCustomerEmail() != null) {
            sb.append("customerEmail like '").append(filter.getCustomerEmail()).append("', ");
        }
        if (filter.getStatuses() != null) {
            sb.append("statuses in ").append(filter.getStatuses()).append(", ");
        }
        if (filter.getMinAmount() != null) {
            sb.append("minAmount=").append(filter.getMinAmount()).append(", ");
        }
        if (filter.getMaxAmount() != null) {
            sb.append("maxAmount=").append(filter.getMaxAmount()).append(", ");
        }
        if (filter.getOrderedAfter() != null) {
            sb.append("orderedAfter=").append(filter.getOrderedAfter()).append(", ");
        }
        if (filter.getOrderedBefore() != null) {
            sb.append("orderedBefore=").append(filter.getOrderedBefore()).append(", ");
        }
        if (filter.getNotCancelled() != null) {
            sb.append("notCancelled=").append(filter.getNotCancelled()).append(", ");
        }

        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }
}