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

package com.phono.rtp;

import biz.source_code.Base64Coder;
import com.phono.android.audio.Log;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SRTPSecContext {

    static int KEYLEN = 16; // 128 bit
    static int MACKEYLEN = 20;
    static int BLOCKSZ = 16; // also but possibly different
    private Cipher _anAES;
    private byte[] _sessionKey = new byte[0];
    ByteBuffer _masterSalt;
    private byte[] _cipherSalt;
    private byte[] _sessionAuth;
    private int _tag;
    private int _authTail;
    byte[] _masterKey;
    private int _mki;
    private int _mkiLen;
    private int _kdr;
    private int _keyLife;
    private boolean _doAuth;
    private boolean _doCrypt;
    private boolean _in;
    private String _dirn;

    SRTPSecContext(boolean in) {
        _in = in;
        _dirn = in ? "in " : "out ";
    }

    SRTPSecContext() {
        this(false);
    }

    static byte[] saba(short[] s) {
        byte[] r = new byte[s.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) s[i];
        }
        return r;
    }

    public int getAuthTail() {
        return _authTail;
    }

    public static void main(String argv[]) {
        SRTPSecContext s = new SRTPSecContext();
        try {
            System.out.println("test key derivation");
            s.testDeriv();
            s.testHmac();
        } catch (GeneralSecurityException ex) {
            System.out.println("Failed ");
            ex.printStackTrace();
        }

    }

    private void testResult(byte[] a, byte[] b) throws GeneralSecurityException {
        if (a.length != b.length) {
            throw new GeneralSecurityException("lengths dont match " + a.length + " != " + b.length);
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                throw new GeneralSecurityException("values " + i + " dont match " + a[i] + " != " + b[i]);
            }
        }
    }

    private void testHmac() throws GeneralSecurityException {
        short hmac_test_case_0_keyS[] = {
            0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b,
            0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b,
            0x0b, 0x0b, 0x0b, 0x0b
        };

        short hmac_test_case_0_dataS[] = {
            0x48, 0x69, 0x20, 0x54, 0x68, 0x65, 0x72, 0x65 /* "Hi There" */};

        short hmac_test_case_0_tagS[] = {
            0xb6, 0x17, 0x31, 0x86, 0x55, 0x05, 0x72, 0x64,
            0xe2, 0x8b, 0xc0, 0xb6, 0xfb, 0x37, 0x8c, 0x8e,
            0xf1, 0x46, 0xbe, 0x00
        };
        System.err.println("test hmac ");

        byte[] hmac_test_case_0_key = saba(hmac_test_case_0_keyS);
        byte[] hmac_test_case_0_data = saba(hmac_test_case_0_dataS);
        byte[] hmac_test_case_0_tag = saba(hmac_test_case_0_tagS);
        this._sessionAuth = hmac_test_case_0_key;
        Mac m = this.getAuthMac();
        byte[] res = m.doFinal(hmac_test_case_0_data);
        for (int i = 0; i < res.length; i++) {
            if (res[i] != hmac_test_case_0_tag[i]) {
                throw new GeneralSecurityException("auth mac failed selftest ");
            }
        }
    }

    private void testDeriv() throws GeneralSecurityException {
        short[] masterkeyS = {0xE1, 0xF9, 0x7A, 0x0D, 0x3E, 0x01, 0x8B, 0xE0, 0xD6, 0x4F, 0xA3, 0x2C, 0x06, 0xDE, 0x41, 0x39};
        short[] mastersaltS = {0x0E, 0xC6, 0x75, 0xAD, 0x49, 0x8A, 0xFE, 0xEB, 0xB6, 0x96, 0x0B, 0x3A, 0xAB, 0xE6};
        short[] plaintextS = {0x0E, 0xC6, 0x75, 0xAD, 0x49, 0x8A, 0xFE, 0xEB, 0xB6, 0x96, 0x0B, 0x3A, 0xAB, 0xE6, 0x00, 0x00};
        short[] reskeyS = {0xC6, 0x1E, 0x7A, 0x93, 0x74, 0x4F, 0x39, 0xEE, 0x10, 0x73, 0x4A, 0xFE, 0x3F, 0xF7, 0xA0, 0x87};
        short[] csaltS = {0x30, 0xCB, 0xBC, 0x08, 0x86, 0x3D, 0x8C, 0x85, 0xD4, 0x9D, 0xB3, 0x4A, 0x9A, 0xE1, 0x7A, 0xC6};
        short[] authKeyS = {0xCE, 0xBE, 0x32, 0x1F, 0x6F, 0xF7, 0x71, 0x6B, 0x6F, 0xD4, 0xAB, 0x49, 0xAF, 0x25, 0x6A, 0x15, 0x6D, 0x38, 0xBA, 0xA4};
        _masterKey = saba(masterkeyS);
        byte mastersalt[] = saba(mastersaltS);
        _masterSalt = ByteBuffer.allocate(16);
        _masterSalt.put(mastersalt);
        byte plaintext[] = saba(plaintextS);
        byte reskey[] = saba(reskeyS);
        byte csalt[] = saba(csaltS);
        byte authkey[] = saba(authKeyS);
        deriveKeys(0, 0);
        System.err.println("test session key");
        testResult(_sessionKey, reskey);

        System.err.println("test session salt");
        testResult(_cipherSalt, csalt);


        System.err.println("test auth key");
        testResult(_sessionAuth, authkey);

    }

    static void bbxor(ByteBuffer l, ByteBuffer r) {
        int num = Math.min(l.capacity(), r.capacity());
        for (int i = 0; i < num; i++) {
            byte a = (byte) (r.get(i) ^ l.get(i));
            l.put(i, a);
        }
    }

    ByteBuffer cloneByteBuffer(ByteBuffer orig) {
        ByteBuffer ret = ByteBuffer.allocate(orig.capacity());
        ret.put(orig.array(), 0, orig.capacity());
        return ret;
    }

    void deriveKeys(long index) throws GeneralSecurityException {
        if (haveKeys() && (_kdr != 0) && ((index % _kdr) == 0)) {
            deriveKeys(index, _kdr);
        } else {
            if (!haveKeys()) {
                deriveKeys(0L, 0);
            }
        }
    }

    private void deriveKeys(long index, int kdr) throws GeneralSecurityException {
        /*
        The input block for
        AES-CM is generated by exclusive-oring the master salt with the
        concatenation of the encryption key label 0x00 with (index DIV kdr),
        then padding on the right with two null octets
         *
         */

        /*
         * The resulting value
        is then AES-CM- encrypted using the master key to get the cipher key.
         */
        byte label = 0;
        ByteBuffer myinpblk = cloneByteBuffer(_masterSalt);
        ByteBuffer lex = ByteBuffer.allocate(BLOCKSZ);
        long idivk = (kdr == 0) ? 0 : index / kdr;
        lex.putLong(6, idivk);
        lex.put(7, label);
        bbxor(myinpblk, lex);
        _sessionKey = getKeyBytes(myinpblk, KEYLEN);

        label = 2;
        myinpblk = cloneByteBuffer(_masterSalt);
        lex = ByteBuffer.allocate(BLOCKSZ);
        idivk = (kdr == 0) ? 0 : index / kdr;
        lex.putLong(6, idivk);
        lex.put(7, label);
        bbxor(myinpblk, lex);
        _cipherSalt = getKeyBytes(myinpblk, KEYLEN);
        // now zap the tail bytes

        _cipherSalt[14] = 0;
        _cipherSalt[15] = 0;




        label = 1;
        myinpblk = cloneByteBuffer(_masterSalt);
        lex = ByteBuffer.allocate(BLOCKSZ);
        idivk = (kdr == 0) ? 0 : index / kdr;
        lex.putLong(6, idivk);
        lex.put(7, label);

        bbxor(myinpblk, lex);

        _sessionAuth = getKeyBytes(myinpblk, MACKEYLEN);
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "Derive keys for index = " + index + " kdr =" + kdr);
            Log.verb(_dirn + "Session Key = " + SRTPProtocolImpl.getHex(_sessionKey));
            Log.verb(_dirn + "CipherSalt Key = " + SRTPProtocolImpl.getHex(_cipherSalt));
            Log.verb(_dirn + "Auth Key = " + SRTPProtocolImpl.getHex(_sessionAuth));
        }
    }

    /*
     *
     * repeatedly encrypt the salt - and a counter
     * resulting in a pseudo random stream of bytes of
     * the required length.
     * (but predictable if you have the key and salt)
     */
    void getCypherStreamBytes(Cipher aes, ByteBuffer asalt, ByteBuffer stream) throws GeneralSecurityException {
        int blocksz = 128 / 8;
        ByteBuffer slop;

        int toget = stream.capacity() - 32; // ensuring that there are 16bytes of overhead
        stream.position(0);
        int blks = toget / blocksz;
        char bno = 0;
        // repeatedly encrypt the salt (and some pepper)
        // where the pepper is 16bit unsigned int at the end of the 128 bit int
        // the pepper increments once with each round.
        // tedious issue where aes won't crypt unless there are 32 bytes to go in stream.

        for (; bno < blks; bno++) {
            asalt.putChar(14, bno);
            asalt.position(0);
            aes.update(asalt, stream);
        }
        // now finish off the < 128 bits at the end.
        asalt.putChar(14, bno);
        asalt.position(0);
        aes.doFinal(asalt, stream);
    }

    private byte[] getKeyBytes(ByteBuffer inp, int want) throws GeneralSecurityException {
        int blocksz = 128 / 8;
        byte[] ret = null;
        if (want == blocksz) {
            // odd special case where crypto wants bigger bucket
            ByteBuffer slop = ByteBuffer.allocate(want * 2);
            inp.putChar(14, (char) 0);
            inp.position(0);
            _anAES.doFinal(inp, slop);
            ret = new byte[want];
            slop.position(0);
            slop.get(ret);
        } else {
            ByteBuffer stream = ByteBuffer.allocate(want + 32);
            getCypherStreamBytes(_anAES, inp, stream);
            ret = new byte[want];
            System.arraycopy(stream.array(), 0, ret, 0, want);
        }
        return ret;
    }

    Mac getAuthMac() throws GeneralSecurityException {
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "getting Hmac key = " + SRTPProtocolImpl.getHex(_sessionAuth));
        }
        SecretKey key = new SecretKeySpec(this._sessionAuth, "HmacSHA1");
        Mac m = Mac.getInstance("HmacSHA1");
        m.init(key);
        return m;
    }

    void decipher(ByteBuffer in, ByteBuffer out, ByteBuffer pepper) throws GeneralSecurityException {


        SecretKeySpec keyp = new SecretKeySpec(_sessionKey, "AES");

        Cipher aes = Cipher.getInstance("AES"); // perhaps AES/None/NoPadding or ""
        aes.init(Cipher.ENCRYPT_MODE, keyp);
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: key    =" + SRTPProtocolImpl.getHex(_sessionKey));
        }

        /*
        where the 128-bit integer value IV SHALL be defined by the SSRC, the
        SRTP packet index i, and the SRTP session salting key k_s, as below.

        IV = (k_s * 2^16) XOR (SSRC * 2^64) XOR (i * 2^16)
         *
         */
        ByteBuffer csalt = ByteBuffer.wrap(_cipherSalt);
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: salt   =" + SRTPProtocolImpl.getHex(csalt.array()));
        }
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: pepper =" + SRTPProtocolImpl.getHex(pepper.array()));
        }


        bbxor(pepper, csalt);
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: IV     =" + SRTPProtocolImpl.getHex(pepper.array()));
        }

        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: in     =" + SRTPProtocolImpl.getHex(in.array()));
        }
        getCypherStreamBytes(aes, pepper, out);
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: stream =" + SRTPProtocolImpl.getHex(out.array()));
        }

        bbxor(out, in);
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb(_dirn + "decipher: xor    =" + SRTPProtocolImpl.getHex(out.array()));
        }

    }

    boolean haveKeys() {
        return ((_sessionAuth != null) && (_sessionKey != null) && (this._cipherSalt != null));
    }

    static String stripQ(String s) {
        String d = s;
        if ((s != null) && (s.indexOf("'") == 0)) {
            int lq = s.lastIndexOf("'");

            d = s.substring(1, lq);

        }

        return d;
    }

    void parseCryptoProps(Properties cryptoProps) throws GeneralSecurityException {
        /*
         * required='1' crypto-suite='AES_CM_128_HMAC_SHA1_80' key-params='inline:WVNfX19zZW1jdGwgKCkgewkyMjA7fQp9CnVubGVz' session-params='KDR=1 UNENCRYPTED_SRTCP' tag='1'
         */
        String crypto_suite = stripQ(cryptoProps.getProperty("crypto-suite"));
        String key_params = stripQ(cryptoProps.getProperty("key-params"));
        String session_params = stripQ(cryptoProps.getProperty("session-params"));
        String tag = stripQ(cryptoProps.getProperty("tag"));
        if (crypto_suite.equals("AES_CM_128_HMAC_SHA1_80")) {
            _authTail = 10;
        } else if (crypto_suite.equals("AES_CM_128_HMAC_SHA1_32")) {
            _authTail = 4;
        } else {
            throw new GeneralSecurityException("Unsupported crypto suite " + crypto_suite);
        }
        if ((key_params != null) && (key_params.startsWith("inline:"))) {
            // "inline:" <key||salt> ["|" lifetime] ["|" MKI ":" length]
            String ks = key_params.substring("inline:".length());
            String mkil = null;
            String life = null;
            if (ks.indexOf("|") > 0) {
                String[] kbits = ks.split("\\|");
                ks = kbits[0];

                // now deal with the ugly optional syntax

                if ((kbits.length > 1) && kbits[1].contains(":")) {
                    mkil = kbits[1];
                } else {
                    if (kbits.length > 1) {
                        life = kbits[1];
                    }
                    if ((kbits.length > 2) && kbits[1].contains(":")) {
                        mkil = kbits[2];
                    }
                }
            }
            Log.debug(_dirn + "key n salt =" + ks);
            byte mks[] = Base64Coder.decode(ks);
            if (mks.length != 30) {
                throw new GeneralSecurityException("Master ket/salt too short - expecting 30 got" + mks.length);
            }
            _masterKey = new byte[16];
            System.arraycopy(mks, 0, _masterKey, 0, _masterKey.length);
            SecretKeySpec keyp = new SecretKeySpec(_masterKey, "AES");
            _anAES = Cipher.getInstance("AES"); // perhaps AES/None/NoPadding or ""
            _anAES.init(Cipher.ENCRYPT_MODE, keyp);
            _masterSalt = ByteBuffer.allocate(KEYLEN);
            _masterSalt.put(mks, _masterKey.length, 14);


            if (mkil != null) {
                String[] mbits = mkil.split("\\:");
                if (mbits.length == 2) {
                    _mki = Integer.parseInt(mbits[0]);
                    _mkiLen = Integer.parseInt(mbits[1]);
                    Log.warn(_dirn + "MKI:length defined");
                } else {
                    throw new GeneralSecurityException("Expecting MKI:length");
                }
            }
            if (life != null) {
                if (life.contains("^")) {
                    String klb[] = life.split("\\^");
                    if (klb.length == 2) {
                        int f = Integer.parseInt(klb[0]);
                        int p = Integer.parseInt(klb[1]);
                        if (f == 2) {
                            _keyLife = 1 << p;
                        } else {
                            _keyLife = (int) Math.pow(f, p);
                        }
                    }
                } else {
                    _keyLife = Integer.parseInt(life);
                }
                Log.warn("Key life is " + _keyLife);

            }
        } else {
            throw new GeneralSecurityException("Missing inline key/salt in SDP");
        }

// now do something with the session params and the tag.
        if (session_params != null) {
            String[] sbits = session_params.split(" ");
            for (int i = 0; i < sbits.length; i++) {
                Log.debug(_dirn + "SDP session param " + sbits[i]);

                if (sbits[i].startsWith("KDR=")) {
                    //The SRTP Key Derivation Rate is the rate at which a
                    // pseudo-random function is applied to a master key.
                    String kdrs = sbits[i].substring("KDR=".length());
                    _kdr = Integer.parseInt(kdrs);
                    Log.debug(_dirn + "setting kdr to " + _kdr);
                    if (_kdr != 0) {
                        Log.debug(_dirn + "Note: Non-zero kdr");
                    }
                }
                if ("UNENCRYPTED_SRTP".equals(sbits[i])) {
                    // - :      SRTP messages are not encrypted.
                    _doCrypt = false;
                    Log.warn(_dirn + "Switching off encryption - is this what you wanted ?");
                }
                if ("UNENCRYPTED_SRTCP".equals(sbits[i])) {
                    //SRTCP messages are not encrypted.
                    Log.debug(_dirn + "No support for SRTCP (yet) so SDP setting ignored");
                }
                if ("UNAUTHENTICATED_SRTP".equals(sbits[i])) {
                    _doAuth = false;
                }
                //:  SRTP messages are not authenticated.
                if ("FEC_ORDER".equals(sbits[i])) {
                    //Order of forward error correction (FEC)
                    //           relative to SRTP services.
                }
                if ("FEC_KEY".equals(sbits[i])) {
                    //Master Key for FEC when the FEC stream is sent
                    //           to a separate address and/or port.
                }
                if ("WSH".equals(sbits[i])) {
                    //Window Size Hint.
                }
            }
        }
        if (tag != null) {
            _tag = Integer.parseInt(tag);
            Log.debug(_dirn + "SDP tag = " + _tag);
        }
    }
    /*
    4.3.1.  Key Derivation Algorithm

    Regardless of the encryption or message authentication transform that
    is employed (it may be an SRTP pre-defined transform or newly
    introduced according to Section 6), interoperable SRTP
    implementations MUST use the SRTP key derivation to generate session
    keys.  Once the key derivation rate is properly signaled at the start
    of the session, there is no need for extra communication between the
    parties that use SRTP key derivation.

    packet index ---+
    |
    v
    +-----------+ master  +--------+ session encr_key
    | ext       | key     |        |---------->
    | key mgmt  |-------->|  key   | session auth_key
    | (optional |         | deriv  |---------->
    | rekey)    |-------->|        | session salt_key
    |           | master  |        |---------->
    +-----------+ salt    +--------+

    Figure 5: SRTP key derivation.

    At least one initial key derivation SHALL be performed by SRTP, i.e.,
    the first key derivation is REQUIRED.  Further applications of the
    key derivation MAY be performed, according to the
    "key_derivation_rate" value in the cryptographic context.  The key
    derivation function SHALL initially be invoked before the first
    packet and then, when r > 0, a key derivation is performed whenever
    index mod r equals zero.  This can be thought of as "refreshing" the
    session keys.  The value of "key_derivation_rate" MUST be kept fixed
    for the lifetime of the associated master key.

    Interoperable SRTP implementations MAY also derive session salting
    keys for encryption transforms, as is done in both of the pre-
    defined transforms.

    Let m and n be positive integers.  A pseudo-random function family is
    a set of keyed functions {PRF_n(k,x)} such that for the (secret)
    random key k, given m-bit x, PRF_n(k,x) is an n-bit string,
    computationally indistinguishable from random n-bit strings, see
    [HAC].  For the purpose of key derivation in SRTP, a secure PRF with
    m = 128 (or more) MUST be used, and a default PRF transform is
    defined in Section 4.3.3.

    Let "a DIV t" denote integer division of a by t, rounded down, and
    with the convention that "a DIV 0 = 0" for all a.  We also make the
    convention of treating "a DIV t" as a bit string of the same length
    as a, and thus "a DIV t" will in general have leading zeros.

    Key derivation SHALL be defined as follows in terms of <label>, an
    8-bit constant (see below), master_salt and key_derivation_rate, as
    determined in the cryptographic context, and index, the packet index
    (i.e., the 48-bit ROC || SEQ for SRTP):

     *  Let r = index DIV key_derivation_rate (with DIV as defined above).

     *  Let key_id = <label> || r.

     *  Let x = key_id XOR master_salt, where key_id and master_salt are
    aligned so that their least significant bits agree (right-
    alignment).

    <label> MUST be unique for each type of key to be derived.  We
    currently define <label> 0x00 to 0x05 (see below), and future
    extensions MAY specify new values in the range 0x06 to 0xff for other
    purposes.  The n-bit SRTP key (or salt) for this packet SHALL then be
    derived from the master key, k_master as follows:

    PRF_n(k_master, x).

    (The PRF may internally specify additional formatting and padding of
    x, see e.g., Section 4.3.3 for the default PRF.)

    The session keys and salt SHALL now be derived using:

    - k_e (SRTP encryption): <label> = 0x00, n = n_e.

    - k_a (SRTP message authentication): <label> = 0x01, n = n_a.

    - k_s (SRTP salting key): <label> = 0x02, n = n_s.

    where n_e, n_s, and n_a are from the cryptographic context.

    The master key and master salt MUST be random, but the master salt
    MAY be public.

    Note that for a key_derivation_rate of 0, the application of the key
    derivation SHALL take place exactly once.

    The definition of DIV above is purely for notational convenience.
    For a non-zero t among the set of allowed key derivation rates, "a
    DIV t" can be implemented as a right-shift by the base-2 logarithm of

    t.  The derivation operation is further facilitated if the rates are
    chosen to be powers of 256, but that granularity was considered too
    coarse to be a requirement of this specification.

    The upper limit on the number of packets that can be secured using
    the same master key (see Section 9.2) is independent of the key
    derivation.
     */
    /*
    The currently defined PRF, keyed by 128, 192, or 256 bit master key,
    has input block size m = 128 and can produce n-bit outputs for n up
    to 2^23.  PRF_n(k_master,x) SHALL be AES in Counter Mode as described
    in Section 4.1.1, applied to key k_master, and IV equal to (x*2^16),
    and with the output keystream truncated to the n first (left-most)
    bits.  (Requiring n/128, rounded up, applications of AES.)
     */
    // test code .....

    /*
    B.3.  Key Derivation Test Vectors

    This section provides test data for the default key derivation
    function, which uses AES-128 in Counter Mode.  In the following, we
    walk through the initial key derivation for the AES-128 Counter Mode
    cipher, which requires a 16 octet session encryption key and a 14
    octet session salt, and an authentication function which requires a
    94-octet session authentication key.  These values are called the
    cipher key, the cipher salt, and the auth key in the following.
    Since this is the initial key derivation and the key derivation rate
    is equal to zero, the value of (index DIV key_derivation_rate) is
    zero (actually, a six-octet string of zeros).  In the following, we
    shorten key_derivation_rate to kdr.

    The inputs to the key derivation function are the 16 octet master key
    and the 14 octet master salt:

    master key:  E1F97A0D3E018BE0D64FA32C06DE4139
    master salt: 0EC675AD498AFEEBB6960B3AABE6

    We first show how the cipher key is generated.  The input block for
    AES-CM is generated by exclusive-oring the master salt with the
    concatenation of the encryption key label 0x00 with (index DIV kdr),
    then padding on the right with two null octets (which implements the
    multiply-by-2^16 operation, see Section 4.3.3).  The resulting value
    is then AES-CM- encrypted using the master key to get the cipher key.

    index DIV kdr:                 000000000000
    label:                       00
    master salt:   0EC675AD498AFEEBB6960B3AABE6
    -----------------------------------------------
    xor:           0EC675AD498AFEEBB6960B3AABE6     (x, PRF input)

    x*2^16:        0EC675AD498AFEEBB6960B3AABE60000 (AES-CM input)

    cipher key:    C61E7A93744F39EE10734AFE3FF7A087 (AES-CM output)

    Next, we show how the cipher salt is generated.  The input block for
    AES-CM is generated by exclusive-oring the master salt with the
    concatenation of the encryption salt label.  That value is padded and
    encrypted as above.

    index DIV kdr:                 000000000000
    label:                       02
    master salt:   0EC675AD498AFEEBB6960B3AABE6

    ----------------------------------------------
    xor:           0EC675AD498AFEE9B6960B3AABE6     (x, PRF input)

    x*2^16:        0EC675AD498AFEE9B6960B3AABE60000 (AES-CM input)

    30CBBC08863D8C85D49DB34A9AE17AC6 (AES-CM ouptut)

    cipher salt:   30CBBC08863D8C85D49DB34A9AE1

    We now show how the auth key is generated.  The input block for AES-
    CM is generated as above, but using the authentication key label.

    index DIV kdr:                   000000000000
    label:                         01
    master salt:     0EC675AD498AFEEBB6960B3AABE6
    -----------------------------------------------
    xor:             0EC675AD498AFEEAB6960B3AABE6     (x, PRF input)

    x*2^16:          0EC675AD498AFEEAB6960B3AABE60000 (AES-CM input)

    Below, the auth key is shown on the left, while the corresponding AES
    input blocks are shown on the right.

    auth key                           AES input blocks
    CEBE321F6FF7716B6FD4AB49AF256A15   0EC675AD498AFEEAB6960B3AABE60000
    6D38BAA48F0A0ACF3C34E2359E6CDBCE   0EC675AD498AFEEAB6960B3AABE60001
    E049646C43D9327AD175578EF7227098   0EC675AD498AFEEAB6960B3AABE60002
    6371C10C9A369AC2F94A8C5FBCDDDC25   0EC675AD498AFEEAB6960B3AABE60003
    6D6E919A48B610EF17C2041E47403576   0EC675AD498AFEEAB6960B3AABE60004
    6B68642C59BBFC2F34DB60DBDFB2       0EC675AD498AFEEAB6960B3AABE60005

     */
}
