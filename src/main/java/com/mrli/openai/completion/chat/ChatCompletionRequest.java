package com.mrli.openai.completion.chat;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatCompletionRequest {

    // 指定使用的模型ID。目前仅支持gpt-3.5-turbo和gpt-3.5-turbo-0301。
    String model;

    // 要为其生成聊天完成的消息列表，采用<a href="https://platform.openai.com/docs/guides/chat/introduction">聊天格式</a>。
    List<ChatMessage> messages;


}
