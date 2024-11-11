package com.sinakamali.anix.anixCore;

import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AnixCoreMessage {
    public byte[] getMessage() {
        return message;
    }

    public PSU getSenderPSU() {
        return senderPSU;
    }

    public byte[] getSenderSignature() {
        return senderSignature;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    private final byte[] message;
    private final PSU senderPSU;
    private final String separator = "|";
    private final byte[] senderSignature;
    private final List<Vote> votes = new ArrayList<>();

    private boolean do_once = true;

    public AnixCoreMessage(byte[] message, PSU senderPSU) throws Exception {
        this.message = message;
        this.senderPSU = senderPSU;

        this.senderSignature = KeyManager.signMessage(this.senderPSU.getPrivateSKey(), message);
    }

    public String dumpMessageToString() {
        String m = new String(message);
        String signature = Base64.getEncoder().encodeToString(senderSignature);
        String psu = Base64.getEncoder().encodeToString(senderPSU.getPsuBytes());

        return psu + signature + m;
    }

    public byte[] dumpMessageToBytes() throws IOException {
        if (do_once) {
            System.out.println("psu string size: " + senderPSU.getPsuBytes().length);
            System.out.println("sender signature size: " + senderSignature.length);
            System.out.println("message size: " + message.length);
            do_once = false;
        }
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        String signature = Base64.getEncoder().encodeToString(senderSignature);
//        outputStream.write(message);
//        outputStream.write(separator.getBytes());
//        outputStream.write(signature.getBytes());

        return Bytes.concat(senderPSU.getPsuBytes(), senderSignature, message);

//        return outputStream.toByteArray();
    }

    public byte[] dumpVotesToBytes() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Vote vote : votes) {
            outputStream.write(vote.dumpVoteToBytes());
        }
        return outputStream.toByteArray();
    }

    public Vote addVote(PrivateKey signerKey, PublicKey pubKey, Boolean approve) throws Exception {
        byte approveByte = (byte) (approve ? 1 : 0);
        byte[] approveByteArr = new byte[1];
        approveByteArr[0] = approveByte;
        byte[] messageToSign = Bytes.concat(message, senderSignature, senderPSU.getPsuBytes(), approveByteArr);

        byte[] voteString = KeyManager.bSignMessageEdDSA(messageToSign, signerKey, pubKey);

        Vote vote = new Vote(approve, voteString);
        votes.add(vote);
        return vote;
    }
}
