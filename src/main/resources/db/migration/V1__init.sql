CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    raw_xml TEXT NOT NULL
);

CREATE TABLE risk_analysis (
    id UUID PRIMARY KEY,
    score DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL
);