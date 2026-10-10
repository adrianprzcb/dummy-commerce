CREATE TABLE dlt_replays (
    dlt_topic VARCHAR(255) NOT NULL,
    partition_id INTEGER NOT NULL CHECK (partition_id >= 0),
    record_offset BIGINT NOT NULL CHECK (record_offset >= 0),
    replayed_at TIMESTAMPTZ,
    PRIMARY KEY (dlt_topic, partition_id, record_offset)
);
