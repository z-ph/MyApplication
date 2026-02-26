"""
Pytest configuration and fixtures for Appium tests

This module provides fixtures for setting up and tearing down
Appium driver connections for Android testing.
"""

import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.webdriver import WebDriver
from typing import Generator


# Default capabilities for Android testing
DEFAULT_CAPABILITIES = {
    "platformName": "Android",
    "deviceName": "emulator-5554",  # Default emulator
    "automationName": "UiAutomator2",
    "appPackage": "com.example.myapplication",
    "appActivity": ".MainActivity",
    "noReset": True,  # Don't reset app state between tests
    "newCommandTimeout": 300,
    "autoGrantPermissions": True,
}

# Appium server URL
APPIUM_SERVER_URL = "http://localhost:4723/wd/hub"


def pytest_addoption(parser):
    """Add custom command line options for pytest."""
    parser.addoption(
        "--device-name",
        action="store",
        default="emulator-5554",
        help="Device name or UDID for testing"
    )
    parser.addoption(
        "--appium-url",
        action="store",
        default=APPIUM_SERVER_URL,
        help="Appium server URL"
    )
    parser.addoption(
        "--app-path",
        action="store",
        default=None,
        help="Path to APK file (optional, uses installed app if not provided)"
    )


@pytest.fixture(scope="session")
def appium_url(request) -> str:
    """Get Appium server URL from command line or default."""
    return request.config.getoption("--appium-url")


@pytest.fixture(scope="session")
def device_name(request) -> str:
    """Get device name from command line or default."""
    return request.config.getoption("--device-name")


@pytest.fixture(scope="session")
def app_path(request) -> str | None:
    """Get APK path from command line or None."""
    return request.config.getoption("--app-path")


@pytest.fixture(scope="function")
def driver(
    appium_url: str,
    device_name: str,
    app_path: str | None
) -> Generator[WebDriver, None, None]:
    """
    Create and yield an Appium WebDriver instance.

    The driver is created before each test and quit after.
    """
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = device_name
    options.automation_name = "UiAutomator2"
    options.app_package = DEFAULT_CAPABILITIES["appPackage"]
    options.app_activity = DEFAULT_CAPABILITIES["appActivity"]
    options.no_reset = DEFAULT_CAPABILITIES["noReset"]
    options.new_command_timeout = DEFAULT_CAPABILITIES["newCommandTimeout"]
    options.auto_grant_permissions = DEFAULT_CAPABILITIES["autoGrantPermissions"]

    if app_path:
        options.app = app_path

    driver = webdriver.Remote(appium_url, options=options)

    try:
        yield driver
    finally:
        driver.quit()


@pytest.fixture(scope="function")
def restart_app(driver: WebDriver) -> None:
    """Restart the app before each test."""
    driver.close_app()
    driver.launch_app()


class PageObject:
    """Base class for page objects."""

    def __init__(self, driver: WebDriver):
        self.driver = driver

    def find_element_by_id(self, resource_id: str):
        """Find element by resource ID."""
        return self.driver.find_element("id", resource_id)

    def find_element_by_text(self, text: str):
        """Find element by text."""
        return self.driver.find_element(
            "-android uiautomator",
            f'new UiSelector().text("{text}")'
        )

    def find_element_by_text_contains(self, text: str):
        """Find element that contains text."""
        return self.driver.find_element(
            "-android uiautomator",
            f'new UiSelector().textContains("{text}")'
        )

    def find_element_by_desc(self, desc: str):
        """Find element by content description."""
        return self.driver.find_element(
            "-android uiautomator",
            f'new UiSelector().description("{desc}")'
        )

    def click_element_by_id(self, resource_id: str):
        """Click element by resource ID."""
        self.find_element_by_id(resource_id).click()

    def click_element_by_text(self, text: str):
        """Click element by text."""
        self.find_element_by_text(text).click()

    def wait_for_element_by_text(self, text: str, timeout: int = 10):
        """Wait for element with text to appear."""
        from appium.webdriver.common.appiumby import AppiumBy
        self.driver.implicitly_wait(timeout)
        element = self.driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR,
            f'new UiSelector().text("{text}")')
        self.driver.implicitly_wait(0)  # Reset implicit wait
        return element

    def input_text_by_id(self, resource_id: str, text: str):
        """Input text into element by resource ID."""
        element = self.find_element_by_id(resource_id)
        element.clear()
        element.send_keys(text)

    def is_element_present_by_text(self, text: str) -> bool:
        """Check if element with text is present."""
        try:
            self.driver.implicitly_wait(2)
            self.find_element_by_text(text)
            return True
        except Exception:
            return False
        finally:
            self.driver.implicitly_wait(0)


class ChatScreenPage(PageObject):
    """Page object for Chat screen."""

    def get_message_input(self):
        """Get the message input field."""
        # Try to find by hint text
        return self.driver.find_element(
            "-android uiautomator",
            'new UiSelector().textContains("输入消息")'
        )

    def get_send_button(self):
        """Get the send button."""
        return self.driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("Send")'
        )

    def get_menu_button(self):
        """Get the menu button for session drawer."""
        return self.driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("Sessions")'
        )

    def type_message(self, message: str):
        """Type a message in the input field."""
        input_field = self.get_message_input()
        input_field.click()
        input_field.send_keys(message)

    def send_message(self):
        """Click the send button."""
        self.get_send_button().click()

    def is_message_displayed(self, message: str) -> bool:
        """Check if a message is displayed in the chat."""
        return self.is_element_present_by_text(message)

    def open_session_drawer(self):
        """Open the session drawer."""
        self.get_menu_button().click()


class ApiConfigScreenPage(PageObject):
    """Page object for API Config screen."""

    def navigate_to_config(self):
        """Navigate to API config screen from main screen."""
        # This depends on your app's navigation structure
        # Example: Click settings icon, then API config
        settings_btn = self.driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("Settings")'
        )
        settings_btn.click()

    def get_add_config_button(self):
        """Get the add config button."""
        return self.driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("添加配置")'
        )

    def get_config_by_name(self, name: str):
        """Get a config card by name."""
        return self.driver.find_element(
            "-android uiautomator",
            f'new UiSelector().textContains("{name}")'
        )

    def is_empty_state_displayed(self) -> bool:
        """Check if empty state is displayed."""
        return self.is_element_present_by_text("暂无 API 配置")


@pytest.fixture
def chat_page(driver: WebDriver) -> ChatScreenPage:
    """Get ChatScreen page object."""
    return ChatScreenPage(driver)


@pytest.fixture
def api_config_page(driver: WebDriver) -> ApiConfigScreenPage:
    """Get ApiConfigScreen page object."""
    return ApiConfigScreenPage(driver)
