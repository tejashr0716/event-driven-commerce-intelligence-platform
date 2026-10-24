package com.infosys.commerce.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.infosys.commerce.domain.dto.ProductResponse;
import com.infosys.commerce.domain.enums.DataSourceMode;
import com.infosys.commerce.domain.model.Inventory;
import com.infosys.commerce.domain.model.Product;
import com.infosys.commerce.domain.repository.InventoryRepository;
import com.infosys.commerce.domain.repository.ProductRepository;
import com.infosys.commerce.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final RestClient restClient;

    @Value("${app.catalog.fakestore-url:https://fakestoreapi.com/products}")
    private String fakeStoreUrl;

    public CatalogService(ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          InventoryService inventoryService,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.searchProducts(category, query, pageable);
        List<ProductResponse> responseList = products.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responseList, pageable, products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Product getEntityBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findAllCategories();
    }

    @Transactional
    public Map<String, Object> syncWithFakeStore() {
        log.info("Initiating FakeStore API catalog sync from {}", fakeStoreUrl);
        DataSourceMode mode = DataSourceMode.LIVE_EXTERNAL_DATA;
        int syncedCount = 0;

        try {
            List<FakeStoreItem> externalItems = restClient.get()
                    .uri(fakeStoreUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FakeStoreItem>>() {});

            if (externalItems != null && !externalItems.isEmpty()) {
                for (FakeStoreItem item : externalItems) {
                    String sku = "SKU-FS-" + String.format("%03d", item.id());
                    BigDecimal price = BigDecimal.valueOf(item.price()).setScale(2, RoundingMode.HALF_UP);
                    
                    Product product = productRepository.findBySku(sku).orElse(new Product());
                    product.setSku(sku);
                    product.setTitle(item.title());
                    product.setDescription(item.description());
                    product.setCategory(item.category());
                    product.setPrice(price);
                    product.setCurrency("USD");
                    product.setImageUrl(item.image());
                    product.setSourceOrigin(DataSourceMode.LIVE_EXTERNAL_DATA);
                    productRepository.save(product);

                    // Seed default stock if inventory missing
                    if (!inventoryRepository.existsById(sku)) {
                        inventoryService.initializeStock(sku, 50);
                    }
                    syncedCount++;
                }
                log.info("Successfully synced {} items from live FakeStore API", syncedCount);
            }
        } catch (Exception e) {
            log.warn("Live FakeStore API sync encountered issue ({}); retaining/updating with cached catalog", e.getMessage());
            mode = DataSourceMode.CACHED_EXTERNAL_DATA;
            syncedCount = seedFallbackCatalog();
        }

        // Ensure concurrency benchmark item exists
        seedHotTestItem();

        auditService.record("CATALOG_SYNC", UUID.randomUUID().toString(), "system",
                "Synced " + syncedCount + " items in mode " + mode, "127.0.0.1");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("itemsSynced", syncedCount);
        result.put("dataSourceMode", mode);
        result.put("timestamp", java.time.Instant.now().toString());
        return result;
    }

    @Transactional
    public void seedHotTestItem() {
        String hotSku = "SKU-HOT-ITEM-99";
        if (!productRepository.existsBySku(hotSku)) {
            Product hotProduct = new Product(
                    hotSku,
                    "Enterprise High-Demand UltraBook Pro 16",
                    "High-concurrency stress test asset configured with limited inventory pool to verify zero overselling under race conditions.",
                    "electronics",
                    new BigDecimal("1299.99"),
                    "USD",
                    "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500",
                    DataSourceMode.SEEDED_DEMO_DATA
            );
            productRepository.save(hotProduct);
        }
        // Initial stock = 5 for test assertions
        inventoryService.initializeStock(hotSku, 5);
        log.info("Ensured benchmark SKU={} is initialized with 5 units.", hotSku);
    }

    @Transactional
    public int seedFallbackCatalog() {
        List<Product> defaults = List.of(
                new Product("SKU-FS-001", "Fjallraven - Foldsack No. 1 Backpack", "Your perfect pack for everyday use and walks in the forest. Stash your laptop up to 15 inches.", "men's clothing", new BigDecimal("109.95"), "USD", "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-002", "Mens Casual Premium Slim Fit T-Shirts", "Slim-fitting style, contrast raglan long sleeve, three-button henley placket, light weight & soft fabric.", "men's clothing", new BigDecimal("22.30"), "USD", "https://fakestoreapi.com/img/71-3HjGNDUL._AC_SY879._SX._UX._SY._UT_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-003", "Mens Cotton Jacket", "Great outerwear jackets for Spring/Autumn/Winter, suitable for many occasions.", "men's clothing", new BigDecimal("55.99"), "USD", "https://fakestoreapi.com/img/71li-ujtlUL._AC_UX679_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-004", "Solid Gold Petite Micropave", "Satisfaction Guaranteed. Return or exchange any order within 30 days. Designed and sold by Hafeez Center in the United States.", "jewelery", new BigDecimal("168.00"), "USD", "https://fakestoreapi.com/img/61sbMiUnoGL._AC_UL640_QL65_ML3_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-005", "WD 2TB Elements Portable External Hard Drive", "USB 3.0 and USB 2.0 Compatibility Fast data transfers Improve PC Performance High Capacity.", "electronics", new BigDecimal("64.00"), "USD", "https://fakestoreapi.com/img/61IBBVJvSDL._AC_SY879_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-006", "SanDisk SSD PLUS 1TB Internal SSD", "Easy upgrade for faster boot up, shutdown, application load and response. Boosts burst write performance.", "electronics", new BigDecimal("109.00"), "USD", "https://fakestoreapi.com/img/61U7T1koQqL._AC_SX679_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-007", "Silicon Power 256GB SSD 3D NAND", "3D NAND flash are applied to deliver high transfer speeds. Remarkable transfer speeds.", "electronics", new BigDecimal("109.00"), "USD", "https://fakestoreapi.com/img/71kWymZ+c+L._AC_SX679_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-008", "WD 4TB Gaming Drive Works with Playstation 4", "Expand your PS4 gaming experience, Play anywhere Fast and easy, setup Sleek design with high capacity.", "electronics", new BigDecimal("114.00"), "USD", "https://fakestoreapi.com/img/61mtL65D4cL._AC_SX679_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-009", "Acer SB220Q bi 21.5 inches Full HD IPS Ultra-Thin", "21. 5 inches Full HD widescreen IPS display and Radeon free Sync technology. No compatibility for VESA Mount.", "electronics", new BigDecimal("599.00"), "USD", "https://fakestoreapi.com/img/81QpkIctqPL._AC_SX679_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA),
                new Product("SKU-FS-010", "Samsung 49-Inch CHG90 144Hz Curved Gaming Monitor", "49 INCH SUPER ULTRAWIDE 32:9 CURVED GAMING MONITOR with dual 27 inch screen side by side.", "electronics", new BigDecimal("999.99"), "USD", "https://fakestoreapi.com/img/81Zt42ioCgL._AC_SX679_.jpg", DataSourceMode.CACHED_EXTERNAL_DATA)
        );

        int count = 0;
        for (Product p : defaults) {
            if (!productRepository.existsBySku(p.getSku())) {
                productRepository.save(p);
                inventoryService.initializeStock(p.getSku(), 30);
                count++;
            }
        }
        return count;
    }

    private ProductResponse toResponse(Product product) {
        int available = inventoryRepository.findById(product.getSku())
                .map(Inventory::getAvailableStock)
                .orElse(0);

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getTitle(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getCurrency(),
                product.getImageUrl(),
                product.getSourceOrigin(),
                available
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FakeStoreItem(
            Long id,
            String title,
            double price,
            String description,
            String category,
            String image
    ) {}
}
