package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class FacebookService {

    public void postLink(String videoUrl, String title, String description, List<String> hashtags) {
        String postContent = buildPostContent(videoUrl, title, description, hashtags);
        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToFacebook(driver);
            openPostDialog(driver);
            enterContent(driver, postContent);
            waitForLinkPreview(videoUrl);
            clickPostButton(driver);
            System.out.println("Facebook post submitted.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private String buildPostContent(String videoUrl, String title, String description, List<String> hashtags) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty())
            sb.append(title).append("\n\n");
        if (description != null && !description.isEmpty())
            sb.append(description).append("\n\n");
        if (videoUrl != null && !videoUrl.isEmpty())
            sb.append(videoUrl).append("\n\n");
        if (hashtags != null) {
            for (String tag : hashtags)
                sb.append(tag).append(" ");
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

    private void navigateToFacebook(WebDriver driver) {
        driver.get("https://www.facebook.com");
    }

    private void openPostDialog(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("""
                            //span[contains(text(), "What's on your mind")]/ancestor::div[@role='button']
                            """)))
                    .click();
        } catch (Exception e) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("""
                                //span[contains(text(), "在想些什麼")]/ancestor::div[@role='button']
                                """))).click();
            } catch (Exception ex) {
                System.out.println("Could not find the initial post input area.");
                throw ex;
            }
        }
    }

    private void enterContent(WebDriver driver, String content) {
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(
                ExpectedConditions.elementToBeClickable(By.xpath("//div[@role='dialog']//div[@role='textbox']")))
                .sendKeys(content);
    }

    private void waitForLinkPreview(String videoUrl) throws InterruptedException {
        if (videoUrl != null && !videoUrl.isEmpty()) {
            Thread.sleep(5000);
        }
    }

    private void clickPostButton(WebDriver driver) throws InterruptedException {
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(
                By.xpath("""
                        //div[@role='dialog']//div[@aria-label='Post' or @aria-label='發佈']
                        """))).click();
        Thread.sleep(10000);
    }
}
