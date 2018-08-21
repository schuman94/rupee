package edu.umkc.rupee.lib;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.StructureTools;
import org.biojava.nbio.structure.align.StructureAlignment;
import org.biojava.nbio.structure.align.StructureAlignmentFactory;
import org.biojava.nbio.structure.align.model.AFPChain;
import org.biojava.nbio.structure.align.util.AFPChainScorer;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.PDBFileReader;

public class Aligning
{
    public static AlignRecord align(String dbId1, String dbId2, AlignCriteria align, DbTypeCriteria dbType) {

        AlignRecord record = null;

        try {

            PDBFileReader reader = new PDBFileReader();
            reader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);

            FileInputStream queryFile = new FileInputStream(dbType.getImportPath() + dbId1 + ".pdb.gz");
            GZIPInputStream queryFileGz = new GZIPInputStream(queryFile);
            FileInputStream targetFile = new FileInputStream(dbType.getImportPath() + dbId2 + ".pdb.gz");
            GZIPInputStream targetFileGz = new GZIPInputStream(targetFile);

            Structure queryStructure = reader.getStructure(queryFileGz);
            Structure targetStructure = reader.getStructure(targetFileGz);

            record = align(queryStructure, targetStructure, align);

        } catch (IOException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (StructureException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } 

        return record;
    }
    
    public static AlignRecord align(Structure queryStructure, Structure targetStructure, AlignCriteria align) 
        throws IOException, StructureException {

        Atom[] atoms1 = StructureTools.getAtomCAArray(queryStructure);
        Atom[] atoms2 = StructureTools.getAtomCAArray(targetStructure);

        StructureAlignment algorithm = StructureAlignmentFactory.getAlgorithm(align.getAlgorithmName());
        AFPChain afps = algorithm.align(atoms1, atoms2, align.getParams());
        
        afps.setName1(queryStructure.getName());
        afps.setName2(targetStructure.getName());

        afps.setTMScore(AFPChainScorer.getTMScore(afps, atoms1, atoms2));

        AlignRecord record = new AlignRecord();
        record.algorithmName = align.getAlgorithmName();
        record.afps = afps;
        record.atoms1 = atoms1;
        record.atoms2 = atoms2;

        return record;
    }
}
