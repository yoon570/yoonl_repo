from csc_bse_9bit import *
from tqdm import tqdm
import argparse
import glob
import tqdm

html_content_top = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>memvis_revised_9bit</title>
                    <style>
                        body {
                            font-family: 'Courier New', Courier, monospace; /* Use a monospaced font */
                            line-height: 1.8;
                            white-space: pre-wrap; /* Preserve formatting */
                            background-color: #fcf0d9;
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
                <body>"""


RESERVE1 = "background-color:rgb(87, 59, 30); color:rgb(255, 255, 255)"    # Dark Brown
RESERVE2 = "background-color:rgb(79, 79, 79); color:rgb(255, 255, 255)"    # Dark Grey
RESERVE3 = "background-color:rgb(127, 0, 127); color:rgb(255, 255, 255)"    # Dark Magenta

html_content_bot = """
                </body>
                </html>
                """

bse_palette = [
    "background-color:rgb(255, 0, 0); color:rgb(0, 0, 0)",     # Red
    "background-color:rgb(255, 127, 0); color:rgb(0, 0, 0)",   # Orange
    "background-color:rgb(255, 255, 0); color:rgb(0, 0, 0)",   # Yellow
    "background-color:rgb(127, 255, 0); color:rgb(0, 0, 0)",   # Chartreuse
    
    "background-color:rgb(0, 255, 255); color:rgb(0, 0, 0)",   # Cyan
    "background-color:rgb(0, 127, 255); color:rgb(0, 0, 0)",   # Azure
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
    "background-color:rgb(63, 0, 127); color:rgb(255, 255, 255)"    # Dark Violet
]

csc_palette = [
    "background-color:rgb(255, 0, 0); color:rgb(0, 0, 0)",     # Red
    "background-color:rgb(255, 127, 0); color:rgb(0, 0, 0)",   # Orange
    "background-color:rgb(255, 255, 0); color:rgb(0, 0, 0)",   # Yellow
    "background-color:rgb(0, 255, 255); color:rgb(0, 0, 0)",   # Cyan
]

pattern_names = [
    "ALT0Byte",
    "DB08Byte",
    "DB04Byte",
    "RLE0Byte"
]

BLOCK1 = 512
BLOCK2 = 1024

block_bse = 2
page_num = 10
start_page = 0

# Needs SubMetabyte dictionary from below, expands only nested BSE for now...
def expand_dict(smdict):
    expanded_dict = defaultdict(list)
    
    for key in smdict:
        for inx, value in enumerate(smdict[key]):
            if isinstance(value, LitByte):
                smdict[key][inx] = f"{value.num_encoding:02x}"
        
    for key, seq in smdict.items():
        expanded_sequence = []
        for item in seq:
            if item in expanded_dict:
                # extend first instance of num in the dictionary
                expanded_sequence.extend(expanded_dict[item][-1])
            else:
                expanded_sequence.append(item)
        expanded_dict[key].append(expanded_sequence)
        
    return expanded_dict
    

def bse_runner():
    print(f"Running benchmarks {benchmarks} with blocks: {block_bse}, sp: {start_page}, pr: {page_num}, BSE priority")
    submeta = [mb for mb in SubMetaByteGenerator()]      
    
    for benchmark in benchmarks:
        progress_bar = tqdm.tqdm(total=page_num, desc=f"{benchmark}",ncols=100)
        with open(f"benchmark_vis/9bit_{benchmark}_{start_page}_bse.html", "w") as htmlf:
            htmlf.write(html_content_top)
            htmlf.write(f"\nNumber of blocks: {block_bse}\n")
            
            pages = read_file_in_chunks(f"well_rounded_tests/{benchmark}_rand1k")
            
            for page_id, page in enumerate(pages[start_page:]):
                
                if page_id >= page_num:
                    break
                
                htmlf.write(f"[Page {page_id}:]\n")
                
                # Open a new HTML file if we hit 100 pages.
                if (page_id) % 100 == 0 and page_id > 0:
                    htmlf.write(html_content_bot)
                    htmlf.close()
                    htmlf = open(f"benchmark_vis/9bit_{benchmark}_{page_id + start_page}_bse.html", "a")
                    htmlf.write(html_content_top)
                
                # Compressing input
                compressed, dictionary, post_csc, post_bse = compress_all_stages(page, block_bse, 256)
                # Decompressing input to get metadata
                decompressed = decompress_all_stages(compressed, dictionary)
                
                for z, metabyte in enumerate(decompressed):
                    assert page[z] == metabyte.num_encoding
                
                """ print("\n\n\n\n")
                for nn, thing in enumerate(decompressed):
                    if nn % 64 == 0:
                        print("\n\n")
                    print(thing.get_metadata(), end = ",") """
                
                            
                compressed_nochunk = "".join(compressed)                
                
                pagestr = ""
                # Extract sequences and populate the dictionary for HTML
                sequences, _, seq_lengths = extract_sequences(dictionary)
                smdict = {}
                sequences = deque(sequences)
                
                for idx, sl in enumerate(seq_lengths):
                    if sl > 0:
                        smdict[submeta[idx]] = sequences.popleft()
                
                # Calculate the savings here
                csc_savings = [0] * 4                
                bse_global_savings = bse_savings_get()
                
                
                # Appearing in compressed input
                csc_appear = [0] * 4
                bse_appear = [0] * 63
                literal_count = 0
                
                for chunk in post_bse:
                    for metab in chunk:
                        if isinstance(metab, BSESub):
                            bse_appear[metab.sub_index] += 1
                        elif isinstance(metab, RLE0Byte):
                            csc_appear[0] += 1
                        elif isinstance(metab, DB04Byte):
                            csc_appear[1] += 1
                        elif isinstance(metab, DB08Byte):
                            csc_appear[2] += 1
                        elif isinstance(metab, ALT0Byte):
                            csc_appear[3] += 1
                        else:
                            literal_count += 1
                        
                block_limit = 6
                dictstr = ""
                limit_n1 = 0
                limit_n2 = 0
                
                post_bse = unchunk_output(post_bse)
                post_csc = unchunk_output(post_csc)
                
                dict_lengths = [0] * 63
                
                for limit_idx, (k, v) in enumerate(smdict.items()):
                    dict_lengths[limit_idx] = len(v)
                    block_limit += 2 + (len(v) * 9)
                    if block_limit > BLOCK2:
                        limit_n2 = limit_idx
                    elif block_limit > BLOCK1:
                        limit_n1 = limit_idx
                
                bse_total = 0
                bse_actual_savings = [0] * 63
                
                for i in range(63):
                    bse_actual_savings[i] = bse_global_savings[i] * (dict_lengths[i] - 1)                                   

                for n, item in enumerate(decompressed):
                    if n % 64 == 0:
                        pagestr += "\n"
                        
                    csc_meta = item.get_metadata()[0]
                    bse_meta_presort = item.get_metadata()[1]
                    bse_meta = sorted(bse_meta_presort, reverse=True)
                    
                    if (len(bse_meta) > 0):
                        bse_total += 1                    
                    
                    if (len(csc_meta) >= 1):
                        for c_sav in csc_meta:
                            csc_savings[c_sav] += 1
                        pagestr += f"<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">{item.num_encoding:02x} </span>"
                    elif (len(bse_meta) >= 1):                       
                        if limit_n2 > 0 and bse_meta[0] >= limit_n2:
                            pagestr += f"<span style=\"{RESERVE3}\">{item.num_encoding:02x} </span>"
                        elif limit_n1 > 0 and bse_meta[0] >= limit_n1:
                            pagestr += f"<span style=\"{RESERVE2}\">{item.num_encoding:02x} </span>"
                        elif bse_meta[0] >= len(bse_palette):
                            pagestr += f"<span style=\"{RESERVE1}\">{item.num_encoding:02x} </span>"
                        else:
                            pagestr += f"<span style=\"{bse_palette[bse_meta[0]]}\">{item.num_encoding:02x} </span>"
                        
                    else:
                        pagestr += f"{item.num_encoding:02x} "
                
                expanded_dict = expand_dict(smdict)
                
                block_savings = [0] * 3
                    
                for limit_idx, (k, v) in enumerate(smdict.items()):
                    if limit_n2 > 0 and limit_idx >= limit_n2:
                        dictstr += f"<span style=\"{RESERVE3}\">[{k.sub_index}]: {smdict[k]}, {bse_global_savings[k.sub_index]}, {bse_appear[k.sub_index]}, {bse_actual_savings[k.sub_index]}]</span> "
                        block_savings[2] += bse_actual_savings[k.sub_index]
                    elif limit_n1 > 0 and limit_idx >= limit_n1:
                        dictstr += f"<span style=\"{RESERVE2}\">[{k.sub_index}]: {smdict[k]}, {bse_global_savings[k.sub_index]}, {bse_appear[k.sub_index]}, {bse_actual_savings[k.sub_index]}</span> "
                        block_savings[1] += bse_actual_savings[k.sub_index]
                    elif limit_idx >= len(bse_palette):
                        dictstr += f"<span style=\"{RESERVE1}\">[{k.sub_index}]: {smdict[k]}, {bse_global_savings[k.sub_index]}, {bse_appear[k.sub_index]}, {bse_actual_savings[k.sub_index]}</span> "
                        block_savings[0] += bse_actual_savings[k.sub_index]
                    elif limit_idx == 0:
                        dictstr += f"<span style=\"{bse_palette[k.sub_index]}\">[{k.sub_index}]: {[item for item in smdict[k]]}, applied {bse_global_savings[k.sub_index]}, in compressed {bse_appear[k.sub_index]}, savings: {bse_actual_savings[k.sub_index]}</span> "
                        block_savings[0] += bse_actual_savings[k.sub_index]
                    else:
                        dictstr += f"<span style=\"{bse_palette[k.sub_index]}\">[{k.sub_index}]: {[item for item in smdict[k]]}, {bse_global_savings[k.sub_index]}, {bse_appear[k.sub_index]}, {bse_actual_savings[k.sub_index]}</span> "
                        block_savings[0] += bse_actual_savings[k.sub_index]
                    # For now, just append
                    # Figure out how to add CSC shading here
                dictstr += "\n\n"
                
                for limit_idx, (k, v) in enumerate(expanded_dict.items()):
                    for entry in v:
                        if limit_n2 > 0 and limit_idx >= limit_n2:
                            dictstr += f"<span style=\"{RESERVE3}\">[{k.sub_index}]: </span>"
                        elif limit_n1 > 0 and limit_idx >= limit_n1:
                            dictstr += f"<span style=\"{RESERVE2}\">[{k.sub_index}]: </span>"
                        elif limit_idx >= len(bse_palette):
                            dictstr += f"<span style=\"{RESERVE1}\">[{k.sub_index}]: </span>"
                        else:
                            dictstr += f"<span style=\"{bse_palette[k.sub_index]}\">[{k.sub_index}]: </span>"
                        for subentry in entry:
                            if isinstance(subentry, RLE0Byte):
                                dictstr += "<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">"
                                for _ in range(subentry.repeats + 1):
                                    dictstr += "00 "
                                dictstr += "</span>"
                            elif isinstance(subentry, DB04Byte) or isinstance(subentry, DB08Byte) or isinstance(subentry, ALT0Byte):
                                dictstr += f"<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">{subentry} </span>"
                            else:
                                if limit_n2 > 0 and limit_idx >= limit_n2:
                                    dictstr += f"<span style=\"{RESERVE3}\">{subentry} </span>"
                                elif limit_n1 > 0 and limit_idx >= limit_n1:
                                    dictstr += f"<span style=\"{RESERVE2}\">{subentry} </span>"
                                elif limit_idx >= len(bse_palette):
                                    dictstr += f"<span style=\"{RESERVE1}\">{subentry} </span>"
                                else:
                                    dictstr += f"<span style=\"{bse_palette[k.sub_index]}\">{subentry} </span>"
                                
                        dictstr += "  "
                dictstr += "\n"
                
                csc_algo_app = csc_savings_get()

                ##### Writing logic is here! #####
                htmlf.write(f"Bytes in raw, saved, codes CSC: {sum(csc_savings)}, {len(page) - len(post_csc)}, {sum(csc_appear)}; bytes in raw, saved, codes by BSE: {bse_total}, {len(post_csc) - len(post_bse)}, {sum(bse_appear)}")
                htmlf.write(f", total bytes in raw & saved: {len(page) - literal_count} & {len(page) - len(post_bse)}, uncompressed: {literal_count}, final size: {len(compressed_nochunk) / 8} Bytes, ")
                htmlf.write(f"\ncompression ratio: {round((4096 * 8) / (len(dictionary) + len(compressed_nochunk)), 3)}.\n\n")
                htmlf.write("CSC statistics:\n")
                htmlf.write(f"""<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">[Run Length 0]: in raw {csc_savings[0]}, in compressed {csc_appear[0]}, saves {csc_savings[0] - csc_algo_app[0]}, applied {csc_algo_app[0]} times</span> """)
                htmlf.write(f"""<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">[Two 0 every 4]: in raw {csc_savings[1]}, in compressed {csc_appear[1]}, saves {csc_savings[1] - csc_algo_app[1]}, applied {csc_algo_app[1]} times</span>\n""")
                htmlf.write(f"""<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">[Two 0 every 8]: in raw {csc_savings[2]}, in compressed {csc_appear[2]}, saves {csc_savings[2] - csc_algo_app[2]}, applied {csc_algo_app[2]} times</span> """)
                htmlf.write(f"""<span style=\"background-color:rgb(0,0,0); color:rgb(255,255,255);\">[Alternating 0]: in raw {csc_savings[3]}, in compressed {csc_appear[3]}, saves {csc_savings[3] - csc_algo_app[3]}, applied {csc_algo_app[3]} times</span>\n""")
                htmlf.write("\nBSE Dictionary:\n")
                htmlf.write(f"Block 1 savings: {block_savings[0]}, block 2: {block_savings[1]}, block 3: {block_savings[2]}\n")
                htmlf.write(dictstr)
                htmlf.write(pagestr)
                htmlf.write("\n\n\n\n")

                print()
                print(sum(bse_actual_savings))
                print(sum(csc_savings) - sum(csc_algo_app))
                print(literal_count)

                progress_bar.update(1)      
            
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
        
    if not args.cscpriority:
        bse_runner()
    else:
        #csc_runner()
        print("this is where csc goes")
    
    print("Running complete. Terminating process.")
    
        
    