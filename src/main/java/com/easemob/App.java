package com.easemob;

import com.easemob.service.HMMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class App {

    @Autowired
    private HMMService hmm;

    @PostConstruct
    private void initHMMService() throws Exception {
        hmm.init("bigrams.txt");
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
