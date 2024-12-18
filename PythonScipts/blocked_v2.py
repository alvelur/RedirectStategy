import csv
from http.client import responses

import requests

input_csv = "redirects.csv"
output_csv = "results.csv"
timeout = 5

with open(input_csv, "r") as csv_file, open(output_csv, "w", newline="") as results_file:
    reader = csv.DictReader(csv_file)
    writer = csv.writer(results_file)

    writer.writerow(["original_link", "expected_link", "actual_link", "status", "http_Status_code", "error"])

    for row in reader:
        original_link = row["original_link"]
        expected_link = row["expected_link"]

        try:
            response = requests.get(original_link,timeout=timeout, allow_redirects=True)
            actual_link = response.url
            http_status_code = response.status_code

            if actual_link == expected_link:
                status = "OK"
            else:
                status = "Not OK"

            writer.writerow([original_link,expected_link,actual_link,status,http_status_code, ""])
        except requests.RequestException as e:
            writer.writerow([original_link, expected_link,"","Error","", str(e)])
            print(f"Error procesando{original_link}:{e}")

print(f"Archivo de resultados: {output_csv}")