package com.mrli.openai.config;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * description
 *
 * @author LiDaShuai
 * @version 1.0
 * @project chatgpt
 * @date: 2023/3/4 09:51
 * @copyright 2009–2022xxxxx all rights reserved.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "chatgpt")
public class ChatGptConfig {
    private String username;
    private String password;
    private String ip;
    private Integer port;
    private String token;
    private Long timeout;

    public static Proxy proxy = null;

    public static Authenticator authenticator = null;
}
