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
public class BilibiliService {

    public void uploadVideo(String filePath, String title, String description, String visibility,
            List<String> hashtags) {
        String simpleTitle = ZhConverterUtil.toSimple(title);
        String simpleDesc = ZhConverterUtil.toSimple(description);
        System.out.println("Processed Title (Simplified): " + simpleTitle);
        System.out.println("Processed Description (Simplified): " + simpleDesc);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToUploadPage(driver);
            uploadFile(driver, filePath);
            Thread.sleep(5000); // Wait for form
            setTitle(driver, simpleTitle);
            setDescription(driver, simpleDesc);
            setTags(driver, hashtags);
            waitForUploadComplete(driver);
            clickPublishButton(driver);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null)
                driver.quit();
        }
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
        System.out.println("Navigating to Bilibili Upload Page...");
        driver.get("https://member.bilibili.com/platform/upload/video/frame");
        Thread.sleep(5000);
    }

    private void uploadFile(WebDriver driver, String filePath) {
        System.out.println("Waiting for file input...");
        try {
            WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(60))
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
            fileInput.sendKeys(filePath);
            System.out.println("Sent file path: " + filePath);
        } catch (Exception e) {
            System.out.println("Could not find file input. Please check if login is required.");
            throw e;
        }
    }

    private void setTitle(WebDriver driver, String title) {
        if (title == null || title.isEmpty())
            return;
        System.out.println("Setting title...");
        try {
            WebElement titleInput = new WebDriverWait(driver, Duration.ofSeconds(60))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '标题') or contains(@class, 'input-val')]")));
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
            WebElement descInput = new WebDriverWait(driver, Duration.ofSeconds(60))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@contenteditable='true' and contains(@class, 'editor')] | //textarea")));
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

    private void setTags(WebDriver driver, List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty())
            return;
        System.out.println("Setting tags...");
        try {
            WebElement tagInput = new WebDriverWait(driver, Duration.ofSeconds(60))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '创建标签') or contains(@class, 'tag-input')]")));
            for (String tag : hashtags) {
                tagInput.sendKeys(ZhConverterUtil.toSimple(tag));
                tagInput.sendKeys(Keys.ENTER);
                Thread.sleep(500);
            }
            System.out.println("Tags set.");
        } catch (Exception e) {
            System.out.println("Could not find tag input: " + e.getMessage());
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        System.out.println("Waiting for upload to reach 100%...");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(60)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath(
                            """
                                    //*[contains(text(), '上传完成') or contains(text(), '100%') or contains(text(), 'Upload Complete')]
                                    """)));
            System.out.println("Upload 100% detected.");
        } catch (Exception e) {
            System.out.println(
                    "Warning: Explicit '100%' text not found within timeout. Proceeding based on button state.");
        }
    }

    private void clickPublishButton(WebDriver driver) throws Exception {
        System.out.println("Waiting for Publish button to be ready...");
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
            Thread.sleep(5000);
        } else {
            throw new RuntimeException("Publish button never became enabled.");
        }
        System.out.println("Bilibili upload sequence finished.");
        Thread.sleep(5000);
    }

    private WebElement waitForPublishButton(WebDriver driver) throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            try {
                WebElement btn = driver.findElement(
                        By.xpath("//span[contains(text(), '立即投稿')] | //div[contains(@class, 'submit-add')]"));
                if (btn.isEnabled())
                    return btn;
                if (i % 5 == 0)
                    System.out.println("Waiting for publish button...");
            } catch (Exception ignored) {
            }
            Thread.sleep(1000);
        }
        return null;
    }
}
