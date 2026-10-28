# Library Lending System

A Java Swing desktop app backed by a SQLite database. A librarian can manage books and members, check books out and back in, and see who has overdue books and what late fees they owe.

**Target user:** a small-library librarian.

## How to run

1. Install a JDK (Java 11 or newer).
2. Download the **sqlite-jdbc** driver JAR (search "sqlite-jdbc maven", open the latest version, download the `.jar`) and save it as `lib/sqlite-jdbc.jar`.
3. From this folder:
   - Windows: double-click `Library Lending System.vbs` to build and launch the app without leaving a console window open.
   - Windows PowerShell: `.\run.bat`
   - Mac/Linux: `sh run.sh`

   (In an IDE: mark `src` as the source folder, add the JAR to the project libraries, and run `app.Main` with this folder as the working directory.)

The first run builds `library.db` automatically from `schema.sql` and `seed.sql`. Use **Reset sample data** in the app header to reload the sample data.

## Run with Docker

Docker runs the Swing desktop inside a virtual display and serves it in your browser with noVNC. From this folder, run:

```sh
docker compose up --build
```

Open `http://localhost:6080/vnc.html?autoconnect=1&resize=scale`. Stop it with `Ctrl+C` or `docker compose down`. The SQLite database is kept in the `library-data` Docker volume between runs. The web desktop is bound to localhost and has no login, so do not expose port 6080 to a public network.

Docker downloads the pinned SQLite JDBC driver and its SLF4J API dependency during the image build. For running locally, put the JDBC jar at `lib/sqlite-jdbc.jar` as described above.

## Publish on GitHub

The `.gitignore` excludes the local database, compiled classes, and downloaded JDBC jar. Create an empty repository on GitHub, then run these commands from this project folder, replacing the URL with your repository's HTTPS URL:

```sh
git add .
git commit -m "Prepare library app for Docker"
git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPOSITORY.git
git push -u origin main
```

Use **Settings** in the app header to switch between Light, Dark, Pink, Blue, Green, Yellow, Red, and Orange appearance themes. Your choice is saved for the next launch.

The **Tic Tac Toe** tab includes a quick game against an unbeatable local bot. Your score is kept for the current app session. **Trap the Cat** is a second quick game where you block hexes while the cat searches for an open path to the edge.

In the Books tab, **Find series online** looks up an author and subject tags through Open Library and lets you add one catalog row using the series title you entered. A subject may suggest one of the existing categories, which you can change before saving. It does not import volume or edition details.

The Books tab can be filtered by category, author, and checkout status, in addition to title/author text search. The catalog includes Fiction, Science Fiction, Mystery, History, Science, Manga, Romance, Fantasy, Horror, Biography, Poetry, Children's, and Nonfiction. New categories are added to existing databases automatically at startup without changing current book records.

## Project layout

```
schema.sql, seed.sql        database build scripts
src/model/                  Person, Member, Category, Book, Loan
src/dao/                    Database, MemberDAO, BookDAO, CategoryDAO, LoanDAO (all SQL lives here)
src/app/                    Main, LibraryWindow, LateFeeCalculator (Java algorithm)
```

## Schema

| Table | Columns |
|---|---|
| member | member_id (PK), name NOT NULL, email NOT NULL UNIQUE |
| category | category_id (PK), name NOT NULL UNIQUE |
| book | book_id (PK), title NOT NULL, author NOT NULL, category_id (FK -> category, ON DELETE SET NULL) |
| loan | loan_id (PK), member_id (FK -> member, RESTRICT) NOT NULL, book_id (FK -> book, RESTRICT) NOT NULL, checkout_date NOT NULL, due_date NOT NULL, return_date (NULL = not returned yet) |

Relationships:
- One category has many books (one-to-many).
- One member has many loans (one-to-many).
- Members and books are many-to-many, implemented through the `loan` junction table.
- CHECK constraints: `due_date >= checkout_date`, and `return_date` is NULL or not before `checkout_date`.

Loans use `ON DELETE RESTRICT` so a member or book with borrowing history can't be deleted and erase the library's records.

## Requirements checklist

- CRUD on two entities: books (options 3, 4, 5, 1) and members (options 7, 8, 9, 6).
- JOINs: loan + member + book (options 12, 13, 15).
- Aggregate: GROUP BY / COUNT / HAVING (option 14).
- Search driven by user input: option 2 (`LIKE ?`).
- `PreparedStatement` everywhere, no string-built SQL.
- Inheritance: `Member extends Person`.
- Java algorithm: `LateFeeCalculator` totals fees per member and ranks them with a hand-written insertion sort.
- Validation and error handling: bad numbers, blank input, bad emails, duplicate emails, and blocked deletes all print a message instead of crashing.

## Questions my database can answer

1. **Who currently has a book checked out, and what is it?**
   ```sql
   SELECT m.name, b.title, l.due_date
   FROM loan l
   JOIN member m ON l.member_id = m.member_id
   JOIN book b   ON l.book_id   = b.book_id
   WHERE l.return_date IS NULL;
   ```
2. **Which loans are overdue today?**
   ```sql
   SELECT m.name, b.title, l.due_date
   FROM loan l
   JOIN member m ON l.member_id = m.member_id
   JOIN book b   ON l.book_id   = b.book_id
   WHERE l.return_date IS NULL AND l.due_date < date('now')
   ORDER BY l.due_date;
   ```
3. **Which members have borrowed at least 2 books?**
   ```sql
   SELECT m.name, COUNT(*) AS total
   FROM loan l JOIN member m ON l.member_id = m.member_id
   GROUP BY m.member_id, m.name
   HAVING COUNT(*) >= 2
   ORDER BY total DESC;
   ```
4. **Which categories are borrowed most?**
   ```sql
   SELECT c.name, COUNT(*) AS loans
   FROM loan l
   JOIN book b     ON l.book_id = b.book_id
   JOIN category c ON b.category_id = c.category_id
   GROUP BY c.category_id, c.name
   ORDER BY loans DESC;
   ```
5. **Which books have never been borrowed?**
   ```sql
   SELECT b.title
   FROM book b
   LEFT JOIN loan l ON b.book_id = l.book_id
   WHERE l.loan_id IS NULL;
   ```

## Normalization notes

The schema is in 3NF: every non-key column depends only on its table's primary key. Categories live in their own table instead of repeating text in `book`, and loans store ids instead of copying member or book details.

**Intentional denormalization:** `book.author` is a plain text column rather than its own table. This keeps the schema simple, but an author's name could be spelled inconsistently across books, and a book can't easily have more than one author.

## Reflection

If I started over, I would add a `copy` table so the library can own several physical copies of one title and track which copy is checked out, and a `hold` table for a reservation queue (ordered by request time). I would also move `author` into its own table with a many-to-many link to books.
