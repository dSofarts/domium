CREATE TABLE chat_room (
  id UUID PRIMARY KEY,
  project_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (project_id)
);

CREATE TABLE chat_member (
  id UUID PRIMARY KEY,
  chat_id UUID NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
  user_id UUID NOT NULL,
  username VARCHAR(255) NOT NULL,
  joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE(chat_id, user_id)
);

CREATE TABLE chat_message (
  id UUID PRIMARY KEY,
  chat_id UUID NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
  sender_id UUID NOT NULL,
  content TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT false,
  read_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_message_chat_id ON chat_message(chat_id);
CREATE INDEX idx_chat_message_created_at ON chat_message(created_at);
