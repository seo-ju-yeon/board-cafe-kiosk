package org.example.board_cafe_kiosk_2603.service.kiosk;

import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cafePackage.CafePackage;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cafePackage.CafePackageDTO;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cafePackage.CafePackageMapper;
import org.example.board_cafe_kiosk_2603.service.kiosk.cafePackage.CafePackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@Log4j2
@ExtendWith(MockitoExtension.class)
class CafePackageServiceTest {

    @Mock private CafePackageMapper cafePackageMapper;
    @Mock private ModelMapper        modelMapper;

    @InjectMocks
    private CafePackageService cafePackageService;

    private CafePackage buildPackage(int id, String name, int price) {
        return CafePackage.builder()
                .id(id).name(name).type("HOURLY")
                .durationMinutes(60).basePrice(price).active(true)
                .build();
    }

    @Test
    void getActivePackages_returnsList() {
        CafePackage pkg1 = buildPackage(1, "평일 1시간 권", 3000);
        CafePackage pkg2 = buildPackage(2, "평일 무제한 권", 15000);
        given(cafePackageMapper.findAllActive()).willReturn(List.of(pkg1, pkg2));

        CafePackageDTO dto1 = new CafePackageDTO();
        CafePackageDTO dto2 = new CafePackageDTO();
        given(modelMapper.map(pkg1, CafePackageDTO.class)).willReturn(dto1);
        given(modelMapper.map(pkg2, CafePackageDTO.class)).willReturn(dto2);

        List<CafePackageDTO> result = cafePackageService.getActivePackages();

        assertThat(result).hasSize(2);
        then(cafePackageMapper).should().findAllActive();
    }

    @Test
    void getActivePackages_empty() {
        given(cafePackageMapper.findAllActive()).willReturn(List.of());

        assertThat(cafePackageService.getActivePackages()).isEmpty();
    }

    @Test
    void getById_success() {
        CafePackage pkg = buildPackage(1, "평일 1시간 권", 3000);
        CafePackageDTO dto = new CafePackageDTO();
        given(cafePackageMapper.findById(1)).willReturn(pkg);
        given(modelMapper.map(pkg, CafePackageDTO.class)).willReturn(dto);

        assertThat(cafePackageService.getById(1)).isNotNull();
    }

    @Test
    void getById_notFound() {
        given(cafePackageMapper.findById(999)).willReturn(null);

        assertThat(cafePackageService.getById(999)).isNull();
        then(modelMapper).should(never()).map(any(), any());
    }
}
