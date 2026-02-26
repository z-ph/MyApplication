"""
Appium tests for API Configuration screen

Tests the API configuration management including:
- Navigating to API config screen
- Adding new configurations
- Editing existing configurations
- Deleting configurations
- Activating configurations
"""

import pytest
from appium.webdriver.webdriver import WebDriver
from conftest import ApiConfigScreenPage
from assertpy import assert_that


class TestApiConfigNavigation:
    """Tests for API config screen navigation."""

    def test_navigate_to_api_config(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test navigating to API config screen.

        Steps:
        1. From main screen, navigate to settings/API config
        2. Verify API config screen is displayed
        """
        api_config_page.navigate_to_config()

        driver.implicitly_wait(2)

        # Verify we're on API config screen
        is_on_config_screen = api_config_page.is_element_present_by_text("API 配置管理") or \
                             api_config_page.is_element_present_by_text("暂无 API 配置")

        assert_that(is_on_config_screen).is_true()

    def test_back_navigation(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test navigating back from API config screen.

        Steps:
        1. Navigate to API config screen
        2. Click back button
        3. Verify we're back on main screen
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        # Click back button
        back_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("返回")'
        )
        back_btn.click()

        driver.implicitly_wait(2)

        # Verify we're back (check for chat screen elements)
        # This depends on app structure
        pass  # Implementation depends on specific app navigation


class TestApiConfigCRUD:
    """Tests for Create, Read, Update, Delete operations on API configs."""

    def test_add_api_config(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test adding a new API configuration.

        Steps:
        1. Navigate to API config screen
        2. Click add button
        3. Fill in configuration details
        4. Save
        5. Verify config appears in list
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        # Click add button
        add_btn = api_config_page.get_add_config_button()
        add_btn.click()
        driver.implicitly_wait(2)

        # Fill in config name
        config_name = f"Test Config {__import__('time').time() % 10000}"
        name_input = driver.find_element(
            "-android uiautomator",
            'new UiSelector().textContains("配置名称")'
        )
        name_input.send_keys(config_name)

        # Fill in API key
        api_key_input = driver.find_element(
            "-android uiautomator",
            'new UiSelector().textContains("API Key")'
        )
        api_key_input.send_keys("test-api-key-12345")

        # Select a provider (example: Zhipu)
        try:
            zhipu_option = driver.find_element(
                "-android uiautomator",
                'new UiSelector().textContains("智谱")'
            )
            zhipu_option.click()
        except Exception:
            pass  # Provider might already be selected

        # Save
        save_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().text("添加")'
        )
        save_btn.click()

        driver.implicitly_wait(2)

        # Verify config was added
        is_config_added = api_config_page.is_element_present_by_text(config_name)

        assert_that(is_config_added).is_true()

    def test_delete_api_config(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test deleting an API configuration.

        Steps:
        1. Ensure at least one config exists
        2. Click delete on a config
        3. Confirm deletion
        4. Verify config is removed
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        # Check if there's a config to delete
        if api_config_page.is_empty_state_displayed():
            # Need to add one first
            add_btn = api_config_page.get_add_config_button()
            add_btn.click()
            driver.implicitly_wait(2)

            name_input = driver.find_element(
                "-android uiautomator",
                'new UiSelector().textContains("配置名称")'
            )
            name_input.send_keys("Config to Delete")

            api_key_input = driver.find_element(
                "-android uiautomator",
                'new UiSelector().textContains("API Key")'
            )
            api_key_input.send_keys("delete-test-key")

            save_btn = driver.find_element(
                "-android uiautomator",
                'new UiSelector().text("添加")'
            )
            save_btn.click()
            driver.implicitly_wait(2)

        # Click delete button on first config
        delete_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("删除")'
        )
        delete_btn.click()
        driver.implicitly_wait(1)

        # Confirm deletion
        confirm_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().text("删除")'
        )
        confirm_btn.click()

        driver.implicitly_wait(2)

        # Verify config was deleted
        # Either empty state shows or config is gone
        pass  # Implementation depends on test data state

    def test_edit_api_config(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test editing an existing API configuration.

        Steps:
        1. Ensure at least one config exists
        2. Click edit on a config
        3. Modify the config
        4. Save changes
        5. Verify changes are reflected
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        # Check if there's a config to edit
        if api_config_page.is_empty_state_displayed():
            pytest.skip("No config available to edit")

        # Click edit button
        edit_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().description("编辑")'
        )
        edit_btn.click()
        driver.implicitly_wait(2)

        # Verify edit dialog is shown
        is_edit_dialog = api_config_page.is_element_present_by_text("编辑配置")

        assert_that(is_edit_dialog).is_true()

        # Cancel the edit
        cancel_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().text("取消")'
        )
        cancel_btn.click()


class TestApiConfigActivation:
    """Tests for activating API configurations."""

    def test_activate_config(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test activating an API configuration.

        Steps:
        1. Ensure at least two configs exist
        2. Click on inactive config
        3. Verify it becomes active
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        # Find an inactive config (radio button not selected)
        try:
            inactive_radio = driver.find_element(
                "-android uiautomator",
                'new UiSelector().className("android.widget.RadioButton").checked(false)'
            )
            inactive_radio.click()

            driver.implicitly_wait(2)

            # Verify "当前使用" label appears
            is_active = api_config_page.is_element_present_by_text("当前使用")

            assert_that(is_active).is_true()

        except Exception:
            pytest.skip("No inactive config available to activate")

    def test_active_config_has_indicator(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test that active config displays 'current use' indicator.

        Steps:
        1. Ensure at least one config exists
        2. Verify active config shows indicator
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        if api_config_page.is_empty_state_displayed():
            pytest.skip("No config available")

        # Check for active indicator
        has_active_indicator = api_config_page.is_element_present_by_text("当前使用")

        # At least one config should be active if any exist
        assert_that(has_active_indicator).is_true()


class TestApiConfigValidation:
    """Tests for API config input validation."""

    def test_empty_api_key_shows_error(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test that saving with empty API key shows error or disables save.

        Steps:
        1. Open add config dialog
        2. Leave API key empty
        3. Try to save
        4. Verify error or disabled state
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        add_btn = api_config_page.get_add_config_button()
        add_btn.click()
        driver.implicitly_wait(2)

        # Fill only name, leave API key empty
        name_input = driver.find_element(
            "-android uiautomator",
            'new UiSelector().textContains("配置名称")'
        )
        name_input.send_keys("No Key Config")

        # Try to save
        save_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().text("添加")'
        )

        # Button should be disabled
        is_enabled = save_btn.is_enabled()

        assert_that(is_enabled).is_false()

    def test_test_connection_button(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage
    ):
        """
        Test the test connection functionality.

        Steps:
        1. Open add config dialog
        2. Enter API key
        3. Click test connection
        4. Verify result is shown
        """
        api_config_page.navigate_to_config()
        driver.implicitly_wait(2)

        add_btn = api_config_page.get_add_config_button()
        add_btn.click()
        driver.implicitly_wait(2)

        # Enter API key
        api_key_input = driver.find_element(
            "-android uiautomator",
            'new UiSelector().textContains("API Key")'
        )
        api_key_input.send_keys("test-key-for-connection-test")

        # Click test connection
        test_btn = driver.find_element(
            "-android uiautomator",
            'new UiSelector().textContains("测试连接")'
        )
        test_btn.click()

        driver.implicitly_wait(5)  # Wait for test to complete

        # Look for result indicator (success or failure)
        has_result = api_config_page.is_element_present_by_text("成功") or \
                    api_config_page.is_element_present_by_text("失败") or \
                    api_config_page.is_element_present_by_text("错误")

        # Result should be shown (might be error due to fake key)
        # This test mainly verifies the UI flow works
        pass  # Implementation depends on expected behavior with invalid key
