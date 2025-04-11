package com.healthMini.chatDto;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
public class MessageDTO {
    private String content;
    private boolean isUserMessage;

    // Constructor
    public MessageDTO(String content, boolean isUserMessage) {
        this.content = content;
        this.isUserMessage = isUserMessage;
    }
}
