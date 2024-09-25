from csc_bse_9bit import compress_all_stages, read_file_in_chunks, decode_value, compress_only_bse, decompress_only_bse
from itertools import batched
import multiprocessing
from multiprocessing import Pool
from glob import glob
from tqdm import tqdm
from statistics import harmonic_mean, geometric_mean
from collections import defaultdict
from abc import ABC, abstractmethod
from dataclasses import dataclass
import argparse
import os

# Design considerations #
""" 
The input needs to be converted to a split of a short into two bytes before processing
How does this affect compression ratio? After completing the algo, should consult Dr. Jian

Will not implement self-referential logic. This needs to be dictionary free
Just figure out a simple search to skim through the remaining bytes
"""

# Global variable for modularity considerations
MIN_MATCH_LEN = 2
TOKEN_MAX = 15

MIN_LIT_VAL = 0
MAX_LIT_VAL = 511
LIT_SIZE = 9

MAX_OVERFLOW_VAL = 255
OVERFLOW_LEN = 8

MIN_OFFSET = 1
MAX_OFFSET = 65535
OFFSET_OVERALL = 8
OFFSET_CHAR_SIZE = 8

CODESIZE = 9

@dataclass
class LZ4Seq(ABC):
    def __repr__(self) -> str:
        return self.repr_str
    def binary_encode(self) -> str:
        return self.encoding
    def get_length(self) -> int:
        return len(self.encoding)
    
class CompressedFlag(LZ4Seq):
    def __init__(self, compressed: bool):
        self._compressed = compressed
    def __repr__(self) -> str:
        return f"[Cflag: ({self._compressed})]"
    @property
    def encoding(self) -> str:
        return str(int(self._compressed))
    
class Token(LZ4Seq):
    def __init__(self, lit_len: int, match_len: int):
        assert lit_len <= TOKEN_MAX
        assert MIN_MATCH_LEN <= match_len <= TOKEN_MAX + MIN_MATCH_LEN
        self._lit_len = lit_len
        self._match_len = match_len
    @property
    def literals(self) -> int:
        """ Return literal length token portion """
        return self._lit_len
    @property
    def matches(self) -> int:
        """ Return match length token portion """
        return self._match_len
    @property
    def lit_over(self) -> bool:
        """ Check if literals need overflow bytes, checked against token maximum val """
        return self._lit_len == TOKEN_MAX
    @property
    def mat_over(self) -> bool:
        """ Check if matches need overflow bytes, checked against minimum length + token maximum val """
        return self._match_len == MIN_MATCH_LEN + TOKEN_MAX
    @property
    def encoding(self) -> str:
        literals_bin = bin(self._lit_len)[2:].zfill(4)
        match_no_to_binary = self._match_len - MIN_MATCH_LEN
        assert match_no_to_binary >= 0
        match_bin = bin(match_no_to_binary)[2:].zfill(4)
        return f"{literals_bin}{match_bin}"
    @property
    def repr_str(self) -> str:
        """ String representation of LZ4 token """
        return f"[Tok.: ({self._lit_len}, {self._match_len})]"
    
class Lit_Overflow(LZ4Seq):
    def __init__(self, literal_overflow: int):
        assert 0 <= literal_overflow <= MAX_OVERFLOW_VAL
        self._literal_overflow = literal_overflow
    @property
    def value(self) -> int:
        """ Return number of literals to count """
        return self._literal_overflow
    @property
    def encoding(self) -> str:
        overflow_binary = bin(self._literal_overflow)[2:].zfill(OVERFLOW_LEN)
        return f"{overflow_binary}"
    @property
    def repr_str(self) -> str:
        return f"[LO: ({self._literal_overflow})]"
    
class Literal(LZ4Seq):
    def __init__(self, value: int):
        assert MIN_LIT_VAL <= value <= MAX_LIT_VAL
        self._value = value
    @property
    def encoding(self) -> str:
        value_binary = bin(self._value)[2:].zfill(LIT_SIZE)
        return f"{value_binary}"
    @property
    def value(self) -> int:
        return self._value
    @property
    def repr_str(self) -> str:
        return f"[Lit.: ({self._value})]"
    
class Offset(LZ4Seq):
    def __init__(self, value: int):
        assert MIN_OFFSET <= value <= MAX_OFFSET
        self._value = value
    @property
    def encoding(self) -> str:
        binary_offset = bin(self._value)[2:].zfill(OFFSET_OVERALL)
        return f"{binary_offset}"
    @property
    def value(self) -> int:
        return self._value
    @property
    def repr_str(self) -> str:
        return f"[Off.: ({self._value})]"
    
class Match_Overflow(LZ4Seq):
    def __init__(self, match_overflow: int):
        assert 0 <= match_overflow <= 255
        self._match_overflow = match_overflow
    @property
    def value(self) -> int:
        """ Return number of literals to count """
        return self._match_overflow
    @property
    def encoding(self) -> str:
        overflow_binary = bin(self._match_overflow)[2:].zfill(OVERFLOW_LEN)
        return f"{overflow_binary}"
    @property
    def repr_str(self) -> str:
        return f"[MO: ({self._match_overflow})]"
        
def return_as_is(chunk) -> list[LZ4Seq]:
    output = []
    output.append(CompressedFlag(False))
    for literal_num in chunk:
        literal = Literal(literal_num)
        output.append(literal)
    return output

def lz_compress(chunk):
    # default dictionary that contains all indices of where a certain byte character appears
    
    # If a file has less than 13 bytes, return it as literals
    """ if len(chunk) < 13:
        return chunk """
    
    hypomatch_2 = 0
    hypomatch_3 = 0
    
    history = defaultdict(list)
    idx = 0
    literals = []
    compressed_chunk : list[LZ4Seq] = []
    while idx < len(chunk):
        current_byte = chunk[idx]
        matches = [] # stores pairs in form (idx, length)
        for start_idx in history[current_byte]:
            count = 0
            # scan ahead for the current byte and see if elements count ahead are equal for each history location
            while idx + count < len(chunk) and chunk[idx+count]==chunk[start_idx+count]:
                count += 1
            matches.append((count, start_idx))
            
        
        # Need a tracker here for hypothetical statistics for 2 and 3 byte sequences being shortened to 1
            # This passes through the match and adds it to history
        if not matches:
            history[chunk[idx]].append(idx)
            literals.append(chunk[idx]) # literals
            idx += 1
            continue
        
        # Match length 2 and 3 hypothetical replacements for measurements
        if max(matches)[0] < MIN_MATCH_LEN:
            history[chunk[idx]].append(idx)
            literals.append(chunk[idx]) # literals
            idx += 1
            continue
    
        # take the longest and latest match
        match_length, match_start = max(matches)
        
        # This portion of the code is only reached if there is a valid match #
        assert match_length >= MIN_MATCH_LEN # catch statement to ensure proper match lengths
        
        match_end = idx
        
        for _ in range(match_length):
            if max(matches)[0] == 3:
                literals.append(chunk[idx])
            elif max(matches)[0] == 2:
                literals.append(chunk[idx])
            history[chunk[idx]].append(idx)
            idx += 1
            
        if max(matches)[0] == 3:
            if match_end - match_start >= 3:
                hypomatch_3 += 1
            continue
        elif max(matches)[0] == 2:
            if match_end - match_start >= 2:
                hypomatch_2 += 1
            continue
        
        # can have 0+ literals
        literal_overflow = []
        match_overflow = []
        
        if len(literals) >= TOKEN_MAX:
            literal_overflow_val = len(literals) - TOKEN_MAX
            # Keep appending MAX_OVERFLOW_VAL overflow bytes if overflow val > MAX_OVERFLOW_VAL, then append remainder
            while literal_overflow_val >= MAX_OVERFLOW_VAL:
                literal_overflow.append(Lit_Overflow(MAX_OVERFLOW_VAL))
                literal_overflow_val = literal_overflow_val - MAX_OVERFLOW_VAL
            literal_overflow.append(Lit_Overflow(literal_overflow_val))
            token_literal_length = TOKEN_MAX
        else:
            token_literal_length = len(literals)
        
        # 4 is a match length of 0, 19+ should be 15 plus extra bytes
        if match_length >= TOKEN_MAX + MIN_MATCH_LEN:
            match_overflow_val = match_length - (TOKEN_MAX + MIN_MATCH_LEN)
            while match_overflow_val >= MAX_OVERFLOW_VAL:
                match_overflow.append(Match_Overflow(MAX_OVERFLOW_VAL))
                match_overflow_val = match_overflow_val - MAX_OVERFLOW_VAL
            match_overflow.append(Match_Overflow(match_overflow_val))
            token_match_length = (TOKEN_MAX + MIN_MATCH_LEN)
        else:
            token_match_length = match_length
                   
        # Here we build the LZ4 representation that can be output into the final page
        token = Token(token_literal_length, token_match_length)               
        # Token first, always present
        compressed_chunk.append(token)
        # Literal overflow next, optional
        if literal_overflow:
            compressed_chunk.extend(literal_overflow)
        # Literals, optional 
        if literals:
            for literal in literals:
                lz4_literal = Literal(literal)
                compressed_chunk.append(lz4_literal)
        # Offset, optional, difference between current index and match_idx
        # Fix offset to be 2 bytes instead, use standard lz4
        if matches:
            offset_val = match_end - match_start
            offset = Offset(offset_val)        
            compressed_chunk.append(offset)    
        if match_overflow:
            compressed_chunk.extend(match_overflow)
            
        literals = []
        
    if literals:
        literal_overflow = []
        # Flushing literals and inserting a dummy token, just ignore the match for this one
        # Need a catch for this in the decoder
        if len(literals) >= TOKEN_MAX:
            literal_overflow_val = len(literals) - TOKEN_MAX
            # Keep appending MAX_OVERFLOW_VAL overflow bytes if overflow val > MAX_OVERFLOW_VAL, then append remainder
            while literal_overflow_val >= MAX_OVERFLOW_VAL:
                literal_overflow.append(Lit_Overflow(MAX_OVERFLOW_VAL))
                literal_overflow_val = literal_overflow_val - MAX_OVERFLOW_VAL
            literal_overflow.append(Lit_Overflow(literal_overflow_val))
            endcap_token = Token(TOKEN_MAX, MIN_MATCH_LEN)
        else:
            endcap_token = Token(len(literals), MIN_MATCH_LEN)
            
        compressed_chunk.append(endcap_token)
        
        if literal_overflow:
            compressed_chunk.extend(literal_overflow)
            
        for literal in literals:
            input_literal = Literal(literal)
            compressed_chunk.append(input_literal)        
        
    if not compressed_chunk:
        return return_as_is(chunk), hypomatch_2, hypomatch_3, 0
    
    length_count = 0
    for lz4_meta in compressed_chunk:
        length_count += lz4_meta.get_length()
    
    if (len(chunk) * LIT_SIZE) <= length_count:
        return return_as_is(chunk), hypomatch_2, hypomatch_3, 0
    
    compressed_chunk.insert(0, CompressedFlag(True))
    return compressed_chunk, hypomatch_2, hypomatch_3, 1

def lz_decompress(chunk: LZ4Seq):
    decompressed = []
    idx = 0
    if chunk[0].encoding == "0":
        for literal_num in chunk[1:]:
            decompressed.append(literal_num.value)
        return decompressed
    else:
        assert chunk[idx].encoding == "1"
        idx += 1
        while idx < len(chunk):
            token : Token = chunk[idx]
            
            literal_count = token.literals
            match_count = token.matches
            
            # Ensure no empty token exists
            assert literal_count != 0 or match_count != 0
            
            idx += 1
            
            if token.lit_over:
                literal_overflow = chunk[idx]
                
                if literal_overflow.value == MAX_OVERFLOW_VAL:
                    while literal_overflow.value == MAX_OVERFLOW_VAL:
                        literal_count += literal_overflow.value
                        idx += 1
                        literal_overflow = chunk[idx]
                
                literal_count += literal_overflow.value
                idx += 1
                
            literals = []
            for _ in range(literal_count):
                literals.append(chunk[idx])
                idx += 1
                
            # If we have reached the end of the file and the sequence hasn't ended yet we could have to empty out literals
            if idx >= len(chunk):
                decompressed.extend(literals)
                break

            offset_byte = chunk[idx]
            assert isinstance(offset_byte, Offset)
            offset = offset_byte.value
            idx += 1
            
            if token.mat_over:
                match_overflow = chunk[idx]
                
                if match_overflow.value == MAX_OVERFLOW_VAL:
                    while match_overflow.value == MAX_OVERFLOW_VAL:
                        match_count += match_overflow.value
                        idx += 1
                        match_overflow = chunk[idx]
                
                match_count += match_overflow.value
                idx += 1
            
            decompressed.extend(literals)
            
            match_arr = []
            
            if offset >= match_count:
                for idy in range(match_count):
                    match_arr.append(decompressed[-offset+idy])
            else:
                tail = decompressed[-offset:]
                for idy in range(match_count):
                    match_arr.append(tail[idy % len(tail)])
                    
            decompressed.extend(match_arr)
            
        return [litnum.value for litnum in decompressed]
    
# Depreciated! #
def process_benchmark_cscbse_print(benchmark):
    pages = read_file_in_chunks(benchmark)

    with open("pageprint.txt", "w") as pageprint:    
    # Use tqdm inside the function to track progress per thread
        for pageno, page in enumerate(tqdm(pages)):
            compressed, _ = compress_only_bse(page, 2, 256)
            post_lz4 = []
            pre_length = 0
            post_length = 0
            hypothetical2 = 0
            hypothetical3 = 0
            for chunk in compressed:
                lz4_input = []
                for metabyte in chunk:
                    lz4_input.append(metabyte.num_encoding)
                    pre_length += LIT_SIZE
                    
                lz4_output, hm2, hm3, _ = lz_compress(lz4_input)
                post_lz4.append(lz4_output)
                hypothetical2 += hm2
                hypothetical3 += hm3
                
                # Decompression validation
                pre_lz4 = lz_decompress(lz4_output)
                assert pre_lz4 == lz4_input
                
            hypothetical2_savings = ((LIT_SIZE * 2) - CODESIZE) * hypothetical2
            hypothetical3_savings = ((LIT_SIZE * 3) - CODESIZE) * hypothetical3
            
            for chunk in post_lz4:
                for lz4meta in chunk:
                    post_length += len(lz4meta.encoding)
                    
            page_compression_ratio = pre_length / post_length
            hypoboth_cr = pre_length / (post_length - hypothetical2_savings - hypothetical3_savings)
            
            pageprint.write(f"{benchmark}: {pageno}\n")
            pageprint.write(f"Base LZ4: {page_compression_ratio}\n")
            pageprint.write(f"Hypothetical Both: {hypoboth_cr}\n\n")

def compress_chunkwise(page):
    page_size = 0
    comp_length = 0
    savings2 = 0
    savings3 = 0
    _, _, csc_bse_out = compress_all_stages(page, 2, 256)
    
    for chunk in csc_bse_out:
        lz4_input = []
        for metabyte in chunk:
            page_size += LIT_SIZE
            lz4_input.append(metabyte.num_encoding)
        lz_compressed, hypo2, hypo3, applied_counter = lz_compress(lz4_input)
        savings2 += ((LIT_SIZE * 2) - CODESIZE) * hypo2
        savings3 += ((LIT_SIZE * 3) - CODESIZE) * hypo3
        savings_total = savings2 + savings3
        for lzmeta in lz_compressed:
            comp_length += len(lzmeta.encoding)
        lz_decompressed = lz_decompress(lz_compressed)
        assert lz4_input == lz_decompressed
            
    return page_size, comp_length, savings_total, applied_counter

def compress_chunkwise_bse_only(page):
    page_size = 0
    comp_length = 0
    savings2 = 0
    savings3 = 0
    bse_out, _ = compress_only_bse(page, 2, 256)
    
    for chunk in bse_out:
        lz4_input = []
        for metabyte in chunk:
            page_size += LIT_SIZE
            lz4_input.append(metabyte.num_encoding)
        lz_compressed, hypo2, hypo3, applied_counter = lz_compress(lz4_input)
        savings2 += ((LIT_SIZE * 2) - CODESIZE) * hypo2
        savings3 += ((LIT_SIZE * 3) - CODESIZE) * hypo3
        savings_total = savings2 + savings3
        for lzmeta in lz_compressed:
            comp_length += len(lzmeta.encoding)
        lz_decompressed = lz_decompress(lz_compressed)
        assert lz4_input == lz_decompressed
        
    return page_size, comp_length, savings_total, applied_counter
            
# Main processing function
def process_benchmark_parallel_pagelevel(pages, benchname, pool_size = None):
    if pool_size is None:
        pool_size = os.cpu_count() - 1
        
    tasks = [
        (page,) for page in pages
    ]
    
    with Pool(pool_size) as pool:
        results = pool.starmap(
            compress_chunkwise_bse_only, tqdm(tasks, desc=benchname), chunksize=20
        )
    
    page_size, compressed_size, hypo_size, applied_count = zip(*results)
    total_raw_size = sum(page_size)
    total_compressed_size = sum(compressed_size)
    total_hypo_size = sum(hypo_size)
    times_applied = f"Applied {sum(applied_count)}/{len(applied_count)}\n"

    base_cr = total_raw_size / total_compressed_size
    hypo_cr = total_raw_size / (total_compressed_size - total_hypo_size)
    
    return [f"Benchmark {benchname}\n", f"Base CR: {base_cr}\n", f"Hypo CR: {hypo_cr}\n", times_applied]

def process_benchmark_serial_pagelevel(pages, benchname, pool_size = None):
    if pool_size is None:
        pool_size = os.cpu_count()
        
    tasks = [
        (page,) for page in pages
    ]
    
    results = [compress_chunkwise_bse_only(*params) for params in tqdm(tasks, desc=benchname)]
    
    page_size, compressed_size, hypo_size, applied_count = zip(*results)
    total_raw_size = sum(page_size)
    total_compressed_size = sum(compressed_size)
    total_hypo_size = sum(hypo_size)
    times_applied = f"Applied {sum(applied_count)}/{len(applied_count)}\n"

    base_cr = total_raw_size / total_compressed_size
    hypo_cr = total_raw_size / (total_compressed_size - total_hypo_size)
    
    return [f"Benchmark {benchname}\n", f"Base CR: {base_cr}\n", f"Hypo CR: {hypo_cr}\n", times_applied]

if __name__ == "__main__":
    directory_path = '../bse_summer_2024/well_rounded_tests/*'
    filepaths = glob(directory_path)
    
    parser = argparse.ArgumentParser(description="multithreading option -t")
    parser.add_argument("-t", "--thread", action="store_true", help="multithread or per-page printing switch, off is per-page")
    args = parser.parse_args()
    
    mthread = args.thread
    
    if mthread:
        with open("results.txt", "w") as outfile:
            for benchmark in sorted(filepaths):
                pages = read_file_in_chunks(benchmark)
                writeline = process_benchmark_parallel_pagelevel(pages, benchmark[38:])
                outfile.writelines(writeline)
    else:
        for benchmark in filepaths:
            process_benchmark_cscbse_print(benchmark)