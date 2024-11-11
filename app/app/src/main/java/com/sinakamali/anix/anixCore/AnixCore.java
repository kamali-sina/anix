package com.sinakamali.anix.anixCore;

import com.google.common.primitives.Bytes;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class AnixCore {
    public final KeyManager keyManager;
    private final List<Contact> ContactList = new ArrayList<>();

    public static final String SAVED_KEYPAIR_KEY = "saved_keypairs";
    public static final String NOT_FOUND_ERROR = "not found";

    public AnixCore() throws Exception {
        setupBouncyCastle();
        keyManager = new KeyManager();
    }

    public AnixCore(String keypair_string) throws Exception {
        setupBouncyCastle();
        keyManager = new KeyManager(keypair_string);
    }

    public String getKeyPairString() {
        return keyManager.dumpKeyPairAsString();
    }

    public String getPublicKeyString() {
        return keyManager.dumpPublicKeyAsString();
    }

    public AnixCoreMessage createMessage(byte[] message) throws Exception {
        PSU psu = keyManager.generateNewPSU();
        return new AnixCoreMessage(message, psu);
    }

    public AnixCoreMessage createMessage(byte[] message, PSU temPsu) throws Exception {
        return new AnixCoreMessage(message, temPsu);
    }

    public AnixCoreMessage voteOnMessage(AnixCoreMessage anixCoreMessage, boolean approve) throws Exception {
        anixCoreMessage.addVote(keyManager.getCurrSigningPrivateKey(), keyManager.getCurrSigningPublicKey(), approve);
        return anixCoreMessage;
    }

    public byte[] createOWTMessage(PSU receiverPSU) throws Exception {
        byte[] OWTtoEncrypt = Bytes.concat(keyManager.getCurrSigningPublicKey().getEncoded(),
                keyManager.getCurrEncyptionPublicKey().getEncoded());
        return KeyManager.encryptMessage(receiverPSU.getPublicEKey(), OWTtoEncrypt);
    }

    public byte[] createO2OMessage(byte[] message, PublicKey receiverPubKey) throws Exception {
        byte[] encryptedMessage = KeyManager.encryptMessage(keyManager.getCurrEncryptionPrivateKey(), message);
        byte[] finalMessageToEncrypt = Bytes.concat("message:".getBytes(), encryptedMessage);
        return KeyManager.encryptMessage(receiverPubKey, finalMessageToEncrypt);
    }

    public void renewKeys() throws Exception {
        keyManager.renewKeys();
    }

    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            return;
        }
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }
}
