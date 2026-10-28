PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT NOT NULL,
    email     TEXT NOT NULL UNIQUE
);

CREATE TABLE category (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE
);

CREATE TABLE book (
    book_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT NOT NULL,
    author      TEXT NOT NULL,
    category_id INTEGER,
    FOREIGN KEY (category_id) REFERENCES category(category_id)
        ON DELETE SET NULL
);

CREATE TABLE loan (
    loan_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id     INTEGER NOT NULL,
    book_id       INTEGER NOT NULL,
    checkout_date DATE NOT NULL,
    due_date      DATE NOT NULL,
    return_date   DATE,  -- NULL means not returned yet
    CHECK (due_date >= checkout_date),
    CHECK (return_date IS NULL OR return_date >= checkout_date),
    FOREIGN KEY (member_id) REFERENCES member(member_id) ON DELETE RESTRICT,
    FOREIGN KEY (book_id)   REFERENCES book(book_id)     ON DELETE RESTRICT
);
