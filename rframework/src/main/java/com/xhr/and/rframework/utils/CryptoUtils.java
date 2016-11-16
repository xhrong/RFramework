package com.xhr.and.rframework.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 1、RSA加密、解密和签名验证
 * 2、AES字符串和文件加解密
 * 3、摘要算法MD5、SHA系列
 * <p/>
 * State：完成
 * <p/>
 * Created by xhrong on 2016/11/8.
 */
public class CryptoUtils {

    //读SD中的文件
    private static String readFile(File file) {
        String res = "";
        try {
            FileInputStream fin = new FileInputStream(file);

            int length = fin.available();

            byte[] buffer = new byte[length];
            fin.read(buffer);

            res = new String(buffer, "UTF-8");

            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * RSA公钥/私钥/签名工具
     * <p/>
     * 字符串格式的密钥在未在特殊说明情况下都为BASE64编码格式<br/>
     * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
     * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
     */
    public static class RSAHelper {

        public static final String RSA = "RSA";// 非对称加密密钥算法
        public static final String ECB_PKCS1_PADDING = "RSA/ECB/PKCS1PADDING";//加密填充方式
        public static int KEY_SIZE = 2048;//秘钥默认长度
        public static final byte[] DEFAULT_SPLIT = "#PART#".getBytes();    // 当要加密的内容超过bufferSize，则采用partSplit进行分块加密
        public static int BUFFER_SIZE = (KEY_SIZE / 8) - 11;// 当前秘钥支持加密的最大字节数
        public static final String SIGNATURE_ALGORITHM = "MD5withRSA";//签名算法

        /**
         * 设置KeySize。KeySize不同，能够加解密的字符串长度是不同的。
         *
         * @param keySize 密钥长度，范围：512～2048。一般1024
         **/
        public static void setKeySize(int keySize) {
            KEY_SIZE = keySize;
            BUFFER_SIZE = (KEY_SIZE / 8) - 11;
        }

        /**
         * 生成RSA密钥对
         *
         * @return
         */

        public static KeyPair generateKeyPair() {
            return generateKeyPair(null, null);
        }

        /**
         * 随机生成RSA密钥对，并保存的指定文件。不建议使用。建议生成后，自行保存
         *
         * @param publicKeyFile  公钥保存路径
         * @param privateKeyFile 私钥保存路径
         * @return
         */
        @Deprecated
        public static KeyPair generateKeyPair(String publicKeyFile, String privateKeyFile) {
            try {
                /** RSA算法要求有一个可信任的随机数源 */
                SecureRandom sr = new SecureRandom();
                /** 为RSA算法创建一个KeyPairGenerator对象 */
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA);
                /** 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
                kpg.initialize(KEY_SIZE, sr);

                //将密钥写入文件
                if ((privateKeyFile != null && !privateKeyFile.equals("")) && (publicKeyFile != null && !publicKeyFile.equals(""))) {
                    FileOutputStream oos1 = null;
                    FileOutputStream oos2 = null;
                    try {
                        /** 生成密匙对 */
                        KeyPair kp = kpg.generateKeyPair();
                        /** 得到公钥 */
                        Key publicKey = kp.getPublic();
                        /** 得到私钥 */
                        Key privateKey = kp.getPrivate();
                        /** 用对象流将生成的密钥写入文件 */
                        oos1 = new FileOutputStream(publicKeyFile);
                        oos2 = new FileOutputStream(privateKeyFile);
                        oos1.write(Base64.encode(publicKey.getEncoded()).getBytes());
                        oos2.write(Base64.encode(privateKey.getEncoded()).getBytes());

                    } catch (Exception e) {
                        e.printStackTrace();

                    } finally {
                        /** 清空缓存，关闭文件输出流 */
                        if (oos1 != null) oos1.close();
                        if (oos2 != null) oos2.close();
                    }
                }
                return kpg.genKeyPair();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 从字符串中加载公钥
         *
         * @param publicKeyStr 公钥数据字符串
         */
        public static PublicKey loadPublicKey(String publicKeyStr) {
            try {
                byte[] buffer = Base64.decode(publicKeyStr);
                KeyFactory keyFactory = KeyFactory.getInstance(RSA);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
                return keyFactory.generatePublic(keySpec);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 从文件加载公钥
         *
         * @param publicKeyFile 公钥文件
         * @return
         */
        public static PublicKey loadPublicKeyForFile(String publicKeyFile) {
            File file = new File(publicKeyFile);
            if (!file.exists()) return null;

            String publicKeyStr = CryptoUtils.readFile(file);
            if (publicKeyStr == null || publicKeyStr.equals("")) return null;
            return loadPublicKey(publicKeyStr);
        }

        /**
         * 从字符串中加载私钥<br>
         * 加载时使用的是PKCS8EncodedKeySpec（PKCS#8编码的Key指令）。
         *
         * @param privateKeyStr
         * @return
         */
        public static PrivateKey loadPrivateKey(String privateKeyStr) {
            try {
                byte[] buffer = Base64.decode(privateKeyStr);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
                KeyFactory keyFactory = KeyFactory.getInstance(RSA);
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 从文件加载私钥
         *
         * @param privateKeyFile 私钥文件
         * @return
         */
        public static PrivateKey loadPrivateKeyForFile(String privateKeyFile) {
            File file = new File(privateKeyFile);
            if (!file.exists()) return null;

            String privateKeyStr = CryptoUtils.readFile(file);
            if (privateKeyStr == null || privateKeyStr.equals("")) return null;
            return loadPrivateKey(privateKeyStr);
        }


        /**
         * 用私钥对信息生成数字签名
         *
         * @param data       已加密数据
         * @param privateKey 私钥
         * @return
         * @throws Exception
         */
        public static String sign(byte[] data, PrivateKey privateKey) throws Exception {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return Base64.encode(signature.sign());
        }

        /**
         * 校验数字签名
         *
         * @param data      已加密数据
         * @param publicKey 公钥(BASE64编码)
         * @param sign      数字签名
         * @return
         * @throws Exception
         */
        public static boolean verify(byte[] data, PublicKey publicKey, String sign)
                throws Exception {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(Base64.decode(sign));
        }


        /**
         * 公钥分段加密
         *
         * @param data      要加密的原始数据
         * @param publicKey 公钥
         */
        public static byte[] encryptWithPublicKey(byte[] data, PublicKey publicKey) throws Exception {
            int dataLen = data.length;
            if (dataLen <= BUFFER_SIZE) {
                return encryptByPublicKey(data, publicKey);
            }
            List<Byte> allBytes = new ArrayList<Byte>(2048);
            int bufIndex = 0;
            int subDataLoop = 0;
            byte[] buf = new byte[BUFFER_SIZE];
            for (int i = 0; i < dataLen; i++) {
                buf[bufIndex] = data[i];
                if (++bufIndex == BUFFER_SIZE || i == dataLen - 1) {
                    subDataLoop++;
                    if (subDataLoop != 1) {
                        for (byte b : DEFAULT_SPLIT) {
                            allBytes.add(b);
                        }
                    }
                    byte[] encryptBytes = encryptByPublicKey(buf, publicKey);
                    for (byte b : encryptBytes) {
                        allBytes.add(b);
                    }
                    bufIndex = 0;
                    if (i == dataLen - 1) {
                        buf = null;
                    } else {
                        buf = new byte[Math.min(BUFFER_SIZE, dataLen - i - 1)];
                    }
                }
            }
            byte[] bytes = new byte[allBytes.size()];
            {
                int i = 0;
                for (Byte b : allBytes) {
                    bytes[i++] = b.byteValue();
                }
            }
            return bytes;
        }


        /**
         * 私钥分段加密
         *
         * @param data       要加密的原始数据
         * @param privateKey 私钥
         */
        public static byte[] encryptWithPrivateKey(byte[] data, PrivateKey privateKey) throws Exception {
            int dataLen = data.length;
            if (dataLen <= BUFFER_SIZE) {
                return encryptByPrivateKey(data, privateKey);
            }
            List<Byte> allBytes = new ArrayList<Byte>(2048);
            int bufIndex = 0;
            int subDataLoop = 0;
            byte[] buf = new byte[BUFFER_SIZE];
            for (int i = 0; i < dataLen; i++) {
                buf[bufIndex] = data[i];
                if (++bufIndex == BUFFER_SIZE || i == dataLen - 1) {
                    subDataLoop++;
                    if (subDataLoop != 1) {
                        for (byte b : DEFAULT_SPLIT) {
                            allBytes.add(b);
                        }
                    }
                    byte[] encryptBytes = encryptByPrivateKey(buf, privateKey);
                    for (byte b : encryptBytes) {
                        allBytes.add(b);
                    }
                    bufIndex = 0;
                    if (i == dataLen - 1) {
                        buf = null;
                    } else {
                        buf = new byte[Math.min(BUFFER_SIZE, dataLen - i - 1)];
                    }
                }
            }
            byte[] bytes = new byte[allBytes.size()];
            {
                int i = 0;
                for (Byte b : allBytes) {
                    bytes[i++] = b.byteValue();
                }
            }
            return bytes;
        }


        /**
         * 公钥分段解密
         *
         * @param encrypted 待解密数据
         * @param publicKey 公钥
         */
        public static byte[] decryptWithPublicKey(byte[] encrypted, PublicKey publicKey) throws Exception {
            int splitLen = DEFAULT_SPLIT.length;
            if (splitLen <= 0) {
                return decryptByPublicKey(encrypted, publicKey);
            }
            int dataLen = encrypted.length;
            List<Byte> allBytes = new ArrayList<Byte>(1024);
            int latestStartIndex = 0;
            for (int i = 0; i < dataLen; i++) {
                byte bt = encrypted[i];
                boolean isMatchSplit = false;
                if (i == dataLen - 1) {
                    // 到data的最后了
                    byte[] part = new byte[dataLen - latestStartIndex];
                    System.arraycopy(encrypted, latestStartIndex, part, 0, part.length);
                    byte[] decryptPart = decryptByPublicKey(part, publicKey);
                    for (byte b : decryptPart) {
                        allBytes.add(b);
                    }
                    latestStartIndex = i + splitLen;
                    i = latestStartIndex - 1;
                } else if (bt == DEFAULT_SPLIT[0]) {
                    // 这个是以split[0]开头
                    if (splitLen > 1) {
                        if (i + splitLen < dataLen) {
                            // 没有超出data的范围
                            for (int j = 1; j < splitLen; j++) {
                                if (DEFAULT_SPLIT[j] != encrypted[i + j]) {
                                    break;
                                }
                                if (j == splitLen - 1) {
                                    // 验证到split的最后一位，都没有break，则表明已经确认是split段
                                    isMatchSplit = true;
                                }
                            }
                        }
                    } else {
                        // split只有一位，则已经匹配了
                        isMatchSplit = true;
                    }
                }
                if (isMatchSplit) {
                    byte[] part = new byte[i - latestStartIndex];
                    System.arraycopy(encrypted, latestStartIndex, part, 0, part.length);
                    byte[] decryptPart = decryptByPublicKey(part, publicKey);
                    for (byte b : decryptPart) {
                        allBytes.add(b);
                    }
                    latestStartIndex = i + splitLen;
                    i = latestStartIndex - 1;
                }
            }
            byte[] bytes = new byte[allBytes.size()];
            {
                int i = 0;
                for (Byte b : allBytes) {
                    bytes[i++] = b.byteValue();
                }
            }
            return bytes;
        }

        /**
         * 私钥分段解密
         *
         * @param encrypted  待解密数据
         * @param privateKey 私钥
         */
        public static byte[] decryptWithPrivateKey(byte[] encrypted, PrivateKey privateKey) throws Exception {
            int splitLen = DEFAULT_SPLIT.length;
            if (splitLen <= 0) {
                return decryptByPrivateKey(encrypted, privateKey);
            }
            int dataLen = encrypted.length;
            List<Byte> allBytes = new ArrayList<Byte>(1024);
            int latestStartIndex = 0;
            for (int i = 0; i < dataLen; i++) {
                byte bt = encrypted[i];
                boolean isMatchSplit = false;
                if (i == dataLen - 1) {
                    // 到data的最后了
                    byte[] part = new byte[dataLen - latestStartIndex];
                    System.arraycopy(encrypted, latestStartIndex, part, 0, part.length);
                    byte[] decryptPart = decryptByPrivateKey(part, privateKey);
                    for (byte b : decryptPart) {
                        allBytes.add(b);
                    }
                    latestStartIndex = i + splitLen;
                    i = latestStartIndex - 1;
                } else if (bt == DEFAULT_SPLIT[0]) {
                    // 这个是以split[0]开头
                    if (splitLen > 1) {
                        if (i + splitLen < dataLen) {
                            // 没有超出data的范围
                            for (int j = 1; j < splitLen; j++) {
                                if (DEFAULT_SPLIT[j] != encrypted[i + j]) {
                                    break;
                                }
                                if (j == splitLen - 1) {
                                    // 验证到split的最后一位，都没有break，则表明已经确认是split段
                                    isMatchSplit = true;
                                }
                            }
                        }
                    } else {
                        // split只有一位，则已经匹配了
                        isMatchSplit = true;
                    }
                }
                if (isMatchSplit) {
                    byte[] part = new byte[i - latestStartIndex];
                    System.arraycopy(encrypted, latestStartIndex, part, 0, part.length);
                    byte[] decryptPart = decryptByPrivateKey(part, privateKey);
                    for (byte b : decryptPart) {
                        allBytes.add(b);
                    }
                    latestStartIndex = i + splitLen;
                    i = latestStartIndex - 1;
                }
            }
            byte[] bytes = new byte[allBytes.size()];
            {
                int i = 0;
                for (Byte b : allBytes) {
                    bytes[i++] = b.byteValue();
                }
            }
            return bytes;
        }

        /**
         * 用公钥对字符串进行加密
         *
         * @param data 原文
         */
        private static byte[] encryptByPublicKey(byte[] data, PublicKey publicKey) throws Exception {
            // 加密数据
            Cipher cp = Cipher.getInstance(ECB_PKCS1_PADDING);
            cp.init(Cipher.ENCRYPT_MODE, publicKey);
            return cp.doFinal(data);
        }

        /**
         * 私钥加密
         *
         * @param data       待加密数据
         * @param privateKey 密钥
         * @return byte[] 加密数据
         */
        private static byte[] encryptByPrivateKey(byte[] data, PrivateKey privateKey) throws Exception {
            // 数据加密
            Cipher cipher = Cipher.getInstance(ECB_PKCS1_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        }

        /**
         * 公钥解密
         *
         * @param data      待解密数据
         * @param publicKey 密钥
         * @return byte[] 解密数据
         */
        private static byte[] decryptByPublicKey(byte[] data, PublicKey publicKey) throws Exception {
            // 数据解密
            Cipher cipher = Cipher.getInstance(ECB_PKCS1_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        }

        /**
         * 使用私钥进行解密
         */
        private static byte[] decryptByPrivateKey(byte[] encrypted, PrivateKey privateKey) throws Exception {
            // 解密数据
            Cipher cp = Cipher.getInstance(ECB_PKCS1_PADDING);
            cp.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] arr = cp.doFinal(encrypted);
            return arr;
        }
    }


    /**
     * AES加解密工具。AES用来取代DES加密算法
     */
    public static class AESHelper {

        private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";//AES是加密方式 CBC是工作模式 PKCS5Padding是填充模式
        private static final String AES = "AES";//AES 加密
        private static final String SHA1PRNG = "SHA1PRNG";//// SHA1PRNG 强随机种子算法, 要区别4.2以上版本的调用方法


        /**
         * 生成随机数，可以当做动态的密钥 加密和解密的密钥必须一致，不然将不能解密
         *
         * @return
         */
        public static String generateKey() {
            try {
                SecureRandom localSecureRandom = SecureRandom.getInstance(SHA1PRNG);
                byte[] bytes_key = new byte[20];
                localSecureRandom.nextBytes(bytes_key);
                String str_key = Hex.encode(bytes_key);
                return str_key;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        // 对密钥进行处理
        private static byte[] getRawKey(byte[] seed) throws Exception {
            KeyGenerator kgen = KeyGenerator.getInstance(AES);
            //for android
            SecureRandom sr = null;
            // 在4.2以上版本中，SecureRandom获取方式发生了改变
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                sr = SecureRandom.getInstance(SHA1PRNG, "Crypto");
            } else {
                sr = SecureRandom.getInstance(SHA1PRNG);
            }
            // for Java
            // secureRandom = SecureRandom.getInstance(SHA1PRNG);
            sr.setSeed(seed);
            kgen.init(128, sr); //256 bits or 128 bits,192bits
            //AES中128位密钥版本有10个加密循环，192比特密钥版本有12个加密循环，256比特密钥版本则有14个加密循环。
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            return raw;
        }


        /**
         * 加密
         *
         * @param key  密钥
         * @param data 数据
         * @return
         */
        public static String encrypt(String key, String data) {
            if (data == null || data.equals("")) {
                return data;
            }
            try {
                byte[] result = encrypt(key, data.getBytes("UTF-8"));
                return Base64.encode(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static byte[] encrypt(String key, byte[] clear) throws Exception {
            byte[] raw = getRawKey(key.getBytes());
            SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
            byte[] encrypted = cipher.doFinal(clear);
            return encrypted;
        }

        /**
         * 解密
         *
         * @param key       密钥
         * @param encrypted 加密后的数据
         * @return
         */
        public static String decrypt(String key, String encrypted) {
            if (encrypted == null || encrypted.equals("")) {
                return encrypted;
            }
            try {
                byte[] enc = Base64.decode(encrypted);
                byte[] result = decrypt(key, enc);
                return new String(result, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static byte[] decrypt(String key, byte[] encrypted) throws Exception {
            byte[] raw = getRawKey(key.getBytes());
            SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
            byte[] decrypted = cipher.doFinal(encrypted);
            return decrypted;
        }


        /**
         * 对文件进行AES加密
         *
         * @param sourceFile
         * @param encrypFile
         * @param key
         */
        public static void encryptFile(File sourceFile, File encrypFile, String key) {
            //新建临时加密文件
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = new FileInputStream(sourceFile);
                outputStream = new FileOutputStream(encrypFile);
                byte[] raw = getRawKey(key.getBytes());
                SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
                Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));

                //以加密流写入文件
                CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
                byte[] cache = new byte[1024];
                int nRead = 0;
                while ((nRead = cipherInputStream.read(cache)) != -1) {
                    outputStream.write(cache, 0, nRead);
                    outputStream.flush();
                }
                cipherInputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        /**
         * AES方式解密文件
         *
         * @param encryptedFile
         * @param outFile
         * @param key
         */
        public static void decryptFile(File encryptedFile, File outFile, String key) {
            //  File decryptFile = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                //decryptFile = File.createTempFile(encryptedFile.getName(),fileType);

                inputStream = new FileInputStream(encryptedFile);
                outputStream = new FileOutputStream(outFile);

                byte[] raw = getRawKey(key.getBytes());
                SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
                Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));

                CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
                byte[] buffer = new byte[1024];
                int r;
                while ((r = inputStream.read(buffer)) >= 0) {
                    cipherOutputStream.write(buffer, 0, r);
                }
                cipherOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }


    /**
     * 消息摘要工具类，支持MD5和SHA系列算法
     */
    public static class MessageDigestHelper {

        public static final String ALGORITHM_MD5 = "MD5";
        public static final String ALGORITHM_SHA1 = "SHA-1";
        public static final String ALGORITHM_SHA256 = "SHA-256";
        public static final String ALGORITHM_SHA384 = "SHA-384";
        public static final String ALGORITHM_SHA512 = "SHA-512";

        /**
         * 对字符串取MD5值
         *
         * @param data 输入字符串
         * @return
         */
        public static String getMD5(String data) {
            return getMD(data, ALGORITHM_MD5);
        }

        /**
         * 取文件MD5值
         *
         * @param file
         * @return
         */
        public static String getMD5(File file) {
            return getMD(file, ALGORITHM_MD5);
        }

        /**
         * 对字符串取SHA-1值
         *
         * @param data 输入字符串
         * @return
         */
        public static String getSHA1(String data) {
            return getMD(data, ALGORITHM_SHA1);
        }


        /**
         * 取文件SHA1值
         *
         * @param file
         * @return
         */
        public static String getSHA1(File file) {
            return getMD(file, ALGORITHM_SHA1);
        }


        /**
         * 按输入算法名取摘要签名
         *
         * @param data      输入字符串
         * @param alogrithm 摘要算法名 MD5	,SHA-1,SHA-256,SHA-384,SHA-512
         * @return
         */
        public static String getMD(String data, String alogrithm) {
            try {
                MessageDigest md = MessageDigest.getInstance(alogrithm);
                md.update(data.getBytes("utf-8"));
                byte temp[] = md.digest();
                return Hex.encode(temp);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 按输入算法名取摘要签名
         *
         * @param file
         * @param alogrithm
         * @return
         */
        public static String getMD(File file, String alogrithm) {
            FileInputStream fis = null;
            MessageDigest md;
            try {
                md = MessageDigest.getInstance(alogrithm);
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

    }


    /**
     * HEX字符串与字节码转换工具
     *
     * @author steven-pan
     */
    private static class Hex {

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
    }


    /**
     * 异或加解密码算法
     */
    public static class XORHelper {

        public static byte[] encrypt(byte[] bytes, int key) {
            if (bytes == null) {
                return null;
            }
            int len = bytes.length;
            //     int key = 0x12;
            for (int i = 0; i < len; i++) {
                bytes[i] = (byte) (bytes[i] ^ key);
                key = bytes[i];
            }
            return bytes;
        }

        public static byte[] decrypt(byte[] bytes, int key) {
            if (bytes == null) {
                return null;
            }
            int len = bytes.length;
            //      int key = 0x12;
            for (int i = len - 1; i > 0; i--) {
                bytes[i] = (byte) (bytes[i] ^ bytes[i - 1]);
            }
            bytes[0] = (byte) (bytes[0] ^ key);
            return bytes;
        }
    }


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        final String source = "夺顶替顶替夺顶替顶替顶替夺顶替夺顶替顶替夺顶替顶替夺顶替顶替夺顶替顶替";// 要加密的字符串


        {
            System.out.println("RSA加解密测试：");

            RSAHelper.setKeySize(1024);
            //   KeyPair keyPair = RSAHelper.generateKeyPair("pub", "pri");

            PublicKey publicKey = RSAHelper.loadPublicKeyForFile("pub");
            PrivateKey privateKey = RSAHelper.loadPrivateKeyForFile("pri");

            System.out.println("加密前:" + source);

            byte[] cryptograph = RSAHelper.encryptWithPublicKey(source.getBytes(), publicKey);// 生成的密文
            System.out.println("加密结果:" + Base64.encode(cryptograph));

            String sign = RSAHelper.sign(cryptograph, privateKey);
            System.out.println("签名值:" + new String(sign));
            System.out.println("签名验证结果:" + RSAHelper.verify(cryptograph, publicKey, sign));

            byte[] target = RSAHelper.decryptWithPrivateKey(cryptograph, privateKey);// 解密密文
            System.out.println("解密后:" + new String(target));
        }

        System.out.println("====================================");

        {
            //生成一个动态key
            String secretKey = CryptoUtils.AESHelper.generateKey();
            System.out.println("AES动态secretKey ---->" + secretKey);

            //AES加密
            long start = System.currentTimeMillis();
            String encryStr = CryptoUtils.AESHelper.encrypt(secretKey, source);
            long end = System.currentTimeMillis();
            System.out.println("AES加密耗时---->" + (end - start));
            System.out.println("AES加密后数据 ---->" + encryStr);
            System.out.println("AES加密后数据长度 ---->" + encryStr.length());

            //AES解密
            start = System.currentTimeMillis();
            String decryStr = CryptoUtils.AESHelper.decrypt(secretKey, encryStr);
            end = System.currentTimeMillis();
            System.out.println("AES解密耗时---->" + (end - start));
            System.out.println("AES解密后数据 ---->" + decryStr);


            File sourceFile = new File("学校格式.txt");
            File outFile1 = new File("en");
            File outFile2 = new File("de");
            CryptoUtils.AESHelper.encryptFile(sourceFile, outFile1, secretKey);
            CryptoUtils.AESHelper.decryptFile(outFile1, outFile2, secretKey);


        }

        {
            System.out.println("摘要测试：");
            System.out.println(MessageDigestHelper.getMD5("abcv"));
            System.out.println(MessageDigestHelper.getSHA1("abcv"));

            File sourceFile = new File("学校格式.txt");

            System.out.println(MessageDigestHelper.getMD5(sourceFile));
            System.out.println(MessageDigestHelper.getSHA1(sourceFile));
            System.out.println(MessageDigestHelper.getMD(sourceFile, MessageDigestHelper.ALGORITHM_SHA384));
        }

        {
            byte[] bytes = XORHelper.encrypt("whoislcj".getBytes(), 10);//加密
            String str1 = new String(XORHelper.decrypt(bytes, 10));//解密
            System.out.println(str1);
        }
    }
}
