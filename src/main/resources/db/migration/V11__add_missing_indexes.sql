
CREATE INDEX IF NOT EXISTS idx_audit_action_actor_time
    ON audit_logs (action, actor_id, occurred_at DESC);
