package edu.umkc.rupee.chain;

import edu.umkc.rupee.base.Import;
import edu.umkc.rupee.defs.DbTypeCriteria;

public class ChainImport extends Import {

    public DbTypeCriteria getDbType() {

        return DbTypeCriteria.CHAIN;
    }
}
