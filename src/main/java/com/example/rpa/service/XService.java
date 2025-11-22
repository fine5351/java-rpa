package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class XService {

    public void postLink(String videoUrl, String title, String description, List<String> hashtags) {
        // Set console output to UTF-8
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Construct post content
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

        String postContent = contentBuilder.toString().trim();
        System.out.println("Post Content: " + postContent);

        // Setup WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // Use a separate user data directory for X to allow a different Google account
        // login
        String userDataDir = "d:/work/workspace/java/rpa/chrome-data-x";
        options.addArguments("user-data-dir=" + userDataDir);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = null;
        try {
            System.out.println("Initializing ChromeDriver for X (Profile: chrome-data-x)...");
            driver = new ChromeDriver(options);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            // 1. Go to X Compose Tweet page
            System.out.println("Navigating to https://x.com/compose/tweet ...");
            driver.get("https://x.com/compose/tweet");

            // 2. Wait for editor
            System.out.println("Waiting for editor...");
            WebElement editor = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@data-testid='tweetTextarea_0']")));
            editor.click();

            // 3. Enter content
            System.out.println("Entering content...");
            editor.sendKeys(postContent);

            // 4. Enter Hashtags
            if (hashtags != null && !hashtags.isEmpty()) {
                editor.sendKeys(Keys.ENTER); // New line for tags
                editor.sendKeys(Keys.ENTER);

                for (String tag : hashtags) {
                    String cleanTag = tag.replace("#", "").trim();
                    if (!cleanTag.isEmpty()) {
                        System.out.println("Entering hashtag: #" + cleanTag);
                        editor.sendKeys("#" + cleanTag);
                        Thread.sleep(2000); // Wait for suggestion dropdown
                        editor.sendKeys(Keys.ENTER); // Select first suggestion
                        Thread.sleep(500);
                    }
                }
            }

            // Wait for link preview if URL is present
            if (videoUrl != null && !videoUrl.isEmpty()) {
                System.out.println("Waiting for link preview...");
                Thread.sleep(5000); // Give it some time to fetch preview
            }

            // 5. Click Post button
            System.out.println("Clicking Post button...");
            WebElement postButton = null;
            try {
                // Try standard data-testid (User confirmed it's a <button>)
                postButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[@data-testid='tweetButton']")));
            } catch (Exception e) {
                try {
                    // Try text based (English and Traditional Chinese) - searching broadly
                    postButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    "//*[@role='button']//span[contains(text(), 'Post') or contains(text(), '發佈') or contains(text(), '推文')]")));
                } catch (Exception ex) {
                    // Try finding any button that looks like the primary action in the compose bar
                    postButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//*[@data-testid='tweetButtonInline']")));
                }
            }

            if (postButton != null) {
                // Ensure it's enabled
                try {
                    // Wait up to 5 seconds for the button to become enabled (in case of lag after
                    // typing)
                    new WebDriverWait(driver, Duration.ofSeconds(5)).until(
                            ExpectedConditions
                                    .not(ExpectedConditions.attributeToBe(postButton, "aria-disabled", "true")));
                } catch (Exception e) {
                    System.out.println(
                            "Warning: Post button might be disabled (aria-disabled=true). Attempting click anyway...");
                }

                System.out.println("Found Post button: " + postButton.getTagName() + ", Text: " + postButton.getText());
                postButton.click();
            } else {
                throw new RuntimeException("Could not find Post button.");
            }

            // Wait for post to complete
            Thread.sleep(5000);
            System.out.println("X post submitted.");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to post on X: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
