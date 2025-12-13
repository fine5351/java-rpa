package com.example.rpa.post.controller;

import com.example.rpa.post.dto.FacebookPostRequest;
import com.example.rpa.post.dto.HoyolabPostRequest;
import com.example.rpa.post.dto.ThreadsPostRequest;
import com.example.rpa.post.dto.XPostRequest;
import com.example.rpa.post.service.FacebookService;
import com.example.rpa.post.service.HoyolabService;
import com.example.rpa.post.service.ThreadsService;
import com.example.rpa.post.service.XService;

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

    @Autowired
    private HoyolabService hoyolabService;

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

    @PostMapping("/hoyolab")
    public String postToHoyolab(@RequestBody HoyolabPostRequest request) {
        log.info("Received Hoyolab post request: {}", request);
        try {
            hoyolabService.postLink(request.getVideoLink(), request.getTitle(), request.getDescription(),
                    request.getCircleName(), request.getCategoryName(), request.getTopics());
            return "Posted to Hoyolab successfully!";
        } catch (Exception e) {
            log.error("Failed to post to Hoyolab", e);
            return "Failed to post to Hoyolab: " + e.getMessage();
        }
    }

    @PostMapping("/facebook")
    public String postToFacebook(@RequestBody FacebookPostRequest request) {
        log.info("Received Facebook post request: {}", request);
        try {
            facebookService.postLink(request.getVideoUrl(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getHashtags());
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
