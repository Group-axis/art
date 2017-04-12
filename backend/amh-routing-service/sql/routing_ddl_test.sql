/*
 select count(*) from ROUTINGRULESDATA

select count(*) from ROUTINGRULESDATAS

select * from ROUTINGRULESDATA order by ROUTINGPOINTNAME

select * from ROUTINGRULESDATAS order by ROUTINGPOINTNAME

select length(criteria) from ROUTINGRULESDATA

select t.criteria from ROUTINGRULESDATA t where t.criteria like '%''BNPAGB22%'') and ((Mesg_type = ''544'') or (Mesg_type = ''545'') or (Mesg_type = ''546'')%'

alter table 
   ROUTINGRULESDATAS
modify 
( 
   criteria    varchar2(4000)
); 
 * */

--<ScriptOptions statementTerminator=";"/>

CREATE TABLE GRPDBA.ROUTINGRULESDATAS (
		ROUTINGPOINTNAME VARCHAR2(255) NOT NULL,
		FULL VARCHAR2(10),
		SEQ NUMBER NOT NULL,
		RULEDESCRIPTION VARCHAR2(255),
		SCHEMAMAP VARCHAR2(26),
		CONDITIONON VARCHAR2(500),
		FUNCTIONLIST VARCHAR2(4000),
		CRITERIA VARCHAR2(500),
		ACTIONON VARCHAR2(500),
		INSTANCEACTION VARCHAR2(255),
		INSTANCEINTERVENTIONTYPE VARCHAR2(255),
		INSTANCEROUTINGCODE VARCHAR2(255),
		INSTANCETARGETQUEUE VARCHAR2(255),
		NEWINSTTYPE VARCHAR2(255),
		NEWINSTACTION VARCHAR2(255),
		NEWINSTTARGETQUEUE VARCHAR2(255),
		NEWINSTINTERVENTIONTYPE VARCHAR2(255),
		NEWINSTINSTANCEROUTINGCODE VARCHAR2(255),
		INSTANCEINTERVENTIONTYPETEXT VARCHAR2(500),
		NEWINSTANCEINTERVENTIONTYPTXT VARCHAR2(500),
		INSTANCEUNIT VARCHAR2(255),
		NEWINSTANCEUNIT VARCHAR2(255),
		INSTANCEPRIORITY VARCHAR2(255),
		NEWINSTANCEPRIORITY VARCHAR2(255)
	);

ALTER TABLE GRPDBA.ROUTINGRULESDATAS ADD CONSTRAINT PK_ROUTINGRULESDATAS PRIMARY KEY (ROUTINGPOINTNAME, SEQ);
--DROP TRIGGER GRPDBA.TRG_ROUTINGRULESDATAS_ARIUD
CREATE OR REPLACE TRIGGER GRPDBA.TRG_ROUTINGRULESDATAS_ARIUD
AFTER INSERT OR UPDATE OR DELETE
ON ROUTINGRULESDATAS
FOR EACH ROW
DECLARE
-------------------------------------------------------------------------------
-- TRIGGER              :  TRG_ROUTINGRULESDATA_ARIUD
--
--
-- DESCRIPTION          :
--                      Audit trigger on table ROUTINGRULESDATA  AFTER INSERT/UPDATE/DELETE
--
--
-- ERREURS              :
--
-- REVISIONS  : DATE         PAR         MOTIF
--              ----------- ----------- ---------------------------------------
--              JJ/MM/AAAA               CREATION
--              11/05/2016  K.Rajan      V1.0
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
--                          VARIABLES GESTION ERREUR                         --
-------------------------------------------------------------------------------
LV_TYPE_CHANGEMENT              VARCHAR2(1)                                   ;
LIGNE_ANCIEN                    ROUTINGRULESDATAS%ROWTYPE              ;
LIGNE_NOUVEAU                   ROUTINGRULESDATAS%ROWTYPE              ;
LV_LOG_INFO                     VARCHAR2(200)                                 ;
LV_RETURN                       NUMBER                                        ;
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
FUNCTION COPIE_ANCIEN RETURN ROUTINGRULESDATAS%ROWTYPE IS lV_RETOUR ROUTINGRULESDATAS%ROWTYPE; 
-------------------------------------------------------------------------------
BEGIN
   LV_RETOUR.ROUTINGPOINTNAME                     :=  :OLD.ROUTINGPOINTNAME;                                            
   LV_RETOUR.FULL                                 :=  :OLD.FULL;                                                        
   LV_RETOUR.SEQ                                  :=  :OLD.SEQ;                                                         
   LV_RETOUR.RULEDESCRIPTION                      :=  :OLD.RULEDESCRIPTION;                                             
   LV_RETOUR.SCHEMAMAP                            :=  :OLD.SCHEMAMAP;                                                   
   LV_RETOUR.CONDITIONON                          :=  :OLD.CONDITIONON;                                                 
   LV_RETOUR.FUNCTIONLIST                         :=  :OLD.FUNCTIONLIST;                                                
   LV_RETOUR.CRITERIA                             :=  :OLD.CRITERIA;                                                    
   LV_RETOUR.ACTIONON                             :=  :OLD.ACTIONON;                                                    
   LV_RETOUR.INSTANCEACTION                       :=  :OLD.INSTANCEACTION;                                              
   LV_RETOUR.INSTANCEINTERVENTIONTYPE             :=  :OLD.INSTANCEINTERVENTIONTYPE;                                    
   LV_RETOUR.INSTANCEROUTINGCODE                  :=  :OLD.INSTANCEROUTINGCODE;                                         
   LV_RETOUR.INSTANCETARGETQUEUE                  :=  :OLD.INSTANCETARGETQUEUE;                                         
   LV_RETOUR.NEWINSTTYPE                          :=  :OLD.NEWINSTTYPE;                                                 
   LV_RETOUR.NEWINSTACTION                        :=  :OLD.NEWINSTACTION;                                               
   LV_RETOUR.NEWINSTTARGETQUEUE                   :=  :OLD.NEWINSTTARGETQUEUE;                                          
   LV_RETOUR.NEWINSTINTERVENTIONTYPE              :=  :OLD.NEWINSTINTERVENTIONTYPE;                                     
   LV_RETOUR.NEWINSTINSTANCEROUTINGCODE           :=  :OLD.NEWINSTINSTANCEROUTINGCODE;                                  
   RETURN lV_RETOUR;
END COPIE_ANCIEN;
-------------------------------------------------------------------------------
FUNCTION COPIE_NOUVEAU RETURN ROUTINGRULESDATAS%ROWTYPE IS lV_RETOUR ROUTINGRULESDATAS%ROWTYPE; 
-------------------------------------------------------------------------------
BEGIN
   LV_RETOUR.ROUTINGPOINTNAME                     :=  :NEW.ROUTINGPOINTNAME;                                            
   LV_RETOUR.FULL                                 :=  :NEW.FULL;                                                        
   LV_RETOUR.SEQ                                  :=  :NEW.SEQ;                                                         
   LV_RETOUR.RULEDESCRIPTION                      :=  :NEW.RULEDESCRIPTION;                                             
   LV_RETOUR.SCHEMAMAP                            :=  :NEW.SCHEMAMAP;                                                   
   LV_RETOUR.CONDITIONON                          :=  :NEW.CONDITIONON;                                                 
   LV_RETOUR.FUNCTIONLIST                         :=  :NEW.FUNCTIONLIST;                                                
   LV_RETOUR.CRITERIA                             :=  :NEW.CRITERIA;                                                    
   LV_RETOUR.ACTIONON                             :=  :NEW.ACTIONON;                                                    
   LV_RETOUR.INSTANCEACTION                       :=  :NEW.INSTANCEACTION;                                              
   LV_RETOUR.INSTANCEINTERVENTIONTYPE             :=  :NEW.INSTANCEINTERVENTIONTYPE;                                    
   LV_RETOUR.INSTANCEROUTINGCODE                  :=  :NEW.INSTANCEROUTINGCODE;                                         
   LV_RETOUR.INSTANCETARGETQUEUE                  :=  :NEW.INSTANCETARGETQUEUE;                                         
   LV_RETOUR.NEWINSTTYPE                          :=  :NEW.NEWINSTTYPE;                                                 
   LV_RETOUR.NEWINSTACTION                        :=  :NEW.NEWINSTACTION;                                               
   LV_RETOUR.NEWINSTTARGETQUEUE                   :=  :NEW.NEWINSTTARGETQUEUE;                                          
   LV_RETOUR.NEWINSTINTERVENTIONTYPE              :=  :NEW.NEWINSTINTERVENTIONTYPE;                                     
   LV_RETOUR.NEWINSTINSTANCEROUTINGCODE           :=  :NEW.NEWINSTINSTANCEROUTINGCODE;                                  
   RETURN lV_RETOUR;
END COPIE_NOUVEAU;
-------------------------------------------------------------------------------
FUNCTION INSERT_NOUVEAU( PV_ROWTYPE_I ROUTINGRULESDATAS%ROWTYPE)  RETURN NUMBER IS    
-------------------------------------------------------------------------------
BEGIN
   INSERT INTO AUDIT_ROUTINGRULESDATAS ( 
           ROUTINGPOINTNAME                                  ,
           FULL                                              ,
           SEQ                                               ,
           RULEDESCRIPTION                                   ,
           SCHEMAMAP                                         ,
           CONDITIONON                                       ,
           FUNCTIONLIST                                      ,
           CRITERIA                                          ,
           ACTIONON                                          ,
           INSTANCEACTION                                    ,
           INSTANCEINTERVENTIONTYPE                          ,
           INSTANCEROUTINGCODE                               ,
           INSTANCETARGETQUEUE                               ,
           NEWINSTTYPE                                       ,
           NEWINSTACTION                                     ,
           NEWINSTTARGETQUEUE                                ,
           NEWINSTINTERVENTIONTYPE                           ,
           NEWINSTINSTANCEROUTINGCODE                        ,
           AUDIT_DATE                                        ,
           TYPE_CHANGEMENT                                   ,
           UTILISATEUR_CONNEXION )                           
   VALUES (
           PV_ROWTYPE_I.ROUTINGPOINTNAME                    ,
           PV_ROWTYPE_I.FULL                                ,
           PV_ROWTYPE_I.SEQ                                 ,
           PV_ROWTYPE_I.RULEDESCRIPTION                     ,
           PV_ROWTYPE_I.SCHEMAMAP                           ,
           PV_ROWTYPE_I.CONDITIONON                         ,
           PV_ROWTYPE_I.FUNCTIONLIST                        ,
           PV_ROWTYPE_I.CRITERIA                            ,
           PV_ROWTYPE_I.ACTIONON                            ,
           PV_ROWTYPE_I.INSTANCEACTION                      ,
           PV_ROWTYPE_I.INSTANCEINTERVENTIONTYPE            ,
           PV_ROWTYPE_I.INSTANCEROUTINGCODE                 ,
           PV_ROWTYPE_I.INSTANCETARGETQUEUE                 ,
           PV_ROWTYPE_I.NEWINSTTYPE                         ,
           PV_ROWTYPE_I.NEWINSTACTION                       ,
           PV_ROWTYPE_I.NEWINSTTARGETQUEUE                  ,
           PV_ROWTYPE_I.NEWINSTINTERVENTIONTYPE             ,
           PV_ROWTYPE_I.NEWINSTINSTANCEROUTINGCODE          ,
           SYSTIMESTAMP                                     ,
           LV_TYPE_CHANGEMENT                               ,
           LV_LOG_INFO);                                    
   RETURN 0;
END INSERT_NOUVEAU;
BEGIN
   SELECT 'Host <'||SYS_CONTEXT('USERENV','HOST' ) ||'> OS_USER < '||SYS_CONTEXT('USERENV','OS_USER' ) ||'> LOGIN   < '||SYS_CONTEXT('USERENV','SESSION_USER' ) ||'>' INTO LV_LOG_INFO  FROM  dual;
   LIGNE_ANCIEN  := COPIE_ANCIEN();
   LIGNE_NOUVEAU := COPIE_NOUVEAU();
   IF INSERTING THEN
      LV_TYPE_CHANGEMENT := 'I';
      LV_RETURN := INSERT_NOUVEAU(LIGNE_NOUVEAU);
   ELSIF UPDATING THEN
        LV_TYPE_CHANGEMENT := 'U';
      LV_RETURN := INSERT_NOUVEAU(LIGNE_NOUVEAU);
   ELSIF DELETING THEN
        LV_TYPE_CHANGEMENT := 'D';
      LV_RETURN := INSERT_NOUVEAU(LIGNE_ANCIEN);
   END IF;
END;

