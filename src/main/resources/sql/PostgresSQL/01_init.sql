/*
  Board Wave PostgreSQL 초기화 스크립트

  AI 보드게임 안내 기능에서 사용할 벡터 저장소용 데이터베이스를 생성한다.
  MariaDB는 서비스 운영 데이터를 저장하고, PostgreSQL은 pgvector 기반 유사도 검색에 사용한다.
*/

-- AI 검색용 PostgreSQL 데이터베이스
CREATE DATABASE board_cafe_kiosk_2603;