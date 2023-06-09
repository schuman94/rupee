
CREATE OR REPLACE FUNCTION get_upload_grams (p_upload_id INTEGER)
RETURNS TABLE (
    grams INTEGER ARRAY,
    coords REAL ARRAY
)
AS $$
BEGIN

    RETURN QUERY
    SELECT 
        g.grams,
        g.coords
    FROM
        upload_grams g
    WHERE
        g.upload_id = p_upload_id;

END;
$$LANGUAGE plpgsql;


