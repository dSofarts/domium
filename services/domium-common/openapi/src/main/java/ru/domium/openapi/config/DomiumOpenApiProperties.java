package ru.domium.openapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "domium.openapi")
public class DomiumOpenApiProperties {

    /**
     * Enable/disable the common OpenAPI auto-configuration.
     */
    private boolean enabled = true;

    private String title;

    private String description;

    private String version = "1.0.0";

    private String serverUrl;

    private String serverDescription;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerDescription() {
        return serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }
}
