-- give access to IAM users (GCP)
GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;

CREATE TABLE innsending
(
    innsendingsreferanse UUID PRIMARY KEY,
    opprettet            TIMESTAMP NOT NULL
);

CREATE TABLE fil
(
    filreferanse         UUID PRIMARY KEY,
    innsendingsreferanse UUID REFERENCES innsending (innsendingsreferanse) ON DELETE CASCADE,
    tittel               TEXT      NOT NULL,
    opprettet            TIMESTAMP NOT NULL,
    fil                  BYTEA
);

