package org.example.board_cafe_kiosk_2603.mapper.admin;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.admin.point.Customer;
import org.example.board_cafe_kiosk_2603.mapper.admin.point.CustomerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@Log4j2
@SpringBootTest
class CustomerMapperTest {
    @Autowired
    private CustomerMapper customerMapper;

    @Test
    void insertCustomerTest() {
        Customer customer = Customer.builder()
                .phone("01011112222")
                .isActive(true)
                .build();


        customerMapper.insertCustomer(customer);
        log.info("등록된 고객: {}", customer);

    }

    @Test
    void selectByPhoneTest() {
        Customer found = customerMapper.selectByPhone("010-1111-2222");
        log.info("조회된 고객: {}", found);
    }

}