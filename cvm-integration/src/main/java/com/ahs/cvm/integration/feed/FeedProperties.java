package com.ahs.cvm.integration.feed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "cvm.feed")
public class FeedProperties {

    @NestedConfigurationProperty
    private FeedConfig nvd = new FeedConfig("https://services.nvd.nist.gov", "", true, 50, 30);

    @NestedConfigurationProperty
    private FeedConfig ghsa = new FeedConfig("https://api.github.com/graphql", "", false, 30, 60);

    @NestedConfigurationProperty
    private FeedConfig kev = new FeedConfig(
            "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json",
            "",
            true,
            60,
            60);

    @NestedConfigurationProperty
    private FeedConfig epss = new FeedConfig("https://api.first.org/data/v1/epss", "", true, 60, 60);

    public FeedConfig getNvd() { return nvd; }
    public void setNvd(FeedConfig nvd) { this.nvd = nvd; }
    public FeedConfig getGhsa() { return ghsa; }
    public void setGhsa(FeedConfig ghsa) { this.ghsa = ghsa; }
    public FeedConfig getKev() { return kev; }
    public void setKev(FeedConfig kev) { this.kev = kev; }
    public FeedConfig getEpss() { return epss; }
    public void setEpss(FeedConfig epss) { this.epss = epss; }

    public static class FeedConfig {
        private String baseUrl;
        private String apiKey;
        private boolean enabled;
        private int requestsPerWindow;
        private int windowSeconds;

        public FeedConfig() {}

        public FeedConfig(String baseUrl, String apiKey, boolean enabled,
                          int requestsPerWindow, int windowSeconds) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.enabled = enabled;
            this.requestsPerWindow = requestsPerWindow;
            this.windowSeconds = windowSeconds;
        }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRequestsPerWindow() { return requestsPerWindow; }
        public void setRequestsPerWindow(int requestsPerWindow) { this.requestsPerWindow = requestsPerWindow; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
    }
}
