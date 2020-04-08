
DO $$

    DECLARE p_benchmark VARCHAR := 'casp_mtm_d246'; 
    DECLARE p_version VARCHAR := 'casp_chain_v01_01_2020'; 
    DECLARE p_search_type VARCHAR := 'contained_in'; -- contained_in, rmsd (NOTE: I have not collected this data for rmsd)
    DECLARE p_sort_by INTEGER := 3; -- 3 (tm_q_tm_score), 5 (tm_rmsd)
    DECLARE p_limit INTEGER := 100; 

BEGIN

    DROP TABLE IF EXISTS figure_table;
   
    CREATE TABLE figure_table AS 
        WITH rupee_all_aligned AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'all_aligned', p_search_type, p_sort_by, p_limit)
        ),
        rupee_top_aligned AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'top_aligned', p_search_type, p_sort_by, p_limit)
        ),
        mtm AS
        (   
            SELECT * FROM get_mtm_results(p_benchmark, p_version, p_sort_by, p_limit) 
        ),
        ranked AS
        (
            SELECT 
                n,
                'RUPEE All-Aligned' AS app,
                db_id_1,
                CASE 
                    WHEN p_sort_by = 3 THEN tm_q_tm_score 
                    WHEN p_sort_by = 5 THEN tm_rmsd
                END AS score
            FROM
                rupee_all_aligned
            UNION ALL
            SELECT 
                n,
                'RUPEE Top-Aligned' AS app,
                db_id_1,
                CASE 
                    WHEN p_sort_by = 3 THEN tm_q_tm_score 
                    WHEN p_sort_by = 5 THEN tm_rmsd
                END AS score
            FROM
                rupee_top_aligned
            UNION ALL
            SELECT 
                n,
                'mTM' AS app,
                db_id_1,
                CASE 
                    WHEN p_sort_by = 3 THEN tm_q_tm_score 
                    WHEN p_sort_by = 5 THEN tm_rmsd
                END AS score
            FROM
                mtm
        ),
        accumulated AS
        (
            SELECT 
                n,
                app,
                AVG(score) OVER (PARTITION BY app, db_id_1 ORDER BY n ROWS UNBOUNDED PRECEDING) AS cume_score
            FROM
                ranked
        ),
        averaged AS 
        (
            SELECT
                n,
                app,
                AVG(cume_score) AS avg_cume_score
            FROM
                accumulated 
            GROUP BY
                n,
                app
        )
        SELECT
            *
        FROM
            averaged
        ORDER BY
            app,
            n;

END $$;
    
SELECT * FROM figure_table;

