-- give access to IAM users (GCP)
GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;

CREATE TABLE fil(
    filreferanse            UUID NOT NULL,
    tittel                  TEXT NOT NULL,
    opprettet               TIMESTAMP NOT NULL,
    fil_base64              TEXT,
    CONSTRAINT UNIQUE (filreferanse)
);

