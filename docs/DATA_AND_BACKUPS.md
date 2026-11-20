# Data, migration and backups

The live store is a complete versioned snapshot, `library-state.txt`, inside the configured data directory. Each record is a single-line, quote-aware CSV row encoded as Base64 so that record categories can share a single atomic snapshot. CSV quoting supports Arabic and other Unicode text, commas, quotes and empty trailing fields. Embedded line breaks in metadata are rejected.

Snapshot saves stage the entire collection before replacement. The service serializes all three record categories together for borrowing, returns and CRUD operations. An I/O failure reloads the last readable state. This protects the tested failed-save path, but does not constitute a power-loss/fsync or distributed-database durability guarantee. The GUI permits only one running application instance per directory. Applications that bypass the desktop entry point must not write concurrently through separate service instances.

On startup, active transactions determine book availability and active-loan counts. Duplicate record IDs, duplicate active loans, missing active-loan references, malformed CSV and unreadable snapshots stop loading. Completed transactions may refer to removed books/members because finished history is retained.

Legacy import is read-only until a successful change creates the snapshot. The importer accepts:

- Base book rows with six unprefixed columns or seven columns beginning with BOOK.
- FICTION and NONFICTION rows with ten columns.
- TEXTBOOK and REFERENCE rows with thirteen columns.
- The earlier doubled NONFICTION subtype prefix, which produced fourteen columns.
- Member rows with seven columns and transaction rows with eight columns.

Back up an existing directory before using it with the maintained version. Do not edit the old text files after migration and expect those edits to appear; the snapshot takes precedence. Do not remove a live snapshot to force a fallback to stale legacy files.

Use Tools > Back Up Library Data to select a ZIP destination outside the live directory. The backup includes the snapshot, available legacy files, and covers/member images. Temporary files, symlinks and the active lock file are excluded. The application reports failure if creating or writing the backup fails.

For a manual restore, close the application, preserve a separate copy of the current data directory, and extract a trusted backup into an empty private directory. Start the app with `-Dlibrary.dataDir` pointing at that directory. Keep the current copy until the restored library has been checked. Automated restore, encrypted backups and scheduled backups are not implemented.

Catalogue CSV export includes book metadata, not member contact details. Formula-like metadata is prefixed as text in the exported CSV to reduce spreadsheet formula interpretation; stored metadata is unchanged. Log export writes the currently visible session log. The complete persisted loan history remains in the snapshot/backup.

Image saves are separate from record commits. They decode supported images with file-size and pixel limits, normalize the image to PNG, replace it, and remove obsolete alternatives for the same ID. A record can be successfully saved even if its image fails; the GUI reports that distinction. Existing original photos and records are not shipped in this repository.
