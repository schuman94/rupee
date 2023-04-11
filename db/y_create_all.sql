
-- tables
\i cath_domain.sql
\i cath_grams.sql
\i cath_hashes.sql
\i cath_name.sql
\i chain_grams.sql
\i chain_hashes.sql
\i chain.sql
\i dir_chain.sql
\i dir_grams.sql
\i dir_hashes.sql
\i scop_domain.sql
\i scop_grams.sql
\i scop_hashes.sql
\i scop_name.sql
\i upload_grams.sql
\i upload_hashes.sql

-- alter sequences
ALTER SEQUENCE cath_domain_cath_sid_seq CYCLE;
ALTER SEQUENCE chain_chain_sid_seq CYCLE;
ALTER SEQUENCE dir_chain_db_sid_seq CYCLE;
ALTER SEQUENCE scop_domain_scop_sid_seq CYCLE;
ALTER SEQUENCE upload_grams_upload_id_seq CYCLE;

-- functions
\i get_cath_augmented_results.sql
\i get_cath_band_matches.sql
\i get_cath_grams_split.sql
\i get_cath_grams.sql
\i get_cath_hashes.sql
\i get_cath_split_matches.sql
\i get_cath_split.sql
\i get_chain_augmented_results.sql
\i get_chain_band_matches.sql
\i get_chain_grams_split.sql
\i get_chain_grams.sql
\i get_chain_hashes.sql
\i get_chain_split_matches.sql
\i get_chain_split.sql
\i get_dir_augmented_results.sql
\i get_dir_band_matches.sql
\i get_dir_grams_split.sql
\i get_dir_grams.sql
\i get_dir_hashes.sql
\i get_dir_split_matches.sql
\i get_dir_split.sql
\i get_probabilities.sql
\i get_scop_augmented_results.sql
\i get_scop_band_matches.sql
\i get_scop_grams_split.sql
\i get_scop_grams.sql
\i get_scop_hashes.sql
\i get_scop_split_matches.sql
\i get_scop_split.sql
\i get_upload_grams.sql
\i get_upload_hashes.sql
\i insert_cath_grams.sql
\i insert_cath_hashes.sql
\i insert_chain_grams.sql
\i insert_chain_hashes.sql
\i insert_dir_chains.sql
\i insert_dir_grams.sql
\i insert_dir_hashes.sql
\i insert_scop_grams.sql
\i insert_scop_hashes.sql
\i insert_upload_grams.sql
\i insert_upload_hashes.sql

