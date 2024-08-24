This program utilizes HTML formatting to show the effectiveness of an in-progress memory compression algorithm.
The algorithm was designed an implemented by Nolan Chu of the HEAP lab at Virginia Tech.
The actual functionality of the algorithm is not included in this repository, since I do not have permission to disclose the actual functionality and code.
A short summary of the algorithm's general functionality is included below instead.

*****
STAGE 1: Constant Stride Compression
The algorithm first finds Byte-sized characters that repeat at a constant interval. The algorithm then compresses this "constant stride" into two characters.
The specifics of what data is included in these characters is not permitted for disclosure.

STAGE 2: Byte Sequence Encoding
The algorithm finds repeating sequences of Byte characters and compresses them into a single value. I am unable to discuss specifics of how these characters are chosen.
These sequences were then stored in a "dictionary" that included the replacement characters and the sequences they represented.

STAGE 3: IN PROGRESS
*****

There are three iterations of the program. The first iteration was a beta version intended to test feasibility of such a tool. As such, it had certain limitations,
such as an inability to directly access the compression/decompression algorithms. The second iteration was used to test the effectiveness of an 8-bit aligned
version of the algorithm. The third iteration, more catered towards a novel approach that artificially increased the size of each Byte character from 8 bits
to 9 bits to allow for more per-Byte information storage to allow for smaller, more efficient dictionary usage. The third iteration is currently in progress.
