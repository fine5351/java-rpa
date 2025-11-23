package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
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

        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToPostPage(driver);
            enterVideoLink(driver, videoLink);
            setDescription(driver, description);
            setCircle(driver, hashtags);
            setCategory(driver, category);
            setTopics(driver, hashtags);
            setCopyright(driver);
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

    private void navigateToPostPage(WebDriver driver) throws InterruptedException {
        System.out.println("Navigating to Hoyolab Post Link Page...");
        driver.get("https://www.hoyolab.com/newArticle/5?subType=link");
        Thread.sleep(5000);
    }

    private void enterVideoLink(WebDriver driver, String videoLink) throws InterruptedException {
        System.out.println("Entering video link...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            WebElement linkInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[contains(@placeholder, '\u9023\u7d50') or contains(@placeholder, 'Link')]")));
            linkInput.click();
            linkInput.sendKeys(videoLink);
            System.out.println("Video link entered.");
            Thread.sleep(2000);
            WebElement confirmButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            """
                                    //button[contains(., '\u78ba\u5b9a')] | //div[contains(., '\u78ba\u5b9a') and contains(@class, 'button')] | //div[contains(text(), '\u78ba\u5b9a')]
                                    """)));
            confirmButton.click();
            System.out.println("Clicked Confirm button.");
            Thread.sleep(3000);
        } catch (Exception e) {
            System.out.println("Could not find link input or confirm button: " + e.getMessage());
            throw e;
        }
    }

    private void setDescription(WebDriver driver, String description) {
        if (description == null || description.isEmpty())
            return;
        System.out.println("Setting description...");
        try {
            WebElement descInput = new WebDriverWait(driver, Duration.ofSeconds(60))
                    .until(ExpectedConditions.presenceOfElementLocated(
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

    private void setCircle(WebDriver driver, List<String> hashtags) throws InterruptedException {
        if (hashtags == null || hashtags.isEmpty())
            return;
        String circleName = hashtags.get(0);
        System.out.println("Selecting Circle: " + circleName);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            "//div[contains(@class, 'mhy-select__container') and contains(., '\u9078\u64c7\u5708\u5b50')]")))
                    .click();
            Thread.sleep(1000);
            try {
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                """
                                        //div[contains(@class, 'mhy-classification-selector-menu__biz') and (@title='%s' or contains(., '%s'))]
                                        """
                                        .formatted(circleName, circleName))))
                        .click();
            } catch (Exception ex) {
                WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@placeholder='\u641c\u5c0b\u5708\u5b50']")));
                searchInput.sendKeys(circleName);
                Thread.sleep(1000);
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class, 'search-result')]//div[contains(text(), '" + circleName
                                + "')]")))
                        .click();
            }
            System.out.println("Circle selected.");
        } catch (Exception e) {
            System.out.println("Could not select circle: " + e.getMessage());
        }
    }

    private void setCategory(WebDriver driver, String category) throws InterruptedException {
        String categoryName = (category != null && !category.isEmpty()) ? category : "攻略與分析";
        System.out.println("Selecting Category: " + categoryName);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            "//div[contains(@class, 'mhy-select__container') and contains(., '\u9078\u64c7\u6b63\u78ba\u5206\u985e')]")))
                    .click();
            Thread.sleep(1000);
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            """
                                    //div[contains(@class, 'mhy-classification-selector-menu__biz') and (@title='%s' or .//div[contains(@class, 'desc-title') and contains(text(), '%s')]) ]
                                    """
                                    .formatted(categoryName, categoryName))))
                    .click();
            System.out.println("Category selected.");
        } catch (Exception e) {
            System.out.println("Could not select category: " + e.getMessage());
        }
    }

    private void setTopics(WebDriver driver, List<String> hashtags) throws InterruptedException {
        if (hashtags == null || hashtags.size() <= 1)
            return;
        System.out.println("Setting Topics...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            WebElement topicInput = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            "//div[contains(@class, 'topic-autocomplete')]//input[contains(@class, 'mhy-autocomplete__input')]")));
            topicInput.click();
            Thread.sleep(500);
            for (int i = 1; i < hashtags.size(); i++) {
                String topic = hashtags.get(i);
                topicInput.sendKeys(topic);
                Thread.sleep(1000);
                try {
                    wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    """
                                            //span[contains(@class, 'topic-autocomplete-recommend__match') and contains(text(), '%s')]
                                            """
                                            .formatted(topic))))
                            .click();
                } catch (Exception ex) {
                    topicInput.sendKeys(Keys.ENTER);
                }
                Thread.sleep(500);
            }
            driver.findElement(By.tagName("body")).click();
            System.out.println("Topics set.");
        } catch (Exception e) {
            System.out.println("Could not set topics: " + e.getMessage());
        }
    }

    private void setCopyright(WebDriver driver) {
        System.out.println("Setting Copyright...");
        try {
            driver.findElement(By
                    .xpath("""
                            //div[contains(text(), '\u9019\u662f\u6211\u7684\u539f\u5275')]/following-sibling::div//input | //div[contains(text(), '\u9019\u662f\u6211\u7684\u539f\u5275')]/..//div[contains(@class, 'switch')]
                            """))
                    .click();
            System.out.println("Copyright set.");
        } catch (Exception e) {
            System.out.println("Could not set copyright: " + e.getMessage());
        }
    }

    private void clickPublishButton(WebDriver driver) throws InterruptedException {
        System.out.println("Waiting for Publish button...");
        WebElement publishButton = waitForPublishButton(driver);
        if (publishButton != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});",
                    publishButton);
            Thread.sleep(1000);
            System.out.println("Clicking Publish button with Actions...");
            new Actions(driver).moveToElement(publishButton).click().perform();
            System.out.println("Clicked Publish button.");
            Thread.sleep(5000);
        } else {
            System.out.println("Publish button not found or not enabled.");
        }
        System.out.println("Hoyolab upload sequence finished.");
        Thread.sleep(5000);
    }

    private WebElement waitForPublishButton(WebDriver driver) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            try {
                WebElement btn = driver.findElement(By.xpath(
                        "//button[contains(@class, 'hyl-button') and contains(., '\u767c\u8868')] | //button//span[contains(text(), '\u767c\u8868')]"));
                if (btn.isEnabled())
                    return btn;
            } catch (Exception ignored) {
            }
            Thread.sleep(1000);
        }
        return null;
    }
}
