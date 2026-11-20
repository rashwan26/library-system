# Changes from the supplied project

- Preserved the Java Swing interface, book inheritance hierarchy, factory and custom exceptions.
- Fixed inconsistent base-book and nested subtype serialization; added single-line quote-aware CSV throughout entity persistence.
- Centralized book/member CRUD and circulation rules in LibraryService; removed GUI routines that rewrote different file formats.
- Preserved subtype details, join dates, membership IDs, loan counts and availability when editing.
- Used exact record identifiers rather than substring matching to update/delete records.
- Replaced millisecond loan identifiers with UUID-based identifiers.
- Calculated overdue fines using the return date and prevented a returned transaction being returned twice.
- Enforced non-borrowable reference books and reference-specific loan lengths.
- Blocked book/member deletion with active loans; retained completed loan history.
- Added a versioned complete snapshot, atomic replacement where supported, failed-save rollback and a desktop application lock.
- Added validated, bounded PNG image storage and safe record identifiers.
- Replaced success-only export/backup placeholders with actual CSV, log and ZIP writing, including overwrite prompts and error reporting.
- Added sortable tables with model/view index conversion and numeric year/price sorting.
- Simplified edit-button state management and replaced internal search debug output with user-facing results.
- Corrected the Java requirement to 17+, added cross-platform scripts and an executable JAR build, and updated optional Maven packaging.
- Added 39 dependency-free regression scenarios and English documentation.
- Excluded supplied records, photos, compiled classes, backups and unused static HTML/CSS from publication.

A visual desktop walkthrough, encrypted storage, staff login, database server, multiple-copy stock management and automated restore are outside this maintenance change.
