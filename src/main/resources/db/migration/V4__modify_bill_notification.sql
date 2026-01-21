-- 1. 새로운 ENUM 타입 정의
-- publish_status용 ENUM
DO $$ BEGIN
    CREATE TYPE publish_status_enum AS ENUM ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- send_status용 ENUM
DO $$ BEGIN
    CREATE TYPE send_status_enum AS ENUM ('PENDING', 'SENDING', 'SENT', 'FAILED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;


-- 2. [Bill 테이블] send_status 컬럼 삭제
-- ALTER TABLE bill
--  DROP COLUMN IF EXISTS send_status;


-- 3. [Bill_Notification 테이블] send_status 컬럼 삭제
ALTER TABLE bill_notification
    DROP COLUMN IF EXISTS send_status;


-- 4. [Bill_Notification 테이블] 컬럼 4개 일괄 생성
-- (publish_status, send_status, member_id, process_start_time)
ALTER TABLE bill_notification
    ADD COLUMN publish_status publish_status_enum NOT NULL DEFAULT 'PENDING',
    ADD COLUMN send_status send_status_enum NOT NULL DEFAULT 'PENDING',
    ADD COLUMN member_id BIGINT,              -- Java Long 대응 (기존 데이터 호환을 위해 Nullable로 생성)
    ADD COLUMN process_start_time TIMESTAMP;  -- Java LocalDateTime 대응 (선점용 시간)


-- 5. (선택사항) 더 이상 사용하지 않는 과거 ENUM 타입 삭제
DROP TYPE IF EXISTS bill_notification_status_enum;
