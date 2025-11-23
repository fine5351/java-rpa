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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TikTokService {

    private static final List<String> AUTO_HASHTAG_KEYWORDS = List.of(
            "深境螺旋", "幻想真境劇詩", "虛構敘事", "忘卻之庭", "末日幻影",
            "式輿防衛戰", "零號空洞", "幽境危戰", "異相仲裁", "擬真鏖戰試煉", "危局強襲戰");

    public void uploadVideo(String filePath, String title, String description, String visibility,
            List<String> hashtags) {
        String caption = buildBaseCaption(title, description, hashtags);
        List<String> autoTags = getAutoHashtags(caption);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToUploadPage(driver);
            uploadFile(driver, filePath);
            Thread.sleep(10000); // Initial wait for upload
            setCaption(driver, caption, autoTags);
            setVisibility(driver, visibility);
            clickPostButton(driver);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private String buildBaseCaption(String title, String description, List<String> hashtags) {
        StringBuilder caption = new StringBuilder();
        if (title != null && !title.isEmpty())
            caption.append(title);
        if (description != null && !description.isEmpty()) {
            if (!caption.isEmpty())
                caption.append("\n\n");
            caption.append(description);
        }
        if (hashtags != null && !hashtags.isEmpty()) {
            if (!caption.isEmpty())
                caption.append("\n\n");
            for (String tag : hashtags)
                caption.append(" ").append(tag);
        }
        return caption.toString();
    }

    private List<String> getAutoHashtags(String caption) {
        List<String> tagsToAdd = new ArrayList<>();
        for (String keyword : AUTO_HASHTAG_KEYWORDS) {
            if (caption.contains(keyword)) {
                tagsToAdd.add("#" + keyword);
            }
        }
        return tagsToAdd;
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

    private void navigateToUploadPage(WebDriver driver) throws InterruptedException {
        System.out.println("Navigating to TikTok Upload...");
        driver.get("https://www.tiktok.com/tiktokstudio/upload");
        Thread.sleep(5000);
    }

    private void uploadFile(WebDriver driver, String filePath) {
        System.out.println("Waiting for file input...");
        try {
            WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
            fileInput.sendKeys(filePath);
            System.out.println("Sent file path: " + filePath);
        } catch (Exception e) {
            System.out.println("Could not find file input immediately.");
            throw e;
        }
    }

    private void setCaption(WebDriver driver, String caption, List<String> autoTags) {
        if (caption == null || caption.isEmpty())
            return;
        System.out.println("Setting caption...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            WebElement captionEditor = findCaptionEditor(driver, wait);
            clearAndTypeCaption(captionEditor, caption);
            addAutoTags(driver, captionEditor, autoTags);
            System.out.println("Caption set.");
        } catch (Exception e) {
            System.out.println("Failed to set caption: " + e.getMessage());
        }
    }

    private WebElement findCaptionEditor(WebDriver driver, WebDriverWait wait) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(@class, 'DraftEditor-editorContainer')]//div[@contenteditable='true']")));
        } catch (Exception e) {
            System.out.println("Using fallback caption editor locator...");
            return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@contenteditable='true']")));
        }
    }

    private void clearAndTypeCaption(WebElement editor, String text) {
        editor.click();
        editor.sendKeys(Keys.CONTROL + "a");
        editor.sendKeys(Keys.BACK_SPACE);
        editor.sendKeys(text);
    }

    private void addAutoTags(WebDriver driver, WebElement editor, List<String> autoTags) throws InterruptedException {
        for (String tag : autoTags) {
            editor.sendKeys(" " + tag);
            Thread.sleep(1500);
            selectSuggestion(driver, tag);
        }
    }

    private void selectSuggestion(WebDriver driver, String tag) {
        try {
            List<WebElement> candidates = driver.findElements(By.xpath("//*[contains(text(), '" + tag + "')]"));
            for (WebElement cand : candidates) {
                if (cand.isDisplayed() && !isEditorElement(cand)) {
                    System.out.println("Found suggestion for " + tag + ", clicking...");
                    cand.click();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not select suggestion for " + tag);
        }
    }

    private boolean isEditorElement(WebElement element) {
        String cls = element.getAttribute("class");
        String ce = element.getAttribute("contenteditable");
        return (cls != null && cls.contains("public-Draft")) || "true".equals(ce);
    }

    private void setVisibility(WebDriver driver, String visibility) {
        if (visibility == null)
            return;
        System.out.println("Setting visibility to: " + visibility);
        String visibilityText = "Everyone";
        if ("FRIENDS".equalsIgnoreCase(visibility))
            visibilityText = "Friends";
        if ("PRIVATE".equalsIgnoreCase(visibility))
            visibilityText = "Only you";

        try {
            WebElement visOption = driver.findElement(By.xpath("//div[contains(text(), '" + visibilityText + "')]"));
            if (visOption.isDisplayed()) {
                visOption.click();
            } else {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click()", visOption);
            }
        } catch (Exception e) {
            System.out.println("Could not set visibility automatically. Using default.");
        }
    }

    private void clickPostButton(WebDriver driver) throws Exception {
        System.out.println("Waiting for upload to complete and Post button to be ready...");
        WebElement postButton = waitForPostButtonReady(driver);

        if (postButton != null) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", postButton);
            Thread.sleep(1000);
            try {
                postButton.click();
                System.out.println("Clicked Post button (Standard).");
            } catch (Exception e) {
                js.executeScript("arguments[0].click();", postButton);
                System.out.println("Clicked Post button (JS).");
            }
            handleModal(driver);
            waitForSuccess(driver);
        } else {
            throw new RuntimeException("Post button never became enabled or upload never finished.");
        }
        System.out.println("TikTok upload sequence finished.");
        Thread.sleep(15000);
    }

    private WebElement waitForPostButtonReady(WebDriver driver) throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            try {
                boolean uploaded = isUploadComplete(driver);
                WebElement btn = findPostButton(driver);
                boolean enabled = isButtonEnabled(btn);
                boolean uploading = isUploading(driver);

                if (uploaded && enabled && !uploading) {
                    System.out.println("Upload complete and Post button ready.");
                    return btn;
                }
                if (i % 5 == 0)
                    System.out.println("Waiting... (Uploaded: " + uploaded + ", Enabled: " + enabled + ")");
            } catch (Exception ignored) {
            }
            Thread.sleep(1000);
        }
        return null;
    }

    private boolean isUploadComplete(WebDriver driver) {
        try {
            WebElement status = driver
                    .findElement(By.xpath("/html/body/div[1]/div/div/div[2]/div[2]/div/div/div/div[1]/div"));
            String text = status.getText();
            return text.contains("Uploaded") || text.contains("已上傳");
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement findPostButton(WebDriver driver) {
        try {
            return driver
                    .findElement(By.xpath("/html/body/div[1]/div/div/div[2]/div[2]/div/div/div/div[5]/div/button[1]"));
        } catch (Exception e) {
            return driver.findElement(By.xpath("//button[.//div[contains(text(), 'Post') or contains(text(), '發佈')]]"));
        }
    }

    private boolean isButtonEnabled(WebElement btn) {
        String disabled = btn.getAttribute("disabled");
        String cls = btn.getAttribute("class");
        return (disabled == null || !"true".equals(disabled)) &&
                (cls == null || (!cls.contains("disabled") && !cls.contains("disable")
                        && !cls.contains("TUXButton--disabled")));
    }

    private boolean isUploading(WebDriver driver) {
        try {
            return driver.findElement(By.xpath(
                    "//div[contains(text(), 'Uploading') or contains(text(), '上傳中') or contains(text(), '正在檢查')]"))
                    .isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private void handleModal(WebDriver driver) {
        try {
            Thread.sleep(2000);
            WebElement btn = driver
                    .findElement(By.xpath("//button[contains(., '立即發佈') or contains(., 'Post immediately')]"));
            if (btn.isDisplayed()) {
                btn.click();
                System.out.println("Clicked 'Post immediately' modal.");
            }
        } catch (Exception ignored) {
        }
    }

    private void waitForSuccess(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("""
                            //div[contains(text(), 'Video uploaded') or contains(text(), '影片已上傳')
                            or contains(text(), 'Manage posts') or contains(text(), '管理貼文')
                            or contains(text(), 'Upload another video') or contains(text(), '上傳另一支影片')]
                            """)));
            System.out.println("Success indicator found.");
        } catch (Exception e) {
            System.out.println("Proceeding without specific success text.");
        }
    }
}
