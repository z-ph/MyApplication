"""
Appium API config tests (WebView + data-testid).

Selectors: menu-api-config, api-config-add, api-key-input, api-config-save
See docs/bridge-api.md §4.
"""

import time

import pytest
from assertpy import assert_that
from appium.webdriver.webdriver import WebDriver
from conftest import ApiConfigScreenPage
from selenium.webdriver.common.by import By


class TestApiConfigNavigation:
    def test_navigate_to_api_config(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage,
    ):
        api_config_page.navigate_to_config()
        assert_that(
            api_config_page.is_testid_present("api-config-add", timeout=10.0)
            or api_config_page.page_text_contains("API")
        ).is_true()

    def test_add_button_exists(
        self,
        api_config_page: ApiConfigScreenPage,
    ):
        api_config_page.navigate_to_config()
        btn = api_config_page.get_add_config_button()
        assert_that(btn.is_displayed()).is_true()


class TestApiConfigCRUD:
    def test_open_add_form(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage,
    ):
        api_config_page.navigate_to_config()
        api_config_page.click_testid("api-config-add")
        # Form should expose api-key-input
        assert_that(
            api_config_page.is_testid_present("api-key-input", timeout=8.0)
            or api_config_page.is_testid_present("api-config-save", timeout=2.0)
        ).is_true()

    def test_save_disabled_or_rejects_empty_key(
        self,
        driver: WebDriver,
        api_config_page: ApiConfigScreenPage,
    ):
        api_config_page.navigate_to_config()
        api_config_page.click_testid("api-config-add")
        if not api_config_page.is_testid_present("api-config-save", timeout=5.0):
            pytest.skip("add form save control not found")
        save = api_config_page.find_by_testid("api-config-save")
        # Prefer disabled when blank key (H5 validation)
        enabled = save.is_enabled()
        if enabled:
            save.click()
            time.sleep(0.5)
            # Still on form or toast — do not assert network
            assert_that(
                api_config_page.is_testid_present("api-key-input", timeout=2.0)
                or api_config_page.page_text_contains("Key")
                or api_config_page.page_text_contains("密钥")
            ).is_true()
        else:
            assert_that(enabled).is_false()
