package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
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

@Service
public class YouTubeService {

    public void uploadVideo(String filePath, String title, String description, String playlist, String visibility,
            java.util.List<String> hashtags) {
        // Auto-append hashtags based on title
        if (title != null) {
            if (description == null) {
                description = "";
            }
            // Genshin
            if (title.contains("深境螺旋"))
                description += " #深境螺旋";
            if (title.contains("幻想真境劇詩"))
                description += " #幻想真境劇詩";

            // Star Rail
            if (title.contains("虛構敘事"))
                description += " #虛構敘事";
            if (title.contains("忘卻之庭"))
                description += " #忘卻之庭";
            if (title.contains("末日幻影"))
                description += " #末日幻影";

            // ZZZ
            if (title.contains("式輿防衛戰"))
                description += " #式輿防衛戰";
            if (title.contains("零號空洞"))
                description += " #零號空洞";

            // Wuthering Waves / Others
            if (title.contains("幽境危戰"))
                description += " #幽境危戰";
            if (title.contains("異相仲裁"))
                description += " #異相仲裁";
            if (title.contains("擬真鏖戰試煉"))
                description += " #擬真鏖戰試煉";
            if (title.contains("危局強襲戰"))
                description += " #危局強襲戰";
        }

        // Append explicit hashtags
        if (hashtags != null && !hashtags.isEmpty()) {
            if (description == null)
                description = "";
            for (String tag : hashtags) {
                if (!description.contains(tag)) {
                    description += " " + tag;
                }
            }
        }

        // Setup WebDriver
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // STRATEGY: Use a dedicated Chrome profile for this RPA tool.
        String userDataDir = "d:/work/workspace/java/rpa/chrome-data";
        options.addArguments("user-data-dir=" + userDataDir);

        // Anti-detection flags
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));

        // Stability flags
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;

            // 1. Go to YouTube Studio
            driver.get("https://studio.youtube.com");
            Thread.sleep(5000); // Wait for full load

            // 0. Handle "Welcome to YouTube Studio" popup if it exists
            try {
                WebElement continueButton = driver.findElement(
                        By.xpath("//tp-yt-paper-button[@id='button' and .//div[contains(text(),'Continue')]]"));
                if (continueButton.isDisplayed()) {
                    continueButton.click();
                    System.out.println("Dismissed 'Welcome' dialog.");
                    Thread.sleep(1000);
                }
            } catch (Exception ignored) {
                // Popup not found, ignore
            }

            // 2. Click Upload Button
            System.out.println("Attempting to find Upload/Create button...");

            // Define a robust JS function to find elements by piercing Shadow DOMs
            String findElementRecursiveScript = """
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

            WebElement uploadButton = null;
            boolean isQuickAction = false;

            for (int i = 0; i < 10; i++) {
                try {
                    // STRATEGY A: Dashboard "Quick Action" Upload Button
                    String quickActionScript = findElementRecursiveScript + """
                            var qa = findElementRecursive(startNode, null, null, null, 'YTCP-QUICK-ACTIONS');
                            if (qa) {
                              return qa.querySelector('ytcp-icon-button');
                            }
                            return null;
                            """;

                    uploadButton = (WebElement) js.executeScript(quickActionScript);
                    if (uploadButton != null) {
                        System.out.println("Found Dashboard Quick Action Upload button.");
                        isQuickAction = true;
                        break;
                    }

                    // STRATEGY B: Header "Create" Button (ID: create-icon)
                    String createIconScript = findElementRecursiveScript + """
                            return findElementRecursive(startNode, 'create-icon', null, null, null);
                            """;

                    uploadButton = (WebElement) js.executeScript(createIconScript);
                    if (uploadButton != null) {
                        System.out.println("Found Header Create button (by ID).");
                        break;
                    }

                    // STRATEGY C: Header "Create" Button (Class:
                    // yt-spec-touch-feedback-shape__fill)
                    String classScript = findElementRecursiveScript
                            + """
                                    return findElementRecursive(startNode, null, null, 'yt-spec-touch-feedback-shape__fill', null);
                                    """;

                    uploadButton = (WebElement) js.executeScript(classScript);
                    if (uploadButton != null) {
                        System.out.println("Found Header Create button (by Class).");
                        break;
                    }

                } catch (Exception e) {
                    // Ignore errors during search
                }
                Thread.sleep(1000);
            }

            if (uploadButton != null) {
                // Scroll into view
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", uploadButton);
                Thread.sleep(500);

                // If we found the inner class element (Strategy C), traverse up to the button
                if (!isQuickAction) {
                    String tagName = uploadButton.getTagName();
                    if (!tagName.equalsIgnoreCase("YTCP-BUTTON") && !tagName.equalsIgnoreCase("TP-YT-PAPER-ICON-BUTTON")
                            && !tagName.equalsIgnoreCase("YTCP-ICON-BUTTON")) {
                        try {
                            WebElement parent = uploadButton.findElement(By.xpath("./.."));
                            if (parent != null)
                                uploadButton = parent;
                        } catch (Exception ignore) {
                        }
                    }
                }

                // Dispatch events
                js.executeScript("""
                        var evt1 = new MouseEvent('mousedown', {bubbles: true, cancelable: true, view: window});
                        var evt2 = new MouseEvent('mouseup', {bubbles: true, cancelable: true, view: window});
                        var evt3 = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                        arguments[0].dispatchEvent(evt1);
                        arguments[0].dispatchEvent(evt2);
                        arguments[0].dispatchEvent(evt3);
                        """,
                        uploadButton);

                System.out.println("Dispatched click events to Upload button.");
            } else {
                throw new RuntimeException("Could not find any Upload/Create button.");
            }

            Thread.sleep(2000);

            // Check if we need to click "Upload videos" in the menu
            boolean fileInputFound = false;
            try {
                WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
                fileInputFound = true;
            } catch (Exception e) {
                // Not found yet
            }

            if (!fileInputFound) {
                System.out.println("File input not found yet, checking for 'Upload videos' menu item...");
                try {
                    WebElement uploadOption = wait.until(ExpectedConditions
                            .presenceOfElementLocated(By.xpath(
                                    "//tp-yt-paper-item[.//div[contains(text(),'Upload videos') or contains(text(),'上傳影片')]]")));

                    js.executeScript("""
                            var evt1 = new MouseEvent('mousedown', {bubbles: true, cancelable: true, view: window});
                            var evt2 = new MouseEvent('mouseup', {bubbles: true, cancelable: true, view: window});
                            var evt3 = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                            arguments[0].dispatchEvent(evt1);
                            arguments[0].dispatchEvent(evt2);
                            arguments[0].dispatchEvent(evt3);
                            """,
                            uploadOption);
                    System.out.println("Clicked Upload Videos option.");
                } catch (Exception e) {
                    System.out.println("Could not find Upload Videos option via XPath. Trying JS recursive...");
                    String findUploadOptionScript = findElementRecursiveScript + """
                            var opt = findElementRecursive(startNode, null, 'Upload videos', null, null);
                            if (!opt) opt = findElementRecursive(startNode, null, '上傳影片', null, null);
                            return opt;
                            """;

                    WebElement uploadOption = (WebElement) js.executeScript(findUploadOptionScript);
                    if (uploadOption != null) {
                        js.executeScript("""
                                var evt1 = new MouseEvent('mousedown', {bubbles: true, cancelable: true, view: window});
                                var evt2 = new MouseEvent('mouseup', {bubbles: true, cancelable: true, view: window});
                                var evt3 = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                                arguments[0].dispatchEvent(evt1);
                                arguments[0].dispatchEvent(evt2);
                                arguments[0].dispatchEvent(evt3);
                                """,
                                uploadOption);
                        System.out.println("Clicked Upload Videos option (via JS).");
                    }
                }
            }

            // 3. Upload File
            System.out.println("Waiting for file input...");
            WebElement fileInput = wait
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
            fileInput.sendKeys(filePath);
            System.out.println("Sent file path: " + filePath);

            // 4. Handle Upload Wizard
            Thread.sleep(5000);

            // --- NEW: Set Title and Description ---
            System.out.println("Setting metadata...");
            try {
                // Title
                if (title != null && !title.isEmpty()) {
                    // Try to find the title box. It's usually the first
                    // ytcp-social-suggestions-textbox
                    // We can use the ID 'title-textarea' if available, or find by structure.
                    WebElement titleBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//ytcp-social-suggestions-textbox[@id='title-textarea']//div[@id='textbox']")));

                    // Clear and set text. clear() might not work on contenteditable, so use keys.
                    titleBox.sendKeys(Keys.CONTROL + "a");
                    titleBox.sendKeys(Keys.BACK_SPACE);
                    titleBox.sendKeys(title);
                    System.out.println("Set title: " + title);
                }

                // Description
                if (description != null && !description.isEmpty()) {
                    WebElement descBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//ytcp-social-suggestions-textbox[@id='description-textarea']//div[@id='textbox']")));

                    descBox.sendKeys(Keys.CONTROL + "a");
                    descBox.sendKeys(Keys.BACK_SPACE);
                    descBox.sendKeys(description);
                    System.out.println("Set description.");
                }

                // Playlist (Optional)
                if (playlist != null && !playlist.isEmpty()) {
                    System.out.println("Setting playlist: " + playlist);
                    // Click dropdown
                    WebElement playlistTrigger = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    "//ytcp-text-dropdown-trigger[.//span[contains(text(),'Select') or contains(text(),'選取')]]")));
                    js.executeScript("arguments[0].click();", playlistTrigger);
                    Thread.sleep(1000);

                    // Find playlist checkbox by text
                    // This is tricky because it's in a dialog.
                    // Try to find a span with the playlist name
                    try {
                        WebElement playlistItem = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//ytcp-ve[.//span[contains(text(), '" + playlist
                                        + "')]]//div[@id='checkbox-container']")));
                        playlistItem.click();
                        System.out.println("Selected playlist item.");

                        // Click Done
                        WebElement doneBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//ytcp-button[.//div[contains(text(), 'Done') or contains(text(), '完成')]]")));
                        doneBtn.click();
                    } catch (Exception e) {
                        System.out.println("Could not find playlist: " + playlist);
                        // Click away or close to continue?
                        // Just try to continue.
                    }
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.out.println("Error setting metadata: " + e.getMessage());
            }

            // A. Required: Select "No, it's not made for kids"
            WebElement notMadeForKids = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//tp-yt-paper-radio-button[@name='VIDEO_MADE_FOR_KIDS_NOT_MFK']")));
            notMadeForKids.click();

            // B. Click Next (Details -> Video Elements)
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("next-button")));
            nextButton.click();
            Thread.sleep(1000);

            // C. Click Next (Video Elements -> Checks)
            nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("next-button")));
            nextButton.click();
            Thread.sleep(1000);

            // D. Click Next (Checks -> Visibility)
            nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("next-button")));
            nextButton.click();
            Thread.sleep(1000);

            // E. Select Visibility
            System.out.println("Setting visibility: " + visibility);
            String visibilityName = "PRIVATE"; // Default
            if (visibility != null) {
                if (visibility.equalsIgnoreCase("PUBLIC"))
                    visibilityName = "PUBLIC";
                else if (visibility.equalsIgnoreCase("UNLISTED"))
                    visibilityName = "UNLISTED";
            }

            WebElement visibilityRadio = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//tp-yt-paper-radio-button[@name='" + visibilityName + "']")));
            visibilityRadio.click();

            // F. Click Save/Publish
            WebElement doneButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("done-button")));
            doneButton.click();
            System.out.println("Clicked Done button.");

            // Wait for the "Video published" or "Video saved" dialog
            try {
                WebElement closeButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("close-button")));
                System.out.println("Found Close button on success dialog. Upload/Save complete.");
                closeButton.click();
            } catch (Exception e) {
                System.out.println("Close button not found (maybe already closed or different UI).");
            }

            System.out.println("Video uploaded successfully!");

            // Keep browser open for a while to ensure everything finishes
            System.out.println("Waiting 60 seconds to ensure upload finishes...");
            Thread.sleep(60000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null)
                driver.quit();
        }
    }
}
