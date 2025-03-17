from collections import defaultdict
from bse_9bit_rle_nost import read_file_in_chunks
from multiprocessing import Pool
from tqdm import tqdm
import os
from collections import OrderedDict
import statistics

def matchskim(chunk, max_dict_bytes, short_size):
    """ Search for potential sequences to replace with codes """
    history = defaultdict(list)
    idx = 0
    longest_matches = defaultdict(int)
    zrl_lengths = defaultdict(int)
    longest_zrl = 0
    max_seq_len = 4096
    if short_size:
        max_seq_len = 5

    # Check if a page is all the same..? What page hitches?

    if len(set(chunk)) == 1:
        return longest_matches

    while idx < 4096:
        current_byte = chunk[idx]
        current_zrl = 0
        matches = set() # stores pairs in form (idx, length)

        while current_byte == 0 and idx < 4095 and current_zrl <= 2048:
            current_zrl += 1
            idx += 1
            current_byte = chunk[idx]

        if current_zrl > 1:
            # length 5 cap
            if current_zrl > max_seq_len:
                continue
            if current_zrl > longest_zrl:
                longest_zrl = current_zrl 
                zrl_lengths[longest_zrl] = 1
                if zrl_lengths:
                    zrl_candidates = [k for k in zrl_lengths.keys() if k < current_zrl]
                    if zrl_candidates:
                        best_zrl_len = max(zrl_candidates)
                        if best_zrl_len > 1:
                            zrl_lengths[best_zrl_len] += 1
            else:
                if zrl_lengths:
                    zrl_candidates = [k for k in zrl_lengths.keys() if k <= current_zrl]
                    if zrl_candidates:
                        best_zrl_len = max(zrl_candidates)
                        if best_zrl_len > 1:
                            zrl_lengths[best_zrl_len] += 1


        # Non zero matching logic... need some way to just add zeroes to the match and skip thru
        for start_idx in history[current_byte]:
            count = 0
            noMatch = False
            # scan ahead for the current byte and see if elements count ahead are equal for each history location
            while idx + count < 4096 and chunk[idx+count]==chunk[start_idx+count]:
                count += 1
                if count > max_seq_len:
                    idx += max_seq_len
                    noMatch = True
                    break
                if count >= max_dict_bytes - 2: # Not true max_dict_bytes size limit, saving space for codes.
                    idx += max_dict_bytes - 2
                    noMatch = True
                    break
            if count >= 2 and not noMatch:
                # saving indices and lengths instead of actual match hashes, tuples are hashable
                match = chunk[idx:idx+count]
                matches.add((bytes(match), start_idx, count)) # store as indices
        
        if matches:
            # take the longest and latest match
            match, match_start, match_length = max(matches, key=lambda x:x[2])
            
            if idx - match_start >= 1 and ((idx - match_length) >= match_length):
                matchkey = match
                if matchkey in longest_matches:
                    idx += match_length
                    longest_matches[matchkey] += 1
                    continue
                else:
                    longest_matches[matchkey] = 1
                    idx += match_length
                    continue
            
        history[current_byte].append(idx)
        idx += 1

    for length, appearances in zrl_lengths.items():
        if appearances > 1:
            longest_matches[bytes([0] * length)] = appearances

    return longest_matches


# FIXME encoding needs to happen last. Repr is impossible, which is why debugging is terrible
def naive_bse_compress(page, max_dict_bytes, args):

    """ Compress a page using a naive searching algorithm similar to LZ """
    compressed = []
    currsize = 0
    
    # Skim page for matches using LZ-like search
    short_size = False

    if args.short:
        short_size = True
    preprocess_dict = matchskim(page, max_dict_bytes, short_size)
    unsorted_dict = defaultdict(int)
    
    # Utility incorrect. Reference notes. Nolan provided general formula.
    # TODO needs to be sorted postfactum.
    for k, v in preprocess_dict.items():
        dict_entry_size = len(k)*9 + 7
        savings = v*(len(k)-1)*9 - dict_entry_size
        utility = savings/dict_entry_size
        if util_arg == 2:
            unsorted_dict[k] = utility
        elif util_arg == 1:
            unsorted_dict[k] = savings
        else:
            unsorted_dict[k] = v

    # NOTE this appears to be fine
    # Sort dictionary based on utility? -u for override

    # Utility: total savings / dictionary entry size
    # Remember to account for changing of bits per character (8/9, 9/8? Decide later.)

    if util_arg > 0:
        sorted_dict = OrderedDict(sorted(unsorted_dict.items(), key=lambda x:x[1], reverse=True))
    else: 
        sorted_dict = unsorted_dict

    # Compression here, while adding.

    final_dict = []
    encoded_dict = []
    entrycount = 0
    compressed_dict = defaultdict(int)
    if max_dict_bytes <= 512:
        for item in sorted_dict.keys():
            bytearr = list(item)
            # Check if adding item to the dictionary is possible, or if full
            item_length = len(bytearr)
            code_length = 0
            if 0 < item_length <= 16:
                code_length = 2 + 4 + (item_length * 9)
            elif 17 <= item_length <= 32:
                code_length = 3 + 4 + (item_length * 9)
            elif 33 <= item_length <= 48:   
                code_length = 3 + 4 + (item_length * 9)
            elif 49 <= item_length <= 64:
                code_length = 3 + 4 + (item_length * 9)
            elif 65 <= item_length <= 128:
                code_length = 3 + 6 + (item_length * 9)
            elif 129 <= item_length <= 255:
                code_length = 3 + 7 + (item_length * 9)
            elif 256 <= item_length <= 510:
                code_length = 3 + 8 + (item_length * 9)
            else:
                continue

            if bytearr not in final_dict and entrycount < 255:
                if (currsize + (code_length)) <= max_dict_bytes * 9:
                        final_dict.append(bytearr)
                        entrycount += 1
                        if args.compressdict:
                            for i, parentseq in enumerate(final_dict):
                                for j, subseq in enumerate(final_dict):
                                    if i != j:
                                        pos = 0
                                        while pos <= len(parentseq) - len(subseq):
                                            if parentseq[pos:pos+len(subseq)] == subseq:
                                                item_index = (j + 256)
                                                parentseq = parentseq[:pos] + [item_index] + parentseq[pos + len(subseq):]
                                                pos += len(subseq)
                                            else:
                                                pos += 1

                                compressed_dict[i] = parentseq
                                currsize -= len(bytearr) - len(parentseq)
                        currsize += ((code_length) + item_length * 9)
                        # compress here
                else:
                    break
                    
    else:
        for item in sorted_dict.keys():
            bytearr = list(item)
            if bytearr not in final_dict and entrycount < 255:
                entrycount += 1
                final_dict.append(bytearr)
                if args.compressdict:
                    for i, parentseq in enumerate(final_dict):
                        for j, subseq in enumerate(final_dict):
                            if i != j:
                                pos = 0
                                while pos <= len(parentseq) - len(subseq):
                                    if parentseq[pos:pos+len(subseq)] == subseq:
                                        item_index = (j + 257)
                                        parentseq = parentseq[:pos] + [item_index] + parentseq[pos + len(subseq):]
                                        pos += len(subseq)
                                    else:
                                        pos += 1

                        compressed_dict[i] = parentseq
        
    i = 0
    # Checks all sequences against sliding window from index. Should be sufficient.
    while i < 4096:
        match_found = False
        candidates = defaultdict(int)
        for idx, seq in enumerate(final_dict):
            if page[i:i+len(seq)] == seq:
                savings = (len(seq) * 8) - 9
                index = 256 + idx
                candidates[index] = savings
                match_found = True

        if match_found:
            best_c = max(candidates)
            compressed.append(bin(best_c)[2:].zfill(9))
            i += len(page[i:i + len(final_dict[best_c - 256])])

        if not match_found:
            compressed.append(bin(page[i])[2:].zfill(9))
            i += 1
    maxitemlen = 0
    for item in final_dict:
        if len(item) > maxitemlen:
            maxitemlen = len(item)
    
    if args.compressdict:
        final_dict = []
        for item in compressed_dict.values():
            final_dict.append(item)
    if max_dict_bytes <= 512:
        encoded_dict.append("1")
        for item in final_dict:
            item_length = len(item)
            if 2 <= item_length <= 17:
                # Code 00 + 4b
                encoded_dict.append("00")
                encoded_dict.append(bin(item_length - 2)[2:].zfill(4))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item]))
            elif 18 <= item_length <= 33:
                # Code 010 + 4b
                encoded_dict.append("010")
                encoded_dict.append(bin(item_length - 18)[2:].zfill(4))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item]))
            elif 34 <= item_length <= 49:
                # Code 011 + 4b
                encoded_dict.append("011")
                encoded_dict.append(bin(item_length - 34)[2:].zfill(4))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item]))     
            elif 50 <= item_length <= 65:
                # Code 100 + 4b
                encoded_dict.append("100")
                encoded_dict.append(bin(item_length - 50)[2:].zfill(4))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item]))  
            elif 65 <= item_length <= 128:
                # Code 101 + 6b
                encoded_dict.append("101")
                encoded_dict.append(bin(item_length - 65)[2:].zfill(6))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item]))  
            elif 129 <= item_length <= 255:
                # Code 110 + 7b
                encoded_dict.append("110")
                encoded_dict.append(bin(item_length - 129)[2:].zfill(7))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item])) 
            elif 256 <= item_length <= 510:
                # Code 111 + 8b
                encoded_dict.append("111")
                encoded_dict.append(bin(item_length - 256)[2:].zfill(8))
                encoded_dict.append("".join([bin(x)[2:].zfill(9) for x in item]))  
            else:
                print("Unreachable, debug dictionary encoding/dictionary construction")
                exit(1)
    else:
        # NOTE this is fine as is, I think. There's some sort of pretty punishing overhead, probably because of shorter sequences.
        # I can't possibly think of a better way to do this. Anything that needs sequence lengths is worse than this.
        encoded_dict.append("0")
        for item in final_dict:
            for char in item:
                encoded_dict.append(bin(char)[2:].zfill(9))
            encoded_dict.append(bin(256)[2:].zfill(9))      

    # NOTE single bit overhead is fine. Again, why did I encode immediately?
    # Metachars, like in BSE and LZ4 is better

    if args.compressdict:
        return "".join(compressed), "1" + "".join(encoded_dict), maxitemlen
    else:
        return "".join(compressed), "0" + "".join(encoded_dict), maxitemlen

def parse_compressed(compressed):
    """ Split compressed into 9-bit characters, then parse as ints. """
    csize = len(compressed)
    return [int(compressed[i:i+9], 2) for i in range(0, csize, 9)]

def recover_dict(dict_bits):
    """ Recover the dictionary from its encoded state. """
    dict_compressed = False
    if dict_bits[0] == "1":
        dict_compressed = True
    dict_bits = dict_bits[1:]
    i = 0
    # TODO Recover compression that happened to dictionary
    # while i < len(dict_bits):
    # Init dictionary decoding
    recovered_dict = []
    if dict_bits[0] == "1":
        i += 1
        # This needs new logic
        dict_bs = len(dict_bits)
        while i < dict_bs:
            seq_len = ""
            code = dict_bits[i:i+2]
            seq = []
            # Check if code is 2b, otherwise, read in 1 more bit for total of 3b
            if code == "00":
                i += 2
                seq_len = int(dict_bits[i:i+4], 2) + 2
                i += 4
            else:
                i += 2
                code += dict_bits[i:i+1]
                i += 1
                # 3b code logic
                if code == "010":
                    seq_len = int(dict_bits[i:i+4], 2) + 18
                    i += 4
                elif code == "011":
                    seq_len = int(dict_bits[i:i+4], 2) + 34
                    i += 4
                elif code == "100":
                    seq_len = int(dict_bits[i:i+4], 2) + 50
                    i += 4
                elif code == "101":
                    seq_len = int(dict_bits[i:i+6], 2) + 65
                    i += 6
                elif code == "110":
                    seq_len = int(dict_bits[i:i+7], 2) + 129
                    i += 7
                elif code == "111":
                    seq_len = int(dict_bits[i:i+8], 2) + 256
                    i += 8
                else:
                    print("Unreachable, debug dict_code logic")
                    exit(1)
            for _ in range(seq_len):
                seq.append(int(dict_bits[i:i+9], 2))
                i += 9
            recovered_dict.append(seq)
    else:
        recovered_seq = []
        i += 1
        dict_bs = len(dict_bits)
        while i < dict_bs:
            currchar = dict_bits[i:i+9]
            if int(currchar, 2) == 256:
                recovered_dict.append(recovered_seq)
                recovered_seq = []
                i += 9
            else:
                recovered_seq.append(int(currchar, 2))
                i += 9

    # Move this internal to recover_dict with signal bit on?
    if dict_compressed:
        decompressed_dict = []
        for parentseq in recovered_dict:
            decompressed_seq = []
            stack_index = 0 
            while stack_index < len(parentseq):
                symbol = parentseq[stack_index]
                if symbol > 255:
                    stack_index += 1
                    if dict_bits[0] == "1":
                        stack = recovered_dict[symbol - 256]
                    else:
                        stack = recovered_dict[symbol - 257]
                    parentseq = stack + parentseq[stack_index:]
                    stack_index = 0
                else:
                    decompressed_seq.append(symbol)
                    stack_index += 1
            decompressed_dict.append(decompressed_seq)
        return decompressed_dict
    else:
        return recovered_dict

def naive_bse_decompress(compressed, dict_bits):
    """ Decompressor for the naive bse encoder. 
    Compressed output and dict_bits are decoupled for ease of debugging. """
    decompressed = []
    # Parsing of compression output
    dictionary = recover_dict(dict_bits)
    compressed_9bit = parse_compressed(compressed)
    csize = len(compressed_9bit)
    idx = 0
    while idx < csize:
        if compressed_9bit[idx] > 255:
            entryno = compressed_9bit[idx] - 256
            decompressed.extend(dictionary[entryno])
            idx += 1
        else:
            decompressed.append(compressed_9bit[idx])
            idx += 1

    return decompressed

def process_page(page, max_dict_bytes, args):
    """ Convert to list """
    page = list(page)
    # Compress
    compressed, dict_bits, maxitemlen = naive_bse_compress(page, max_dict_bytes, args)
    csize = len(compressed) + len(dict_bits)
    # If compressed size grows, set to 4096B
    csize = csize if csize < (4096 * 8) else (4096 * 8)
    # Decompress and verify
    decompressed = naive_bse_decompress(compressed, dict_bits)
    assert(page == decompressed)
    return csize, len(dict_bits), maxitemlen

def runc_serial(pages, benchmark, max_dict_bytes, args):
    results = [
        process_page(page, max_dict_bytes, args) for page in tqdm(pages, desc=f"{benchmark}")
    ]
    return results

def runc_parallel(pages, benchmark, max_dict_bytes, args):
    pool_size = os.cpu_count() - 1

    tasks = [
        (page, max_dict_bytes, args)
        for page in pages
    ]

    with Pool(pool_size) as pool:
        results = pool.starmap(
            process_page, tqdm(tasks, desc=f"{benchmark}") , chunksize=20
        )
        
    return results

from glob import glob
import argparse
# directory hint: should be of this format for globbing
# TEST_DIR = "../mem_files/well_rounded_tests/*"
if __name__ == "__main__":
    max_dict_bytes = 0
    args = None
    util_arg = 0
    parser = argparse.ArgumentParser()

    parser.add_argument("-ds", "--dictsize", help="valid dictionary sizes: 128, 256, 512; no arg=256 entries, no limit on size")
    parser.add_argument("-c", "--compressdict", action="store_true", help="compress dictionary?")
    parser.add_argument("-f", "--filepath", required=True, help="file directory for all tests to compress")
    parser.add_argument("-panic", help=argparse.SUPPRESS) # debugging function, for benchmark testing
    parser.add_argument("-u", "--util", type=int, help="savings=1, utility=2")
    parser.add_argument("-sl", "--serial", action="store_true", help="run compression serially")
    parser.add_argument("-st", "--short", action="store_true", help="limit entry size to 5")
    
    # Parse arguments
    args = parser.parse_args()

    # Check for invalid dictionary sizes
    try:
        max_dict_bytes = int(args.dictsize)
    except:
        print("Using 256 entry limit on dictionary size.")

    if args.dictsize not in ["128", "256", "512"]:
        max_dict_bytes = 4096
    else:
        max_dict_bytes = int(args.dictsize)
        print("Using dictionary size of", max_dict_bytes)

    util_formul = {
        0: "default",
        1: "savings",
        2: "utility"
    }

    fpsource = args.filepath

    filepaths = sorted(glob(fpsource + "/*"))
    with open("report.csv", "a") as outf:
        mean_crs = []
        if args.compressdict:
            utilarr = [2]
        else:
            utilarr = [0, 1, 2]
        for util in utilarr:
            util_arg = util
            outf.write(f"LZBSE:{9}b, {max_dict_bytes}B, util={util_formul[util_arg]}, dc={args.compressdict}\n")
            crs = []
            for file in filepaths:
                pages = read_file_in_chunks(file)
                if args.panic:
                    pages = [pages[int(args.panic)]]
                    results, dictsizes, maxitemlens = zip(*runc_serial(pages, file, max_dict_bytes, args))
                elif args.serial:
                    results, dictsizes, maxitemlens = zip(*runc_serial(pages, file, max_dict_bytes, args))
                else:
                    results, dictsizes, maxitemlens = zip(*runc_parallel(pages, file, max_dict_bytes, args))
                totalcsize = sum(results)
                totaldictsize = sum(dictsizes)
                dictentrymax = sum(maxitemlens) / len(maxitemlens)
                print("csize", totalcsize)
                print("dictsize", totaldictsize)
                print("dict%", totaldictsize / totalcsize * 100)
                print("average longest dictionary entry", dictentrymax)
                print(f"CR {(4096 * 8 * len(pages))/totalcsize}")
                crs.append((4096 * 8 * len(pages))/totalcsize)
                outf.write(f"{file}\n{(4096 * 8 * len(pages))/totalcsize}\n")
            mean_crs.append(statistics.mean(crs))
            outf.write("\n")
        if args.compressdict:
            outf.write(f"UTILC:{mean_crs[0]}")
            print((f"UTILC:{mean_crs[0]}"))
        else:
            outf.write(f"FCFS:{mean_crs[0]},SAVE:{mean_crs[1]},UTIL:{mean_crs[2]}\n")
            print(f"FCFS:{mean_crs[0]},SAVE:{mean_crs[1]},UTIL:{mean_crs[2]}")