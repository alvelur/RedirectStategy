import pandas as pd
import requests
import os

# Read the CSV file as input
file_path = input("Please enter the path to the CSV file: ")
df = pd.read_csv(file_path)

# Verify that the first two columns contain HTTPS URLs
if not df.iloc[:, 0].str.startswith('https').all() or not df.iloc[:, 1].str.startswith('https').all():
    raise ValueError("The first two columns must contain URLs with the HTTPS protocol")

# Create lists to store request and response headers, and validate the Location header
request_headers_list = []
response_headers_list = []
location_match_list = []

# Create a new column for the results based on the first column (query URL)
def fetch_headers_and_check(url, expected_url):
    try:
        response = requests.get(url, allow_redirects=False)
        request_headers_list.append(response.request.headers)
        response_headers_list.append(response.headers)
        # Validate the status code
        result = "Pass" if 200 <= response.status_code <= 399 else "False"
        # Validate the Location header
        location_header = response.headers.get('Location', None)
        if location_header == expected_url:
            location_match_list.append("Match")
        else:
            location_match_list.append(f"Mismatch: {location_header}")
        return f"{result} ({response.status_code})"
    except requests.RequestException as e:
        request_headers_list.append(None)
        response_headers_list.append(None)
        location_match_list.append(f"Error: {e}")
        return f"Error ({e})"

# Apply the function to the columns (index 0 and 1)
df['Result'] = [
    fetch_headers_and_check(url, expected_url)
    for url, expected_url in zip(df.iloc[:, 0], df.iloc[:, 1])
]

# Add headers and validation results to the DataFrame
df['Request Headers'] = request_headers_list
df['Response Headers'] = response_headers_list
df['Location Match'] = location_match_list

# Display the results
print(df)

# Save the results to a CSV file in the same directory as the original file
output_file = os.path.join(os.path.dirname(file_path), 'results.csv')
df.to_csv(output_file, index=False)
print(f"Results saved to {output_file}")