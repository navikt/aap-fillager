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
    tittel               TEXT DEFAULT NULL,
    opprettet            TIMESTAMP NOT NULL,
    fil                  BYTEA,
    CONSTRAINT unique_filreferanse UNIQUE (filreferanse)
);

CREATE TABLE innsending_fil
(
    filreferanse            UUID REFERENCES fil (filreferanse) ON DELETE CASCADE,
    innsendingsreferanse    UUID REFERENCES innsending (innsendingsreferanse)
);



