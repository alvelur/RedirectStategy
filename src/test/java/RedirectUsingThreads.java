import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class RedirectUsingThreads {

    @Test
    public void testRedirect() {
        String[] csvFilePaths = {
                "src/test/resources/input/redirects.csv",
                "src/test/resources/input/redirects20.csv",
                "src/test/resources/input/redirects80.csv"
        };
        String[] resultCsvFilePaths = {
                "src/test/resources/output/redirect_results1.csv",
                "src/test/resources/output/redirect_results2.csv",
                "src/test/resources/output/redirect_results3.csv"
        };
        int maxThreads = Math.min(csvFilePaths.length, Runtime.getRuntime().availableProcessors());
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
        try {
            for (int i = 0; i < csvFilePaths.length; i++) {
                String inputCsv = csvFilePaths[i];
                String outputCsv = resultCsvFilePaths[i];
                executorService.submit(() -> processCsvFile(inputCsv, outputCsv));
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            System.out.println("✅ Todos los archivos CSV han sido procesados.");
        } catch (Exception e) {
            System.out.println("⚠️ Error al iniciar los hilos: " + e.getMessage());
        }
    }
    private void processCsvFile(String csvFilePath, String resultCsvFilePath) {
        Set<String> processedLinks = new HashSet<>();
        File resultFile = new File(resultCsvFilePath);
        if (resultFile.exists()) {
            try (Reader reader = new FileReader(resultCsvFilePath);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for (CSVRecord record : csvParser) {
                    String originalLink = "https://www.montrealgazette.com" + record.get("original_link");
                    processedLinks.add(originalLink);
                }
                System.out.println("✅ Cargados " + processedLinks.size() + " registros ya procesados desde: " + resultCsvFilePath);
            } catch (Exception e) {
                System.out.println("⚠️ Error al cargar registros procesados: " + e.getMessage());
            }
        }
        try (
                Reader reader = new FileReader(csvFilePath);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
                FileWriter writer = new FileWriter(resultCsvFilePath, true);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)
        ) {
            System.out.println("⏳ Procesando archivo: " + csvFilePath);
            for (CSVRecord record : csvParser) {
                String originalLink = "https://www.montrealgazette.com" + record.get("original_link");
                if (processedLinks.contains(originalLink)) {
                    System.out.println("⏩ Registro ya procesado, omitiendo: " + originalLink);
                    continue;
                }
                WebDriver driver = null;
                String expectedLink = "https://www.montrealgazette.com" + record.get("expected_link");
                String actualLink = "";
                String status = "Failed";
                try {
                    driver = DriverFactory.initializeDriver("chrome");
                    for (int attempt = 1; attempt <= 3; attempt++) {
                        try {
                            System.out.println("Intento #" + attempt + " - URL: " + originalLink);
                            driver.get(originalLink);
                            actualLink = driver.getCurrentUrl();
                            if (expectedLink.equals(actualLink)) {
                                status = "Success";
                                System.out.println("✅ Redirección correcta: " + actualLink);
                                break;
                            } else {
                                System.out.println("⚠️ Redirección incorrecta, refrescando...");
                                driver.navigate().refresh();
                                Thread.sleep(2000);
                            }
                        } catch (UnreachableBrowserException e) {
                            System.out.println("⚠️ Navegador inalcanzable, reintentando...");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error procesando URL: " + originalLink + " - " + e.getMessage());
                } finally {
                    if (driver != null) {
                        try {
                            DriverFactory.quitDriver(driver);
                        } catch (Exception e) {
                            System.out.println("⚠️ Error cerrando el navegador: " + e.getMessage());
                        }
                    }
                }
                csvPrinter.printRecord(originalLink, expectedLink, actualLink, status);
                csvPrinter.flush();
                System.out.println("Registro escrito: " + originalLink);
                processedLinks.add(originalLink);
            }
            System.out.println("✅ Resultados guardados en: " + resultCsvFilePath);
        } catch (Exception e) {
            System.out.println("⚠️ Error procesando archivo: " + csvFilePath + " - " + e.getMessage());
        }
    }
}