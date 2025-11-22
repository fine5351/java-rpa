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

import com.github.houbb.opencc4j.util.ZhConverterUtil;

@Service
public class XiaohongshuService {

    public void uploadVideo(String filePath, String title, String description, String visibility) {
        // 0. Auto-tagging and Chinese Conversion
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

        // Convert to Simplified Chinese
        if (title != null) {
            title = ZhConverterUtil.toSimple(title);
        }
        if (description != null) {
            description = ZhConverterUtil.toSimple(description);
        }

        System.out.println("Processed Title (Simplified): " + title);
        System.out.println("Processed Description (Simplified): " + description);

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

            // 1. Go to Xiaohongshu Creator Studio
            System.out.println("Navigating to Xiaohongshu Creator Studio...");
            driver.get("https://creator.xiaohongshu.com/publish/publish");

            // Wait for potential redirect or load
            Thread.sleep(5000);

            // 2. Upload File
            System.out.println("Waiting for file input...");
            try {
                // Try to find the file input directly
                WebElement fileInput = wait
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
                fileInput.sendKeys(filePath);
                System.out.println("Sent file path: " + filePath);
            } catch (Exception e) {
                System.out.println("Could not find file input immediately. Please check if login is required.");
                throw e;
            }

            // 3. Wait for upload to process
            System.out.println("Waiting for upload to process...");
            // Wait for the title input to appear as a sign that upload has
            // started/processed
            // Xiaohongshu usually shows the form after file selection
            Thread.sleep(5000);

            // 4. Set Title
            if (title != null && !title.isEmpty()) {
                System.out.println("Setting title...");
                try {
                    // Look for input with placeholder containing "标题" (Title)
                    WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '标题') or contains(@class, 'title-input')]")));

                    // Clear and type
                    titleInput.click();
                    titleInput.sendKeys(Keys.CONTROL + "a");
                    titleInput.sendKeys(Keys.BACK_SPACE);
                    titleInput.sendKeys(title);
                    System.out.println("Title set.");
                } catch (Exception e) {
                    System.out.println("Could not find title input: " + e.getMessage());
                }
            }

            // 5. Set Description
            if (description != null && !description.isEmpty()) {
                System.out.println("Setting description...");
                try {
                    // Look for contenteditable div or textarea for description
                    // Usually has placeholder "填写更全面的描述信息" or similar
                    WebElement descInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//div[@contenteditable='true' and not(contains(@class, 'title'))] | //textarea")));

                    descInput.click();
                    // Clear might be tricky, append for now or try Ctrl+A
                    descInput.sendKeys(Keys.CONTROL + "a");
                    descInput.sendKeys(Keys.BACK_SPACE);
                    descInput.sendKeys(description);
                    System.out.println("Description set.");
                } catch (Exception e) {
                    System.out.println("Could not find description input: " + e.getMessage());
                }
            }

            // 6. Set Visibility
            // Xiaohongshu might default to public.
            // If visibility logic is needed, we can add it here.
            // For now, we assume default (Public).
            if ("PRIVATE".equalsIgnoreCase(visibility)) {
                System.out.println("Private visibility requested but not yet implemented for Xiaohongshu.");
            }

            // 7. Publish
            System.out.println("Waiting for upload to complete and Publish button to be ready...");

            WebElement publishButton = null;
            boolean readyToClick = false;

            // Wait loop
            for (int i = 0; i < 300; i++) {
                try {
                    // Find Publish Button (发布)
                    // Usually a button with text "发布"
                    try {
                        publishButton = driver.findElement(By.xpath("//button[contains(., '发布')]"));
                    } catch (Exception e) {
                        // Retry
                    }

                    if (publishButton != null) {
                        // Check if disabled
                        String disabledAttr = publishButton.getAttribute("disabled");
                        String classAttr = publishButton.getAttribute("class");
                        boolean isDisabled = (disabledAttr != null && "true".equals(disabledAttr)) ||
                                (classAttr != null && classAttr.contains("disabled"));

                        // Also check for "Uploading" text if possible to be sure
                        // For now, we rely on the button state
                        if (!isDisabled && publishButton.isEnabled()) {
                            System.out.println("Publish button is enabled.");
                            readyToClick = true;
                            break;
                        }
                    }

                    if (i % 5 == 0)
                        System.out.println("Waiting for publish button to be enabled...");

                } catch (Exception e) {
                    // Ignore
                }
                Thread.sleep(1000);
            }

            if (readyToClick && publishButton != null) {
                // Scroll to view
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", publishButton);
                Thread.sleep(1000);

                try {
                    publishButton.click();
                    System.out.println("Clicked Publish button.");
                } catch (Exception e) {
                    js.executeScript("arguments[0].click();", publishButton);
                    System.out.println("Clicked Publish button (JS).");
                }

                // Wait for success
                System.out.println("Waiting for success confirmation...");
                Thread.sleep(5000);
                // Look for "发布成功" or similar
                try {
                    WebElement successMsg = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//*[contains(text(), '发布成功') or contains(text(), 'Submitted')]")));
                    System.out.println("Success detected: " + successMsg.getText());
                } catch (Exception e) {
                    System.out.println("Success message not explicitly found, but flow finished.");
                }

            } else {
                throw new RuntimeException("Publish button never became enabled.");
            }

            System.out.println("Xiaohongshu upload sequence finished.");
            Thread.sleep(10000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
