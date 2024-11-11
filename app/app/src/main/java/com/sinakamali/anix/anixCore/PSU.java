package com.sinakamali.anix.anixCore;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.common.primitives.Bytes;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import kotlin.NotImplementedError;

public class PSU {
    private final PrivateKey privateSKey;
    private final PrivateKey privateEKey;

    private final PublicKey publicSKey;

    private final PublicKey publicEKey;
    private final byte[] signature;

    private final byte[] psuBytes;

    private final boolean do_once = true;

    public PrivateKey getPrivateSKey() {
        return privateSKey;
    }

    public PrivateKey getPrivateEKey() {
        return privateEKey;
    }

    public PublicKey getPublicSKey() {
        return publicSKey;
    }

    public PublicKey getPublicEKey() {
        return publicEKey;
    }

    public byte[] getPsuBytes() {
        return psuBytes;
    }

    public PSU(PrivateKey signingKey, PublicKey pubSigningKey) throws Exception {
        KeyPair baseSigningKeyPair = KeyManager.generateEdDSAKeyPair();
        privateSKey = baseSigningKeyPair.getPrivate();
        publicSKey = baseSigningKeyPair.getPublic();

        KeyPair baseEncryptionKeyPair = KeyManager.generateEncryptionKeyPair();
        privateEKey = baseEncryptionKeyPair.getPrivate();
        publicEKey = baseEncryptionKeyPair.getPublic();

        BCECPublicKey publicKeyE = (BCECPublicKey) publicEKey;
        byte[] x = publicKeyE.getQ().getAffineXCoord().getEncoded();
        byte[] rawPublicE = ByteBuffer.allocate(x.length).put(x).array();

        BCEdDSAPublicKey publicKeyS = (BCEdDSAPublicKey) publicSKey;
        byte[] rawPublicS = publicKeyS.getPointEncoding();
        signature = KeyManager.bSignMessageEdDSA(Bytes.concat(rawPublicE, rawPublicS), signingKey, pubSigningKey);

//        if (do_once) {
//            System.out.println("public E key size: " + publicEKey.getEncoded().length);
//            System.out.println("raw public E key size: " + rawPublicE.length);
//            System.out.println("public S key size: " + publicSKey.getEncoded().length);
//            System.out.println("raw public S key size: " + rawPublicS.length);
//            System.out.println(Arrays.toString(publicSKey.getEncoded()));
//            System.out.println(publicSKey.toString());
//            System.out.println("signature size: " + signature.length);
//            do_once = false;
//        }

        psuBytes = Bytes.concat(rawPublicE, rawPublicS, signature);
    }

    public boolean verifySigner(PublicKey signersPublicKey) throws Exception {
        BCECPublicKey publicKeyE = (BCECPublicKey) publicEKey;
        byte[] x = publicKeyE.getQ().getAffineXCoord().getEncoded();
        byte[] rawPublicE = ByteBuffer.allocate(x.length).put(x).array();

        BCEdDSAPublicKey publicKeyS = (BCEdDSAPublicKey) publicSKey;
        byte[] rawPublicS = publicKeyS.getPointEncoding();
        return KeyManager.verifyMessageEdDSA(Bytes.concat(rawPublicE, rawPublicS), signature, signersPublicKey);
    }

    @NonNull
    @Override
    public String toString() {
        return Base64.encodeToString(psuBytes, Base64.DEFAULT);
    }

    public byte[] encrypt(byte[] message) throws Exception {
        /*
        Encrypts using the public key
        */
//        return KeyManager.encryptMessage(publicKey, message);
        throw new NotImplementedError();
    }

    public byte[] decrypt(byte[] ciphertext) throws Exception {
        /*
        Decrypts using the private key
        */
        if (privateEKey == null) {
            System.out.println("Error: Can't decrypt using a PSU with no private key!\n");
            return null;
        }
        return KeyManager.decryptMessage(privateEKey, ciphertext);
    }
}
