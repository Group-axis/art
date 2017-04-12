--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.3
-- Dumped by pg_dump version 9.5.3

-- Started on 2016-09-05 15:50:21

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
--SET row_security = off;

SET search_path = public, pg_catalog;

SET default_with_oids = false;

--
-- TOC entry 187 (class 1259 OID 16436)
-- Name: SYS_APPLICATION_LOG; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_APPLICATION_LOG" (
    "ID_LOG" integer NOT NULL,
    "MODULE" character varying(100) NOT NULL,
    "SUB_MODULE" character varying(100) NOT NULL,
    "ENV" character varying(20) NOT NULL,
    "VERSION" bigint NOT NULL,
    "LEVEL_INFO" character varying(200),
    "DATE_START" timestamp without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    "DATE_END" timestamp without time zone,
    "ID_STATUS" numeric(38,0),
    "ID_USER" character varying(50),
    "NO_OF_LINES" numeric(38,0),
    "EXTR_INFO" character varying(1024)
);


ALTER TABLE "SYS_APPLICATION_LOG" OWNER TO postgres;

--
-- TOC entry 3411 (class 0 OID 0)
-- Dependencies: 187
-- Name: TABLE "SYS_APPLICATION_LOG"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_APPLICATION_LOG" IS 'System Application log table';


--
-- TOC entry 189 (class 1259 OID 16487)
-- Name: SYS_MODULE; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_MODULE" (
    "MODULE" character varying(20) NOT NULL,
    "NAME" character varying(50) NOT NULL,
    "ACTIVE" character varying(1) DEFAULT 'N'::character varying NOT NULL,
    "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);


ALTER TABLE "SYS_MODULE" OWNER TO postgres;

--
-- TOC entry 3412 (class 0 OID 0)
-- Dependencies: 189
-- Name: TABLE "SYS_MODULE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_MODULE" IS 'Tables contains all the avaialble modules for this application';


--
-- TOC entry 3413 (class 0 OID 0)
-- Dependencies: 189
-- Name: COLUMN "SYS_MODULE"."MODULE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_MODULE"."MODULE" IS 'Module Code';


--
-- TOC entry 3414 (class 0 OID 0)
-- Dependencies: 189
-- Name: COLUMN "SYS_MODULE"."NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_MODULE"."NAME" IS 'Name of module';


--
-- TOC entry 3415 (class 0 OID 0)
-- Dependencies: 189
-- Name: COLUMN "SYS_MODULE"."ACTIVE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_MODULE"."ACTIVE" IS 'Whether module is active ';


--
-- TOC entry 3416 (class 0 OID 0)
-- Dependencies: 189
-- Name: COLUMN "SYS_MODULE"."DATE_CREATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_MODULE"."DATE_CREATION" IS 'Date of creation ';


--
-- TOC entry 208 (class 1259 OID 16772)
-- Name: SYS_PARAMETER; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_PARAMETER" (
    "KEY_NAME" character varying(255) NOT NULL,
    "KEY_VALUE" character varying(255) NOT NULL
);


ALTER TABLE "SYS_PARAMETER" OWNER TO postgres;

--
-- TOC entry 3417 (class 0 OID 0)
-- Dependencies: 208
-- Name: TABLE "SYS_PARAMETER"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_PARAMETER" IS 'JSON type key value table';


--
-- TOC entry 202 (class 1259 OID 16708)
-- Name: SYS_PERMISSION; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_PERMISSION" (
    "MODULE" character varying(20) NOT NULL,
    "NAME" character varying(255),
    "TAG" character varying(255) NOT NULL,
    "ID_PERMISSION" integer NOT NULL
);


ALTER TABLE "SYS_PERMISSION" OWNER TO postgres;

--
-- TOC entry 3418 (class 0 OID 0)
-- Dependencies: 202
-- Name: TABLE "SYS_PERMISSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_PERMISSION" IS 'Table contains list of permissions we could give to profiles';


--
-- TOC entry 3419 (class 0 OID 0)
-- Dependencies: 202
-- Name: COLUMN "SYS_PERMISSION"."MODULE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PERMISSION"."MODULE" IS 'Module Name';


--
-- TOC entry 3420 (class 0 OID 0)
-- Dependencies: 202
-- Name: COLUMN "SYS_PERMISSION"."NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PERMISSION"."NAME" IS 'Display Name ';


--
-- TOC entry 3421 (class 0 OID 0)
-- Dependencies: 202
-- Name: COLUMN "SYS_PERMISSION"."TAG"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PERMISSION"."TAG" IS 'Action string(technical one';


--
-- TOC entry 206 (class 1259 OID 16739)
-- Name: SYS_PERMISSION_ID_PERMISSION_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE "SYS_PERMISSION_ID_PERMISSION_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "SYS_PERMISSION_ID_PERMISSION_seq" OWNER TO postgres;

--
-- TOC entry 3422 (class 0 OID 0)
-- Dependencies: 206
-- Name: SYS_PERMISSION_ID_PERMISSION_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE "SYS_PERMISSION_ID_PERMISSION_seq" OWNED BY "SYS_PERMISSION"."ID_PERMISSION";


--
-- TOC entry 204 (class 1259 OID 16718)
-- Name: SYS_PROFILE; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_PROFILE" (
    "ID_PROFILE" integer NOT NULL,
    "MODULE" character varying(20) NOT NULL,
    "ENV" character varying(20),
    "NAME" character varying(50),
    "ACTIVE" character varying DEFAULT 'Y'::character varying NOT NULL
);


ALTER TABLE "SYS_PROFILE" OWNER TO postgres;

--
-- TOC entry 3423 (class 0 OID 0)
-- Dependencies: 204
-- Name: TABLE "SYS_PROFILE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_PROFILE" IS 'Table contains profile details par  module / environment level ';


--
-- TOC entry 3424 (class 0 OID 0)
-- Dependencies: 204
-- Name: COLUMN "SYS_PROFILE"."ID_PROFILE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PROFILE"."ID_PROFILE" IS 'Unique serial id';


--
-- TOC entry 3425 (class 0 OID 0)
-- Dependencies: 204
-- Name: COLUMN "SYS_PROFILE"."MODULE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PROFILE"."MODULE" IS 'Module beloings to this id_profile';


--
-- TOC entry 3426 (class 0 OID 0)
-- Dependencies: 204
-- Name: COLUMN "SYS_PROFILE"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PROFILE"."ENV" IS 'Enviornment level segration ';


--
-- TOC entry 3427 (class 0 OID 0)
-- Dependencies: 204
-- Name: COLUMN "SYS_PROFILE"."NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PROFILE"."NAME" IS 'Display Name ';


--
-- TOC entry 3428 (class 0 OID 0)
-- Dependencies: 204
-- Name: COLUMN "SYS_PROFILE"."ACTIVE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_PROFILE"."ACTIVE" IS 'Default this profile is actve ';


--
-- TOC entry 203 (class 1259 OID 16716)
-- Name: SYS_PROFILE_ID_PROFILE_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE "SYS_PROFILE_ID_PROFILE_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "SYS_PROFILE_ID_PROFILE_seq" OWNER TO postgres;

--
-- TOC entry 3429 (class 0 OID 0)
-- Dependencies: 203
-- Name: SYS_PROFILE_ID_PROFILE_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE "SYS_PROFILE_ID_PROFILE_seq" OWNED BY "SYS_PROFILE"."ID_PROFILE";


--
-- TOC entry 207 (class 1259 OID 16766)
-- Name: SYS_PROFILE_PERMISSION; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_PROFILE_PERMISSION" (
    "ID_PERMISSION" integer NOT NULL,
    "ID_PROFILE" integer NOT NULL,
    "ACTIVE" character varying(1) DEFAULT 'N'::character varying NOT NULL
);


ALTER TABLE "SYS_PROFILE_PERMISSION" OWNER TO postgres;

--
-- TOC entry 3430 (class 0 OID 0)
-- Dependencies: 207
-- Name: TABLE "SYS_PROFILE_PERMISSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_PROFILE_PERMISSION" IS 'PROFILE_USER persmission';


--
-- TOC entry 210 (class 1259 OID 16790)
-- Name: SYS_STATUS_REFERENTIAL; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_STATUS_REFERENTIAL" (
    "TABLE_NAME" character varying(30) NOT NULL,
    "ID_STATUS" integer NOT NULL,
    "ID_TRANSLATION" integer,
    "DESCRIPTION" character varying(255)
);


ALTER TABLE "SYS_STATUS_REFERENTIAL" OWNER TO postgres;

--
-- TOC entry 3431 (class 0 OID 0)
-- Dependencies: 210
-- Name: TABLE "SYS_STATUS_REFERENTIAL"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_STATUS_REFERENTIAL" IS 'Referential table contains status for a given table ';


--
-- TOC entry 3432 (class 0 OID 0)
-- Dependencies: 210
-- Name: COLUMN "SYS_STATUS_REFERENTIAL"."TABLE_NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_STATUS_REFERENTIAL"."TABLE_NAME" IS 'Table Name';


--
-- TOC entry 3433 (class 0 OID 0)
-- Dependencies: 210
-- Name: COLUMN "SYS_STATUS_REFERENTIAL"."ID_STATUS"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_STATUS_REFERENTIAL"."ID_STATUS" IS 'ID Technique';


--
-- TOC entry 3434 (class 0 OID 0)
-- Dependencies: 210
-- Name: COLUMN "SYS_STATUS_REFERENTIAL"."ID_TRANSLATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_STATUS_REFERENTIAL"."ID_TRANSLATION" IS 'Translation ID';


--
-- TOC entry 3435 (class 0 OID 0)
-- Dependencies: 210
-- Name: COLUMN "SYS_STATUS_REFERENTIAL"."DESCRIPTION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_STATUS_REFERENTIAL"."DESCRIPTION" IS 'Default description ';


--
-- TOC entry 209 (class 1259 OID 16780)
-- Name: SYS_TYPE_REFERENTIAL; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_TYPE_REFERENTIAL" (
    "TABLE_NAME" character varying(30) NOT NULL,
    "COLUMN_NAME" character varying(30) NOT NULL,
    "ID_TYPE" integer NOT NULL,
    "TYPE_CODE" character varying(255),
    "ID_TRANSLATION" integer,
    "DESCRIPTION" character varying(255)
);


ALTER TABLE "SYS_TYPE_REFERENTIAL" OWNER TO postgres;

--
-- TOC entry 3436 (class 0 OID 0)
-- Dependencies: 209
-- Name: TABLE "SYS_TYPE_REFERENTIAL"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_TYPE_REFERENTIAL" IS 'System Type Referential table contains table column drop down list box generique';


--
-- TOC entry 200 (class 1259 OID 16650)
-- Name: SYS_USER; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_USER" (
    "ID_USER" character varying(50) NOT NULL,
    "FIRSTNAME" character varying(32) NOT NULL,
    "LASTNAME" character varying(32) NOT NULL,
    "EMAIL" character varying(62),
    "ACTIVE" character varying(1) DEFAULT 'Y'::character varying NOT NULL,
    "ID_USER_CREATION" character varying(50),
    "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()),
    "ID_USER_MODIFICATION" character varying(50),
    "DATE_MODIFICATION" timestamp without time zone
);


ALTER TABLE "SYS_USER" OWNER TO postgres;

--
-- TOC entry 3437 (class 0 OID 0)
-- Dependencies: 200
-- Name: TABLE "SYS_USER"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_USER" IS 'Sys table contains user information';


--
-- TOC entry 3438 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."ID_USER"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."ID_USER" IS 'Unqiue ID_USER';


--
-- TOC entry 3439 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."FIRSTNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."FIRSTNAME" IS 'Import Origin file Name';


--
-- TOC entry 3440 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."LASTNAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."LASTNAME" IS ' ';


--
-- TOC entry 3441 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."ID_USER_CREATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."ID_USER_CREATION" IS 'Id of user who created this entity';


--
-- TOC entry 3442 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."DATE_CREATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."DATE_CREATION" IS 'Date of creation ';


--
-- TOC entry 3443 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."ID_USER_MODIFICATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."ID_USER_MODIFICATION" IS 'user last modified this entity';


--
-- TOC entry 3444 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN "SYS_USER"."DATE_MODIFICATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER"."DATE_MODIFICATION" IS 'Last date of modification';


--
-- TOC entry 201 (class 1259 OID 16658)
-- Name: SYS_USER_DETAIL; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_USER_DETAIL" (
    "ID_USER" character varying(50) NOT NULL,
    "ID_USER_RESET" character varying(50),
    "DATE_RESET" timestamp without time zone DEFAULT timezone('utc'::text, now()),
    "LAST_CONNECTION_DATE" timestamp without time zone,
    "MD5" character varying(1024),
    "LOCK_STATUS" character varying(1) DEFAULT 'N'::character varying NOT NULL,
    "NB_FAILED_CONNECTION" integer DEFAULT 3 NOT NULL
);


ALTER TABLE "SYS_USER_DETAIL" OWNER TO postgres;

--
-- TOC entry 3445 (class 0 OID 0)
-- Dependencies: 201
-- Name: TABLE "SYS_USER_DETAIL"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_USER_DETAIL" IS 'User extra information';


--
-- TOC entry 3446 (class 0 OID 0)
-- Dependencies: 201
-- Name: COLUMN "SYS_USER_DETAIL"."ID_USER_RESET"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER_DETAIL"."ID_USER_RESET" IS 'The admin user resetted the account';


--
-- TOC entry 3447 (class 0 OID 0)
-- Dependencies: 201
-- Name: COLUMN "SYS_USER_DETAIL"."MD5"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_USER_DETAIL"."MD5" IS 'MD5 converted pwd';


--
-- TOC entry 205 (class 1259 OID 16728)
-- Name: SYS_USER_PROFILE; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_USER_PROFILE" (
    "ID_PROFILE" integer NOT NULL,
    "ID_USER" character varying(50) NOT NULL
);


ALTER TABLE "SYS_USER_PROFILE" OWNER TO postgres;

--
-- TOC entry 3448 (class 0 OID 0)
-- Dependencies: 205
-- Name: TABLE "SYS_USER_PROFILE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_USER_PROFILE" IS 'User belongs to particular profile';


--
-- TOC entry 188 (class 1259 OID 16481)
-- Name: SYS_VERSION; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "SYS_VERSION" (
    "MODULE" character varying(20) NOT NULL,
    "ENV" character varying(20) NOT NULL,
    "VERSION" character varying(100) NOT NULL,
    "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    "NAME" character varying(255),
    "ACTIVE" character varying(1) DEFAULT 'N'::character varying,
    "ID_USER_CREATION" character varying(25),
    "SOURCE_VERSION" character varying(100)
);


ALTER TABLE "SYS_VERSION" OWNER TO postgres;

--
-- TOC entry 3449 (class 0 OID 0)
-- Dependencies: 188
-- Name: TABLE "SYS_VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE "SYS_VERSION" IS 'Table Contains Module Version Information';


--
-- TOC entry 3450 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."MODULE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."MODULE" IS 'Module Name ';


--
-- TOC entry 3451 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."ENV"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."ENV" IS 'Environment code ';


--
-- TOC entry 3452 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."VERSION" IS 'Latest version ';


--
-- TOC entry 3453 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."DATE_CREATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."DATE_CREATION" IS 'Date Created';


--
-- TOC entry 3454 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."NAME"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."NAME" IS 'Version Name';


--
-- TOC entry 3455 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."ACTIVE"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."ACTIVE" IS 'only one active version per modue par version';


--
-- TOC entry 3456 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."ID_USER_CREATION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."ID_USER_CREATION" IS 'User who created this version';


--
-- TOC entry 3457 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN "SYS_VERSION"."SOURCE_VERSION"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN "SYS_VERSION"."SOURCE_VERSION" IS 'could be import or from other version';


--
-- TOC entry 3259 (class 2604 OID 16741)
-- Name: ID_PERMISSION; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PERMISSION" ALTER COLUMN "ID_PERMISSION" SET DEFAULT nextval('"SYS_PERMISSION_ID_PERMISSION_seq"'::regclass);


--
-- TOC entry 3260 (class 2604 OID 16721)
-- Name: ID_PROFILE; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PROFILE" ALTER COLUMN "ID_PROFILE" SET DEFAULT nextval('"SYS_PROFILE_ID_PROFILE_seq"'::regclass);


--
-- TOC entry 3265 (class 2606 OID 16599)
-- Name: PK_SYS_APPLICATION_LOG; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_APPLICATION_LOG"
    ADD CONSTRAINT "PK_SYS_APPLICATION_LOG" PRIMARY KEY ("ID_LOG");


--
-- TOC entry 3269 (class 2606 OID 16493)
-- Name: PK_SYS_MODULE; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_MODULE"
    ADD CONSTRAINT "PK_SYS_MODULE" PRIMARY KEY ("MODULE");


--
-- TOC entry 3276 (class 2606 OID 16751)
-- Name: PK_SYS_PERMISSION; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PERMISSION"
    ADD CONSTRAINT "PK_SYS_PERMISSION" PRIMARY KEY ("ID_PERMISSION");


--
-- TOC entry 3289 (class 2606 OID 16794)
-- Name: PK_SYS_STATUS_REFERENTIAL; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_STATUS_REFERENTIAL"
    ADD CONSTRAINT "PK_SYS_STATUS_REFERENTIAL" PRIMARY KEY ("TABLE_NAME", "ID_STATUS");


--
-- TOC entry 3287 (class 2606 OID 16784)
-- Name: PK_SYS_TYPE_REFERENTIAL; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_TYPE_REFERENTIAL"
    ADD CONSTRAINT "PK_SYS_TYPE_REFERENTIAL" PRIMARY KEY ("TABLE_NAME", "COLUMN_NAME", "ID_TYPE");


--
-- TOC entry 3271 (class 2606 OID 16656)
-- Name: PK_SYS_USER; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_USER"
    ADD CONSTRAINT "PK_SYS_USER" PRIMARY KEY ("ID_USER");


--
-- TOC entry 3281 (class 2606 OID 16738)
-- Name: PK_SYS_USER_PROFILE; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_USER_PROFILE"
    ADD CONSTRAINT "PK_SYS_USER_PROFILE" PRIMARY KEY ("ID_PROFILE", "ID_USER");


--
-- TOC entry 3267 (class 2606 OID 17448)
-- Name: PK_SYS_VERSION; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_VERSION"
    ADD CONSTRAINT "PK_SYS_VERSION" PRIMARY KEY ("MODULE", "ENV", "VERSION");


--
-- TOC entry 3285 (class 2606 OID 16779)
-- Name: SYS_PARAMETER_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PARAMETER"
    ADD CONSTRAINT "SYS_PARAMETER_pkey" PRIMARY KEY ("KEY_NAME", "KEY_VALUE");


--
-- TOC entry 3283 (class 2606 OID 16771)
-- Name: SYS_PROFILE_PERMISSION_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PROFILE_PERMISSION"
    ADD CONSTRAINT "SYS_PROFILE_PERMISSION_pkey" PRIMARY KEY ("ID_PERMISSION", "ID_PROFILE");


--
-- TOC entry 3279 (class 2606 OID 16727)
-- Name: SYS_PROFILE_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PROFILE"
    ADD CONSTRAINT "SYS_PROFILE_pkey" PRIMARY KEY ("ID_PROFILE");


--
-- TOC entry 3274 (class 2606 OID 16664)
-- Name: SYS_USER_DETAIL_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_USER_DETAIL"
    ADD CONSTRAINT "SYS_USER_DETAIL_pkey" PRIMARY KEY ("ID_USER");


--
-- TOC entry 3263 (class 1259 OID 17436)
-- Name: IDX_SYS_APPLICATION_LOG; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IDX_SYS_APPLICATION_LOG" ON "SYS_APPLICATION_LOG" USING btree ("ENV", "VERSION", "MODULE", "SUB_MODULE");


--
-- TOC entry 3277 (class 1259 OID 16749)
-- Name: UK_SYS_PERMISSION; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX "UK_SYS_PERMISSION" ON "SYS_PERMISSION" USING btree ("MODULE", "TAG");


--
-- TOC entry 3272 (class 1259 OID 16657)
-- Name: UK_SYS_USER; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX "UK_SYS_USER" ON "SYS_USER" USING btree ("ID_USER");


--
-- TOC entry 3290 (class 2606 OID 16676)
-- Name: FK_SYS_USER_DETAIL_SYS_USER; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_USER_DETAIL"
    ADD CONSTRAINT "FK_SYS_USER_DETAIL_SYS_USER" FOREIGN KEY ("ID_USER") REFERENCES "SYS_USER"("ID_USER");


--
-- TOC entry 3291 (class 2606 OID 17437)
-- Name: SYS_PERMISSION_MODULE_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PERMISSION"
    ADD CONSTRAINT "SYS_PERMISSION_MODULE_fkey" FOREIGN KEY ("MODULE") REFERENCES "SYS_MODULE"("MODULE");


--
-- TOC entry 3292 (class 2606 OID 16761)
-- Name: SYS_PROFILE_MODULE_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "SYS_PROFILE"
    ADD CONSTRAINT "SYS_PROFILE_MODULE_fkey" FOREIGN KEY ("MODULE") REFERENCES "SYS_MODULE"("MODULE");


-- Completed on 2016-09-05 15:50:24

--
-- PostgreSQL database dump complete
--

