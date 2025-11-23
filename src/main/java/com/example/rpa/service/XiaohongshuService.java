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
import com.github.houbb.opencc4j.util.ZhConverterUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class XiaohongshuService {

    private static final List<String> AUTO_HASHTAG_KEYWORDS = List.of(
            "深境螺旋", "幻想真境劇詩", "虛構敘事", "忘卻之庭", "末日幻影",
            "式輿防衛戰", "零號空洞", "幽境危戰", "異相仲裁", "擬真鏖戰試煉", "危局強襲戰");

    public void uploadVideo(String filePath, String title, String description, String visibility,
            List<String> hashtags) {
        String finalDesc = buildDescription(title, description, hashtags);
        String simpleTitle = ZhConverterUtil.toSimple(title);
        String simpleDesc = ZhConverterUtil.toSimple(finalDesc);

        System.out.println("Processed Title (Simplified): " + simpleTitle);
        System.out.println("Processed Description (Simplified): " + simpleDesc);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToStudio(driver);
            uploadFile(driver, filePath);
            Thread.sleep(5000); // Wait for form
            setTitle(driver, simpleTitle);
            setDescription(driver, simpleDesc);
            checkVisibility(visibility);
            clickPublishButton(driver);
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
                if (title.contains(keyword))
                    description += " #" + keyword;
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
        System.out.println("Navigating to Xiaohongshu Creator Studio...");
        driver.get("https://creator.xiaohongshu.com/publish/publish");
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
            System.out.println("Could not find file input immediately. Please check if login is required.");
            throw e;
        }
    }

    private void setTitle(WebDriver driver, String title) {
        if (title == null || title.isEmpty())
            return;
        System.out.println("Setting title...");
        try {
            WebElement titleInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '标题') or contains(@class, 'title-input')]")));
            clearAndType(titleInput, title);
            System.out.println("Title set.");
        } catch (Exception e) {
            System.out.println("Could not find title input: " + e.getMessage());
        }
    }

    private void setDescription(WebDriver driver, String description) {
        if (description == null || description.isEmpty())
            return;
        System.out.println("Setting description...");
        try {
            WebElement descInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//div[@contenteditable='true' and not(contains(@class, 'title'))] | //textarea")));
            clearAndType(descInput, description);
            System.out.println("Description set.");
        } catch (Exception e) {
            System.out.println("Could not find description input: " + e.getMessage());
        }
    }

    private void clearAndType(WebElement element, String text) {
        element.click();
        element.sendKeys(Keys.CONTROL + "a");
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(text);
    }

    private void checkVisibility(String visibility) {
        if ("PRIVATE".equalsIgnoreCase(visibility)) {
            System.out.println("Private visibility requested but not yet implemented for Xiaohongshu.");
        }
    }

    private void clickPublishButton(WebDriver driver) throws Exception {
        System.out.println("Waiting for upload to complete and Publish button to be ready...");
        WebElement publishButton = waitForPublishButton(driver);

        if (publishButton != null) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", publishButton);
            Thread.sleep(1000);
            try {
                publishButton.click();
                System.out.println("Clicked Publish button.");
            } catch (Exception e) {
                js.executeScript("arguments[0].click();", publishButton);
                System.out.println("Clicked Publish button (JS).");
            }
            waitForSuccess(driver);
        } else {
            throw new RuntimeException("Publish button never became enabled.");
        }
        System.out.println("Xiaohongshu upload sequence finished.");
        Thread.sleep(10000);
    }

    private WebElement waitForPublishButton(WebDriver driver) throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            try {
                WebElement btn = driver.findElement(By.xpath("//button[contains(., '发布')]"));
                if (isButtonEnabled(btn)) {
                    System.out.println("Publish button is enabled.");
                    return btn;
                }
                if (i % 5 == 0)
                    System.out.println("Waiting for publish button to be enabled...");
            } catch (Exception ignored) {
            }
            Thread.sleep(1000);
        }
        return null;
    }

    private boolean isButtonEnabled(WebElement btn) {
        String disabled = btn.getAttribute("disabled");
        String cls = btn.getAttribute("class");
        return (disabled == null || !"true".equals(disabled)) &&
                (cls == null || !cls.contains("disabled"));
    }

    private void waitForSuccess(WebDriver driver) throws InterruptedException {
        System.out.println("Waiting for success confirmation...");
        Thread.sleep(5000);
        try {
            WebElement msg = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("""
                                    //*[contains(text(), '发布成功') or contains(text(), 'Submitted')]
                                    """)));
            System.out.println("Success detected: " + msg.getText());
        } catch (Exception e) {
            System.out.println("Success message not explicitly found, but flow finished.");
        }
    }
}
