package edu.umkc.rupee.ecod;

import edu.umkc.rupee.base.Import;
import edu.umkc.rupee.defs.DbTypeCriteria;

public class EcodImport extends Import {

    public DbTypeCriteria getDbType() {

        return DbTypeCriteria.ECOD;
    }
}
