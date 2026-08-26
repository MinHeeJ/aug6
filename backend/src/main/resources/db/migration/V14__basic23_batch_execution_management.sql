ALTER TABLE batch_executions ADD COLUMN IF NOT EXISTS execution_parameter_json jsonb NOT NULL DEFAULT '{}'::jsonb;
COMMENT ON COLUMN batch_executions.execution_parameter_json IS 'BatchExecutionService.createBatchExecution/createBatchRerun 처리 시 실행 요청 파라미터로 저장';

UPDATE batch_executions
SET execution_parameter_json = '{"year":2026}'::jsonb
WHERE execution_id = 'SEED-BATCH-EXEC-001'
  AND execution_parameter_json = '{}'::jsonb;
