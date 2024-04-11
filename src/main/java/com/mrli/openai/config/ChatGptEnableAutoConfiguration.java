package com.mrli.openai.config;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Credentials;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * description
 *
 * @author LiDaShuai
 * @version 1.0
 * @project chatgpt
 * @date: 2023/3/4 10:49
 * @copyright 2009–2022xxxxx all rights reserved.
 */
@Configuration
@EnableConfigurationProperties(ChatGptConfig.class)
@Getter
@Setter
public class ChatGptEnableAutoConfiguration implements InitializingBean {
    private final ChatGptConfig config;

    public ChatGptEnableAutoConfiguration(ChatGptConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() {
        ChatGptConfig.authenticator = (route, response) -> {
            String credential = Credentials.basic(config.getUsername(), config.getPassword());
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        };
        ChatGptConfig.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getIp(), config.getPort()));
    }
}
