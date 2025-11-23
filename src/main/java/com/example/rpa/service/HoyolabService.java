package com.example.rpa.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class HoyolabService {

    public void uploadVideo(String videoUrl, String title, String description, String circleName, String categoryName,
            List<String> topics) {
        log.info("Starting Hoyolab upload for video: {}", videoUrl);
        WebDriver driver = null;
        try {
            driver = initializeDriver();
            navigateToPostPage(driver);
            enterVideoLink(driver, videoUrl);
            enterTitle(driver, title);
            enterDescription(driver, description);
            selectCircle(driver, circleName);
            selectCategory(driver, categoryName);
            selectTopics(driver, topics);
            selectCopyright(driver);
            clickPublish(driver);
            waitForSuccess(driver);
        } catch (Exception e) {
            log.error("Error during Hoyolab upload", e);
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
        log.info("Navigating to Hoyolab Post Page...");
        driver.get("https://www.hoyolab.com/creator/post/video");
        Thread.sleep(5000);
    }

    private void enterVideoLink(WebDriver driver, String videoUrl) {
        log.info("Entering video link...");
        try {
            WebElement input = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//input[contains(@placeholder, 'Enter video link') or contains(@placeholder, '輸入影片連結')]")));
            input.sendKeys(videoUrl);

            // Wait for validation/preview
            Thread.sleep(3000);

            // Click "Insert" or similar if needed (Hoyolab usually auto-fetches)
            try {
                WebElement insertBtn = driver
                        .findElement(By.xpath("//button[contains(text(), 'Insert') or contains(text(), '插入')]"));
                if (insertBtn.isDisplayed()) {
                    insertBtn.click();
                }
            } catch (Exception ignored) {
            }

            log.info("Video link entered.");
        } catch (Exception e) {
            log.error("Could not enter video link.");
        }
    }

    private void enterTitle(WebDriver driver, String title) {
        if (title == null || title.isEmpty())
            return;
        log.info("Entering title...");
        try {
            WebElement input = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder, 'Title') or contains(@placeholder, '標題')]")));
            input.sendKeys(title);
        } catch (Exception e) {
            log.warn("Could not enter title: {}", e.getMessage());
        }
    }

    private void enterDescription(WebDriver driver, String description) {
        if (description == null || description.isEmpty())
            return;
        log.info("Entering description...");
        try {
            WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//div[contains(@class, 'DraftEditor-editorContainer')]//div[@contenteditable='true']")));
            editor.sendKeys(description);
        } catch (Exception e) {
            log.warn("Could not enter description: {}", e.getMessage());
        }
    }

    private void selectCircle(WebDriver driver, String circleName) {
        if (circleName == null || circleName.isEmpty())
            return;
        log.info("Selecting circle: {}", circleName);
        try {
            WebElement dropdown = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(@class, 'select-circle')]")));
            dropdown.click();
            Thread.sleep(1000);

            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(@class, 'circle-item') and contains(text(), '" + circleName
                                    + "')]")));
            option.click();
        } catch (Exception e) {
            log.warn("Could not select circle: {}", e.getMessage());
        }
    }

    private void selectCategory(WebDriver driver, String categoryName) {
        if (categoryName == null || categoryName.isEmpty())
            return;
        log.info("Selecting category: {}", categoryName);
        try {
            // Assuming category selection logic similar to circle or radio buttons
            WebElement category = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(@class, 'category-item') and contains(text(), '" + categoryName
                                    + "')]")));
            category.click();
        } catch (Exception e) {
            log.warn("Could not select category: {}", e.getMessage());
        }
    }

    private void selectTopics(WebDriver driver, List<String> topics) {
        if (topics == null || topics.isEmpty())
            return;
        log.info("Selecting topics...");
        try {
            WebElement addTopicBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(@class, 'add-topic')]")));
            addTopicBtn.click();
            Thread.sleep(1000);

            WebElement searchInput = driver.findElement(By.xpath("//input[contains(@placeholder, 'Search topic')]"));

            for (String topic : topics) {
                searchInput.sendKeys(Keys.CONTROL + "a");
                searchInput.sendKeys(Keys.BACK_SPACE);
                searchInput.sendKeys(topic);
                Thread.sleep(1000);

                try {
                    WebElement result = new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//div[contains(@class, 'topic-item') and contains(text(), '" + topic
                                            + "')]")));
                    result.click();
                } catch (Exception ignored) {
                    log.warn("Topic not found: {}", topic);
                }
            }

            // Close topic selector if needed
            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.ESCAPE).perform();

        } catch (Exception e) {
            log.warn("Could not select topics: {}", e.getMessage());
        }
    }

    private void selectCopyright(WebDriver driver) {
        log.info("Selecting copyright (Original)...");
        try {
            WebElement originalRadio = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(text(), 'Original') or contains(text(), '原創')]")));
            originalRadio.click();
        } catch (Exception e) {
            log.warn("Could not select copyright: {}", e.getMessage());
        }
    }

    private void clickPublish(WebDriver driver) {
        log.info("Clicking Publish...");
        try {
            WebElement publishBtn = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("""
                                    //button[contains(text(), 'Post') or contains(text(), '發佈')]
                                    """)));

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", publishBtn);
            Thread.sleep(1000);

            new Actions(driver).moveToElement(publishBtn).click().perform();
            log.info("Clicked Publish.");
        } catch (Exception e) {
            log.warn("Could not click Publish: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(text(), 'Post success') or contains(text(), '發佈成功')]")));
            log.info("Success indicator found.");
        } catch (Exception e) {
            log.info("Proceeding without specific success text.");
        }
    }
}
