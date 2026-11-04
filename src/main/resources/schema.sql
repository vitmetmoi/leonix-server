-- LeoNix Database Schema Initialization
-- Compatible with PostgreSQL 14+ and H2 Database

-- 1. USERS & AUTHENTICATION
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    hashed_key VARCHAR(255) NOT NULL UNIQUE,
    last_used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_user ON user_api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_hashed ON user_api_keys(hashed_key);

CREATE TABLE IF NOT EXISTS user_llm_configs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    base_url VARCHAR(255) NOT NULL DEFAULT 'https://api.openai.com/v1',
    api_key_encrypted VARCHAR(500) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_llm_configs_user ON user_llm_configs(user_id);

-- 2. WALLET & PAYMENTS
CREATE TABLE IF NOT EXISTS wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    reserved DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wallets_user ON wallets(user_id);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    amount_vnd DECIMAL(15, 2) NOT NULL,
    credits DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'VIETQR',
    transfer_content VARCHAR(50) NOT NULL UNIQUE,
    bank_code VARCHAR(20) DEFAULT 'TPBank',
    account_number VARCHAR(50),
    account_name VARCHAR(100),
    qr_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_transaction_id VARCHAR(100),
    paid_at TIMESTAMP,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_transfer_content ON payments(transfer_content);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    reference_type VARCHAR(30),
    reference_id VARCHAR(64),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_ref ON wallet_transactions(reference_type, reference_id);

-- 3. AI GATEWAY: DISPATCH GROUPS & UPSTREAM CURL ENDPOINTS (FAILOVER ENGINE)
CREATE TABLE IF NOT EXISTS ai_dispatch_groups (
    id BIGSERIAL PRIMARY KEY,
    group_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    strategy VARCHAR(30) NOT NULL DEFAULT 'PRIORITY_FAILOVER',
    failover_status_codes VARCHAR(100) NOT NULL DEFAULT '429,500,502,503,504',
    max_retries_per_upstream INT NOT NULL DEFAULT 1,
    timeout_ms INT NOT NULL DEFAULT 30000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_upstream_endpoints (
    id BIGSERIAL PRIMARY KEY,
    dispatch_group_id BIGINT NOT NULL REFERENCES ai_dispatch_groups(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    priority INT NOT NULL DEFAULT 1,
    weight INT NOT NULL DEFAULT 100,
    endpoint_url VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT 'POST',
    headers_template TEXT NOT NULL,
    api_key_encrypted VARCHAR(500),
    request_body_template TEXT NOT NULL,
    response_mapping TEXT NOT NULL,
    polling_url_template VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    health_status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_failed_at TIMESTAMP,
    last_error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_upstream_group_prio ON ai_upstream_endpoints(dispatch_group_id, priority ASC);
CREATE INDEX IF NOT EXISTS idx_upstream_status ON ai_upstream_endpoints(is_active, health_status);

-- 4. AI MODELS & CAPABILITIES
CREATE TABLE IF NOT EXISTS ai_models (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    service_type VARCHAR(20) NOT NULL,
    capabilities TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_maintenance BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_models_type ON ai_models(service_type, is_active);

CREATE TABLE IF NOT EXISTS ai_capabilities (
    id BIGSERIAL PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL REFERENCES ai_models(id) ON DELETE CASCADE,
    capability_code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    supported_resolutions VARCHAR(255),
    supported_durations VARCHAR(255),
    supported_aspect_ratios VARCHAR(255),
    dispatch_group_id BIGINT NOT NULL REFERENCES ai_dispatch_groups(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_model_capability UNIQUE(model_id, capability_code)
);
CREATE INDEX IF NOT EXISTS idx_ai_cap_dispatch ON ai_capabilities(dispatch_group_id);

CREATE TABLE IF NOT EXISTS ai_model_pricing (
    id BIGSERIAL PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL REFERENCES ai_models(id) ON DELETE CASCADE,
    capability_code VARCHAR(30) NOT NULL,
    resolution VARCHAR(20),
    duration VARCHAR(20),
    price_unit VARCHAR(30) NOT NULL DEFAULT 'PER_CALL',
    sell_price DECIMAL(12, 2) NOT NULL,
    cost_price DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_pricing_query ON ai_model_pricing(model_id, capability_code, resolution, duration);

-- 5. WORKSPACE: PROJECTS & PROMPTS
CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    kind VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    cover_url VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_projects_user ON projects(user_id);

CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    service_type VARCHAR(20) NOT NULL,
    prompt_vi TEXT NOT NULL,
    prompt_en TEXT,
    preview_media_url VARCHAR(500),
    tags VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    likes_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_prompts_category ON prompt_templates(category, service_type);

-- 6. AI JOBS & MEDIA LIBRARY
CREATE TABLE IF NOT EXISTS ai_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_uuid VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    model_id VARCHAR(100) NOT NULL REFERENCES ai_models(id),
    service_type VARCHAR(20) NOT NULL,
    input_mode VARCHAR(20) NOT NULL,
    prompt TEXT NOT NULL,
    negative_prompt TEXT,
    params TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progress INT NOT NULL DEFAULT 0,
    cost_credits DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    output_media_url VARCHAR(500),
    error_message TEXT,
    worker_task_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_user_status ON ai_jobs(user_id, status);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_project ON ai_jobs(project_id);

CREATE TABLE IF NOT EXISTS job_reference_media (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES ai_jobs(id) ON DELETE CASCADE,
    media_type VARCHAR(30) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_job_ref_media ON job_reference_media(job_id);

CREATE TABLE IF NOT EXISTS media_assets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    job_id BIGINT REFERENCES ai_jobs(id) ON DELETE SET NULL,
    media_type VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    duration_seconds INT,
    width INT,
    height INT,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_media_assets_user ON media_assets(user_id, media_type);
CREATE INDEX IF NOT EXISTS idx_media_assets_project ON media_assets(project_id);
