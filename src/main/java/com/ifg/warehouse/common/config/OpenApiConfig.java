package com.ifg.warehouse.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Warehouse Inventory API",
        version = "0.0.1",
        description = "Manage items, variants, pricing, stock levels and orders. "
                + "Orders never oversell: insufficient stock returns HTTP 409."))
public class OpenApiConfig {
}
