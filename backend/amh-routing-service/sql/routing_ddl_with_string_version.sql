
DROP TABLE public."BCKENDCHASSGNRULECRITERIA";
DROP TABLE public."BCKENDCHASSNSELTABLE";

DROP TABLE public."DISTCPYSELECTIONTABLE";
DROP TABLE public."DISTCPYSELECTIONTABLEMCD";
DROP TABLE public."DISTCPYSELECTNTBLRULECRITERIA";

DROP TABLE public."FEEDBCKDISTCPYRULECRITERIA";
DROP TABLE public."FEEDBCKDISTCPYSELTABLE";
DROP TABLE public."FEEDBCKDISTCPYSELTABLEMFD";

DROP TABLE public."GATEWAY_RULECRITERIA";
DROP TABLE public."BACKENDCONFIGURATION";

CREATE TABLE public."GATEWAY_RULECRITERIA"
(
  "CODE" character varying(50) NOT NULL, -- Unqiue data code
  "DATAOWNER" character varying(255),
  "CRITERIA" character varying(1024),
  "LOCKCODE" character varying(255),
  "TYPE" character varying(255),
  "FILENAME" character varying(100), -- Import Origin file Name
  "ID_USER_CREATION" character varying(50), -- Id of user who created this entity
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()), -- Date of creation
  "ID_USER_MODIFICATION" character varying(50), -- user last modified this entity
  "DATE_MODIFICATION" timestamp without time zone, -- Last date of modification
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying, -- Environment variable
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying, -- Initial version
  "INDICATOR_DELETE" character varying(1) NOT NULL DEFAULT 'N'::character varying, -- when creating not in delete state
  CONSTRAINT "GATEWAY_RULECRITERIA_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);

ALTER TABLE public."GATEWAY_RULECRITERIA"
  OWNER TO postgres;
GRANT ALL ON TABLE public."GATEWAY_RULECRITERIA" TO postgres WITH GRANT OPTION;
COMMENT ON TABLE public."GATEWAY_RULECRITERIA"
  IS 'Gate Way Rule criteria';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."CODE" IS 'Unqiue data code';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."FILENAME" IS 'Import Origin file Name';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."ID_USER_CREATION" IS 'Id of user who created this entity';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."DATE_CREATION" IS 'Date of creation ';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."ID_USER_MODIFICATION" IS 'user last modified this entity';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."DATE_MODIFICATION" IS 'Last date of modification';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."ENV" IS 'Environment variable ';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."VERSION" IS 'Initial version';
COMMENT ON COLUMN public."GATEWAY_RULECRITERIA"."INDICATOR_DELETE" IS 'when creating not in delete state';


CREATE UNIQUE INDEX "UK_GATEWAY_RULECRITERIA"
  ON public."GATEWAY_RULECRITERIA"
  USING btree
  ("ENV" COLLATE pg_catalog."default", "VERSION", "CODE" COLLATE pg_catalog."default");
--TABLESPACE rpl_indx;

CREATE TABLE public."BACKENDCONFIGURATION"
(
  "CODE" character varying(50) NOT NULL,
  "DATAOWNER" character varying(255),
  "DIRECTION" character varying(255),
  "FILENAME" character varying(100),
  "BCODE" character varying(50),
  "DESCRIPTION" character varying(255),
  "NAME" character varying(255),
  "LOCKCODE" character varying(50),
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "PK_BACKENDCONFIGURATION" PRIMARY KEY ("ENV", "VERSION", "CODE")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."BACKENDCONFIGURATION"
  OWNER TO postgres;


CREATE UNIQUE INDEX "UK_BACKENDCON"
  ON public."BACKENDCONFIGURATION"
  USING btree
  ("ENV" COLLATE pg_catalog."default", "VERSION", "BCODE" COLLATE pg_catalog."default", "DIRECTION" COLLATE pg_catalog."default");

CREATE TABLE public."FEEDBCKDISTCPYSELTABLEMFD"
(
  "CODE" character varying(255) NOT NULL,
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "BCKENDCH_CODE" character varying(255) NOT NULL,
  "BCKENDCH_DIRECTION" character varying(255) NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "PK_FEEDBCKDISTCPYSELTABLEMFD" PRIMARY KEY ("ENV", "VERSION", "CODE", "BCKENDCH_CODE", "BCKENDCH_DIRECTION")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."FEEDBCKDISTCPYSELTABLEMFD"
  OWNER TO postgres;
COMMENT ON TABLE public."FEEDBCKDISTCPYSELTABLEMFD"
  IS 'FeedbackDistributionCopyMultiFeedbackDestination/MultiFeedbackDestination/BackendChannel';

CREATE TABLE public."FEEDBCKDISTCPYSELTABLE"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "FILENAME" character varying(100) NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "NBOFCOPIES" bigint, -- No of copies tag  <NumberOfCopies>
  "PRINTLAYOUTTEMPLATE" character varying(255), -- Tag < PrintLayoutTemplate>
  "SELECTIONGROUP" character varying(255), -- <SelectionGroup>
  "NAME" character varying(255),
  CONSTRAINT "FEEDBCKDISTCPYSELTABLE_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."FEEDBCKDISTCPYSELTABLE"
  OWNER TO postgres;
COMMENT ON TABLE public."FEEDBCKDISTCPYSELTABLE"
  IS 'FeedBackDistributionCopySelectionTable';
COMMENT ON COLUMN public."FEEDBCKDISTCPYSELTABLE"."NBOFCOPIES" IS 'No of copies tag  <NumberOfCopies>';
COMMENT ON COLUMN public."FEEDBCKDISTCPYSELTABLE"."PRINTLAYOUTTEMPLATE" IS 'Tag < PrintLayoutTemplate>';
COMMENT ON COLUMN public."FEEDBCKDISTCPYSELTABLE"."SELECTIONGROUP" IS '<SelectionGroup>';



CREATE TABLE public."FEEDBCKDISTCPYRULECRITERIA"
(
  "CODE" character varying(255) NOT NULL,
  "RC_CODE" character varying(255) NOT NULL,
  "RC_DATAOWNER" character varying(255),
  "RC_LOCKCODE" character varying(255),
  "RC_SEQUENCENUMBER" bigint NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  CONSTRAINT "PK_FEEDBCKDISTCPYRC" PRIMARY KEY ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "FK_FEEDBCKDISTCPY_RULE_CRIT" FOREIGN KEY ("ENV", "VERSION", "RC_CODE")
      REFERENCES public."GATEWAY_RULECRITERIA" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "FEEDBCKDISTCPYRULECRITERIA_ENV_VERSION_CODE_RC_CODE_RC_SEQU_key" UNIQUE ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."FEEDBCKDISTCPYRULECRITERIA"
  OWNER TO postgres;
COMMENT ON TABLE public."FEEDBCKDISTCPYRULECRITERIA"
  IS 'FeedBackDistribution Copy Rule Criteria information ';


CREATE TABLE public."DISTCPYSELECTNTBLRULECRITERIA"
(
  "CODE" character varying(255) NOT NULL,
  "RC_CODE" character varying(255) NOT NULL,
  "RC_DATAOWNER" character varying(255),
  "RC_LOCKCODE" character varying(255),
  "RC_SEQUENCENUMBER" bigint NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  CONSTRAINT "PK_DISTCPYSELECTNTBLRC" PRIMARY KEY ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "FK_DISTCPYSELECTRC_RULE_CRIT" FOREIGN KEY ("ENV", "VERSION", "RC_CODE")
      REFERENCES public."GATEWAY_RULECRITERIA" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "DISTCPYSELECTNTBLRULECRITERIA_ENV_VERSION_CODE_RC_CODE_RC_S_key" UNIQUE ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."DISTCPYSELECTNTBLRULECRITERIA"
  OWNER TO postgres;
COMMENT ON TABLE public."DISTCPYSELECTNTBLRULECRITERIA"
  IS 'DistributionCopySelectionTableRuleCriteria';


CREATE TABLE public."DISTCPYSELECTIONTABLEMCD"
(
  "CODE" character varying(255) NOT NULL,
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "BCKENDCH_CODE" character varying(255) NOT NULL,
  "BCKENDCH_DIRECTION" character varying(255) NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "PK_DISTCPYSELECTIONTABLEMCD" PRIMARY KEY ("ENV", "VERSION", "CODE", "BCKENDCH_CODE", "BCKENDCH_DIRECTION")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."DISTCPYSELECTIONTABLEMCD"
  OWNER TO postgres;
COMMENT ON TABLE public."DISTCPYSELECTIONTABLEMCD"
  IS 'DistributionCopyMultiCopyDestination/MultiCopyDestination/MultiCopyDestination';


CREATE TABLE public."DISTCPYSELECTIONTABLE"
(
  "CODE" character varying(255) NOT NULL, -- DistributionCopySelectionTable>/<Code>
  "ACTIVE" character varying(20), -- <DistributionCopySelectionTable>/<Active>
  "DATAOWNER" character varying(255), -- <DistributionCopySelectionTable>/<DataOwner>
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "FILENAME" character varying(100) NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255), -- <DistributionCopySelectionTable>/<Description>
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying, -- Environment code
  "NBOFCOPIES" bigint, -- <NumberOfCopies>
  "PRINTLAYOUTTEMPLATE" character varying(255), -- <PrintLayoutTemplate>
  "SELECTIONGROUP" character varying(255), -- <SelectionGroup>
  "NAME" character varying(255),
  CONSTRAINT "DISTCPYSELECTIONTABLE_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."DISTCPYSELECTIONTABLE"
  OWNER TO postgres;
COMMENT ON TABLE public."DISTCPYSELECTIONTABLE"
  IS 'DistributionCopySelectionTableBackendChannel';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."CODE" IS 'DistributionCopySelectionTable>/<Code>';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."ACTIVE" IS '<DistributionCopySelectionTable>/<Active>';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."DATAOWNER" IS '<DistributionCopySelectionTable>/<DataOwner>';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."DESCRIPTION" IS '<DistributionCopySelectionTable>/<Description>';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."ENV" IS 'Environment code';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."NBOFCOPIES" IS '<NumberOfCopies>';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."PRINTLAYOUTTEMPLATE" IS '<PrintLayoutTemplate>';
COMMENT ON COLUMN public."DISTCPYSELECTIONTABLE"."SELECTIONGROUP" IS '<SelectionGroup>';



CREATE TABLE public."BCKENDCHASSNSELTABLE"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "BCKENDCH_CODE" character varying(255) NOT NULL,
  "BCKENDCH_DIRECTION" character varying(255) NOT NULL,
  "FILENAME" character varying(100) NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "BCKENDCHASSNSELTABLE_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "BCKENDCH_CODE", "BCKENDCH_DIRECTION", "SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."BCKENDCHASSNSELTABLE"
  OWNER TO postgres;
GRANT ALL ON TABLE public."BCKENDCHASSNSELTABLE" TO postgres WITH GRANT OPTION;
COMMENT ON TABLE public."BCKENDCHASSNSELTABLE"
  IS 'BackEnd Channel Assignment Selection Table';

CREATE TABLE public."BCKENDCHASSGNRULECRITERIA"
(
  "CODE" character varying(255) NOT NULL,
  "RC_CODE" character varying(255) NOT NULL,
  "RC_DATAOWNER" character varying(255),
  "RC_LOCKCODE" character varying(255),
  "RC_SEQUENCENUMBER" bigint NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "VERSION" character varying(100) NOT NULL DEFAULT 'VERSION_01'::character varying,
  CONSTRAINT "BCKENDCHASSGNRULECRITERIA_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "FK_BEARC_GW_RC" FOREIGN KEY ("ENV", "VERSION", "RC_CODE")
      REFERENCES public."GATEWAY_RULECRITERIA" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."BCKENDCHASSGNRULECRITERIA"
  OWNER TO postgres;
COMMENT ON TABLE public."BCKENDCHASSGNRULECRITERIA"
  IS 'BackEnd Channel Assignment Selection Rule Criteria';


-------------------------------------------------------------
select "ENV","VERSION"  from "FEEDBCKDISTCPYSELTABLE";
select "ENV","VERSION"  from "FEEDBCKDISTCPYRULECRITERIA";
select "ENV","VERSION"  from "FEEDBCKDISTCPYSELTABLEMFD";
select "ENV","VERSION"  from "DISTCPYSELECTIONTABLE";
select "ENV","VERSION" from "DISTCPYSELECTNTBLRULECRITERIA";
select "ENV","VERSION" from "DISTCPYSELECTIONTABLEMCD";
select "ENV","VERSION" from "BACKENDCONFIGURATION";
select "ENV","VERSION" from "GATEWAY_RULECRITERIA";

insert into "DISTCPYSELECTIONTABLE" ("CODE","SEQUENCENUMBER","ENV","VERSION","FILENAME","ACTIVE","DATAOWNER","LOCKCODE","DESCRIPTION","SELECTIONGROUP","PRINTLAYOUTTEMPLATE","NBOFCOPIES","NAME")  
values                              ('BNP_Distribute_Autocancel_TOBEX',10,'ENV01','DEFAULT','Noname.xml','true',NULL,NULL,NULL,NULL,NULL,NULL,NULL)

insert into "DISTCPYSELECTNTBLRULECRITERIA" ("CODE","RC_SEQUENCENUMBER","RC_CODE","RC_DATAOWNER","RC_LOCKCODE","ENV","VERSION")  
values ('BNP_Distribute_Autocancel_TOBEX',10,'BNP_Autocancel_Distribution_TOBEX',NULL,NULL,'ENV01','DEFAULT')

select * from "GATEWAY_RULECRITERIA" where "CODE" like 'BNP%'

insert into "BCKENDCHASSGNRULECRITERIA" ("CODE","RC_SEQUENCENUMBER","RC_CODE","RC_DATAOWNER","RC_LOCKCODE")  values ('AEMM6',10,'BA-PARBITMM-T2S-AEMM6',NULL,NULL);


