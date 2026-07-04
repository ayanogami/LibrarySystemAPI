create table authors (
    id bigint generated always as identity,
    name varchar(255) not null,
    birth_date date not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint pk_authors primary key (id),
    constraint chk_authors_birth_date check (birth_date <= current_date)
);

create table books (
    id bigint generated always as identity,
    title varchar(255) not null,
    price integer not null,
    publication_status varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint pk_books primary key (id),
    constraint chk_books_price check (price >= 0),
    constraint chk_books_publication_status check (publication_status in ('UNPUBLISHED', 'PUBLISHED'))
);

create table book_authors (
    book_id bigint not null,
    author_id bigint not null,
    constraint pk_book_authors primary key (book_id, author_id),
    constraint fk_book_authors_book_id foreign key (book_id) references books (id),
    constraint fk_book_authors_author_id foreign key (author_id) references authors (id)
);

create index idx_book_authors_author_id on book_authors (author_id);
