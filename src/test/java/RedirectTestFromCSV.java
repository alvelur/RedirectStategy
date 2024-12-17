import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.io.FileReader;
import java.io.Reader;

public class RedirectTestFromCSV {
    protected WebDriver driver;

    @Test
    public void testRedirect() {

        String baseUrl = "https://www.montrealgazette.com";
        String csvFilePath = "src/test/resources/redirects80.csv";
        int urlCounter = 0;

        try {
            Reader reader = new FileReader(csvFilePath);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim());

            for (CSVRecord record : csvParser) {
                driver = DriverFactory.initializeDriver("chrome");
                System.out.println("----------------------------Esta es la URL #" + ++urlCounter + "--------------------------------");

                String longUrl = baseUrl + record.get("LongUrl");
                String expectedRedirect = baseUrl + record.get("Redirect");

                boolean success = false;
                int attempt = 0;

                while (attempt < 3 && !success) {
                    try {
                        attempt++;
                        System.out.println("Intento #" + attempt + " - Probando redirección desde: " + longUrl + " hacia: " + expectedRedirect);

                        driver.get(longUrl);
                        String currentUrl = driver.getCurrentUrl();

                        if (expectedRedirect.equals(currentUrl)) {
                            System.out.println("✅ Redirección correcta: " + currentUrl);
                            success = true;
                            driver.close();
                        } else {
                            System.out.println(" 500 refrescando...");
                            driver.navigate().refresh();
                            Thread.sleep(2000);
                        }
                    } catch (UnreachableBrowserException e) {
                        System.out.println("⚠️ Navegador inalcanzable, reintentando...");
                    } catch (Exception e) {
                        System.out.println("⚠️ Excepción al probar redirección: " + e.getMessage());
                    }
                }

                if (!success) {
                    System.out.println("❗Redirección fallida después de 3 intentos: " + longUrl);
                    Assertions.assertEquals(expectedRedirect, driver.getCurrentUrl(), "❌ Redirección incorrecta: ");
                }

                DriverFactory.quitDriver(driver);
            }

            csvParser.close();
        } catch (Exception e) {
            System.out.println("⚠️ Error al procesar el archivo CSV: " + e.getMessage());
        }
    }
}
