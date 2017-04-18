/*
MSG:
ROUTING' and swiftParameters/requestReference = 'FDI05.*' and (senderAddress = 'ou=funds,ou=live,o=parbfrpp,o=swift' or senderAddress = 'ou=funds,ou=live,o=parbitmm,o=swift' or senderAddress = 'ou=funds,ou=live,o=parbesmx,o=swift' or senderAddress = 'cn=rta,ou=funds,ou=live,o=parblull,o=swift' or senderAddress = 'ou=funds,ou=live,o=parbjesh,o=swift')

(senderAddress = 'ou=funds,ou=live,o=parbfrpp,o=swift' or senderAddress = 'ou=funds,ou=live,o=parbitmm,o=swift' or senderAddress = 'ou=funds,ou=live,o=parbesmx,o=swift' or senderAddress = 'cn=rta,ou=funds,ou=live,o=parblull,o=swift' or senderAddress = 'ou=funds,ou=live,o=parbjesh,o=swift') and direction = 'ROUTING' and swiftParameters/requestReference = 'FDI05.*'

*/

Constant
 = "params" _ "=" _ "{" props: Properties "}" _ resp:Expression {
  //console.log(JSON.stringify(props));
  
  return resp;
 }

Properties
 = head: Property tail: ("," _? prop: Property)* {
   var obj = {}, tt,i;
   obj[head.key]=head.value;
   if (!options["parameters"]) { 
   	options["parameters"] = {};
   }
   options["parameters"][head.key] = head.value;
   for (i = 0; i < tail.length; i++) { 
        tt =tail[i][2];
      //  console.log(" PROP "+tt.key);
        options["parameters"][tt.key] = tt.value;
        obj[tt.key]=tt.value;
      } 
    return obj;  
 }

Property
 = "\"" key : Token "\"" ":" "\"" value: Token "\"" {
   return {"key":key, "value":value}
 }
 
Expression
  = head: (Equal / Different / MultiEqual) tail: (_ And _ (Equal / Different / MultiEqual))* {
     var result = false, i; 
     
     //console.debug(" head type "+head.type);
     if (head.type == "A") {
     //  console.debug("Exp head "+head.key);
      // console.log(" ++ Expre HEAD - " + head.key + " = " + options["parameters"][head.key.replace(/\//g,"")]);

      var value = options["parameters"][head.key.replace(/\//g,"_")];
      if (value) {
         result = value.search(new RegExp(head.value)) >= 0;
      } 
      
   //   console.debug("... Exp HEAD A "+result) 	
     } else if (head.type == "D") {
     //  console.debug("Exp head "+head.key);
      // console.log(" ++ Expre HEAD - " + head.key + " = " + options["parameters"][head.key.replace(/\//g,"")]);

      var value = options["parameters"][head.key.replace(/\//g,"_")];
      if (value) {
         result = value.search(new RegExp(head.value)) == -1;
      }
      
  //    console.debug("... Exp HEAD A "+result) 	
     }
     else { 
       result = head.result;
      // console.debug("... Exp HEAD M "+result) 	
     }
 
 	 var item, ruleKey, ruleValue;
     for (i = 0; i < tail.length; i++) {
        item=tail[i][3];
     //   console.log("tail item type "+ item.type);
        
        if ( item.type == "A" ) {
        ruleKey = item.key;
        ruleValue = item.value;
   //     console.log(" item key "+ item.key + " value " + item.value);
        var msgValue = options["parameters"][ruleKey.replace(/\//g,"_")];
        //console.log(" ++ EXpEqual TAIL -" + ruleValue + "=" + msgValue + "give " + (msgValue.search(new RegExp(ruleValue)) >= 0));
        if (msgValue) {
           result = result && msgValue.search(new RegExp(ruleValue)) >= 0;
        } 
        
        //console.log("-- "+ruleValue+" = "+msgValue+" ==> "+result);
                
        } else if ( item.type == "D" ) {
        ruleKey = item.key;
        ruleValue = item.value;
   //     console.log(" item key "+ item.key + " value " + item.value);
        var msgValue = options["parameters"][ruleKey.replace(/\//g,"_")];
        //console.log(" ++ EXpEqual TAIL -" + ruleValue + "=" + msgValue + "give " + (msgValue.search(new RegExp(ruleValue)) >= 0));
        if (msgValue) {
   	   result = result && msgValue.search(new RegExp(ruleValue)) == -1;
        } 
        
        //console.log("-- "+ruleValue+" = "+msgValue+" ==> "+result);
                
        }
        else {
        	result = result && item.result;
        }
      } 
      
	return result;
  }

Different = rw:ReservedWord _ "<>" _ w:Word {
   return {"key":rw, "value":w, "type":"D"};
}

Equal = rw:ReservedWord _ "=" _ w:Word {
   return {"key":rw, "value":w, "type":"A"};
}
 
MultiEqual
 = "(" head: Equal tail: (_ Or _ Equal)+ ")" {
   var result = false, i;
   // console.debug("Multi head "+JSON.stringify(options["parameters"]));
    //console.log("za zaz "+options["parameters"]["senderAddress"]);
    //console.log(" ++ MultiEqual HEAD - " + head.key + " = " + options["parameters"][head.key.replace(/\//g,"_")]);
    
    var value = options["parameters"][head.key.replace(/\//g,"_")];
	//console.log("-- "+head.key+" =  "+value+" ==> ");
    if (value) {
      result = value.search(new RegExp(head.value)) >= 0;
    } 
    
	//console.log("-- "+head["value"]+" =  "+value+" ==> "+result);
  
    var ruleKey, ruleValue;  
    for (i = 0; i < tail.length; i++) {
        ruleKey = tail[i][3].key;
        ruleValue = tail[i][3].value;
        var msgValue = options["parameters"][ruleKey.replace(/\//g,"_")];
        //console.log(" ++ MultiEqual TAIL -" + ruleValue + "=" + msgValue + "give " + (msgValue.search(new RegExp(ruleValue)) >= 0));
        if (msgValue) {
          result = result || msgValue.search(new RegExp(ruleValue)) >= 0;
        } 
        
        //console.log("-- "+ruleValue+" = "+msgValue+" ==> "+result);
      } 

   return {"type":"M", "result":result};
 }

Word = SingleQuote Token SingleQuote  { var txt = text(); return txt.substring(1,txt.length-1);}

Token = word: PointStart? [{\-:} \\/|+=,\-a-zA-Z0-9\.!_$\*]+ PointStart? { return text();}

PointStart = [\.{1-2}\*] {  return text();  }

SingleQuote "'"   =  "'" {  return text(); }
 
 And "'and'"   = "and"
 
 Or "'or'"   = "or"
 
 ReservedWord "one criteria (\"direction\" \"messageType/code\", etc.)"
  = "field21" / "field57A" / "field57D" / "document/data" / "field79" / "messageReference" / "networkProtocol" /  "relatedReference" / "FINParameters/messageType" / "direction" / "messageType/code" / "swiftParameters/requestReference" / "swiftParameters/ackIndicator" / "senderAddress" / "receiverAddress" / "swiftParameters/service" / "swift.fin!p" / "swift.fin" 
  { return text();}
  
_ "whitespace"   = [ ]