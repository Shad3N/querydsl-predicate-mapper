package io.github.shad3n.predicatemapper.examples.sender.client;

import io.github.shad3n.predicatemapper.examples.shared.ProductFilter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * HTTP client interface using Spring 6 HttpExchange.
 * <p>
 * Communicates with the JPA application receiver service.
 * ProductFilter fields become query parameters automatically.
 */
@HttpExchange("/api/products")
public interface ProductClient {

    /**
     * Search products - filter fields become query params.
     * Example: GET /api/products?name=widget&status=AVAILABLE&activeOnly=true
     */
    @GetExchange
    List<Map<String, Object>> search(@RequestParam Map<String, String> params);

    /**
     * Alternative: accept ProductFilter directly.
     * Spring will serialize fields to query parameters.
     */
    @GetExchange
    List<Map<String, Object>> searchByFilter(ProductFilter filter);
}