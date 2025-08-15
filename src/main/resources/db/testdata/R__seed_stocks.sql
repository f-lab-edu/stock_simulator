-- ================================
-- R__seed_baseline.sql
-- 기준 데이터 시드 (PK 충돌 시 UPDATE로 멱등 보장)
-- 파일 내용 변경 시 Flyway가 재실행
-- ================================

-- 0) (선택) 카테고리 예시 - stocks.stock_category_id와 FK는 없지만 UNIQUE(category_name) 충족 예시
--    이미 카테고리가 존재하면 아래 INSERT는 실패할 수 있으므로 필요 시 ON DUPLICATE 패턴 사용
-- INSERT INTO stock_categories (id, category_name)
-- VALUES (1, 'TECH')
-- ON DUPLICATE KEY UPDATE
--   category_name = VALUES(category_name);

-- 1) USERS (PK=id)
INSERT INTO users (id, user_id, name, email, location, phone_number, created_at, updated_at)
VALUES
  (1, 'buyerA', 'buyerName1', 'buyerA@example.com', 'Seoul', '010-0000-0000', NOW(), NOW()),
  (2, 'sellerA', 'sellerName1', 'sellerA@example.com', 'Suwon', '010-1234-5678', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  user_id      = VALUES(user_id),
  name         = VALUES(name),
  email        = VALUES(email),
  location     = VALUES(location),
  phone_number = VALUES(phone_number),
  updated_at   = VALUES(updated_at);

-- 2) ACCOUNTS (FK: accounts.user_id -> users.id, UNIQUE: account_number)
INSERT INTO accounts (id, user_id, is_active, account_number, bank_name, user_name, money_value, created_at, updated_at)
VALUES
  (1, 1, b'1', '111-222-333333', 'K-BANK', 'buyerName1', 10000000, NOW(), NOW()),
  (2, 2, b'1', '444-555-666666', 'NongHyup', 'sellerName1', 10000000, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  user_id        = VALUES(user_id),
  is_active      = VALUES(is_active),
  account_number = VALUES(account_number),
  bank_name      = VALUES(bank_name),
  user_name      = VALUES(user_name),
  money_value    = VALUES(money_value),
  updated_at     = VALUES(updated_at);

-- 3) PORTFOLIOS (FK: portfolios.user_id -> users.id, UNIQUE: (user_id, portfolio_type))
INSERT INTO portfolios (id, user_id, portfolio_type, available_cash, reserved_cash, portfolio_total_value, created_at, updated_at)
VALUES
  (1, 1, 'STOCK', 0, 0, 0, NOW(), NOW()),
  (2, 2, 'STOCK', 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  user_id               = VALUES(user_id),
  portfolio_type        = VALUES(portfolio_type),
  available_cash        = VALUES(available_cash),
  reserved_cash         = VALUES(reserved_cash),
  portfolio_total_value = VALUES(portfolio_total_value),
  updated_at            = VALUES(updated_at);

-- 4) STOCKS (UNIQUE: stock_code, stock_name)
--    stock_category_id는 현재 FK 없음 → NULL 유지 (필요 시 위 0)에서 넣은 카테고리 id 사용)
INSERT INTO stocks (id, stock_code, stock_name, stock_price, available_quantity, stock_category_id)
VALUES
  (1, '005930', '삼성전자', 100000, 1000000, NULL),
  (2, '035420', '네이버',    90000, 1000000, NULL)
ON DUPLICATE KEY UPDATE
  stock_code         = VALUES(stock_code),
  stock_name         = VALUES(stock_name),
  stock_price        = VALUES(stock_price),
  available_quantity = VALUES(available_quantity),
  stock_category_id  = VALUES(stock_category_id);

-- 5) PORTFOLIO_STOCKS (보유 종목 1건 생성)
-- PK: id (AUTO_INCREMENT), FK: portfolio_id, stock_id
-- money_value: 총 평가금액(평단가 × 수량), reserved_quantity: 예약된 수량
INSERT INTO portfolio_stocks (
    id,
    last_updated_at,
    money_value,
    portfolio_id,
    portfolio_quantity,
    reserved_quantity,
    stock_id
)
VALUES
    -- 포트폴리오 #2가 삼성전자(주식 id=1) 10주 보유, 예약수량 0, 평가금액 1,000,000원
    (1, NOW(6), 1000000, 2, 10, 0, 1)
ON DUPLICATE KEY UPDATE
    last_updated_at   = VALUES(last_updated_at),
    money_value       = VALUES(money_value),
    portfolio_quantity = VALUES(portfolio_quantity),
    reserved_quantity = VALUES(reserved_quantity),
    stock_id          = VALUES(stock_id);
