package com.example.rpa.service;

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
public class XiaohongshuService {

    private static final List<String> AUTO_HASHTAG_KEYWORDS = List.of(
            "深境螺旋", "幻想真境劇詩", "虛構敘事", "忘卻之庭", "末日幻影",
            "式輿防衛戰", "零號空洞", "幽境危戰", "異相仲裁", "擬真鏖戰試煉", "危局強襲戰");

    public void uploadVideo(String filePath, String title, String description, List<String> hashtags) {
        String simplifiedTitle = ZhConverterUtil.toSimple(title);
        String finalDescription = buildDescription(title, description, hashtags);
        String simplifiedDescription = ZhConverterUtil.toSimple(finalDescription);

        log.info("Simplified Title: {}", simplifiedTitle);
        log.info("Simplified Description: {}", simplifiedDescription);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToCreatorStudio(driver);
            uploadFile(driver, filePath);
            waitForUploadComplete(driver);
            setTitle(driver, simplifiedTitle);
            setDescription(driver, simplifiedDescription);
            clickPublish(driver);
            waitForSuccess(driver);
        } catch (Exception e) {
            log.error("Error during Xiaohongshu upload", e);
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private String buildDescription(String title, String description, List<String> hashtags) {
        String desc = "";
        if (description != null)
            desc += description + "\n";

        if (title != null) {
            for (String keyword : AUTO_HASHTAG_KEYWORDS) {
                if (title.contains(keyword))
                    desc += " #" + keyword;
            }
        }

        if (hashtags != null) {
            for (String tag : hashtags) {
                if (!desc.contains(tag))
                    desc += " " + tag;
            }
        }
        return desc.trim();
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

    private void navigateToCreatorStudio(WebDriver driver) throws InterruptedException {
        log.info("Navigating to Xiaohongshu Creator Studio...");
        driver.get("https://creator.xiaohongshu.com/publish/publish");
        Thread.sleep(5000);
    }

    private void uploadFile(WebDriver driver, String filePath) {
        log.info("Uploading file...");
        try {
            WebElement fileInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='file']")));
            fileInput.sendKeys(filePath);
        } catch (Exception e) {
            log.error("File input not found.");
            throw e;
        }
    }

    private void waitForUploadComplete(WebDriver driver) {
        log.info("Waiting for upload to complete...");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(120)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(text(), '上传成功') or contains(text(), 'Upload success')]")));
            log.info("Upload complete.");
        } catch (Exception e) {
            log.warn("Upload completion text not found, proceeding...");
        }
    }

    private void setTitle(WebDriver driver, String title) {
        log.info("Setting title...");
        try {
            WebElement titleInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '标题') or contains(@placeholder, 'Title')]")));
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
        log.info("Setting description...");
        try {
            WebElement descInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@id='post-textarea']")));
            descInput.click();

            // Split description by spaces to handle hashtags
            String[] parts = description.split(" ");
            for (String part : parts) {
                descInput.sendKeys(part);
                descInput.sendKeys(" ");
                if (part.startsWith("#")) {
                    try {
                        Thread.sleep(1000); // Wait for suggestion
                        WebElement suggestion = new WebDriverWait(driver, Duration.ofSeconds(2))
                                .until(ExpectedConditions.presenceOfElementLocated(
                                        By.xpath("//li[contains(@class, 'topic-item')]")));
                        suggestion.click();
                    } catch (Exception ignored) {
                        // No suggestion or timeout, just continue
                    }
                }
            }
            log.info("Description set.");
        } catch (Exception e) {
            log.warn("Could not set description: {}", e.getMessage());
        }
    }

    private void clickPublish(WebDriver driver) {
        log.info("Clicking Publish...");
        try {
            WebElement publishBtn = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(text(), '发布') or contains(text(), 'Publish')]")));

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", publishBtn);
            Thread.sleep(1000);

            publishBtn.click();
            log.info("Clicked Publish.");
        } catch (Exception e) {
            log.warn("Could not click Publish: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("""
                            //div[contains(text(), '发布成功') or contains(text(), 'Publish success')]
                            """)));
            log.info("Success indicator found.");
        } catch (Exception e) {
            log.info("Proceeding without specific success text.");
        }
    }
}
