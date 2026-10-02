ALTER TABLE sys_tenant_quota
    ADD COLUMN max_ai_tokens_per_month BIGINT NOT NULL DEFAULT 1000000;

ALTER TABLE ai_usage_record
    MODIFY COLUMN input_tokens INT NULL,
    MODIFY COLUMN output_tokens INT NULL,
    ADD COLUMN total_tokens INT NULL,
    ADD COLUMN provider VARCHAR(32) NULL,
    ADD COLUMN feature VARCHAR(64) NULL,
    ADD COLUMN usage_source VARCHAR(32) NULL,
    ADD COLUMN estimated_cost DECIMAL(18, 6) NULL,
    ADD COLUMN currency VARCHAR(8) NULL;

CREATE TABLE ai_model_pricing (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(100) NOT NULL,
    input_price_per_million DECIMAL(18, 8) NOT NULL,
    output_price_per_million DECIMAL(18, 8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_model_pricing_provider_model UNIQUE (provider, model)
);

CREATE INDEX idx_ai_usage_model_created ON ai_usage_record (model, created_at);
CREATE INDEX idx_ai_usage_status_created ON ai_usage_record (status, created_at);
