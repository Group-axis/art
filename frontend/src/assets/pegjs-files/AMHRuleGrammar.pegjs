Expression
  = head: (Assignment / MultiAssignment) tail: (_ And _ (Assignment / MultiAssignment))* {
     var result = head, i;
     var rs = {};
     //console.debug("head "+head.rw);
     if (head.rw) {
     	rs[head.rw.replace(/\//g,"")]=1;
     }
     var tailRw = "";
     for (i = 0; i < tail.length; i++) {
        tailRw = tail[i][3].rw;
        if (tailRw) {
        var rwt = tailRw.replace(/\//g,""); 
       // console.debug("and "+rwt);
        if (rwt && rs[rwt]) {
       // 	console.debug("twice "+rwt+" at "+tail[i][3].loc.start.column);
            error("found reserved word ["+tailRw+"] twice.",tail[i][3].loc)
        }
        }
      } 

	return result;
  }

Assignment = rw:ReservedWord _ "=" _ w:Word {
   //console.log("Assignment");
   return {"rw":rw, "loc":location()}; 
}
 
MultiAssignment
 = "(" head: Assignment tail: (_ Or _ Assignment)+ ")" {
   var result = head, i;
     var rs = {};
     //console.debug("Multi head "+head.rw);
     if (head.rw) {
     	rs[head.rw.replace(/\//g,"")]=1;
     }
     var tailRw = "";
     for (i = 0; i < tail.length; i++) {
        tailRw = tail[i][3].rw;
        if (tailRw) {
        
        var rwt = tailRw.replace(/\//g,""); 
     //   console.debug("or "+rwt + " rs " + rs[rwt]);
        if (rwt && rs[rwt] == undefined) {
      //  	console.debug("twice "+rwt+" at "+tail[i][3].loc.start.column);
            error("found reserved word ["+tailRw+"] but expected ["+head.rw+"].",tail[i][3].loc)
        }
        }
      } 

   return {"rw":"", "loc":location()};
 }


Word = SingleQuote Token SingleQuote  { var txt = text(); return txt.substring(1,txt.length-1);}

Token "'an expression'" = word: PointStart? [{\-:} \\/|+=,\-a-zA-Z0-9\.!_$\*]+ PointStart? { return text();}

PointStart = [\.{1-2}\*] {  return text();  }

SingleQuote "'"   =  "'" {  return text(); }



 And "'and'"   = "and"
 
 Or "'or'"   = "or"

 ReservedWord "a criteria ('direction' 'messageType/code', etc.)"
  = "field21" / "field57A" / "field57D" / "document/data" / "field79" / "messageReference" / "networkProtocol" /  "relatedReference" / "FINParameters/messageType" / "direction" / "messageType/code" / "swiftParameters/requestReference" / "swiftParameters/ackIndicator" / "senderAddress" / "receiverAddress" / "swiftParameters/service" / "swift.fin!p" / "swift.fin"
  { 
  //console.log("ReservedWord "+ text()+" " +text().indexOf("/")); 
  return text();}
  
_ "whitespace"   = [ ]