/*
select * from aud_diff;

select * from aud_change;

select * from aud_container_change;

select * from aud_value_change;
*/

-----------------------------------------------  DIFFERENCE -------------------------------------
-- Sequence: aud_diff_id_seq

-- DROP SEQUENCE aud_diff_id_seq;

CREATE SEQUENCE aud_diff_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 79548
  CACHE 1;
ALTER TABLE aud_diff_id_seq
  OWNER TO postgres;

-- Table: aud_diff

-- DROP TABLE aud_diff;

CREATE TABLE aud_diff
(
  id integer NOT NULL DEFAULT nextval('aud_diff_id_seq'::regclass), -- auto increment
  user_id character varying(50) NOT NULL,
  creation_date date  NOT NULL,
  entity_type character varying(30)  NOT NULL, 
  entity_id character varying(30)  NOT NULL, 
  CONSTRAINT pk_aud_diff PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE aud_diff
  OWNER TO postgres;
COMMENT ON TABLE aud_diff
  IS 'Message information ';
COMMENT ON COLUMN aud_diff.id IS 'auto increment ';
COMMENT ON COLUMN aud_diff.entity_type IS 'Affected entity (Rule, Assignment, User, etc.)';
COMMENT ON COLUMN aud_diff.entity_id IS ' Code or unique identifier of the audited entity';

-----------------------------------------------  CHANGE -------------------------------------

-- Sequence: aud_change_id_seq

-- DROP SEQUENCE aud_change_id_seq;

CREATE SEQUENCE aud_change_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 79548
  CACHE 1;
ALTER TABLE aud_change_id_seq
  OWNER TO postgres;

-- Table: aud_change

-- DROP TABLE aud_change;

CREATE TABLE aud_change
(
  id integer NOT NULL DEFAULT nextval('aud_change_id_seq'::regclass), -- auto increment
  diff_id integer NOT NULL,
  change_type character varying(20)  NOT NULL, 
  property_name character varying(20)  NOT NULL, 
  CONSTRAINT pk_aud_change PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE aud_change
  OWNER TO postgres;
COMMENT ON TABLE aud_change
  IS 'Message information ';
COMMENT ON COLUMN aud_change.id IS 'auto increment ';
COMMENT ON COLUMN aud_change.change_type IS '';
COMMENT ON COLUMN aud_change.property_name IS ' ';

-----------------------------------------------  CONTAINER CHANGE -------------------------------------

-- Sequence: aud_container_change_id_seq

-- DROP SEQUENCE aud_container_change_id_seq;

CREATE SEQUENCE aud_container_change_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 79548
  CACHE 1;
ALTER TABLE aud_container_change_id_seq
  OWNER TO postgres;

-- Table: aud_container_change

-- DROP TABLE aud_container_change;

CREATE TABLE aud_container_change
(
  id integer NOT NULL DEFAULT nextval('aud_container_change_id_seq'::regclass), -- auto increment
  change_id integer NOT NULL,
  change_value character varying(1024)  NOT NULL, 
  add_or_remove character varying(1)  NOT NULL, 
  CONSTRAINT pk_aud_container_change PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE aud_container_change
  OWNER TO postgres;
COMMENT ON TABLE aud_container_change
  IS 'Message information ';
COMMENT ON COLUMN aud_container_change.id IS 'auto increment ';
COMMENT ON COLUMN aud_container_change.change_value IS '';
COMMENT ON COLUMN aud_container_change.add_or_remove IS ' ';

-----------------------------------------------  VALUE CHANGE -------------------------------------

-- Sequence: aud_value_change_id_seq

-- DROP SEQUENCE aud_value_change_id_seq;

CREATE SEQUENCE aud_value_change_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 79548
  CACHE 1;
ALTER TABLE aud_value_change_id_seq
  OWNER TO postgres;

-- Table: aud_value_change

-- DROP TABLE aud_value_change;

CREATE TABLE aud_value_change
(
  id integer NOT NULL DEFAULT nextval('aud_value_change_id_seq'::regclass), -- auto increment
  change_id integer NOT NULL,
  old_value character varying(1024)  NOT NULL, 
  new_value character varying(1024)  NOT NULL, 
  CONSTRAINT pk_aud_value_change PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE aud_value_change
  OWNER TO postgres;
COMMENT ON TABLE aud_value_change
  IS 'Message information ';
COMMENT ON COLUMN aud_value_change.id IS 'auto increment ';
COMMENT ON COLUMN aud_value_change.old_value IS '';
COMMENT ON COLUMN aud_value_change.new_value IS ' ';
