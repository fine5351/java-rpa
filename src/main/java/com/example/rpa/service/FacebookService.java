package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
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
public class FacebookService {

    public void postLink(String content, List<String> hashtags) {
        String finalContent = buildContent(content, hashtags);
        log.info("Final Content: {}", finalContent);

        WebDriver driver = null;
        boolean success = false;
        try {
            driver = initializeDriver();
            navigateToFacebook(driver);
            openPostDialog(driver);
            enterContent(driver, finalContent);
            clickPost(driver);
            waitForSuccess(driver);
            success = true;
        } catch (Exception e) {
            log.error("Error during Facebook post", e);
        } finally {
            if (driver != null && success) {
                driver.quit();
                log.info("Browser closed successfully.");
            } else if (driver != null) {
                log.warn("Browser left open for debugging.");
            }
        }
    }

    private String buildContent(String content, List<String> hashtags) {
        StringBuilder sb = new StringBuilder();
        if (content != null)
            sb.append(content);

        if (hashtags != null && !hashtags.isEmpty()) {
            if (sb.length() > 0)
                sb.append("\n\n");
            for (String tag : hashtags) {
                sb.append("#").append(tag).append(" ");
            }
        }
        return sb.toString().trim();
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

    private void navigateToFacebook(WebDriver driver) throws InterruptedException {
        log.info("Navigating to Facebook...");
        driver.get("https://www.facebook.com");
        Thread.sleep(5000);
    }

    private void openPostDialog(WebDriver driver) {
        log.info("Opening post dialog...");
        try {
            WebElement inputTrigger = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("""
                                    //div[contains(@aria-label, "What's on your mind")]
                                    | //span[contains(text(), "What's on your mind")]
                                    | //span[contains(text(), "在想些什麼")]
                                    """)));
            inputTrigger.click();
            Thread.sleep(2000);
        } catch (Exception e) {
            log.error("Could not open post dialog.");
        }
    }

    private void enterContent(WebDriver driver, String content) {
        log.info("Entering content...");
        try {
            WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//div[@aria-label='What's on your mind?' or @aria-label='在想些什麼？']//div[@contenteditable='true']")));
            editor.sendKeys(content);
            Thread.sleep(2000); // Wait for link preview if any
        } catch (Exception e) {
            log.error("Could not find editor.");
        }
    }

    private void clickPost(WebDriver driver) {
        log.info("Clicking Post...");
        try {
            WebElement postBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[@aria-label='Post' or @aria-label='發佈']")));
            postBtn.click();
            log.info("Clicked Post.");
        } catch (Exception e) {
            log.warn("Could not click Post: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        log.info("Waiting for success...");
        while (true) {
            try {
                List<WebElement> dialogs = driver.findElements(By.xpath("//div[@role='dialog']"));
                if (dialogs.isEmpty()) {
                    log.info("Post dialog closed, assuming success.");
                    break;
                }
            } catch (Exception e) {
                // Ignore
            }
            log.info("Waiting for Facebook post to complete...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
