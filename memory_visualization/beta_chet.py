
import re
import string
import sys
import subprocess
import time
import binascii
from collections import defaultdict

SEQ_LEN_BIT = 3

# Returns all the printable objects
def extract_dict(dictionaries):
    
    # Uncomment to log dictionary content to file
    # dict_log = open("output/dictionarylog.txt", "w")
    # lognum = 1
    output = []
    
    
    
    for dictionary in dictionaries:
        d = dictionary
        d = d[10:]
        pattern = r"0x([0-9A-F]+)"
        d = eval(re.sub(pattern, replace_hex, d))
        
        replacements = []
        for _, (seq, replacement) in d:
            replacements.append((list(bytes.fromhex(seq)), list(bytes.fromhex(replacement))[0]))


        # now want something mapping right values to left ones (expanded)
        expanded_dict = defaultdict(list)
        ordering = defaultdict(list)
        
        maxi = -1
        
        for idx, (seq, byte) in enumerate(replacements):
            expanded_sequence = []
            for num in seq:
                if num in expanded_dict:
                    # extend first instance of num in the dictionary
                    expanded_sequence.extend(expanded_dict[num][-1])
                else:
                    expanded_sequence.append(num)
            expanded_dict[byte].append(expanded_sequence)
            ordering[byte].append(idx)
            
            if idx > maxi:
                maxi = idx
          
        # Uncomment to log dictionary content to file  
        # dict_log.write(f"Page {lognum}\n")
        # dict_log.write(str(expanded_dict))
        # dict_log.write("\n----\n")
        # dict_log.write(str(ordering))
        # dict_log.write("\n----\n")
    
        # output.append(expanded_dict)
        actual_output = [(-1, [-1])] * (maxi + 1)
        # Here we need to iterate through the expanded dictionary, and assign each element
        # to its static position in the final output list
        
        for key in expanded_dict:            
            indices = ordering[key]
            sequences = expanded_dict[key]
            for idx, _ in enumerate(indices):
                actual_output[indices[idx]] = (key, sequences[idx])
                
        
        output.append(actual_output)
        
        # Uncomment to log dictionary content to file 
        # dict_log.write(str(actual_output))
        # dict_log.write("\n----\n")
        # lognum += 1
        
    # Uncomment to log dictionary content to file 
    # dict_log.close()
        
    return output

def bytekey(dictionary):
    new_dict = {}
    count = 1
    for _, seq in dictionary:
        new_dict[count] = seq
        count += 1
    return new_dict


def replace_hex(match):
   hex_value = match.group(1)
   return f'"{hex_value}"'


def palette():
    swatches = ["rgb(186, 70, 78)",      # Red
    "rgb(231, 105, 65)",    # Orange
    "rgb(187, 255, 0)",    # Yellow-Orange
    "rgb(255, 110, 0)",    # Yellow
    "rgb(150, 157, 62)",    # Lime
    
    "rgb(124, 217, 62)",    # Green
    "rgb(62, 217, 129)",    # Teal
    "rgb(62, 217, 194)",    # Turquoise
    "rgb(62, 175, 217)",    # Blue
    "rgb(91, 86, 223)",    # Cyan-Blue
    
    "rgb(148, 0, 190)",     # Blue
    "rgb(178, 140, 236)",  # Violet
    "rgb(249, 73, 156)",  # Light Violet
    "rgb(202, 97, 251)",    # Magenta
    "rgb(105, 112, 123)",  # Gainsboro
    
    "rgb(192, 192, 192)",  # Silver
    "rgb(161, 184, 155)",  # Dimgray
    "rgb(119, 136, 153)",  # Light Slategray
    
    "rgb(63, 89, 131)",  # Blue --> Block 1 extend, index (18)
    "rgb(66, 133, 105)",   # Blue-gray --> Block 2 (19)
    "rgb(147, 129, 110)"]     # Brown --> Block 3? (20)
    
    return swatches

# Note, should add a way to store addresses
def main():
    print("\n[Compression HEatmap Tool]")
    print("""
        |            |   
   __|  __ \    _ \  __| 
  (     | | |   __/  |   
 \___| _| |_| \___| \__|
""")
    time.sleep(1)
    try:
        if sys.argv[1] != "-n":
            raise Exception
        page_count = int(sys.argv[2])
        if page_count > 1000:
            raise Exception
        dictfile = sys.argv[3]
        rawfile = sys.argv[4]
    except:
        print(sys.argv)
        exit("ERROR: Invalid usage.\nUsage: python byte_reader [-n] (number of pages <= 1000) dictfile.bin dumpfile.bin")
        
    print("Creating output dictionary...")
    
    subprocess.run(["mkdir", "output"])
    time.sleep(1)
    
    filename = sys.argv[3]
    print(f"\n>> {filename} <<\n")
    htmlfile = "output/" + filename[:-4] + "_0.html"
    
    counter = 0
    content = ""
    
    block2 = "rgb(168, 153, 114)"
    block3 = "rgb(66, 133, 105)"
    
    with open(dictfile, "rb") as dict_f, open(rawfile, "rb") as raw_f:
        print("Extracting dictionaries...")
        rawdict = dict_f.read()
        filtered_string = "".join(map(chr, filter(lambda x : x in map(ord, string.printable), list(rawdict))))
        dictionaries = list(map(str, re.findall(r'@@@@@@@@@@\[[^\]]*\]', filtered_string)))
        
        # THIS LOGIC IS ONLY FOR ONE PAGE
        # Read in first page
        # Read in expanded dictionary and create color map

        raw_dictionaries = extract_dict(dictionaries)
        htmlfile = "output/" + filename[:-4] + f"_001.html"
        html_f = open(htmlfile, "w")
                
        while counter < page_count:
            
            if (counter % 100) == 0 and counter > 0:
                html_content = f"""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Memory Visualizer</title>
                    <style>
                        body {{
                            font-family: 'Courier New', Courier, monospace; /* Use a monospaced font */
                            line-height: 1.6;
                            white-space: pre-wrap; /* Preserve formatting */
                            background-color: #f9f9f9;
                            margin: 20px;
                            padding: 20px;
                            word-wrap: break-word;
                            overflow-wrap: break-word;
                            word-break: break-all;
                            border: 1px solid #ccc;
                            border-radius: 10px;
                        }}
                    </style>
                </head>
                <body>
{content}
                </body>
                </html>
                """
                # Close all files
                html_f.write(html_content)
                if html_f:
                    html_f.close()
                content = ""
                htmlfile = "output/" + filename[:-4] + f"_{counter + 1}.html"
                html_f = open(htmlfile, "w")
            
            print(f"Extracting {counter + 1}")
            colors = [0] * 4096
            # Skip pagesize literals and read in a page
            raw_f.seek(64, 1)
            page = raw_f.read(4096)
            page = page.hex()
            # This is an array of decimal integers
            page = list(binascii.unhexlify(page))            
            byte_k = bytekey(raw_dictionaries[counter])
            
            appearance = [0] * len(byte_k)
            
            # Assign color numbers corresponding to each byte
            for i in range(len(byte_k), 0, -1):
                sequence = byte_k[i]
                j = 0
                
                while j < len(page):
                    
                    if colors[j] != 0:
                        j += 1
                        continue
                    
                    if page[j:j + len(sequence)] == sequence:
                        for k in range(j, (j + len(sequence))):
                            colors[k] = i
                        j += len(sequence)
                        appearance[i-1] += 1
                        continue
                    j += 1
                            
            pal = palette()
            # Skip end padding
            raw_f.seek(64, 1)
            
            rgbkey = dictionaries[counter]
            rgbkey = re.findall(r'(\([^\)]+\)\))', rgbkey)
            
            legend = ""
            openkey = ""
                    
            haz1 = 999
            haz2 = 999
            
            # Formula is as follows: SEQ_LEN_BIT + (8 * TERM) + (8 * DEFINITION)
            # This needs to be compared to 64 * 8 for the first block, then 64 * 8 * 2 
            # for the second block. At least in theory. Need to give indices for the 
            # "hazard" locations on that portion.
            
            length = 0           


            for i in range(0, len(rgbkey)):
                
                tokens = list(map(str.strip, rgbkey[i].replace("0x","").replace("(","").strip(")").split(",")))
                
                length += SEQ_LEN_BIT + (len(tokens[1]) * 4) + (len(tokens[2]) * 4)

                if length > (2 * 64 * 8) and haz2 == 999:
                    haz2 = i
                elif length > (64 * 8) and haz1 == 999:
                    haz1 = i
                    
                if haz2 != 999 and i > haz2:
                    legend += f"<span style=\"background-color:{block3};\">{rgbkey[i]}</span> "
                elif haz1 != 999 and i > haz1:
                    legend += f"<span style=\"background-color:{block2};\">{rgbkey[i]}</span> "
                else:
                    legend += f"<span style=\"background-color:{pal[i]};\">{rgbkey[i]}</span> "
                    
            
            for i in range(1, len(byte_k) + 1):
                # Maybe add spacing here? Hard to read
                hex_string = "".join(format(byte, "02x") for byte in byte_k[i])    
                outstr = " ".join(map(lambda i: hex_string[i:i+4], range(0, len(hex_string), 4)))
                byte_to_hex = f"[{appearance[i-1]}]:" + outstr            
                
                if haz2 != 999 and i > haz2 + 1:
                    openkey += f"<span style=\"background-color:{block3};\">{byte_to_hex}</span> "
                elif haz1 != 999 and i > haz1 + 1:
                    openkey += f"<span style=\"background-color:{block2};\">{byte_to_hex}</span> "
                else:
                    openkey += f"<span style=\"background-color:{pal[i - 1]};\">{byte_to_hex}</span> "
                    
            content += f"<strong>Page {counter + 1}:</strong>\n" + "Compressed file:\n" + legend + "\nRaw file:\n" + openkey + "\n\n"
            # content += f"DEBUG:\nBYTE_K:\n{byte_k}\nRGBKEY:\n{rgbkey}\nRAWDICT:{raw_dictionaries[counter]}\n\n"
            
            inside = False
            # Iterate through page and assign html color tags
            
            for i in range(len(page)):
                                
                if (i % 64 == 0):
                    content += "\n" 
                elif (i % 2 == 0):
                    if colors[i - 1] != colors[i]:
                        content += "</span>"
                    content += " "
                    
                if colors[i] == 0:
                    if inside == True:
                        inside = False
                        content += "</span>"   
                        content += format(page[i], '02x')
                    else:
                        content += format(page[i], '02x')
                else:
                    if inside == False:
                        if (colors[i]) > haz2 + 1:
                            content += f"<span style=\"background-color:{block3};\">{format(page[i], '02x')}"
                            inside = True
                        elif (colors[i]) > haz1 + 1:
                            content += f"<span style=\"background-color:{block2};\">{format(page[i], '02x')}"
                            inside = True
                        else:
                            content += f"<span style=\"background-color:{pal[colors[i] - 1]};\">{format(page[i], '02x')}"
                            inside = True
                    elif colors[i - 1] == colors[i]:
                        content += format(page[i], '02x')
                    else:
                        if (colors[i]) > haz2 + 1:
                            content += f"</span><span style=\"background-color:{block3};\">{format(page[i], '02x')}"
                        elif (colors[i]) > haz1 + 1:
                            content += f"</span><span style=\"background-color:{block2};\">{format(page[i], '02x')}"
                        else:
                            content += f"</span><span style=\"background-color:{pal[colors[i] - 1]};\">{format(page[i], '02x')}"
                        
            
            counter += 1
            content += "</span>"
            content += "\n\n\n"
            
            
        html_content = f"""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Memory Visualizer</title>
            <style>
                body {{
                    font-family: 'Courier New', Courier, monospace; /* Use a monospaced font */
                    line-height: 1.6;
                    white-space: pre-wrap; /* Preserve formatting */
                    background-color: #f9f9f9;
                    margin: 20px;
                    padding: 20px;
                    border: 1px solid #ccc;
                    border-radius: 10px;
                }}
            </style>
        </head>
        <body>
{content}
        </body>
        </html>
        """
        # Close all files
        html_f.write(html_content)
        dict_f.close()
        dict_f.close()
        html_f.close()
        print("*****\nDone.")

if __name__ == "__main__":
    main()