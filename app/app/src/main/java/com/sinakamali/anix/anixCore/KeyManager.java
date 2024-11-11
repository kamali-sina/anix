package com.sinakamali.anix.anixCore;

import com.google.common.primitives.Bytes;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.internal.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;

public class KeyManager {
    private KeyPair currSigningKeyPair;
    private KeyPair currEncryptionKeyPair;
    private final List<PSU> PSUs = new ArrayList<>();

    private static BigInteger L;


    public KeyManager() throws Exception {
        currSigningKeyPair = generateEdDSAKeyPair();
        currEncryptionKeyPair = generateEncryptionKeyPair();
        setupL();
    }

    public KeyManager(String keyPairString) throws Exception {
        loadKeyPairFromString(keyPairString);
        currEncryptionKeyPair = generateEncryptionKeyPair();
        setupL();
    }

    private void setupL() {
        BigInteger xL = new BigInteger("2");
        xL = xL.pow(252);
        BigInteger b = new BigInteger("27742317777372353535851937790883648493");
        xL = xL.add(b);
        KeyManager.L = xL;
    }

    public void renewKeys() throws Exception {
        currSigningKeyPair = generateEdDSAKeyPair();
        currEncryptionKeyPair = generateEncryptionKeyPair();
        PSUs.clear();
    }

//    public static KeyPair generateRSAKeyPair() throws Exception {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//        keyPairGenerator.initialize(512);
//        return keyPairGenerator.generateKeyPair();
//    }

    public static KeyPair generateEdDSAKeyPair() throws Exception {
        String name = "Ed25519";
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EdDSA");
        keyPairGenerator.initialize(new ECGenParameterSpec(name));
        return keyPairGenerator.generateKeyPair();
    }

    public static KeyPair generateEncryptionKeyPair() throws Exception {
        String name = "secp256r1";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH");
        kpg.initialize(new ECGenParameterSpec(name));

        return kpg.generateKeyPair();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] signMessage(PrivateKey privateKey, byte[] message) throws Exception {
        return KeyManager.signMessageEdDSA(message, privateKey);
    }

    public static byte[] encryptMessage(PublicKey publicKey, byte[] message) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES");

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        System.out.println(cipher.getAlgorithm());
        System.out.println(cipher.getParameters().getAlgorithm());

        return cipher.doFinal(message);
    }

    public static byte[] encryptMessage(PrivateKey privateKey, byte[] message) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES");

        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        System.out.println(cipher.getAlgorithm());
        System.out.println(cipher.getParameters().getAlgorithm());

        return cipher.doFinal(message);
    }

    public static byte[] decryptMessage(PrivateKey privateKey, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(ciphertext);
    }

    static byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;

        byte[] result = new byte[length1 + length2];
        System.arraycopy(array1, 0, result, 0, length1);
        System.arraycopy(array2, 0, result, length1, length2);

        return result;
    }

    public static boolean verifySignature(PublicKey publicKey, byte[] message, byte[] signature) throws Exception {
        return verifyMessageEdDSA(message, signature, publicKey);
    }

    public PublicKey getCurrSigningPublicKey() {
        return currSigningKeyPair.getPublic();
    }

    public PublicKey getCurrEncyptionPublicKey() {
        return currEncryptionKeyPair.getPublic();
    }

    public PrivateKey getCurrSigningPrivateKey() {
        return currSigningKeyPair.getPrivate();
    }

    public PrivateKey getCurrEncryptionPrivateKey() {
        return currEncryptionKeyPair.getPrivate();
    }

    public byte[] signMessage(String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(currSigningKeyPair.getPrivate());

        signature.update(message.getBytes());

        return signature.sign();
    }

    public boolean verifySignature(byte[] message, byte[] signature) throws Exception {
        return verifyMessageEdDSA(message, signature, currSigningKeyPair.getPublic());
    }

    public PSU generateNewPSU() throws Exception {
        PSU newPSU = new PSU(currSigningKeyPair.getPrivate(), currSigningKeyPair.getPublic());
        PSUs.add(newPSU);
        return newPSU;
    }

    public String dumpKeyPairAsString() {
        PublicKey publicKey = currSigningKeyPair.getPublic();
        PrivateKey privateKey = currSigningKeyPair.getPrivate();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded()) + "\n"
                + Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public String dumpPublicKeyAsString() {
        PublicKey publicKey = currSigningKeyPair.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public void loadKeyPairFromString(String keyPairString) throws Exception {
        String[] parts = keyPairString.split("\n");
        byte[] publicKeyBytes = Base64.getDecoder().decode(parts[0]);
        byte[] privateKeyBytes = Base64.getDecoder().decode(parts[1]);

        KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        currSigningKeyPair = new KeyPair(publicKey, privateKey);
    }

    public static byte[] signMessageEdDSA(byte[] message, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature dsa = Signature.getInstance("EdDSA"); // Edwards digital signature algorithm
        dsa.initSign(privateKey);
        dsa.update(message, 0, message.length);
        return dsa.sign();
    }

    public static byte[] bSignMessageEdDSA(byte[] message, PrivateKey privateKey, PublicKey publicKey) throws Exception {
        // TODO: Update this whenever possible
        PrivateKey blindedPrivateKey = KeyManager.getBlindedPrivateKey(message, privateKey.getEncoded(), publicKey.getEncoded());
        return signMessageEdDSA(message, blindedPrivateKey);
    }

    public static boolean verifyMessageEdDSA(byte[] message, byte[] signature, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature verificationSignature = Signature.getInstance("EdDSA");
        verificationSignature.initVerify(publicKey);
        verificationSignature.update(message);

        return verificationSignature.verify(signature);
    }

    public static PrivateKey getBlindedPrivateKey(byte[] message, byte[] privateKeyBytes, byte[] publicKeyBytes) throws NoSuchAlgorithmException, IOException, NoSuchProviderException, InvalidKeySpecException {
        byte[] toHash = Bytes.concat(message, publicKeyBytes);
        MessageDigest hashDigest = MessageDigest.getInstance("SHA-256");
        byte[] bk = hashDigest.digest(toHash);
        BigInteger privateKeyBigInt = new BigInteger(privateKeyBytes);
        BigInteger bkFactor = new BigInteger(bk);
        byte[] blindedKeyBytes = privateKeyBigInt.multiply(bkFactor).mod(KeyManager.L).toByteArray();
        blindedKeyBytes = Bytes.ensureCapacity(blindedKeyBytes, 32, 0);

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME);
        PrivateKeyInfo privKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(blindedKeyBytes));
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privKeyInfo.getEncoded());


        return keyFactory.generatePrivate(pkcs8KeySpec);


//        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("Ed25519");

//        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateKeyBytes), spec);

//        ECNamedCurveSpec params = new ECNamedCurveSpec("Ed25519", spec.getCurve(), spec.getG(), spec.getN());
//        java.security.spec.ECPoint w = new java.security.spec.ECPoint(new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 0, 24)), new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 24, 48)));
//        return keyFactory.generatePrivate(ecPrivateKeySpec);
//        try {
//            return keyFactory.generatePrivate(pkcs8KeySpec);
//        } catch (Exception e) {
//            privKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(privateKeyBytes));
//            pkcs8KeySpec = new PKCS8EncodedKeySpec(privKeyInfo.getEncoded());
//            return keyFactory.generatePrivate(pkcs8KeySpec); // TODO: Fix this somehow!!!
//        }
    }

    public static PublicKey getBlindedPublicKey(byte[] message, byte[] publicKeyBytes) throws NoSuchAlgorithmException, IOException, NoSuchProviderException, InvalidKeySpecException {
//        if (L == null) {
//
//        }
        byte[] toHash = new byte[message.length + publicKeyBytes.length];

        for (int i = 0; i < toHash.length; ++i) {
            toHash[i] = i < message.length ? message[i] : publicKeyBytes[i - message.length];
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bk = digest.digest(toHash);
        BigInteger publicKeyBigInt = new BigInteger(publicKeyBytes);
        BigInteger bkFactor = new BigInteger(bk);
        byte[] blindedPublicKeyBytes = publicKeyBigInt.multiply(bkFactor).mod(KeyManager.L).toByteArray();

        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME);
        SubjectPublicKeyInfo blindedPubKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), publicKeyBytes);
        X509EncodedKeySpec newX509KeySpec = new X509EncodedKeySpec(blindedPubKeyInfo.getEncoded());

        return keyFactory.generatePublic(newX509KeySpec);
    }
}
