package com.biyahewise.model;

import lombok.Data;
import java.util.List;

@Data
public class Option {
    private String optionTitle;
    private double estimatedTimeMinutes;
    private double estimatedCostPHP;
    private List<CommuteStep> steps;
}
