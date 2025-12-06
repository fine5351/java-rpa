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

    private static final List<String> AUTO_HASHTAG_KEYWORDS = List.of(
            "深境螺旋", "幻想真境劇詩", "虛構敘事", "忘卻之庭", "末日幻影",
            "式輿防衛戰", "零號空洞", "幽境危戰", "異相仲裁", "擬真鏖戰試煉", "危局強襲戰");

    public void uploadVideo(String filePath, String title, String description, String playlist, String visibility,
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
        } catch (Exception e) {
            log.error("Error during YouTube upload", e);
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
            for (String keyword : AUTO_HASHTAG_KEYWORDS) {
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
        } else {
            throw new RuntimeException("Could not find any Upload/Create button.");
        }
    }

    private WebElement findUploadButton(JavascriptExecutor js) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
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
        }
        return null;
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

        log.info("尋找 上傳影片選項 位置中");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            By selector = By
                    .xpath("//tp-yt-paper-item[.//div[contains(text(),'Upload videos') or contains(text(),'上傳影片')]]");
            WebElement uploadOption = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions
                    .presenceOfElementLocated(selector));
            log.info("已找到 上傳影片選項 : {}", selector);
            log.info("執行 點擊上傳影片選項 操作");
            dispatchClickEvents(js, uploadOption);
        } catch (Exception e) {
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
            }
        }
    }

    private void uploadFile(WebDriver driver, String filePath) {
        log.info("尋找 上傳檔案輸入框 位置中");
        By selector = By.xpath("//input[@type='file']");
        WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.presenceOfElementLocated(selector));
        log.info("已找到 上傳檔案輸入框 : {}", selector);
        log.info("執行 上傳檔案 操作");
        fileInput.sendKeys(filePath);
        log.info("Sent file path: {}", filePath);
    }

    private void enterTitleAndDescription(WebDriver driver, String title, String description) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        try {
            if (title != null && !title.isEmpty()) {
                log.info("尋找 標題輸入框 位置中");
                By titleSelector = By
                        .xpath("//ytcp-social-suggestions-textbox[@id='title-textarea']//div[@id='textbox']");
                WebElement titleBox = wait.until(ExpectedConditions.presenceOfElementLocated(titleSelector));
                log.info("已找到 標題輸入框 : {}", titleSelector);
                log.info("執行 設定標題 操作");
                setText(titleBox, title);
                log.info("Set title: {}", title);
            }
            if (description != null && !description.isEmpty()) {
                log.info("尋找 說明輸入框 位置中");
                By descSelector = By
                        .xpath("//ytcp-social-suggestions-textbox[@id='description-textarea']//div[@id='textbox']");
                WebElement descBox = wait.until(ExpectedConditions.presenceOfElementLocated(descSelector));
                log.info("已找到 說明輸入框 : {}", descSelector);
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            // Click the dropdown trigger (arrow icon)
            log.info("尋找 播放清單選單 位置中");
            By triggerSelector = By.xpath("//ytcp-text-dropdown-trigger//div[contains(@class, 'right-container')]");
            WebElement trigger = wait.until(ExpectedConditions.elementToBeClickable(triggerSelector));
            log.info("已找到 播放清單選單 : {}", triggerSelector);

            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", trigger);
            log.info("執行 開啟播放清單選單 操作");
            js.executeScript("arguments[0].click();", trigger);
            log.info("Clicked playlist dropdown trigger.");

            // Wait for the list to be visible and select the item
            // The item is in an 'li' with class 'ytcp-checkbox-group' containing the label
            // text
            log.info("尋找 播放清單項目 位置中");
            By itemSelector = By.xpath(
                    "//li[contains(@class, 'ytcp-checkbox-group') and .//span[contains(@class, 'label-text') and normalize-space(text())='"
                            + playlist + "']]//div[@id='checkbox-container']");
            WebElement item = wait.until(ExpectedConditions.elementToBeClickable(itemSelector));
            log.info("已找到 播放清單項目 : {}", itemSelector);

            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", item);
            log.info("執行 選擇播放清單 操作");
            item.click();
            log.info("Selected playlist: {}", playlist);

            // Click Done
            log.info("尋找 完成按鈕 位置中");
            By doneSelector = By.xpath("//ytcp-button[.//div[contains(text(), 'Done') or contains(text(), '完成')]]");
            WebElement doneBtn = wait.until(ExpectedConditions.elementToBeClickable(doneSelector));
            log.info("已找到 完成按鈕 : {}", doneSelector);
            log.info("執行 點擊完成按鈕 操作");
            doneBtn.click();
            log.info("Clicked Done.");
        } catch (Exception e) {
            log.warn("Could not select playlist '{}': {}", playlist, e.getMessage());
        }
    }

    private void setKidsRestriction(WebDriver driver) {
        log.info("尋找 兒童選項 位置中");
        By selector = By.xpath("//tp-yt-paper-radio-button[@name='VIDEO_MADE_FOR_KIDS_NOT_MFK']");
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(selector))
                .click();
        log.info("已找到 兒童選項 : {}", selector);
        log.info("執行 設定兒童選項 操作");
    }

    private void navigateWizardPages(WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        for (int i = 0; i < 3; i++) {
            log.info("尋找 下一步按鈕 位置中");
            By selector = By.id("next-button");
            wait.until(ExpectedConditions.elementToBeClickable(selector)).click();
            log.info("已找到 下一步按鈕 : {}", selector);
            log.info("執行 點擊下一步按鈕 操作");
        }
    }

    private void setVisibility(WebDriver driver, String visibility) {
        String vis = "PRIVATE";
        if ("PUBLIC".equalsIgnoreCase(visibility))
            vis = "PUBLIC";
        else if ("UNLISTED".equalsIgnoreCase(visibility))
            vis = "UNLISTED";

        log.info("尋找 公開性選項 位置中");
        By selector = By.xpath("//tp-yt-paper-radio-button[@name='" + vis + "']");
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(selector))
                .click();
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

        log.info("尋找 完成按鈕 位置中");
        By doneSelector = By.id("done-button");
        wait.until(ExpectedConditions.elementToBeClickable(doneSelector)).click();
        log.info("已找到 完成按鈕 : {}", doneSelector);
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
}
