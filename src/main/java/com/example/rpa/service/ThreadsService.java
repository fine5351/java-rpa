package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
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
public class ThreadsService {

    public void postLink(String videoUrl, String title, String description, String topic) {
        setConsoleEncoding();
        String postContent = buildPostContent(videoUrl, title, description);
        String cleanTopic = (topic != null) ? topic.replace("#", "") : null;

        System.out.println("Post Content: " + postContent);
        System.out.println("Topic: " + cleanTopic);

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToThreads(driver);
            openPostDialog(driver);
            WebElement editor = enterContent(driver, postContent);
            setTopic(editor, cleanTopic);
            waitForLinkPreview(videoUrl);
            clickPostButton(driver);
            System.out.println("Threads post submitted.");
        } catch (Exception e) {
            e.printStackTrace();
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
        options.addArguments("user-data-dir=d:/work/workspace/java/rpa/chrome-data");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
        System.out.println("Initializing ChromeDriver...");
        return new ChromeDriver(options);
    }

    private void navigateToThreads(WebDriver driver) {
        System.out.println("Navigating to https://www.threads.net ...");
        driver.get("https://www.threads.net");
    }

    private void openPostDialog(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@aria-label='文字欄位空白。請輸入內容以撰寫新貼文。']"))).click();
        } catch (Exception e) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(By
                        .xpath("""
                                /html/body/div[2]/div/div/div[2]/div[2]/div/div/div/div[1]/div[1]/div[1]/div/div/div[2]/div[1]/div[2]/div/div[1]
                                """)))
                        .click();
            } catch (Exception ex) {
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(text(), '有什麼新鮮事') and not(ancestor::div[@role='article'])]"))).click();
            }
        }
    }

    private WebElement enterContent(WebDriver driver, String content) throws InterruptedException {
        Thread.sleep(2000);
        WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(120))
                .until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@contenteditable='true']")));
        editor.click();
        editor.sendKeys(content);
        return editor;
    }

    private void setTopic(WebElement editor, String topic) {
        if (topic == null || topic.isEmpty())
            return;
        try {
            editor.sendKeys(Keys.ENTER);
            Thread.sleep(500);
            editor.sendKeys("#" + topic);
            Thread.sleep(1000);
            editor.sendKeys(Keys.ENTER);
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Topic selection failed, skipping.");
        }
    }

    private void waitForLinkPreview(String videoUrl) throws InterruptedException {
        if (videoUrl != null && !videoUrl.isEmpty()) {
            Thread.sleep(3000);
        }
    }

    private void clickPostButton(WebDriver driver) throws InterruptedException {
        System.out.println("Attempting to find and click Post button...");
        List<WebElement> candidates = findButtonCandidates(driver);
        WebElement targetButton = selectBestButton(candidates);

        if (targetButton != null) {
            System.out.println("Clicking target button (Text='" + targetButton.getText() + "') using JavaScript.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", targetButton);
            Thread.sleep(5000);
        } else {
            throw new RuntimeException("No clickable Post button found.");
        }
    }

    private List<WebElement> findButtonCandidates(WebDriver driver) {
        try {
            List<WebElement> list = driver
                    .findElements(By.xpath("//div[@role='dialog']//div[contains(@class, 'x10wlt62')]"));
            if (!list.isEmpty())
                return list;
        } catch (Exception ignored) {
        }
        return driver.findElements(By.xpath("//div[contains(@class, 'x10wlt62')]"));
    }

    private WebElement selectBestButton(List<WebElement> candidates) {
        WebElement target = null;
        int maxX = -1;
        int maxY = -1;

        for (WebElement btn : candidates) {
            try {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    String text = btn.getText();
                    if (text == null || text.trim().isEmpty())
                        continue;

                    Point loc = btn.getLocation();
                    if (loc.getY() > maxY + 10) {
                        maxY = loc.getY();
                        maxX = loc.getX();
                        target = btn;
                    } else if (Math.abs(loc.getY() - maxY) <= 10 && loc.getX() > maxX) {
                        maxX = loc.getX();
                        maxY = Math.max(maxY, loc.getY());
                        target = btn;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return target;
    }
}
