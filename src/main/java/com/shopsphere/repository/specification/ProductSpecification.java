package com.shopsphere.repository.specification;

import com.shopsphere.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filter(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            Boolean activeOnly
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter active products
            if (activeOnly == null || activeOnly) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            // Keyword query search (name, description, sku)
            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate skuLike = cb.like(cb.lower(root.get("sku")), pattern);
                predicates.add(cb.or(nameLike, descLike, skuLike));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Price range filter
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }

            // In-stock only filter
            if (inStockOnly != null && inStockOnly) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
