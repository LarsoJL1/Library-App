PRAGMA foreign_keys = ON;

INSERT INTO category (name) VALUES
 ('Fiction'),('Science Fiction'),('Mystery'),('History'),('Science'),
 ('Manga'),('Romance'),('Fantasy'),('Horror'),('Biography'),('Poetry'),
 ('Children''s'),('Nonfiction');

INSERT INTO member (name, email) VALUES
 ('Ava Johnson','ava@example.com'),('Ben Carter','ben@example.com'),
 ('Chloe Davis','chloe@example.com'),('Dan Evans','dan@example.com'),
 ('Ella Foster','ella@example.com'),('Finn Garcia','finn@example.com'),
 ('Grace Hill','grace@example.com'),('Hank Irwin','hank@example.com'),
 ('Ivy Jones','ivy@example.com'),('Jack Kim','jack@example.com');

INSERT INTO book (title, author, category_id) VALUES
 ('To Kill a Mockingbird','Harper Lee',1),
 ('The Great Gatsby','F. Scott Fitzgerald',1),
 ('Dune','Frank Herbert',2),
 ('Ender''s Game','Orson Scott Card',2),
 ('Neuromancer','William Gibson',2),
 ('The Hobbit','J.R.R. Tolkien',1),
 ('Gone Girl','Gillian Flynn',3),
 ('The Da Vinci Code','Dan Brown',3),
 ('And Then There Were None','Agatha Christie',3),
 ('Sapiens','Yuval Noah Harari',4),
 ('Guns, Germs, and Steel','Jared Diamond',4),
 ('The Wright Brothers','David McCullough',4),
 ('A Brief History of Time','Stephen Hawking',5),
 ('Cosmos','Carl Sagan',5),
 ('The Selfish Gene','Richard Dawkins',5),
 ('1984','George Orwell',1),
 ('The Martian','Andy Weir',2),
 ('Sherlock Holmes Stories','Arthur Conan Doyle',3),
 ('The Gene','Siddhartha Mukherjee',5),
 ('Educated','Tara Westover',4);

INSERT INTO loan (member_id, book_id, checkout_date, due_date, return_date) VALUES
 (1,1,'2026-09-01','2026-09-15','2026-09-10'),
 (1,5,'2026-09-20','2026-10-04',NULL),
 (2,2,'2026-09-05','2026-09-19','2026-09-18'),
 (2,8,'2026-10-10','2026-10-24',NULL),
 (3,3,'2026-09-12','2026-09-26','2026-09-30'),
 (3,11,'2026-10-15','2026-10-29',NULL),
 (4,4,'2026-08-20','2026-09-03','2026-09-01'),
 (4,12,'2026-10-01','2026-10-15',NULL),
 (5,6,'2026-09-08','2026-09-22','2026-09-21'),
 (5,14,'2026-10-18','2026-11-01',NULL),
 (6,7,'2026-09-15','2026-09-29','2026-10-05'),
 (6,1,'2026-10-12','2026-10-26',NULL),
 (7,9,'2026-09-02','2026-09-16','2026-09-16'),
 (7,15,'2026-10-20','2026-11-03',NULL),
 (8,10,'2026-09-25','2026-10-09','2026-10-08'),
 (8,2,'2026-10-02','2026-10-16',NULL),
 (9,13,'2026-09-18','2026-10-02','2026-10-01'),
 (9,17,'2026-10-22','2026-11-05',NULL),
 (10,16,'2026-09-28','2026-10-12','2026-10-14'),
 (10,18,'2026-10-05','2026-10-19',NULL);
