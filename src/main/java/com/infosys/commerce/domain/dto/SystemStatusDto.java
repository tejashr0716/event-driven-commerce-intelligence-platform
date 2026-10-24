package com.infosys.commerce.domain.dto;

import java.util.Map;

public record SystemStatusDto(
    String serviceName,
    String status,
    String environment,
    String catalogSource,
    long pendingOutboxCount,
    Map<String, String> adapters
) {}
