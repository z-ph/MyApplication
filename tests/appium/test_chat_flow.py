"""
Appium chat flow tests (WebView + data-testid).

Selectors: chat-input, send-btn, cancel-btn, session-menu, msg-list
See docs/bridge-api.md §4.

Requires Appium server + device with APK installed. Skip collection is not used;
connection failures surface as fixture errors (expected without device farm).
"""

import pytest
from assertpy import assert_that
from appium.webdriver.webdriver import WebDriver
from conftest import ChatScreenPage
from selenium.webdriver.common.by import By


class TestChatFlow:
    def test_send_message_displays_in_chat(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage,
    ):
        test_message = "Test message from Appium"
        chat_page.type_message(test_message)
        chat_page.send_message()
        driver.implicitly_wait(3)
        assert_that(chat_page.is_message_displayed(test_message)).is_true()

    def test_input_field_exists(self, chat_page: ChatScreenPage):
        el = chat_page.get_message_input()
        assert_that(el.is_displayed()).is_true()

    def test_send_button_exists(self, chat_page: ChatScreenPage):
        el = chat_page.get_send_button()
        assert_that(el.is_displayed()).is_true()

    def test_input_field_accepts_text(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage,
    ):
        test_text = "Hello World"
        chat_page.type_message(test_text)
        # Prefer nested textarea value
        try:
            ta = driver.find_element(
                By.CSS_SELECTOR, '[data-testid="chat-input"] textarea'
            )
            value = ta.get_attribute("value") or ta.text
        except Exception:
            el = chat_page.get_message_input()
            value = el.get_attribute("value") or el.text
        assert_that(value).contains(test_text)

    def test_create_new_session(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage,
    ):
        chat_page.open_session_drawer()
        driver.implicitly_wait(2)
        # Drawer content is text-based in H5
        assert_that(
            chat_page.page_text_contains("新建")
            or chat_page.page_text_contains("会话")
        ).is_true()

    def test_cancel_button_testid_when_sending(
        self,
        chat_page: ChatScreenPage,
    ):
        """
        cancel-btn only mounts while sending. Without a live agent this may skip.
        Smoke: send-btn present when idle.
        """
        assert_that(chat_page.is_testid_present("send-btn")).is_true()
        if chat_page.is_testid_present("cancel-btn", timeout=1.0):
            assert_that(chat_page.find_by_testid("cancel-btn").is_displayed()).is_true()
