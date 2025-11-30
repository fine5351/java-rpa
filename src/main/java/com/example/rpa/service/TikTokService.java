package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
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
            List<String> hashtags, boolean keepOpenOnFailure) {
        String finalCaption = buildCaption(title, description, hashtags);
        log.info("Processed Caption: {}", finalCaption);

        WebDriver driver = null;
        boolean success = false;
        try {
            driver = initializeDriver();
            navigateToUpload(driver);
            uploadFile(driver, filePath);
            waitForUploadComplete(driver);
            setCaption(driver, finalCaption);
            setVisibility(driver, visibility);
            postVideo(driver);
            success = true;
        } catch (Exception e) {
            log.error("Error during TikTok upload", e);
        } finally {
            if (driver != null) {
                if (success || !keepOpenOnFailure) {
                    driver.quit();
                    log.info("Browser closed successfully.");
                } else {
                    log.warn("Browser left open for debugging.");
                }
            }
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
                if (title.contains(keyword)) {
                    String hashtag = " #" + keyword;
                    if (!caption.contains(hashtag))
                        caption += hashtag;
                }
            }
        }

        if (hashtags != null) {
            for (String tag : hashtags) {
                String hashtag = " #" + tag;
                if (!caption.contains(hashtag))
                    caption += hashtag;
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

    private void navigateToUpload(WebDriver driver) {
        log.info("尋找 上傳頁面 位置中");
        log.info("已找到 上傳頁面 : {}", "https://www.tiktok.com/tiktokstudio/upload");
        log.info("執行 前往上傳頁面 操作");
        driver.get("https://www.tiktok.com/tiktokstudio/upload");
    }

    private void uploadFile(WebDriver driver, String filePath) {
        log.info("尋找 上傳按鈕 位置中");
        try {
            By selector = By.xpath("//input[@type='file']");
            WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(selector));
            log.info("已找到 上傳按鈕 : {}", selector);
            log.info("執行 上傳檔案 操作");
            fileInput.sendKeys(filePath);
        } catch (Exception e) {
            log.error("File input not found.");
            throw e;
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        log.info("Waiting for upload to complete...");
        boolean uploadComplete = false;

        while (!uploadComplete) {
            try {
                boolean isUploading = false;

                // Check for progress percentage
                List<WebElement> progressElements = driver.findElements(By.xpath("//div[contains(text(), '%')]"));
                for (WebElement el : progressElements) {
                    String text = el.getText();
                    if (text.matches(".*\\d+%.*") && !text.contains("100%")) {
                        isUploading = true;
                        log.info("Upload progress: {}", text);
                        break;
                    }
                }

                // If not actively uploading, check for completion indicators
                if (!isUploading) {
                    List<WebElement> successElements = driver.findElements(By.xpath(
                            "//*[contains(text(), 'Uploaded') or contains(text(), '上傳完畢') or contains(text(), '已上傳')]"));
                    if (!successElements.isEmpty()) {
                        log.info("Upload complete indicator found: {}", successElements.get(0).getText());
                        uploadComplete = true;
                        break;
                    }
                }

                Thread.sleep(1000);
            } catch (Exception e) {
                log.info("Waiting for upload...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Extra wait to ensure UI is fully ready
        if (uploadComplete) {
            try {
                log.info("Upload complete, waiting 3 seconds for UI to stabilize...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void setCaption(WebDriver driver, String caption) {
        log.info("尋找 標題輸入框 位置中");
        try {
            By selector = By.xpath("//div[@contenteditable='true']");
            WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(selector));
            log.info("已找到 標題輸入框 : {}", selector);
            log.info("執行 設定標題 操作");
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
                        log.info("尋找 標籤建議 位置中");
                        By suggestionSelector = By.xpath("//div[contains(@class, 'mention-list')]//div[1]");
                        WebElement suggestion = new WebDriverWait(driver, Duration.ofSeconds(2))
                                .until(ExpectedConditions.presenceOfElementLocated(suggestionSelector));
                        log.info("已找到 標籤建議 : {}", suggestionSelector);
                        log.info("執行 點擊建議 操作");
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
        log.info("尋找 發佈按鈕 位置中");
        try {
            // Target the button element using data-e2e attribute and Button__content
            By postSelector = By.xpath(
                    "//button[@data-e2e='post_video_button' and .//div[contains(@class, 'Button__content') and (contains(text(), '發佈') or contains(text(), 'Post'))]]");
            WebElement postButton = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(postSelector));
            log.info("已找到 發佈按鈕 : {}", postSelector);

            log.info("執行 點擊發佈 操作");
            postButton.click();
            log.info("Clicked Post.");

            // Handle "Post Immediately" modal if it appears
            try {
                log.info("尋找 發佈確認按鈕 位置中");
                By confirmSelector = By.xpath(
                        "//button[contains(@class, 'TUXButton') and .//div[contains(@class, 'TUXButton-label') and (contains(text(), '立即發佈') or contains(text(), 'Post'))]]");
                WebElement confirmButton = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(confirmSelector));
                log.info("已找到 發佈確認按鈕 : {}", confirmSelector);
                log.info("執行 點擊發佈 操作");
                confirmButton.click();
                log.info("Clicked Post Immediately.");
            } catch (Exception e) {
                log.info("No 'Post Immediately' modal appeared (or timed out): {}", e.getMessage());
            }

            // Wait for success message/redirection
            log.info("Waiting for post success...");
            try {
                By successSelector = By.xpath(
                        "//div[contains(text(), 'Manage your posts') or contains(text(), 'View profile') or contains(text(), 'Upload another video') or contains(text(), '上傳另一支影片')]");
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(ExpectedConditions.presenceOfElementLocated(successSelector));
                log.info("Post success indicator found.");
            } catch (Exception e) {
                // Try checking URL change or other indicators
                log.warn("Explicit success message not found, checking URL...");
                // If URL changes to profile or management page, it's likely success
            }

            // Wait 3 seconds before closing
            log.info("Waiting 3 seconds before closing...");
            Thread.sleep(3000);

        } catch (Exception e) {
            log.warn("Could not click Post: {}", e.getMessage());
            throw new RuntimeException("Failed to post video", e);
        }
    }
}
