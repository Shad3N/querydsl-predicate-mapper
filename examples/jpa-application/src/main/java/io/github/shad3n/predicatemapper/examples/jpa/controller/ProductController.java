package io.github.shad3n.predicatemapper.examples.jpa.controller;

import io.github.shad3n.predicatemapper.examples.jpa.entity.Product;
import io.github.shad3n.predicatemapper.examples.jpa.service.ProductService;
import io.github.shad3n.predicatemapper.examples.shared.ProductFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller demonstrating receiver service.
 * <p>
 * Accepts ProductFilter as query parameters via @ModelAttribute.
 * The filter is converted to QueryDSL predicate by the generated mapper.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Search products - filter params become query string.
     * Example: GET /api/products?name=widget&status=AVAILABLE&activeOnly=true
     */
    @GetMapping
    public List<Product> search(@ModelAttribute ProductFilter filter) {
        return productService.search(filter);
    }

    /**
     * Search with pagination.
     * Example: GET /api/products/search?name=widget&page=0&size=20
     */
    @GetMapping("/search")
    public Page<Product> searchPaged(@ModelAttribute ProductFilter filter, Pageable pageable) {
        return productService.searchPaged(filter, pageable);
    }
}