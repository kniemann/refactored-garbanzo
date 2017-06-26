CREATE KEYSPACE images WITH replication = {'class': 'SimpleStrategy' , 'replication_factor': '1'};
CREATE TABLE images.labels (
  uuid uuid PRIMARY KEY,
  image_name varchar,
  source varchar,
  label varchar,
  probability double,
  request_time timestamp
);