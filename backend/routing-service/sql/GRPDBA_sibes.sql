ALTER TABLE public."SBS_ROUTINGRULESDATA" DROP CONSTRAINT "SBS_ROUTINGRULESDATA_ENV_fkey";
ALTER TABLE public."SBS_MESSAGEPARTNER" DROP CONSTRAINT "SBS_MESSAGEPARTNER_pkey";
--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.3
-- Dumped by pg_dump version 9.5.3

-- Started on 2016-07-28 14:27:34

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
--SET row_security = off;

SET search_path = public, pg_catalog;

--SET default_tablespace = 'rpl_data';

SET default_with_oids = false;

--
-- TOC entry 231 (class 1259 OID 17463)
-- Name: SBS_EXITPOINT; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SBS_EXITPOINT" (
    "IDENTIFIERNAME" character varying(100) NOT NULL,
    "QUEUETYPE" character varying(100) NOT NULL,
    "QUEUETHRESHOLD" character varying(10),
    "MESSAGEPARTNER" character varying(100),
    "RULESVISIBLE" character varying(10),
    "RULESMODIFIABLE" character varying(10),
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_EXITPOINT" OWNER TO postgres;

--
-- TOC entry 3396 (class 0 OID 0)
-- Dependencies: 231
-- Name: TABLE "SBS_EXITPOINT"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SBS_EXITPOINT" IS 'Sybes Routing Exist point details  ';


--
-- TOC entry 3397 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."IDENTIFIERNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."IDENTIFIERNAME" IS 'Identifier Name';


--
-- TOC entry 3398 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."QUEUETYPE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."QUEUETYPE" IS 'Queue Type';


--
-- TOC entry 3399 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."QUEUETHRESHOLD"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."QUEUETHRESHOLD" IS 'Queue Threshold';


--
-- TOC entry 3400 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."MESSAGEPARTNER"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."MESSAGEPARTNER" IS 'Message Partner';


--
-- TOC entry 3401 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."RULESVISIBLE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."RULESVISIBLE" IS 'Rules Visible';


--
-- TOC entry 3402 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."RULESMODIFIABLE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."RULESMODIFIABLE" IS 'Rules Modifiable';


--
-- TOC entry 3403 (class 0 OID 0)
-- Dependencies: 231
-- Name: COLUMN "SBS_EXITPOINT"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_EXITPOINT"."ENV" IS 'Envrionment value';


--
-- TOC entry 233 (class 1259 OID 17543)
-- Name: SBS_MESSAGEPARTNER; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SBS_MESSAGEPARTNER" (
    "IDENTIFIERNAME" character varying(100) NOT NULL,
    "DESCRIPTION" character varying(100),
    "CONNECTIONMETHOD" character varying(100),
    "AUTHENTICATIONREQUIRED" character varying(100),
    "ALLOWEDDIRECTION" character varying(100),
    "DATAFORMAT" character varying(100),
    "QUEUEMANAGERNAME" character varying(100),
    "QUEUENAME" character varying(100),
    "ERRORQUEUENAME" character varying(100),
    "KEEPSESSIONOPEN" character varying(100),
    "TRANSFERSAAINFO" character varying(100),
    "INCLUDETRN" character varying(100),
    "REMOVESBLOCK" character varying(100),
    "UNIQUEMESGID" character varying(100),
    "USEBINARYPREFIXFORMAT" character varying(100),
    "SESSIONINITIATION" character varying(100),
    "ALWAYSTRANSFERMACPAC" character varying(100),
    "TRANSFERPKISIGNATURE" character varying(100),
    "INCREMENTSEQACCROSSSESSION" character varying(100),
    "PROFILENAME" character varying(100),
    "ROUTINGCODETRANSMITTED" character varying(100),
    "MESSAGEEMISSIONFORMAT" character varying(100),
    "NOTIFICATIONINCLUDESORIMSG" character varying(100),
    "TRANSFERUUMID" character varying(100),
    "ASSIGNEDMESSAGEPARTNERSNAME" character varying(100),
    "LANG" character varying(100),
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_MESSAGEPARTNER" OWNER TO postgres;

--
-- TOC entry 3405 (class 0 OID 0)
-- Dependencies: 233
-- Name: TABLE "SBS_MESSAGEPARTNER"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SBS_MESSAGEPARTNER" IS 'Sybes Routing MessagePartner details   ';


--
-- TOC entry 3406 (class 0 OID 0)
-- Dependencies: 233
-- Name: COLUMN "SBS_MESSAGEPARTNER"."IDENTIFIERNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_MESSAGEPARTNER"."IDENTIFIERNAME" IS 'Identifier Name';


--
-- TOC entry 3407 (class 0 OID 0)
-- Dependencies: 233
-- Name: COLUMN "SBS_MESSAGEPARTNER"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_MESSAGEPARTNER"."ENV" IS 'Environment value';


--
-- TOC entry 3408 (class 0 OID 0)
-- Dependencies: 233
-- Name: COLUMN "SBS_MESSAGEPARTNER"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_MESSAGEPARTNER"."VERSION" IS 'Version';


--
-- TOC entry 234 (class 1259 OID 17563)
-- Name: SBS_ROUTINGKEYWORD; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SBS_ROUTINGKEYWORD" (
    "NAME" character varying(255) NOT NULL,
    "DESCRIPTION" character varying(255),
    "TYPE" character varying(50) NOT NULL,
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_ROUTINGKEYWORD" OWNER TO postgres;

--
-- TOC entry 3410 (class 0 OID 0)
-- Dependencies: 234
-- Name: TABLE "SBS_ROUTINGKEYWORD"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SBS_ROUTINGKEYWORD" IS 'Sybes Routing Keywords';


--
-- TOC entry 3411 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN "SBS_ROUTINGKEYWORD"."NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGKEYWORD"."NAME" IS 'Routing Keyword  Name';


--
-- TOC entry 3412 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN "SBS_ROUTINGKEYWORD"."DESCRIPTION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGKEYWORD"."DESCRIPTION" IS 'Description ';


--
-- TOC entry 3413 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN "SBS_ROUTINGKEYWORD"."TYPE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGKEYWORD"."TYPE" IS 'Type';


--
-- TOC entry 3414 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN "SBS_ROUTINGKEYWORD"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGKEYWORD"."ENV" IS 'Environment';


--
-- TOC entry 3415 (class 0 OID 0)
-- Dependencies: 234
-- Name: COLUMN "SBS_ROUTINGKEYWORD"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGKEYWORD"."VERSION" IS 'Version';


--
-- TOC entry 214 (class 1259 OID 16969)
-- Name: SBS_ROUTINGPOINTNAME; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SBS_ROUTINGPOINTNAME" (
    "NAME" character varying(255) NOT NULL,
    "DESCRIPTION" character varying(255),
    "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
    "ID_USER_MODIF" character varying(50),
    "ID_USER_CREATION" character varying(50),
    "LAST_DATE_MODIFICATION" timestamp without time zone,
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_ROUTINGPOINTNAME" OWNER TO postgres;

--
-- TOC entry 3417 (class 0 OID 0)
-- Dependencies: 214
-- Name: COLUMN "SBS_ROUTINGPOINTNAME"."DESCRIPTION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGPOINTNAME"."DESCRIPTION" IS 'Description';


--
-- TOC entry 3418 (class 0 OID 0)
-- Dependencies: 214
-- Name: COLUMN "SBS_ROUTINGPOINTNAME"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGPOINTNAME"."ENV" IS 'Environment';


--
-- TOC entry 3419 (class 0 OID 0)
-- Dependencies: 214
-- Name: COLUMN "SBS_ROUTINGPOINTNAME"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGPOINTNAME"."VERSION" IS 'Version value';


--
-- TOC entry 213 (class 1259 OID 16961)"Sib_DBNPAGPGX"
-- Name: SBS_ROUTINGRULESDATA; Type: TABLE; Schema: public; Owner: postgres
--
--select * from "SBS_ROUTINGKEYWORD"
--select * from "SBS_ROUTINGRULESDATA"
--delete from "SBS_ROUTINGRULESDATA"
CREATE TABLE "SBS_ROUTINGRULESDATA" (
    "ROUTINGPOINTNAME" character varying(255) NOT NULL,
    "FULL" character varying(10),
    "SEQ" bigint NOT NULL,
    "RULEDESCRIPTION" character varying(255),
    "SCHEMAMAP" character varying(26),
    "CONDITIONON" character varying(500),
    "FUNCTIONLIST" character varying(4000),
    "CRITERIA" character varying(4000),
    "ACTIONON" character varying(500),
    "INSTANCEACTION" character varying(255),
    "INSTANCEINTERVENTIONTYPE" character varying(255),
    "INSTANCEROUTINGCODE" character varying(255),
    "INSTANCETARGETQUEUE" character varying(255),
    "NEWINSTTYPE" character varying(255),
    "NEWINSTACTION" character varying(255),
    "NEWINSTTARGETQUEUE" character varying(255),
    "NEWINSTINTERVENTIONTYPE" character varying(255),
    "NEWINSTINSTANCEROUTINGCODE" character varying(255),
    "INSTANCEINTERVENTIONTYPETEXT" character varying(500),
    "NEWINSTANCEINTERVENTIONTYPTXT" character varying(500),
    "INSTANCEUNIT" character varying(255),
    "NEWINSTANCEUNIT" character varying(255),
    "INSTANCEPRIORITY" character varying(255),
    "NEWINSTANCEPRIORITY" character varying(255),
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_ROUTINGRULESDATA" OWNER TO postgres;

--
-- TOC entry 3420 (class 0 OID 0)
-- Dependencies: 213
-- Name: TABLE "SBS_ROUTINGRULESDATA"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SBS_ROUTINGRULESDATA" IS 'Constains all the routing rules data ';


--
-- TOC entry 3421 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."ROUTINGPOINTNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."ROUTINGPOINTNAME" IS 'Routing Point Name';


--
-- TOC entry 3422 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."FULL"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."FULL" IS 'Full';


--
-- TOC entry 3423 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."SEQ"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."SEQ" IS 'Rule Sequence Number';


--
-- TOC entry 3424 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."RULEDESCRIPTION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."RULEDESCRIPTION" IS 'Rule Description';


--
-- TOC entry 3425 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."SCHEMAMAP"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."SCHEMAMAP" IS 'Schema Mapping ';


--
-- TOC entry 3426 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."ENV" IS 'Environment value';


--
-- TOC entry 3427 (class 0 OID 0)
-- Dependencies: 213
-- Name: COLUMN "SBS_ROUTINGRULESDATA"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGRULESDATA"."VERSION" IS 'Version value';


--
-- TOC entry 212 (class 1259 OID 16956)
-- Name: SBS_ROUTINGSCHEMA; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SBS_ROUTINGSCHEMA" (
    "NAME" character varying(50) NOT NULL,
    "DESCRIPTION" character varying(255),
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_ROUTINGSCHEMA" OWNER TO postgres;

--
-- TOC entry 3429 (class 0 OID 0)
-- Dependencies: 212
-- Name: TABLE "SBS_ROUTINGSCHEMA"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SBS_ROUTINGSCHEMA" IS 'Sybes Routing schema  ';


--
-- TOC entry 3430 (class 0 OID 0)
-- Dependencies: 212
-- Name: COLUMN "SBS_ROUTINGSCHEMA"."NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGSCHEMA"."NAME" IS 'Unique Routing Schema Name';


--
-- TOC entry 3431 (class 0 OID 0)
-- Dependencies: 212
-- Name: COLUMN "SBS_ROUTINGSCHEMA"."DESCRIPTION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGSCHEMA"."DESCRIPTION" IS 'Description';


--
-- TOC entry 3432 (class 0 OID 0)
-- Dependencies: 212
-- Name: COLUMN "SBS_ROUTINGSCHEMA"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGSCHEMA"."ENV" IS 'Environment value';


--
-- TOC entry 3433 (class 0 OID 0)
-- Dependencies: 212
-- Name: COLUMN "SBS_ROUTINGSCHEMA"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_ROUTINGSCHEMA"."VERSION" IS 'Version Value';


--
-- TOC entry 232 (class 1259 OID 17468)
-- Name: SBS_RTG_KWD_DFN; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SBS_RTG_KWD_DFN" (
    "SYNTAXVERSION" character varying(50) NOT NULL,
    "IDENTIFIERNAME" character varying(255) NOT NULL,
    "FIELDNAME" character varying(255),
    "LINERANGEFROM" integer,
    "LINETO" integer,
    "COLUMNRANGEFROM" integer,
    "COLUMNTO" integer,
    "MESSAGENAME" character varying(255),
    "ENV" character varying(20) DEFAULT 'EN01'::character varying NOT NULL,
    "VERSION" character varying(100) DEFAULT '1.0'::character varying NOT NULL
);


ALTER TABLE "SBS_RTG_KWD_DFN" OWNER TO postgres;

--
-- TOC entry 3435 (class 0 OID 0)
-- Dependencies: 232
-- Name: TABLE "SBS_RTG_KWD_DFN"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SBS_RTG_KWD_DFN" IS 'Constains all the Routing KewyWod defintion  details';


--
-- TOC entry 3436 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."SYNTAXVERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."SYNTAXVERSION" IS 'Syntax of the version ';


--
-- TOC entry 3437 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."IDENTIFIERNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."IDENTIFIERNAME" IS 'Idetifier Name';


--
-- TOC entry 3438 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."FIELDNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."FIELDNAME" IS 'Field Name';


--
-- TOC entry 3439 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."LINERANGEFROM"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."LINERANGEFROM" IS 'Lines range from';


--
-- TOC entry 3440 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."LINETO"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."LINETO" IS 'Lines to';


--
-- TOC entry 3441 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."COLUMNRANGEFROM"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."COLUMNRANGEFROM" IS 'Columns range from ';


--
-- TOC entry 3442 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."COLUMNTO"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."COLUMNTO" IS 'Columns to ';


--
-- TOC entry 3443 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."MESSAGENAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."MESSAGENAME" IS 'Message Name';


--
-- TOC entry 3444 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."ENV" IS 'Environment value';


--
-- TOC entry 3445 (class 0 OID 0)
-- Dependencies: 232
-- Name: COLUMN "SBS_RTG_KWD_DFN"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SBS_RTG_KWD_DFN"."VERSION" IS 'Version Value';


--SET default_tablespace = rpl_indx;

--
-- TOC entry 3270 (class 2606 OID 17584)
-- Name: SBS_EXITPOINT_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_EXITPOINT"
    ADD CONSTRAINT "SBS_EXITPOINT_pkey" PRIMARY KEY ("ENV", "VERSION", "IDENTIFIERNAME", "QUEUETYPE");


--
-- TOC entry 3274 (class 2606 OID 17586)
-- Name: SBS_MESSAGEPARTNER_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_MESSAGEPARTNER"
    ADD CONSTRAINT "SBS_MESSAGEPARTNER_pkey" PRIMARY KEY ("ENV", "VERSION", "IDENTIFIERNAME");


--
-- TOC entry 3276 (class 2606 OID 17588)
-- Name: SBS_ROUTINGKEYWORD_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_ROUTINGKEYWORD"
    ADD CONSTRAINT "SBS_ROUTINGKEYWORD_pkey" PRIMARY KEY ("ENV", "VERSION", "NAME", "TYPE");


--
-- TOC entry 3268 (class 2606 OID 17590)
-- Name: SBS_ROUTINGPOINTNAME_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_ROUTINGPOINTNAME"
    ADD CONSTRAINT "SBS_ROUTINGPOINTNAME_pkey" PRIMARY KEY ("ENV", "VERSION", "NAME");


--
-- TOC entry 3266 (class 2606 OID 17592)
-- Name: SBS_ROUTINGRULESDATA_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_ROUTINGRULESDATA"
    ADD CONSTRAINT "SBS_ROUTINGRULESDATA_pkey" PRIMARY KEY ("ENV", "VERSION", "ROUTINGPOINTNAME", "SEQ");


--
-- TOC entry 3264 (class 2606 OID 17594)
-- Name: SBS_ROUTINGSCHEMA_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_ROUTINGSCHEMA"
    ADD CONSTRAINT "SBS_ROUTINGSCHEMA_pkey" PRIMARY KEY ("ENV", "VERSION", "NAME");


--
-- TOC entry 3272 (class 2606 OID 17596)
-- Name: SBS_RTG_KWD_DFN_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: rpl_indx
--

ALTER TABLE ONLY "SBS_RTG_KWD_DFN"
    ADD CONSTRAINT "SBS_RTG_KWD_DFN_pkey" PRIMARY KEY ("ENV", "VERSION", "SYNTAXVERSION", "IDENTIFIERNAME");


--
-- TOC entry 3277 (class 2606 OID 17597)
-- Name: SBS_ROUTINGRULESDATA_ENV_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SBS_ROUTINGRULESDATA"
    ADD CONSTRAINT "SBS_ROUTINGRULESDATA_ENV_fkey" FOREIGN KEY ("ENV", "VERSION", "SCHEMAMAP") REFERENCES "SBS_ROUTINGSCHEMA"("ENV", "VERSION", "NAME");


--
-- TOC entry 3404 (class 0 OID 0)
-- Dependencies: 231
-- Name: SBS_EXITPOINT; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "SBS_EXITPOINT" FROM PUBLIC;


--
-- TOC entry 3409 (class 0 OID 0)
-- Dependencies: 233
-- Name: SBS_MESSAGEPARTNER; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "SBS_MESSAGEPARTNER" FROM PUBLIC;


--
-- TOC entry 3416 (class 0 OID 0)
-- Dependencies: 234
-- Name: SBS_ROUTINGKEYWORD; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "SBS_ROUTINGKEYWORD" FROM PUBLIC;


--
-- TOC entry 3428 (class 0 OID 0)
-- Dependencies: 213
-- Name: SBS_ROUTINGRULESDATA; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "SBS_ROUTINGRULESDATA" FROM PUBLIC;


--
-- TOC entry 3434 (class 0 OID 0)
-- Dependencies: 212
-- Name: SBS_ROUTINGSCHEMA; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "SBS_ROUTINGSCHEMA" FROM PUBLIC;


--
-- TOC entry 3446 (class 0 OID 0)
-- Dependencies: 232
-- Name: SBS_RTG_KWD_DFN; Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON TABLE "SBS_RTG_KWD_DFN" FROM PUBLIC;


-- Completed on 2016-07-28 14:27:39

--
-- PostgreSQL database dump complete
--

