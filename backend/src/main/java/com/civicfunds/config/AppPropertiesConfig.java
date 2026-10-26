package com.civicfunds.config;

import com.civicfunds.ingestion.adapters.usaspending.UsaSpendingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UsaSpendingProperties.class)
public class AppPropertiesConfig {
}
