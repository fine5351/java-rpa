package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class TikTokService {

    private static final List<String> AUTO_HASHTAG_KEYWORDS = List.of(
            "深境螺旋", "幻想真境劇詩", "虛構敘事", "忘卻之庭", "末日幻影",
            "式輿防衛戰", "零號空洞", "幽境危戰", "異相仲裁", "擬真鏖戰試煉", "危局強襲戰");

    public void uploadVideo(String filePath, String title, String description, String visibility,
            List<String> hashtags) {
        String finalCaption = buildCaption(title, description, hashtags);
        log.info("Processed Caption: {}", finalCaption);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToUpload(driver);
            uploadFile(driver, filePath);
            waitForUploadComplete(driver);
            setCaption(driver, finalCaption);
            setVisibility(driver, visibility);
            postVideo(driver);
            waitForSuccess(driver);
        } catch (Exception e) {
            log.error("Error during TikTok upload", e);
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private String buildCaption(String title, String description, List<String> hashtags) {
        String caption = "";
        if (title != null)
            caption += title + "\n";
        if (description != null)
            caption += description + "\n";

        if (title != null) {
            for (String keyword : AUTO_HASHTAG_KEYWORDS) {
                if (title.contains(keyword))
                    caption += " #" + keyword;
            }
        }

        if (hashtags != null) {
            for (String tag : hashtags) {
                if (!caption.contains(tag))
                    caption += " " + tag;
            }
        }
        return caption.trim();
    }

    private WebDriver initializeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=d:/work/workspace/java/rpa/chrome-data");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
        return new ChromeDriver(options);
    }

    private void navigateToUpload(WebDriver driver) throws InterruptedException {
        log.info("Navigating to TikTok Upload...");
        driver.get("https://www.tiktok.com/tiktokstudio/upload");
        Thread.sleep(5000);
    }

    private void uploadFile(WebDriver driver, String filePath) {
        log.info("Uploading file...");
        try {
            WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
            fileInput.sendKeys(filePath);
        } catch (Exception e) {
            log.error("File input not found.");
            throw e;
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        log.info("Waiting for upload to complete...");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(120)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(text(), 'Uploaded') or contains(text(), '上傳完畢')]")));
            log.info("Upload complete.");
        } catch (Exception e) {
            log.warn("Upload completion text not found, proceeding...");
        }
    }

    private void setCaption(WebDriver driver, String caption) {
        log.info("Setting caption...");
        try {
            WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@contenteditable='true']")));
            editor.click();
            editor.sendKeys(Keys.CONTROL + "a");
            editor.sendKeys(Keys.BACK_SPACE);

            // Split caption by spaces to handle hashtags
            String[] parts = caption.split(" ");
            for (String part : parts) {
                editor.sendKeys(part);
                editor.sendKeys(" ");
                if (part.startsWith("#")) {
                    try {
                        Thread.sleep(1000); // Wait for suggestion
                        WebElement suggestion = new WebDriverWait(driver, Duration.ofSeconds(2))
                                .until(ExpectedConditions.presenceOfElementLocated(
                                        By.xpath("//div[contains(@class, 'mention-list')]//div[1]")));
                        suggestion.click();
                    } catch (Exception ignored) {
                        // No suggestion or timeout, just continue
                    }
                }
            }
            log.info("Caption set.");
        } catch (Exception e) {
            log.warn("Could not set caption: {}", e.getMessage());
        }
    }

    private void setVisibility(WebDriver driver, String visibility) {
        log.info("Setting visibility: {}", visibility);
        // Implementation depends on TikTok's specific UI for visibility
        // This is a placeholder as the selectors are complex and dynamic
    }

    private void postVideo(WebDriver driver) {
        log.info("Clicking Post...");
        try {
            // Handle copyright check modal if it appears
            try {
                WebElement postAnyway = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//button[contains(text(), 'Post anyway') or contains(text(), '仍然發布')]")));
                postAnyway.click();
            } catch (Exception ignored) {
            }

            WebElement postButton = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(text(), 'Post') or contains(text(), '發布')]")));

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", postButton);
            Thread.sleep(1000);

            // Check if disabled
            if (postButton.isEnabled()) {
                postButton.click();
                log.info("Clicked Post.");
            } else {
                log.warn("Post button is disabled.");
            }

        } catch (Exception e) {
            log.warn("Could not click Post: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath(
                            """
                                    //div[contains(text(), 'Video uploaded') or contains(text(), '影片已上傳') or contains(text(), 'Manage posts') or contains(text(), '管理貼文') or contains(text(), 'Upload another video') or contains(text(), '上傳另一支影片')]
                                    """)));
            log.info("Success indicator found.");
        } catch (Exception e) {
            log.info("Proceeding without specific success text.");
        }
    }
}
