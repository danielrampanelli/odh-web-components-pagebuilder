package it.bz.opendatahub.webcomponentspagebuilder.controllers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import freemarker.template.TemplateException;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import it.bz.opendatahub.webcomponentspagebuilder.data.entities.PageVersion;
import it.bz.opendatahub.webcomponentspagebuilder.data.repositories.PageVersionRepository;
import it.bz.opendatahub.webcomponentspagebuilder.rendering.PageRenderer;

@RestController
public class PagesController {

	@Autowired
	PageVersionRepository versionsRepo;

	@Autowired
	PageRenderer pageRenderer;

	@RequestMapping("/pages/page-editor/{uuid}.html")
	public ResponseEntity<String> editablePage(HttpServletRequest request, @PathVariable("uuid") String uuid) {
		Optional<PageVersion> page = versionsRepo.findById(UUID.fromString(uuid));

		if (page.isPresent()) {
			try {
				String html = pageRenderer.renderPage(page.get());

				html = html
						.replaceAll("</head>",
								String.format("<style type=\"text/css\">\n%s\n</style>\n</head>",
										IOUtils.toString(
												request.getServletContext()
														.getResourceAsStream("/frontend/styles/PageEditor-Frame.css"),
												Charset.defaultCharset())));

				return ResponseEntity.ok(html);
			} catch (IOException | TemplateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@RequestMapping("/pages/preview/{uuid}.png")
	public ResponseEntity<byte[]> previewPageAsScreenshot(HttpServletRequest request,
			@PathVariable("uuid") String uuid) {
		Optional<PageVersion> page = versionsRepo.findById(UUID.fromString(uuid));

		if (page.isPresent()) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.IMAGE_PNG);

			System.setProperty("webdriver.chrome.args", "--disable-logging");
			System.setProperty("webdriver.chrome.silentOutput", "true");

			ChromeDriverManager.getInstance(ChromeOptions.class).setup();

			ChromeOptions chromeOptions = new ChromeOptions();
			chromeOptions.addArguments("--log-level=OFF");
			chromeOptions.addArguments("--silent");
			chromeOptions.addArguments("--headless");
			chromeOptions.addArguments("--window-size=1280,960");
			chromeOptions.addArguments("--hide-scrollbars");

			ChromeDriver driver = new ChromeDriver(chromeOptions);
			driver.get(String.format("%s://%s:%d/pages/preview/%s", request.getScheme(), request.getServerName(),
					request.getServerPort(), page.get().getHash()));

			byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

			driver.close();

			return new ResponseEntity<>(screenshot, headers, HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@RequestMapping("/pages/preview/{hash}")
	public ResponseEntity<String> previewPage(@PathVariable("hash") String hash) {
		Optional<PageVersion> page = versionsRepo.findByHash(hash);

		if (page.isPresent()) {
			try {
				return ResponseEntity.ok(pageRenderer.renderPage(page.get()));
			} catch (IOException | TemplateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

}
