package com.easemob.model;

import lombok.Data;

@Data
public class Unigram {
    private final String hanzi;
    private final String pinyin;

    public Unigram(String hanzi, String pinyin) {
        this.hanzi = hanzi;
        this.pinyin = pinyin;
    }
}