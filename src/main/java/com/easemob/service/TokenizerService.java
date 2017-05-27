/* Tokenize Pinyin Sequence */
package com.easemob.service;

import com.easemob.model.Token;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.min;

@Service
public class TokenizerService {

    // 声母
    private static final Set<String> SM = new HashSet<>(
            Arrays.asList("b", "c", "d", "f", "g", "h", "j", "k", "l", "m",
                    "n", "p", "q", "r", "s", "t", "w", "x", "y", "z",
                    "sh", "zh", "ch"));
    // 韵母
    private static final Set<String> YM = new HashSet<>(
            Arrays.asList("a", "ai", "ao", "an", "ang",
                    "e", "ei", "er", "en", "eng",
                    "i", "ia", "ian", "iang", "ie", "in", "ing", "iong", "iu",
                    "o", "ou", "ong",
                    "u", "ua", "uai", "uan", "uang", "ue", "ui", "uo",
                    "v", "ve", "vn"));
    // 分词符号
    private static final String TK = "'";

    private List<Token> tokens = new ArrayList<>();

    /**
     * @param  pinyin
     * @return pinyin   符合规范的拼音，不合规范的尾部会被截去
     */
    public String tokenize(String pinyin) {
        if (tokens.size() != 0) {
            tokens = new ArrayList<>();
        }
        if (!"".equals(pinyin)) {
            nextSm(pinyin);
        }
        return concateTokens();
    }

    private String concateTokens() {
        if (tokens.size() == 0) {
            return "";
        }
        String tmp = "";
        List<String> result = new ArrayList<>();

        for (Token tok : tokens) {
            if (tok.getType() == "sm") {
                if ("".equals(tmp)) {
                    tmp = tok.getStr();
                } else {
                    return String.join("'", result);
                }
            }
            if (tok.getType() == "ym") {
                result.add(tmp + tok.getStr());
                tmp = "";
            }
        }
        return String.join("'", result);
    }

    public boolean isPinyin(String pinyin) {
        if (String.join("", tokenize(pinyin).split("'")).equals(pinyin)) {
            return true;
        }
        return false;
    }

    private void nextSm(String pinyin) {
        nextSm(pinyin, "");
    }
    private void nextSm(String pinyin, String sm) {
        if ("".equals(pinyin) && "".equals(sm)) {
            return;
        }
        if (TK.equals(pinyin.substring(0, 1))) {
            if (!"".equals(sm)) {
                tokens.add(new Token(sm, "sm"));
            }
            nextSm(pinyin.substring(1));
            return;
        }
        String n_sm = getSm(pinyin, sm);
        while (!n_sm.equals(sm)) {
            sm = n_sm;
            pinyin = pinyin.substring(1);
            n_sm = getSm(pinyin, sm);
        }
        if (!"".equals(n_sm)) {
            tokens.add(new Token(n_sm, "sm"));
        }
        if (!"".equals(pinyin) && TK.equals(pinyin.substring(0, 1))) {
            nextSm(pinyin.substring(1));
            return;
        }
        if (!"".equals(getSm(pinyin))) {
            nextSm(pinyin);
            return;
        }
        if (!"".equals(getYm(pinyin))) {
            nextYm(pinyin);
            return;
        }
    }

    private void nextYm(String pinyin) {
        nextYm(pinyin, "");
    }
    private void nextYm(String pinyin, String ym) {
        if ("".equals(pinyin) && "".equals(ym)) {
            return;
        }
        if (TK.equals(pinyin.substring(0, 1))) {
            if (!"".equals(ym)) {
                tokens.add(new Token(ym, "ym"));
            }
            nextSm(pinyin.substring(1));
            return;
        }
        String n_ym = getYm(pinyin, ym);
        while (!n_ym.equals(ym)) {
            ym = n_ym;
            pinyin = pinyin.substring(1);
            n_ym = getYm(pinyin, ym);
        }
        if (!"".equals(n_ym)) {
            tokens.add(new Token(n_ym, "ym"));
        }
        if (!"".equals(pinyin) && TK.equals(pinyin.substring(0, 1))) {
            nextSm(pinyin.substring(1));
            return;
        }
        if (!"".equals(getSm(pinyin))) {
            nextSm(pinyin);
            return;
        }
        if (!"".equals(getYm(pinyin))) {
            nextYm(pinyin);
            return;
        }
    }

    // 获取最长的声母匹配
    private static String getSm(String pinyin) {
        return getSm(pinyin, "");
    }
    private static String getSm(String pinyin, String sm) {
        if (pinyin.length() == 0) {
            return sm;
        }
        if (pinyin.length() == 1) {
            if (SM.contains(sm + pinyin)) {
                return sm + pinyin;
            }
            return sm;
        }
        StringBuilder tmp = new StringBuilder(sm);
        for(int i = 0; i < min(2, pinyin.length()); i++) { // 最长扫描2位
            tmp.append(pinyin.substring(i, i + 1));
            if (SM.contains(tmp.toString())) {
                return tmp.toString();
            }
        }
        return sm;
    }

    // 获取最长的韵母匹配
    private static String getYm(String pinyin) {
        return getYm(pinyin, "");
    }
    private static String getYm(String pinyin, String ym) {
        if (pinyin.length() == 0) {
            return ym;
        }
        if (pinyin.length() == 1) {
            if (YM.contains(ym + pinyin)) {
                return ym + pinyin;
            }
            return ym;
        }
        StringBuilder tmp = new StringBuilder(ym);
        for(int i = 0; i < min(4, pinyin.length()); i++) { // 最长扫描4位
            tmp.append(pinyin.substring(i, i + 1));
            if (YM.contains(tmp.toString())) {
                return tmp.toString();
            }
        }
        return ym;
    }
}
