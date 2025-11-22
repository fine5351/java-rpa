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

@Service
public class ThreadsService {

    public void postLink(String videoUrl, String title, String description, String topic) {
        // Set console output to UTF-8 to avoid garbled text
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        String postContent = contentBuilder.toString().trim();

        // Remove # from topic if present
        if (topic != null) {
            topic = topic.replace("#", "");
        }

        System.out.println("Post Content: " + postContent);
        System.out.println("Topic: " + topic);

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
            System.out.println("Initializing ChromeDriver...");
            driver = new ChromeDriver(options);
            System.out.println("ChromeDriver initialized.");

            // Increase wait time to 120 seconds to allow for manual login if needed
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));

            // 1. Go to Threads
            System.out.println("Navigating to https://www.threads.net ...");
            driver.get("https://www.threads.net");
            System.out.println("Navigated to Threads.");

            // 2. Click "Start a thread..." input
            WebElement startThreadInput = null;
            try {
                // Priority 1: Try exact aria-label from user provided element
                startThreadInput = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[@aria-label='文字欄位空白。請輸入內容以撰寫新貼文。']")));
            } catch (Exception e) {
                try {
                    // Priority 2: User provided XPath
                    startThreadInput = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    "/html/body/div[2]/div/div/div[2]/div[2]/div/div/div/div[1]/div[1]/div[1]/div/div/div[2]/div[1]/div[2]/div/div[1]")));
                } catch (Exception ex) {
                    try {
                        // Priority 3: Text match but strictly exclude articles to avoid clicking posts
                        startThreadInput = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//div[contains(text(), '有什麼新鮮事') and not(ancestor::div[@role='article'])]")));
                    } catch (Exception ex2) {
                        System.out.println("Could not find 'Start a thread' element.");
                        throw ex2;
                    }
                }
            }
            startThreadInput.click();

            // 3. Enter text
            Thread.sleep(2000); // Wait for editor animation

            // The editor is usually a contenteditable div.
            WebElement editor = wait
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@contenteditable='true']")));
            editor.click(); // Ensure focus
            editor.sendKeys(postContent);

            // 4. Set Topic (Inline tagging)
            if (topic != null && !topic.isEmpty()) {
                try {
                    editor.sendKeys(Keys.ENTER);
                    Thread.sleep(500);
                    // Type # then topic
                    editor.sendKeys("#" + topic);
                    Thread.sleep(1000); // Wait for dropdown suggestion (Reduced)

                    // Use keyboard navigation to select the first suggestion
                    // User reported ARROW_DOWN selects the second item, so just ENTER is enough
                    Thread.sleep(500);
                    editor.sendKeys(Keys.ENTER);
                } catch (Exception e) {
                    System.out.println("Topic selection failed, skipping.");
                }
            }

            // Wait for UI to settle after topic selection
            Thread.sleep(1000); // Reduced

            // Wait for link preview if a URL is present
            if (videoUrl != null && !videoUrl.isEmpty()) {
                Thread.sleep(3000); // Reduced
            }

            // 5. Click Post
            System.out.println("Attempting to find and click Post button...");
            try {
                // Strategy:
                // 1. Try to find candidates inside the modal (role='dialog').
                // 2. If not found, search globally.
                // 3. Filter for elements with NON-EMPTY text (to avoid background icons).
                // 4. Pick the bottom-rightmost element.
                // 5. Use JS click.

                java.util.List<WebElement> candidates;
                try {
                    candidates = driver
                            .findElements(By.xpath("//div[@role='dialog']//div[contains(@class, 'x10wlt62')]"));
                    System.out.println("Found " + candidates.size() + " candidates inside dialog.");
                } catch (Exception e) {
                    candidates = Collections.emptyList();
                }

                if (candidates.isEmpty()) {
                    System.out.println("No candidates in dialog, searching globally...");
                    candidates = driver.findElements(By.xpath("//div[contains(@class, 'x10wlt62')]"));
                }

                WebElement targetButton = null;
                int maxX = -1;
                int maxY = -1;

                for (WebElement btn : candidates) {
                    try {
                        if (btn.isDisplayed() && btn.isEnabled()) {
                            String text = btn.getText();
                            org.openqa.selenium.Point location = btn.getLocation();

                            System.out.println(
                                    "Candidate: Text='" + text + "', X=" + location.getX() + ", Y=" + location.getY());

                            // Filter out empty buttons (the Post button definitely has text)
                            if (text == null || text.trim().isEmpty()) {
                                continue;
                            }

                            // Find the bottom-most, then right-most button
                            // Allow a small tolerance for Y alignment (e.g., 10px)
                            if (location.getY() > maxY + 10) {
                                maxY = location.getY();
                                maxX = location.getX();
                                targetButton = btn;
                            } else if (Math.abs(location.getY() - maxY) <= 10 && location.getX() > maxX) {
                                maxX = location.getX();
                                maxY = Math.max(maxY, location.getY());
                                targetButton = btn;
                            }
                        }
                    } catch (Exception innerEx) {
                        System.out.println("Error checking candidate: " + innerEx.getMessage());
                    }
                }

                if (targetButton != null) {
                    System.out.println("Clicking target button (Text='" + targetButton.getText() + "') at (" + maxX
                            + ", " + maxY + ") using JavaScript.");
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();",
                            targetButton);
                    System.out.println("Post button clicked successfully (JS).");
                } else {
                    throw new RuntimeException(
                            "No clickable Post button with text found among " + candidates.size() + " candidates.");
                }

            } catch (Exception e) {
                System.out.println("Failed to find or click Post button: " + e.getMessage());
                System.out.println("Waiting 10 seconds for debugging...");
                Thread.sleep(10000); // Reduced wait
                throw e;
            }

            // Wait for post to complete
            Thread.sleep(5000); // Reduced wait
            System.out.println("Threads post submitted.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
