package com.sinakamali.anix.anixCore;

import java.security.PublicKey;

public class Contact {
    private String name;
    private PublicKey publicKey;

    public Contact(String name, PublicKey publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
