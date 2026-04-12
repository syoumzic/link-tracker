CREATE TABLE IF NOT EXISTS chats (
    chatId BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS links (
    id SERIAL PRIMARY KEY,
    chatId BIGINT NOT NULL REFERENCES chats(chatId) ON DELETE CASCADE,
    url TEXT NOT NULL,
    apiUrl TEXT NOT NULL,
    site TEXT NOT NULL,
    lastUpdate TIMESTAMP WITH TIME ZONE,
    UNIQUE (chatId, url)
    );

CREATE TABLE IF NOT EXISTS tags (
    id SERIAL PRIMARY KEY,
    linkId INTEGER NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    UNIQUE (linkId, name)
);

CREATE INDEX IF NOT EXISTS idx_links_chatId ON links(chatId);

CREATE INDEX IF NOT EXISTS idx_links_url ON links(url);

CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);

CREATE INDEX IF NOT EXISTS idx_tags_linkId ON tags(linkId);