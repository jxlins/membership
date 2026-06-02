package com.jxl.membership.toolkit;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MemberNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generate() {
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "M" + LocalDateTime.now().format(FORMATTER) + random;
    }
}