package com.example.rpa.video.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;

@Slf4j
public class WebDriverUtil {

    public static WebDriver initializeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=d:/work/workspace/java/rpa/chrome-data");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
        try {
            return new ChromeDriver(options);
        } catch (org.openqa.selenium.SessionNotCreatedException e) {
            log.warn("Chrome Driver start failed. Attempting to kill locked Chrome instance...", e);
            try {
                Runtime.getRuntime().exec(new String[] { "cmd", "/c",
                        "wmic process where \"name='chrome.exe' and commandline like '%chrome-data%'\" call terminate" })
                        .waitFor();
                java.io.File lockFile = new java.io.File("d:/work/workspace/java/rpa/chrome-data/SingletonLock");
                if (lockFile.exists()) {
                    lockFile.delete();
                }
                Thread.sleep(2000);
            } catch (Exception ex) {
                log.error("Failed to cleanup locked Chrome profile", ex);
            }
            return new ChromeDriver(options);
        }
    }

    public static WebElement findElement(WebDriver driver, String stepName, By selector, String elementName) {
        while (true) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(selector));
                log.info("已找到 {} : {}", elementName, selector);
                return element;
            } catch (Exception e) {
                log.info("步驟 : {}, 持續尋找中 {}...", stepName, elementName);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for " + elementName, ex);
                }
            }
        }
    }

    public static WebElement findClickableElement(WebDriver driver, String stepName, By selector, String elementName) {
        while (true) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(selector));
                return element;
            } catch (Exception e) {
                log.info("步驟 : {}, 持續尋找中 {}...", stepName, elementName);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for " + elementName, ex);
                }
            }
        }
    }

    public static void dispatchClickEvents(JavascriptExecutor js, WebElement element) {
        js.executeScript("""
                var evt1 = new MouseEvent('mousedown', {bubbles: true, cancelable: true, view: window});
                var evt2 = new MouseEvent('mouseup', {bubbles: true, cancelable: true, view: window});
                var evt3 = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                arguments[0].dispatchEvent(evt1);
                arguments[0].dispatchEvent(evt2);
                arguments[0].dispatchEvent(evt3);
                """, element);
    }
}
