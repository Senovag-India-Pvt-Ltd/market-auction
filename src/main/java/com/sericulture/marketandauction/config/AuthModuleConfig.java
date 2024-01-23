package com.sericulture.marketandauction.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.sericulture.authentication"})
public class AuthModuleConfig {
}