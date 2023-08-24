-- give access to IAM users (GCP)
GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;

CREATE TABLE innsending
(
    innsendingsreferanse UUID,
    opprettet            TIMESTAMP NOT NULL,
    CONSTRAINT unique_innsendingsreferanse UNIQUE (innsendingsreferanse)
);

CREATE TABLE fil
(
    filreferanse         UUID,
    innsendingsreferanse UUID REFERENCES innsending (innsendingsreferanse) ON DELETE CASCADE,
    tittel               TEXT      NOT NULL,
    opprettet            TIMESTAMP NOT NULL,
    fil                  BYTEA,
    CONSTRAINT unique_filreferanse UNIQUE (filreferanse)
);

