# 회차별 좌석 조회 인덱스 성능 비교

## 1. 테스트 목적
복합 UNIQUE 인덱스가 회차별 좌석 조회 성능에 미치는 영향을 확인한다.

`도메인 규칙이기 때문에 무조건 있어야 하지만 없다는 가정`하에 쿼리 성능이 어떻게 되는지 확인하기 위해 기록한 문서다.

## 2. 테스트 환경
- PostgreSQL 17
- 테스트 데이터 규모
  - 회차당 좌석 수: 500 ~ 1,000
  - 전체 좌석 수: 297,360

## 3. 테이블과 인덱스
Session Seat 엔티티는 하나의 PerformanceSession에 동일한 seatNumber를 방지해야하기 때문에 UNIQUE 제약 조건을 추가했다.

``` sql
-- V3__add_session_seats_uk.sql

ALTER TABLE seat_reservation.session_seats
ADD CONSTRAINT uk_session_seat_session_number
UNIQUE (session_id, seat_number);
```

### 인덱스를 적용 상태의 실행 계획
``` sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * from seat_reservation.session_seats where session_id = 50;
```
``` plaintext
실행 결과

Index Scan using uk_session_seat_session_number on session_seats  (cost=0.42..1654.26 rows=1080 width=77) (actual time=0.016..0.095 rows=990 loops=1)
  Index Cond: (session_id = 50)
  Buffers: shared hit=18
Planning Time: 0.056 ms
Execution Time: 0.130 ms
```

### 인덱스를 적용하지 않은 상태의 실행 계획
``` sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * from seat_reservation.session_seats where session_id = 50;
```
``` plaintext
실행 결과

Gather  (cost=1000.00..6332.47 rows=1080 width=77) (actual time=1.223..7.625 rows=990 loops=1)
  Workers Planned: 1
  Workers Launched: 1
  Buffers: shared hit=3038
  ->  Parallel Seq Scan on session_seats  (cost=0.00..5224.47 rows=635 width=77) (actual time=2.379..4.706 rows=495 loops=2)
        Filter: (session_id = 50)
        Rows Removed by Filter: 148185
        Buffers: shared hit=3038
Planning Time: 0.050 ms
Execution Time: 7.666 ms
```

### 분석
**인덱스 적용 상태**
- 복합 UNIQUE 인덱스가 있을 때는 `session_id`를 첫 컬럼으로 사용하는 인덱스 스캔이 수행되었다.
- 조회에 18개 버퍼가 사용되었으며 실행 시간은 `0.130ms`

**인덱스 적용 하지 않은 상태**
- 전체 297,360개 좌석을 대상으로 병렬 순차 탐색 수행
- 버퍼 사용량 3,038개, 실행 시간은 `7.666ms`로 증가

**분석 결과**

UNIQUE 제약 조건은 도메인 중복을 방지할 뿐만 아니라 회차별 좌석 조회 범위를 효율적으로 제한할 수 있다.
