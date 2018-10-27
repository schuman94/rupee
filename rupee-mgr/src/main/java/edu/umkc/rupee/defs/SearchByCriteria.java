package edu.umkc.rupee.defs;

public enum SearchByCriteria {
    
    DB_ID(1, "Structure Id"),
    UPLOAD(2, "PDB File");

    private int id;
    private String name;

    SearchByCriteria(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public static SearchByCriteria fromId(int id) {
        if (id == DB_ID.getId()) {
            return DB_ID;
        }
        else {
            return UPLOAD;
        }
    }
}
