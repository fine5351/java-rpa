package com.example.rpa.video.service;

import com.example.rpa.shared.constant.AutoAppendHashtag;
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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class XiaohongshuService {

    public boolean uploadVideo(String filePath, String title, String description, List<String> hashtags,
            boolean keepOpenOnFailure) {
        String simplifiedTitle = ZhConverterUtil.toSimple(title);
        String finalDescription = buildDescription(title, description, hashtags);
        String simplifiedDescription = ZhConverterUtil.toSimple(finalDescription);

        log.info("Simplified Title: {}", simplifiedTitle);
        log.info("Simplified Description: {}", simplifiedDescription);

        WebDriver driver = null;
        boolean success = false;
        try {
            driver = initializeDriver();
            navigateToCreatorStudio(driver);
            uploadFile(driver, filePath);
            waitForUploadComplete(driver);
            setTitle(driver, simplifiedTitle);
            setDescription(driver, simplifiedDescription);
            waitForPublishComplete(driver);
            clickPublish(driver);
            success = true;
            return true;
        } catch (Exception e) {
            log.error("Error during Xiaohongshu upload", e);
            return false;
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
                if (title.contains(keyword))
                    desc += " #" + keyword;
            }
        }

        if (hashtags != null) {
            for (String tag : hashtags) {
                if (!desc.contains(tag))
                    desc += " #" + tag;
            }
        }
        return desc.trim();
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

    private void navigateToCreatorStudio(WebDriver driver) {
        log.info("尋找 上傳頁面 位置中");
        log.info("已找到 上傳頁面 : {}", "https://creator.xiaohongshu.com/publish/publish");
        log.info("執行 前往上傳頁面 操作");
        driver.get("https://creator.xiaohongshu.com/publish/publish");
    }

    private void uploadFile(WebDriver driver, String filePath) {
        log.info("尋找 上傳按鈕 位置中");
        try {
            By selector = By.xpath("//input[@type='file']");
            WebElement fileInput = findElement(driver, selector, "上傳按鈕");
            log.info("執行 上傳檔案 操作");
            fileInput.sendKeys(filePath);
        } catch (Exception e) {
            log.error("File input not found.");
            throw e;
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        log.info("Waiting for upload to complete...");
        while (true) {
            try {
                boolean isUploading = false;
                List<WebElement> progressElements = driver.findElements(By.xpath("//*[contains(text(), '%')]"));
                for (WebElement el : progressElements) {
                    String text = el.getText();
                    if (text.matches(".*\\d+%.*") && !text.contains("100%")) {
                        isUploading = true;
                        log.info("Upload progress: {}", text);
                        break;
                    }
                }

                if (!isUploading) {
                    List<WebElement> successElements = driver.findElements(
                            By.xpath("//*[contains(text(), '上传成功') or contains(text(), 'Upload success')]"));
                    if (!successElements.isEmpty()) {
                        log.info("Upload complete.");
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
            By selector = By.xpath("//input[contains(@placeholder, '填写标题') or contains(@placeholder, '標題')]");
            WebElement titleInput = findElement(driver, selector, "標題輸入框");
            log.info("執行 設定標題 操作");
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
            By selector = By.xpath(
                    "//div[contains(@class, 'tiptap') and contains(@class, 'ProseMirror') and @contenteditable='true']");
            WebElement descInput = findElement(driver, selector, "說明輸入框");
            log.info("執行 設定說明 操作");
            descInput.click();

            // Split description by spaces to handle hashtags
            String[] parts = description.split(" ");
            for (String part : parts) {
                descInput.sendKeys(part);
                Thread.sleep(2000);
                if (part.startsWith("#")) {
                    try {
                        log.info("尋找 標籤建議 位置中");
                        By suggestionSelector = By
                                .xpath("/html/body/div[16]/div/div[1]/div/div/div[1]");
                        List<WebElement> suggestions = new WebDriverWait(driver, Duration.ofSeconds(5))
                                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(suggestionSelector));
                        log.info("已找到 標籤建議 : {}", suggestionSelector);

                        WebElement bestMatch = null;
                        long maxViews = -1;

                        for (WebElement item : suggestions) {
                            try {
                                String name = item.findElement(By.className("name")).getText();
                                String numText = item.findElement(By.className("num")).getText();

                                if (name.equals(part)) {
                                    long views = parseViews(numText);
                                    log.info("瀏覽人數: {}", views);
                                    if (views > maxViews) {
                                        maxViews = views;
                                        bestMatch = item;
                                    }
                                }
                            } catch (Exception ignored) {
                                log.warn("Could not parse views: {}", ignored.getMessage());
                            }
                        }

                        if (bestMatch != null) {
                            log.info("執行 點擊最佳建議 操作");
                            bestMatch.click();
                        } else if (!suggestions.isEmpty()) {
                            // Fallback: click the first one if no exact match found, or just ignore
                            log.info("執行 點擊首個建議 操作");
                            suggestions.get(0).click();
                        }
                        descInput.sendKeys(" ");

                    } catch (Exception ignored) {
                        // No suggestion or timeout, just continue
                    }
                }
            }
            log.info("Description set.");
        } catch (Exception e) {
            log.warn("Could not set description: {}", e.getMessage());
        }
    }

    private long parseViews(String numText) {
        if (numText == null)
            return 0;
        numText = numText.replace("人浏览", "").trim();
        double multiplier = 1;
        if (numText.endsWith("亿")) {
            multiplier = 100000000;
            numText = numText.replace("亿", "");
        } else if (numText.endsWith("万")) {
            multiplier = 10000;
            numText = numText.replace("万", "");
        }
        try {
            return (long) (Double.parseDouble(numText) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void waitForPublishComplete(WebDriver driver) {
        log.info("Waiting for publish to complete...");
        while (true) {
            try {
                // Check for upload progress (e.g., "上传中 28%")
                List<WebElement> progressElements = driver.findElements(By.xpath("//div[contains(text(), '上传中')]"));
                if (!progressElements.isEmpty()) {
                    String progressText = progressElements.get(0).getText().trim();
                    log.info("Xiaohongshu publish progress: {}", progressText);
                    Thread.sleep(2000);
                    continue;
                }

                // If no progress indicator, assume ready to publish
                break;
            } catch (Exception e) {
                // Ignore and continue waiting
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void clickPublish(WebDriver driver) {
        log.info("尋找 發佈按鈕 (使用 class 'publishBtn')");
        By selector = By.cssSelector("button.publishBtn");

        while (true) {
            try {
                // Wait for at least one element to be present (short wait)
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(selector));

                List<WebElement> elements = driver.findElements(selector);
                WebElement publishBtn = null;
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        publishBtn = el;
                        break;
                    }
                }

                if (publishBtn != null) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", publishBtn);
                    new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.elementToBeClickable(publishBtn));

                    log.info("執行 點擊發佈 操作");
                    publishBtn.click();
                    log.info("Clicked Publish.");

                    // Check for success or button gone
                    try {
                        By successSelector = By
                                .xpath("//*[contains(text(), '发布成功') or contains(text(), 'Publish success')]");
                        WebElement successElement = new WebDriverWait(driver, Duration.ofSeconds(10))
                                .until(ExpectedConditions.presenceOfElementLocated(successSelector));
                        log.info("Success indicator found: {}", successElement.getText());
                        break; // Success
                    } catch (Exception e) {
                        try {
                            boolean btnStillVisible = !driver.findElements(selector).isEmpty()
                                    && driver.findElement(selector).isDisplayed();
                            if (!btnStillVisible) {
                                log.info("Publish button is gone. Assuming success.");
                                break; // Success
                            }
                        } catch (Exception ex) {
                            log.info("Publish button is gone. Assuming success.");
                            break; // Success
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore and retry
            }

            log.info("找不到或無法點擊發佈按鈕, 持續尋找中...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Wait a bit before closing
        try {
            Thread.sleep(3000);
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

}
