from selenium import webdriver
import time
import unittest
import HTMLTestRunnerCN
import os

driver_path = "./driver/chromedriver"
test_path = 'https://www.cnki.net/'


class MyTestCase(unittest.TestCase):
    def setUp(self):
        options = webdriver.ChromeOptions()
        prefs = {'profile.default_content_settings.popups': 0,
                 'download.default_directory': os.getcwd()}  # 下载路径为当前文件夹
        options.add_experimental_option('prefs', prefs)
        self.driver = webdriver.Chrome(driver_path, chrome_options=options)


    def tearDown(self):
        self.driver.quit()

    def testCaseInputAndButtonClick(self):
        self.driver.get(test_path)
        time.sleep(2)  # 缓冲
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)   # 缓冲
        # check the input correct or not
        result = self.driver.find_element_by_xpath('//*[@id="txt_search"]').get_attribute('value')
        self.assertEqual('软件测试', result, '输入框以及按钮测试成功')

    def testMutilSelect(self):
        self.driver.get(test_path)
        time.sleep(2)
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)   # 缓冲
        result1 = self.driver.find_element_by_xpath('/html/body/div[5]/div[1]/div/div/a/em').text

        # get the second result
        self.driver.get(test_path)
        time.sleep(2)
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        select1 = self.driver.find_element_by_xpath('//*[@id="CIPD"]/i')
        select1.click()
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)   # 缓冲
        result2 = self.driver.find_element_by_xpath('/html/body/div[5]/div[1]/div/div/a/em').text

        # get the third result
        self.driver.get(test_path)
        time.sleep(2)
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        select1 = self.driver.find_element_by_xpath('//*[@id="CIPD"]/i')
        select1.click()
        select2 = self.driver.find_element_by_xpath('//*[@id="CDMD"]/i')
        select2.click()
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)   # 缓冲
        result3 = self.driver.find_element_by_xpath('/html/body/div[5]/div[1]/div/div/a/em').text

        self.assertNotEqual(result1, result2, "多选框测试1通过")
        self.assertNotEqual(result1, result3, '多选框测试2通过')
        self.assertNotEqual(result2, result3, '多选框测试3通过')


    def testMutilSelect2(self):
        self.driver.get(test_path)
        time.sleep(2)
        Input1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input1.send_keys("软件测试")
        Input_Button = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        Input_Button.click()
        time.sleep(2)  # 缓冲
        All_select = self.driver.find_element_by_xpath('//*[@id="selectCheckAll1"]')
        All_select.click()
        # check whether we select all or not
        g_click = self.driver.find_element_by_xpath('//*[@id="selectCount"]').text
        self.assertNotEqual(0, g_click, '多选测试2通过')

    def testPullTable(self):
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
        self.assertEqual('篇名：软件工程', input_value, '下拉框测试通过')


    def testLi(self):
        self.driver.get(test_path)
        time.sleep(2)
        elemnt1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/ul/li[2]')
        elemnt1.click()
        time.sleep(2)
        input_value1 = self.driver.find_element_by_xpath(
            '/html/body/div[1]/div[2]/div/div[3]/ul[1]/li[9]/a').text
        elemnt2 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/ul/li[3]')
        elemnt2.click()
        time.sleep(2)
        input_value2 = self.driver.find_element_by_xpath(
            '/html/body/div[1]/div[2]/div/div[3]/ul[2]/li/a').text
        self.assertEqual('方法', input_value1, '列表测试1通过')
        self.assertEqual('中国引文数据库', input_value2, '列表测试2通过')


    def testDownload(self):
        self.driver.get(test_path)
        time.sleep(2)  # 缓冲
        Input = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[1]')
        Input.send_keys("软件测试")
        elemnt1 = self.driver.find_element_by_xpath('/html/body/div[1]/div[2]/div/div[1]/input[2]')
        elemnt1.click()
        time.sleep(2)  # 缓冲
        elemnt2 = self.driver.find_element_by_xpath('/html/body/div[5]/div[2]/div[2]/div[2]/form/div/table/tbody/tr[1]/td[2]/a')
        elemnt2.click()  # 选择论文并点击
        time.sleep(2)  # 缓冲
        self.driver.switch_to.window(self.driver.window_handles[-1])  # 最新打开的窗口

        elemnt3 = self.driver.find_element_by_xpath('/html/body/div[2]/div[1]/div[3]/div/div/div[9]/div[1]/ul/li[4]/a')
        elemnt3.click()
        time.sleep(5)
        la = os.listdir(os.getcwd())  # 获得当前路径下所有文件
        for i in la:
            if i == "航空通信电台软件测试系统的设计与实现_王月波.pdf":
                self.assertEqual('航空通信电台软件测试系统的设计与实现_王月波.pdf', i, '列表测试1通过')


def Suite():
    #定义一个单元测试容器
    suiteTest = unittest.TestSuite()
    #将测试用例加入到容器
    suiteTest.addTest(MyTestCase("testCaseInputAndButtonClick"))
    suiteTest.addTest(MyTestCase("testMutilSelect"))
    suiteTest.addTest(MyTestCase("testMutilSelect2"))
    suiteTest.addTest(MyTestCase("testPullTable"))
    suiteTest.addTest(MyTestCase("testLi"))
    suiteTest.addTest(MyTestCase("testDownload"))
    return suiteTest


if __name__ == '__main__':
    #确定生成报告的路径
    filePath ='./report/ReportCN.html'
    fp = open(filePath,'wb')
    #生成报告的Title,描述
    runner = HTMLTestRunnerCN.HTMLTestReportCN(
        stream=fp,
        title='自动化测试报告',
        #description='详细测试用例结果',
        tester='YYH&ZYT'
        )
    #运行测试用例
    runner.run(Suite())
    # 关闭文件，否则会无法生成文件
    #fp.close()

