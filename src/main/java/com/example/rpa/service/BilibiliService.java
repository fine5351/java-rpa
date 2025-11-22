package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import com.github.houbb.opencc4j.util.ZhConverterUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class BilibiliService {

    public void uploadVideo(String filePath, String title, String description, String visibility,
            List<String> hashtags) {
        // 0. Auto-tagging and Chinese Conversion (Simplified Chinese)
        if (title != null) {
            title = ZhConverterUtil.toSimple(title);
        }
        if (description != null) {
            description = ZhConverterUtil.toSimple(description);
        }

        System.out.println("Processed Title (Simplified): " + title);
        System.out.println("Processed Description (Simplified): " + description);

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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60)); // Increased timeout
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. Go to Bilibili Upload Page
            System.out.println("Navigating to Bilibili Upload Page...");
            driver.get("https://member.bilibili.com/platform/upload/video/frame");

            // Wait for page load (manual login might be needed if not logged in)
            Thread.sleep(5000);

            // 2. Upload File
            System.out.println("Waiting for file input...");
            try {
                // Bilibili usually has an input type=file, sometimes hidden
                WebElement fileInput = wait
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
                fileInput.sendKeys(filePath);
                System.out.println("Sent file path: " + filePath);
            } catch (Exception e) {
                System.out.println("Could not find file input. Please check if login is required.");
                throw e;
            }

            // 3. Wait for upload to process and form to appear
            System.out.println("Waiting for upload to process...");
            // Wait for title input to be visible
            Thread.sleep(5000);

            // 4. Set Title
            if (title != null && !title.isEmpty()) {
                System.out.println("Setting title...");
                try {
                    // Look for title input
                    WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '标题') or contains(@class, 'input-val')]")));

                    // Clear and type
                    titleInput.click();
                    titleInput.sendKeys(Keys.CONTROL + "a");
                    titleInput.sendKeys(Keys.BACK_SPACE);
                    titleInput.sendKeys(title);
                    System.out.println("Title set.");
                } catch (Exception e) {
                    System.out.println("Could not find title input: " + e.getMessage());
                }
            }

            // 5. Set Description
            if (description != null && !description.isEmpty()) {
                System.out.println("Setting description...");
                try {
                    // Look for description textarea or contenteditable div
                    WebElement descInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@contenteditable='true' and contains(@class, 'editor')] | //textarea")));

                    descInput.click();
                    descInput.sendKeys(Keys.CONTROL + "a");
                    descInput.sendKeys(Keys.BACK_SPACE);
                    descInput.sendKeys(description);
                    System.out.println("Description set.");
                } catch (Exception e) {
                    System.out.println("Could not find description input: " + e.getMessage());
                }
            }

            // 6. Handle Tags (Specific Input)
            if (hashtags != null && !hashtags.isEmpty()) {
                System.out.println("Setting tags...");
                try {
                    // Look for tag input. Usually has placeholder "按回车键Enter创建标签"
                    WebElement tagInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '创建标签') or contains(@class, 'tag-input')]")));

                    for (String tag : hashtags) {
                        String simpleTag = ZhConverterUtil.toSimple(tag);
                        tagInput.sendKeys(simpleTag);
                        tagInput.sendKeys(Keys.ENTER);
                        Thread.sleep(500);
                    }
                    System.out.println("Tags set.");
                } catch (Exception e) {
                    System.out.println("Could not find tag input: " + e.getMessage());
                }
            }

            // 7. Wait for Upload Complete (100%)
            System.out.println("Waiting for upload to reach 100%...");
            try {
                // Wait for text "上传完成" or "100%"
                // This selector might need adjustment based on actual UI
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath(
                                "//*[contains(text(), '上传完成') or contains(text(), '100%') or contains(text(), 'Upload Complete')]")));
                System.out.println("Upload 100% detected.");
            } catch (Exception e) {
                System.out.println(
                        "Warning: Explicit '100%' text not found within timeout. Proceeding based on button state.");
            }

            // 8. Publish
            System.out.println("Waiting for Publish button to be ready...");

            WebElement publishButton = null;
            boolean readyToClick = false;

            // Wait loop for upload completion
            for (int i = 0; i < 300; i++) {
                try {
                    // Find Publish Button (立即投稿)
                    try {
                        publishButton = driver.findElement(
                                By.xpath("//span[contains(text(), '立即投稿')] | //div[contains(@class, 'submit-add')]"));
                    } catch (Exception e) {
                        // Retry
                    }

                    if (publishButton != null) {
                        // Check if enabled
                        if (publishButton.isEnabled()) {
                            readyToClick = true;
                            break;
                        }
                    }

                    if (i % 5 == 0)
                        System.out.println("Waiting for publish button...");
                } catch (Exception e) {
                    // Ignore
                }
                Thread.sleep(1000);
            }

            if (readyToClick && publishButton != null) {
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", publishButton);
                Thread.sleep(1000);
                try {
                    publishButton.click();
                    System.out.println("Clicked Publish button.");
                } catch (Exception e) {
                    js.executeScript("arguments[0].click();", publishButton);
                    System.out.println("Clicked Publish button (JS).");
                }

                // Wait for success
                Thread.sleep(5000);
            } else {
                throw new RuntimeException("Publish button never became enabled.");
            }

            System.out.println("Bilibili upload sequence finished.");
            Thread.sleep(5000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
