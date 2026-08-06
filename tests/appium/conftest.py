"""
Pytest fixtures and page objects for Appium (H5 WebView + data-testid).

Strategy (docs/bridge-api.md §4):
1. Switch to Capacitor WEBVIEW context after launch.
2. Locate critical controls via CSS [data-testid=...].
3. Package com.example.myapplication unchanged.

Requires: Appium server, device/emulator with app installed.
Without those, pytest collection still works; tests fail at driver connect.
"""

from __future__ import annotations

import time
from typing import Generator

import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
from appium.webdriver.webdriver import WebDriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


DEFAULT_CAPABILITIES = {
    "platformName": "Android",
    "deviceName": "emulator-5554",
    "automationName": "UiAutomator2",
    "appPackage": "com.example.myapplication",
    "appActivity": ".MainActivity",
    "noReset": True,
    "newCommandTimeout": 300,
    "autoGrantPermissions": True,
    # Chromedriver for hybrid WebView (Capacitor)
    "chromedriverAutodownload": True,
}

# Appium 2 default base path is / (no /wd/hub). Override with --appium-url.
APPIUM_SERVER_URL = "http://localhost:4723"


def pytest_addoption(parser):
    parser.addoption(
        "--device-name",
        action="store",
        default="emulator-5554",
        help="Device name or UDID for testing",
    )
    parser.addoption(
        "--appium-url",
        action="store",
        default=APPIUM_SERVER_URL,
        help="Appium server URL",
    )
    parser.addoption(
        "--app-path",
        action="store",
        default=None,
        help="Path to APK file (optional)",
    )


@pytest.fixture(scope="session")
def appium_url(request) -> str:
    return request.config.getoption("--appium-url")


@pytest.fixture(scope="session")
def device_name(request) -> str:
    return request.config.getoption("--device-name")


@pytest.fixture(scope="session")
def app_path(request) -> str | None:
    return request.config.getoption("--app-path")


@pytest.fixture(scope="function")
def driver(
    appium_url: str,
    device_name: str,
    app_path: str | None,
) -> Generator[WebDriver, None, None]:
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = device_name
    options.automation_name = "UiAutomator2"
    options.app_package = DEFAULT_CAPABILITIES["appPackage"]
    options.app_activity = DEFAULT_CAPABILITIES["appActivity"]
    options.no_reset = DEFAULT_CAPABILITIES["noReset"]
    options.new_command_timeout = DEFAULT_CAPABILITIES["newCommandTimeout"]
    options.auto_grant_permissions = DEFAULT_CAPABILITIES["autoGrantPermissions"]
    # Best-effort; ignored if server lacks the capability.
    try:
        options.set_capability("chromedriverAutodownload", True)
    except Exception:
        pass

    if app_path:
        options.app = app_path

    drv = webdriver.Remote(appium_url, options=options)
    try:
        yield drv
    finally:
        drv.quit()


class PageObject:
    """Base page object: native NATIVE_APP helpers + WebView helpers."""

    def __init__(self, driver: WebDriver):
        self.driver = driver

    def switch_to_webview(self, timeout: float = 20.0) -> str:
        """Switch to first WEBVIEW_* context. Returns context name."""
        deadline = time.time() + timeout
        last_contexts: list[str] = []
        while time.time() < deadline:
            try:
                contexts = list(self.driver.contexts)
                last_contexts = contexts
                for ctx in contexts:
                    if "WEBVIEW" in ctx.upper():
                        self.driver.switch_to.context(ctx)
                        # Wait for document ready
                        WebDriverWait(self.driver, 10).until(
                            lambda d: d.execute_script("return document.readyState")
                            == "complete"
                        )
                        return ctx
            except Exception:
                pass
            time.sleep(0.4)
        raise TimeoutError(
            f"No WEBVIEW context within {timeout}s; last contexts={last_contexts}"
        )

    def switch_to_native(self) -> None:
        self.driver.switch_to.context("NATIVE_APP")

    def by_testid(self, testid: str):
        return (By.CSS_SELECTOR, f'[data-testid="{testid}"]')

    def find_by_testid(self, testid: str, timeout: float = 10.0):
        self.ensure_webview()
        wait = WebDriverWait(self.driver, timeout)
        return wait.until(EC.presence_of_element_located(self.by_testid(testid)))

    def click_testid(self, testid: str, timeout: float = 10.0) -> None:
        el = self.find_by_testid(testid, timeout=timeout)
        wait = WebDriverWait(self.driver, timeout)
        wait.until(EC.element_to_be_clickable(self.by_testid(testid)))
        el.click()

    def ensure_webview(self) -> None:
        ctx = getattr(self.driver, "current_context", None) or ""
        if "WEBVIEW" not in str(ctx).upper():
            self.switch_to_webview()

    def is_testid_present(self, testid: str, timeout: float = 3.0) -> bool:
        try:
            self.find_by_testid(testid, timeout=timeout)
            return True
        except Exception:
            return False

    def page_text_contains(self, text: str) -> bool:
        self.ensure_webview()
        try:
            body = self.driver.find_element(By.TAG_NAME, "body").text
            return text in body
        except Exception:
            return False

    # --- legacy native helpers (still useful for system dialogs) ---

    def find_element_by_text(self, text: str):
        return self.driver.find_element(
            AppiumBy.ANDROID_UIAUTOMATOR,
            f'new UiSelector().text("{text}")',
        )

    def is_element_present_by_text(self, text: str) -> bool:
        try:
            self.ensure_webview()
            return self.page_text_contains(text)
        except Exception:
            return False


class ChatScreenPage(PageObject):
    """Chat tab: data-testid chat-input / send-btn / cancel-btn / msg-list."""

    def prepare(self) -> None:
        """Enter WebView and land on chat if possible."""
        self.switch_to_webview()
        # If permission gate still showing, continue if present
        if self.is_testid_present("perm-continue", timeout=2.0):
            try:
                self.click_testid("perm-continue", timeout=2.0)
            except Exception:
                pass
        # Ensure chat controls exist (may need tab bar)
        if not self.is_testid_present("chat-input", timeout=5.0):
            # Native Capacitor uses HashRouter only — do not touch pathname/pushState.
            try:
                self.driver.execute_script("window.location.hash = '#/tabs/chat';")
            except Exception:
                pass
            self.find_by_testid("chat-input", timeout=15.0)

    def get_message_input(self):
        return self.find_by_testid("chat-input")

    def get_send_button(self):
        return self.find_by_testid("send-btn")

    def type_message(self, message: str) -> None:
        el = self.get_message_input()
        el.click()
        # antd-mobile TextArea may wrap textarea
        try:
            el.clear()
        except Exception:
            pass
        el.send_keys(message)
        # If wrapper, try nested textarea
        try:
            ta = el.find_element(By.TAG_NAME, "textarea")
            ta.clear()
            ta.send_keys(message)
        except Exception:
            try:
                ta = self.driver.find_element(
                    By.CSS_SELECTOR, '[data-testid="chat-input"] textarea'
                )
                ta.clear()
                ta.send_keys(message)
            except Exception:
                pass

    def send_message(self) -> None:
        self.click_testid("send-btn")

    def is_message_displayed(self, message: str) -> bool:
        return self.page_text_contains(message)

    def open_session_drawer(self) -> None:
        self.click_testid("session-menu")


class ApiConfigScreenPage(PageObject):
    """API config via Profile menu or settings data-testid."""

    def prepare(self) -> None:
        self.switch_to_webview()

    def navigate_to_config(self) -> None:
        self.prepare()
        # Prefer profile menu item
        if self.is_testid_present("menu-api-config", timeout=3.0):
            self.click_testid("menu-api-config")
            return
        if self.is_testid_present("settings-api-config", timeout=2.0):
            self.click_testid("settings-api-config")
            return
        # HashRouter-only fallback (native WebView; no pathname/pushState)
        try:
            self.driver.execute_script("window.location.hash = '#/api-config';")
        except Exception:
            pass
        self.find_by_testid("api-config-add", timeout=15.0)

    def get_add_config_button(self):
        return self.find_by_testid("api-config-add")

    def is_empty_state_displayed(self) -> bool:
        return self.page_text_contains("暂无") or self.page_text_contains("API")


@pytest.fixture
def chat_page(driver: WebDriver) -> ChatScreenPage:
    page = ChatScreenPage(driver)
    page.prepare()
    return page


@pytest.fixture
def api_config_page(driver: WebDriver) -> ApiConfigScreenPage:
    page = ApiConfigScreenPage(driver)
    page.prepare()
    return page
