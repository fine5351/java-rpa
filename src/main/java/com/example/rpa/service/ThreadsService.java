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
public class ThreadsService {

    public void postLink(String content, List<String> hashtags) {
        String finalContent = buildContent(content, hashtags);
        log.info("Final Content: {}", finalContent);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToThreads(driver);
            openPostDialog(driver);
            enterContent(driver, finalContent);
            clickPost(driver);
            waitForSuccess(driver);
        } catch (Exception e) {
            log.error("Error during Threads post", e);
        } finally {
            if (driver != null)
                driver.quit();
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

    private void navigateToThreads(WebDriver driver) throws InterruptedException {
        log.info("Navigating to Threads...");
        driver.get("https://www.threads.net");
        Thread.sleep(5000);
    }

    private void openPostDialog(WebDriver driver) {
        log.info("Opening post dialog...");
        try {
            WebElement inputTrigger = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(text(), 'Start a thread') or contains(text(), '建立串文')]")));
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
                            By.xpath("//div[@contenteditable='true']")));
            editor.click();
            editor.sendKeys(content);
            Thread.sleep(2000);
        } catch (Exception e) {
            log.error("Could not find editor.");
        }
    }

    private void clickPost(WebDriver driver) {
        log.info("Clicking Post...");
        try {
            // Threads post button can be tricky, often has "Post" text
            WebElement postBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[text()='Post' or text()='發佈']")));

            // Check if disabled
            if (!Boolean.parseBoolean(postBtn.getAttribute("aria-disabled"))) {
                postBtn.click();
                log.info("Clicked Post.");
            } else {
                log.warn("Post button is disabled.");
            }

        } catch (Exception e) {
            log.warn("Could not click Post: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(text(), 'Posted') or contains(text(), '已發佈')]")));
            log.info("Success indicator found.");
        } catch (Exception e) {
            log.info("Proceeding without specific success text.");
        }
    }
}
