-- Only4_Backend
유레카 3기 종합 프로젝트
🎯 목적: 당월 요금 청구서 발송 이벤트 발행
- 생성된 요금 청구서 발송 이벤트를 Kafka로 발행
    - 발송 완료 처리되지 않은 요금 청구서 대상
    
📌 전제: 요금 청구서 및 청구 항목 데이터가 DB에 존재
👉 트리거: 매일 특정 시간 or 관리자 버튼 클릭 or 등록된 예약 발송 일시

Job: 회원별 요금 청구서 알림 발송 요청 생성
Step 1: 알림 대상 추출 및 적재

Reader: 해당 날짜 & 금지시간이 아닌 시간 & 아직 발송되지 않은 메시지를 받고 싶은 회원 및 해당 회원의 청구서 선별
Processor: Reader에서 조회한 청구서 데이터를 알림 발송 대기 상태로 변환
Writer: 필터링을 통과한 대상만 BillNotification 테이블에 상태 PENDING으로 저장

Step 2: Kafka에 알림 발송 요청 메시지 생성 및 이벤트 발행

Reader: BillNotification 테이블에서 발송 대기(PENDING) 상태인 데이터를 조회
Processor: 내부 DB 엔티티를 외부 시스템 전송용 DTO로 변환
Writer:
    - Kafka Producer: notification-topic으로 알림 이벤트를 비동기 전송
    - DB Updater: 전송 성공한 건에 한해 상태를 PUBLISHED로 변경하여 중복 발송을 방지

현재 방식 선택 이유
Step 1: 누구에게 보낼 것인가?(Targeting)에만 집중하여 정합성을 확보

Step 2: 어떻게 보낼 것인가?(Delivery)에만 집중하여 전송 안정성을 확보
