ALTER TABLE data_change_histories ADD COLUMN IF NOT EXISTS request_id varchar(100);
COMMENT ON COLUMN data_change_histories.request_id IS 'EvaluationAreaService 등 업무 서비스 저장 요청의 X-Request-Id 또는 서버 생성 요청 식별자';
CREATE INDEX IF NOT EXISTS idx_data_change_histories_request_id ON data_change_histories(request_id);
