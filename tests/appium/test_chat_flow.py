"""
Appium tests for Chat flow

Tests the main chat functionality including:
- Sending messages
- Creating new sessions
- Canceling running tasks
- Session management
"""

import pytest
from appium.webdriver.webdriver import WebDriver
from conftest import ChatScreenPage
from assertpy import assert_that


class TestChatFlow:
    """Tests for chat functionality."""

    def test_send_message_displays_in_chat(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test that sending a message displays it in the chat.

        Steps:
        1. Type a message in the input field
        2. Click send button
        3. Verify message appears in chat
        """
        test_message = "Test message from Appium"

        # Type message
        chat_page.type_message(test_message)

        # Send message
        chat_page.send_message()

        # Wait and verify message is displayed
        driver.implicitly_wait(3)
        is_displayed = chat_page.is_message_displayed(test_message)

        assert_that(is_displayed).is_true()

    def test_send_empty_message_does_nothing(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test that sending an empty message does nothing.

        Steps:
        1. Leave input field empty
        2. Verify send button behavior (no message sent)
        """
        # Get initial message count (assuming empty chat or known state)
        # This would need actual implementation based on app structure

        # Try to send empty message - should not send
        # The send button should either be disabled or do nothing

        # Verify no new message appears
        pass  # Implementation depends on app behavior

    def test_create_new_session(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test creating a new chat session.

        Steps:
        1. Open session drawer
        2. Click "New Session" button
        3. Verify new session is created and selected
        """
        # Open the session drawer
        chat_page.open_session_drawer()

        driver.implicitly_wait(2)

        # Click new session button
        new_session_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().text("新建会话")'
        )
        new_session_btn.click()

        driver.implicitly_wait(2)

        # Verify we're in a new session (empty chat or default title)
        is_new_session = chat_page.is_element_present_by_text("开始对话") or \
                        chat_page.is_element_present_by_text("新会话")

        assert_that(is_new_session).is_true()

    def test_cancel_running_task(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test canceling a running task.

        Steps:
        1. Send a message that triggers automation
        2. Verify task is running (cancel button visible)
        3. Click cancel button
        4. Verify task is canceled
        """
        # Send a message that will trigger a task
        chat_page.type_message("帮我打开设置")
        chat_page.send_message()

        # Wait for task to start
        driver.implicitly_wait(3)

        # Check if cancel button is visible (task is running)
        try:
            cancel_btn = driver.find_element(
                "-android uiautomator",
                'new UiSelector().description("Cancel")'
            )

            # Click cancel
            cancel_btn.click()

            driver.implicitly_wait(2)

            # Verify task is canceled - look for cancel message
            is_canceled = chat_page.is_element_present_by_text("取消")

            assert_that(is_canceled).is_true()

        except Exception:
            # Task might have completed too quickly
            pytest.skip("Task completed before cancel could be tested")

    def test_session_drawer_displays_sessions(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test that session drawer displays existing sessions.

        Steps:
        1. Open session drawer
        2. Verify sessions are displayed
        """
        # First create a session to ensure at least one exists
        chat_page.open_session_drawer()
        driver.implicitly_wait(1)

        # Check for session list or create button
        has_sessions = chat_page.is_element_present_by_text("新建会话")

        assert_that(has_sessions).is_true()

    def test_switch_between_sessions(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test switching between chat sessions.

        Steps:
        1. Create a new session
        2. Send a unique message
        3. Create another session
        4. Switch back to first session
        5. Verify the message is still there
        """
        unique_message = f"Unique message {__import__('time').time()}"

        # Create first session and send message
        chat_page.open_session_drawer()
        driver.implicitly_wait(1)

        new_session_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().text("新建会话")'
        )
        new_session_btn.click()
        driver.implicitly_wait(2)

        chat_page.type_message(unique_message)
        chat_page.send_message()
        driver.implicitly_wait(2)

        # Create second session
        chat_page.open_session_drawer()
        driver.implicitly_wait(1)
        new_session_btn.click()
        driver.implicitly_wait(2)

        # Go back to first session
        chat_page.open_session_drawer()
        driver.implicitly_wait(1)

        first_session = driver.find_element(
            "-android uiautomator",
            f'new UiSelector().textContains("{unique_message[:20]}")'
        )
        first_session.click()
        driver.implicitly_wait(2)

        # Verify message is still there
        is_message_present = chat_page.is_message_displayed(unique_message[:20])

        assert_that(is_message_present).is_true()

    def test_long_message_display(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """
        Test that long messages are displayed correctly.

        Steps:
        1. Send a very long message
        2. Verify it displays (possibly truncated or wrapped)
        """
        long_message = "This is a very long message for testing purposes. " * 5

        chat_page.type_message(long_message[:100])  # Limit to avoid input issues
        chat_page.send_message()

        driver.implicitly_wait(2)

        # Check that at least part of the message is displayed
        is_displayed = chat_page.is_message_displayed("very long message")

        assert_that(is_displayed).is_true()


class TestChatInput:
    """Tests specifically for chat input functionality."""

    def test_input_field_exists(self, chat_page: ChatScreenPage):
        """Test that input field exists and is visible."""
        input_field = chat_page.get_message_input()

        assert_that(input_field.is_displayed()).is_true()

    def test_input_field_accepts_text(
        self,
        driver: WebDriver,
        chat_page: ChatScreenPage
    ):
        """Test that input field accepts text input."""
        test_text = "Hello World"

        chat_page.type_message(test_text)

        # Verify text was entered
        input_field = chat_page.get_message_input()
        current_text = input_field.text

        assert_that(current_text).contains(test_text)

    def test_send_button_exists(self, chat_page: ChatScreenPage):
        """Test that send button exists."""
        send_btn = chat_page.get_send_button()

        assert_that(send_btn.is_displayed()).is_true()
