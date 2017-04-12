webpackJsonp([1],{904:function(e,t,n){"use strict";var i=n(14),o=n(922);i.Observable.prototype.reduce=o.reduce},922:function(e,t,n){"use strict";function reduce(e,t){return this.lift(new r(e,t))}var i=this&&this.__extends||function(e,t){function __(){this.constructor=e}for(var n in t)t.hasOwnProperty(n)&&(e[n]=t[n]);e.prototype=null===t?Object.create(t):(__.prototype=t.prototype,new __)},o=n(70);t.reduce=reduce;var r=function(){function ReduceOperator(e,t){this.project=e,this.seed=t}return ReduceOperator.prototype.call=function(e,t){return t._subscribe(new s(e,this.project,this.seed))},ReduceOperator}();t.ReduceOperator=r;var s=function(e){function ReduceSubscriber(t,n,i){e.call(this,t),this.hasValue=!1,this.acc=i,this.project=n,this.hasSeed="undefined"!=typeof i}return i(ReduceSubscriber,e),ReduceSubscriber.prototype._next=function(e){this.hasValue||(this.hasValue=this.hasSeed)?this._tryReduce(e):(this.acc=e,this.hasValue=!0)},ReduceSubscriber.prototype._tryReduce=function(e){var t;try{t=this.project(this.acc,e)}catch(e){return void this.destination.error(e)}this.acc=t},ReduceSubscriber.prototype._complete=function(){(this.hasValue||this.hasSeed)&&this.destination.next(this.acc),this.destination.complete()},ReduceSubscriber}(o.Subscriber);t.ReduceSubscriber=s},888:function(e,t,n){"use strict";function __export(e){for(var n in e)t.hasOwnProperty(n)||(t[n]=e[n])}__export(n(1153))},926:function(e,t,n){"use strict";function __export(e){for(var n in e)t.hasOwnProperty(n)||(t[n]=e[n])}__export(n(1167))},1167:function(e,t){"use strict";var n=function(){function Option(e,t,n,i){void 0===i&&(i=!1),this.code=t,this.id=e,this.description=n,this.selected=i}return Object.defineProperty(Option.prototype,"htmlText",{get:function(){return this.description},enumerable:!0,configurable:!0}),Option.fromJson=function(e){var t=JSON.parse(e);return new Option(t.id,t.code,t.description)},Option.mapToProperty=function(e){return e.id},Option}();t.Option=n},1153:function(e,t){"use strict";function updateFields(e,t,n){n.forEach(function(n){return e[n]=t[n]||e[n]})}var n=function(){function ItemContainer(e,t){this.id=e,this.description=t,this.children=[]}return Object.defineProperty(ItemContainer.prototype,"htmlText",{get:function(){return this.description},enumerable:!0,configurable:!0}),ItemContainer.prototype.addChild=function(e){this.children.push(e)},ItemContainer}();t.ItemContainer=n;var i=function(){function ItemChild(e,t){this.id=e,this.description=t}return Object.defineProperty(ItemChild.prototype,"htmlText",{get:function(){return this.description},enumerable:!0,configurable:!0}),ItemChild}();t.ItemChild=i;var o=function(){function IdCodeDescription(e,t,n){this.id=e,this.code=t,this.description=n}return Object.defineProperty(IdCodeDescription.prototype,"htmlText",{get:function(){return this.description},enumerable:!0,configurable:!0}),IdCodeDescription.fromJson=function(e){var t=JSON.parse(e);return new IdCodeDescription(t.id,t.code,t.description)},IdCodeDescription.empty=function(){return new IdCodeDescription(1,"","")},IdCodeDescription.mapToProperty=function(e){return e.code},IdCodeDescription}();t.IdCodeDescription=o,t.updateFields=updateFields},1168:function(e,t,n){"use strict";var i=n(886),o=function(){function AssignmentConfig(e,t){this.type=e,this.maxBackendsAllowed=this.calculeMaxBackendsAllowed(e,t)}return AssignmentConfig.prototype.calculeMaxBackendsAllowed=function(e,t){if(t)return t;switch(e){case i.AssignType.BK_CHANNEL:return 1;case i.AssignType.DTN_COPY:case i.AssignType.FEED_DTN_COPY:return 1e3}},AssignmentConfig.prototype.isTypeDifferent=function(e){return e!=i.AssignType[this.type]},AssignmentConfig.prototype.typeAsString=function(){return i.AssignType[this.type]},AssignmentConfig.prototype.showExtraFields=function(){switch(this.type){case i.AssignType.BK_CHANNEL:return!1;case i.AssignType.DTN_COPY:case i.AssignType.FEED_DTN_COPY:return!0}},AssignmentConfig}();t.AssignmentConfig=o},1169:function(e,t){"use strict";var n=function(){function AssignmentList(e,t,n,i,o,r,s,c,u,a,p){this.active=e,this.code=t,this.backCode=n,this.backDirection=i,this.backName=o,this.backSequence=r,this.ruleCode=s,this.ruleExpressions=c,this.ruleSequence=u,this.environment=a,this.version=p}return AssignmentList}();t.AssignmentList=n},1170:function(e,t){"use strict";var n=function(){function AssignmentRule(e,t,n,i,o,r,s){this.code=e,this.dataOwner=t,this.lockCode=n,this.sequence=i,this.environment=o,this.version=r,this.expression=s}return AssignmentRule}();t.AssignmentRule=n},886:function(e,t){"use strict";!function(e){e[e.BK_CHANNEL=1]="BK_CHANNEL",e[e.DTN_COPY=2]="DTN_COPY",e[e.FEED_DTN_COPY=4]="FEED_DTN_COPY"}(t.AssignType||(t.AssignType={}));t.AssignType},1130:function(e,t,n){"use strict";var i=n(886),o=function(){function AssignmentUniqueBackend(e,t,n,i){this.code=e,this.direction=t,this.dataOwner=n,this.lockCode=i}return AssignmentUniqueBackend.prototype.toWriteModel=function(e,t,n,o,r,s,c,u){switch(e){case i.AssignType.DTN_COPY:case i.AssignType.FEED_DTN_COPY:return{code:t,backCode:this.code,backDirection:this.direction,dataOwner:this.dataOwner,lockCode:this.lockCode,environment:n,version:o,modificationUserId:r,modificationDate:s,creationUserId:c,creationDate:u};default:return{}}},AssignmentUniqueBackend.fromJson=function(e){return new AssignmentUniqueBackend(e.code,e.direction,e.dataOwner,e.lockCode)},AssignmentUniqueBackend}();t.AssignmentUniqueBackend=o},1131:function(e,t,n){"use strict";var i=n(886),o=function(){function AssignmentUniqueRule(e,t,n,i,o){this.code=e,this.dataOwner=t,this.lockCode=n,this.sequence=i,this.expression=o}return AssignmentUniqueRule.prototype.toWriteModel=function(e,t,n,o,r,s,c,u){switch(e){case i.AssignType.BK_CHANNEL:case i.AssignType.DTN_COPY:case i.AssignType.FEED_DTN_COPY:return{code:t,sequence:this.sequence,ruleCode:this.code,dataOwner:this.dataOwner,lockCode:this.lockCode,environment:n,version:o,modificationUserId:r,modificationDate:s,creationUserId:c,creationDate:u}}},AssignmentUniqueRule.fromJson=function(e){return new AssignmentUniqueRule(e.code,e.dataOwner,e.lockCode,e.sequence,e.expression)},AssignmentUniqueRule}();t.AssignmentUniqueRule=o},1171:function(e,t,n){"use strict";var i=n(1130),o=n(1131),r=n(886),s=function(){function AssignmentUnique(e,t,n,i,o,r,s,c,u,a,p,d,h){this.active=e,this.backendPrimaryKey=t,this.code=n,this.dataOwner=i,this.description=o,this.lockCode=r,this.sequence=s||void 0,this.copies=c||void 0,this.name=u,this.environment=a,this.version=p,this.rules=d||[],this.backends=h||[]}return AssignmentUnique.prototype.backendCodeList=function(){return this.backendPrimaryKey?[this.backendPrimaryKey.code]:this.backends.map(function(e){return e.code})},AssignmentUnique.prototype.toWriteModel=function(e,t,n,i,o,s,c){var u=this,a=this.rules||[];a=a.map(function(r){return r.toWriteModel(e,u.code,t,n,i,o,s,c)});var p=this.backends||[];p=p.map(function(r){return r.toWriteModel(e,u.code,t,n,i,o,s,c)});var d={sequence:this.sequence,active:String(this.active),dataOwner:this.dataOwner,lockCode:this.lockCode,description:this.description,environment:t,version:n,rules:a,modificationUserId:i,modificationDate:o,creationUserId:s,creationDate:c};switch(e){case r.AssignType.BK_CHANNEL:d.backCode=this.backendPrimaryKey.code,d.backDirection=this.backendPrimaryKey.direction;break;case r.AssignType.DTN_COPY:case r.AssignType.FEED_DTN_COPY:d.copies=this.copies,d.name=this.name,d.backends=p}return d},AssignmentUnique.fromJson=function(e){var t=e.rules||[],n=e.backends||[];return new AssignmentUnique(e.active,e.backendPrimaryKey,e.code,e.dataOwner,e.description,e.lockCode,e.sequence,e.copies,e.name,e.environment,e.version,t.map(function(e){return o.AssignmentUniqueRule.fromJson(e)}),n.map(function(e){return i.AssignmentUniqueBackend.fromJson(e)}))},AssignmentUnique}();t.AssignmentUnique=s},1172:function(e,t){"use strict";var n=function(){function Assignment(e,t,n,i,o,r,s,c,u,a){this.active=e,this.backendPrimaryKey=t,this.code=n,this.dataOwner=i,this.description=o,this.lockCode=r,this.sequence=s,this.environment=c,this.version=u,this.rules=a||[]}return Assignment}();t.Assignment=n},1173:function(e,t){"use strict";var n=function(){function BackendPK(e,t){this.code=e,this.direction=t}return BackendPK.hashPK=function(e){return e.code.toString()+"@"+e.direction.toString()},BackendPK.hash=function(e,t){return e.toString()+"@"+t.toString()},BackendPK}();t.BackendPK=n},1174:function(e,t){"use strict";var n=function(){function Backend(e,t,n,i,o,r,s){this.pkCode=e,this.pkDirection=t,this.code=n,this.dataOwner=i,this.description=o,this.lockCode=r,this.name=s}return Backend}();t.Backend=n},880:function(e,t,n){"use strict";function __export(e){for(var n in e)t.hasOwnProperty(n)||(t[n]=e[n])}__export(n(1170)),__export(n(1172)),__export(n(1169)),__export(n(1173)),__export(n(1174)),__export(n(1176)),__export(n(1171)),__export(n(1131)),__export(n(1130)),__export(n(886)),__export(n(1168)),__export(n(1175))},1175:function(e,t){"use strict";!function(e){e[e.ALL=1]="ALL",e[e.ASSIGNED=2]="ASSIGNED",e[e.UNASSIGNED=4]="UNASSIGNED"}(t.RuleAssignType||(t.RuleAssignType={}));t.RuleAssignType},1176:function(e,t,n){"use strict";var i=n(542),o=function(e){function AMHRule(t,n,i,o,r,s,c){e.call(this),this.selected=!1,this.code=t,this.dataOwner=i,this.expression=n,this.lockCode=o,this.type=r}return __extends(AMHRule,e),AMHRule.prototype.toWriteModel=function(){return{expression:this.expression,dataOwner:this.dataOwner,lockCode:this.lockCode,ruleType:this.type,environment:this.environment,version:this.version,modificationUserId:this.modificationUserId,modificationDate:this.modificationDate,creationUserId:this.creationUserId,creationDate:this.creationDate}},AMHRule.prototype.setEnvAndVersion=function(e,t){this.environment=e,this.version=t},AMHRule.prototype.cloneWithUpperCaseCode=function(){return new AMHRule(this.code.toUpperCase(),this.expression,this.dataOwner,this.lockCode,this.type,this.environment,this.version)},AMHRule.fromJson=function(e){return new AMHRule(e.code,e.expression,e.dataOwner,e.lockCode,e.type,e.environment,e.version)},AMHRule}(i.BaseAudit);t.AMHRule=o},1200:function(e,t){"use strict";!function(e){e[e.Source=1]="Source",e[e.NewInstance=2]="NewInstance",e[e.SourceAndNewInstance=4]="SourceAndNewInstance"}(t.ActionOn||(t.ActionOn={}));t.ActionOn},1183:function(e,t,n){"use strict";var i=n(888),o=n(1179),r=n(1186),s=function(){function Action(e,t,n){this.actionOn=e,this.source=t,this.newInstance=n}return Action.empty=function(){return new Action(i.IdCodeDescription.empty(),o.Source.empty(),r.NewInstance.empty())},Action.prototype.update=function(e){Action.updateField(e,["actionOn","source","newInstance"],this)},Action.prototype.toWriteModel=function(){var e=1==this.actionOn.id?"SOURCE":2==this.actionOn.id?"NEW_INSTANCE":"SOURCE_AND_NEW_INSTANCE",t=this.newInstance.instanceType+(this.newInstance.instanceTypeOption?"_"+this.newInstance.instanceTypeOption:"");return{actionOn:e,instanceAction:this.source.action,instanceInterventionType:this.source.intervention,instanceInterventionTypeText:this.source.interventionText,instanceRoutingCode:this.source.routingCode,instanceTargetQueue:this.source.actionOption,instanceUnit:this.source.unit,instancePriority:this.source.priority,newInstanceAction:this.newInstance.action,newInstanceRoutingCode:this.newInstance.routingCode,newInstanceInterventionType:this.newInstance.intervention,newInstanceInterventionTypeText:this.newInstance.interventionText,newInstanceTargetQueue:this.newInstance.actionOption,newInstanceType:t,newInstanceUnit:this.newInstance.unit,newInstancePriority:this.newInstance.priority}},Action.updateField=function(e,t,n){t.forEach(function(t){return n[t]=e[t]||n[t]})},Action}();t.Action=s},1201:function(e,t){"use strict";!function(e){e[e.MESSAGE=1]="MESSAGE",e[e.FUNCTION=2]="FUNCTION",e[e.MESSAGEANDFUNCTION=4]="MESSAGEANDFUNCTION",e[e.ALWAYS=8]="ALWAYS"}(t.ConditionType||(t.ConditionType={}));t.ConditionType},1184:function(e,t,n){"use strict";var i=n(888),o=n(888),r=function(){function Condition(e,t,n){this.conditionOn=e,this.functions=t,this.message=n}return Condition.empty=function(){return new Condition(i.IdCodeDescription.empty(),[],"")},Condition.prototype.update=function(e){o.updateFields(this,e,["conditionOn","functions","message"])},Condition.prototype.toWriteModel=function(){var e=void 0;this.functions&&this.functions.length>0&&(e=this.functions.reduce(function(e,t){return e.length>0?e+","+t.description:t.description},""));var t=1==this.conditionOn.id?"MESSAGE":2==this.conditionOn.id?"FUNCTION":"MESSAGE_AND_FUNCTION";return{conditionOn:t,criteria:2!=this.conditionOn.id?this.message:"",functionList:1!=this.conditionOn.id?e:""}},Condition}();t.Condition=r},1185:function(e,t,n){"use strict";function __export(e){for(var n in e)t.hasOwnProperty(n)||(t[n]=e[n])}__export(n(1200)),__export(n(1183)),__export(n(1201)),__export(n(1184)),__export(n(1186)),__export(n(1204)),__export(n(1179)),__export(n(1203)),__export(n(1202))},1186:function(e,t,n){"use strict";var i=n(1179),o=function(e){function NewInstance(t,n,i,o,r,s,c,u,a){e.call(this,i,o,r,s,c,u,a),this.instanceType=t,this.instanceTypeOption=n}return __extends(NewInstance,e),NewInstance.empty=function(){return new NewInstance("","","","","","","","","")},NewInstance}(i.Source);t.NewInstance=o},1202:function(e,t){"use strict";var n=function(){function PointList(e,t,n,i,o,r,s,c,u){this.pointName=e,this.schemas=t,this.ruleSeq=n,this.ruleDescription=i,this.ruleMessage=o,this.originalPoint=r,this.originalCode=s,this.copyPoint=c,this.copyCode=u}return PointList}();t.PointList=n},1203:function(e,t){"use strict";var n=function(){function Point(e,t,n){this.pointName=e,this.full=t,this.rules=n}return Point}();t.Point=n},1204:function(e,t,n){"use strict";var i=n(1184),o=n(1183),r=n(888),s=function(){function Rule(e,t,n,i,o,r,s,c,u){this.sequence=e,this.routingPoint=t,this.description=n,this.schemas=i,this.lastModification=o,this.creationDate=r,this.createdBy=s,this.condition=c,this.action=u}return Rule.empty=function(){return new Rule(void 0,"","",[],void 0,void 0,void 0,i.Condition.empty(),o.Action.empty())},Rule.prototype.update=function(e){r.updateFields(this,e,["sequence","routingPoint","description","schemas","lastModification","creationDate","createdBy"]),this.condition.update(e.condition),this.action.update(e.action)},Rule.prototype.toWriteModel=function(e,t){return{full:"true",ruleDescription:this.description,environment:e,version:t,action:this.action.toWriteModel(),condition:this.condition.toWriteModel()}},Rule}();t.Rule=s},1179:function(e,t){"use strict";var n=function(){function Source(e,t,n,i,o,r,s){this.action=e,this.actionOption=t,this.intervention=n,this.interventionText=i,this.unit=o,this.routingCode=r,this.priority=s}return Source.empty=function(){return new Source("","","","","","","")},Source}();t.Source=n},543:function(e,t,n){"use strict";function __export(e){for(var n in e)t.hasOwnProperty(n)||(t[n]=e[n])}__export(n(1248)),__export(n(1187))},1218:function(e,t,n){e.exports=function(e){return new Promise(function(t){n.e(20,function(i){if(e)t(n(1209)[e]);else{var o=n(1209);t(o.__esModule?o.default:o)}})})}},1257:function(e,t,n){e.exports=function(e){return new Promise(function(t){n.e(18,function(i){if(e)t(n(1210)[e]);else{var o=n(1210);t(o.__esModule?o.default:o)}})})}},1258:function(e,t,n){e.exports=function(e){return new Promise(function(t){n.e(25,function(i){if(e)t(n(1211)[e]);else{var o=n(1211);t(o.__esModule?o.default:o)}})})}},1219:function(e,t,n){e.exports=function(e){return new Promise(function(t){n.e(24,function(i){if(e)t(n(1212)[e]);else{var o=n(1212);t(o.__esModule?o.default:o)}})})}},1220:function(e,t,n){e.exports=function(e){return new Promise(function(t){n.e(10,function(i){if(e)t(n(1213)[e]);else{var o=n(1213);t(o.__esModule?o.default:o)}})})}},1248:function(e,t,n){"use strict";var i=n(30),o=n(58),r=function(){function SAARouting(){}return SAARouting.prototype.ngOnInit=function(){},SAARouting.prototype.asyncDataWithWebpack=function(){},SAARouting=__decorate([i.Component({selector:"saa-routing",directives:[o.ROUTER_DIRECTIVES],template:"\n    <router-outlet></router-outlet>\n  "}),o.RouteConfig([{path:"/",name:"SAARoutingHome",loader:function(){return n(1258)("SAARoutingHome")},useAsDefault:!0},{path:"/routing-overview",name:"PointsOverview",loader:function(){return n(1257)("SAAPointsOverviewComponent")}},{path:"/routing-point/create",name:"PointCreate",loader:function(){return n(1219)("SAARoutingPoint")}},{path:"/routing-point/:id/edit",name:"PointEdit",loader:function(){return n(1219)("SAARoutingPoint")}},{path:"/routing-rule/create",name:"RuleCreate",loader:function(){return n(1220)("SAARoutingRule")}},{path:"/routing-rule/:pointName/:sequence/edit",name:"RuleEdit",loader:function(){return n(1220)("SAARoutingRule")}},{path:"/export",name:"SAAExport",loader:function(){return n(1218)("SAAExportImportComponent")}},{path:"/simulation",name:"SAASimulation",loader:function(){return n(1259)("SAASimulatorComponent")}},{path:"/import",name:"SAAImport",loader:function(){return n(1218)("SAAExportImportComponent")}}]),__metadata("design:paramtypes",[])],SAARouting)}();t.SAARouting=r},1187:function(e,t,n){"use strict";var i=n(257),o=n(30),r=n(14),s=n(880),c=n(1185),u=n(38),a=n(1185),p=n(926);n(904);var d=function(){function SAARoutingService(e,t,n){this.http=e,this.config=t,this.logger=n,this.schemaClause='\n  {\n    "match": {\n        "rules.schemas": {\n        "query": "##TO_REPLACE##",\n        "analyzer": "standard",\n        "operator": "and"\n        }\n      }\n  }\n  ',this.pointsBySchemaQuery='\n  { \n    "query": {\n    "bool": {\n      "must": [\n        {\n          "nested": {\n            "path": "rules",\n            "query": {\n              "bool": {\n                "should": [\n                  ##MATCH_CLAUSES##\n                ]\n              }\n            }\n          }\n        }\n      ]\n    }\n  }\n    ##FILTER_RULES_CLAUSE##\n    ##RANGE_SORT_CLAUSE##\n  }',this.filterRules='\n  ,\n  "filter":{\n    ##RULE_CLAUSE##\n  }',this.query=' \n    {\n  "query": {\n    ##RULE_CLAUSE##\n  }\n  ##RANGE_SORT_CLAUSE##\n}\n  ',this.fromSortClauses='\n  ,\n  "from": 0,\n  "size": 1000,\n  "sort": [\n    {\n      "pointName": {\n        "order": "asc"\n      }\n    },\n    {\n      "rules.sequence": {\n        "order": "desc"\n      }\n    }\n  ]\n  ',this.queryRule=' \n    "bool": {\n      "should": [\n        {\n          "match": {\n            "pointName": {\n              "query": "##TO_REPLACE##",\n              "analyzer": "standard",\n              "operator": "and"\n            }\n          }\n        },\n        {\n          "nested": {\n            "path": "rules.condition",\n            "query": {\n              "bool": {\n                "must": [\n                  {\n                    "match": {\n                      "rules.condition.message": {\n                        "query": "##TO_REPLACE##",\n                        "analyzer": "standard",\n                        "operator": "and"\n                      }\n                    }\n                  }\n                ]\n              }\n            }\n          }  \n        },\n        {\n          "nested": {\n            "path": "rules",\n            "query": {\n              "bool": {\n                "must": [\n                  {\n                    "match": {\n                      "rules.description": {\n                        "query": "##TO_REPLACE##",\n                        "analyzer": "standard",\n                        "operator": "and"\n                      }\n                    }\n                  }\n                ]\n              }\n            }\n          }\n        }\n      ]\n    }\n  '}return SAARoutingService.prototype.findAssignmentByID=function(e){return this.http.get("/products/${productID}").map(function(e){return e.json()})},SAARoutingService.prototype.findPoints=function(){return this.http.post(this.config.get("esBackUrl")+"/routing/points/_search",'{"sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}').map(function(e){return e.json()})},SAARoutingService.prototype.findPointNames=function(){return this.http.post(this.config.get("esBackUrl")+"/routing/points/_search?size=1000",'{ "fields": ["pointName"], "sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}').map(function(e){return e.json()}).flatMap(function(e){return r.Observable.from(e.hits.hits)}).map(function(e){var t={pointName:e.fields.pointName[0]};return t})},SAARoutingService.prototype.findPointMatches=function(e,t){return t?this.findPointMatchesJson(e,t).flatMap(this.toHits).flatMap(this.toPointList).reduce(function(e,t){return e.push(t),e},new Array):this.findPointMatchesJson(e,t).flatMap(this.toHits).flatMap(this.toPointList).reduce(function(e,t){return e.push(t),e},new Array)},SAARoutingService.prototype.toPointList=function(e){var t=[];return e.rules.length>0?e.rules.map(function(e){var n=e.action.source?e.action.source.actionOption:"",i=e.action.source?e.action.source.routingCode:"",o=e.action.newInstance?e.action.newInstance.actionOption:"",r=e.action.newInstance?e.action.newInstance.routingCode:"";t.push(new a.PointList(e.routingPoint,e.schemas,e.sequence,e.description,e.condition?e.condition.message:"",n,i,o,r))}):t.push(new a.PointList(e.pointName,"",void 0,"","")),r.Observable.from(t)},SAARoutingService.prototype.findPointMatchesJson=function(e,t){var n,i=t&&t.length>=1;if(e&&e.length>=1){var o=i?this.filterRules.replace(/##RULE_CLAUSE##/g,this.queryRule.replace(/##TO_REPLACE##/g,t)):"";n=this.pointsBySchemaQuery.replace(/##MATCH_CLAUSES##/g,this.createSchemaQueryClauses(e)).replace(/##FILTER_RULES_CLAUSE##/g,o).replace(/##RANGE_SORT_CLAUSE##/g,this.fromSortClauses)}else n=i?this.query.replace(/##RULE_CLAUSE##/g,this.queryRule.replace(/##TO_REPLACE##/g,t)).replace(/##RANGE_SORT_CLAUSE##/g,this.fromSortClauses):'{"sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}';return this.http.post(this.config.get("esBackUrl")+"/routing/points/_search?size=1000",n).map(function(e){return e.json()})},SAARoutingService.prototype.findPointByName=function(e){return this.http.get(this.config.get("esBackUrl")+"/routing/points/"+e).map(function(e){return e.json()}).map(function(e){return{found:e.found,source:e._source}})},SAARoutingService.prototype.findRuleByPointAndSequence=function(e,t){var n=this;return this.findPointByName(e).flatMap(function(e){if(e.found){var i=[],o=e.source.rules.find(function(e){return e.sequence==t});return o&&i.push({found:!0,value:o}),n.logger.log("ruleBySequence "+i),r.Observable.from(i)}return n.logger.error("no point found "),r.Observable.from([{found:!0,value:{}}])})},SAARoutingService.prototype.toRules=function(e){var t=[];return e.rules.length>0&&e.rules.map(function(e){e.schemas&&e.schemas.length>0,t.push(new c.Rule(e.sequence,e.routingPoint,e.description,e.schemas,void 0,void 0,e.condition,e.action))}),r.Observable.from(t)},SAARoutingService.prototype.createSchemaQueryClauses=function(e){var t=this;return!e||e.length<=0?"":e.map(function(e){return t.schemaClause.replace(/##TO_REPLACE##/g,e)}).join(",")},SAARoutingService.prototype.findOne=function(e,t){return!(!e||0==e.length||!t||0==t.length)&&t.some(function(t){return e.indexOf(t)!=-1})},SAARoutingService.prototype.findRulesBySchemas=function(e,t){var n=this;void 0===t&&(t=!0),this.logger.log(">> looking for rules with schemas "+JSON.stringify(e));var i=this.pointsBySchemaQuery.replace(/##MATCH_CLAUSES##/g,this.createSchemaQueryClauses(e)).replace(/##FILTER_RULES_CLAUSE##/g,"").replace(/##RANGE_SORT_CLAUSE##/g,this.fromSortClauses),o=this.http.post(this.config.get("esBackUrl")+"/routing/points/_search?size=1000",i).map(function(e){return e.json()}).flatMap(this.toHits).flatMap(this.toRules);return t&&(o=o.filter(function(t){var i=n.findOne(t.schemas,e);return n.logger.debug("in filter rules: "+t.schemas+" schemaFound = "+i),i})),o.reduce(function(e,t){return e.push(t),e},new Array)},SAARoutingService.prototype.findRuleByCode=function(e){var t=this;this.logger.log(">> looking for "+e);new s.AMHRule("","");return this.findRules().flatMap(function(n){var i=SAARoutingService.getFromSource(n).find(function(t){return t.code.toLowerCase()==(e?e.toLocaleLowerCase():"")});return t.logger.log(">> elastic return  "+i),i?r.Observable.create(function(e){return e.next(i),e.complete(),function(){return t.logger.log("disposed found")}}):r.Observable.create(function(e){return e.next(new s.AMHRule("","")),t.logger.log("returning to observer code=empty"),e.complete(),function(){return t.logger.log("disposed not found")}})})},SAARoutingService.prototype.findRules=function(){return this.http.get(this.config.get("esBackUrl")+"/amhrouting/rules/_search?size=1000").map(function(e){return e.json()})},SAARoutingService.prototype.findBackends=function(){return this.logger.log("findBackends url = "+this.config.get("esBackUrl")),this.http.get(this.config.get("esBackUrl")+"/amhrouting/backends/_search?size=1000").map(function(e){return e.json()})},SAARoutingService.prototype.saveRule=function(e,t,n){var o=this,r=new i.Headers({"Content-Type":"application/json"}),s=new i.RequestOptions({headers:r}),c=JSON.stringify(n.toWriteModel(e,t));return this.logger.log(" save rule sent: "+c),this.http.put(this.config.get("saaBackUrl")+"/points/"+n.routingPoint+"/rules/"+n.sequence,c,s).map(function(e){o.logger.log(" from save "+e.json()),e.json()})},SAARoutingService.prototype.createRule=function(e,t,n){var o=this,r=new i.Headers({"Content-Type":"application/json"}),s=new i.RequestOptions({headers:r}),c=JSON.stringify(n.toWriteModel(e,t));return this.logger.log(" create rule sent: "+c),this.http.post(this.config.get("saaBackUrl")+"/points/"+n.routingPoint+"/rules/"+n.sequence,c,s).map(function(e){o.logger.log(" from create "+e.json()),e.json()})},SAARoutingService.prototype.saveAssignment=function(e){var t=this,n=new i.Headers({"Content-Type":"application/json"}),o=new i.RequestOptions({headers:n}),r=this.assignToWriteModel(e);return this.logger.log(" save assignment sent: "+r),this.http.put(this.config.get("saaBackUrl")+"/amhrouting/assignments/"+e.code,r,o).map(function(e){t.logger.log(" from save "+e.json()),e.json()})},SAARoutingService.prototype.createAssignment=function(e){var t=this,n=new i.Headers({"Content-Type":"application/json"}),o=new i.RequestOptions({headers:n}),r=this.assignToWriteModel(e);return this.logger.log(" create assignment sent: "+r),this.http.post(this.config.get("saaBackUrl")+"/amhrouting/assignments/"+e.code,r,o).map(function(e){return t.logger.log(" from create "+e.json()),e.json()})},SAARoutingService.prototype.export=function(e,t,n){var o=this,r=new i.Headers({"Content-Type":"application/json"}),s=new i.RequestOptions({headers:r}),c=JSON.stringify({filePath:"useless",fileName:n,env:e,version:t});return this.logger.log(" export sent to SAA: "+c),this.http.post(this.config.get("saaBackUrl")+"/routing/export",c,s).map(function(e){return o.logger.log(" from export "+e.json().response),e.json()})},SAARoutingService.prototype.getExportedFile=function(e){return this.http.get(this.config.get("saaBackUrl")+"/routing/export/"+e).map(function(e){return e.json()})},SAARoutingService.prototype.import=function(e){var t=this,n=new i.Headers({"Content-Type":"application/json"}),o=new i.RequestOptions({headers:n}),r=JSON.stringify({filePath:e});return this.logger.log(" import sent: "+r),this.http.post(this.config.get("saaBackUrl")+"/routing/import",r,o).map(function(e){return t.logger.log(" from import "+e.json()),e.json()})},SAARoutingService.prototype.assignToWriteModel=function(e){var t=this,n={sequence:e.sequence,backCode:e.backendPrimaryKey.code,backDirection:e.backendPrimaryKey.direction,active:String(e.active),dataOwner:e.dataOwner,lockCode:e.lockCode,description:e.description,rules:[]},i=e.rules||[];return i.forEach(function(i){var o=t.assigRuleToModel(e.code,i);n.rules.push(o)}),JSON.stringify(n)},SAARoutingService.prototype.assigRuleToModel=function(e,t){return this.logger.log("assigRuleToModel with code "+e+" assignmentRule.sequence "+t.sequence+" assignmentRule.code "+t.code+" assignmentRule.dataOwner "+t.dataOwner+" assignmentRule.lockCode "+t.lockCode),{code:e,sequence:t.sequence,ruleCode:t.code,dataOwner:t.dataOwner,lockCode:t.lockCode}},SAARoutingService.getFromSource=function(e){var t=e.hits.hits,n=t.map(function(e){return e._source});return n},SAARoutingService.prototype.toHits=function(e){return r.Observable.from(e.hits.hits).map(function(e){return e._source})},SAARoutingService.prototype.toFoundHits=function(e){var t=e.hits.total;if(t>0){var n=e.hits.hits.map(function(e){return e._source});return r.Observable.from([{found:!0,hits:n}])}return r.Observable.from([{found:!1,hits:[]}])},SAARoutingService.prototype.findSchemas=function(){return this.logger.log("findSchemas url = "+this.config.get("esBackUrl")),this.http.get(this.config.get("esBackUrl")+"/routing/schemas/_search?size=1000").map(function(e){return e.json()}).flatMap(this.toFoundHits).map(function(e){return e.hits.map(function(e){return new p.Option(1,e.name,e.name)})})},SAARoutingService=__decorate([o.Injectable(),__metadata("design:paramtypes",[i.Http,u.Config,u.Logger])],SAARoutingService)}();t.SAARoutingService=d},1259:function(e,t,n){e.exports=function(e){return new Promise(function(t){n.e(8,function(i){if(e)t(n(1214)[e]);else{var o=n(1214);t(o.__esModule?o.default:o)}})})}}});
//# sourceMappingURL=1.c2d024fea936b7873685.bundle.map