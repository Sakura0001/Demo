package org.example.controller;

import org.example.common.R;
import org.example.service.DelayQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class DelayQueueController {

    @Autowired
    DelayQueueService delayQueueService;

    @GetMapping ("")
    public R<String> file(@RequestParam Map<String,String> map) {
        delayQueueService.addToDelayQueue(map.get("new"));
        System.out.println(map.get("new"));
        return R.success(map.get("new"));
    }


}
