
CREATE OR REPLACE FUNCTION get_chain_band_matches (
    p_search_type INTEGER, 
    p_db_id VARCHAR, 
    p_upload_id INTEGER,
    p_band_index INTEGER
)
RETURNS TABLE (
    db_id VARCHAR,
    pdb_id VARCHAR, 
    sort_key VARCHAR,
    min_hashes INTEGER ARRAY,
    band_hashes INTEGER ARRAY
)
AS $$
    DECLARE band_value INTEGER;
BEGIN

    -- get band value of query
    IF p_upload_id = -1 THEN
    
        IF p_search_type = 1 THEN
            
            SELECT
                h.band_hashes[p_band_index]
            INTO
                band_value
            FROM
                scop_hashes h
            WHERE
                h.db_id = p_db_id;

        ELSIF p_search_type = 2 THEN
            
            SELECT
                h.band_hashes[p_band_index]
            INTO
                band_value
            FROM
                cath_hashes h
            WHERE
                h.db_id = p_db_id;
        
        ELSIF p_search_type = 3 THEN

            SELECT
                h.band_hashes[p_band_index]
            INTO
                band_value
            FROM
                ecod_hashes h
            WHERE
                h.db_id = p_db_id;

        ELSIF p_search_type = 4 THEN

            SELECT
                h.band_hashes[p_band_index]
            INTO
                band_value
            FROM
                chain_hashes h
            WHERE
                h.db_id = p_db_id;

        ELSIF p_search_type = 5 THEN

            SELECT
                h.band_hashes[p_band_index]
            INTO
                band_value
            FROM
                afdb_hashes h
            WHERE
                h.db_id = p_db_id;
        END IF;

    ELSE -- UPLOAD

        SELECT
            h.band_hashes[p_band_index]
        INTO
            band_value
        FROM
            upload_hashes h
        WHERE
            h.upload_id = p_upload_id;

    END IF;

    RETURN QUERY
    SELECT 
        h.db_id,
        c.pdb_id,
        c.sort_key,
        h.min_hashes,
        h.band_hashes
    FROM
        chain c
        INNER JOIN chain_hashes h
            ON h.db_id = c.chain_id
    WHERE  
        h.band_hashes[p_band_index] = band_value;

END;
$$LANGUAGE plpgsql;


