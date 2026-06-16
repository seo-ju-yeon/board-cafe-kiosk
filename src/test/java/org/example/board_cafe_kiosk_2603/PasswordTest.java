package org.example.board_cafe_kiosk_2603;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Log4j2
public class PasswordTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void encode() {
//        int rawPassword = 1111
        System.out.println(passwordEncoder.encode("여기에실제비밀번호"));
    }
}
