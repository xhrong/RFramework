package com.xhr.android.rframework.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加密、解密、摘要算法
 *
 * State：未完成
 *
 * Created by xhrong on 2016/11/8.
 */
public class CryptoUtils {

    /**
     * RSA非对称加密算法。用法：1公钥加密(C)，私钥解密(S)；2私钥加密(S)，公钥解密(C)。
     */
    public static class RSAHelper {

        /**
         * 指定加密算法为RSA
         */
        private static String ALGORITHM = "RSA";

        /**
         * 指定key的大小
         */
        private static int KEYSIZE = 1024;

        /**
         * 指定公钥存放文件
         */
        private static String PUBLIC_KEY_FILE = "PublicKey";

        /**
         * 指定私钥存放文件
         */
        private static String PRIVATE_KEY_FILE = "PrivateKey";


        /**
         * 生成密钥对
         */
        private static void generateKeyPair() throws Exception {
            /** RSA算法要求有一个可信任的随机数源 */
            SecureRandom sr = new SecureRandom();
            /** 为RSA算法创建一个KeyPairGenerator对象 */
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
            /** 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
            kpg.initialize(KEYSIZE, sr);
            /** 生成密匙对 */
            KeyPair kp = kpg.generateKeyPair();
            /** 得到公钥 */
            Key publicKey = kp.getPublic();
            /** 得到私钥 */
            Key privateKey = kp.getPrivate();
            /** 用对象流将生成的密钥写入文件 */
            ObjectOutputStream oos1 = new ObjectOutputStream(new FileOutputStream(PUBLIC_KEY_FILE));
            ObjectOutputStream oos2 = new ObjectOutputStream(new FileOutputStream(PRIVATE_KEY_FILE));
            oos1.writeObject(publicKey);
            oos2.writeObject(privateKey);
            /** 清空缓存，关闭文件输出流 */
            oos1.close();
            oos2.close();
        }

        /**
         * 加密方法 source： 源数据
         */
        public static String encrypt(String source) throws Exception {
            /** 将文件中的公钥对象读出 */
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
            Key key = (Key) ois.readObject();
            ois.close();
            /** 得到Cipher对象来实现对源数据的RSA加密 */
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] b = source.getBytes();
            /** 执行加密操作 */
            byte[] b1 = cipher.doFinal(b);
            return Base64.encode(b1);
        }

        /**
         * 解密算法 cryptograph:密文
         */
        public static String decrypt(String cryptograph) throws Exception {
            /** 将文件中的私钥对象读出 */
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PRIVATE_KEY_FILE));
            Key key = (Key) ois.readObject();
            ois.close();
            /** 得到Cipher对象对已用公钥加密的数据进行RSA解密 */
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] b1 = Base64.decode(cryptograph);
            /** 执行解密操作 */
            byte[] b = cipher.doFinal(b1);
            return new String(b);
        }

    }

    /**
     * 3DES加解密
     *
     * @author steven-pan
     */
    public static class DESedeHelper {

        private static final String ALGORITHM_MD5 = "md5";

        private static final String ALGORITHM_DESEDE = "DESede";

        private static final String CIPHER_TRANSFORMATION = "DESede/CBC/PKCS5Padding";

        private static final String CHARSET_UTF_8 = "UTF-8";


        /**
         * encoded message
         *
         * @param message origin message
         * @param sKey    origin privateKey
         * @return
         * @throws Exception
         */
        public static byte[] encrypt(String message, String sKey) throws Exception {
            final byte[] keyBytes = getKeyBytes(sKey);

            final SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM_DESEDE);
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            final byte[] plainTextBytes = message.getBytes(CHARSET_UTF_8);
            final byte[] cipherText = cipher.doFinal(plainTextBytes);

            return cipherText;
        }

        /**
         * decode from encoded message
         *
         * @param message encoded message
         * @param sKey    origin privateKey
         * @return
         * @throws Exception
         */
        public static String decrypt(byte[] message, String sKey) throws Exception {
            final byte[] keyBytes = getKeyBytes(sKey);

            final SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM_DESEDE);
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            final Cipher decipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            decipher.init(Cipher.DECRYPT_MODE, key, iv);

            final byte[] plainText = decipher.doFinal(message);
            return new String(plainText, CHARSET_UTF_8);
        }

        /**
         * generate keyBytes
         *
         * @param sKey origin privateKey
         * @return
         * @throws Exception
         */
        private static byte[] getKeyBytes(String sKey) throws Exception {
            final MessageDigest md = MessageDigest.getInstance(ALGORITHM_MD5);
            final byte[] digestOfPassword = md.digest(sKey.getBytes(CHARSET_UTF_8));
            final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
            for (int j = 0, k = 16; j < 8; ) {
                keyBytes[k++] = keyBytes[j++];
            }
            return keyBytes;
        }
    }


    public static class AESHelper {

        private static Cipher cipher = null; // 私鈅加密对象Cipher

        static {
            try {
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            }
        }

        /**
         * 加密
         *
         * @param message
         * @return
         */
        public static byte[] encrypt(String message, String passWord) {
            try {
            /* AES算法 */
                SecretKey secretKey = new SecretKeySpec(passWord.getBytes(), "AES");// 获得密钥
            /* 获得一个私鈅加密类Cipher，DESede-》AES算法，ECB是加密模式，PKCS5Padding是填充方式 */
                cipher.init(Cipher.ENCRYPT_MODE, secretKey); // 设置工作模式为加密模式，给出密钥
                byte[] resultBytes = cipher.doFinal(message.getBytes("UTF-8")); // 正式执行加密操作
                return resultBytes;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 解密
         *
         * @param messageBytes
         * @return
         * @throws Exception
         */
        public static String decrypt(byte[] messageBytes, String passWord) {
            String result = "";
            try {
            /* AES算法 */
                SecretKey secretKey = new SecretKeySpec(passWord.getBytes(), "AES");// 获得密钥
                cipher.init(Cipher.DECRYPT_MODE, secretKey); // 设置工作模式为解密模式，给出密钥
                byte[] resultBytes = cipher.doFinal(messageBytes);// 正式执行解密操作
                result = new String(resultBytes, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        /**
         * 去掉加密字符串换行符
         *
         * @param str
         * @return
         */
        public static String filter(String str) {
            String output = "";
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                int asc = str.charAt(i);
                if (asc != 10 && asc != 13) {
                    sb.append(str.subSequence(i, i + 1));
                }
            }
            output = new String(sb);
            return output;
        }

    }

    public static class SignatureHelper {

        /**
         * algorithm: MD5
         */
        private static final String ALGORITHM_MD5 = "MD5";

        /**
         * algorithm: SHA-1
         */
        private static final String ALGORITHM_SHA1 = "SHA-1";

        /**
         * 对字符串取MD5值
         *
         * @param strInput 输入字符串
         * @return
         */
        public static String encryptStringMD5(String strInput) {
            return encyptByAlogrithm(strInput, ALGORITHM_MD5);
        }

        /**
         * 取文件MD5值
         *
         * @param file
         * @return
         */
        public static String encryptFileMD5(File file) {
            FileInputStream fis = null;
            MessageDigest md;
            try {
                md = MessageDigest.getInstance(ALGORITHM_MD5);
                fis = new FileInputStream(file);
                byte[] buffer = new byte[102400];
                int length;
                while ((length = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, length);
                }
                return Hex.encode(md.digest());
            } catch (Exception e) {
                return null;
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 对字符串取SHA-1值
         *
         * @param strInput 输入字符串
         * @return
         */
        public static String encryptStringSHA1(String strInput) {
            return encyptByAlogrithm(strInput, ALGORITHM_SHA1);
        }

        /**
         * 按输入算法名取摘要签名
         *
         * @param input     输入字符串
         * @param alogrithm 摘要算法名
         * @return
         */
        private static String encyptByAlogrithm(String input, String alogrithm) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest
                        .getInstance(alogrithm);
                md.update(input.getBytes("utf-8"));
                byte temp[] = md.digest();
                return Hex.encode(temp);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * HEX字符串与字节码转换工具
     *
     * @author steven-pan
     */
    public static class Hex {

        /**
         * 将16进制转换为二进制(服务端)
         *
         * @param hexStr
         * @return
         */
        public static byte[] deocde(String hexStr) {
            if (hexStr.length() < 1)
                return null;
            byte[] result = new byte[hexStr.length() / 2];
            for (int i = 0; i < hexStr.length() / 2; i++) {
                int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
                int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
                result[i] = (byte) (high * 16 + low);
            }
            return result;
        }

        /**
         * 将二进制转换成16进制
         *
         * @param buf
         * @return
         */
        public static String encode(byte buf[]) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < buf.length; i++) {
                String hex = Integer.toHexString(buf[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                sb.append(hex.toUpperCase());
            }
            return sb.toString();
        }

    }

    /**
     * Base64字符串与字节码转换工具
     *
     * @author steven-pan
     */
    public static class Base64 {
        private static final char[] legalChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        /**
         * data[]进行编码
         *
         * @param data
         * @return
         */
        public static String encode(byte[] data) {
            int start = 0;
            int len = data.length;
            StringBuffer buf = new StringBuffer(data.length * 3 / 2);

            int end = len - 3;
            int i = start;
            int n = 0;

            while (i <= end) {
                int d = ((((int) data[i]) & 0x0ff) << 16)
                        | ((((int) data[i + 1]) & 0x0ff) << 8)
                        | (((int) data[i + 2]) & 0x0ff);
                buf.append(legalChars[(d >> 18) & 63]);
                buf.append(legalChars[(d >> 12) & 63]);
                buf.append(legalChars[(d >> 6) & 63]);
                buf.append(legalChars[d & 63]);
                i += 3;
                if (n++ >= 14) {
                    n = 0;
                    buf.append(" ");
                }
            }

            if (i == start + len - 2) {
                int d = ((((int) data[i]) & 0x0ff) << 16)
                        | ((((int) data[i + 1]) & 255) << 8);
                buf.append(legalChars[(d >> 18) & 63]);
                buf.append(legalChars[(d >> 12) & 63]);
                buf.append(legalChars[(d >> 6) & 63]);
                buf.append("=");
            } else if (i == start + len - 1) {
                int d = (((int) data[i]) & 0x0ff) << 16;
                buf.append(legalChars[(d >> 18) & 63]);
                buf.append(legalChars[(d >> 12) & 63]);
                buf.append("==");
            }

            return buf.toString();
        }

        private static int decode(char c) {
            if (c >= 'A' && c <= 'Z')
                return ((int) c) - 65;
            else if (c >= 'a' && c <= 'z')
                return ((int) c) - 97 + 26;
            else if (c >= '0' && c <= '9')
                return ((int) c) - 48 + 26 + 26;
            else
                switch (c) {
                    case '+':
                        return 62;
                    case '/':
                        return 63;
                    case '=':
                        return 0;
                    default:
                        throw new RuntimeException("unexpected code: " + c);
                }
        }

        /**
         * Decodes the given Base64 encoded String to a new byte array. The byte
         * array holding the decoded data is returned.
         */

        public static byte[] decode(String s) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                decode(s, bos);
            } catch (IOException e) {
                throw new RuntimeException();
            }
            byte[] decodedBytes = bos.toByteArray();
            try {
                bos.close();
                bos = null;
            } catch (IOException ex) {
                System.err.println("Error while decoding BASE64: " + ex.toString());
            }
            return decodedBytes;
        }

        private static void decode(String s, OutputStream os) throws IOException {
            int i = 0;
            int len = s.length();
            while (true) {
                while (i < len && s.charAt(i) <= ' ')
                    i++;
                if (i == len)
                    break;
                int tri = (decode(s.charAt(i)) << 18)
                        + (decode(s.charAt(i + 1)) << 12)
                        + (decode(s.charAt(i + 2)) << 6)
                        + (decode(s.charAt(i + 3)));
                os.write((tri >> 16) & 255);
                if (s.charAt(i + 2) == '=')
                    break;
                os.write((tri >> 8) & 255);
                if (s.charAt(i + 3) == '=')
                    break;
                os.write(tri & 255);
                i += 4;
            }
        }

        /**
         * 去掉BASE64加密字符串换行符
         *
         * @param str
         * @return
         */
        public static String filter(String str) {
            String output = "";
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                int asc = str.charAt(i);
                if (asc != 10 && asc != 13) {
                    sb.append(str.subSequence(i, i + 1));
                }
            }
            output = new String(sb);
            return output;
        }


        public static void main(String[] args) {
            try {
                System.out.println(encode("asdssdfafasdfsw12345?@!#~#@#%#$%&%^*&&(&*)_()+()+sdfadsfsdfsfasd-*/878ssdfasdfasdfsadfsdafsadfasdfasdf".getBytes("UTF-8")));
                System.out.println(new String(decode("YXNkc3NkZmFmYXNkZnN3MTIzNDU/QCEjfiNAIyUjJCUmJV4qJiYoJiopXygp KygpK3NkZmFkc2ZzZGZzZmFzZC0qLzg3OHNzZGZhc2RmYXNkZnNhZGZzZGFm c2FkZmFzZGZhc2Rm"), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("MD5签名编测试：");
        System.out.println(SignatureHelper.encryptStringMD5("abcv"));
        System.out.println(SignatureHelper.encryptStringSHA1("abcv"));


        System.out.println("AES加解密测试：");

        String password = "c8a9229820ffa315bc6a17a9e43d01a9";
        String content = "6222001521522152212";
        // 加密（传输)
        System.out.println("加密前：" + content);
        byte[] encryptResult = AESHelper.encrypt(content, password);

        // 以HEX进行传输
        String codedtextb = Base64.encode(encryptResult);// data transfer as text
        System.out.println("Base64 format:" + codedtextb);
        encryptResult = Base64.decode(codedtextb);

        // 解密
        String decryptResultb = AESHelper.decrypt(encryptResult, password);
        System.out.println("解密后：" + decryptResultb);


        System.out.println("RSA加解密测试：");

        RSAHelper.generateKeyPair();

        final String source = "73C58BAFE578C59366D8C995CD0B9";// 要加密的字符串
        System.out.println("加密前:" + source);

        String cryptograph = RSAHelper.encrypt(source);// 生成的密文
        System.out.println("Base64 format:" + cryptograph);

        String target = RSAHelper.decrypt(cryptograph);// 解密密文
        System.out.println("解密后:" + target);


        System.out.println("DESede加解密测试：");

        final String privateKey = "HG58YZ3CR9HG58YZ3CR9HG58YZ3CR9";

        String text = "hello world!";// origin data
        System.out.println("加密前:" + text);
        byte[] codedtext = DESedeHelper.encrypt(text, privateKey);

        String codedtextb1 = Base64.encode(codedtext);// data transfer as text
        System.out.println("Base64 format:" + codedtextb1);
        codedtext = Base64.decode(codedtextb1);

        String decodedtext = DESedeHelper.decrypt(codedtext, privateKey);
        System.out.println("解密后:" + decodedtext); // This correctly shows "hello world!"


    }
}
