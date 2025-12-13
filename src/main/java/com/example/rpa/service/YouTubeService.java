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

import com.example.rpa.constant.AutoAppendHashtag;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class YouTubeService {

    private static final String FIND_ELEMENT_RECURSIVE_SCRIPT = """
            function findElementRecursive(root, id, text, className, tagName) {
              if (!root) return null;
              if (id && root.id === id) return root;
              if (tagName && root.tagName === tagName.toUpperCase()) return root;
              if (className && root.classList && root.classList.contains(className)) return root;
              if (text && root.innerText && root.innerText.includes(text)) return root;
              if (root.shadowRoot) {
                var child = findElementRecursive(root.shadowRoot, id, text, className, tagName);
                if (child) return child;
              }
              if (root.children) {
                for (var i = 0; i < root.children.length; i++) {
                  var child = findElementRecursive(root.children[i], id, text, className, tagName);
                  if (child) return child;
                }
              }
              return null;
            }
            var app = document.querySelector('ytcp-app');
            var startNode = app ? app : document.body;
            """;

    public boolean uploadVideo(String filePath, String title, String description, String playlist, String visibility,
            List<String> hashtags, boolean keepOpenOnFailure) {
        String finalDescription = buildDescription(title, description, hashtags);
        WebDriver driver = null;
        boolean success = false;
        try {
            driver = initializeDriver();
            navigateToStudio(driver);
            clickCreateButton(driver);
            selectUploadOption(driver);
            uploadFile(driver, filePath);
            enterTitleAndDescription(driver, title, finalDescription);
            selectPlaylist(driver, playlist);
            setKidsRestriction(driver);
            navigateWizardPages(driver);
            setVisibility(driver, visibility);
            saveAndClose(driver);
            success = true;
            return true;
        } catch (Exception e) {
            log.error("Error during YouTube upload", e);
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
        if (description == null)
            description = "";

        description += "\n\n";

        if (hashtags != null) {
            for (String tag : hashtags) {
                if (!description.contains(tag))
                    description += "#" + tag + " ";
            }
        }

        if (title != null) {
            for (String keyword : AutoAppendHashtag.AUTO_HASHTAG_KEYWORDS) {
                if (title.contains(keyword)) {
                    description += "#" + keyword + " ";
                }
            }
        }

        return description;
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

    private void navigateToStudio(WebDriver driver) {
        log.info("尋找 YouTube Studio 位置中");
        log.info("已找到 YouTube Studio : {}", "https://studio.youtube.com");
        log.info("執行 前往 YouTube Studio 操作");
        driver.get("https://studio.youtube.com");
        try {
            WebElement continueButton = new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions
                    .elementToBeClickable(
                            By.xpath("//tp-yt-paper-button[@id='button' and .//div[contains(text(),'Continue')]]")));
            continueButton.click();
        } catch (Exception ignored) {
        }

    }

    private void handleTrustTiersPopup(WebDriver driver) {
        log.info("Checking for potential 'Trust Tiers' popup...");
        try {
            // Full xpath provided by user:
            // /html/body/yt-trust-tiers-wizard-dialog/ytcp-dialog/tp-yt-paper-dialog/div[3]/div/ytcp-button/ytcp-button-shape/button/yt-touch-feedback-shape/div[2]
            // We'll use a slightly more robust relative xpath targeting the button within
            // the specific dialog
            By popupSelector = By.xpath("//yt-trust-tiers-wizard-dialog");

            // Short wait to see if it appears
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(popupSelector));
                log.info("'Trust Tiers' popup detected.");

                // Find the confirm button ("Got it" / "知道了")
                // Using a relative path similar to user's but targeting the button itself
                By confirmBtnSelector = By.xpath(
                        "//yt-trust-tiers-wizard-dialog//ytcp-button[.//div[contains(@class, 'yt-spec-touch-feedback-shape__fill')]]");

                WebElement confirmBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(confirmBtnSelector));

                log.info("Found confirmation button, clicking...");
                confirmBtn.click();
                log.info("Popup dismissed.");
                Thread.sleep(1000); // Wait for animation
            } catch (Exception e) {
                log.info("Popup did not appear or button not found: " + e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Error handling popup: " + e.getMessage());
        }
    }

    private void clickCreateButton(WebDriver driver) throws Exception {
        log.info("尋找 建立按鈕 位置中");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement uploadButton = findUploadButton(js);
        if (uploadButton != null) {
            log.info("已找到 建立按鈕 : {}", uploadButton);
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", uploadButton);
            uploadButton = adjustButtonTarget(uploadButton);
            log.info("執行 點擊建立按鈕 操作");
            dispatchClickEvents(js, uploadButton);
            log.info("Dispatched click events to Upload button.");
            handleTrustTiersPopup(driver);
        } else {
            throw new RuntimeException("Could not find any Upload/Create button.");
        }
    }

    private WebElement findUploadButton(JavascriptExecutor js) throws InterruptedException {
        while (true) {
            try {
                String quickAction = FIND_ELEMENT_RECURSIVE_SCRIPT + """
                        var qa = findElementRecursive(startNode, null, null, null, 'YTCP-QUICK-ACTIONS');
                        return qa ? qa.querySelector('ytcp-icon-button') : null;
                        """;
                WebElement btn = (WebElement) js.executeScript(quickAction);
                if (btn != null)
                    return btn;

                String createIcon = FIND_ELEMENT_RECURSIVE_SCRIPT + """
                        return findElementRecursive(startNode, 'create-icon', null, null, null);
                        """;
                btn = (WebElement) js.executeScript(createIcon);
                if (btn != null)
                    return btn;

                String classSearch = FIND_ELEMENT_RECURSIVE_SCRIPT + """
                        return findElementRecursive(startNode, null, null, 'yt-spec-touch-feedback-shape__fill', null);
                        """;
                btn = (WebElement) js.executeScript(classSearch);
                if (btn != null)
                    return btn;
            } catch (Exception ignored) {
            }
            log.info("找不到 建立按鈕, 持續尋找中...");
            Thread.sleep(2000);
        }
    }

    private WebElement adjustButtonTarget(WebElement button) {
        String tagName = button.getTagName();
        if (!tagName.equalsIgnoreCase("YTCP-BUTTON") &&
                !tagName.equalsIgnoreCase("TP-YT-PAPER-ICON-BUTTON") &&
                !tagName.equalsIgnoreCase("YTCP-ICON-BUTTON")) {
            try {
                WebElement parent = button.findElement(By.xpath("./.."));
                if (parent != null)
                    return parent;
            } catch (Exception ignored) {
            }
        }
        return button;
    }

    private void selectUploadOption(WebDriver driver) throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
            return; // File input already present
        } catch (Exception ignored) {
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Infinite loop to find "Upload videos" option
        while (true) {
            try {
                By selector = By
                        .xpath("//tp-yt-paper-item[.//div[contains(text(),'Upload videos') or contains(text(),'上傳影片')]]");
                // Quick check with short timeout
                WebElement uploadOption = new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions
                        .presenceOfElementLocated(selector));
                log.info("已找到 上傳影片選項 : {}", selector);
                log.info("執行 點擊上傳影片選項 操作");
                dispatchClickEvents(js, uploadOption);
                return;
            } catch (Exception e) {
                // Try JS fallback
                String script = FIND_ELEMENT_RECURSIVE_SCRIPT + """
                        var opt = findElementRecursive(startNode, null, 'Upload videos', null, null);
                        if (!opt) opt = findElementRecursive(startNode, null, '上傳影片', null, null);
                        return opt;
                        """;
                WebElement uploadOption = (WebElement) js.executeScript(script);
                if (uploadOption != null) {
                    log.info("已找到 上傳影片選項 (Script) : {}", uploadOption);
                    log.info("執行 點擊上傳影片選項 操作");
                    dispatchClickEvents(js, uploadOption);
                    return;
                }
            }
            log.info("找不到 上傳影片選項, 持續尋找中...");
            Thread.sleep(2000);
        }
    }

    private void uploadFile(WebDriver driver, String filePath) {
        By selector = By.xpath("//input[@type='file']");
        WebElement fileInput = findElement(driver, selector, "上傳檔案輸入框");
        log.info("執行 上傳檔案 操作");
        fileInput.sendKeys(filePath);
        log.info("Sent file path: {}", filePath);
    }

    private void enterTitleAndDescription(WebDriver driver, String title, String description) {
        try {
            if (title != null && !title.isEmpty()) {
                By titleSelector = By
                        .xpath("//ytcp-social-suggestions-textbox[@id='title-textarea']//div[@id='textbox']");
                WebElement titleBox = findElement(driver, titleSelector, "標題輸入框");

                log.info("執行 設定標題 操作");
                setText(titleBox, title);
                log.info("Set title: {}", title);
            }
            if (description != null && !description.isEmpty()) {
                By descSelector = By
                        .xpath("//ytcp-social-suggestions-textbox[@id='description-textarea']//div[@id='textbox']");
                WebElement descBox = findElement(driver, descSelector, "說明輸入框");

                log.info("執行 設定說明 操作");
                setText(descBox, description);
                log.info("Set description.");
            }
        } catch (Exception e) {
            log.error("Error setting metadata: {}", e.getMessage());
        }
    }

    private void setText(WebElement element, String text) {
        element.sendKeys(Keys.CONTROL + "a");
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(text);
    }

    private void selectPlaylist(WebDriver driver, String playlist) throws InterruptedException {
        if (playlist == null || playlist.isEmpty())
            return;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            // Click the dropdown trigger (arrow icon)
            By triggerSelector = By.xpath("//ytcp-text-dropdown-trigger//div[contains(@class, 'right-container')]");
            WebElement trigger = findClickableElement(driver, triggerSelector, "播放清單選單");

            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", trigger);
            log.info("執行 開啟播放清單選單 操作");
            js.executeScript("arguments[0].click();", trigger);
            log.info("Clicked playlist dropdown trigger.");

            // Wait for the list to be visible and select the item
            // The item is in an 'li' with class 'ytcp-checkbox-group' containing the label
            // text
            By itemSelector = By.xpath(
                    "//li[contains(@class, 'ytcp-checkbox-group') and .//span[contains(@class, 'label-text') and normalize-space(text())='"
                            + playlist + "']]//div[@id='checkbox-container']");
            WebElement item = findClickableElement(driver, itemSelector, "播放清單項目");

            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", item);
            log.info("執行 選擇播放清單 操作");
            item.click();
            log.info("Selected playlist: {}", playlist);

            // Click Done
            By doneSelector = By.xpath("//ytcp-button[.//div[contains(text(), 'Done') or contains(text(), '完成')]]");
            WebElement doneBtn = findClickableElement(driver, doneSelector, "完成按鈕");

            log.info("執行 點擊完成按鈕 操作");
            doneBtn.click();
            log.info("Clicked Done.");
        } catch (Exception e) {
            log.warn("Could not select playlist '{}': {}", playlist, e.getMessage());
        }
    }

    private void setKidsRestriction(WebDriver driver) {
        By selector = By.xpath("//tp-yt-paper-radio-button[@name='VIDEO_MADE_FOR_KIDS_NOT_MFK']");
        findClickableElement(driver, selector, "兒童選項").click();
        log.info("執行 設定兒童選項 操作");
    }

    private void navigateWizardPages(WebDriver driver) throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            By selector = By.id("next-button");
            findClickableElement(driver, selector, "下一步按鈕").click();
            log.info("執行 點擊下一步按鈕 操作");
        }
    }

    private void setVisibility(WebDriver driver, String visibility) {
        String vis = "PRIVATE";
        if ("PUBLIC".equalsIgnoreCase(visibility))
            vis = "PUBLIC";
        else if ("UNLISTED".equalsIgnoreCase(visibility))
            vis = "UNLISTED";

        By selector = By.xpath("//tp-yt-paper-radio-button[@name='" + vis + "']");
        findClickableElement(driver, selector, "公開性選項").click();
        log.info("已找到 公開性選項 : {}", selector);
        log.info("執行 設定公開性 操作");
    }

    private void saveAndClose(WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Wait for upload/processing to complete
        log.info("Waiting for video processing to complete...");
        while (true) {
            try {
                List<WebElement> statusElements = driver.findElements(By.xpath("//ytcp-video-upload-progress"));
                boolean isComplete = false;
                for (WebElement status : statusElements) {
                    String text = status.getText();
                    log.info("Current status: {}", text);
                    if ((text.contains("Checks complete") || text.contains("No issues found")
                            || text.contains("檢查完畢") || text.contains("處理完畢") || text.contains("無任何問題")
                            || text.contains("Upload complete") || text.contains("上傳完畢"))
                            && !text.contains("%")) {
                        isComplete = true;
                        break;
                    }
                }

                if (isComplete) {
                    log.info("Processing complete.");
                    break;
                }
            } catch (Exception e) {
                // Ignore
            }
            Thread.sleep(2000);
        }

        By doneSelector = By.id("done-button");
        findClickableElement(driver, doneSelector, "完成按鈕").click();
        log.info("執行 點擊完成按鈕 操作");
        log.info("Clicked Done button.");
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("close-button"))).click();
        } catch (Exception ignored) {
        }
        log.info("Video uploaded successfully!");
    }

    private void dispatchClickEvents(JavascriptExecutor js, WebElement element) {
        js.executeScript("""
                var evt1 = new MouseEvent('mousedown', {bubbles: true, cancelable: true, view: window});
                var evt2 = new MouseEvent('mouseup', {bubbles: true, cancelable: true, view: window});
                var evt3 = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                arguments[0].dispatchEvent(evt1);
                arguments[0].dispatchEvent(evt2);
                arguments[0].dispatchEvent(evt3);
                """, element);
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
