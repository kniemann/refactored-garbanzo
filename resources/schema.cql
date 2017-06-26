CREATE KEYSPACE images WITH replication = {'class': 'SimpleStrategy' , 'replication_factor': '1'};
CREATE TABLE images.labels (
  --uuid uuid PRIMARY KEY,
  PRIMARY KEY (uuid, request_time)
  image_name varchar,
  source varchar,
  label varchar,
  probability double,
  request_time timestamp
) WITH CLUSTERING ORDER BY request_time ASC;

CREATE TABLE images.labels (
  PRIMARY KEY (uuid, request_time),
  uuid uuid,
  image_name varchar,
  source varchar,
  label varchar,
  probability double,
  request_time timestamp
) WITH CLUSTERING ORDER BY (request_time ASC);