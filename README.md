# Library System

A Java Swing desktop application for managing a book catalogue, book-cover images, member records, borrowing and returns. This repository improves the supplied student project while preserving its desktop interface and object-oriented book hierarchy.

## Features

- Add, edit, search and delete books by ISBN or inventory ID.
- Record title, author, publication year, price, type and availability.
- Book cover uploads and optional member photos, with bounded image validation.
- Five book types: Book, Fiction, Non-fiction, Textbook and Reference.
- Member contact details, join dates and active-loan counts.
- Borrowing, returns, overdue views and late-return fine calculation.
- Search by title, author, ID or all fields; sortable tables with correct row selection.
- Actual catalogue CSV export, visible-log export and data/image ZIP backups.
- Durable UTF-8 storage and an application lock to prevent simultaneous desktop writers.

This is a **desktop application**, not a web app. It has no third-party runtime dependencies and does not require a database server.

## Requirements and run

Install a **JDK 17 or newer** and use a desktop session. Start from the project folder:

**Windows**

```bat
run.bat
```

**Linux / macOS**

```sh
sh run.sh
```

These scripts compile the sources and produce `build/library-system.jar`. After building, launch directly with:

```sh
java -jar build/library-system.jar
```

The app starts with an empty catalogue. No member records, photos, circulation history or compiled binaries from the supplied archive are included in Git.

Optional Maven packaging is configured in `pom.xml` (`mvn package`), producing `target/library-system-1.1.0.jar`. The standalone regression suite is run separately using `test.sh` / `test.bat`; `mvn package` is not a substitute for those tests.

## Typical workflow

1. In Books, enter a unique ISBN/inventory ID, title, author, year and positive price. Choose the book type, optionally upload a cover, then add the book.
2. In Members, add a member ID, name and optional contact information/photo.
3. Borrow using the member ID and book ID. The normal limit is five active loans per member, with a 14-day due date.
4. Return the book using the same IDs. Late returns accrue one displayed monetary unit per late day; the amount is frozen at return.
5. Select an existing record and use Edit / Save to change it. Clear exits edit mode. IDs remain fixed.
6. Use File > Export Report for catalogue CSV or Tools > Back Up Library Data for a ZIP containing records and images.

Reference books default to in-library use. A reference object configured as borrowable uses its own maximum loan duration. The main GUI edits common book metadata; specialized subtype fields are preserved when editing the same type, but do not have separate form inputs.

The loan limits and fine rate are project rules, not a verified institutional library policy. One catalogue record represents one lendable item; multiple-copy inventory is not modeled.

## Storage and compatibility

The default data directory is `data/`, relative to the working directory. Run scripts select the project directory automatically. To choose another location:

```sh
java -Dlibrary.dataDir="/path/to/private/library-data" -jar build/library-system.jar
```

| Path inside the data directory | Purpose |
|---|---|
| `library-state.txt` | Authoritative snapshot of books, members and loan history |
| `book_covers/` | Uploaded book-cover images |
| `member_photos/` | Optional member images |
| `.app.lock` | Single-instance lock |

The snapshot uses versioned, Base64-encoded UTF-8 CSV records. Base64 is a storage encoding, not encryption. A save writes the complete record snapshot to a temporary file and replaces the previous snapshot. Borrowing/returning commit book availability, member counts and the transaction together. Failed writes reload the previous state rather than leaving in-memory balances changed. Replacement falls back to a normal replacement on filesystems without atomic-move support.

Before a snapshot exists, the app can read the supplied legacy `books.txt`, `members.txt` and `transactions.txt` formats from the data directory. Typed books, untyped base books and the old doubled Non-fiction prefix in Textbook/Reference records are supported. Existing malformed records stop loading rather than being silently dropped or overwritten. Legacy names/titles containing unquoted commas cannot always be recovered automatically.

After the first successful save, `library-state.txt` is authoritative; the legacy files remain untouched and are no longer the live store. Active loans determine availability and member counts on reload. Completed transaction history remains when a book/member is removed. Outstanding loans must be returned before deletion. Back up old data before migrating it; see [data and backups](docs/DATA_AND_BACKUPS.md).

New images are normalized to PNG under their validated record ID. Supported input images include JPG/JPEG, PNG, GIF and BMP, limited to 10 MB and 16 million pixels. GIF covers use a static frame. Images and record snapshots are separate saves; an image failure does not undo a successful record save and is reported separately.

## Verification

Run the dependency-free suite:

```sh
sh test.sh
```

On Windows:

```bat
test.bat
```

**39 regression scenarios pass locally on Java 17.** They cover serialization for all book types, legacy compatibility, Unicode/quoted metadata, persistence, loans/returns, late fines, duplicate IDs, exact matching, active-loan deletion protection, rollback after a simulated write failure, images, actual exports/backups and sorted JTable selections.

The complete GUI compiles, and table selection is tested with Swing components in headless mode. Full visual desktop interaction has not been exercised in this environment. All 39 scenarios also pass on GitHub Actions on **Windows and Ubuntu with Java 17 and 21**; optional Maven packaging passes on Ubuntu / Java 17. See [verification evidence](docs/VERIFICATION.md). These checks do not establish visual GUI behavior.

## Project structure

```text
src/entities/     Book hierarchy, Member, Librarian, Person and Transaction
src/patterns/     BookFactory
src/services/     Library rules, image validation and exports
src/fileio/       Snapshot persistence and application lock
src/gui/          Existing Swing interface, wired to the shared service layer
src/main/         Desktop entry point
src/util/         CSV, validation and table selection helpers
test/             Dependency-free regression suite
docs/             Architecture, storage notes and fixes
```

Original project supplied by **rashwan26**. See [changes](docs/CHANGES.md) and [class diagram](docs/UML_Diagram.md).

This remains a local, trusted-operator desktop prototype: it has no staff authentication, encryption at rest or network/multi-user service. Keep library records, photos, exported reports and backup ZIPs private. The repository intentionally excludes supplied personal data and unused HTML/CSS mockups.
