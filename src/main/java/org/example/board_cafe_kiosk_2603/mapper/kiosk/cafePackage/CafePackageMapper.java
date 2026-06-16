package org.example.board_cafe_kiosk_2603.mapper.kiosk.cafePackage;

import org.apache.ibatis.annotations.Mapper;
import org.example.board_cafe_kiosk_2603.domain.kiosk.cafePackage.CafePackage;

import java.util.List;

@Mapper
public interface CafePackageMapper {

    // 활성화된 패키지 전체 조회 (키오스크 선택 화면)
    List<CafePackage> findAllActive();

    // ID로 단건 조회
    CafePackage findById(int id);
}
