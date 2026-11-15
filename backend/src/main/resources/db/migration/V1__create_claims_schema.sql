-- Core claims schema: users, policies, claims, the status history and information requests.

CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(200) NOT NULL,
    role          VARCHAR(30)  NOT NULL CHECK (role IN ('CLAIMANT', 'CLAIMS_OFFICER', 'CLAIMS_MANAGER'))
);

CREATE TABLE policy (
    id               UUID PRIMARY KEY,
    policy_number    VARCHAR(30)    NOT NULL UNIQUE,
    holder_id        UUID           NOT NULL REFERENCES app_user (id),
    line_of_business VARCHAR(20)    NOT NULL CHECK (line_of_business IN ('MOTOR', 'PROPERTY')),
    country_code     CHAR(2)        NOT NULL,
    currency         CHAR(3)        NOT NULL,
    sum_insured      NUMERIC(14, 2) NOT NULL CHECK (sum_insured > 0),
    cover_start      DATE           NOT NULL,
    cover_end        DATE           NOT NULL,
    CHECK (cover_end >= cover_start)
);

CREATE INDEX idx_policy_holder ON policy (holder_id);

CREATE SEQUENCE claim_number_seq START WITH 1;

CREATE TABLE claim (
    id                  UUID PRIMARY KEY,
    claim_number        VARCHAR(30)    NOT NULL UNIQUE,
    policy_id           UUID           NOT NULL REFERENCES policy (id),
    claimant_id         UUID           NOT NULL REFERENCES app_user (id),
    assigned_officer_id UUID REFERENCES app_user (id),
    incident_date       DATE           NOT NULL,
    incident_location   VARCHAR(200),
    description         VARCHAR(2000)  NOT NULL,
    claimed_amount      NUMERIC(14, 2) NOT NULL CHECK (claimed_amount > 0),
    reserve_amount      NUMERIC(14, 2) NOT NULL CHECK (reserve_amount >= 0),
    approved_amount     NUMERIC(14, 2),
    currency            CHAR(3)        NOT NULL,
    status              VARCHAR(20)    NOT NULL
        CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'INFO_REQUESTED', 'APPROVED', 'REJECTED', 'SETTLED')),
    decision_reason     VARCHAR(1000),
    submitted_at        TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    version             BIGINT         NOT NULL DEFAULT 0
);

-- The three ways claims are listed: a claimant's own claims, an officer's work, and the queue by status.
CREATE INDEX idx_claim_claimant ON claim (claimant_id);
CREATE INDEX idx_claim_assigned_officer ON claim (assigned_officer_id);
CREATE INDEX idx_claim_status_submitted_at ON claim (status, submitted_at);

CREATE TABLE claim_status_history (
    id          UUID PRIMARY KEY,
    claim_id    UUID        NOT NULL REFERENCES claim (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    changed_by  UUID        NOT NULL REFERENCES app_user (id),
    note        VARCHAR(1000),
    changed_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_claim_status_history_claim ON claim_status_history (claim_id, changed_at);

CREATE TABLE information_request (
    id           UUID PRIMARY KEY,
    claim_id     UUID          NOT NULL REFERENCES claim (id),
    question     VARCHAR(1000) NOT NULL,
    requested_by UUID          NOT NULL REFERENCES app_user (id),
    requested_at TIMESTAMPTZ   NOT NULL,
    response     VARCHAR(2000),
    responded_at TIMESTAMPTZ
);

CREATE INDEX idx_information_request_claim ON information_request (claim_id, requested_at);
