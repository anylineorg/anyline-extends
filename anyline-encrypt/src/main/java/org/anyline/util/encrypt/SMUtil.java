/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.anyline.util.encrypt;

import org.anyline.util.NumberUtil;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 如果外部提供了公钥一般直接调用内部类SM2的静态方法加密解密<br/>
 * SMUtil.SM2.encrypt(publicKey, data);<br/>
 *<br/>
 * 如果外部提供了密钥对或者需要频繁加密解密一般先构造一个SM2<br/>
 * SM2 sm2 = SMUtil.sm2(publicKey, privateKey);<br/>
 * 等同于 new SM2(publicKey, privateKey);<br/>
 *<br/>
 * 如果需要生成密钥对<br/>
 * SM2 sm2 = SMUtil.sm2();<br/>
 * sm2.encrypt(data);   //因为sm2本身有公钥 方法中就不需要提供了<br/>
 * sm2.decrypt(data);<br/>
 * <br/>
 * 注意1:<br/>
 * 密钥是可以是byte[] 也可以是hex格式的String<br/>
 * 输入参数是byte[]或String<br/>
 * 如果要输入hex的String格式 先转换成byte[]<br/>
 * <br/>
 * 注意2:<br/>
 * 公钥和密文中有04前缀 根据情况去留 补上02的钥就是65位了
 * 注意3:<br/>
 * 解密后的返回结果是hex格式的String如果有需要可以通过NumberUtil.hex2string转换<br/>
 * <br/>
 * 返回结果一般与输入参数对应,输入bytes[]也返回bytes 输入hex也返回hex<br/>
 * string byte hex之间转换可以调用NumberUtil.hex2byte,byte2hex等<br/>
 */
public class SMUtil {
    /**
     * 获取sm2密钥对
     * BC库使用的公钥=64个字节+1个字节（04标志位）,BC库使用的私钥=32个字节
     * SM2秘钥的组成部分有 私钥D 、公钥X 、 公钥Y , 他们都可以用长度为64的16进制的HEX串表示,
     * <br/>SM2公钥并不是直接由X+Y表示 , 而是额外添加了一个头,当启用压缩时:公钥=有头+公钥X ,即省略了公钥Y的部分
     *
     * @param compress 是否压缩公钥（加密解密都使用BC库才能使用压缩）
     * @return SM2
     */
    public static SM2 sm2(boolean compress) {
        //获取一条SM2曲线参数
        X9ECParameters params = GMNamedCurves.getByName(SM2.CRYPTO_NAME);
        //构造domain参数
        ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());
        //1.创建密钥生成器
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        //2.初始化生成器,带上随机数
        try {
            keyPairGenerator.init(new ECKeyGenerationParameters(domainParameters, SecureRandom.getInstance(SM2.ALGORITHM)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //3.生成密钥对
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = keyPairGenerator.generateKeyPair();
        ECPublicKeyParameters publicKeyParameters = (ECPublicKeyParameters) asymmetricCipherKeyPair.getPublic();
        ECPoint ecPoint = publicKeyParameters.getQ();
        // 把公钥放入map中,默认压缩公钥
        // 公钥前面的02或者03表示是压缩公钥,04表示未压缩公钥,04的时候,可以去掉前面的04
        String publicKey = NumberUtil.byte2hex(ecPoint.getEncoded(compress));
        ECPrivateKeyParameters privateKeyParameters = (ECPrivateKeyParameters) asymmetricCipherKeyPair.getPrivate();
        BigInteger intPrivateKey = privateKeyParameters.getD();
        // 把私钥放入map中
        String privateKey = intPrivateKey.toString(16);
        return new SM2(publicKey, privateKey);
    }

    public static SM2 sm2() {
        return sm2(false);
    }

    /**
     * 提供公钥或私钥
     * @param publicKey 私钥
     * @param privateKey 公钥
     * @return SM2
     */
    public static SM2 sm2(String publicKey, String privateKey) {
        return new SM2(publicKey, privateKey);
    }
    public static SM2 sm2(byte[] publicKey, byte[] privateKey) {
        return new SM2(publicKey, privateKey);
    }

    public static SM2 sm2(String publicKey) {
        return new SM2(publicKey, null);
    }
    public static SM2 sm2(byte[] publicKey) {
        return new SM2(publicKey, null);
    }


    public static class SM2 {
        /**
         * sm2曲线参数名称
         */
        public static final String CRYPTO_NAME = "sm2p256v1";
        public static final String ALGORITHM = "SHA1PRNG";
        public static final String DEFAULT_ID = "1234567812345678";

        /**
         * 公钥
         */
        private String publicKey;
        /**
         * 私钥
         */
        private String privateKey;

        public SM2() {

        }

        public SM2(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
        public SM2(byte[] publicKey, byte[] privateKey) {
            this.publicKey = NumberUtil.byte2hex(publicKey);
            this.privateKey = NumberUtil.byte2hex(privateKey);
        }

        public SM2(String publicKey) {
            this.publicKey = publicKey;
        }
        public SM2(byte[] publicKey) {
            this.publicKey = NumberUtil.byte2hex(publicKey);
        }
        public String getPublicKey() {
            return publicKey;
        }
        public byte[] getPublicBytes(){
            return NumberUtil.hex2bytes(publicKey);
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
        public void setPublicKey(byte[] publicKey) {
            this.publicKey = NumberUtil.byte2hex(publicKey);
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public byte[] getPrivateBytes() {
            return NumberUtil.hex2bytes(privateKey);
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
        public void setPrivateKey(byte[] privateKey) {
            this.privateKey = NumberUtil.byte2hex(privateKey);
        }


        /**
         * 加密<br/>
         * 务必注意返回的是16进制
         * @param publicKey 公钥 hex格式的String
         * @param data      待加密的数据 如果是hex格式要先转成byte[]
         * @return 密文,BC库产生的密文带由04标识符,与非BC库对接时需要去掉开头的04
         */
        public static String encrypt(String publicKey, String data) {
            return encrypt(publicKey, data, SM2Engine.CIPHER_MODE_CN);
        }
        public static String encrypt(byte[] publicKey, String data) {
            return encrypt(publicKey, data, SM2Engine.CIPHER_MODE_CN);
        }

        public String encrypt(String data) {
            return encrypt(publicKey, data);
        }

        public static byte[] encrypt(String publicKey, byte[] data) {
            return encrypt(publicKey, data, SM2Engine.CIPHER_MODE_CN);
        }
        public static byte[] encrypt(byte[] publicKey, byte[] data) {
            return encrypt(publicKey, data, SM2Engine.CIPHER_MODE_CN);
        }

        public byte[] encrypt(byte[] data) {
            // 按国密排序标准加密
            return encrypt(publicKey, data);
        }

        /**
         * 加密<br/>
         * 务必注意返回的是16进制
         * @param publicKey 公钥
         * @param bytes     待加密的数据
         * @param mode      密文排列方式0-C1C2C3；1-C1C3C2；
         * @return 密文,BC库产生的密文带由04标识符,与非BC库对接时需要去掉开头的04
         */
        public static byte[] encrypt(byte[] publicKey, byte[] bytes, int mode) {
            // 获取一条SM2曲线参数
            X9ECParameters params = GMNamedCurves.getByName(CRYPTO_NAME);
            // 构造ECC算法参数,曲线方程、椭圆曲线G点、大整数N
            ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());
            //提取公钥点
            ECPoint pukPoint = params.getCurve().decodePoint(publicKey);
            // 公钥前面的02或者03表示是压缩公钥,04表示未压缩公钥, 04的时候,可以去掉前面的04
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);

            SM2Engine sm2Engine = new SM2Engine();
            // 设置sm2为加密模式
            sm2Engine.init(true, mode, new ParametersWithRandom(publicKeyParameters, new SecureRandom()));

            byte[] result = null;
            try {
                //byte[] in = data.getBytes();
                result = sm2Engine.processBlock(bytes, 0, bytes.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
        public static byte[] encrypt(String publicKey, byte[] bytes, int mode) {
            return encrypt(NumberUtil.hex2bytes(publicKey), bytes, mode);
        }

        public byte[] encrypt(byte[] bytes, int mode) {
            return encrypt(publicKey, bytes, mode);
        }

        public static String encrypt(String publicKey, String data, int mode) {
            byte[] bytes = encrypt(publicKey, data.getBytes(), mode);
            return NumberUtil.byte2hex(bytes);
        }
        public static String encrypt(byte[] publicKey, String data, int mode) {
            byte[] bytes = encrypt(publicKey, data.getBytes(), mode);
            return NumberUtil.byte2hex(bytes);
        }

        public String encrypt(String data, int mode) {
            return encrypt(publicKey, data, mode);
        }



        public static String encryptHex(String publicKey, String hex, int mode) {
            byte[] bytes = encrypt(publicKey, NumberUtil.hex2bytes(hex), mode);
            return NumberUtil.byte2hex(bytes);
        }
        public static String encryptHex(byte[] publicKey, String hex, int mode) {
            byte[] bytes = encrypt(publicKey, NumberUtil.hex2bytes(hex), mode);
            return NumberUtil.byte2hex(bytes);
        }

        public String encryptHex(String hex, int mode) {
            byte[] bytes = encrypt(NumberUtil.hex2bytes(hex), mode);
            return NumberUtil.byte2hex(bytes);
        }

        /**
         * 解密<br/>
         * 务必注意返回的是16进制
         * @param privateKey 私钥
         * @param data       密文数据(16进制string 不区分大小写,可以空格分隔,没有也可)
         *    047c7876a0412479d9a59717b59624fbf43a39....
         *    04 7C 78 76 A0 41 ....
         * @return hex
         */
        public static String decrypt(String privateKey, String data) {
            //按国密排序标准解密
            byte[] bytes = decrypt(privateKey, data, SM2Engine.CIPHER_MODE_CN);
            return NumberUtil.byte2hex(bytes);
        }

        public static String decrypt(byte[] privateKey, String data) {
            //按国密排序标准解密
            byte[] bytes = decrypt(privateKey, data, SM2Engine.CIPHER_MODE_CN);
            return NumberUtil.byte2hex(bytes);
        }

        public String decrypt(String data) {
            return decrypt(privateKey, data);
        }

        /**
         * 解密<br/>
         * 务必注意返回的是16进制
         * @param privateKey 私钥(16进制string 可以空格分隔,没有也可)
         * @param data 密文数据(16进制string 可以空格分隔,没有也可)
         * @param mode 密文排列方式0-C1C2C3；1-C1C3C2；
         * @return 解密后bytes
         */
        public static byte[] decrypt(String privateKey, String data, int mode) {
            // 使用BC库加解密时密文以04开头,传入的密文前面没有04则补上
            if (!data.startsWith("04")) {
                data = "04" + data;
            }
            if(privateKey.contains(" ")){
                privateKey = privateKey.replace(" ", "");
            }
            byte[] cipherDataByte = Hex.decode(data);

            //获取一条SM2曲线参数
            X9ECParameters params = GMNamedCurves.getByName(CRYPTO_NAME);
            //构造domain参数
            ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());

            BigInteger privateKeyD = new BigInteger(privateKey, 16);
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);

            SM2Engine sm2Engine = new SM2Engine();
            // 设置sm2为解密模式
            sm2Engine.init(false, mode, privateKeyParameters);

            try {
                byte[] arrayOfBytes = sm2Engine.processBlock(cipherDataByte, 0, cipherDataByte.length);
                return arrayOfBytes;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        public static byte[] decrypt(String privateKey, byte[] data, int mode) {
            return decrypt(privateKey, NumberUtil.byte2hex(data), mode);
        }

        public static byte[] decrypt(byte[] privateKey, String data, int mode) {
            return decrypt(NumberUtil.byte2hex(privateKey), data, mode);
        }
        public static byte[] decrypt(byte[] privateKey, byte[] data, int mode) {
            return decrypt(NumberUtil.byte2hex(privateKey), data, mode);
        }
        public byte[] decrypt(String data, int mode) {
            return decrypt(privateKey, data, mode);
        }
        public byte[] decrypt(byte[] data, int mode) {
            return decrypt(privateKey, data, mode);
        }


        /**
         * 私钥签名 1.64 模拟 1.57
         * @param content       待签名内容
         * @return hex
         */
        public String sign(String content)   {
            //待签名内容转为字节数组
            byte[] message = content.getBytes();
            //获取一条SM2曲线参数
            X9ECParameters params = GMNamedCurves.getByName(CRYPTO_NAME);
            //构造domain参数
            ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());
            BigInteger privateKeyD = new BigInteger(privateKey, 16);
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);
            //创建签名实例
            SM2Signer signer = new SM2Signer();
            //初始化签名实例,带上ID,国密的要求,ID默认值:1234567812345678
            try {
                signer.init(true, new ParametersWithID(new ParametersWithRandom(privateKeyParameters, SecureRandom.getInstance(ALGORITHM)), Strings.toByteArray(DEFAULT_ID)));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            signer.update(message, 0, message.length);
            //生成签名,签名分为两部分r和s,分别对应索引0和1的数组
            byte[] signBytes = new byte[0];
            try {
                signBytes = signer.generateSignature();
            } catch (CryptoException e) {
                throw new RuntimeException(e);
            }
            //bc1.57版本中，signData是纯r+s字符串拼接，如果为了兼容低版本的bc包，则需要加这一句
            byte[] bytes = decodeDERSM2Sign(domainParameters, signBytes);
            return Hex.toHexString(bytes);
        }

        /**
         * 私钥签名 DER
         * @param content       待签名内容
         * @return hex
         */
        public String sign4Der(String content) throws CryptoException {
            byte[] message = content.getBytes();
            //获取一条SM2曲线参数
            X9ECParameters params = GMNamedCurves.getByName(CRYPTO_NAME);
            //构造domain参数
            ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());
            BigInteger privateKeyD = new BigInteger(privateKey, 16);
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);
            //创建签名实例
            SM2Signer signer = new SM2Signer();
            //初始化签名实例,带上ID,国密的要求,ID默认值:1234567812345678
            try {
                signer.init(true, new ParametersWithID(new ParametersWithRandom(privateKeyParameters, SecureRandom.getInstance(ALGORITHM)), Strings.toByteArray(DEFAULT_ID)));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            signer.update(message, 0, message.length);
            //生成签名,签名分为两部分r和s,分别对应索引0和1的数组
            byte[] bytes = signer.generateSignature();
            return Hex.toHexString(bytes);
        }
        /**
         * 验证签名
         * @param content       待签名内容
         * @param sign          签名值
         * @return
         */
        public boolean verify(String content, String sign)  {
            byte[] message = Hex.decode(Hex.toHexString(content.getBytes()));
            byte[] signData = Hex.decode(sign);
            try {
                signData = encodeSM2SignToDER(signData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 获取一条SM2曲线参数
            X9ECParameters params = GMNamedCurves.getByName(CRYPTO_NAME);
            // 构造domain参数
            ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());
            //提取公钥点
            ECPoint pukPoint = params.getCurve().decodePoint(Hex.decode(publicKey));
            // 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);
            SM2Signer signer = new SM2Signer();
            ParametersWithID parametersWithID = new ParametersWithID(publicKeyParameters, Strings.toByteArray(DEFAULT_ID));
            signer.init(false, parametersWithID);
            signer.update(message, 0, message.length);
            return signer.verifySignature(signData);
        }
        /**
         * 验证签名
         * @param content       待签名内容
         * @param sign          签名值
         * @return boolean
         */
        public boolean verify4Der(String content, String sign)  {
            byte[] message = Hex.decode(content);
            byte[] signData = Hex.decode(sign);
            // 获取一条SM2曲线参数
            X9ECParameters params = GMNamedCurves.getByName(CRYPTO_NAME);
            // 构造domain参数
            ECDomainParameters domainParameters = new ECDomainParameters(params.getCurve(), params.getG(), params.getN());
            //提取公钥点
            ECPoint pukPoint = params.getCurve().decodePoint(Hex.decode(publicKey));
            // 公钥前面的02或者03表示是压缩公钥，04表示未压缩公钥, 04的时候，可以去掉前面的04
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);
            SM2Signer signer = new SM2Signer();
            ParametersWithID parametersWithID = new ParametersWithID(publicKeyParameters, Strings.toByteArray(DEFAULT_ID));
            signer.init(false, parametersWithID);
            signer.update(message, 0, message.length);
            return signer.verifySignature(signData);
        }

        /**
         * 把64字节的纯R+S字节流转换成DER编码字节流
         * @param rawSign rawSign
         * @return bytes
         * @throws IOException 异常
         */
        public static byte[] encodeSM2SignToDER(byte[] rawSign) throws IOException {
            //要保证大数是正数
            BigInteger r = new BigInteger(1, extractBytes(rawSign, 0, 32));
            BigInteger s = new BigInteger(1, extractBytes(rawSign, 32, 32));
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1Integer(r));
            v.add(new ASN1Integer(s));
            return new DERSequence(v).getEncoded(ASN1Encoding.DER);
        }
        private static byte[] extractBytes(byte[] src, int offset, int length) {
            byte[] result = new byte[length];
            System.arraycopy(src, offset, result, 0, result.length);
            return result;
        }


        public static byte[] decodeDERSM2Sign(ECDomainParameters domainParams, byte[] derSign) {
            ASN1Sequence as = DERSequence.getInstance(derSign);
            byte[] rBytes = ((ASN1Integer) as.getObjectAt(0)).getValue().toByteArray();
            byte[] sBytes = ((ASN1Integer) as.getObjectAt(1)).getValue().toByteArray();
            //由于大数的补0规则，所以可能会出现33个字节的情况，要修正回32个字节
            rBytes = fixToCurveLengthBytes(domainParams, rBytes);
            sBytes = fixToCurveLengthBytes(domainParams, sBytes);
            byte[] rawSign = new byte[rBytes.length + sBytes.length];
            System.arraycopy(rBytes, 0, rawSign, 0, rBytes.length);
            System.arraycopy(sBytes, 0, rawSign, rBytes.length, sBytes.length);
            return rawSign;
        }

        private static byte[] fixToCurveLengthBytes(ECDomainParameters domainParams, byte[] src) {
            int curveLen = getCurveLength(domainParams);
            if (src.length == curveLen) {
                return src;
            }
            byte[] result = new byte[curveLen];
            if (src.length > curveLen) {
                System.arraycopy(src, src.length - result.length, result, 0, result.length);
            } else {
                System.arraycopy(src, 0, result, result.length - src.length, src.length);
            }
            return result;
        }

        public static int getCurveLength(ECDomainParameters domainParams) {
            return (domainParams.getCurve().getFieldSize() + 7) / 8;
        }

    }
}
