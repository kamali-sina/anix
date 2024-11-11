package com.sinakamali.anix.anixCore;

import java.io.IOException;

public class Vote {
    public boolean isApprove() {
        return approve;
    }

    public byte[] getVoteString() {
        return voteString;
    }

    private final boolean approve;
    private final byte[] voteString;

    private boolean do_once = true;

    public Vote(boolean approve, byte[] voteString) {
        this.approve = approve;
        this.voteString = voteString;
    }

    public byte[] dumpVoteToBytes() throws IOException {
        if (do_once) {
            System.out.println("vote string size: " + voteString.length);
            do_once = false;
        }
        return voteString;
    }

//    public Vote(boolean approve, message) {
//        this.approve = approve;
//        this.voteString = voteString;
//    }

    // TODO: Add vote creation
}
