package com.easemob.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HMMService extends JedisService {

    private static final String DATA_DIR = System.getProperty("user.dir") + "/data/";
    private static final String START_PROB = "start_prob";
    private static final String EMISS_PROB = "emiss_prob";
    private static final String TRANS_PROB = "trans_prob";
    private static final String PINYIN_DICT = "pinyin_dict";

    private boolean isTrained = false;
    private Map<String, Double> startProb = new HashMap<>();
    private Map<String, Double> emissProb = new HashMap<>();
    private Map<String, Double> transProb = new HashMap<>();
    private Map<String, Set<String>> pinyinDict = new HashMap<>();

    public void init(String fname) throws Exception {
        if (isTrained() == true) {
            return;
        }
        jedis.delete(jedis.keys("path_*"));

        if (loadDefaultDict()) {
            log.info("默认词典已加载..");
        }

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(DATA_DIR + fname)));

        log.info("开始计数..开始词典构建..");

        int s_total = 0;
        int e_total = 0;
        int t_total = 0;
        while ((line = reader.readLine()) != null) {
            if (line.split(" ").length != 2) {
                continue;
            }

            String[] string = line.split(" ");
            String[] pinyin = string[1].split("'");

            if (string[0].length() != pinyin.length) {
                continue;
            }
            if (string[0].length() < 2) {
                continue;
            }

            for (int i = 0; i < string[0].length() - 1; i++) {
                String hanzi_1 = string[0].substring(i,   i+1);
                String hanzi_2 = string[0].substring(i+1, i+2);

                // 起始概率
                String sk_1 = START_PROB + '_' + hanzi_1;
                String sk_2 = START_PROB + '_' + hanzi_2;

                if (!startProb.containsKey(sk_1)) {
                    startProb.put(sk_1, 1.0); s_total++;
                }
                if (!startProb.containsKey(sk_2)) {
                    startProb.put(sk_2, 1.0); s_total++;
                }

                startProb.put(sk_1, startProb.get(sk_1) + 1); s_total += 1;

                // 发射概率
                String ek_1 = EMISS_PROB + '_' + hanzi_1;
                String ek_2 = EMISS_PROB + '_' + hanzi_2;

                if (!startProb.containsKey(ek_1)) {
                    emissProb.put(ek_1, 0.0); e_total++;
                }
                if (!startProb.containsKey(ek_2)) {
                    emissProb.put(ek_2, 0.0); e_total++;
                }

                emissProb.put(ek_1, emissProb.get(ek_1) + 1); e_total++;
                emissProb.put(ek_2, emissProb.get(ek_2) + 1); e_total++;

                // 转移概率
                String key = TRANS_PROB + '_' + hanzi_1 + '_' + hanzi_2;

                if (!transProb.containsKey(key)) {
                    transProb.put(key, 0.0);
                }
                transProb.put(key, transProb.get(key) + 1); t_total++;

                // 构建字典
                addHanziToDict(pinyin[i],   hanzi_1);
                addHanziToDict(pinyin[i+1], hanzi_2);
            }
        }
        reader.close();
        log.info("计数完成..词典构建完成..");

        // 计数转换为对数概率
        for (String key : startProb.keySet()) {
            double val = Math.log(startProb.get(key) / s_total);
            startProb.put(key, val);
        }
        for (String key : emissProb.keySet()) {
            double val = Math.log(emissProb.get(key) / e_total);
            emissProb.put(key, val);
        }
        for (String key : transProb.keySet()) {
            double val = Math.log(transProb.get(key) / t_total);
            transProb.put(key, val);
        }
        log.info("概率统计完成..");

        log.info("startProb: " + startProb.keySet().size());
        log.info("emissProb: " + emissProb.keySet().size());
        log.info("transProb: " + transProb.keySet().size());

        isTrained = true;
    }

    public boolean isTrained() {
        return isTrained;
    }

    private boolean loadDefaultDict() throws Exception {
        File f = new File(DATA_DIR + "dict.txt");
        if (!f.exists()) {
            log.error("HMMService default dict fail to load");
            return false;
        }

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(DATA_DIR + "dict.txt")));
        while ((line = reader.readLine()) != null) {
            String pinyin = line.split(" ")[0];
            String hanzis = line.split(" ")[1];
            Set<String> hanziSet = new HashSet<>();
            for (char c : hanzis.toCharArray()) {
                hanziSet.add(String.valueOf(c));
            }
            pinyinDict.put( pinyin, hanziSet);
        }
        reader.close();
        return true;
    }

    /* 可能的汉字 */
    public Set<String> toHanzi(String prevHanzi, String pinyin) {
        if ("".equals(prevHanzi)) {
            return getHanzi(pinyin);
        }
        return getHanzi(pinyin).stream()
                .filter(x -> getTransProb(prevHanzi, x) > Double.NEGATIVE_INFINITY)
                .collect(Collectors.toSet());
    }

    /* 查找数据 */
    public double getStartProb(String hanzi) {
        String key = START_PROB + '_' + hanzi;
        if (startProb.containsKey(key)) {
            return startProb.get(key);
        }
        return Double.NEGATIVE_INFINITY;
    }

    public double getEmissProb(String hanzi) {
        String key = EMISS_PROB + '_' + hanzi;
        if (emissProb.containsKey(key)) {
            return emissProb.get(key);
        }
        return Double.NEGATIVE_INFINITY;
    }

    public double getTransProb(String hanzi_1, String hanzi_2) {
        String key = TRANS_PROB + '_' + hanzi_1 + '_' + hanzi_2;
        if (transProb.containsKey(key)) {
            return transProb.get(key);
        }
        return Double.NEGATIVE_INFINITY;
    }

    public Set<String> getHanzi(String pinyin) {
        String key = PINYIN_DICT + '_' + pinyin;
        if (pinyinDict.containsKey(key)) {
            return pinyinDict.get(key);
        }
        return new HashSet<>();
    }

    /**
     * 找出最大可能的汉字序列
     *
     * [pinyin, pinyin, ..] -> [path, path, ..]
     *
     * @param  pinyins   拼音序列
     * @return paths     最大可能的汉字序列
     */
    public List<String> maxProbPath(List<String> pinyins) {
        Set<String> allPaths = new HashSet<>();
        genProbPath(pinyins, allPaths, "");

        Map<String, Double> result = allPaths.stream()
                .collect(Collectors.toMap(
                        x -> x,
                        x -> jedisDouble("path_" + x)));
        List<String> paths = result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return paths.subList(0, Math.min(10, paths.size()));
    }

    /**
     * 找出所有可能的汉字序列
     *
     * @param pinyins  拼音序列
     * @param paths    正在构建的所有可能的汉字序列
     * @param path     正在构建的路径
     */
    private void genProbPath(List<String> pinyins, Set<String> paths, String path) {
        if (pinyins.size() == 0) {
            log.debug("path generated: " + path + ", " + jedisDouble("path_" + path));
            savePath(paths, path); return;
        }
        String prevHanzi = "";
        if (!"".equals(path)) {
            prevHanzi = path.substring(path.length() - 1);
        }
        log.debug("可能的汉字(" + pinyins.get(0) + "): " + toHanzi(prevHanzi, pinyins.get(0)));

        for (String hanzi : toHanzi(prevHanzi, pinyins.get(0))) { // @FIXME: possibly empty
            double prob;
            String key = "path_" + path + hanzi;

            if (!jedis.hasKey(key)) {
                if ("".equals(path)) {
                    prob = getEmissProb(hanzi) +
                            getStartProb(hanzi);
                } else {
                    double prev_prob = jedisDouble("path_" + path); // @FIXME: getPrevPathProb(path) -> prob
                    double tran_prob = getTransProb(prevHanzi, hanzi);
                    prob = getEmissProb(hanzi) +
                            tran_prob +
                            prev_prob;
                }
                log.debug("path saved to redis: " + key + ", " + prob);
                jedis.boundHashOps(key).put(key, String.valueOf(prob));
            }
            genProbPath(pinyins.subList(1, pinyins.size()), paths, path + hanzi);
        }
    }

    /* maintain a PrioritySet, keep set size using probability */
    private void savePath(Set<String> paths, String path) {
        if (paths.size() < 10) {
            paths.add(path); return;
        }
        double prob = jedisDouble("path_" + path);
        double min_prob = Collections.min(
                paths.stream()
                        .map(p -> jedisDouble("path_" + p))
                        .collect(Collectors.toSet())
        );
        if (prob > min_prob) {
            paths.stream()
                    .filter(x -> jedisDouble("path_" + x) == min_prob)
                    .map(x -> paths.remove(x));
            paths.add(path);
        }
    }

    /* 构建拼音到汉字的字典 */
    private void addHanziToDict(String pinyin, String hanzi) {
        String key = PINYIN_DICT + '_' + pinyin;
        Set<String> hanzis = new HashSet<>();

        if (pinyinDict.containsKey(key)) {
            hanzis = pinyinDict.get(key);
        }
        hanzis.add(hanzi);
        pinyinDict.put(key, hanzis);
    }
}
