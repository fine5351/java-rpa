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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class HoyolabService {

    public void uploadVideo(String videoLink, String title, String description, List<String> hashtags,
            String category) {
        System.out.println("Title: " + title);
        System.out.println("Description: " + description);
        System.out.println("Video Link: " + videoLink);

        // Setup WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        String userDataDir = "d:/work/workspace/java/rpa/chrome-data";
        options.addArguments("user-data-dir=" + userDataDir);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. Go to Hoyolab Post Link Page
            System.out.println("Navigating to Hoyolab Post Link Page...");
            driver.get("https://www.hoyolab.com/newArticle/5?subType=link");

            // Wait for page load
            Thread.sleep(5000);

            // 2. Enter Video Link
            System.out.println("Entering video link...");
            try {
                // Look for link input. Usually placeholder "請輸入連結" or similar
                // \u9023\u7d50 = 連結
                WebElement linkInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[contains(@placeholder, '\u9023\u7d50') or contains(@placeholder, 'Link')]")));
                linkInput.click();
                linkInput.sendKeys(videoLink);
                System.out.println("Video link entered.");

                // Wait for link preview or validation if needed
                Thread.sleep(2000);

                // Click Confirm (確定) button - MANDATORY
                // \u78ba\u5b9a = 確定
                System.out.println("Clicking Confirm button...");
                // Use . instead of text() to match nested text (e.g. <span>確定</span>)
                // Also wait for it to be clickable
                WebElement confirmButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//button[contains(., '\u78ba\u5b9a')] | //div[contains(., '\u78ba\u5b9a') and contains(@class, 'button')] | //div[contains(text(), '\u78ba\u5b9a')]")));
                confirmButton.click();
                System.out.println("Clicked Confirm button.");

                // Wait for the next section to load (e.g., Title input)
                Thread.sleep(3000);
            } catch (Exception e) {
                System.out.println("Could not find link input or confirm button: " + e.getMessage());
                throw e;
            }

            // 3. Set Title - SKIPPED (Auto-extracted from link)
            // User requested to skip title setting as Hoyolab fetches it automatically.
            System.out.println("Skipping title setting (auto-extracted).");

            // 4. Set Description
            if (description != null && !description.isEmpty()) {
                System.out.println("Setting description...");
                try {
                    // Placeholder "影片簡介" or "簡介"
                    // \u7c21\u4ecb = 簡介
                    WebElement descInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//textarea[contains(@placeholder, '\u7c21\u4ecb')] | //div[@contenteditable='true']")));
                    descInput.click();
                    descInput.sendKeys(Keys.CONTROL + "a");
                    descInput.sendKeys(Keys.BACK_SPACE);
                    descInput.sendKeys(description);
                    System.out.println("Description set.");
                } catch (Exception e) {
                    System.out.println("Could not find description input: " + e.getMessage());
                }
            }

            // 5. Set Circle (Hashtag[0])
            if (hashtags != null && !hashtags.isEmpty()) {
                String circleName = hashtags.get(0);
                System.out.println("Selecting Circle: " + circleName);
                try {
                    // Click "選擇圈子" dropdown
                    // \u9078\u64c7\u5708\u5b50 = 選擇圈子
                    // HTML: <div class="mhy-select__container">...<span>選擇圈子</span>...</div>
                    WebElement circleDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    "//div[contains(@class, 'mhy-select__container') and contains(., '\u9078\u64c7\u5708\u5b50')]")));
                    circleDropdown.click();
                    Thread.sleep(1000);

                    // Try to find text directly in the list first (Common case)
                    try {
                        System.out.println("Looking for circle option: " + circleName);
                        // HTML: <div title="原神" class="mhy-classification-selector-menu__biz">... 原神
                        // ...</div>
                        WebElement circleOption = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//div[contains(@class, 'mhy-classification-selector-menu__biz') and (@title='"
                                        + circleName
                                        + "' or contains(., '" + circleName + "'))]")));
                        circleOption.click();
                    } catch (Exception ex) {
                        System.out.println("Direct option click failed, trying search...");
                        // If not found, try to type in search box
                        // \u641c\u5c0b\u5708\u5b50 = 搜尋圈子
                        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.xpath("//input[@placeholder='\u641c\u5c0b\u5708\u5b50']")));
                        searchInput.sendKeys(circleName);
                        Thread.sleep(1000);
                        // Click the first result
                        WebElement result = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//div[contains(@class, 'search-result')]//div[contains(text(), '" + circleName
                                        + "')]")));
                        result.click();
                    }
                    System.out.println("Circle selected.");
                } catch (Exception e) {
                    System.out.println("Could not select circle: " + e.getMessage());
                }
            }

            // 6. Set Category
            // Default to "攻略與分析" if not provided
            String categoryName = (category != null && !category.isEmpty()) ? category : "攻略與分析";
            System.out.println("Selecting Category: " + categoryName);
            try {
                // Find "選擇正確分類..." dropdown
                // \u9078\u64c7\u6b63\u78ba\u5206\u985e = 選擇正確分類
                // HTML: <div
                // class="mhy-select__container">...<span>選擇正確分類會被更多人看到哦~</span>...</div>
                WebElement categoryDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//div[contains(@class, 'mhy-select__container') and contains(., '\u9078\u64c7\u6b63\u78ba\u5206\u985e')]")));
                categoryDropdown.click();
                Thread.sleep(1000);

                // Click Category Option
                // HTML: <div title="討論分享" class="mhy-classification-selector-menu__biz
                // ...">...<div class="desc-title">討論分享</div>...</div>
                WebElement categoryOption = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class, 'mhy-classification-selector-menu__biz') and (@title='"
                                + categoryName
                                + "' or .//div[contains(@class, 'desc-title') and contains(text(), '" + categoryName
                                + "')]) ]")));
                categoryOption.click();
                System.out.println("Category selected.");
            } catch (Exception e) {
                System.out.println("Could not select category: " + e.getMessage());
            }

            // 7. Set Topics (Hashtag[1..n])
            if (hashtags != null && hashtags.size() > 1) {
                System.out.println("Setting Topics...");
                try {
                    // HTML: <div class="topic-autocomplete">...<input
                    // class="mhy-autocomplete__input">...</div>
                    WebElement topicInput = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    "//div[contains(@class, 'topic-autocomplete')]//input[contains(@class, 'mhy-autocomplete__input')]")));

                    topicInput.click();
                    Thread.sleep(500);

                    for (int i = 1; i < hashtags.size(); i++) {
                        String topic = hashtags.get(i);
                        System.out.println("Adding topic: " + topic);
                        topicInput.sendKeys(topic);
                        Thread.sleep(1000);

                        // Try to click the suggestion from dropdown
                        try {
                            // HTML: <span class="topic-autocomplete-recommend__match">式輿防衛戰</span>
                            WebElement topicOption = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath(
                                            "//span[contains(@class, 'topic-autocomplete-recommend__match') and contains(text(), '"
                                                    + topic + "')]")));
                            topicOption.click();
                        } catch (Exception ex) {
                            System.out.println("Could not click topic option, trying Enter key...");
                            topicInput.sendKeys(Keys.ENTER);
                        }
                        Thread.sleep(500);
                    }
                    // Close dropdown if needed (click outside)
                    driver.findElement(By.tagName("body")).click();
                    System.out.println("Topics set.");
                } catch (Exception e) {
                    System.out.println("Could not set topics: " + e.getMessage());
                }
            }

            // 8. Copyright (這是我的原創)
            System.out.println("Setting Copyright...");
            try {
                // Find the switch/checkbox for "這是我的原創"
                // \u9019\u662f\u6211\u7684\u539f\u5275 = 這是我的原創
                WebElement copyrightSwitch = driver.findElement(By.xpath(
                        "//div[contains(text(), '\u9019\u662f\u6211\u7684\u539f\u5275')]/following-sibling::div//input | //div[contains(text(), '\u9019\u662f\u6211\u7684\u539f\u5275')]/..//div[contains(@class, 'switch')]"));
                // Check if already checked?
                // Usually switches in these UIs are divs.
                // Let's try clicking the switch container.
                copyrightSwitch.click();
                System.out.println("Copyright set.");
            } catch (Exception e) {
                System.out.println("Could not set copyright: " + e.getMessage());
            }

            // 9. Publish
            System.out.println("Waiting for Publish button...");
            WebElement publishButton = null;
            boolean readyToClick = false;
            for (int i = 0; i < 60; i++) {
                try {
                    // \u767c\u8868 = 發表
                    // HTML: <button class="hyl-button ..."><span>發表</span></button>
                    publishButton = driver.findElement(By.xpath(
                            "//button[contains(@class, 'hyl-button') and contains(., '\u767c\u8868')] | //button//span[contains(text(), '\u767c\u8868')]"));
                    if (publishButton.isEnabled()) {
                        readyToClick = true;
                        break;
                    }
                } catch (Exception e) {
                }
                Thread.sleep(1000);
            }

            if (readyToClick && publishButton != null) {
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", publishButton);
                Thread.sleep(1000);

                // Use Actions to click
                System.out.println("Clicking Publish button with Actions...");
                new org.openqa.selenium.interactions.Actions(driver)
                        .moveToElement(publishButton)
                        .click()
                        .perform();
                System.out.println("Clicked Publish button.");

                // Wait for success
                Thread.sleep(5000);
            } else {
                System.out.println("Publish button not found or not enabled.");
            }

            System.out.println("Hoyolab upload sequence finished.");
            Thread.sleep(5000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
