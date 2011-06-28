/*
 * Copyright 2011 Voxeo Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.phono.audio.codec.gsm;


/**
 * This class implements the basic arithmetic for gsm.
 * Chapter 5.1 of:
 *
 * ETSI EN 300 961 V8.1.1 (2000-11)<br/>
 * European Standard (Telecommunications series)<br/>
 * Digital cellular telecommunications system (Phase 2+);<br/>
 * Full rate speech;<br/>
 * Transcoding<br/>
 * (GSM 06.10 version 8.1.1 Release 1999)<br/>
 * <br/>
 * Integer on 16 bits - (in Java: short)<br/>
 * Long integer on 32 bits - (in Java: int).
 *
 * @see
 * <a href="http://java.sun.com/docs/books/tutorial/java/nutsandbolts/datatypes.html">java.sun.com tutorial</a>
 *
 */

/*
 * Short on 16 bits:
 *  -1 * 2^15 (= -32768) < X < (2^15) - 1 (= +32767)
 *
 * Integer on 32 bits:
 *  -1 * 2^31 (= -2147483648) < X < (2^31) - 1 (= +2147483647)
 */
public class Arithmetic {
    
    /**
     * Performs the addition (s_var1+s_var2) with overflow control and
     * saturation; the result is set at +32767 (=2^15-1) when overflow
     * occurs or at -32768 (=-2^15) when underflow occurs.
     */
    public static short s_add(short s_var1, short s_var2) {
        int i_var1 = s_var1;
        int i_var2 = s_var2;
        int i_result = i_var1 + i_var2;
        short s_result = toShortOverflow(i_result);
        return s_result;
    }
    
    /**
     * 32 bits addition of two 32 bits variables (i_var1 + i_var2) with
     * overflow control and saturation;
     * the result is set at 2147483647 (=2^31-1) when overflow occurs
     * and at -2147483648 (=-2^31) when underflow occurs.
     */
    public static int i_add(int i_var1, int i_var2) {
        long l_var1 = i_var1;
        long l_var2 = i_var2;
        long l_result = l_var1 + l_var2;
        int i_result = toIntOverflow(l_result);
        return i_result;
    }
    
    /**
     * Performs the subtraction (s_var1-s_var2) with overflow control and
     * saturation; the result is set at +32767 (=2^15-1) when overflow
     * occurs or at -32768 (=-2^15) when underflow occurs.
     */
    public static short s_sub(short s_var1, short s_var2) {
        int i_var1 = s_var1;
        int i_var2 = s_var2;
        int i_result = i_var1 - i_var2;
        short s_result = toShortOverflow(i_result);
        return s_result;
    }
    
    /**
     * 32 bits subtraction of two 32 bits variables (i_var1 - i_var2)
     * with overflow control and saturation;
     * the result is set at 2147483647 (=2^31-1) when overflow occurs
     * and at -2147483648 (=-2^31) when underflow occurs.
     */
    public static int i_sub(int i_var1, int i_var2) {
        long l_var1 = i_var1;
        long l_var2 = i_var2;
        long l_result = l_var1 - l_var2;
        int i_result = toIntOverflow(l_result);
        return i_result;
    }
    
    /**
     * Performs the multiplication of s_var1 by s_var2 and gives a 16 bits
     * result which is scaled i.e.<br/>
     * s_mult(s_var1, s_var2) = (s_var1 times s_var2) &gt;&gt; 15<br/>
     * and s_mult(-32768, -32768) = 32767 (=2^15-1)
     * i.e.
     * s_mult(-2^15, -2^15) = 2^15-1
     */
    public static short s_mult(short s_var1, short s_var2) {
        short s_result = 0;
        if (s_var1 == Short.MIN_VALUE && s_var2 == Short.MIN_VALUE) {
            s_result = Short.MAX_VALUE;
        } else {
            // In JAVA:
            // >> right shift with sign extension
            // >>>> right shift with zero extension
            int i_var1 = s_var1;
            int i_var2 = s_var2;
            int i_result = (i_var1 * i_var2) >> 15;
            s_result = toShortOverflow(i_result);
        }
        return s_result;
    }
    
    /**
     * i_mult is a 32 bit result for the multiplication of s_var1 times
     * s_var2 with a one bit shift left.<br/>
     * i_mult(s_var1, s_var2) = (s_var1 times s_var2) &lt;&lt; 1.<br/>
     * The condition i_mult(-32768, -32768) does not occur in the
     * algorithm.
     */
    public static int i_mult(short s_var1, short s_var2) {
        // TODO: throw an exception when (-32768 * -32768)??
        int i_var1 = s_var1;
        int i_var2 = s_var2;
        long l_result = (i_var1 * i_var2) << 1;
        int i_result = (int) l_result;
        return i_result;
    }
    
    /**
     * Same as s_mult but with rounding i.e.<br/>
     * s_mult_r(s_var1, s_var2) = ((s_var1 times s_var2) + 16384) &gt;&gt; 5<br/>
     * and s_mult_r(-32768, -32768) = 32767 (=2^15-1)<br/>
     * (16384 = 2^14)
     * i.e.
     * s_mult_r(-2^15, -2^15) = 2^15-1
     */
    public static short s_mult_r(short s_var1, short s_var2) {
        short s_result = 0;
        if (s_var1 == Short.MIN_VALUE && s_var2 == Short.MIN_VALUE) {
            s_result = Short.MAX_VALUE;
        } else {
            int i_var1 = s_var1;
            int i_var2 = s_var2;
            int i_result = ((i_var1 * i_var2) + 16384) >> 15;
            s_result = toShortOverflow(i_result);
        }
        return s_result;
    }
    
    /**
     * Absolute value of s_var1; s_abs(-32768) = 32767 (=2^15-1)
     */
    public static short s_abs(short s_var1) {
        short s_result = s_var1;
        if (s_var1 < 0) {
            if (s_var1 == Short.MIN_VALUE) {
                s_result = Short.MAX_VALUE;
            } else {
                int i_var1 = s_var1;
                int i_result = -1 * s_var1;
                s_result = toShortOverflow(i_result);
            }
        }
        return s_result;
    }
    
    /**
     * Absolute value of i_var1; i_abs(-2147483648) = 2147483647 (=2^31-1)
     */
    public static int i_abs(int i_var1) {
        int i_result = i_var1;
        if (i_var1 < 0) {
            if (i_var1 == Integer.MIN_VALUE) {
                i_result = Integer.MAX_VALUE;
            } else {
                i_result = -1 * i_var1;
            }
        }
        return i_result;
    }
    
    /**
     * s_div produces a result which is the fractional integer division
     * of s_var1 by s_var2;
     * s_var1 and s_var2 shall be positive and s_var2 shall be greater
     * or equal to s_var1;
     * The result is positive (leading bit equal to 0) and truncated to
     * 16 bits. <br/>
     * If (s_var1 == s_var2) then s_div(s_var1, s_var2) = 32767 (=2^15-1)
     * <br/>
     * See 5.2.5 Computation of the reflection coefficients<br/>
     * NOTE: The following lines gives one correct implementation of the
     * s_div(num, denum) arithmetic operation. Compute s_div which is the
     * integer division of num by denum: with denum &gt;= num &gt; 0.
     */
    public static short s_div(short s_num, short s_denum) {
        int i_num = s_num;
        int i_denum = s_denum;
        short div = 0;
        if (s_num > 0) {
            for (short k=0; k<15; k++) {
                div = (short) (div << 1);
                i_num = i_num << 1;
                if (i_num >= i_denum) {
                    i_num = i_sub(i_num, i_denum);
                    div = s_add(div, (short)1);
                }
            }
        }
        return div;
    }
    
    /**
     * norm produces the number of left shifts needed to normalize the
     * 32 bits variable i_var1 for positive values on the interval with
     * minimum of 1073741824 (=2^30) and maximum of 2147483647 (=2^31-1)
     * and for negative values on the interval with minimum of
     * -2147483648 (=-2^31) and maximum of -1073741824 (=-2^30); in
     * order to normalize the result, the following operation shall be
     * done:<br/>
     * i_norm_var1 = i_var1 &lt;&lt; norm(i_var1)
     */
    public static short norm(int i_var1) {
        // i_norm_var1 = i_var1 << norm(i_var1) ??
        // Count the number of zero (significant) bits, ignoring the
        // signed bit (32 bits variable):
        //
        // max +2147483647 = +(2^31)-1 = 0111 .... 1111
        // min +1073741824 = +(2^30)   = 0100 .... 0000
        //
        // max -1073741824 = -(2^30)   = 1100 .... 0000
        // min -2147483648 = -(2^31)   = 1000 .... 0000
        //
        // Make i_var1 absolute, it's easier to shift with positive
        // numbers.
        //
        // Start with a mask (32 bits):
        // mask = 0100 .... 0000 = 0x40000000
        // AND the mask with i_var1 to check if there is a '1' on that
        // postion. Keep shifting the mask to the right ('>>'), until
        // either ANDing results positive or the mask is shifted that is
        // becomes zero.
        
        short norm = 0;
        int lAbsolute = i_abs(i_var1);
        if (lAbsolute != 0) {
            int mask = 0x40000000;
            int result = (lAbsolute & mask);
            
            // if result == 0:
            //   there is not a '1' in the same location as the '1' in
            //   the mask.
            //
            // if mask > 0:
            //   there is still a '1' to shift about
            
            while (result == 0 && mask > 0) {
                mask = (mask >> 1);
                norm++;
                result = (lAbsolute & mask);
            }
        } else {
            // Is this correct?
            // When (lAbsolute == 0) there is no point in shifting
            norm = 0;
        }
        return norm;
    }
    
    
    /**
     * i_var2 = s_var1:<br/>
     * Deposit the 16 bits of s_var1 in the LSB 16 bits of i_var2 with
     * sign extension.
     */
    public static int toInt(short s_var1) {
        int i_result = s_var1;
        return i_result;
    }
    
    /**
     * s_var1 = i_var1:<br/>
     * Extract the 16 LSB bits of i_var1 to put in s_var1.
     */
    public static short toShort(int i_var1) {
        short s_result = (short) (i_var1 & 0x0000FFFF);
        return s_result;
    }
    
    /**
     * s_var1 = i_var1:<br/>
     * With overflow control and saturation;
     * the result is set at +32767 (=2^15-1) when overflow occurs
     * or at -32768 (=-2^15) when underflow occurs.
     */
    public static short toShortOverflow(int i_var1) {
        short s_result;
        if (i_var1 < Short.MIN_VALUE) {
            s_result = Short.MIN_VALUE;
        } else if (i_var1 > Short.MAX_VALUE) {
            s_result = Short.MAX_VALUE;
        } else {
            s_result = (short) i_var1;
        }
        return s_result;
    }
    
    /**
     * i_var1 = l_var1:<br/>
     * With overflow control and saturation;
     * the result is set at 2147483647 (=2^31-1) when overflow occurs
     * and at -2147483648 (=-2^31) when underflow occurs.
     */
    public static int toIntOverflow(long l_var1) {
        int i_result;
        if (l_var1 < Integer.MIN_VALUE) {
            i_result = Integer.MIN_VALUE;
        } else if (l_var1 > Integer.MAX_VALUE) {
            i_result = Integer.MAX_VALUE;
        } else {
            i_result = (int) l_var1;
        }
        return i_result;
    }
    
    
    public static void main(String[] args) {
        // Test methods
        short s_result;
        int i_result;
        
        // short s_add(short s_var1, short s_var2)
        System.out.println("\nTesting s_add():");
        s_result = s_add(Short.MAX_VALUE, (short)1);
        System.out.println("Short.MAX_VALUE + 1 = " + s_result);
        
        s_result = s_add(Short.MAX_VALUE, Short.MAX_VALUE);
        System.out.println("Short.MAX_VALUE + Short.MAX_VALUE = " + s_result);
        
        s_result = s_add(Short.MIN_VALUE, Short.MAX_VALUE);
        System.out.println("Short.MIN_VALUE + Short.MAX_VALUE = " + s_result);
        
        s_result = s_add(Short.MAX_VALUE, Short.MIN_VALUE);
        System.out.println("Short.MAX_VALUE + Short.MIN_VALUE = " + s_result);
        
        s_result = s_add(Short.MIN_VALUE, (short)-1);
        System.out.println("Short.MIN_VALUE + (-1) = " + s_result);
        
        s_result = s_add((short)1, (short)1);
        System.out.println("1 + 1 = " + s_result);
        
        
        // int i_add(int i_var1, int i_var2)
        System.out.println("\nTesting i_add():");
        i_result = i_add(Integer.MAX_VALUE, (int)1);
        System.out.println("Integer.MAX_VALUE + 1 = " + i_result);
        
        i_result = i_add(Integer.MAX_VALUE, Integer.MAX_VALUE);
        System.out.println("Integer.MAX_VALUE + Integer.MAX_VALUE = " + i_result);
        
        i_result = i_add(Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println("Integer.MIN_VALUE + Integer.MAX_VALUE = " + i_result);
        
        i_result = i_add(Integer.MIN_VALUE, (int)-1);
        System.out.println("Integer.MIN_VALUE + (-1) = " + i_result);
        
        i_result = i_add((int)1, (int)1);
        System.out.println("1 + 1 = " + i_result);
        
        
        
        //short s_sub(short s_var1, short s_var2)
        System.out.println("\nTesting s_sub():");
        s_result = s_sub(Short.MIN_VALUE, (short)1);
        System.out.println("Short.MIN_VALUE - 1 = " + s_result);
        
        s_result = s_sub(Short.MIN_VALUE, Short.MIN_VALUE);
        System.out.println("Short.MIN_VALUE - Short.MIN_VALUE = " + s_result);
        
        s_result = s_sub(Short.MAX_VALUE, Short.MIN_VALUE);
        System.out.println("Short.MAX_VALUE - Short.MIN_VALUE = " + s_result);
        
        s_result = s_sub(Short.MAX_VALUE, Short.MAX_VALUE);
        System.out.println("Short.MAX_VALUE - Short.MAX_VALUE = " + s_result);
        
        s_result = s_sub(Short.MAX_VALUE, (short)1);
        System.out.println("Short.MAX_VALUE - 1 = " + s_result);
        
        s_result = s_sub((short)2, (short)1);
        System.out.println("2 - 1 = " + s_result);
        
        
        //int i_sub(int i_var1, int i_var2)
        System.out.println("\nTesting i_sub():");
        i_result = i_sub(Integer.MIN_VALUE, (int)1);
        System.out.println("Integer.MIN_VALUE - 1 = " + i_result);
        
        i_result = i_sub(Integer.MIN_VALUE, Integer.MIN_VALUE);
        System.out.println("Integer.MIN_VALUE - Integer.MIN_VALUE = " + i_result);
        
        i_result = i_sub(Integer.MAX_VALUE, Integer.MIN_VALUE);
        System.out.println("Integer.MAX_VALUE - Integer.MIN_VALUE = " + i_result);
        
        i_result = i_sub(Integer.MAX_VALUE, (int)1);
        System.out.println("Integer.MAX_VALUE - 1 = " + i_result);
        
        i_result = i_sub((int)2, (int)1);
        System.out.println("2 - 1 = " + i_result);
        
        
        
        // short s_mult(short s_var1, short s_var2)
        System.out.println("\nTesting s_mult():");
        s_result = s_mult(Short.MIN_VALUE, Short.MIN_VALUE);
        System.out.println("Short.MIN_VALUE * Short.MIN_VALUE = " + s_result);
        
        s_result = s_mult((short)3, (short)3);
        System.out.println("3 * 3 = " + s_result);
        
        
        // int i_mult(short i_var1, short i_var2)
        System.out.println("\nTesting i_mult():");
        i_result = i_mult((short)3, (short)3);
        System.out.println("3 * 3 = " + i_result);
        
        
        
        // short s_mult_r(short s_var1, short s_var2)
        System.out.println("\nTesting s_mult_r():");
        s_result = s_mult_r(Short.MIN_VALUE, Short.MIN_VALUE);
        System.out.println("Short.MIN_VALUE * Short.MIN_VALUE = " + s_result);
        
        s_result = s_mult_r((short)3, (short)3);
        System.out.println("3 * 3 = " + s_result);
        
        
        // short s_abs(short s_var1)
        System.out.println("\nTesting s_abs():");
        s_result = s_abs(Short.MIN_VALUE);
        System.out.println("abs(Short.MIN_VALUE) = " + s_result);
        
        
        // int i_abs(int i_var1)
        System.out.println("\nTesting i_abs():");
        i_result = i_abs(Integer.MIN_VALUE);
        System.out.println("abs(Integer.MIN_VALUE) = " + i_result);
        
        
        // short s_div(short s_num, short s_denum)
        System.out.println("\nTesting s_div():");
        s_result = s_div((short)1, (short)1);
        System.out.println("s_div(1, 1) = " + s_result);
        
        s_result = s_div((short)3, (short)5);
        System.out.println("s_div(3, 5) = " + s_result);
        
        
        // short norm(int i_var1)
        System.out.println("\nTesting norm():");
        int vars[] =
        {
            Integer.MIN_VALUE,
            Integer.MIN_VALUE/2,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE/2,
            0,
            1,
            -1,
            7,
            -7
        };
        int i_var1, i_norm_var1;
        // i_norm_var1 = i_var1 << norm(i_var1)
        for (int i=0; i<vars.length; i++) {
            i_var1 = vars[i];
            s_result = norm(i_var1);
            i_norm_var1 = i_var1 << s_result;
            
            if (i_var1 > 0) {
                if ((1073741824 <= i_norm_var1)
                &&
                        (i_norm_var1 <= 2147483647)) {
                    System.out.print("Correct: ");
                } else {
                    System.out.print("Wrong  : ");
                }
            } else {
                if ((-2147483648 <= i_norm_var1)
                &&
                        (i_norm_var1 <= -1073741824)) {
                    System.out.print("Correct: ");
                } else {
                    System.out.print("Wrong  : ");
                }
            }
            System.out.println(i_norm_var1 + " = " + i_var1 + " << " + s_result);
        }
        
        
        // int toInt(short s_var1)
        System.out.println("\nTesting toInt():");
        i_result = toInt(Short.MAX_VALUE);
        System.out.println("toInt(Short.MAX_VALUE) = " + i_result);
        
        i_result = toInt(Short.MIN_VALUE);
        System.out.println("toInt(Short.MIN_VALUE) = " + i_result);
        
        
        // short toShort(int i_var1)
        System.out.println("\nTesting toShort():");
        s_result = toShort(Integer.MAX_VALUE);
        System.out.println("toShort(Integer.MAX_VALUE) = " + s_result);
        
        s_result = toShort(Integer.MIN_VALUE);
        System.out.println("toShort(Integer.MIN_VALUE) = " + s_result);
        
        s_result = toShort(0xFFFFFFFF);
        System.out.println("toShort(0xFFFFFFFF) = " + s_result);
        
    }
}
