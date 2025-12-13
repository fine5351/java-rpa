package com.example.rpa.service;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
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
import com.example.rpa.constant.AutoAppendHashtag;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BilibiliService {

    public void uploadVideo(String filePath, String title, String description, List<String> hashtags,
            boolean keepOpenOnFailure) {
        String simplifiedTitle = (title);

        String finalDescription = buildDescription(title, description, hashtags);

        WebDriver driver = null;
        boolean success = false;
        try {
            driver = initializeDriver();
            navigateToUpload(driver);
            uploadFile(driver, filePath);
            waitForUploadComplete(driver);
            setTitle(driver, simplifiedTitle);
            setDescription(driver, finalDescription);
            setTags(driver, hashtags);
            clickSubmit(driver);
            waitForSuccess(driver);
            success = true;
        } catch (Exception e) {
            log.error("Error during Bilibili upload", e);
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

    private String buildDescription(String title, String description, List<String> hashtags) {
        String desc = "";
        if (description != null)
            desc += description + "\n";

        if (title != null) {
            for (String keyword : AutoAppendHashtag.AUTO_HASHTAG_KEYWORDS) {
                if (title.contains(keyword)) {
                    desc += " #" + keyword;
                    hashtags.add(keyword);
                }
            }
        }

        if (hashtags != null) {
            for (String tag : hashtags) {
                if (!desc.contains(tag))
                    desc += " #" + tag;
            }
        }
        return ZhConverterUtil.toSimple(desc.trim());
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
        log.info("已找到 上傳頁面 : {}", "https://member.bilibili.com/platform/upload/video/frame");
        log.info("執行 前往上傳頁面 操作");
        driver.get("https://member.bilibili.com/platform/upload/video/frame");
    }

    private void uploadFile(WebDriver driver, String filePath) {
        log.info("尋找 上傳按鈕 位置中");
        try {
            // Wait for page to settle
            Thread.sleep(3000);

            // Try to find the file input directly first (global search)
            By globalInputSelector = By.xpath("//input[@type='file']");
            List<WebElement> inputs = driver.findElements(globalInputSelector);

            if (inputs.isEmpty()) {
                log.info("未直接找到檔案輸入框，嘗試等待...");
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.presenceOfElementLocated(globalInputSelector));
                    inputs = driver.findElements(globalInputSelector);
                } catch (Exception ignored) {
                }
            }

            if (inputs.isEmpty()) {
                log.info("仍未找到檔案輸入框，嘗試點擊上傳區域以觸發...");
                // If not found, try clicking the upload area to trigger it
                By uploadAreaSelector = By.xpath("//div[contains(@class, 'upload-area')]");

                // Infinite wait for upload area
                WebElement uploadArea = findClickableElement(driver, uploadAreaSelector, "上傳區域");

                log.info("執行 點擊上傳區域 操作");
                uploadArea.click();

                // Wait for input to appear (Infinite wait)
                log.info("等待檔案輸入框出現...");
                WebElement fileInput = findElement(driver, globalInputSelector, "上傳按鈕 (觸發後)");

                log.info("執行 上傳檔案 操作");
                fileInput.sendKeys(filePath);
            } else {
                log.info("已找到 上傳按鈕 : {}", globalInputSelector);
                log.info("執行 上傳檔案 操作");
                inputs.get(0).sendKeys(filePath);
            }
        } catch (Exception e) {
            log.error("File input not found even after trying to trigger it.");
            throw new RuntimeException(e);
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        log.info("Waiting for upload to complete...");
        while (true) {
            try {
                // Check for success message or completion state FIRST
                // User reported seeing "上传完成" while progress was still 99%
                List<WebElement> successElements = driver.findElements(
                        By.xpath(
                                "//*[contains(text(), '上传成功') or contains(text(), 'Upload success') or contains(text(), '上传完成')]"));
                if (!successElements.isEmpty()) {
                    log.info("Upload complete (success message found).");
                    break;
                }

                List<WebElement> progressElements = driver.findElements(By.xpath("//*[contains(text(), '%')]"));
                for (WebElement el : progressElements) {
                    String text = el.getText();
                    if (text.matches(".*\\d+%.*") && !text.contains("100%")) {
                        log.info("Upload progress: {}", text);
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
    }

    private void setTitle(WebDriver driver, String title) {
        try {
            By selector = By.xpath("//input[contains(@placeholder, '标题') or contains(@placeholder, 'Title')]");
            WebElement titleInput = findElement(driver, selector, "標題輸入框");

            log.info("執行 設定標題 操作");

            // Clear existing title (Bilibili might auto-fill from filename)
            titleInput.click();
            titleInput.sendKeys(Keys.CONTROL + "a");
            titleInput.sendKeys(Keys.BACK_SPACE);

            titleInput.sendKeys(title);
            log.info("Title set.");
        } catch (Exception e) {
            log.warn("Could not set title: {}", e.getMessage());
        }
    }

    private void setDescription(WebDriver driver, String description) {
        try {
            // Use CSS selector for better readability and precision with attributes
            // matching the user's provided HTML
            By selector = By.cssSelector("div.ql-editor[contenteditable='true'][data-placeholder*='填写更全面的相关信息']");

            WebElement descInput = findElement(driver, selector, "說明輸入框");

            log.info("執行 設定說明 操作 (使用 JS)");

            // Focus first
            try {
                descInput.click();
            } catch (Exception ignored) {
            }

            // Use JS to set content directly
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Escape description for JS string just in case, though arguments handling
            // usually covers it
            // We wrap it in <p> tags as that's the default structure
            js.executeScript("arguments[0].innerHTML = '<p>' + arguments[1] + '</p>';", descInput, description);

            // Trigger input event to ensure the frontend framework detects the change
            js.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", descInput);

            log.info("Description set.");
        } catch (Exception e) {
            log.warn("Could not set description: {}", e.getMessage());
            // Fallback to previous method if JS fails for some reason
            try {
                By fallbackSelector = By.xpath("//div[contains(@class, 'ql-editor') and @contenteditable='true']");
                WebElement fallbackInput = driver.findElement(fallbackSelector);
                fallbackInput.sendKeys(description);
            } catch (Exception ex) {
                log.error("Fallback description set failed", ex);
            }
        }
    }

    private void setTags(WebDriver driver, List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty())
            return;

        try {
            // Updated selector based on user feedback
            By selector = By.xpath("//input[contains(@class, 'input-val') and contains(@placeholder, '创建标签')]");
            WebElement tagInput = findElement(driver, selector, "標籤輸入框");

            for (String tag : hashtags) {
                log.info("執行 設定標籤 操作");
                String simplifiedTag = ZhConverterUtil.toSimple(tag);
                tagInput.sendKeys(simplifiedTag);
                log.info("標籤輸入: {}", simplifiedTag);
                tagInput.sendKeys(Keys.ENTER);
                // Small delay to ensure tag is registered
                Thread.sleep(500);
            }
            log.info("Tags set.");
        } catch (Exception e) {
            log.warn("Could not set tags: {}", e.getMessage());
        }
    }

    private void clickSubmit(WebDriver driver) {
        try {
            By selector = By.xpath("//span[contains(text(), '立即投稿') or contains(text(), 'Submit')]");
            WebElement submitBtn = findClickableElement(driver, selector, "發佈按鈕");

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);

            log.info("執行 點擊發佈 操作");
            submitBtn.click();
            log.info("Clicked Submit.");
        } catch (Exception e) {
            log.warn("Could not click Submit: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        log.info("Waiting for success...");
        By successSelector = By.xpath("//div[contains(@class, 'step-des') and contains(text(), '稿件投递成功')]");
        findElement(driver, successSelector, "成功訊息");
        log.info("Success indicator found.");
        // Wait 2 seconds before closing
        log.info("Waiting 2 seconds before closing...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private WebElement findElement(WebDriver driver, By selector, String description) {
        log.info("尋找 {} 位置中", description);
        while (true) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(selector));
                log.info("已找到 {} : {}", description, selector);
                return element;
            } catch (Exception e) {
                log.info("找不到 {}, 持續尋找中...", description);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for " + description, ex);
                }
            }
        }
    }

    private WebElement findClickableElement(WebDriver driver, By selector, String description) {
        log.info("尋找 {} 位置中", description);
        while (true) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(selector));
                log.info("已找到 {} : {}", description, selector);
                return element;
            } catch (Exception e) {
                log.info("找不到 {}, 持續尋找中...", description);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for " + description, ex);
                }
            }
        }
    }
}
