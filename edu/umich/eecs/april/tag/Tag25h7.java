/*
Copyright (c) 2013 by the Regents of the University of Michigan.
Developed by the APRIL robotics lab under the direction of Edwin Olson (ebolson@umich.edu).

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation
are those of the authors and should not be interpreted as representing
official policies, either expressed or implied, of the FreeBSD
Project.

This implementation of the AprilTags detector is provided for
convenience as a demonstration.  It is an older version implemented in
Java that has been supplanted by a much better performing C version.
If your application demands better performance, you will need to
replace this implementation with the newer C version and using JNI or
JNA to interface the C version to Java.

For details about the C version, see
https://april.eecs.umich.edu/wiki/index.php/AprilTags-C

 */

package edu.umich.eecs.april.tag;

/** Tag family with 242 distinct codes.
    bits: 25,  minimum hamming: 7,  minimum complexity: 8

    Max bits corrected       False positive rate
            0                    0.000721 %
            1                    0.018752 %
            2                    0.235116 %
            3                    1.893914 %

    Generation time: 72.585000 s

    Hamming distance between pairs of codes (accounting for rotation):

       0  0
       1  0
       2  0
       3  0
       4  0
       5  0
       6  0
       7  2076
       8  4161
       9  5299
      10  6342
      11  5526
      12  3503
      13  1622
      14  499
      15  114
      16  16
      17  3
      18  0
      19  0
      20  0
      21  0
      22  0
      23  0
      24  0
      25  0
 **/
public class Tag25h7 extends TagFamily {
    public Tag25h7() {
        super(25, 7, new long[] { 0x4b770dL, 0x11693e6L, 0x1a599abL, 0xc3a535L,
                0x152aafaL, 0xaccd98L, 0x1cad922L, 0x2c2fadL, 0xbb3572L,
                0x14a3b37L, 0x186524bL, 0xc99d4cL, 0x23bfeaL, 0x141cb74L,
                0x1d0d139L, 0x1670aebL, 0x851675L, 0x150334eL, 0x6e3ed8L,
                0xfd449dL, 0xaa55ecL, 0x1c86176L, 0x15e9b28L, 0x7ca6b2L,
                0x147c38bL, 0x1d6c950L, 0x8b0e8cL, 0x11a1451L, 0x1562b65L,
                0x13f53c8L, 0xd58d7aL, 0x829ec9L, 0xfaccf1L, 0x136e405L,
                0x7a2f06L, 0x10934cbL, 0x16a8b56L, 0x1a6a26aL, 0xf85545L,
                0x195c2e4L, 0x24c8a9L, 0x12bfc96L, 0x16813aaL, 0x1a42abeL,
                0x1573424L, 0x1044573L, 0xb156c2L, 0x5e6811L, 0x1659bfeL,
                0x1d55a63L, 0x5bf065L, 0xe28667L, 0x1e9ba54L, 0x17d7c5aL,
                0x1f5aa82L, 0x1a2bbd1L, 0x1ae9f9L, 0x1259e51L, 0x134062bL,
                0xe1177aL, 0xed07a8L, 0x162be24L, 0x59128bL, 0x1663e8fL,
                0x1a83cbL, 0x45bb59L, 0x189065aL, 0x4bb370L, 0x16fb711L,
                0x122c077L, 0xeca17aL, 0xdbc1f4L, 0x88d343L, 0x58ac5dL,
                0xba02e8L, 0x1a1d9dL, 0x1c72eecL, 0x924bc5L, 0xdccab3L,
                0x886d15L, 0x178c965L, 0x5bc69aL, 0x1716261L, 0x174e2ccL,
                0x1ed10f4L, 0x156aa8L, 0x3e2a8aL, 0x2752edL, 0x153c651L,
                0x1741670L, 0x765b05L, 0x119c0bbL, 0x172a783L, 0x4faca1L,
                0xf31257L, 0x12441fcL, 0x0d3748L, 0xc21f15L, 0xac5037L,
                0x180e592L, 0x7d3210L, 0xa27187L, 0x2beeafL, 0x26ff57L,
                0x690e82L, 0x77765cL, 0x1a9e1d7L, 0x140be1aL, 0x1aa1e3aL,
                0x1944f5cL, 0x19b5032L, 0x169897L, 0x1068eb9L, 0xf30dbcL,
                0x106a151L, 0x1d53e95L, 0x1348ceeL, 0xcf4fcaL, 0x1728bb5L,
                0xdc1eecL, 0x69e8dbL, 0x16e1523L, 0x105fa25L, 0x18abb0cL,
                0xc4275dL, 0x6d8e76L, 0xe8d6dbL, 0xe16fd7L, 0x1ac2682L,
                0x77435bL, 0xa359ddL, 0x3a9c4eL, 0x123919aL, 0x1e25817L,
                0x02a836L, 0x1545a4L, 0x1209c8dL, 0xbb5f69L, 0x1dc1f02L,
                0x5d5f7eL, 0x12d0581L, 0x13786c2L, 0xe15409L, 0x1aa3599L,
                0x139aad8L, 0xb09d2aL, 0x54488fL, 0x13c351cL, 0x976079L,
                0xb25b12L, 0x1addb34L, 0x1cb23aeL, 0x1175738L, 0x1303bb8L,
                0xd47716L, 0x188ceeaL, 0xbaf967L, 0x1226d39L, 0x135e99bL,
                0x34adc5L, 0x2e384dL, 0x90d3faL, 0x232713L, 0x17d49b1L,
                0xaa84d6L, 0xc2ddf8L, 0x1665646L, 0x4f345fL, 0x2276b1L,
                0x1255dd7L, 0x16f4cccL, 0x4aaffcL, 0xc46da6L, 0x85c7b3L,
                0x1311fcbL, 0x9c6c4fL, 0x187d947L, 0x8578e4L, 0xe2bf0bL,
                0xa01b4cL, 0xa1493bL, 0x7ad766L, 0xccfe82L, 0x1981b5bL,
                0x1cacc85L, 0x562cdbL, 0x15b0e78L, 0x8f66c5L, 0x3332bfL,
                0x12ce754L, 0x096a76L, 0x1d5e3baL, 0x27ea41L, 0x14412dfL,
                0x67b9b4L, 0xdaa51aL, 0x1dcb17L, 0x4d4afdL, 0x6335d5L,
                0xee2334L, 0x17d4e55L, 0x1b8b0f0L, 0x14999e3L, 0x1513dfaL,
                0x765cf2L, 0x56af90L, 0x12e16acL, 0x1d3d86cL, 0xff279bL,
                0x18822ddL, 0x99d478L, 0x8dc0d2L, 0x34b666L, 0xcf9526L,
                0x186443dL, 0x7a8e29L, 0x19c6aa5L, 0x1f2a27dL, 0x12b2136L,
                0xd0cd0dL, 0x12cb320L, 0x17ddb0bL, 0x05353bL, 0x15b2cafL,
                0x1e5a507L, 0x120f1e5L, 0x114605aL, 0x14efe4cL, 0x568134L,
                0x11b9f92L, 0x174d2a7L, 0x692b1dL, 0x39e4feL, 0xaaff3dL,
                0x96224cL, 0x13c9f77L, 0x110ee8fL, 0xf17beaL, 0x99fb5dL,
                0x337141L, 0x02b54dL, 0x1233a70L });
    }
}
