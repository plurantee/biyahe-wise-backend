package com.biyahewise.model;

import lombok.Data;
import java.util.List;

@Data
public class CommuteResponse {
    private double estimatedLitersUsed;  // Only for drive
    private List<Option> options;
}
