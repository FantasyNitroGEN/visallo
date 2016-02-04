CREATE TABLE IF NOT EXISTS visallo_systemNotifications (
  id VARCHAR(100) PRIMARY KEY,
  visibility VARCHAR(100) NOT NULL,
  severity VARCHAR(255),
  actionPayload TEXT,
  endDate TIMESTAMP,
  title TEXT,
  message TEXT,
  actionEvent TEXT,
  startDate TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visallo_userNotifications (
  id VARCHAR(100) PRIMARY KEY,
  visibility VARCHAR(100) NOT NULL,
  sentDate TIMESTAMP,
  actionPayload TEXT,
  expirationAgeUnit VARCHAR(255),
  markedRead BOOLEAN,
  expirationAgeAmount INTEGER,
  title TEXT,
  message TEXT,
  actionEvent TEXT,
  userId TEXT
);

CREATE TABLE IF NOT EXISTS visallo_jettySession (
  id VARCHAR(100) PRIMARY KEY,
  visibility VARCHAR(100) NOT NULL,
  data BLOB,
  created BIGINT,
  clusterId TEXT,
  accessed BIGINT,
  version BIGINT
);

CREATE TABLE IF NOT EXISTS visallo_artifactThumbnail (
  id VARCHAR(100) PRIMARY KEY,
  visibility VARCHAR(100) NOT NULL,
  format VARCHAR(100) NOT NULL,
  data LONGBLOB
);

CREATE TABLE IF NOT EXISTS visallo_dictionaryEntry (
  id VARCHAR(100) PRIMARY KEY,
  visibility varchar(100) NOT NULL,
  concept TEXT,
  tokens TEXT,
  resolvedName TEXT
);

CREATE TABLE IF NOT EXISTS visallo_vertex (
  id varchar(255) primary key,
  object longtext not null
);

CREATE TABLE IF NOT EXISTS visallo_edge (
  id varchar(255) primary key,
  in_vertex_id varchar(255),
  out_vertex_id varchar(255),
  object longtext not null
);

CREATE TABLE IF NOT EXISTS visallo_metadata (
  id varchar(255) primary key,
  object longtext not null
);

CREATE TABLE IF NOT EXISTS visallo_streaming_properties (
  id varchar(255) primary key,
  data longblob not null,
  type varchar(255) not null,
  length bigint not null
);
