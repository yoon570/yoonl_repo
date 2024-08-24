import requests
import pandas as pd
import openpyxl
import datetime
import regex as re

""" 
The following script checks if the provided client number
is indeed valid through the use of a REST API provided by
the ACM. Currently, This script proceeds assuming that SIG 
codes are irrelevant for ACM Membership.

Authored by yoon570
"""

"""
prof_mbr    (ACTIVE ACM Professional Member)
stu_mbr     (ACTIVE ACM Student Member)
sig_stu_mbr	(ACTIVE Student Member of the provided SIG)
sig_mbr     (ACTIVE Member of the provided SIG)
non_mbr     (not an active member of anything)
"""

# Debug statement
# print(api_url)

""" test_no = 9999999
product_url = api_url + str(test_no)

response = requests.get(product_url)
mbr_json = response.json()

print(mbr_json) """


def run_scrape(sheetname, filepath, log_file, skim_type, ieee):
    # Baseline API url
    api_url = "API URL REMOVED FOR PRIVACY REASONS"
    ieee_count = 0
    acm_count = 0
    other_count = 0

    # Read in the xlsx file
    file_name = filepath
    if len(file_name) == 0:
        print("Using default path")
        file_name = "orders_newest.xlsx"
    
    # Opening student sheet
    data_sheet = pd.read_excel(file_name, sheet_name=sheetname, engine='openpyxl', dtype={'What is your ACM/IEEE member number?': str})

    # Debug statement
    #print(data_sheet)

    # List of first and last name columns
    # List of acm_numbers
    acm_number = data_sheet.iloc[:, 11]
    """ 
    Below are the formats for accessing dates and emails to tie to numbers
    
    acm_date = data_sheet.iloc[1:, 0]
    acm_email = data_sheet.iloc[1:, 3] 
    """

    # This attaches the SIG code to the URL request. Can modify as needed
    sig_code = "001"
    api_url += sig_code + "/"

    # This is a list of all the acceptable codes for valid ACM members
    accept_codes = ["prof_mbr", "stu_mbr", "sig_stu_mbr", "sig_mbr"]
    
    count = 0
    log_file.write(skim_type)
    ieee.write(skim_type)
    log_list = []
    last_no = ""
    
    for number in acm_number:
        # This appends the client number to the URL request
        client_no = number
        if client_no == last_no:
            count += 1
            continue
        last_no = client_no
            
        patternACM = r'\b\d{7}\b'
        patternIEEE = r'\b\d{8,9}\b'
        
        if client_no in log_list:
            print("")
            count += 1
            continue
        
        matchesACM = re.findall(patternACM, client_no)
        matchesIEEE = re.findall(patternIEEE, client_no)
        
        if matchesACM:
            product_url = api_url + matchesACM[0]
        elif matchesIEEE:
            if matchesIEEE in log_list:
                continue
            print("Logging " + client_no + "...")
            ieee.write("Registration date: " + data_sheet.iloc[count, 0] + "\n")
            ieee.write(data_sheet.iloc[count, 2] + ", " + data_sheet.iloc[count, 1])
            ieee.write(": " + data_sheet.iloc[count, 3] + "\n")
            ieee.write(client_no + "\n=====\n")
            count += 1
            ieee_count += 1
            continue
        else:
            product_url = api_url + client_no
        
        response = requests.get(product_url)
        try:
            mbr_json = response.json()
        except:
            print(client_no + " " + "needs manual verification")
            log_file.write(client_no + " " + "needs manual verification\n=====\n")
            count += 1
            other_count += 1
            continue
        
        logging_string = ""
        print("Testing " + client_no + "...", end="")

        json_client = mbr_json["CLIENTNO"]
        
        if len(str(json_client)) > 1:
            if json_client in log_list:
                print("")
                count += 1
                continue
            client_class = mbr_json["CLASS"]
            if client_class not in accept_codes:
                logging_string += "Registration date: " + data_sheet.iloc[count, 0] + "\n"
                logging_string += data_sheet.iloc[count, 2] + ", " + data_sheet.iloc[count, 1] 
                logging_string += ": " + data_sheet.iloc[count, 3] + "\n"
                logging_string += client_no + "\n"
                logging_string += str(mbr_json) + "\n=====\n"
                log_list.append(json_client)
                log_file.write(logging_string)
        elif json_client == "":
            logging_string += "Registration date: " + data_sheet.iloc[count, 0] + "\n"
            logging_string += data_sheet.iloc[count, 2] + ", " + data_sheet.iloc[count, 1] 
            logging_string += ": " + data_sheet.iloc[count, 3] + "\n"
            logging_string += client_no + "\nINVALID NUMBER\n=====\n"
            print(" " + client_no + " " + "needs manual verification", end="")
            log_list.append(json_client)
            log_file.write(logging_string)
        else:
            logging_string += "Registration date: " + data_sheet.iloc[count, 0] + "\n"
            logging_string += data_sheet.iloc[count, 2] + ", " + data_sheet.iloc[count, 1] 
            logging_string += ": " + data_sheet.iloc[count, 3] + "\n"
            logging_string += client_no + "\nINVALID NUMBER\n=====\n"
            print(" " + client_no + " " + "needs manual verification", end="")
            log_list.append(json_client)
            log_file.write(logging_string)
            
        print("")
        count += 1
    
    print(str(count) + " number of entries scanned.")
    print(str(ieee_count) + " IEEE #s.")
    
    
def main():
    logname = "log_" + str(datetime.date.today()) + ".txt"
    log = open(logname, "w")
    ieee = open(("ieee_" + logname), "w")

    # 11 and 12 for old sheet, 56 and 57 for orders listing
    print("Running student members...")
    run_scrape("question form-Final Question F2", input("Give filepath, hit enter for default: "), log, "**Student**\n=====\n", ieee)
    log.write("\n")
    print("Running staff members...")
    run_scrape("question form-Final Question F3", input("Give filepath, hit enter for default: "), log, "**Staff**\n=====\n", ieee)
    
    log.close()
    ieee.close()
    
    print("Done.")
    
if __name__ == "__main__":
    main()