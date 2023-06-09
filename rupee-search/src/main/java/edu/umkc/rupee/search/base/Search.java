package edu.umkc.rupee.search.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.postgresql.ds.PGSimpleDataSource;

import edu.umkc.rupee.search.bio.Parser;
import edu.umkc.rupee.search.defs.DbType;
import edu.umkc.rupee.search.defs.SearchBy;
import edu.umkc.rupee.search.defs.SearchMode;
import edu.umkc.rupee.search.defs.SearchType;
import edu.umkc.rupee.search.defs.SortBy;
import edu.umkc.rupee.search.lib.Constants;
import edu.umkc.rupee.search.lib.Db;
import edu.umkc.rupee.search.lib.FileUtils;
import edu.umkc.rupee.search.lib.Grams;
import edu.umkc.rupee.search.lib.Hashes;
import edu.umkc.rupee.search.lib.LCS;
import edu.umkc.rupee.search.lib.Similarity;
import edu.umkc.rupee.tm.Kabsch;
import edu.umkc.rupee.tm.KabschTLS;
import edu.umkc.rupee.tm.TmAlign;
import edu.umkc.rupee.tm.TmMode;
import edu.umkc.rupee.tm.TmResult;

// ASSUMPTIONS: 
// 1. if search mode == FAST sort by must be SIMILARITY
// 2. if search mode != FAST and search type == RMSD sort by must be RMSD
// 3. if search mode != FAST and search type != RMSD sort by must be TM_SCORE

public abstract class Search {

    private static int INITIAL_FILTER = 40000;
    private static int FINAL_FILTER = 8000;
    private static int MAX_LIMIT = 1000;
    private static int LCS_ALTERNATES = 10;

    // *********************************************************************
    // Abstract Methods 
    // *********************************************************************
    
    public abstract DbType getDbType();

    public abstract PreparedStatement getSplitSearchStatement(SearchCriteria criteria, int splitIndex, Connection conn) throws SQLException;

    public abstract PreparedStatement getBandSearchStatement(SearchCriteria criteria, int bandIndex, Connection conn) throws SQLException;

    public abstract void augment(SearchRecord record, ResultSet rs) throws SQLException;

    public abstract SearchRecord getSearchRecord();

    // *********************************************************************
    // Instance Methods
    // *********************************************************************
 
    public SearchResults search(SearchCriteria criteria) throws Exception {

        List<SearchRecord> records = new ArrayList<>();
        List<SearchRecord> alternates = new ArrayList<>();
        boolean suggestAlternate = false;

        // enforce some sorts based on search type 
        if (criteria.searchType == SearchType.RMSD) {
            criteria.sortBy = SortBy.RMSD;
        }
        else { 
            criteria.sortBy = SortBy.TM_SCORE;
        }
       
        // limit
        criteria.limit = Math.min(criteria.limit, MAX_LIMIT); 

        Grams grams = null;
        Hashes hashes = null;
        if (criteria.searchBy == SearchBy.DB_ID) {
            
            grams = Db.getGrams(criteria.dbId, criteria.idDbType, true);
            hashes = Db.getHashes(criteria.dbId, criteria.idDbType);
        }
        else { // UPLOAD

            grams = Db.getUploadGrams(criteria.uploadId);
            hashes = Db.getUploadHashes(criteria.uploadId);
        }

        final Grams grams1 = grams;
        final Hashes hashes1 = hashes;

        if (grams1 != null && hashes1 != null) {
            
            // parse query structure
            PDBFileReader reader = new PDBFileReader();
            reader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);

            Structure structure = null;
            if (criteria.searchBy == SearchBy.DB_ID) {

                String fileName = criteria.idDbType.getImportPath() + criteria.dbId;
                String fileNameWithExt = FileUtils.appendExt(fileName);
                
                FileInputStream inputStream = new FileInputStream(fileNameWithExt);
                GZIPInputStream gzipInputStream = null;
                
                if (fileNameWithExt.endsWith("gz")) {
                    gzipInputStream = new GZIPInputStream(inputStream);
                    structure = reader.getStructure(gzipInputStream);
                } 
                else {
                    structure = reader.getStructure(inputStream); 
                }

                inputStream.close();
                if (gzipInputStream != null) {
                    gzipInputStream.close();
                }
            }
            else { // UPLOAD
                
                String fileName = Constants.UPLOAD_PATH + criteria.uploadId + ".pdb";

                FileInputStream inputStream = new FileInputStream(fileName);

                structure = reader.getStructure(inputStream);
                
                inputStream.close();
            }

            // build comparator for final sorts
            Comparator<SearchRecord> comparator = getComparator(criteria);

            if (criteria.searchMode == SearchMode.FAST || criteria.searchMode == SearchMode.TOP_ALIGNED) {

                // parallel band match searches to gather lsh candidates
                records = IntStream.range(0, Constants.BAND_HASH_COUNT).boxed().parallel()
                    .flatMap(bandIndex -> splitBands(bandIndex, criteria, hashes1).stream())
                    .sorted(Comparator.comparingDouble(SearchRecord::getSimilarity).reversed().thenComparing(SearchRecord::getSortKey))
                    .limit(INITIAL_FILTER) 
                    .collect(Collectors.toList());

                // cache map of residue grams
                List<String> dbIds = records.stream().map(SearchRecord::getDbId).collect(Collectors.toList());
                Map<String, Grams> map = Db.getGrams(dbIds, criteria.searchDbType, false);

                // parallel lcs algorithm
                records.parallelStream()
                    .forEach(record -> {

                        if (map.containsKey(record.getDbId())) {

                            Grams grams2 = map.get(record.getDbId());
                            double score = LCS.getLCSScore(grams1.getGramsAsList(), grams2.getGramsAsList(), criteria.searchType);
                            record.setSimilarity(score);

                            if (criteria.searchType == SearchType.FULL_LENGTH) {
                                
                                // set alternate CONTAINED_IN search type score
                                double scoreQ = LCS.getLCSScore(grams1.getGramsAsList(), grams2.getGramsAsList(), SearchType.CONTAINED_IN);
                                record.setSimilarityQ(scoreQ);
                            }
                        }
                    });
                
                // sort lcs candidates
                records = records.stream()
                    .sorted(Comparator.comparingDouble(SearchRecord::getSimilarity).reversed().thenComparing(SearchRecord::getSortKey))
                    .collect(Collectors.toList());

                if (criteria.searchType == SearchType.FULL_LENGTH) {

                    // get lcs alternates
                    SearchRecord top = records.get(0);
                    List<SearchRecord> lcsAlternates = records.stream()
                        .filter(r -> !r.getDbId().equals(top.getDbId()))
                        .sorted(Comparator.comparingDouble(SearchRecord::getSimilarityQ).reversed().thenComparing(SearchRecord::getSortKey))
                        .limit(LCS_ALTERNATES) 
                        .collect(Collectors.toList());
                    alternates.addAll(lcsAlternates);
                }

                // filter lcs candidates
                records = records.stream()
                    .limit(FINAL_FILTER) 
                    .collect(Collectors.toList());
            } 
            else if (criteria.searchMode == SearchMode.ALL_ALIGNED) {

                // initial filtering based on simple LCS plus tm-align on aligned descriptors
                records = IntStream.range(0, Constants.SEARCH_SPLIT_COUNT).boxed().parallel()
                    .flatMap(splitIndex -> splitAllAligned(splitIndex, criteria, grams1).stream())
                    .sorted(Comparator.comparingDouble(SearchRecord::getSimilarity).reversed().thenComparing(SearchRecord::getSortKey))
                    .limit(FINAL_FILTER) 
                    .collect(Collectors.toList());
            }
            else { // SearchMode.OPTIMAL
                
                Structure queryStructure = structure;

                // initial filtering in this case is a fully exhaustive search to get optimal results
                records = IntStream.range(0, Constants.SEARCH_SPLIT_COUNT).boxed().parallel()
                    .flatMap(splitIndex -> splitOptimal(splitIndex, criteria, queryStructure).stream())
                    .sorted(comparator)
                    .limit(criteria.limit) 
                    .collect(Collectors.toList());
            }

            // NOTE: for all search modes except optimal, we now have 8,000 sorted candidates

            // alignments
            if (criteria.searchMode == SearchMode.ALL_ALIGNED || criteria.searchMode == SearchMode.TOP_ALIGNED) {

                Structure queryStructure = structure;

                // *** filter, align, sort
                
                // filter for fast alignments 
                records = records.stream()
                    .limit(fastAlignmentFilter(grams1.getLength()))
                    .collect(Collectors.toList());
               
                // perform fast alignments
                records.stream().parallel().forEach(record -> align(criteria, record, queryStructure, TmMode.FAST));

                // sort by fast alignments
                records = records.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
                
                // *** 
                if (criteria.searchType == SearchType.FULL_LENGTH) {
                    
                    // alternate search type record from TmMode fast
                    SearchRecord top = records.get(0);
                    SearchRecord fastAlternate = records.stream()
                        .filter(r -> !r.getDbId().equals(top.getDbId()))
                        .collect(
                            Collectors.minBy(Comparator.comparingDouble(SearchRecord::getTmScoreQ).reversed().thenComparing(SearchRecord::getSortKey))
                        ).get();
                    alternates.add(fastAlternate);
                }
                
                // *** filter, align, sort

                // filter for regular alignments
                records = records.stream()
                    .limit(criteria.limit)
                    .collect(Collectors.toList());

                // perform regular alignments
                records.stream().parallel().forEach(record -> align(criteria, record, queryStructure, TmMode.REGULAR));

                // sort by regular alignments
                records = records.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
                
                // *** 

                if (criteria.searchType == SearchType.FULL_LENGTH) {

                    // alternate search type record from TmMode regular
                    SearchRecord top = records.get(0);
                    SearchRecord regularAlternate = records.stream()
                        .filter(r -> !r.getDbId().equals(top.getDbId()))
                        .collect(
                            Collectors.minBy(Comparator.comparingDouble(SearchRecord::getTmScoreQ).reversed().thenComparing(SearchRecord::getSortKey))
                        ).get();
                    alternates.add(regularAlternate);
                }
            } 
            else if (criteria.searchMode == SearchMode.FAST) {

                Structure queryStructure = structure;
                
                // *** filter, align, sort

                // filter for regular alignments
                records = records.stream()
                    .limit(criteria.limit)
                    .collect(Collectors.toList());

                // perform regular alignments
                records.stream().parallel().forEach(record -> align(criteria, record, queryStructure, TmMode.REGULAR));

                // sort by regular alignments
                records = records.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
                
                // *** 

                if (criteria.searchType == SearchType.FULL_LENGTH) {
                    
                    // alternate search type record
                    SearchRecord top = records.get(0);
                    SearchRecord fastAlternate = records.stream()
                        .filter(r -> !r.getDbId().equals(top.getDbId()))
                        .collect(
                            Collectors.minBy(Comparator.comparingDouble(SearchRecord::getTmScoreQ).reversed().thenComparing(SearchRecord::getSortKey))
                        ).get();
                    alternates.add(fastAlternate);
                }
            }
            else { 

                // final sort using comparator
                records = records.stream()
                    .sorted(comparator)
                    .limit(criteria.limit)
                    .collect(Collectors.toList());
            }

            // query db id should be first
            if (criteria.searchBy == SearchBy.DB_ID) {
               
                int i; 
                for (i = 0; i < records.size(); i++) {
                    if (records.get(i).getDbId().equals(criteria.dbId)) {
                        break;
                    }
                }
                
                if (i != 0 && i < records.size()) {
                    
                    SearchRecord record = records.get(i);
                    records.remove(i);
                    records.add(0, record);
                    if (criteria.searchMode != SearchMode.FAST) {
                        records.get(0).setRmsd(0.0);
                        records.get(0).setTmScore(1.0);
                    }
                }
            }

            // suggest alternate or not
            if (alternates.size() > 0) {

                double CROSSOVER = 0.04;
                SearchRecord top = records.get(0);
                for (SearchRecord alternate : alternates) {

                    if (!alternate.getDbId().equals(top.getDbId())) {
                        align(criteria, alternate, structure, TmMode.REGULAR);            
                        if (alternate.getTmScoreQ() - top.getTmScore() >= CROSSOVER) {
                            suggestAlternate = true;
                        }   
                    }
                }
            }

            // augment data set for output
            augment(records);
        }

        SearchResults results = new SearchResults();
        
        results.setRecords(records);
        results.setSuggestAlternate(suggestAlternate);

        return results;
    }

    private int fastAlignmentFilter(int gramCount) {

        if (gramCount <= 200) {
            return 8000; 
        }
        else if (gramCount <= 300) {
            return 6000;
        }
        else if (gramCount <= 400) {
            return 4000;
        }
        else if (gramCount <= 600) {
            return 2000;
        }
        else {
            return MAX_LIMIT;
        }
    }

    private Comparator<SearchRecord> getComparator(SearchCriteria criteria) {

        Comparator<SearchRecord> comparator;

        if (criteria.sortBy == SortBy.SIMILARITY) {
            comparator = Comparator.comparingDouble(SearchRecord::getSimilarity);
        } 
        else if (criteria.sortBy == SortBy.RMSD) {
            comparator = Comparator.comparingDouble(SearchRecord::getRmsd);
        }
        else {
            comparator = Comparator.comparingDouble(SearchRecord::getTmScore);
        }
        if (criteria.sortBy.isDescending()) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(SearchRecord::getSortKey);

        return comparator;
    }

    private void align(SearchCriteria criteria, SearchRecord record, Structure queryStructure, TmMode mode) {

        try {
       
            Parser parser = new Parser(); 

            String fileName = getDbType().getImportPath() + record.getDbId();
            String fileNameWithExt = FileUtils.appendExt(fileName);

            FileInputStream targetFile = new FileInputStream(fileNameWithExt);
            GZIPInputStream targetFileGz = null;

            Structure targetStructure = null;
            if (fileNameWithExt.endsWith("gz")) {
                targetFileGz = new GZIPInputStream(targetFile);
                targetStructure = parser.parsePdbFile(targetFileGz);
            } 
            else {
                targetStructure = parser.parsePdbFile(targetFile);
            }

            targetFile.close();
            if (targetFileGz != null) {
                targetFileGz.close();
            }

            Kabsch kabsch = KabschTLS.get();     
            TmAlign tm = new TmAlign(queryStructure, targetStructure, mode, kabsch);
            TmResult results = tm.align();

            // always get it because it's there
            record.setRmsd(results.get_rmsd());

            if (criteria.searchType == SearchType.FULL_LENGTH) {
                record.setTmScore(results.get_tmavg());    
                record.setTmScoreQ(results.get_tmq());    
            }
            else if (criteria.searchType == SearchType.CONTAINED_IN) {
                record.setTmScore(results.get_tmq());    
            }
            else if (criteria.searchType == SearchType.CONTAINS) {
                record.setTmScore(results.get_tmt());
            }
            else {

                // just get the average for other search types
                record.setTmScore(results.get_tmavg());
            }
        }
        catch (IOException e) {
          
            record.setRmsd(9999);
            record.setTmScore(-1);
        }
        catch (RuntimeException e) {
            
            record.setRmsd(9999);
            record.setTmScore(-1);
        }
    }

    private List<SearchRecord> splitAllAligned(int splitIndex, SearchCriteria criteria, Grams grams1) {

        List<SearchRecord> records = new ArrayList<>();

        try {
   
            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement stmt = getSplitSearchStatement(criteria, splitIndex, conn);
            stmt.setFetchSize(200);
            
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {

                String dbId = rs.getString("db_id");
                String pdbId = rs.getString("pdb_id");
                String sortKey = rs.getString("sort_key");

                Grams grams2 = Grams.fromResultSet(rs, true);                
                
                double similarity = LCS.getLCSPlusScore(grams1, grams2, criteria.searchType);

                SearchRecord record = getSearchRecord();
                record.setDbId(dbId);
                record.setPdbId(pdbId);
                record.setSortKey(sortKey);
                record.setSimilarity(similarity);
                records.add(record);
                
                // TODO: after the first 1000, sort and filter based on the worst score
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, e);
        } 

        return records;
    }

    private List<SearchRecord> splitOptimal(int splitIndex, SearchCriteria criteria, Structure queryStructure) {

        List<SearchRecord> records = new ArrayList<>();

        try {
   
            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement stmt = getSplitSearchStatement(criteria, splitIndex, conn);
            stmt.setFetchSize(200);
            
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {

                String dbId = rs.getString("db_id");
                String pdbId = rs.getString("pdb_id");
                String sortKey = rs.getString("sort_key");

                SearchRecord record = getSearchRecord();
                record.setDbId(dbId);
                record.setPdbId(pdbId);
                record.setSortKey(sortKey);

                // this will set all the relevant scores
                align(criteria, record, queryStructure, TmMode.REGULAR);            

                records.add(record);
            
                // TODO: after the first 1000, sort and filter based on the worst score
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, e);
        } 

        return records;
    }

    private List<SearchRecord> splitBands(int bandIndex, SearchCriteria criteria, Hashes hashes1) {

        List<SearchRecord> records = new ArrayList<>();

        try {
   
            // *** LSH band matches
            
            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement stmt = getBandSearchStatement(criteria, bandIndex, conn);
            stmt.setFetchSize(200);
            
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {

                String dbId = rs.getString("db_id");
                String pdbId = rs.getString("pdb_id");
                String sortKey = rs.getString("sort_key");
                Integer[] minHashes = (Integer[])rs.getArray("min_hashes").getArray();
                Integer[] bandHashes = (Integer[])rs.getArray("band_hashes").getArray();
                
                if(!lowerBandMatch(hashes1.bandHashes, bandHashes, bandIndex)) {
                   
                    double similarity = Similarity.getEstimatedSimilarity(hashes1.minHashes, minHashes); 
                    if (similarity >= Constants.SIMILARITY_THRESHOLD) {

                        SearchRecord record = getSearchRecord();
                        record.setDbId(dbId);
                        record.setPdbId(pdbId);
                        record.setSortKey(sortKey);
                        record.setSimilarity(similarity);
                        records.add(record);
                    }
                }
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, e);
        } 

        return records;
    }
    
    private void augment(List<SearchRecord> records) {

        try {

            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(true);
       
            Object[] objDbIds = records.stream().map(record -> record.getDbId()).toArray();

            String[] dbIds = Arrays.copyOf(objDbIds, objDbIds.length, String[].class);

            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM get_" + getDbType().getTableName() + "_augmented_results(?);");
            stmt.setArray(1, conn.createArrayOf("VARCHAR", dbIds));

            ResultSet rs = stmt.executeQuery();
            
            int n = 1;

            while (rs.next()) {

                // WITH ORDINALITY clause will ensure they are ordered correctly

                SearchRecord record = records.get(n-1);

                record.setN(n++);

                augment(record, rs);
            }
            
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, e);
        } 
    }
    
    // *********************************************************************
    // Static Methods
    // *********************************************************************

    private static boolean lowerBandMatch(Integer[] bands1, Integer[] bands2, int bandIndex) {

        // use this function in case of distributed system to eliminate intermediate results up front

        boolean match = false; 
        for (int i = 0; i < bandIndex; i++) {
           if (bands1[i].equals(bands2[i])) {
                match = true;
                break;
           }
        }
        return match;
    }
}
