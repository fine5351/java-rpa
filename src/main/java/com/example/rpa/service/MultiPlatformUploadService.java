package com.example.rpa.service;

import com.example.rpa.dto.BatchUploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

@Slf4j
@Service
public class MultiPlatformUploadService {

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private BilibiliService bilibiliService;

    @Autowired
    private XiaohongshuService xiaohongshuService;

    @Autowired
    private TikTokService tikTokService;

    public void uploadFromFolder(BatchUploadRequest request) {
        File folder = new File(request.getFolderPath());
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("Invalid folder path: {}", request.getFolderPath());
            return;
        }

        File[] files = folder.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") || lowerName.endsWith(".mkv");
        });

        if (files == null || files.length == 0) {
            log.warn("No video files found in folder: {}", request.getFolderPath());
            return;
        }

        // Sort files by last modified time (Oldest to Newest)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for (File file : files) {
            String title = file.getName().substring(0, file.getName().lastIndexOf('.'));
            log.info("Starting batch upload for file: {}", file.getName());

            try {
                // 1. YouTube
                log.info("Uploading to YouTube: {}", title);
                youTubeService.uploadVideo(
                        file.getAbsolutePath(),
                        title,
                        request.getDescription(),
                        request.getPlaylist(),
                        "PUBLIC",
                        null, // Hashtags
                        false // keepOpenOnFailure
                );
            } catch (Exception e) {
                log.error("Failed to upload to YouTube: {}", title, e);
            }

            try {
                // 2. Bilibili
                log.info("Uploading to Bilibili: {}", title);
                bilibiliService.uploadVideo(
                        file.getAbsolutePath(),
                        title,
                        request.getDescription(),
                        null // Hashtags
                );
            } catch (Exception e) {
                log.error("Failed to upload to Bilibili: {}", title, e);
            }

            try {
                // 3. Xiaohongshu
                log.info("Uploading to Xiaohongshu: {}", title);
                xiaohongshuService.uploadVideo(
                        file.getAbsolutePath(),
                        title,
                        request.getDescription(),
                        null // Hashtags
                );
            } catch (Exception e) {
                log.error("Failed to upload to Xiaohongshu: {}", title, e);
            }

            try {
                // 4. TikTok
                log.info("Uploading to TikTok: {}", title);
                tikTokService.uploadVideo(
                        file.getAbsolutePath(),
                        title,
                        request.getDescription(),
                        request.getDescription(),
                        "PUBLIC",
                        null, // Hashtags
                        false // keepOpenOnFailure
                );
            } catch (Exception e) {
                log.error("Failed to upload to TikTok: {}", title, e);
            }

            log.info("Finished batch upload for file: {}", file.getName());
        }
    }
}
