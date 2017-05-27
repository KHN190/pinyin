package com.easemob.model;

import lombok.Data;

@Data
public class Bigram {
    private final Unigram one;
    private final Unigram two;

    public Bigram(Unigram l, Unigram k) {
        this.one = l;
        this.two = k;
    }
}