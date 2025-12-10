CREATE TABLE chat_room (
  id UUID PRIMARY KEY,
  object_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (object_id)
);

CREATE TABLE chat_participant (
  id UUID PRIMARY KEY,
  chat_id UUID NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
  user_id UUID NOT NULL,
  joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE(chat_id, user_id)
);

CREATE TABLE user_public_key (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  public_key TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  is_revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE encrypted_chat_key (
  id UUID PRIMARY KEY,
  chat_id UUID NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
  user_id UUID NOT NULL,
  encrypted_key TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE(chat_id, user_id)
);

CREATE TABLE chat_message (
  id UUID PRIMARY KEY,
  chat_id UUID NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
  sender_id UUID NOT NULL,
  sender_public_key_id UUID NOT NULL REFERENCES user_public_key(id),
  encrypted_payload TEXT NOT NULL,
  nonce TEXT NOT NULL,
  message_type VARCHAR(20) NOT NULL DEFAULT 'text',
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Индексы для быстрого поиска
CREATE INDEX idx_chat_message_chat_id ON chat_message(chat_id);
CREATE INDEX idx_chat_message_created_at ON chat_message(created_at);
CREATE INDEX idx_user_public_key_user_id ON user_public_key(user_id);
