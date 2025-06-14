package com.biyahewise.model;

import lombok.Data;

@Data
public class CommuteRequest {
    private String origin;
    private String destination;
    private String dateTime;
    private String mode;
    private String carDetails;
}
