package com.example.rpa.video.controller;

import com.example.rpa.video.dto.BilibiliVideoUploadRequest;
import com.example.rpa.video.dto.MultiPlatformVideoUploadRequest;
import com.example.rpa.video.dto.MultiPlatformFolderUploadRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import com.example.rpa.video.dto.TikTokVideoUploadRequest;
import com.example.rpa.video.dto.XiaohongshuVideoUploadRequest;
import com.example.rpa.video.dto.YoutubeVideoUploadRequest;
import com.example.rpa.video.service.BilibiliService;
import com.example.rpa.video.service.TikTokService;
import com.example.rpa.video.service.XiaohongshuService;
import com.example.rpa.video.service.YouTubeService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class VideoController {

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private TikTokService tikTokService;

    @Autowired
    private XiaohongshuService xiaohongshuService;

    @Autowired
    private BilibiliService bilibiliService;

    @PostMapping("/youtube/upload")
    public String uploadToYoutube(@RequestBody YoutubeVideoUploadRequest request) {
        log.info("Received YouTube upload request: {}", request);
        new Thread(() -> {
            try {
                youTubeService.uploadVideo(
                        request.getFilePath(),
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
                        request.getPlaylist(),
                        request.getVisibility(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to YouTube", e);
            }
        }).start();

        return "Upload started! Check the browser window.";
    }

    @PostMapping("/tiktok/upload")
    public String uploadToTikTok(@RequestBody TikTokVideoUploadRequest request) {
        log.info("Received TikTok upload request: {}", request);
        new Thread(() -> {
            try {
                tikTokService.uploadVideo(
                        request.getFilePath(),
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
                        request.getVisibility(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to TikTok", e);
            }
        }).start();

        return "TikTok Upload started! Check the browser window.";
    }

    @PostMapping("/xiaohongshu/upload")
    public String uploadToXiaohongshu(@RequestBody XiaohongshuVideoUploadRequest request) {
        log.info("Received Xiaohongshu upload request: {}", request);
        new Thread(() -> {
            try {
                xiaohongshuService.uploadVideo(
                        request.getFilePath(),
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to Xiaohongshu", e);
            }
        }).start();

        return "Xiaohongshu Upload started! Check the browser window.";
    }

    @PostMapping("/bilibili/upload")
    public String uploadToBilibili(@RequestBody BilibiliVideoUploadRequest request) {
        log.info("Received Bilibili upload request: {}", request);
        new Thread(() -> {
            try {
                bilibiliService.uploadVideo(
                        request.getFilePath(),
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to Bilibili", e);
            }
        }).start();

        return "Bilibili Upload started! Check the browser window.";
    }

    @PostMapping("/multi/upload")
    public String uploadMultiPlatform(@RequestBody MultiPlatformVideoUploadRequest request) {
        log.info("Received Multi-Platform upload request: {}", request);
        new Thread(() -> {
            processMultiPlatformUpload(
                    request.getFilePath(),
                    request.getDescription(),
                    request.getPlaylist(),
                    request.getHashtags());
        }).start();

        return "Multi-Platform Upload started! Check the console for progress.";
    }

    @PostMapping("/multi/upload/folder")
    public String uploadMultiPlatformFromFolder(@RequestBody MultiPlatformFolderUploadRequest request) {
        log.info("Received Multi-Platform folder upload request: {}", request);
        new Thread(() -> {
            File folder = new File(request.getFolderPath());
            if (!folder.exists() || !folder.isDirectory()) {
                log.error("Invalid folder path: {}", request.getFolderPath());
                return;
            }

            File[] files = folder.listFiles();
            if (files != null) {
                Arrays.sort(files, NATURAL_SORT_ORDER);

                for (File file : files) {
                    if (file.isFile()) {
                        log.info("Processing file in batch: {}", file.getName());
                        processMultiPlatformUpload(
                                file.getAbsolutePath(),
                                request.getDescription(),
                                request.getPlaylist(),
                                request.getHashtags());
                    }
                }
            }
        }).start();

        return "Multi-Platform Folder Upload started! Check the console for progress.";
    }

    private void processMultiPlatformUpload(String filePath, String description, String playlist,
            List<String> hashtags) {
        try {
            String title = getFileNameWithoutExtension(filePath);

            // 1. YouTube
            log.info("Starting YouTube upload for {}...", title);
            boolean ytSuccess = youTubeService.uploadVideo(
                    filePath,
                    title,
                    description,
                    playlist,
                    "PUBLIC", // Forced Public
                    hashtags,
                    false); // Close on success, keeping debugging simple for now or strictly follow
                            // sequential?
                            // "Manual verification" usually implies needed visibility, but "RPA" implies
                            // automation.
                            // If fail, we stop.

            if (!ytSuccess) {
                log.error("YouTube upload failed for {}. Stopping sequence.", title);
                return;
            }

            // 2. Bilibili
            log.info("Starting Bilibili upload for {}...", title);
            boolean biliSuccess = bilibiliService.uploadVideo(
                    filePath,
                    title,
                    description,
                    hashtags,
                    false);

            if (!biliSuccess) {
                log.error("Bilibili upload failed for {}. Stopping sequence.", title);
                return;
            }

            // 3. Xiaohongshu
            log.info("Starting Xiaohongshu upload for {}...", title);
            boolean xhsSuccess = xiaohongshuService.uploadVideo(
                    filePath,
                    title,
                    description,
                    hashtags,
                    false);

            if (!xhsSuccess) {
                log.error("Xiaohongshu upload failed for {}. Stopping sequence.", title);
                return;
            }

            // 4. TikTok
            log.info("Starting TikTok upload for {}...", title);
            boolean tiktokSuccess = tikTokService.uploadVideo(
                    filePath,
                    title,
                    description,
                    "PUBLIC", // Forced Public
                    hashtags,
                    false);

            if (!tiktokSuccess) {
                log.error("TikTok upload failed for {}.", title);
                return;
            }

            log.info("All platforms uploaded successfully for {}!", title);

        } catch (Exception e) {
            log.error("Error during multi-platform upload sequence for " + filePath, e);
        }
    }

    private String getFileNameWithoutExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        java.io.File file = new java.io.File(filePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private static final Comparator<File> NATURAL_SORT_ORDER = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            String s1 = f1.getName();
            String s2 = f2.getName();
            int n1 = s1.length();
            int n2 = s2.length();
            int i1 = 0;
            int i2 = 0;
            while (i1 < n1 && i2 < n2) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    long num1 = 0;
                    while (i1 < n1 && Character.isDigit(s1.charAt(i1))) {
                        num1 = num1 * 10 + (s1.charAt(i1) - '0');
                        i1++;
                    }
                    long num2 = 0;
                    while (i2 < n2 && Character.isDigit(s2.charAt(i2))) {
                        num2 = num2 * 10 + (s2.charAt(i2) - '0');
                        i2++;
                    }
                    if (num1 != num2) {
                        return Long.compare(num1, num2);
                    }
                    continue;
                }
                if (c1 != c2) {
                    return c1 - c2;
                }
                i1++;
                i2++;
            }
            return n1 - n2;
        }
    };

}
