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
public class BilibiliService {

    public boolean uploadVideo(String filePath, String title, String description, List<String> hashtags,
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
            setDescription(driver, finalDescription);
            selectCategory(driver);
            setTags(driver, hashtags);
            clickSubmit(driver);
            waitForSuccess(driver);
            success = true;
            return true;
        } catch (Exception e) {
            log.error("Error during Bilibili upload", e);
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
        String stepName = "前往上傳頁面";
        log.info("步驟 : {}, 持續尋找中 {}...", stepName, "https://member.bilibili.com/platform/upload/video/frame");
        driver.get("https://member.bilibili.com/platform/upload/video/frame");
    }

    private void uploadFile(WebDriver driver, String filePath) {
        String stepName = "上傳檔案";
        while (true) {
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
                    WebElement uploadArea = findClickableElement(driver, stepName, uploadAreaSelector, "上傳區域");

                    uploadArea.click();

                    // Wait for input to appear (Infinite wait)
                    WebElement fileInput = findElement(driver, stepName, globalInputSelector, "上傳按鈕 (觸發後)");

                    fileInput.sendKeys(filePath);
                } else {
                    inputs.get(0).sendKeys(filePath);
                }

                // Verify upload started
                log.info("檔案路徑已送出，等待3秒確認上傳進度...");
                Thread.sleep(3000);

                // Check for progress element
                // outerHTML = <span data-v-0092e033="" class="progress-text">67%</span>
                List<WebElement> progressElements = driver.findElements(By.className("progress-text"));
                if (!progressElements.isEmpty()) {
                    log.info("檢測到上傳進度，上傳成功啟動。");
                    break;
                } else {
                    log.warn("未檢測到上傳進度 (progress-text)，重新嘗試上傳...");
                }

            } catch (Exception e) {
                log.error("上傳過程發生錯誤，準備重試...", e);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        String stepName = "等待上傳完成";
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
                log.info("步驟 : {}, 持續尋找中 {}...", stepName, "上傳完成標誌");
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
            String stepName = "設定標題";
            By selector = By.xpath("//input[contains(@placeholder, '标题') or contains(@placeholder, 'Title')]");
            WebElement titleInput = findElement(driver, stepName, selector, "標題輸入框");

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
            String stepName = "設定說明";
            // Use CSS selector for better readability and precision with attributes
            // matching the user's provided HTML
            By selector = By.cssSelector("div.ql-editor[contenteditable='true'][data-placeholder*='填写更全面的相关信息']");

            WebElement descInput = findElement(driver, stepName, selector, "說明輸入框");

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

    private void selectCategory(WebDriver driver) {
        try {
            String stepName = "選擇分區";
            // Locate the dropdown using the user's provided class hint, or general class
            // User provided: <div class="select-controller ...">
            By dropdownSelector = By.cssSelector(".select-controller");
            WebElement dropdown = findClickableElement(driver, stepName, dropdownSelector, "分區下拉選單");

            // Scroll to it first
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", dropdown);
            Thread.sleep(500);

            dropdown.click();
            log.info("Clicked category dropdown.");

            // Wait for options to appear
            Thread.sleep(1000);

            // Select "Game" (游戏)
            // Looking for an element with text "游戏" that is selectable
            By gameOptionSelector = By
                    .xpath("//*[contains(text(), '游戏') and not(contains(@class, 'select-item-cont'))]");
            // Note: The user showed 'select-item-cont' is the selected text.
            // In the dropdown list, it might be different. usually it's a list item.
            // Let's try a broader search for clickable "游戏" text in the dropdown container.

            // Refined selector for the dropdown option:
            By optionSelector = By.xpath(
                    "//div[contains(@class, 'drop-main')]//span[contains(text(), '游戏')] | //div[contains(@class, 'drop-main')]//div[contains(text(), '游戏')]");
            // Fallback to simple text search if class names are dynamic
            By simpleOptionSelector = By.xpath("//*[text()='游戏']");

            WebElement gameOption = null;
            try {
                gameOption = findClickableElement(driver, stepName, simpleOptionSelector, "遊戲選項");
            } catch (Exception e) {
                // Try looking specifically inside the dropdown list container if possible, but
                // global text usually works for unique items like this
                log.warn("Direct '游戏' text not found, trying alternative selectors...");
            }

            if (gameOption != null) {
                gameOption.click();
                log.info("Category 'Game' selected.");
            } else {
                log.error("Could not find '游戏' option.");
            }

            Thread.sleep(500);

        } catch (Exception e) {
            log.warn("Could not select category: {}", e.getMessage());
        }
    }

    private void setTags(WebDriver driver, List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty())
            return;

        try {
            String stepName = "設定標籤";
            // Updated selector based on user feedback
            By selector = By.xpath("//input[contains(@class, 'input-val') and contains(@placeholder, '创建标签')]");
            WebElement tagInput = findElement(driver, stepName, selector, "標籤輸入框");

            for (String tag : hashtags) {
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
            String stepName = "點擊發佈按鈕";
            By selector = By.xpath("//span[contains(text(), '立即投稿') or contains(text(), 'Submit')]");
            WebElement submitBtn = findClickableElement(driver, stepName, selector, "發佈按鈕");

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);

            submitBtn.click();
            log.info("Clicked Submit.");
        } catch (Exception e) {
            log.warn("Could not click Submit: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        String stepName = "等待發佈成功";
        By successSelector = By.xpath("//div[contains(@class, 'step-des') and contains(text(), '稿件投递成功')]");
        findElement(driver, stepName, successSelector, "成功訊息");
        log.info("Success indicator found.");
        // Wait 2 seconds before closing
        log.info("Waiting 2 seconds before closing...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private WebElement findElement(WebDriver driver, String stepName, By selector, String elementName) {
        while (true) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(selector));
                return element;
            } catch (Exception e) {
                log.info("步驟 : {}, 持續尋找中 {}...", stepName, elementName);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for " + elementName, ex);
                }
            }
        }
    }

    private WebElement findClickableElement(WebDriver driver, String stepName, By selector, String elementName) {
        while (true) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(selector));
                return element;
            } catch (Exception e) {
                log.info("步驟 : {}, 持續尋找中 {}...", stepName, elementName);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for " + elementName, ex);
                }
            }
        }
    }
}
