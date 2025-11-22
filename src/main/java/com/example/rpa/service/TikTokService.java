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

@Service
public class TikTokService {

    public void uploadVideo(String filePath, String title, String description, String visibility,
            java.util.List<String> hashtags) {
        // Combine title and description for TikTok caption
        String caption = "";
        if (title != null && !title.isEmpty()) {
            caption += title;
        }
        if (description != null && !description.isEmpty()) {
            if (!caption.isEmpty()) {
                caption += "\n\n";
            }
            caption += description;
        }

        // Append explicit hashtags
        if (hashtags != null && !hashtags.isEmpty()) {
            if (!caption.isEmpty())
                caption += "\n\n";
            for (String tag : hashtags) {
                caption += " " + tag;
            }
        }
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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. Go to TikTok Upload Page
            System.out.println("Navigating to TikTok Upload...");
            driver.get("https://www.tiktok.com/tiktokstudio/upload");

            // Wait for potential redirect or load
            Thread.sleep(5000);

            // 2. Upload File
            // Look for the file input. It's usually an input type='file' hidden somewhere.
            System.out.println("Waiting for file input...");
            try {
                WebElement fileInput = wait
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
                fileInput.sendKeys(filePath);
                System.out.println("Sent file path: " + filePath);
            } catch (Exception e) {
                // Fallback: sometimes it's inside an iframe or requires clicking a button
                // first?
                // Usually TikTok has a direct input on the upload page.
                System.out.println(
                        "Could not find file input immediately. Checking if we need to click 'Select file' first.");
                throw e;
            }

            // 3. Wait for upload to process
            // There's usually a progress bar or a change in UI.
            // We can wait for the "Caption" box to be interactable or the video preview to
            // appear.
            System.out.println("Waiting for upload to process...");
            Thread.sleep(10000); // Initial wait for upload

            // 4. Set Caption
            if (caption != null && !caption.isEmpty()) {
                System.out.println("Setting caption...");

                // Identify hashtags to add
                java.util.List<String> tagsToAdd = new java.util.ArrayList<>();
                // Genshin
                if (caption.contains("深境螺旋"))
                    tagsToAdd.add("#深境螺旋");
                if (caption.contains("幻想真境劇詩"))
                    tagsToAdd.add("#幻想真境劇詩");
                // Star Rail
                if (caption.contains("虛構敘事"))
                    tagsToAdd.add("#虛構敘事");
                if (caption.contains("忘卻之庭"))
                    tagsToAdd.add("#忘卻之庭");
                if (caption.contains("末日幻影"))
                    tagsToAdd.add("#末日幻影");
                // ZZZ
                if (caption.contains("式輿防衛戰"))
                    tagsToAdd.add("#式輿防衛戰");
                if (caption.contains("零號空洞"))
                    tagsToAdd.add("#零號空洞");
                // Wuthering Waves / Others
                if (caption.contains("幽境危戰"))
                    tagsToAdd.add("#幽境危戰");
                if (caption.contains("異相仲裁"))
                    tagsToAdd.add("#異相仲裁");
                if (caption.contains("擬真鏖戰試煉"))
                    tagsToAdd.add("#擬真鏖戰試煉");
                if (caption.contains("危局強襲戰"))
                    tagsToAdd.add("#危局強襲戰");

                // TikTok caption area is often a contenteditable div.
                // Try to find it by class or structure.
                // Common selector for TikTok Studio caption: .public-DraftEditor-content or
                // similar

                // Strategy: Find the editor div.
                // It often has "Caption" label nearby.
                try {
                    WebElement captionEditor = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//div[contains(@class, 'DraftEditor-editorContainer')]//div[@contenteditable='true']")));

                    // Clear existing (if any) and type
                    captionEditor.click();
                    // Clear might be tricky with contenteditable, use Ctrl+A, Backspace
                    captionEditor.sendKeys(Keys.CONTROL + "a");
                    captionEditor.sendKeys(Keys.BACK_SPACE);

                    // Type original caption
                    captionEditor.sendKeys(caption);

                    // Type hashtags interactively
                    for (String tag : tagsToAdd) {
                        captionEditor.sendKeys(" " + tag);
                        Thread.sleep(1500); // Wait for suggestion dropdown

                        try {
                            // Try to find the suggestion in the dropdown
                            // We look for a visible element containing the tag text.
                            // We exclude the editor itself by checking for specific classes or location if
                            // possible,
                            // but for now, we'll try to find the last element with that text or a specific
                            // suggestion class.
                            // TikTok suggestions often appear in a div with class containing 'suggestion'
                            // or similar,
                            // or just a floating div at the bottom.

                            // Strategy: Find all elements with the tag text, filter for visible and NOT the
                            // editor.
                            java.util.List<WebElement> candidates = driver
                                    .findElements(By.xpath("//*[contains(text(), '" + tag + "')]"));
                            for (WebElement cand : candidates) {
                                if (cand.isDisplayed()) {
                                    // Check if it's the editor
                                    String candClass = cand.getAttribute("class");
                                    String contentEditable = cand.getAttribute("contenteditable");

                                    if ((candClass == null || !candClass.contains("public-Draft")) &&
                                            (contentEditable == null || !"true".equals(contentEditable))) {

                                        // It might be the suggestion. Click it.
                                        System.out.println("Found suggestion for " + tag + ", clicking...");
                                        cand.click();
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Could not select suggestion for " + tag);
                        }
                    }

                    System.out.println("Caption set.");

                } catch (Exception e) {
                    System.out.println(
                            "Could not find caption editor via standard class. Trying generic contenteditable.");
                    try {
                        WebElement captionEditor = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.xpath("//div[@contenteditable='true']")));
                        captionEditor.sendKeys(caption);
                        // Fallback for generic editor: just type tags
                        for (String tag : tagsToAdd) {
                            captionEditor.sendKeys(" " + tag);
                        }
                    } catch (Exception ex) {
                        System.out.println("Failed to set caption: " + ex.getMessage());
                    }
                }
            }

            // 5. Set Visibility
            // Default is usually Public.
            if (visibility != null) {
                System.out.println("Setting visibility to: " + visibility);
                // Find the dropdown or radio buttons.
                // TikTok Studio often uses a dropdown or radio group.
                // Assuming it might be a dropdown text "Who can watch this video"

                // This part is highly DOM specific and might need adjustment.
                // For now, we'll try to find a radio/checkbox with the text.
                try {
                    String visibilityText = "Everyone"; // Default
                    if ("FRIENDS".equalsIgnoreCase(visibility))
                        visibilityText = "Friends";
                    if ("PRIVATE".equalsIgnoreCase(visibility))
                        visibilityText = "Only you";

                    // Try to find an element with this text that looks clickable
                    WebElement visOption = driver
                            .findElement(By.xpath("//div[contains(text(), '" + visibilityText + "')]"));
                    if (visOption.isDisplayed()) {
                        visOption.click();
                    } else {
                        // Might need to open a dropdown first
                        // ... skipping complex dropdown logic for MVP, assuming default or manual check
                        System.out.println("Visibility option found but might be hidden. Attempting JS click.");
                        js.executeScript("arguments[0].click()", visOption);
                    }
                } catch (Exception e) {
                    System.out.println("Could not set visibility automatically. Using default.");
                }
            }

            // 6. Post
            System.out.println("Waiting for upload to complete and Post button to be ready...");

            WebElement postButton = null;
            boolean readyToClick = false;

            // Wait up to 300 seconds (5 minutes) for large files
            for (int i = 0; i < 300; i++) {
                try {
                    // 1. Check Status Text (User provided XPath)
                    boolean isUploadCompleteTextFound = false;
                    try {
                        WebElement statusElement = driver.findElement(
                                By.xpath("/html/body/div[1]/div/div/div[2]/div[2]/div/div/div/div[1]/div"));
                        String statusText = statusElement.getText();
                        if (statusText.contains("Uploaded") || statusText.contains("已上傳")) {
                            isUploadCompleteTextFound = true;
                        } else {
                            if (i % 5 == 0)
                                System.out.println("Current status text: " + statusText);
                        }
                    } catch (Exception ignored) {
                        // Status element might not be there yet
                    }

                    // 2. Find Post Button
                    try {
                        postButton = driver.findElement(
                                By.xpath("/html/body/div[1]/div/div/div[2]/div[2]/div/div/div/div[5]/div/button[1]"));
                    } catch (Exception e1) {
                        postButton = driver.findElement(
                                By.xpath("//button[.//div[contains(text(), 'Post') or contains(text(), '發佈')]]"));
                    }

                    // 3. Check Button State
                    String disabledAttr = postButton.getAttribute("disabled");
                    String classAttr = postButton.getAttribute("class");

                    boolean isDisabled = (disabledAttr != null && "true".equals(disabledAttr)) ||
                            (disabledAttr != null && "".equals(disabledAttr)) ||
                            (classAttr != null && (classAttr.contains("disabled") || classAttr.contains("disable")
                                    || classAttr.contains("TUXButton--disabled")));

                    // 4. Check for "Uploading" text (Generic fallback)
                    boolean isUploading = false;
                    try {
                        WebElement uploadingText = driver.findElement(By.xpath(
                                "//div[contains(text(), 'Uploading') or contains(text(), '上傳中') or contains(text(), '正在檢查')]"));
                        if (uploadingText.isDisplayed()) {
                            isUploading = true;
                        }
                    } catch (Exception ignore) {
                    }

                    // Condition: Must have "Uploaded" text AND Button must be enabled
                    if (isUploadCompleteTextFound && !isDisabled && postButton.isEnabled() && !isUploading) {
                        System.out.println("Upload complete (Verified '已上傳') and Post button ready.");
                        readyToClick = true;
                        break;
                    } else {
                        if (i % 5 == 0)
                            System.out.println("Waiting... (Uploaded Text Found: " + isUploadCompleteTextFound
                                    + ", Button Enabled: " + (!isDisabled && postButton.isEnabled()) + ")");
                    }
                } catch (Exception e) {
                    if (i % 5 == 0)
                        System.out.println("Waiting for elements...");
                }
                Thread.sleep(1000);
            }

            if (readyToClick && postButton != null) {
                // Scroll to view
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", postButton);
                Thread.sleep(1000);

                // Try multiple click strategies
                try {
                    postButton.click();
                    System.out.println("Clicked Post button (Standard).");
                } catch (Exception e) {
                    System.out.println("Standard click failed, trying JS click...");
                    js.executeScript("arguments[0].click();", postButton);
                    System.out.println("Clicked Post button (JS).");
                }

                // Handle "Copyright check not complete" modal (e.g. "立即發佈")
                try {
                    System.out.println("Checking for potential modal...");
                    Thread.sleep(2000); // Wait for modal animation
                    WebElement continueButton = null;
                    try {
                        // Look for "Post immediately" or "立即發佈"
                        continueButton = driver.findElement(
                                By.xpath("//button[contains(., '立即發佈') or contains(., 'Post immediately')]"));
                    } catch (Exception e) {
                        // Modal might not appear, which is fine
                    }

                    if (continueButton != null && continueButton.isDisplayed()) {
                        System.out.println("Modal appeared. Clicking 'Post immediately'...");
                        continueButton.click();
                    }
                } catch (Exception e) {
                    System.out.println("Error handling modal: " + e.getMessage());
                }

                // Wait for success confirmation
                System.out.println("Waiting for success confirmation...");
                try {
                    WebElement successIndicator = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[contains(text(), 'Video uploaded') or contains(text(), '影片已上傳') " +
                                    "or contains(text(), 'Manage posts') or contains(text(), '管理貼文') " +
                                    "or contains(text(), 'Upload another video') or contains(text(), '上傳另一支影片')]")));
                    System.out.println("Found success indicator: " + successIndicator.getText());
                } catch (Exception e) {
                    System.out.println("Did not find specific success text, but proceeding after wait.");
                }

            } else {
                throw new RuntimeException("Post button never became enabled or upload never finished.");
            }

            System.out.println("TikTok upload sequence finished.");

            // Keep open for a bit to ensure network requests finish
            Thread.sleep(15000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
