create table EVENT
(
    id          INTEGER IDENTITY PRIMARY KEY,
    workspaceId VARCHAR(30),
    userId      VARCHAR(50),
    mem         INTEGER,
    io          INTEGER,
    cpu         INTEGER
);
