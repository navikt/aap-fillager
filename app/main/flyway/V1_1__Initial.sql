-- give access to IAM users (GCP)
GRANT ALL ON ALL TABLES IN SCHEMA PUBLIC TO cloudsqliamuser;

CREATE TABLE filopplasting(
    id              UUID NOT NULL,
    dato            TIMESTAMP NOT NULL,
    filreferanse    UUID NOT NULL,
    fil             BLOB
);