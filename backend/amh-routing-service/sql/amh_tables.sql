 CREATE TABLE public."AMH_BECONFIG"
(
  "CODE" character varying(50) NOT NULL,
  "DATAOWNER" character varying(255),
  "DIRECTION" character varying(255),
  "FILENAME" character varying(100),
  "BCODE" character varying(50),
  "DESCRIPTION" character varying(255),
  "NAME" character varying(255),
  "LOCKCODE" character varying(50),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "AMH_BECONFIG_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE")
--  USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_BECONFIG"
  OWNER TO postgres;

-- Index: public."AMH_BECONFIG_ENV_VERSION_BCODE_DIRECTION_idx"

-- DROP INDEX public."AMH_BECONFIG_ENV_VERSION_BCODE_DIRECTION_idx";

CREATE UNIQUE INDEX "AMH_BECONFIG_ENV_VERSION_BCODE_DIRECTION_idx"
  ON public."AMH_BECONFIG"
  USING btree
  ("ENV" COLLATE pg_catalog."default", "VERSION" COLLATE pg_catalog."default", "BCODE" COLLATE pg_catalog."default", "DIRECTION" COLLATE pg_catalog."default");

  
  CREATE TABLE public."AMH_BE_CH"
(
  "CODE" character varying(50) NOT NULL,
  "DIRECTION" character varying(255) NOT NULL,
  "FILENAME" character varying(100),
  "DESCRIPTION" character varying(255),
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "NAME" character varying(255),
  CONSTRAINT "AMH_BEC_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_BE_CH"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_BE_CH"
  IS 'BackEndChannel';

  
 CREATE TABLE public."AMH_BE_CH_AS_SEL_RU"
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
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  CONSTRAINT "AMH_BE_CH_AS_RU_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_BE_CH_AS_SEL_RU"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_BE_CH_AS_SEL_RU"
  IS 'BackEnd Channel Assignment Selection Rule Criteria';

 
 CREATE TABLE public."AMH_BE_CH_AS_SEL_TAB"
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
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "AMH_BE_CH_AS_SEL_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "BCKENDCH_CODE", "BCKENDCH_DIRECTION", "SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_BE_CH_AS_SEL_TAB"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_BE_CH_AS_SEL_TAB"
  IS 'BackEnd Channel Assignment Selection Table ';

  
 CREATE TABLE public."AMH_BL_CONFIG"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "BLACKLIST_CODE" character varying(255) NOT NULL,
  "EXCEPTIONLIST_CODE" character varying(255),
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "NAME" character varying(255),
  CONSTRAINT "AMH_BL_CONFIG_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "AMH_BL_CONFIG_ENV_fkey" FOREIGN KEY ("ENV", "VERSION", "BLACKLIST_CODE")
      REFERENCES public."AMH_ML" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "AMH_BL_CONFIG_ENV_fkey1" FOREIGN KEY ("ENV", "VERSION", "EXCEPTIONLIST_CODE")
      REFERENCES public."AMH_ML" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_BL_CONFIG"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_BL_CONFIG"
  IS 'BlackListConfiguration';
  
 CREATE TABLE public."AMH_DT_CP_SEL_TAB"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "FILENAME" character varying(100) NOT NULL,
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "NBOFCOPIES" bigint,
  "PRINTLAYOUTTEMPLATE" character varying(255),
  "SELECTIONGROUP" character varying(255),
  "NAME" character varying(255)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_DT_CP_SEL_TAB"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_DT_CP_SEL_TAB"
  IS 'DistributionCopySelectionTableBackendChannel';

  
 CREATE TABLE public."AMH_DT_CP_SEL_TAB_MCD"
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
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "AMH_DT_CP_SEL_TAB_MCD_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "BCKENDCH_CODE", "BCKENDCH_DIRECTION")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_DT_CP_SEL_TAB_MCD"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_DT_CP_SEL_TAB_MCD"
  IS 'DistributionCopyMultiCopyDestination/MultiCopyDestinationMultiCopyDestination';

 
 CREATE TABLE public."AMH_DT_CP_SEL_TAB_RU"
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
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  CONSTRAINT "AMH_DT_CP_SEL_TAB_RU_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_DT_CP_SEL_TAB_RU"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_DT_CP_SEL_TAB_RU"
  IS 'DistributionCopySelectionTableRuleCriteria';

  
 CREATE TABLE public."AMH_FB_DT_CP_SEL_TAB"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "FILENAME" character varying(100),
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "NBOFCOPIES" bigint,
  "PRINTLAYOUTTEMPLATE" character varying(255),
  "SELECTIONGROUP" character varying(255),
  "NAME" character varying(255),
  CONSTRAINT "PK_AMH_FB_DT_CP_SEL_TAB" PRIMARY KEY ("CODE", "VERSION", "ENV")
 -- USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_FB_DT_CP_SEL_TAB"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_FB_DT_CP_SEL_TAB"
  IS 'FeedBackDistributionCopySelectionTable';

  
  CREATE TABLE public."AMH_FB_DT_CP_SEL_TAB_MFD"
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
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "AMH_FB_DT_CP_SEL_TAB_MFD_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "BCKENDCH_CODE", "BCKENDCH_DIRECTION")
  -- USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_FB_DT_CP_SEL_TAB_MFD"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_FB_DT_CP_SEL_TAB_MFD"
  IS 'FeedbackDistributionCopyMultiFeedbackDestination/MultiFeedbackDestination/BackendChanne';

  
 
CREATE TABLE public."AMH_FB_DT_CP_SEL_TAB_RU"
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
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  CONSTRAINT "AMH_FB_DT_CP_SEL_TAB_RU_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE", "RC_CODE", "RC_SEQUENCENUMBER")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_FB_DT_CP_SEL_TAB_RU"
  OWNER TO postgres;

  
 
CREATE TABLE public."AMH_GW_RU_CRIT"
(
  "CODE" character varying(50) NOT NULL,
  "DATAOWNER" character varying(255),
  "CRITERIA" character varying(1024),
  "LOCKCODE" character varying(255),
  "TYPE" character varying(255),
  "FILENAME" character varying(100),
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "INDICATOR_DELETE" character varying(1) NOT NULL DEFAULT 'N'::character varying,
  CONSTRAINT "AMH_GW_RU_CRIT_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_GW_RU_CRIT"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_GW_RU_CRIT"
  IS 'GateWay Rule Criteria';

  
 CREATE TABLE public."AMH_ML"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "MATCHTYPE" character varying(100) NOT NULL, -- Match Type
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  CONSTRAINT "AMH_ML_pkey" PRIMARY KEY ("ENV", "VERSION", "CODE")
  --USING INDEX TABLESPACE rpl_indx
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_ML"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_ML"
  IS 'MatchList';
COMMENT ON COLUMN public."AMH_ML"."MATCHTYPE" IS 'Match Type';


CREATE TABLE public."AMH_ML_EN"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "ML_CODE" character varying(255) NOT NULL, -- MatchList ID
  CONSTRAINT "PK_AMH_ML_EN" PRIMARY KEY ("ENV", "VERSION", "CODE"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "AMH_ML_EN_ENV_fkey" FOREIGN KEY ("ENV", "VERSION", "ML_CODE")
      REFERENCES public."AMH_ML" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_ML_EN"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_ML_EN"
  IS 'MatchListEntry';
COMMENT ON COLUMN public."AMH_ML_EN"."ML_CODE" IS 'MatchList ID';


CREATE TABLE public."AMH_ML_EN_RU"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "ML_EN_CODE" character varying(255) NOT NULL, -- MatchListMatchListEntry  ID
  "SEQ" bigint NOT NULL, -- Sequence No of the rule
  "RC_CODE" character varying(255) NOT NULL, -- Rule Criteria Code
  CONSTRAINT "PK_AMH_ML_EN_RU" PRIMARY KEY ("ENV", "VERSION", "ML_EN_CODE", "RC_CODE"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "AMH_ML_EN_RU_ENV_fkey" FOREIGN KEY ("ENV", "VERSION", "ML_EN_CODE")
      REFERENCES public."AMH_ML_EN" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "AMH_ML_EN_RU_ENV_fkey1" FOREIGN KEY ("ENV", "VERSION", "RC_CODE")
      REFERENCES public."AMH_GW_RU_CRIT" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
--TABLESPACE rpl_indx;

ALTER TABLE public."AMH_ML_EN_RU"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_ML_EN_RU"
  IS 'MatchListEntryRuleCriteria';
COMMENT ON COLUMN public."AMH_ML_EN_RU"."ML_EN_CODE" IS 'MatchListMatchListEntry  ID';
COMMENT ON COLUMN public."AMH_ML_EN_RU"."SEQ" IS 'Sequence No of the rule';
COMMENT ON COLUMN public."AMH_ML_EN_RU"."RC_CODE" IS 'Rule Criteria Code';



CREATE TABLE public."AMH_WL_CONFIG"
(
  "CODE" character varying(255) NOT NULL,
  "ACTIVE" character varying(20),
  "DATAOWNER" character varying(255),
  "LOCKCODE" character varying(255),
  "SEQUENCENUMBER" bigint NOT NULL,
  "WHITELIST_CODE" character varying(255) NOT NULL,
  "EXCEPTIONLIST_CODE" character varying(255),
  "ID_USER_CREATION" character varying(50),
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "ID_USER_MODIFICATION" character varying(50),
  "DATE_MODIFICATION" timestamp without time zone,
  "DESCRIPTION" character varying(255),
  "VERSION" character varying(100) NOT NULL DEFAULT '1.0'::character varying,
  "ENV" character varying(20) NOT NULL DEFAULT 'EN01'::character varying,
  "NAME" character varying(255),
  CONSTRAINT "PK_AMH_WL_CONFIG" PRIMARY KEY ("ENV", "VERSION", "CODE"),
  --USING INDEX TABLESPACE rpl_indx,
  CONSTRAINT "AMH_WL_CONFIG_ENV_fkey" FOREIGN KEY ("ENV", "VERSION", "WHITELIST_CODE")
      REFERENCES public."AMH_ML" ("VERSION", "ENV", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "AMH_WL_CONFIG_ENV_fkey1" FOREIGN KEY ("ENV", "VERSION", "EXCEPTIONLIST_CODE")
      REFERENCES public."AMH_ML" ("ENV", "VERSION", "CODE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."AMH_WL_CONFIG"
  OWNER TO postgres;
COMMENT ON TABLE public."AMH_WL_CONFIG"
  IS 'WhiteListConfiguration';

  
  
