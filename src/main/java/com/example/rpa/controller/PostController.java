package com.example.rpa.controller;

import com.example.rpa.dto.FacebookPostRequest;
import com.example.rpa.dto.ThreadsPostRequest;
import com.example.rpa.service.FacebookService;
import com.example.rpa.service.ThreadsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PostController {

    @Autowired
    private FacebookService facebookService;

    @Autowired
    private ThreadsService threadsService;

    @Autowired
    private com.example.rpa.service.XService xService;

    @PostMapping("/facebook/post")
    public String postToFacebook(@RequestBody FacebookPostRequest request) {
        new Thread(() -> {
            facebookService.postLink(
                    request.getVideoUrl(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getHashtags());
        }).start();

        return "Facebook Post started! Check the browser window.";
    }

    @PostMapping("/threads/post")
    public String postToThreads(@RequestBody ThreadsPostRequest request) {
        new Thread(() -> {
            threadsService.postLink(
                    request.getVideoUrl(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getTopic());
        }).start();

        return "Threads Post started! Check the browser window.";
    }

    @PostMapping("/x/post")
    public String postToX(@RequestBody com.example.rpa.dto.XPostRequest request) {
        new Thread(() -> {
            xService.postLink(
                    request.getVideoUrl(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getHashtags());
        }).start();

        return "X Post started! Check the browser window.";
    }
}
