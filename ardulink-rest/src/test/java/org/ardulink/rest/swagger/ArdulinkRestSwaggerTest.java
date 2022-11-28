/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.ardulink.rest.swagger;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.port;
import static io.restassured.http.ContentType.JSON;
import static java.awt.GraphicsEnvironment.isHeadless;
import static org.ardulink.core.Pin.analogPin;
import static org.ardulink.testsupport.mock.TestSupport.getMock;
import static org.ardulink.util.ServerSockets.freePort;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.file.Paths;

import org.ardulink.core.Link;
import org.ardulink.core.convenience.Links;
import org.ardulink.rest.main.CommandLineArguments;
import org.ardulink.rest.main.RestMain;
import org.junit.Before;
import org.junit.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public class ArdulinkRestSwaggerTest {

	private static final String MOCK_URI = "ardulink://mock";

	@Before
	public void setup() {
		port = freePort();
	}

	@Test
	public void canAccesApiDoc() throws Exception {
		try (RestMain main = runRestComponent()) {
			given().port(port).get("/api-docs").then().assertThat().statusCode(200).contentType(JSON) //
					.body("info.title", equalTo("User API")) //
					.body("paths", hasKey("/pin/analog/{pin}")) //
					.body("paths", hasKey("/pin/digital/{pin}")) //
			;
		}
	}

	@Test
	public void canAccesApiUi_GotoApiDocs() throws Exception {
		try (RestMain main = runRestComponent()) {
			try (Playwright playwright = Playwright.create()) {
				Browser browser = playwright.chromium()
						.launch(new BrowserType.LaunchOptions().setHeadless(isHeadless() || forcedHeadless()));
				BrowserContext context = browser.newContext(ctx());

				Page page = context.newPage();

				page.navigate("http://localhost:" + port + "/api-browser");

				Page page1 = page.waitForPopup(() -> {
					page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("/api-docs")).click();
				});
			}
		}
	}

	@Test
	public void canAccesApiUi_ExecPutRequestViaApiBrowser() throws Exception {
		int pin = 13;
		int value = 42;
		try (RestMain main = runRestComponent()) {
			try (Playwright playwright = Playwright.create()) {
				Browser browser = playwright.chromium()
						.launch(new BrowserType.LaunchOptions().setHeadless(isHeadless()));
				BrowserContext context = browser.newContext(ctx());

				Page page = context.newPage();

				page.navigate("http://localhost:" + port + "/api-browser");
				page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("put ​/pin​/analog​/{pin}"))
						.click();

				page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Try it out")).click();
				page.getByPlaceholder("pin").click();
				page.getByPlaceholder("pin").fill(String.valueOf(pin));
				page.locator("textarea:has-text(\"string\")").click();
				page.locator("textarea:has-text(\"string\")").press("Control+a");
				page.locator("textarea:has-text(\"string\")").fill(String.valueOf(value));
				page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Execute")).click();

				try (Link mock = getMock(Links.getLink(MOCK_URI))) {
					verify(mock, timeout(5_000)).switchAnalogPin(analogPin(pin), value);
				}
			}
		}
	}

	private static NewContextOptions ctx() {
		Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();
		return forcedNoVideo() ? newContextOptions
				: newContextOptions.setRecordVideoDir(Paths.get("videos/")).setRecordVideoSize(1024, 800);
	}

	private static boolean forcedHeadless() {
		return Boolean.parseBoolean(System.getProperty("test.playwright.force.headless"));
	}

	private static boolean forcedNoVideo() {
		return Boolean.parseBoolean(System.getProperty("test.playwright.force.novideo"));
	}
	
	private RestMain runRestComponent() throws Exception {
		CommandLineArguments args = new CommandLineArguments();
		args.connection = MOCK_URI;
		args.port = port;
		return new RestMain(args);
	}

}

