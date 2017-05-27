package com.easemob.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.easemob.App;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@TestPropertySource(locations="classpath:application.properties")
public class HMMServiceTest {

    @Autowired
    private HMMService hmm;

    @Test
    public void testInit() throws Exception {
        hmm.init("bigrams.txt");
    }

    @Test
    public void testMaxProbPath() throws Exception {
        List<String> pinyins = Arrays.asList("hang", "kong", "mu", "jian");
        List<String> result = hmm.maxProbPath(pinyins);
        System.out.println(pinyins + " -> " + result + " (" + result.size() + ")");

        pinyins = Arrays.asList("hang", "zhou");
        result = hmm.maxProbPath(pinyins);
        System.out.println(pinyins + " -> " + result + " (" + result.size() + ")");

        pinyins = Arrays.asList("hang", "zhou", "zhan");
        result = hmm.maxProbPath(pinyins);
        System.out.println(pinyins + " -> " + result + " (" + result.size() + ")");

        pinyins = Arrays.asList("h", "h", "h");
        result = hmm.maxProbPath(pinyins);
        System.out.println(pinyins + " -> " + result + " (" + result.size() + ")");
    }
}
