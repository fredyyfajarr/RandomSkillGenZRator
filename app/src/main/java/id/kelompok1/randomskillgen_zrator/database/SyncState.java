package id.kelompok1.randomskillgen_zrator.database;

/** Status sinkronisasi Firestore — di-observe oleh UI untuk feedback offline/error. */
public enum SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}