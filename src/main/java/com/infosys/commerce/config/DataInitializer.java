package com.infosys.commerce.config;

import com.infosys.commerce.service.AuthService;
import com.infosys.commerce.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AuthService authService;
    private final CatalogService catalogService;

    public DataInitializer(AuthService authService, CatalogService catalogService) {
        this.authService = authService;
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Enterprise Commerce Data...");
        authService.seedDefaultUsers();
        catalogService.syncWithFakeStore();
        log.info("Data initialization complete. Ready for enterprise transactions.");
    }
}
