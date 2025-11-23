package com.example.rpa.controller;

import com.example.rpa.dto.FacebookPostRequest;
import com.example.rpa.dto.ThreadsPostRequest;
import com.example.rpa.dto.XPostRequest;
import com.example.rpa.service.FacebookService;
import com.example.rpa.service.ThreadsService;
import com.example.rpa.service.XService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private XService xService;

    @Autowired
    private FacebookService facebookService;

    @Autowired
    private ThreadsService threadsService;

    @PostMapping("/x")
    public String postToX(@RequestBody XPostRequest request) {
        log.info("Received X post request: {}", request);
        try {
            xService.postLink(request.getContent(), request.getHashtags());
            return "Posted to X successfully!";
        } catch (Exception e) {
            log.error("Failed to post to X", e);
            return "Failed to post to X: " + e.getMessage();
        }
    }

    @PostMapping("/facebook")
    public String postToFacebook(@RequestBody FacebookPostRequest request) {
        log.info("Received Facebook post request: {}", request);
        try {
            facebookService.postLink(request.getContent(), request.getHashtags());
            return "Posted to Facebook successfully!";
        } catch (Exception e) {
            log.error("Failed to post to Facebook", e);
            return "Failed to post to Facebook: " + e.getMessage();
        }
    }

    @PostMapping("/threads")
    public String postToThreads(@RequestBody ThreadsPostRequest request) {
        log.info("Received Threads post request: {}", request);
        try {
            threadsService.postLink(request.getContent(), request.getHashtags());
            return "Posted to Threads successfully!";
        } catch (Exception e) {
            log.error("Failed to post to Threads", e);
            return "Failed to post to Threads: " + e.getMessage();
        }
    }
}
