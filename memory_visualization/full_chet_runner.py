from small_bse import *
from csc_create_escape import csc_encode, csc_decode
from good_csc import report_pattern_savings
from byteobj import Byte, PageInfo
import time
import pickle
import glob
import argparse
import tqdm

# Take raw page, run it through CSC and BSE
# Create ByteStorage off of the compressed page
# Decompress, ignore markers for now, just make sure byte values are consistent

ONE_BLOCK = 64 * 8
TWO_BLOCK = 2 * 64 * 8

html_content_top = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Memory Visualizer</title>
                    <style>
                        body {
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
                            font-size: 85%
                        }
                    </style>
                </head>
                <body>
                """

def expand_dict(dict_arr):
    """ Expand dictionaries, preserve original ordering """
    
    expanded_dict = defaultdict(list)
    ordering = defaultdict(list)
    
    output = []
        
    maxi = -1
        
    for idx, (byte, seq) in enumerate(dict_arr):
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

RESERVE_1 = "rgb(194, 154, 8)"
RESERVE_2 = "rgb(112, 145, 132)"
EXTEND = "rgb(155, 155, 155)"

html_content_bot = """
                </body>
                </html>
                """

base_colors = [
    "rgb(255, 0, 0)",    # Red
    "rgb(255, 0, 128)",  # Pink
    "rgb(255, 128, 0)",  # Orange
    "rgb(255, 255, 0)",  # Yellow
    "rgb(0, 255, 0)",    # Green
    "rgb(0, 166, 83)",  # Teal
    "rgb(0, 128, 255)",  # Sky Blue
    "rgb(0, 179, 179)",  # Mint
    "rgb(0, 255, 255)",  # Cyan
    "rgb(150, 80, 255)",  # Purple
    "rgb(148, 157, 255)",  # Magenta
    "rgb(222, 138, 255)",  # Light Pink
    "rgb(196, 151, 112)",  # Tan
]

csc_palette = [
    "background-color:rgb(255, 0, 0); color:rgb(0, 0, 0)",     # Red
    "background-color:rgb(255, 127, 0); color:rgb(0, 0, 0)",   # Orange
    "background-color:rgb(255, 255, 0); color:rgb(0, 0, 0)",   # Yellow
    "background-color:rgb(127, 255, 0); color:rgb(0, 0, 0)",   # Chartreuse
    "background-color:rgb(0, 255, 127); color:rgb(0, 0, 0)",   # Spring Green
    "background-color:rgb(0, 255, 255); color:rgb(0, 0, 0)",   # Cyan
    "background-color:rgb(0, 127, 255); color:rgb(0, 0, 0)",   # Azure
    "background-color:rgb(0, 50, 255); color:rgb(0, 0, 0)",     # Blue
    "background-color:rgb(127, 0, 255); color:rgb(0, 0, 0)",   # Violet
    "background-color:rgb(196, 151, 112); color:rgb(0, 0, 0)",   # Tan
    "background-color:rgb(155, 155, 155); color:rgb(0, 0, 0)",   # Grey

    "background-color:rgb(127, 0, 0); color:rgb(255, 255, 255)",     # Dark Red
    "background-color:rgb(127, 63, 0); color:rgb(255, 255, 255)",    # Dark Orange
    "background-color:rgb(127, 127, 0); color:rgb(255, 255, 255)",   # Dark Yellow
    "background-color:rgb(63, 127, 0); color:rgb(255, 255, 255)",    # Dark Chartreuse
    "background-color:rgb(0, 127, 0); color:rgb(255, 255, 255)",     # Dark Green
    "background-color:rgb(0, 127, 127); color:rgb(255, 255, 255)",   # Dark Cyan
    "background-color:rgb(0, 0, 127); color:rgb(255, 255, 255)",     # Dark Blue
    "background-color:rgb(63, 0, 127); color:rgb(255, 255, 255)",    # Dark Violet
    "background-color:rgb(127, 0, 127); color:rgb(255, 255, 255)",    # Dark Magenta
    "background-color:rgb(87, 59, 30); color:rgb(255, 255, 255)",    # Dark Brown
    "background-color:rgb(79, 79, 79); color:rgb(255, 255, 255)"    # Dark Grey
]

pattern_names = [
    "0 RLE",
    "num RLE",
    
    "alternating 0 num",
    "alternating 2 num",
    
    "0 every 4",
    "num every 4",
    "two 0s every 4",
    "two nums every 4",
    
    "0 every 8",
    "num every 8",
    "two 0s every 8",
    "two nums every 8",
    "four 0s every 8",
    "four nums every 8",
    
    "0 every 16",
    "num every 16",
    "two 0s every 16",
    "two nums every 16",
    "four 0s every 16",
    "four nums every 16",
    "eight 0s every 16",
    "eight nums every 16"
]

RESERVE_1 = "rgb(173, 155, 88)"
RESERVE_2 = "rgb(97, 120, 111)"
EXTEND = "rgb(155, 155, 155)"

block_bse = 1
page_num = 100
start_page = 0

def csc_runner(heuristic):
    """ Runs the visualizer with CSC priority """
    print(f"Running benchmarks {benchmarks} with blocks: {block_bse}, sp: {start_page}, pr: {page_num}, CSC priority")
    time.sleep(1)

    for benchmark in benchmarks:
        with open(f"benchmark_vis/{benchmark}_{start_page}_csc.html", "w") as htmlf, open(f"benchmark_vis/_errorlog.txt", "w") as errorlog:
            htmlf.write(html_content_top)
            htmlf.write(f"\nNumber of blocks: {block_bse}\n")
            pages = read_file_in_chunks(f"well_rounded_tests/{benchmark}_rand1k")
            
            progress_bar = tqdm.tqdm(total=page_num, desc=f"{benchmark}",ncols=100)
            
            for page_id, page in enumerate(pages[start_page:]):
                
                # Just doing it for one page for now
                if page_id >= page_num:
                    progress_bar.close()
                    break
                
                if (page_id) % 100 == 0 and page_id > 0:
                    htmlf.write(html_content_bot)
                    htmlf.close()
                    htmlf = open(f"benchmark_vis/{benchmark}_{page_id + start_page}_csc.html", "a")
                    htmlf.write(html_content_top)
                
                # Change 2nd param for how many memory blocks
                
                post_csc = csc_encode(page, heuristic)
                post_bse = bse_encode(post_csc, block_bse)
            
                pre_bse_pair = bse_decode(post_bse)
                pre_bse = pre_bse_pair[0]
                pre_bse_bs = pre_bse_pair[1]
                
                error_flag = False
                
                assert(pre_bse == post_csc)
                assert(len(pre_bse_bs) == len(post_csc))
                
                for check_idx1, char in enumerate(pre_bse):
                    try:
                        assert(char == pre_bse_bs[check_idx1].byte_val)
                    except:
                        print(f"WARNING {page_id} ERROR, BSE!")
                        errorlog.write(f"{page_id + start_page}, {benchmark}, BSE ERROR DETECTED\n")
                        error_flag = True
                        break
                
                pre_csc_pair = csc_decode(pre_bse, pre_bse_bs)
                pre_csc = pre_csc_pair[0]
                pre_csc_bs = pre_csc_pair[1]
                
                assert(pre_csc == page)
                assert(len(pre_csc_bs) == len(page))
                
                for check_idx2, char in enumerate(pre_csc):
                    try:
                        assert(char == pre_csc_bs[check_idx2].byte_val)
                    except:
                        print(page_id + start_page)
                        print(benchmark)
                        print(len(pre_csc_bs), len(page), sep = " : ")
                        print(char, pre_csc_bs[check_idx2].byte_val, sep = " : ")  
                        raise Exception("Why is it always bug?")
                                 
                if error_flag == True:
                    htmlf.write(f"Page #{page_id + start_page} ERROR! See logs.\n")
                    continue

                header_list = []         
                
                with open("page_data.pickl", "rb") as pfile:
                    pickle_page: PageInfo = pickle.load(pfile)
                
                for key, seq in pickle_page.dictionary:
                    header_list.append([key, seq, pickle_page.occurrences[key]])
                    
                sorted_header = sorted(header_list, key = lambda x: x[2], reverse = True)
                
                html_content_main = f"Page #{page_id + start_page}\n"

                no_utility_header = []
                
                csc_count = 0
                bse_count = 0
                total_count = 0
                final_size = len(post_bse)
                pre_bse_size = len(post_csc)

                for byte_raw in pre_csc_bs:                          
                    if byte_raw.stage1:
                        csc_count += 1
                        
                    if len(byte_raw.stage2) > 0:
                        bse_count += 1
                        
                    if byte_raw.stage1 or len(byte_raw.stage2) > 0:
                        total_count += 1
                        
                html_content_main += f"Bytes compressed & saved by CSC: {csc_count} & {4096 - pre_bse_size}, bytes compressed & saved by BSE: {bse_count} & {pre_bse_size - final_size}, total compressed & saved: {total_count} & {4096 - final_size}, uncompressed: {4096 - total_count}, final size: {final_size}\n"
                html_content_main += "Dictionary entries:\n"   
                
                # Creating header for the memory compression sequences (pre-expansion)
                for index, entry in enumerate(sorted_header):                    
                    if (index % 2) == 0:
                        html_content_main += "\n"
                    
                    comp_seen = -1
                    
                    key = entry[0]
                    header_seq = entry[1]
                    util = entry[2]
                    
                    for byte_raw in post_bse:
                        if byte_raw == key:
                            comp_seen += 1
                    
                    no_utility_header.append([key, header_seq])
                    
                    html_content_main += f"<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">({key:02x}, [ "
                    for item in header_seq:
                        html_content_main += f"{item:02x} "
                    html_content_main += f"], applied {util}, "
                    html_content_main += f"appears in compressed {comp_seen}, "
                    
                    savings = ((len(header_seq) - 1) * util)
                    
                    html_content_main += f"saved {savings}b)</span> "
                    
                expanded_dictionaries = expand_dict(no_utility_header)[0]
                html_content_main += "\n\n"
                html_content_main += "Expanded dictionary entries:\n"
                
                for term, byte in expanded_dictionaries:
                    html_content_main += f"<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">({term:02x}, [ "
                    for byte_p in byte:
                        html_content_main += f"{byte_p:02x} "
                    html_content_main += f"])</span> "
                    
                html_content_main += "\n\nCSC Savings:\n"
                for sav_id, p_savings in enumerate(report_pattern_savings()):
                    if sav_id == 0:
                        html_content_main += f"<span style=\"background-color:rgb(255, 0, 0);\">[{pattern_names[sav_id]}: {p_savings}]</span> "
                    else:
                        html_content_main += f"<span style=\"{csc_palette[sav_id]};\">[{pattern_names[sav_id]}: {p_savings}]</span> "                                            
                
                # Expanding sequences to their full length in the raw file
                    
                html_content_main += "\n\n" 
                
                for i in range(4096):
                    
                    if i % 64 == 0:
                        html_content_main += "\n"
                    
                    if (pre_csc_bs[i].stage1 == -1):
                        html_content_main += f"<span style=\"background-color:rgb(255, 0, 0);\">{pre_csc_bs[i].byte_val:02x} </span>"
                    elif (pre_csc_bs[i].stage1 != 0):
                        html_content_main += f"<span style=\"{csc_palette[pre_csc_bs[i].stage1 - 1]};\">{pre_csc_bs[i].byte_val:02x} </span>"   
                    elif (pre_csc_bs[i].stage2):
                        html_content_main += f"<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">{pre_csc_bs[i].byte_val:02x} </span>"
                    else:
                        html_content_main += f"{pre_csc_bs[i].byte_val:02x} "
                                            
                    
                htmlf.write("\n" + html_content_main + "\n")
                progress_bar.update(1)
               
            htmlf.write(html_content_bot)
            progress_bar.close()



def bse_runner(heuristic):
    """ Runs the visualizer with BSE priority """
    print(f"Running benchmarks {benchmarks} with blocks: {block_bse}, sp: {start_page}, pr: {page_num}, BSE priority")
    time.sleep(1)

    for benchmark in benchmarks:
        with open(f"benchmark_vis/{benchmark}_{start_page}_bse.html", "w") as htmlf, open(f"benchmark_vis/_errorlog.txt", "w") as errorlog:
            htmlf.write(html_content_top)
            htmlf.write(f"\nNumber of blocks: {block_bse}\n")
            pages = read_file_in_chunks(f"well_rounded_tests/{benchmark}_rand1k")
            
            progress_bar = tqdm.tqdm(total=page_num, desc=f"{benchmark}",ncols=100)
            
            for page_id, page in enumerate(pages[start_page:]):
                
                # Just doing it for one page for now
                if page_id >= page_num:
                    progress_bar.close()
                    break
                
                if (page_id) % 100 == 0 and page_id > 0:
                    htmlf.write(html_content_bot)
                    htmlf.close()
                    htmlf = open(f"benchmark_vis/{benchmark}_{page_id + start_page}_bse.html", "a")
                    htmlf.write(html_content_top)
                
                # Change 2nd param for how many memory blocks
                
                post_csc = csc_encode(page, heuristic)
                post_bse = bse_encode(post_csc, block_bse)
            
                pre_bse_pair = bse_decode(post_bse)
                pre_bse = pre_bse_pair[0]
                pre_bse_bs = pre_bse_pair[1]
                
                error_flag = False
                
                assert(pre_bse == post_csc)
                assert(len(pre_bse_bs) == len(post_csc))
                
                for check_idx1, char in enumerate(pre_bse):
                    try:
                        assert(char == pre_bse_bs[check_idx1].byte_val)
                    except:
                        print(f"WARNING {page_id} ERROR, BSE!")
                        errorlog.write(f"{page_id + start_page}, {benchmark}, BSE ERROR DETECTED\n")
                        error_flag = True
                        break
                
                pre_csc_pair = csc_decode(pre_bse, pre_bse_bs)
                pre_csc = pre_csc_pair[0]
                pre_csc_bs = pre_csc_pair[1]
                
                
                assert(pre_csc == page)
                assert(len(pre_csc_bs) == len(page))
                
                for check_idx2, char in enumerate(pre_csc):
                    try:
                        assert(char == pre_csc_bs[check_idx2].byte_val)
                    except:
                        print(page_id + start_page)
                        print(benchmark)
                        print(len(pre_csc_bs), len(page), sep = " : ")
                        print(char, pre_csc_bs[check_idx2].byte_val, sep = " : ")  
                        raise Exception("Why is it always bug?")
                                 
                if error_flag == True:
                    htmlf.write(f"Page #{page_id + start_page} ERROR! See logs.\n")
                    continue

                header_list = []         
                
                with open("page_data.pickl", "rb") as pfile:
                    pickle_page: PageInfo = pickle.load(pfile)
                
                for key, seq in pickle_page.dictionary:
                    header_list.append([key, seq, pickle_page.occurrences[key]])
                    
                sorted_header = sorted(header_list, key = lambda x: x[2], reverse = True)
                
                palette = {}
                
                # Creating colors for memory blocks
                total = 24
                for idy, key in enumerate(sorted_header):
                    total += (len(key[1]) * 8) + 1
                    if total > TWO_BLOCK:
                        palette[key[0]] = RESERVE_2
                    elif total > ONE_BLOCK:
                        palette[key[0]] = RESERVE_1
                    else:
                        if idy >= len(base_colors):
                            palette[key[0]] = EXTEND
                        else:
                            palette[key[0]] = base_colors[idy]
                
                html_content_main = f"Page #{page_id + start_page}\n"

                no_utility_header = []
                
                csc_count = 0
                bse_count = 0
                total_count = 0
                final_size = len(post_bse)
                pre_bse_size = len(post_csc)

                for byte_raw in pre_csc_bs:                          
                    if byte_raw.stage1:
                        csc_count += 1
                        
                    if len(byte_raw.stage2) > 0:
                        bse_count += 1
                        
                    if byte_raw.stage1 or len(byte_raw.stage2) > 0:
                        total_count += 1
                        
                html_content_main += f"Bytes compressed & saved by CSC: {csc_count} & {4096 - pre_bse_size}, bytes compressed & saved by BSE: {bse_count} & {pre_bse_size - final_size}, total compressed & saved: {total_count} & {4096 - final_size}, uncompressed: {4096 - total_count}, final size: {final_size}\n"
                html_content_main += "Dictionary entries:\n"   
                
                # Creating header for the memory compression sequences (pre-expansion)
                for index, entry in enumerate(sorted_header):                    
                    if (index % 2) == 0:
                        html_content_main += "\n"
                    
                    comp_seen = -1
                    
                    key = entry[0]
                    header_seq = entry[1]
                    util = entry[2]
                    
                    for byte_raw in post_bse:
                        if byte_raw == key:
                            comp_seen += 1
                    
                    no_utility_header.append([key, header_seq])
                    
                    html_content_main += f"<span style=\"background-color:{palette[key]};\">({key:02x}, [ "
                    for item in header_seq:
                        html_content_main += f"{item:02x} "
                    html_content_main += f"], applied {util}, "
                    html_content_main += f"appears in compressed {comp_seen}, "
                    
                    savings = ((len(header_seq) - 1) * util)
                    
                    html_content_main += f"saved {savings}b)</span> "
                    
                expanded_dictionaries = expand_dict(no_utility_header)[0]
                html_content_main += "\n\n"
                html_content_main += "Expanded dictionary entries:\n"
                
                for term, byte in expanded_dictionaries:
                    html_content_main += f"<span style=\"background-color:{palette[term]};\">({term:02x}, [ "
                    for byte_p in byte:
                        html_content_main += f"{byte_p:02x} "
                    html_content_main += f"])</span> "
                    
                html_content_main += "\n\nCSC Savings:\n"
                for sav_id, p_savings in enumerate(report_pattern_savings()):
                    html_content_main += f"[{pattern_names[sav_id]}: {p_savings}] "                                               
                
                # Expanding sequences to their full length in the raw file
                    
                html_content_main += "\n\n" 
                
                for i in range(4096):
                    
                    if i % 64 == 0:
                        html_content_main += "\n"
                    
                    if (pre_csc_bs[i].stage1 != 0):
                        html_content_main += f"<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">{pre_csc_bs[i].byte_val:02x} </span>"    
                    elif (pre_csc_bs[i].stage2):
                        html_content_main += f"<span style=\"background-color:{palette[pre_csc_bs[i].stage2[0]]};\">{pre_csc_bs[i].byte_val:02x} </span>"
                    else:
                        html_content_main += f"{pre_csc_bs[i].byte_val:02x} "
                                            
                    
                htmlf.write("\n" + html_content_main + "\n")
                progress_bar.update(1)
               
            htmlf.write(html_content_bot)
            progress_bar.close()
              
if __name__ == "__main__": 
    
    parser = argparse.ArgumentParser(
        description="Parse and visualize memory dump pages."
    )
    
    parser.add_argument("-b", "--blocks", type=int, required=False, help="Number of memory blocks (default 3)")
    parser.add_argument("-sp", "--startpage", type=int, required=False, help="Page to start at (default 0)")
    parser.add_argument("-pr", "--pagerange", type=int, required=False, help="Number of pages to run (default 100)")
    parser.add_argument("-bm", "--benchmark", nargs="+", type=str, help = "name of benchmark(s) to run")
    parser.add_argument("-csc", "--cscpriority", action="store_true", help = "alternate csc palette prioritization")
    parser.add_argument("-hu", "--heuristic", help="choose heuristic (default (no input) = savings/span**.5, 0 = savings, 1 = savings/span)")
        
    direct = glob.glob('well_rounded_tests\\*')
    direct_names = []
    for addr in direct:
        direct_names.append(addr.strip("well_rounded_tests")[1:-7])    
      
    benchmarks = direct_names
    
    # Flag parsing begins here
    args = parser.parse_args()
    
    if args.blocks:
        block_bse = args.blocks
        
    if args.startpage:
        start_page = args.startpage
        
    if args.pagerange:
        page_num = args.pagerange
    
    if args.benchmark:
        benchmarks = args.benchmark
        
    if args.heuristic == "0":
        heuristic = 0
    elif args.heuristic == "1":
        heuristic = 1
    else:
        heuristic = 2
        
    if not args.cscpriority:
        bse_runner(heuristic)
    else:
        csc_runner(heuristic)
    
    print("Running complete. Terminating process.")
    
        
    