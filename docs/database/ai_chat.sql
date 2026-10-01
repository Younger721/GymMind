-- Role-scoped multi-turn AI chat persistence.
-- Apply once in production when schema auto-update is disabled.
CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  version BIGINT NOT NULL DEFAULT 0,
  tenant_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  role_context VARCHAR(32) NOT NULL,
  session_key VARCHAR(128) NOT NULL,
  title VARCHAR(160) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  archived BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_chat_session_scope (tenant_id, user_id, role_context, session_key),
  KEY idx_ai_chat_session_scope (tenant_id, user_id, role_context, updated_at)
);

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  version BIGINT NOT NULL DEFAULT 0,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content VARCHAR(12000) NOT NULL,
  sequence_no BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_chat_message_sequence (session_id, sequence_no),
  KEY idx_ai_chat_message_session (session_id, sequence_no),
  CONSTRAINT fk_ai_chat_message_session FOREIGN KEY (session_id) REFERENCES ai_chat_session(id)
);
