create table EVENT
(
    id          INTEGER IDENTITY PRIMARY KEY,
    workspaceId VARCHAR(30),
    userId      VARCHAR(50),
    mem         INTEGER,
    io          INTEGER,
    cpu         INTEGER,
--     INDEX       idx_workspaceId (workspaceId),
--     INDEX       idx_userId (userId),
--     INDEX       idx_workspaceId_userId (workspaceId, userId)
);
