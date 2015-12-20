package com.esec.u2ftoken;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.AppletEvent;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacardx.crypto.Cipher;

public class U2FToken extends Applet {
	
	public static final byte CLA_7816 = 0x00;
	
	public static final byte INS_TEST_ENCRYPT = 0x10;
	public static final byte INS_TEST_DECRYPT = 0x20;
	public static final byte INS_U2F_REGISTER = 0x01; // Registration command
	public static final byte INS_U2F_AUTHENTICATE = 0x02; // Authenticate/sign command
	public static final byte INS_U2F_VERSION = 0x03; //Read version string command
	public static final byte INS_U2F_CHECK_REGISTER = 0x04; // Registration command that incorporates checking key handles
	public static final byte INS_U2F_AUTHENTICATE_BATCH = 0x05; // Authenticate/sign command for a batch of key handles
	
	/**
	 * 存储attestation证书的二进制文件。FID是EF01
	 */
	public BinaryEF attestationCertFile;
	
	/**
	 * 版本号："U2F_V2"
	 */
	private static final byte[] version = {(byte)0x55, (byte)0x32, (byte)0x46, (byte)0x5F, (byte)0x56, (byte)0x32};
	/**
	 * attestation证书的字节数组。
	 * 对应项目中 doc/cert.der
	 */
	private static final byte[] attestationCert = {0x30, (byte)0x82, 0x01, 0x15, 0x30, (byte)0x81, (byte)0xbc, 0x02, 0x09, 0x00, (byte)0xc5, (byte)0xf4, (byte)0xee, 0x4c, 0x59, 0x50, 0x3e, 0x05, 0x30, 0x0a, 0x06, 0x08, 0x2a, (byte)0x86, 0x48, (byte)0xce, 0x3d, 0x04, 0x03, 0x02, 0x30, 0x13, 0x31, 0x11, 0x30, 0x0f, 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x08, 0x59, 0x61, 0x6e, 0x67, 0x5a, 0x68, 0x6f, 0x75, 0x30, 0x1e, 0x17, 0x0d, 0x31, 0x35, 0x31, 0x32, 0x30, 0x39, 0x30, 0x37, 0x30, 0x34, 0x35, 0x38, 0x5a, 0x17, 0x0d, 0x31, 0x36, 0x31, 0x32, 0x30, 0x38, 0x30, 0x37, 0x30, 0x34, 0x35, 0x38, 0x5a, 0x30, 0x13, 0x31, 0x11, 0x30, 0x0f, 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x08, 0x59, 0x61, 0x6e, 0x67, 0x5a, 0x68, 0x6f, 0x75, 0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, (byte)0x86, 0x48, (byte)0xce, 0x3d, 0x02, 0x01, 0x06, 0x08, 0x2a, (byte)0x86, 0x48, (byte)0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00, 0x04, 0x72, (byte)0x9a, 0x71, (byte)0xd0, (byte)0x81, 0x62, 0x42, (byte)0x84, (byte)0x92, (byte)0xf2, (byte)0xd9, 0x61, (byte)0x92, 0x4d, 0x37, 0x44, 0x3a, 0x4f, 0x1b, (byte)0xda, 0x58, 0x0f, (byte)0x8a, (byte)0xea, 0x29, 0x20, (byte)0xd2, (byte)0x99, 0x7c, (byte)0xbe, (byte)0xa4, 0x39, 0x60, (byte)0xce, 0x72, (byte)0x9e, 0x35, (byte)0xc1, (byte)0xf7, 0x40, (byte)0x92, (byte)0xf2, 0x25, 0x0e, 0x60, 0x74, (byte)0x82, 0x3f, (byte)0xc5, 0x7f, 0x33, 0x60, (byte)0xb7, (byte)0xcd, 0x39, 0x69, (byte)0xc3, (byte)0xc3, 0x12, 0x5e, (byte)0xce, 0x26, 0x5c, 0x29, 0x30, 0x0a, 0x06, 0x08, 0x2a, (byte)0x86, 0x48, (byte)0xce, 0x3d, 0x04, 0x03, 0x02, 0x03, 0x48, 0x00, 0x30, 0x45, 0x02, 0x21, 0x00, (byte)0xe7, 0x67, (byte)0xfa, (byte)0x94, 0x10, 0x35, (byte)0xd5, (byte)0x85, 0x3d, 0x52, (byte)0xd8, 0x7d, 0x67, 0x14, 0x70, (byte)0xbc, 0x76, 0x3b, (byte)0xc5, (byte)0xb1, 0x2e, 0x1d, 0x45, 0x77, (byte)0xea, (byte)0x9f, (byte)0x8c, (byte)0xa6, 0x74, (byte)0xe5, (byte)0x9d, 0x39, 0x02, 0x20, 0x3f, (byte)0xe1, 0x1c, (byte)0xad, 0x59, (byte)0xf5, 0x35, 0x76, 0x00, 0x1f, 0x15, (byte)0xee, 0x05, (byte)0xda, (byte)0x87, 0x46, (byte)0xfe, (byte)0xd3, 0x27, 0x6b, 0x16, (byte)0x82, (byte)0x9e, (byte)0x9d, 0x5e, (byte)0xfd, (byte)0xff, 0x70, 0x5e, 0x08, (byte)0x9c, 0x6d};
	
	public SecretKeys mSecretKey;
	private AESKey mAESKeyInstance;
	
	public U2FToken() {
		attestationCertFile = new BinaryEF((byte)0xEF, (byte)0x01); 
		attestationCertFile.setFileContent(attestationCert);
	}
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new U2FToken().register();
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			getSelectResponse(apdu);
			return;
		}

		// 获取APDU的header
		byte[] buf = apdu.getBuffer();
		byte cla = buf[ISO7816.OFFSET_CLA];
		byte p1 = buf[ISO7816.OFFSET_P1];
		byte p2 = buf[ISO7816.OFFSET_P2];
		short lc = (short)(buf[ISO7816.OFFSET_LC] & 0x00FF);
		
		switch (buf[ISO7816.OFFSET_INS]) {
		case (byte) INS_TEST_ENCRYPT:
//			try {
//				KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
//			} catch(CryptoException e) {
//				short reason = e.getReason();
////				ISOException.throwIt(JCSystem.getVersion());
////				ISOException.throwIt(reason);
//			}
			try {
				Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
			} catch(CryptoException e) {
				ISOException.throwIt(JCSystem.getVersion());
				short reason = e.getReason();
				ISOException.throwIt(reason);
			}
			AESencrypt(apdu, cla, p1, p2, lc);
			break;
		case (byte) INS_TEST_DECRYPT:
//			decrypt(apdu, cla, p1, p2, lc);
			break;
		case (byte) INS_U2F_REGISTER: // U2F_REGISTER，注册指令
			u2fregister(apdu, cla, p1, p2, lc);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	/**
	 * 选择applet时，返回"U2F_V2"
	 * @param apdu
	 */
	private void getSelectResponse(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopyNonAtomic(version, (short)0, buffer, (short)0, (short)version.length);
		apdu.setOutgoingAndSend((short)0, (short)version.length);
	}

	/**
	 * 处理注册请求
	 * @param apdu
	 * @param cla 0x00
	 * @param p1 待定，u2f协议例子中是0x03，不知道为什么。不知道会不会是test-of-user-presence
	 * @param p2
	 * @param lc
	 */
	private void u2fregister(APDU apdu, byte cla, byte p1, byte p2, short lc) {
		byte[] buffer = apdu.getBuffer();
		if (cla != CLA_7816) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
		//生成认证公私钥
//		ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		KeyPair pair = SecP256r1.newKeyPair();
		pair.genKeyPair();
		ECPublicKey pubKey = (ECPublicKey) pair.getPublic();
		ECPrivateKey privKey = (ECPrivateKey) pair.getPrivate();
		// 生成KeyHandle
		//TODO 生成KeyHandle，里面的AppID似乎只能是Client传过来的AppID的hash？
		
		short sendlen = pubKey.getW(buffer, (short) 0);
//		ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		apdu.setOutgoingAndSend((short) 0, sendlen);
	}
	
	public void encrypt(APDU apdu, byte cla, byte p1, byte p2, short lc) {
		byte[] buffer = apdu.getBuffer();
		mSecretKey = new SecretKeys(SecretKeys.KEY_TYPE_AES);
		byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
		mSecretKey.keyWrap(data, (short) 0, (short) data.length, buffer, (short) 0, SecretKeys.MODE_ENCRYPT);
//		byte[] data = {0x49, 0x1E, (byte) 0x89, 0x0D, (byte) 0xE9, (byte) 0xAC, (byte) 0xE9, 0x32, (byte) 0x83, (byte) 0x8A, 0x49, 0x79, 0x2F, 0x22, 0x13, (byte) 0xF3};
//		secretKey.keyWrap(data, (short) 0, (short) 16, buffer, (short) 0, SecretKey.MODE_DECRYPT);
		apdu.setOutgoingAndSend((short) 0, (short) 48);
	}
	
	public void AESencrypt(APDU apdu, byte cla, byte p1, byte p2, short lc) {
		byte[] buffer = apdu.getBuffer();
		try {
			// TODO 这里有点问题，没有这个算法？
			mAESKeyInstance = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
		} catch(CryptoException e) {
			short reason = e.getReason();
			ISOException.throwIt(reason);
		}
		byte[] keyData = JCSystem.makeTransientByteArray((short) 16, JCSystem.CLEAR_ON_DESELECT);
		Util.arrayFillNonAtomic(keyData, (short) 0, (short) keyData.length, (byte) 0x00);
		mAESKeyInstance.setKey(keyData, (short) 0);
		
		byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
		Cipher cipher = null;
		try {
			// Cipher.getInstance在这里过不了，在U2FToken里能过？？？
			cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
		} catch (CryptoException e) {
//			ISOException.throwIt(JCSystem.getVersion());
			short reason = e.getReason();
			ISOException.throwIt(reason);
		}
		cipher.init(mAESKeyInstance, Cipher.MODE_ENCRYPT); // 初始向量(iv)是0
//		}
		
		// 加密或解密，doFinal后，cipher对象将被重置
		try {
			cipher.doFinal(data, (short) 0, (short) data.length, buffer, (short) 0);
		} catch(Exception e) {
			ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		}
		apdu.setOutgoingAndSend((short) 0, (short) 48);
	}
	
//	private void decrypt(APDU apdu, byte cla, byte p1, byte p2, short lc) {
//		SecretKeys secreKey = SecretKeys.getInstance(SecretKeys.KEY_TYPE_AES);
//		apdu.setIncomingAndReceive();
//		byte[] buffer = apdu.getBuffer();
//		byte[] data = {0x49, 0x1E, (byte) 0x89, 0x0D, (byte) 0xE9, (byte) 0xAC, (byte) 0xE9, 0x32, (byte) 0x83, (byte) 0x8A, 0x49, 0x79, 0x2F, 0x22, 0x13, (byte) 0xF3};
//		secreKey.keyWrap(data, (short) 0, (short) 16, buffer, (short) 0, SecretKeys.MODE_DECRYPT);
//		apdu.setOutgoingAndSend((short) 0, (short) 16);
//	}
//	public void uninstall() {
//		// TODO Auto-generated method stub
//		SecretKeys.mAESSecretKey = null;
//		SecretKeys.mDESSecretKey = null;
//	}
}
