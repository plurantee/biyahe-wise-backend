package com.biyahewise.controller;

import com.biyahewise.model.CommuteRequest;
import com.biyahewise.model.CommuteResponse;
import com.biyahewise.service.EstimationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estimate")
@CrossOrigin(origins = "*")
public class EstimateController {

    @Autowired
    private EstimationService estimationService;

    @PostMapping
    public CommuteResponse estimate(@RequestBody CommuteRequest request) {
        return estimationService.estimate(request);
    }
}
