package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Slf4j
@Service
public class XService {

    public void postLink(String content, List<String> hashtags) {
        WebDriver driver = null;
        boolean success = false;
        try {
            driver = initializeDriver();
            navigateToCompose(driver);
            enterContent(driver, content);
            enterHashtags(driver, hashtags);
            clickPost(driver);
            waitForSuccess(driver);
            success = true;
        } catch (Exception e) {
            log.error("Error during X post", e);
        } finally {
            if (driver != null && success) {
                driver.quit();
                log.info("Browser closed successfully.");
            } else if (driver != null) {
                log.warn("Browser left open for debugging.");
            }
        }
    }

    private WebDriver initializeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=d:/work/workspace/java/rpa/chrome-data-x");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
        return new ChromeDriver(options);
    }

    private void navigateToCompose(WebDriver driver) throws InterruptedException {
        log.info("Navigating to X Compose...");
        driver.get("https://twitter.com/compose/tweet");
        Thread.sleep(5000);
    }

    private void enterContent(WebDriver driver, String content) {
        log.info("Entering content...");
        try {
            WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@data-testid='tweetTextarea_0']")));
            editor.click();
            editor.sendKeys(content);
            log.info("Content entered.");
        } catch (Exception e) {
            log.error("Could not find editor.");
            throw e;
        }
    }

    private void enterHashtags(WebDriver driver, List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty())
            return;

        log.info("Entering hashtags...");
        try {
            WebElement editor = driver.findElement(By.xpath("//div[@data-testid='tweetTextarea_0']"));
            editor.sendKeys("\n\n");

            for (String tag : hashtags) {
                editor.sendKeys("#" + tag);
                Thread.sleep(1000); // Wait for suggestion
                editor.sendKeys(Keys.ENTER);
                editor.sendKeys(" ");
            }
            log.info("Hashtags entered.");
        } catch (Exception e) {
            log.warn("Could not enter hashtags: {}", e.getMessage());
        }
    }

    private void clickPost(WebDriver driver) {
        log.info("Clicking Post...");
        try {
            WebElement postBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[@data-testid='tweetButton']")));
            postBtn.click();
            log.info("Clicked Post.");

        } catch (Exception e) {
            // Fallback for different button text/selector
            try {
                WebElement postBtnFallback = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(
                                By.xpath("""
                                        //div[@role='button'][.//span[text()='Post']]
                                        """)));
                postBtnFallback.click();
                log.info("Clicked Post (Fallback).");
            } catch (Exception ex) {
                log.warn("Could not click Post: {}", ex.getMessage());
            }
        }
    }

    private void waitForSuccess(WebDriver driver) {
        log.info("Waiting for success...");
        while (true) {
            try {
                List<WebElement> successElements = driver.findElements(By.xpath("//div[@data-testid='toast']"));
                if (!successElements.isEmpty() && successElements.get(0).isDisplayed()) {
                    log.info("Success indicator found.");
                    break;
                }
            } catch (Exception e) {
                // Ignore
            }
            log.info("Waiting for X post success...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
