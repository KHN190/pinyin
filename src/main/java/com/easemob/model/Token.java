package com.easemob.model;

import lombok.Data;

@Data
public class Token {
    private String str;
    private String type;

    public Token() {}
    public Token(String str, String type) {
        this.str = str;
        this.type = type;
    }
}