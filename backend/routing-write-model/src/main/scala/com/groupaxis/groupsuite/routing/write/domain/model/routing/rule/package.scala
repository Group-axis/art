package com.groupaxis.groupsuite.routing.write.domain.model.routing

package object rule {
 
 
  /**********************************************/
  /*  TYPES                                     */
  /**********************************************/
  
 sealed abstract class ConditionType
 case object MessageType extends ConditionType
 case object FunctionType extends ConditionType
 case object MessageAndFunctionType extends ConditionType
 
 sealed abstract class ActionType
 case object SourceType extends ActionType
 case object NewInstanceType extends ActionType
 case object SourceAndNewInstanceType extends ActionType
 
 /**********************************************/
 /*  MARSHALLERS                               */
 /**********************************************/

 
 
 
 
}