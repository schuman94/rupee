
CREATE TABLE mtm_dom_result_matched
(
    version VARCHAR NOT NULL,
    n INTEGER NOT NULL,
    db_id_1 VARCHAR NOT NULL,
    db_id_2 VARCHAR NOT NULL,
    mtm_rmsd NUMERIC NOT NULL,
    mtm_tm_score NUMERIC NOT NULL,
    ce_rmsd NUMERIC NULL DEFAULT 0,
    ce_tm_score NUMERIC NULL DEFAULT -1
); 

CREATE UNIQUE INDEX idx_mtm_dom_result_matched_unique ON mtm_dom_result_matched (version, n, db_id_1);
