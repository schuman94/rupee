
TRUNCATE eu_neff;

COPY eu_neff (target, domain, neff) FROM '/home/ayoub/git/rupee/data/casp/eu_neff.txt' WITH (DELIMITER ',');

