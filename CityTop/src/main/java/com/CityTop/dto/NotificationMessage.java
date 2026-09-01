package com.CityTop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationMessage {

    private String type;
    private String content;
    private Long timestamp;
}
