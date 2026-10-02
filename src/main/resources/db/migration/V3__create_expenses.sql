CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by BIGINT NOT NULL REFERENCES users(id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    description VARCHAR(240) NOT NULL,
    category VARCHAR(32) NOT NULL,
    split_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (category IN ('FOOD', 'GROCERIES', 'RENT', 'UTILITIES', 'TRANSPORT', 'ENTERTAINMENT', 'OTHER')),
    CHECK (split_type IN ('EQUAL', 'CUSTOM'))
);

CREATE TABLE expense_participants (
    expense_id BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    share_minor BIGINT NOT NULL CHECK (share_minor > 0),
    PRIMARY KEY (expense_id, user_id)
);

CREATE INDEX idx_expenses_group_created_at ON expenses(group_id, created_at DESC);
CREATE INDEX idx_expense_participants_user_id ON expense_participants(user_id);
