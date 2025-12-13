package com.example.rpa.post.service;

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

    public void postLink(String videoUrl, String title, String description, String circleName, String categoryName,
            List<String> topics) {
        log.info("Starting Hoyolab upload for video: {}", videoUrl);
        WebDriver driver = null;
        boolean success = false;
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
            success = true;
        } catch (Exception e) {
            log.error("Error during Hoyolab upload", e);
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
        options.addArguments("user-data-dir=d:/work/workspace/java/rpa/chrome-data");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
        return new ChromeDriver(options);
    }

    private void navigateToPostPage(WebDriver driver) {
        log.info("尋找 發文頁面 位置中");
        log.info("已找到 發文頁面 : {}", "https://www.hoyolab.com/creator/post/video");
        log.info("執行 前往發文頁面 操作");
        driver.get("https://www.hoyolab.com/creator/post/video");
    }

    private void enterVideoLink(WebDriver driver, String videoUrl) {
        log.info("Entering video link...");
        try {
            log.info("尋找 影片連結輸入框 位置中");
            By selector = By
                    .xpath("//input[contains(@placeholder, 'Enter video link') or contains(@placeholder, '輸入影片連結')]");
            WebElement input = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.presenceOfElementLocated(selector));
            log.info("已找到 影片連結輸入框 : {}", selector);
            log.info("執行 輸入影片連結 操作");
            input.sendKeys(videoUrl);

            // Wait for validation/preview
            // Thread.sleep(3000); // Removed fixed wait

            // Click "Insert" or similar if needed (Hoyolab usually auto-fetches)
            try {
                log.info("尋找 插入按鈕 位置中");
                By insertSelector = By.xpath("//button[contains(text(), 'Insert') or contains(text(), '插入')]");
                WebElement insertBtn = new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions
                        .elementToBeClickable(insertSelector));
                log.info("已找到 插入按鈕 : {}", insertSelector);
                log.info("執行 點擊插入按鈕 操作");
                insertBtn.click();
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
            log.info("尋找 標題輸入框 位置中");
            By selector = By.xpath("//input[contains(@placeholder, 'Title') or contains(@placeholder, '標題')]");
            WebElement input = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(selector));
            log.info("已找到 標題輸入框 : {}", selector);
            log.info("執行 設定標題 操作");
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
            log.info("尋找 說明輸入框 位置中");
            By selector = By
                    .xpath("//div[contains(@class, 'DraftEditor-editorContainer')]//div[@contenteditable='true']");
            WebElement editor = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(selector));
            log.info("已找到 說明輸入框 : {}", selector);
            log.info("執行 設定說明 操作");
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
            log.info("尋找 圈子選單 位置中");
            By dropdownSelector = By.xpath("//div[contains(@class, 'select-circle')]");
            WebElement dropdown = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(dropdownSelector));
            log.info("已找到 圈子選單 : {}", dropdownSelector);
            log.info("執行 開啟圈子選單 操作");
            dropdown.click();

            log.info("尋找 圈子選項 位置中");
            By optionSelector = By
                    .xpath("//div[contains(@class, 'circle-item') and contains(text(), '" + circleName + "')]");
            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(optionSelector));
            log.info("已找到 圈子選項 : {}", optionSelector);
            log.info("執行 選擇圈子 操作");
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
            log.info("尋找 分類選項 位置中");
            By selector = By
                    .xpath("//div[contains(@class, 'category-item') and contains(text(), '" + categoryName + "')]");
            WebElement category = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(selector));
            log.info("已找到 分類選項 : {}", selector);
            log.info("執行 選擇分類 操作");
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
            log.info("尋找 新增話題按鈕 位置中");
            By addBtnSelector = By.xpath("//div[contains(@class, 'add-topic')]");
            WebElement addTopicBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(addBtnSelector));
            log.info("已找到 新增話題按鈕 : {}", addBtnSelector);
            log.info("執行 點擊新增話題按鈕 操作");
            addTopicBtn.click();

            log.info("尋找 話題搜尋框 位置中");
            By searchSelector = By.xpath("//input[contains(@placeholder, 'Search topic')]");
            WebElement searchInput = driver.findElement(searchSelector);
            log.info("已找到 話題搜尋框 : {}", searchSelector);

            for (String topic : topics) {
                log.info("執行 搜尋話題 操作");
                searchInput.sendKeys(Keys.CONTROL + "a");
                searchInput.sendKeys(Keys.BACK_SPACE);
                searchInput.sendKeys(topic);

                try {
                    log.info("尋找 話題結果 位置中");
                    By resultSelector = By
                            .xpath("//div[contains(@class, 'topic-item') and contains(text(), '" + topic + "')]");
                    WebElement result = new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.elementToBeClickable(resultSelector));
                    log.info("已找到 話題結果 : {}", resultSelector);
                    log.info("執行 選擇話題 操作");
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
            log.info("尋找 原創選項 位置中");
            By selector = By.xpath("//div[contains(text(), 'Original') or contains(text(), '原創')]");
            WebElement originalRadio = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(selector));
            log.info("已找到 原創選項 : {}", selector);
            log.info("執行 選擇原創 操作");
            originalRadio.click();
        } catch (Exception e) {
            log.warn("Could not select copyright: {}", e.getMessage());
        }
    }

    private void clickPublish(WebDriver driver) {
        log.info("Clicking Publish...");
        try {
            log.info("尋找 發佈按鈕 位置中");
            By selector = By.xpath("//button[contains(text(), 'Post') or contains(text(), '發佈')]");
            WebElement publishBtn = new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.elementToBeClickable(selector));
            log.info("已找到 發佈按鈕 : {}", selector);

            // Scroll to view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", publishBtn);

            log.info("執行 點擊發佈 操作");
            new Actions(driver).moveToElement(publishBtn).click().perform();
            log.info("Clicked Publish.");
        } catch (Exception e) {
            log.warn("Could not click Publish: {}", e.getMessage());
        }
    }

    private void waitForSuccess(WebDriver driver) {
        log.info("Waiting for success...");
        while (true) {
            try {
                List<WebElement> successElements = driver
                        .findElements(By.xpath("//div[contains(text(), 'Post success') or contains(text(), '發佈成功')]"));
                if (!successElements.isEmpty() && successElements.get(0).isDisplayed()) {
                    log.info("Success indicator found.");
                    break;
                }
            } catch (Exception e) {
                // Ignore
            }
            log.info("Waiting for Hoyolab publish success...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
