package org.example.board_cafe_kiosk_2603.mapper.kiosk;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cafePackage.CafePackage;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cafePackage.CafePackageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Log4j2
@SpringBootTest
class CafePackageMapperTest {

    @Autowired
    private CafePackageMapper cafePackageMapper;

    @Test
    void findAllActive_returnsActivePackages() {
        List<CafePackage> packages = cafePackageMapper.findAllActive();
        assertThat(packages).isNotEmpty();
        packages.forEach(p -> assertThat(p.isActive()).isTrue());
    }

    @Test
    void findAllActive_sortedByBasePrice() {
        List<CafePackage> packages = cafePackageMapper.findAllActive();
        for (int i = 0; i < packages.size() - 1; i++) {
            assertThat(packages.get(i).getBasePrice())
                    .isLessThanOrEqualTo(packages.get(i + 1).getBasePrice());
        }
    }

    @Test
    void findById_success() {
        List<CafePackage> packages = cafePackageMapper.findAllActive();
        int existingId = packages.get(0).getId();

        CafePackage found = cafePackageMapper.findById(existingId);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(existingId);
        assertThat(found.getName()).isNotBlank();
        assertThat(found.getBasePrice()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void findById_notFound() {
        CafePackage found = cafePackageMapper.findById(99999);
        assertThat(found).isNull();
    }
}
