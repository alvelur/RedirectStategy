import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RedirectTestFromCSV {
    protected WebDriver driver;

    @Test
    public void testRedirect() {

        String baseUrl = "https://www.montrealgazette.com";
        String csvFilePath = "src/test/resources/input/redirects.csv";
        String outputCsvPath = "src/test/resources/output/redirect_results1.csv";

        int urlCounter = 0;

        try {

            BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputCsvPath));
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                    .withHeader("Test #", "Redirect", "URL", "ActualRedirect", "Status"));

            Reader reader = new FileReader(csvFilePath);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim());

            for (CSVRecord record : csvParser) {
                driver = DriverFactory.initializeDriver("chrome");
                System.out.println("----------------------------Esta es la URL #" + ++urlCounter + "--------------------------------");

                String longUrl = baseUrl + record.get("original_link");
                String expectedRedirect = baseUrl + record.get("expected_link");

                boolean success = false;
                int attempt = 0;
                String actualRedirect = "";

                while (attempt < 3 && !success) {
                    try {
                        attempt++;
                        System.out.println("Intento #" + attempt + " - Probando redirección desde: " + longUrl + " hacia: " + expectedRedirect);

                        driver.get(longUrl);
                        actualRedirect = driver.getCurrentUrl();

                        if (expectedRedirect.equals(actualRedirect)) {
                            System.out.println("✅ Redirección correcta: " + actualRedirect);
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

                if (success) {
                    csvPrinter.printRecord(urlCounter, longUrl, expectedRedirect, actualRedirect, "Success");
                } else {
                    System.out.println("❗Redirección fallida después de 3 intentos: " + longUrl);
                    csvPrinter.printRecord(urlCounter, longUrl, expectedRedirect, actualRedirect, "Failure");
                }

                DriverFactory.quitDriver(driver);
                csvPrinter.flush();
            }
            csvParser.close();
            csvPrinter.close();
            System.out.println("✅ Pruebas completadas. Resultados almacenados en: " + outputCsvPath);


        } catch (Exception e) {
            System.out.println("⚠️ Error al procesar el archivo CSV: " + e.getMessage());
        }
    }
}
