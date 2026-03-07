package com.infusi.waflow.starter;

import com.infusi.waflow.config.AsyncConfig;
import com.infusi.waflow.config.RedisConfig;
import com.infusi.waflow.config.WhatsAppConfig;
import com.infusi.waflow.flow.FlowEngine;
import com.infusi.waflow.flow.FlowRegistry;
import com.infusi.waflow.menu.MenuRegistry;
import com.infusi.waflow.message.MessageConverter;
import com.infusi.waflow.message.MessageSender;
import com.infusi.waflow.session.SessionManager;
import com.infusi.waflow.webhook.MessageDispatcher;
import com.infusi.waflow.webhook.WebhookController;
import com.infusi.waflow.webhook.WebhookVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = "com.infusi.waflow")
@Import({WhatsAppConfig.class, RedisConfig.class, AsyncConfig.class})
public class WaflowAutoConfiguration {

    public WaflowAutoConfiguration() {
        log.info("WaFlow framework auto-configuration loaded");
    }
}
