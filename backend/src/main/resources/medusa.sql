CREATE SEQUENCE records_id_seq START WITH 10000001;
CREATE TABLE records (
  records_id  BIGINT PRIMARY KEY DEFAULT nextval('records_id_seq'),
  start_time  BIGINT NOT NULL,
  end_time    BIGINT NOT NULL,
  room_id     BIGINT NOT NULL,
  user_count  INT    NOT NULL,
  frame_count BIGINT NOT NULL
);

CREATE SEQUENCE records_user_map_seq START WITH 1000000001;
CREATE TABLE records_user_map (
  id          BIGINT PRIMARY KEY DEFAULT nextval('records_user_map_seq'),
  records_id  BIGINT       NOT NULL,
  player_id   VARCHAR(127) NOT NULL,
  nickname    VARCHAR(127) NOT NULL,
  play_period TEXT         NOT NULL
);
create table TEST
(
  ID       INTEGER not null auto_increment
    primary key,
  NAME     VARCHAR(255)      not null,
  PASSWORD VARCHAR(255)      not null,
  DISABLED INTEGER default 0 not null
);
CREATE  TABLE  admin(
  ID integer not null auto_increment primary key ,
  USERNAMEA VARCHAR(255) NOT NULL ,
  PASSWORDA VARCHAR(255) NOT NULL
)