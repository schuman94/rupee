
DO $$

    DECLARE p_benchmark VARCHAR := 'casp_cathedral_d247'; 
    DECLARE p_version VARCHAR := 'casp_cath_v4_2_0'; 
    DECLARE p_limit INTEGER := 100; 
    DECLARE p_sort_by INTEGER := 5;

BEGIN

    DROP TABLE IF EXISTS figure_table;
   
    CREATE TABLE figure_table AS 
        WITH rupee_all_aligned AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'all_aligned', 'full_length', p_sort_by, p_limit)
        ),
        rupee_top_aligned AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'top_aligned', 'ssap_score', p_sort_by, p_limit)
        ),
        cathedral AS
        (   
            SELECT * FROM get_cathedral_results(p_benchmark, p_version, p_sort_by, p_limit) 
        ),
        ranked AS
        (
            SELECT 
                n,
                'RUPEE All-Aligned' AS app,
                db_id_1,
                CASE 
                    WHEN p_sort_by = 1 THEN ce_rmsd 
                    WHEN p_sort_by = 2 THEN fatcat_rigid_rmsd
                    WHEN p_sort_by = 3 THEN tm_avg_tm_score
                    WHEN p_sort_by = 4 THEN tm_q_tm_score
                    ELSE ssap_score
                END AS score
            FROM
                rupee_all_aligned
            UNION ALL
            SELECT 
                n,
                'RUPEE Top-Aligned' AS app,
                db_id_1,
                CASE 
                    WHEN p_sort_by = 1 THEN ce_rmsd 
                    WHEN p_sort_by = 2 THEN fatcat_rigid_rmsd
                    WHEN p_sort_by = 3 THEN tm_avg_tm_score
                    WHEN p_sort_by = 4 THEN tm_q_tm_score
                    ELSE ssap_score
                END AS score
            FROM
                rupee_top_aligned
            UNION ALL
            SELECT 
                n,
                'CATHEDRAL' AS app,
                db_id_1,
                CASE 
                    WHEN p_sort_by = 1 THEN ce_rmsd 
                    WHEN p_sort_by = 2 THEN fatcat_rigid_rmsd
                    WHEN p_sort_by = 3 THEN tm_avg_tm_score
                    WHEN p_sort_by = 4 THEN tm_q_tm_score
                    ELSE ssap_score
                END AS score
            FROM
                cathedral
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

