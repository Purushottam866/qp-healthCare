package com.healthMini.chatDto;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class SessionMessages {
    private int sessionId;
    private String title;
    private List<MessageDTO> messages;

   
}
