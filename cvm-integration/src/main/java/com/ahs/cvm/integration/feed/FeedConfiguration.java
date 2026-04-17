package com.ahs.cvm.integration.feed;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Aktiviert {@link FeedProperties} im Context.
 */
@Configuration
@EnableConfigurationProperties(FeedProperties.class)
public class FeedConfiguration {}
