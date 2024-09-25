This is a modular implementation of LZ4 in Python. It takes a few liberties with the format so that it can be applied on a
256 Byte-sized chunk granularity. The format for the compression algorithm is best described by Yann Collet on their blog:
https://fastcompression.blogspot.com/2011/05/lz4-explained.html#. This seeks to improve the algorithm by allowing for
a selective application: that is, if some chunks grow in size after compression, they can be marked to be ignored by LZ4
(with a single bit overhead). This is preferable to the default behavior, where the algorithm is applied universally.
In certain cases within the context of memory compression (compressing values directly from memory dumps), especially when
done at the 256B granularity after another compression algorithm, the application of LZ4 can be detrimental to compression
ratio, increasing the size of the original input, which is not intended.
