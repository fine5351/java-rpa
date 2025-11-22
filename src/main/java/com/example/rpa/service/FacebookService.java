package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class FacebookService {

    public void postLink(String videoUrl, String title, String description, List<String> hashtags) {
        // Construct the post content
        StringBuilder contentBuilder = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            contentBuilder.append(title).append("\n\n");
        }
        if (description != null && !description.isEmpty()) {
            contentBuilder.append(description).append("\n\n");
        }
        if (videoUrl != null && !videoUrl.isEmpty()) {
            contentBuilder.append(videoUrl).append("\n\n");
        }
        if (hashtags != null && !hashtags.isEmpty()) {
            for (String tag : hashtags) {
                contentBuilder.append(tag).append(" ");
            }
        }
        String postContent = contentBuilder.toString().trim();

        // Setup WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        String userDataDir = "d:/work/workspace/java/rpa/chrome-data";
        options.addArguments("user-data-dir=" + userDataDir);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // 1. Go to Facebook
            driver.get("https://www.facebook.com");

            // 2. Click "What's on your mind?" input
            // Try multiple selectors as FB classes change frequently
            WebElement postInput = null;
            try {
                // Common text based selector for "What's on your mind?"
                postInput = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[contains(text(), \"What's on your mind\")]/ancestor::div[@role='button']")));
            } catch (Exception e) {
                // Fallback to Chinese
                try {
                    postInput = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//span[contains(text(), \"在想些什麼\")]/ancestor::div[@role='button']")));
                } catch (Exception ex) {
                    System.out.println("Could not find the initial post input area.");
                    throw ex;
                }
            }
            postInput.click();

            // 3. Enter text
            // The actual input is a contenteditable div inside a dialog
            WebElement editor = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//div[@role='dialog']//div[@role='textbox']")));
            editor.sendKeys(postContent);

            // Wait for link preview if a URL is present (simple wait for now, can be
            // improved)
            if (videoUrl != null && !videoUrl.isEmpty()) {
                Thread.sleep(5000);
            }

            // 4. Click Post
            WebElement postButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@role='dialog']//div[@aria-label='Post' or @aria-label='發佈']")));
            postButton.click();

            // Wait for post to complete
            Thread.sleep(10000);
            System.out.println("Facebook post submitted.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
