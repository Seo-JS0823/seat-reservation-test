# 재점유 vs 취소 API 데드락 해결 기록

## 1. 개요
하나의 좌석에 접근하는 API 2종과 스케줄러의 데이터 정합성이 지켜지는지 동시성 테스트를 실시하는 중이었다. 테스트의 결과를 잘 확인하기 위해 사용중인 PostgreSQL의 에러코드를 매핑시켜주고 예외체인을 타고 내려가서 SQLException에서 SqlState를 직접 가져와서 검사하도록 테스트 유틸리티를 구성했다.

재점유와 취소 API의 동시성 테스트 코드를 작성하고 돌려보았는데 @RepeatedTest(30)으로 30번의 실행 결과중 29건 성공 1건의 실패 그리고 그 원인이 데드락(40P01)로 나온것을 확인했다.

## 2. 테스트 환경
- Postgres 17
- Testcontainers

## 3. 데드락 발생 원인
재점유의 코드가 좌석을 조회하고 HELD일 때 해당 좌석의 현재 점유중인 기록을 조회해온다. 만료검사 후 점유 기록을 변경하는데 JPA의 쓰기 지연에 의해 유니크 제약조건에 걸리기 때문에 이를 회피하기 위해 EXPIRED로 변경한 직후 명시적으로 flush()를 호출했다. 이때 점유행에 대한 행 잠금이 일어난다. 최종적으로 한 트랜잭션에서 이 코드가 성공경로로 가기 위해선 점유행 -> 좌석행 순으로 행 잠금이 일어난다.

취소의 경우 JOIN FETCH를 이용해 SeatHold와 SessionSeat를 한번에 조회해온다. 이후 인가 검사후 두 행을 UPDATE 하게 되는데 이때 행 잠금의 순서가 좌석행 -> 점유행 순으로 행 잠금이 일어난다.

데드락 발생의 원인이 순환 대기인 것을 확인했다.

스케줄러의 행 락 순서도 좌석행 -> 점유행이므로 최소한으로 변경해서 재점유 로직에서

- seatRepo.flush()
- seat.replaceExpiredHold(holding.getHoldId(), holdId);

두 코드의 순서만 바꾸어도 락 순서를 통일 시켜 순환 대기를 예방할 수 있다.

## 4. 실행 분석
### 재점유: SessionSeatService.seatHold()
1. 점유 가능한 좌석 조회 -> SessionSeat
2. 재점유는 해당 좌석이 HELD 일 때 분기
3. 해당 좌석의 현재 점유중인 기록 조회
4. 만료 검사후 점유 기록을 EXPIRED로 변경 후 flush() -> 행 락 잠금
5. 좌석을 새로운 holdId로 바꾸고 새로운 점유 기록을 생성
6. commit()
``` java
@Transactional
public SessionSeatHold seatHold(Long seatId, UUID userId, UUID holdId, LocalDateTime now) {
	// 1. 점유 가능한 좌석 조회
	SessionSeat seat = seatRepo.getSessionSeat(seatId)
			.orElseThrow(() -> new SeatException(SeatError.SESSION_SEAT_NOT_FOUND));
	
	// 2. 점유 가능한 좌석의 상태 확인 및 분기
	boolean usable = false;
	
	switch (seat.getStatus()) {
		case AVAILABLE -> usable = seat.availableOnlySeatHold(holdId);
		
		case HELD -> {
			SeatHold holding = seatRepo.findHoldingSeat(seat)
					.orElseThrow(() -> new SeatException(SeatError.SEAT_HOLD_NOT_STATUS));
			
			if (holding.isExpired(now)) {
				holding.expire(now);
				
				seatRepo.flush();
				
				seat.replaceExpiredHold(holding.getHoldId(), holdId);
				
				usable = true;
			}
		}
		
		default -> throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS);
	}
	
	if (!usable)
		throw new SeatException(SeatError.SEAT_HOLD_NOT_STATUS);
	
	// 3. 좌석 점유 정보 저장
	SeatHold hold = SeatHold.hold(holdId, seat, userId, now, seatProperties.ttl());
	
	seatRepo.saveHold(hold);
	
	return new SessionSeatHold(
			seat.getSeatId(),
			seat.getSession().getSessionId(),
			seat.getSeatNumber(),
			seat.getGrade(),
			seat.getPrice(),
			seat.getStatus(),
			seat.getCurrentHoldId(),
			hold.getExpiresAt(),
			hold.getHoldId()
	);
}
```
### 취소: SessionSeatService.seatCancel()
1. 좌석의 점유 정보 조회 -> JOIN FETCH SeatHold, SessionSeat
2. 취소 시켜도 되는지 검사 후 점유 기록을 취소하고 좌석도 점유 가능한 상태로 변경
``` java
@Transactional
public void seatCancel(Long seatId, UUID userId, UUID holdId, LocalDateTime now) {
	// 1. 좌석 점유 정보 조회 + SessionSeat Join
	SeatHold currentHold = seatRepo.findMeHoldingSeat(holdId, userId)
			.orElseThrow(() -> new SeatException(SeatError.SEAT_HOLDING_NOT_FOUND));
	
	// 2. 조회한 점유 정보가 요청 경로의 좌석 및 현재 점유 정보와 일치하는지 확인
	SessionSeat seat = currentHold.getSeat();
	
	boolean usable =
			
			// 2-1. holdId가 좌석의 currentHoldId와 같은가?
			currentHold.getHoldId().equals(seat.getCurrentHoldId())
			
			// 2-2. 요청으로 들어온 좌석 식별자가 점유 정보와 연결된 좌석의 seatId와 같은가?
			&& seatId.equals(seat.getSeatId())
			
			// 2-3. 좌석의 상태가 HELD 인가?
			&& seat.getStatus() == SeatStatus.HELD;
	
	if (!usable) {
		throw new SeatException(SeatError.SEAT_HOLD_INVALID_STATE);
	}
	
	// 3. 좌석 점유를 취소
	currentHold.cancel(now);
	seat.release(holdId);
}
```

## 테스트 코드
```
@RepeatedTest(30)
	@DisplayName("[동시성 테스트] 재점유 vs 취소: 누가 이기든 정합성은 지켜지고, 진 쪽은 SeatException을 받는다")
	void rehold_vs_cancel(RepetitionInfo repetition) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		
		Throwable reholdError;
		Throwable cancelError;
		UUID holdB = null;
		
		try {
			// 1. 두 요청을 출발 선에 세운다.
			Future<SessionSeatHold> rehold = pool.submit(() -> {
				start.await();
				return seatHoldUsecase.seatHold(seatId, userB);
			});
			
			Future<?> cancel = pool.submit(() -> {
				start.await();
				seatHoldUsecase.cancelSeatHold(seatId, userA, holdA);
				return null;
			});
			
			// 2. 동시에 출발시키고 둘 다 끝날 때까지 기다린다.
			start.countDown();
			
			reholdError = failureOf(rehold);
			cancelError = failureOf(cancel);
			
			if (reholdError == null) {
				holdB = rehold.get().holdId();
			}
		}
		
		finally {
			pool.shutdown();
		}
		
		String success = ResultCode.SUCCESS.code();
		String reholdResult = label(reholdError);
		String cancelResult = label(cancelError);
		SeatRow seat = testData.findSeat(seatId);
		
		System.out.printf(
				"[%2d회] 재점유=%s, 취소=%s, version=%d%n",
				repetition.getCurrentRepetition(),
				reholdResult,
				cancelResult,
				seat.version()
		);
		
		// 3. 둘 다 실패할 수 없다.
		assertThat(List.of(reholdResult, cancelResult)).contains(success);
		
		// 4. 데이터 정합성은 둘 중 누가 경합에서 이기던 항상 성립해야 한다.
		HoldRow a = testData.findHold(holdA);
		assertThat(a.status()).isEqualTo(HoldStatus.EXPIRED.name());
		assertThat(a.endedAt()).isEqualTo(assertEndedAt);
		
		int successCount = (reholdError == null ? 1 : 0) + (cancelError == null ? 1 : 0);
		assertThat(seat.version()).isEqualTo(1 + successCount);
		
		if (reholdError == null) {
			assertThat(seat.status()).isEqualTo(SeatStatus.HELD.name());
			assertThat(seat.currentHoldId()).isEqualTo(holdB);
			
			HoldRow b = testData.findHold(holdB);
			assertThat(b.status()).isEqualTo(HoldStatus.ACTIVE.name());
			assertThat(b.expiresAt()).isEqualTo(assertExpiresAt);
			
			assertThat(testData.countLiveHolds(seatId)).isOne();
		}
		
		else {
			assertThat(seat.status()).isEqualTo(SeatStatus.AVAILABLE.name());
			assertThat(seat.currentHoldId()).isNull();
			
			assertThat(testData.countLiveHolds(seatId)).isZero();
		}
		
		// 5. 예외 검증
		assertThat(reholdResult).isIn(
				success,
				SeatError.SEAT_HOLD_NOT_STATUS.getCode()
		);
		assertThat(cancelResult).isIn(
				success,
				SeatError.SEAT_HOLDING_NOT_FOUND.getCode(),
				SeatError.SEAT_HOLD_INVALID_STATE.getCode()
		);
	}
```

## 데드락 로그
```
== A. 요약 ==
이름: rehold-vs-cancel-01-cancel
오류: 데드락(40P01)

== B. 예외 체인 (바깥 → 안쪽) ==
[0]org.springframework.dao.CannotAcquireLockException
    could not execute statement [ERROR: deadlock detected
  세부 정보: Process 71 waits for ShareLock on transaction 755; blocked by process 70.
Process 70 waits for ShareLock on transaction 756; blocked by process 71.
  힌트: See server log for query details.
  위치: while updating tuple (0,1) in relation "seat_holds"] [update seat_reservation.seat_holds set ended_at=?,status=? where hold_id=?]; SQL [update seat_reservation.seat_holds set ended_at=?,status=? where hold_id=?]
[1]org.hibernate.exception.LockAcquisitionException
    could not execute statement [ERROR: deadlock detected
  세부 정보: Process 71 waits for ShareLock on transaction 755; blocked by process 70.
Process 70 waits for ShareLock on transaction 756; blocked by process 71.
  힌트: See server log for query details.
  위치: while updating tuple (0,1) in relation "seat_holds"] [update seat_reservation.seat_holds set ended_at=?,status=? where hold_id=?]
[2]org.postgresql.util.PSQLException
    ERROR: deadlock detected
  세부 정보: Process 71 waits for ShareLock on transaction 755; blocked by process 70.
Process 70 waits for ShareLock on transaction 756; blocked by process 71.
  힌트: See server log for query details.
  위치: while updating tuple (0,1) in relation "seat_holds"

== C. 이 구간의 서버로그 ==
2026-11-28 03:34:55.288 UTC [71] 00000 LOG:  statement: 
2026-11-28 03:34:55.288 UTC [70] 00000 LOG:  statement: BEGIN
2026-11-28 03:34:55.288 UTC [70] 00000 LOG:  execute <unnamed>: select ss1_0.seat_id,ss1_0.current_hold_id,ss1_0.current_reservation_id,ss1_0.grade,ss1_0.price,ss1_0.seat_number,ss1_0.session_id,ss1_0.status,ss1_0.version from seat_reservation.session_seats ss1_0 where ss1_0.seat_id=$1
2026-11-28 03:34:55.288 UTC [70] 00000 DETAIL:  Parameters: $1 = '5594455468126942502'
2026-11-28 03:34:55.338 UTC [71] 00000 LOG:  statement: BEGIN
2026-11-28 03:34:55.338 UTC [70] 00000 LOG:  execute <unnamed>: select sh1_0.hold_id,sh1_0.ended_at,sh1_0.expires_at,sh1_0.held_at,sh1_0.seat_id,sh1_0.status,sh1_0.user_id from seat_reservation.seat_holds sh1_0 where sh1_0.seat_id=$1 and sh1_0.status=$2
2026-11-28 03:34:55.338 UTC [70] 00000 DETAIL:  Parameters: $1 = '5594455468126942502', $2 = 'ACTIVE'
2026-11-28 03:34:55.339 UTC [71] 00000 LOG:  execute <unnamed>: select sh1_0.hold_id,sh1_0.ended_at,sh1_0.expires_at,sh1_0.held_at,sh1_0.seat_id,s1_0.seat_id,s1_0.current_hold_id,s1_0.current_reservation_id,s1_0.grade,s1_0.price,s1_0.seat_number,s1_0.session_id,s1_0.status,s1_0.version,sh1_0.status,sh1_0.user_id from seat_reservation.seat_holds sh1_0 join seat_reservation.session_seats s1_0 on s1_0.seat_id=sh1_0.seat_id where sh1_0.hold_id=$1 and sh1_0.user_id=$2 and sh1_0.status=$3
2026-11-28 03:34:55.339 UTC [71] 00000 DETAIL:  Parameters: $1 = '5dbf914f-65bd-483f-adb9-0c1d30292cef', $2 = '0549aaba-d03a-4353-a32e-1a67ab797611', $3 = 'ACTIVE'
2026-11-28 03:34:55.343 UTC [71] 00000 LOG:  execute <unnamed>: update seat_reservation.session_seats set current_hold_id=$1,current_reservation_id=$2,grade=$3,price=$4,seat_number=$5,session_id=$6,status=$7,version=$8 where seat_id=$9 and version=$10
2026-11-28 03:34:55.343 UTC [71] 00000 DETAIL:  Parameters: $1 = NULL, $2 = NULL, $3 = 'VIP', $4 = '10000', $5 = '1', $6 = '5594455468126942502', $7 = 'AVAILABLE', $8 = '2', $9 = '5594455468126942502', $10 = '1'
2026-11-28 03:34:55.343 UTC [70] 00000 LOG:  execute <unnamed>: update seat_reservation.seat_holds set ended_at=$1,status=$2 where hold_id=$3
2026-11-28 03:34:55.343 UTC [70] 00000 DETAIL:  Parameters: $1 = '2030-01-01 11:59:00', $2 = 'EXPIRED', $3 = '5dbf914f-65bd-483f-adb9-0c1d30292cef'
2026-11-28 03:34:55.344 UTC [71] 00000 LOG:  execute <unnamed>: update seat_reservation.seat_holds set ended_at=$1,status=$2 where hold_id=$3
2026-11-28 03:34:55.344 UTC [71] 00000 DETAIL:  Parameters: $1 = '2030-01-01 11:59:00', $2 = 'EXPIRED', $3 = '5dbf914f-65bd-483f-adb9-0c1d30292cef'
2026-11-28 03:34:55.344 UTC [70] 00000 LOG:  execute <unnamed>: select sh1_0.hold_id,sh1_0.ended_at,sh1_0.expires_at,sh1_0.held_at,sh1_0.seat_id,sh1_0.status,sh1_0.user_id from seat_reservation.seat_holds sh1_0 where sh1_0.hold_id=$1
2026-11-28 03:34:55.344 UTC [70] 00000 DETAIL:  Parameters: $1 = 'f3ffc394-5d15-4854-9a49-60285431a1f8'
2026-11-28 03:34:55.345 UTC [70] 00000 LOG:  execute <unnamed>: insert into seat_reservation.seat_holds (ended_at,expires_at,held_at,seat_id,status,user_id,hold_id) values ($1,$2,$3,$4,$5,$6,$7)
2026-11-28 03:34:55.345 UTC [70] 00000 DETAIL:  Parameters: $1 = NULL, $2 = '2030-01-01 12:05:00', $3 = '2030-01-01 12:00:00', $4 = '5594455468126942502', $5 = 'ACTIVE', $6 = '793a48c8-ca4a-43b9-80fe-9544c09a6c0d', $7 = 'f3ffc394-5d15-4854-9a49-60285431a1f8'
2026-11-28 03:34:55.346 UTC [70] 00000 LOG:  execute <unnamed>: update seat_reservation.session_seats set current_hold_id=$1,current_reservation_id=$2,grade=$3,price=$4,seat_number=$5,session_id=$6,status=$7,version=$8 where seat_id=$9 and version=$10
2026-11-28 03:34:55.346 UTC [70] 00000 DETAIL:  Parameters: $1 = 'f3ffc394-5d15-4854-9a49-60285431a1f8', $2 = NULL, $3 = 'VIP', $4 = '10000', $5 = '1', $6 = '5594455468126942502', $7 = 'HELD', $8 = '2', $9 = '5594455468126942502', $10 = '1'
2026-11-28 03:34:56.344 UTC [71] 40P01 ERROR:  deadlock detected
2026-11-28 03:34:56.344 UTC [71] 40P01 DETAIL:  Process 71 waits for ShareLock on transaction 755; blocked by process 70.
  Process 70 waits for ShareLock on transaction 756; blocked by process 71.
  Process 71: update seat_reservation.seat_holds set ended_at=$1,status=$2 where hold_id=$3
  Process 70: update seat_reservation.session_seats set current_hold_id=$1,current_reservation_id=$2,grade=$3,price=$4,seat_number=$5,session_id=$6,status=$7,version=$8 where seat_id=$9 and version=$10
2026-11-28 03:34:56.344 UTC [71] 40P01 HINT:  See server log for query details.
2026-11-28 03:34:56.344 UTC [71] 40P01 CONTEXT:  while updating tuple (0,1) in relation "seat_holds"
2026-11-28 03:34:56.344 UTC [71] 40P01 STATEMENT:  update seat_reservation.seat_holds set ended_at=$1,status=$2 where hold_id=$3
2026-11-28 03:34:56.346 UTC [70] 00000 LOG:  execute S_1: COMMIT
2026-11-28 03:34:56.355 UTC [71] 00000 LOG:  execute S_1: ROLLBACK
2026-11-28 03:34:56.361 UTC [70] 00000 LOG:  execute <unnamed>: SELECT status, current_hold_id, version
  FROM seat_reservation.session_seats
  WHERE seat_id = $1
  
2026-11-28 03:34:56.361 UTC [70] 00000 DETAIL:  Parameters: $1 = '5594455468126942502'


```