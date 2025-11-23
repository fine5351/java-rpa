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
public class BilibiliService {

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
            navigateToUpload(driver);
            uploadFile(driver, filePath);
            waitForUploadComplete(driver);
            setTitle(driver, simplifiedTitle);
            setDescription(driver, simplifiedDescription);
            setTags(driver, hashtags);
            clickSubmit(driver);
            waitForSuccess(driver);
        } catch (Exception e) {
            log.error("Error during Bilibili upload", e);
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

    private void navigateToUpload(WebDriver driver) throws InterruptedException {
        log.info("Navigating to Bilibili Upload...");
        driver.get("https://member.bilibili.com/platform/upload/video/frame");
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
            new WebDriverWait(driver, Duration.ofSeconds(300)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("""
                            //span[contains(text(), '上传完成') or contains(text(), 'Upload complete')]
                            """)));
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

            // Clear existing title (Bilibili might auto-fill from filename)
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
                            By.xpath("//div[contains(@class, 'editor-box')]//div[@contenteditable='true']")));
            descInput.click();
            descInput.sendKeys(description);
            log.info("Description set.");
        } catch (Exception e) {
            log.warn("Could not set description: {}", e.getMessage());
        }
    }

    private void setTags(WebDriver driver, List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty())
            return;

        log.info("Setting tags...");
        try {
            WebElement tagInput = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, '标签') or contains(@placeholder, 'Tag')]")));

            for (String tag : hashtags) {
                String simplifiedTag = ZhConverterUtil.toSimple(tag);
                tagInput.sendKeys(simplifiedTag);
                tagInput.sendKeys(Keys.ENTER);
                Thread.sleep(500);
            }
            log.info("Tags set.");
        } catch (Exception e) {
            log.warn("Could not set tags: {}", e.getMessage());
        }
    }

    private void clickSubmit(WebDriver driver) {
        log.info("Clicking Submit...");
        try {
            WebElement submitBtn = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//span[contains(text(), '立即投稿') or contains(text(), 'Submit')]")));

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
            Thread.sleep(1000);

            submitBtn.click();
            log.info("Clicked Submit.");
        } catch (Exception e) {
            log.warn("Could not click Submit: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(text(), '投稿成功') or contains(text(), 'Success')]")));
            log.info("Success indicator found.");
        } catch (Exception e) {
            log.info("Proceeding without specific success text.");
        }
    }
}
