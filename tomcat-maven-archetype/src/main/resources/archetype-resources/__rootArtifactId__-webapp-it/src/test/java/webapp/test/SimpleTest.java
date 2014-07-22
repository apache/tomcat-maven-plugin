#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.webapp.test;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author Olivier Lamy
 */
@RunWith(JUnit4.class)
public class SimpleTest
{
    private WebDriver driver;
    private String serverUrl;

    @Before
    public void setup() throws Exception
    {
        this.serverUrl = System.getProperty("serverUrl", "http://localhost:9898/");
        if(!this.serverUrl.endsWith("/")) {
            this.serverUrl += "/";
        }

        this.driver = new HtmlUnitDriver(true);  // HtmlUnitDriver with JavaScript enabled
    }

    @After
    public void teardown() throws Exception
    {
        this.driver.close();
    }

    @Test
    public void testSimple() throws Exception
    {
        this.driver.get(this.serverUrl + "index.html");

        String whoToSend = "foo";

        WebElement who = this.driver.findElement(By.id("who"));
        who.sendKeys(whoToSend);

        WebElement sendBtn = this.driver.findElement(By.id("send-btn"));
        sendBtn.click();

        // wait 5 secs for ajax response
        new WebDriverWait(this.driver, 5).until(
                ExpectedConditions.textToBePresentInElement(By.id("response"), whoToSend)
        );

        WebElement response = this.driver.findElement(By.id("response"));
        String text = response.getText();

        Assert.assertEquals("Hello " + whoToSend, text);

    }
    
}
