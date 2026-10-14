package com.shopsphere.service.impl;

import com.shopsphere.dto.common.PageResponse;
import com.shopsphere.dto.product.ProductRequestDTO;
import com.shopsphere.dto.product.ProductResponseDTO;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.specification.ProductSpecification;
import com.shopsphere.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDTO> getProducts(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        // Defensive validation for price range
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.shopsphere.exception.BadRequestException("Minimum price cannot be negative");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.shopsphere.exception.BadRequestException("Maximum price cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new com.shopsphere.exception.BadRequestException("Minimum price cannot be greater than maximum price");
        }

        // Enforce safe pagination parameters
        int pageNumber = Math.max(0, page);
        int pageSize = (size <= 0 || size > 100) ? 12 : size;

        // Build Sort safely
        String sortProperty = resolveSortProperty(sortBy);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortProperty);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Specification<Product> spec = ProductSpecification.filter(
                query, categoryId, minPrice, maxPrice, inStockOnly, true
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductResponseDTO> content = productPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast(),
                productPage.hasNext(),
                productPage.hasPrevious()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return mapToDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return mapToDTO(product);
    }

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        if (productRepository.existsBySku(requestDTO.getSku())) {
            throw new DuplicateResourceException("Product", "sku", requestDTO.getSku());
        }

        String slug = generateSlug(requestDTO.getSlug(), requestDTO.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product", "slug", slug);
        }

        Category category = categoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", requestDTO.getCategoryId()));

        Product product = new Product();
        product.setSku(requestDTO.getSku().trim().toUpperCase());
        product.setName(requestDTO.getName().trim());
        product.setSlug(slug);
        product.setDescription(requestDTO.getDescription());
        product.setCostPrice(requestDTO.getCostPrice());
        product.setSellingPrice(requestDTO.getSellingPrice());
        product.setStockQuantity(requestDTO.getStockQuantity());
        product.setLowStockThreshold(requestDTO.getLowStockThreshold());
        product.setImageUrl(requestDTO.getImageUrl());
        product.setCategory(category);
        product.setActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true);

        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Validate SKU uniqueness across other records
        productRepository.findBySku(requestDTO.getSku().trim().toUpperCase())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Product", "sku", requestDTO.getSku());
                    }
                });

        String slug = generateSlug(requestDTO.getSlug(), requestDTO.getName());
        productRepository.findBySlug(slug)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Product", "slug", slug);
                    }
                });

        Category category = categoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", requestDTO.getCategoryId()));

        product.setSku(requestDTO.getSku().trim().toUpperCase());
        product.setName(requestDTO.getName().trim());
        product.setSlug(slug);
        product.setDescription(requestDTO.getDescription());
        product.setCostPrice(requestDTO.getCostPrice());
        product.setSellingPrice(requestDTO.getSellingPrice());
        product.setStockQuantity(requestDTO.getStockQuantity());
        product.setLowStockThreshold(requestDTO.getLowStockThreshold());
        product.setImageUrl(requestDTO.getImageUrl());
        product.setCategory(category);
        if (requestDTO.getIsActive() != null) {
            product.setActive(requestDTO.getIsActive());
        }

        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Soft deactivation preserves relational integrity with order history
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null) {
            return "createdAt";
        }
        return switch (sortBy.toLowerCase()) {
            case "price", "sellingprice" -> "sellingPrice";
            case "name" -> "name";
            case "stock", "stockquantity" -> "stockQuantity";
            case "sku" -> "sku";
            default -> "createdAt";
        };
    }

    private String generateSlug(String customSlug, String name) {
        if (customSlug != null && !customSlug.trim().isEmpty()) {
            return customSlug.trim().toLowerCase()
                    .replaceAll("[^a-z0-9-]+", "-")
                    .replaceAll("^-|-$", "");
        }
        return name.trim().toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-|-$", "");
    }

    private ProductResponseDTO mapToDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setDescription(product.getDescription());
        dto.setCostPrice(product.getCostPrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setLowStockThreshold(product.getLowStockThreshold());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.isActive());
        dto.setInStock(product.isInStock());
        dto.setLowStock(product.isLowStock());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        return dto;
    }
}
