package vip.youwe.sheller.utils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.NullCipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtils {

    static byte[] RSA_OAEPPaddingPublicKeyEncrpt(byte[] data, PublicKey publicKey) {
        if (data == null || publicKey == null) {
            return new byte[0];
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding");

            cipher.init(1, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {

            e.printStackTrace();

            return new byte[0];
        }
    }


    public static final String TAG = "CipherUtils";


    static byte[] RSA_OAEPPaddingPrivateKeyDecrpt(byte[] data, PrivateKey privateKey) {
        if (data == null || privateKey == null) {
            return new byte[0];
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding");

            cipher.init(2, privateKey);
            return cipher.doFinal(data);
        } catch (Exception e) {

            e.printStackTrace();

            return new byte[0];
        }
    }


    static PublicKey generatePublicKey(BigInteger modulus, BigInteger publicExponent) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
        } catch (Exception e) {
            e.printStackTrace();


            return null;
        }
    }


    static PrivateKey generatePrivateKey(BigInteger modulus, BigInteger publicExponent) {
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, publicExponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }


    static byte[] AES_CBC_PKCS5PaddingDecrypt(byte[] data, byte[] key, byte[] IV) {
        if (key == null || key.length == 0) {
            return new byte[0];
        }
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, skeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();


            return new byte[0];
        }
    }


    static byte[] AES_CBC_PKCS5PaddingEncrypt(byte[] data, byte[] key, byte[] IV) {
        if (key == null || key.length == 0) {
            return new byte[0];
        }

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, skeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        } catch (NoSuchPaddingException noSuchPaddingException) {
        } catch (InvalidKeyException invalidKeyException) {
        } catch (InvalidAlgorithmParameterException invalidAlgorithmParameterException) {
        } catch (IllegalBlockSizeException illegalBlockSizeException) {
        } catch (BadPaddingException badPaddingException) {
        }

        return new byte[0];
    }

    static Cipher generateAES_CFB_NoPaddingEncryptCipher(byte[] key, byte[] IV) {

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
            cipher.init(1, skeySpec, ivParameterSpec);
            return cipher;
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        } catch (NoSuchPaddingException noSuchPaddingException) {
        } catch (InvalidKeyException invalidKeyException) {
        } catch (InvalidAlgorithmParameterException invalidAlgorithmParameterException) {
        }

        return new NullCipher();
    }

    static Cipher generateAES_CFB_NoPaddingDecryptCipher(byte[] key, byte[] IV) {

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
            cipher.init(2, skeySpec, ivParameterSpec);
            return cipher;
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        } catch (NoSuchPaddingException noSuchPaddingException) {
        } catch (InvalidKeyException invalidKeyException) {
        } catch (InvalidAlgorithmParameterException invalidAlgorithmParameterException) {
        }

        return new NullCipher();
    }


    static byte[] hmacSha256(byte[] data, byte[] key) {
        if (key == null || key.length == 0) {
            return new byte[0];
        }

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance(secretKeySpec.getAlgorithm());
            mac.init(secretKeySpec);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        } catch (InvalidKeyException invalidKeyException) {
        }


        return new byte[0];
    }

    public static String bytesToHexStr(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        String hexStr = "0123456789ABCDEF";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            builder.append(hexStr.charAt((data[i] & 0xF0) >>> 4));
            builder.append(hexStr.charAt(data[i] & 0xF));
        }
        return builder.toString();
    }

    public static byte[] hexStrToBytes(String hexStr) {
        byte[] result = null;

        try {
            if (hexStr == null || hexStr.length() == 0) {
                return new byte[0];
            }

            char[] hexChars = hexStr.toCharArray();
            if ((hexChars.length & 1) != 0) {
                throw new DecodeHexStrException("hexStr is Odd number");
            }
            result = new byte[hexChars.length / 2];
            for (int i = 0, j = 0; i < hexChars.length; i++, j++) {
                int h = Character.digit(hexChars[i], 16);
                int l = Character.digit(hexChars[++i], 16);
                if (h == -1 || l == -1) {
                    throw new DecodeHexStrException("Illegal hexStr");
                }
                result[j] = (byte) (h << 4 | l);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return result;
    }


    static byte[] intToByte(int i) {
        byte[] result = new byte[4];
        result[3] = (byte) (i >>> 24 & 0xFF);
        result[2] = (byte) (i >>> 16 & 0xFF);
        result[1] = (byte) (i >>> 8 & 0xFF);
        result[0] = (byte) (i >>> 0 & 0xFF);
        return result;
    }

    public static byte[] mergeByteArray(byte[]... byteArray) {
        int totalLength = 0;
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] != null) {

                totalLength += byteArray[i].length;
            }
        }
        byte[] result = new byte[totalLength];
        int cur = 0;
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] != null) {


                System.arraycopy(byteArray[i], 0, result, cur, byteArray[i].length);
                cur += byteArray[i].length;
            }
        }
        return result;
    }


    static class DecodeHexStrException
            extends Exception {
        private static final long serialVersionUID = 938776570614030665L;

        DecodeHexStrException(String string) {
            super(string);
        }
    }


    public static String sha256Hex(InputStream is) {
        byte[] buffer = new byte[1024];

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int read;
            while ((read = is.read(buffer)) > -1) {
                digest.update(buffer, 0, read);
            }
            byte[] result = digest.digest();
            return bytesToHexStr(result);
        } catch (IOException iOException) {
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        }

        return "";
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data);
            byte[] result = digest.digest();
            return bytesToHexStr(result);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {

            return "";
        }
    }

    public static byte[] bytesXor(byte[] b1, byte[] b2) {
        byte[] shortbytes, longbytes;
        if (b1.length >= b2.length) {
            longbytes = b1;
            shortbytes = b2;
        } else {
            longbytes = b2;
            shortbytes = b1;
        }
        byte[] xorstr = new byte[longbytes.length];
        int i = 0;
        for (; i < shortbytes.length; i++) {
            xorstr[i] = (byte) (shortbytes[i] ^ longbytes[i]);
        }
        for (; i < longbytes.length; i++) {
            xorstr[i] = longbytes[i];
        }
        return xorstr;
    }
}