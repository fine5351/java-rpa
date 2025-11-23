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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

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
            List<String> hashtags) {
        String finalDescription = buildDescription(title, description, hashtags);
        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToStudio(driver);
            clickCreateButton(driver);
            selectUploadOption(driver);
            uploadFile(driver, filePath);
            Thread.sleep(5000); // Wait for wizard
            enterTitleAndDescription(driver, title, finalDescription);
            selectPlaylist(driver, playlist);
            setKidsRestriction(driver);
            navigateWizardPages(driver);
            setVisibility(driver, visibility);
            saveAndClose(driver);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private String buildDescription(String title, String description, List<String> hashtags) {
        if (description == null)
            description = "";

        if (title != null) {
            for (String keyword : AUTO_HASHTAG_KEYWORDS) {
                if (title.contains(keyword)) {
                    description += " #" + keyword;
                }
            }
        }

        if (hashtags != null) {
            for (String tag : hashtags) {
                if (!description.contains(tag))
                    description += " " + tag;
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

    private void navigateToStudio(WebDriver driver) throws InterruptedException {
        driver.get("https://studio.youtube.com");
        Thread.sleep(5000);
        try {
            WebElement continueButton = driver.findElement(
                    By.xpath("//tp-yt-paper-button[@id='button' and .//div[contains(text(),'Continue')]]"));
            if (continueButton.isDisplayed()) {
                continueButton.click();
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {
        }
    }

    private void clickCreateButton(WebDriver driver) throws Exception {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement uploadButton = findUploadButton(js);
        if (uploadButton != null) {
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", uploadButton);
            Thread.sleep(500);
            uploadButton = adjustButtonTarget(uploadButton);
            dispatchClickEvents(js, uploadButton);
            System.out.println("Dispatched click events to Upload button.");
            Thread.sleep(2000);
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
            Thread.sleep(1000);
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

        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            WebElement uploadOption = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions
                    .presenceOfElementLocated(By.xpath(
                            "//tp-yt-paper-item[.//div[contains(text(),'Upload videos') or contains(text(),'上傳影片')]]")));
            dispatchClickEvents(js, uploadOption);
        } catch (Exception e) {
            String script = FIND_ELEMENT_RECURSIVE_SCRIPT + """
                    var opt = findElementRecursive(startNode, null, 'Upload videos', null, null);
                    if (!opt) opt = findElementRecursive(startNode, null, '上傳影片', null, null);
                    return opt;
                    """;
            WebElement uploadOption = (WebElement) js.executeScript(script);
            if (uploadOption != null)
                dispatchClickEvents(js, uploadOption);
        }
    }

    private void uploadFile(WebDriver driver, String filePath) {
        System.out.println("Waiting for file input...");
        WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
        fileInput.sendKeys(filePath);
        System.out.println("Sent file path: " + filePath);
    }

    private void enterTitleAndDescription(WebDriver driver, String title, String description) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        try {
            if (title != null && !title.isEmpty()) {
                WebElement titleBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//ytcp-social-suggestions-textbox[@id='title-textarea']//div[@id='textbox']")));
                setText(titleBox, title);
                System.out.println("Set title: " + title);
            }
            if (description != null && !description.isEmpty()) {
                WebElement descBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//ytcp-social-suggestions-textbox[@id='description-textarea']//div[@id='textbox']")));
                setText(descBox, description);
                System.out.println("Set description.");
            }
        } catch (Exception e) {
            System.out.println("Error setting metadata: " + e.getMessage());
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
            WebElement trigger = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            "//ytcp-text-dropdown-trigger[.//span[contains(text(),'Select') or contains(text(),'選取')]]")));
            js.executeScript("arguments[0].click();", trigger);
            Thread.sleep(1000);
            WebElement item = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//ytcp-ve[.//span[contains(text(), '" + playlist
                            + "')]]//div[@id='checkbox-container']")));
            item.click();
            WebElement doneBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//ytcp-button[.//div[contains(text(), 'Done') or contains(text(), '完成')]]")));
            doneBtn.click();
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Could not find playlist: " + playlist);
        }
    }

    private void setKidsRestriction(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(
                By.xpath("//tp-yt-paper-radio-button[@name='VIDEO_MADE_FOR_KIDS_NOT_MFK']"))).click();
    }

    private void navigateWizardPages(WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        for (int i = 0; i < 3; i++) {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("next-button"))).click();
            Thread.sleep(1000);
        }
    }

    private void setVisibility(WebDriver driver, String visibility) {
        String vis = "PRIVATE";
        if ("PUBLIC".equalsIgnoreCase(visibility))
            vis = "PUBLIC";
        else if ("UNLISTED".equalsIgnoreCase(visibility))
            vis = "UNLISTED";

        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(
                By.xpath("//tp-yt-paper-radio-button[@name='" + vis + "']"))).click();
    }

    private void saveAndClose(WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("done-button"))).click();
        System.out.println("Clicked Done button.");
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("close-button"))).click();
        } catch (Exception ignored) {
        }
        System.out.println("Video uploaded successfully! Waiting 60s...");
        Thread.sleep(60000);
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
