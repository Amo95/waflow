package com.infusi.waflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for WhatsApp Cloud API connection.
 */
@Data
@ConfigurationProperties(prefix = "waflow.whatsapp")
public class WhatsAppProperties {

    /** WhatsApp Business phone number ID. */
    private String phoneNumberId;

    /** Permanent or temporary access token for the WhatsApp Cloud API. */
    private String accessToken;

    /** Token used to verify webhook registration. */
    private String verifyToken;

    /** Path the webhook controller listens on. */
    private String webhookPath = "/webhook";

    /** WhatsApp Cloud API version. */
    private String apiVersion = "v22.0";

    /** Graph API base URL. */
    private String baseUrl = "https://graph.facebook.com";

    /** Computed full API base URL. */
    public String getApiBaseUrl() {
        return baseUrl + "/" + apiVersion;
    }
}
