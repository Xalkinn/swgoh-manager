package fr.xalkinn.swgohmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swgoh")
public class SwgohProperties {

    private final Comlink comlink = new Comlink();

    public Comlink getComlink() {
        return comlink;
    }

    public static class Comlink {
        private String baseUrl = "http://localhost:3000";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String endpoint(String chemin) {
            String base = baseUrl == null ? "" : baseUrl.trim();

            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }

            if (chemin == null || chemin.isBlank()) {
                return base;
            }

            return base + (chemin.startsWith("/") ? chemin : "/" + chemin);
        }
    }
}
