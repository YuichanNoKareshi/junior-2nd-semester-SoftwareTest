from selenium import webdriver
import time
import unittest
import HTMLTestRunnerCN

driver_path = "./driver/chromedriver"
test_path = 'https://www.cnki.net/'


class MyTestCase():
    def __init__(self):
        self.option = webdriver.ChromeOptions()
        self.driver = webdriver.Chrome(driver_path)


    def testCaseInputAndButtonClick(self):
        self.driver.get(test_path)
        time.sleep(2)  # 缓冲
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)   # 缓冲
        get_result = self.driver.find_element_by_xpath('//*[@id="txt_search"]')
        result = get_result.get_attribute('value')
        print(result)

    def test2(self):
        self.driver.get(test_path)
        time.sleep(2)  # 缓冲
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)   # 缓冲
        result1 = self.driver.find_element_by_xpath('/html/body/div[5]/div[1]/div/div/a/em').text
        self.driver.get(test_path)
        time.sleep(2)  # 缓冲
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")

    def test3(self):
        self.driver.get(test_path)
        time.sleep(2)  # 缓冲
        Input1 = self.driver.find_element_by_xpath('//*[@id="DBFieldList"]/ul/li[4]/a')
        Input1.click()
        Input2 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input2.send_keys("软件测试")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(10)

    def test4(self):
        self.driver.get(test_path)
        time.sleep(2)
        elemnt = self.driver.find_element_by_xpath('//*[@id="DBFieldBox"]/div[1]')
        elemnt.click()
        new_choice = self.driver.find_element_by_xpath('//*[@id="DBFieldList"]/ul/li[4]/a')
        new_choice.click()
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件工程")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)  # 缓冲
        input_value = self.driver.find_element_by_xpath(
            '/html/body/div[5]/div[2]/div[2]/div[1]/div/span[2]/a').text
        print(input_value)


tmp = MyTestCase()
tmp.test4()
