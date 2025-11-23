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

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class XService {

    public void postLink(String videoUrl, String title, String description, List<String> hashtags) {
        setConsoleEncoding();
        String postContent = buildPostContent(videoUrl, title, description);
        System.out.println("Post Content: " + postContent);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToCompose(driver);
            enterContent(driver, postContent);
            enterHashtags(driver, hashtags);
            waitForLinkPreview(videoUrl);
            clickPostButton(driver);
            System.out.println("X post submitted.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to post on X: " + e.getMessage());
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private void setConsoleEncoding() {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildPostContent(String videoUrl, String title, String description) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty())
            sb.append(title).append("\n\n");
        if (description != null && !description.isEmpty())
            sb.append(description).append("\n\n");
        if (videoUrl != null && !videoUrl.isEmpty())
            sb.append(videoUrl).append("\n\n");
        return sb.toString().trim();
    }

    private WebDriver initializeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=d:/work/workspace/java/rpa/chrome-data-x");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
        System.out.println("Initializing ChromeDriver for X (Profile: chrome-data-x)...");
        return new ChromeDriver(options);
    }

    private void navigateToCompose(WebDriver driver) {
        System.out.println("Navigating to https://x.com/compose/tweet ...");
        driver.get("https://x.com/compose/tweet");
    }

    private void enterContent(WebDriver driver, String content) {
        System.out.println("Waiting for editor...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        WebElement editor = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@data-testid='tweetTextarea_0']")));
        editor.click();
        System.out.println("Entering content...");
        editor.sendKeys(content);
    }

    private void enterHashtags(WebDriver driver, List<String> hashtags) throws InterruptedException {
        if (hashtags == null || hashtags.isEmpty())
            return;

        WebElement editor = driver.findElement(By.xpath("//div[@data-testid='tweetTextarea_0']"));
        editor.sendKeys(Keys.ENTER);
        editor.sendKeys(Keys.ENTER);

        for (String tag : hashtags) {
            String cleanTag = tag.replace("#", "").trim();
            if (!cleanTag.isEmpty()) {
                System.out.println("Entering hashtag: #" + cleanTag);
                editor.sendKeys("#" + cleanTag);
                Thread.sleep(2000);
                editor.sendKeys(Keys.ENTER);
                Thread.sleep(500);
            }
        }
    }

    private void waitForLinkPreview(String videoUrl) throws InterruptedException {
        if (videoUrl != null && !videoUrl.isEmpty()) {
            System.out.println("Waiting for link preview...");
            Thread.sleep(5000);
        }
    }

    private void clickPostButton(WebDriver driver) {
        System.out.println("Clicking Post button...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        WebElement postButton = findPostButton(wait);

        if (postButton != null) {
            ensureButtonEnabled(driver, postButton);
            System.out.println("Found Post button: " + postButton.getTagName() + ", Text: " + postButton.getText());
            postButton.click();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        } else {
            throw new RuntimeException("Could not find Post button.");
        }
    }

    private WebElement findPostButton(WebDriverWait wait) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@data-testid='tweetButton']")));
        } catch (Exception e) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(By
                        .xpath("""
                                //*[@role='button']//span[contains(text(), 'Post') or contains(text(), '發佈') or contains(text(), '推文')]
                                """)));
            } catch (Exception ex) {
                try {
                    return wait.until(
                            ExpectedConditions.elementToBeClickable(By.xpath("//*[@data-testid='tweetButtonInline']")));
                } catch (Exception exc) {
                    return null;
                }
            }
        }
    }

    private void ensureButtonEnabled(WebDriver driver, WebElement button) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(
                    ExpectedConditions.not(ExpectedConditions.attributeToBe(button, "aria-disabled", "true")));
        } catch (Exception e) {
            System.out.println("Warning: Post button might be disabled. Attempting click anyway...");
        }
    }
}
