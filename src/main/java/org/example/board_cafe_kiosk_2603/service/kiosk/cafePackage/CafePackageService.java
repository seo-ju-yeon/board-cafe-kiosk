package org.example.board_cafe_kiosk_2603.service.kiosk.cafePackage;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cafePackage.CafePackage;
import org.example.board_cafe_kiosk_2603.dto.kiosk.cafePackage.CafePackageDTO;
import org.example.board_cafe_kiosk_2603.mapper.kiosk.cafePackage.CafePackageMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class CafePackageService {

    private final CafePackageMapper cafePackageMapper;
    private final ModelMapper       modelMapper;

    /** 활성화된 패키지 목록 조회 */
    public List<CafePackageDTO> getActivePackages() {
        return cafePackageMapper.findAllActive()
                .stream()
                .map(p -> modelMapper.map(p, CafePackageDTO.class))
                .collect(Collectors.toList());
    }

    /** ID로 단건 조회 */
    public CafePackageDTO getById(int id) {
        CafePackage pkg = cafePackageMapper.findById(id);
        return pkg != null ? modelMapper.map(pkg, CafePackageDTO.class) : null;
    }
}
