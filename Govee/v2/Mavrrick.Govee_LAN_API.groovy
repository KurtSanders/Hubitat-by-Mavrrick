library (
 author: "Mavrrick",
 category: "Govee",
 description: "Govee LAN API",
 name: "Govee_LAN_API",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

//////////////////////////////
// Standard device Commands //
//////////////////////////////

def lanOn() {
    sendCommandLan(GoveeCommandBuilder("turn",1, "turn"))
    runInMillis(500, 'devStatus')
    if (descLog) log.info "${device.label} was turned on."  
}

def lanOff() {
    sendCommandLan(GoveeCommandBuilder("turn",0, "turn"))
    runInMillis(500, 'devStatus')
    if (descLog) log.info "${device.label} was turned off."
} 

def lanCT(value, level, transitionTime) {
    int intvalue = value.toInteger()
    sendCommandLan(GoveeCommandBuilder("colorwc",value, "ct"))
    if (level != null) lanSetLevel(level,transitionTime);
    sendEvent(name: "colorMode", value: "CT")
    runInMillis(500, 'devStatus')   
    if (effectNum != 0){
        sendEvent(name: "effectNum", value: 0)
        sendEvent(name: "effectName", value: "None") 
    }
	setCTColorName(intvalue)
}

def lanSetLevel(float v,duration = 0){
    int intv = v.toInteger()
    switch(true) {
        case intv > 100:
        	if (debugLog) {log.debug ("lanSetLevel(): Value of ${v} is over 100. Setting to 100")};
            intv = 100;
        break;
        case intv < 0:
        	if (debugLog) {log.debug ("lanSetLevel(): Value of ${v} is below 0. Setting to 0")};
            intv = 0;
        break;
        default: 
            if (debugLog) {log.debug ("lanSetLevel(): Setting Level to ${v}")};
        break;
    } 
//    int intv = v.toInteger()
//    if (descLog) log.info "${device.label} Level was set to ${intv}%"
    if (duration>0){
        int intduration = duration.toInteger()
//        sendEvent(name: "switch", value: "on")
    runInMillis(500, 'devStatus')
        fade(intv,intduration)
    }
    else {
        lanSetLevel2(intv)
    }
}

def lanSetLevel2(int v){
    if (descLog) log.info "${device.label} Level was set to ${v}%"      
    sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
    runInMillis(500, 'devStatus')
}

def fade(int v,float duration){
    unschedule(fadeUp)
    unschedule(fadeDown)
    int curLevel = device.currentValue("level")
    if (v < curLevel){
    float fadeRep = (curLevel-v)/fadeInc
    float fadeInt = (duration*1000)/fadeRep
    fadeDown(curLevel, v, fadeRep, fadeInt)
        }
    else if (v > curLevel){
    float fadeRep = (v-curLevel)/fadeInc
    float fadeInt = (duration*1000)/fadeRep
    fadeUp(curLevel, v, fadeRep, fadeInt)
        }
    else {
        if (debugLog) {log.debug "fade(): Level is not changing"}
    }
}

def fadeDown( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeDown(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v = (curLevel-fadeInc).toInteger()
//    log.debug "fadeDown(): v ${v}"
    if (v <= level) {
        if (debugLog) {log.debug "Final Loop Setting level to ${level}"}
        if ( level == 0 ) {
            log.debug "fadeDown(): to off"
            off()
        } else {
            log.debug "fadeDown(): Final fade to ${level}"
            sendCommandLan(GoveeCommandBuilder("brightness",level, "level"))
//            sendEvent(name: "level", value: level) 
            runInMillis(500, 'devStatus')
        }
    } else {
        log.debug "fadeDown(): Fade to ${v}"
            sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
            runInMillis(500, 'devStatus')
//            sendEvent(name: "level", value: v)
//            if (debugLog) {log.debug "fadeDown(): continueing  fading to ${v}"}
            def int delay = fadeRep
            if (debugLog) {log.debug "fadeDown(): delay ia ${delay}"}
            if (debugLog) {log.debug "fadeDown(): executing loop to fadedown() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
            runInMillis(delay, fadeDown, [data:[v ,level, fadeInt,fadeRep]])
    }
} 

def fadeUp( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeUp(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v= (curLevel+fadeInc).toInteger()
//    log.debug "fadeUp(): v ${v}"
    if (v >= level)    {
        log.debug "fadeUp(): Final fade to ${level}"
        sendCommandLan(GoveeCommandBuilder("brightness",level, "level"))
//        sendEvent(name: "level", value: level)
        runInMillis(500, 'devStatus')
    }
    else {
        log.debug "fadeUp(): Fade to ${v}"
        def int delay= fadeRep
        if (debugLog) {log.debug "fadeUp(): delay ia ${delay}"}
        if (debugLog) {log.debug "fadeUp(): executing loop to fadeup() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
//        sendEvent(name: "level", value: v)
        runInMillis(500, 'devStatus')
        runInMillis(delay, fadeUp, [data:[v ,level, fadeInt,fadeRep]])
    }
} 

def lanSetEffect (effectNo) {
//    effectNumber = effectNo.toInteger()
    effectNumber = effectNo.toString()
    lanScenes = loadSceneFile()
    if (descLog) log.info "${device.label} SetEffect: ${effectNumber}"
    if (lanScenes.keySet().contains(device.getDataValue("DevType"))) {
        tag = device.getDataValue("DevType")
    } else if (lanScenes.keySet().contains(device.getDataValue("deviceModel"))) {
        tag = device.getDataValue("deviceModel")
    } 
    if (debugLog) log.debug "${lanScenes.get("${tag}").keySet()}"
//    if (descLog) log.info "${lanScenes.get(device.getDataValue("DevType")).get(effectNumber)}"
    if (lanScenes.get("${tag}").containsKey(effectNumber)) {
        String sceneInfo =  lanScenes.get("${tag}").get(effectNumber).name
        String sceneCmd =  lanScenes.get("${tag}").get(effectNumber).cmd
        if (debugLog) {log.debug ("setEffect(): Activate effect number ${effectNo} called ${sceneInfo} with command ${sceneCmd}")}
        if (debugLog) log.debug "Scene number is present"
        sendEvent(name: "colorMode", value: "EFFECTS")
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
//    if (debugLog) {log.debug ("setEffect(): setEffect to ${effectNo}")}
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("setEffect(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
   } else {
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
    // Cozy Light Effect (static Scene to very warm light)
    if (effectNo == 6) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cozy Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        sendEvent(name: "effectName", value: "Cozy")
    }
    // Sunrise Effect
    if (effectNo == 9) {
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",1, "level"))
        sendEvent(name: "level", value: 1)
        fade(100,1800)        
        sendEvent(name: "effectName", value: "Sunrise")
    }
    // Sunset Effect
    if (effectNo == 10) {
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        fade(0,1800)
        sendEvent(name: "effectName", value: "Sunset")
    }
    // Warm White Light Effect (static Scene to very warm light)
    if (effectNo == 11) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Warm White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3500, "ct"))
        sendEvent(name: "colorTemperature", value: 3500)
	    setCTColorName(3500)
        sendEvent(name: "effectName", value: "Warm White")
    } 
    // Daylight Light Effect    
    if (effectNo == 12) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Daylight Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",5600, "ct"))
        sendEvent(name: "colorTemperature", value: 5600)
	    setCTColorName(5600)
        sendEvent(name: "effectName", value: "Daylight")
    }
    // Cool White Light Effect    
    if (effectNo == 13) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cool White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        sendEvent(name: "colorTemperature", value: 6500)
	    setCTColorName(6500)
        sendEvent(name: "effectName", value: "Cool White")
    }  
    // Night Light Effect   
    if (effectNo == 14) {
        if (debugLog) {log.debug ("setEffect(): Static Night Light Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",5, "level"))
        sendEvent(name: "level", value: 5)
        sendEvent(name: "effectName", value: "Night Light")
    }
    // Focus Effect   
    if (effectNo == 15) {
        if (debugLog) {log.debug ("setEffect(): Focus Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",4500, "ct"))
        sendEvent(name: "colorTemperature", value: 4500)
	    setCTColorName(4500)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Focus")
    } 
    // Relax Effect   
    if (effectNo == 16) {
        if (debugLog) {log.debug ("setEffect(): Static Relax Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:255, g:194, b:194], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Relax")
    }
    // True Color Effect   
    if (effectNo == 17) {
        if (debugLog) {log.debug ("setEffect(): True Color Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3350, "ct"))
        sendEvent(name: "colorTemperature", value: 3350)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "effectName", value: "True Color")
    }
    // TV Time Effect   
    if (effectNo == 18) {
        if (debugLog) {log.debug ("setEffect(): Static TV Time Effect Called. Calling CT Command directly")}        
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:179, g:134, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "TV Time")
    }
    // Plant Growth Effect   
    if (effectNo == 19) {
        if (debugLog) {log.debug ("setEffect(): Static Plant Growth Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:247, g:154, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "Plant Growth")
    }
    } 
}

def lanSetNextEffect () {
    if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) {log.debug ("setNextEffect(): current effectNum ${device.currentValue("effectNum")}")}
    if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == device.getDataValue("maxScene")) {
        setEffect(1)
    } 
    else if (device.currentValue("effectNum") == "21") {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        setEffect(101) 
    } else {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        int nextEffect = device.currentValue("effectNum").toInteger()+1
        setEffect(nextEffect)
        }  
}

def lanSetPreviousEffect () {
        if (debugLog) {log.debug ("setPreviousEffect(): Current Color mode ${device.currentValue("colorMode")}")}
        unschedule(fadeUp)
        unschedule(fadeDown)
        if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == "1") {
            setEffect(device.getDataValue("maxScene"))
            } else if (device.currentValue("effectNum") == 101) {
            setEffect(21) 
            } else {
                if (debugLog) {log.debug ("setNextEffect(): Increment to next scene}")}
                int prevEffect = device.currentValue("effectNum").toInteger()-1
                setEffect(prevEffect)
        }
}

def lanActivateDIY (diyActivate) {
    diyScenes = loadDIYFile()
    if (descLog) log.info "${device.label} ActivateDIY: ${diyActivate}"
    if (debugLog) {log.debug ("activateDIY(): Activate effect number ${diyActivate} from ${diyScenes}")}
        String diyEffectNumber = diyActivate.toString()
        String sceneInfo = diyScenes.get(device.getDataValue("deviceModel")).get(diyEffectNumber).name
        String sceneCmd = diyScenes.get(device.getDataValue("deviceModel")).get(diyEffectNumber).cmd
        if (debugLog) {log.debug ("activateDIY(): Activate effect number ${diyActivate} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: diyEffectNumber)
        sendEvent(name: "switch", value: "on")
        sendEvent(name: "colorMode", value: "DIY_EFFECTS")
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("activateDIY(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
}

/////////////////////////////////////////////////////
// Helper methods to retrieve data or send command //
/////////////////////////////////////////////////////

def retrieveScenes() {
    state.remove("sceneOptions")  //Needs to be removed at a future date
    state.remove("diySceneOptions")  //Needs to be removed at a future date
    state.remove("diyScene") //Needs to be removed at a future date
    state.scenes = [] as List
    state.diyEffects = [] as List
    if (debugLog) {log.debug ("retrieveScenes(): Retrieving Scenes from parent device")}
/*    if (scenes.containsKey(device.getDataValue("DevType")) == false ) {   
        if (debugLog) {log.debug ("retrieveScenes(): Scenes does not contain entries for device")}
            scenes = scenes + parent."${"lightEffect_"+(device.getDataValue("DevType"))}"()
    } */
    lanScenes = loadSceneFile()
    if (lanScenes != null) {
    if (debugLog) {log.debug ("retrieveScenes(): Scenes Keyset ${lanScenes.get(device.getDataValue("DevType"))}")}
    if (debugLog) {log.debug ("retrieveScenes(): Scenes Keyset ${lanScenes.keySet()}")}
    if (lanScenes.keySet().contains(device.getDataValue("DevType"))) {
        tag = device.getDataValue("DevType")
    } else if (lanScenes.keySet().contains(device.getDataValue("deviceModel"))) {
        tag = device.getDataValue("deviceModel")
    }                                                                  
    lanScenes.get("${tag}").each {
        if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
        if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.scenes.add(sceneValue)
            state.scenes = state.scenes.sort()
        }
    }

    if (parent.label == "Govee v2 Device Manager") {   
        diyScenes = loadDIYFile()
        if (debugLog) {log.debug ("retrieveScenes(): Retrieving DIYScenes from integration app ${diyScenes}")}
        if (diyScenes == null) {
            if (debugLog) {log.debug ("retrieveScenes(): Device has no DIY Scenes")}
        } else {        
//        if (debugLog) {log.debug ("retrieveScenes(): DIY Keyset ${diyScenes.keySet()}")}
      diyScenes.get(device.getDataValue("deviceModel")).each {
//        diyScenes.each {    
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.diyEffects.add(sceneValue)
            state.diyEffects = state.diyEffects.sort()
            }
        }
    } else {
        diyScenes = loadDIYFile()
        if (diyScenes.containsKey((device.getDataValue("deviceModel"))) == false) {
            if (debugLog) {log.debug ("retrieveScenes(): No DIY Scenes to retrieve for device")}    
        } else {
//            diyScenes = parent.state.diyEffects.(device.getDataValue("deviceModel"))
//            diyScenes.put((device.getDataValue("deviceModel")),diyReturn)
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            diyScenes.get(device.getDataValue("deviceModel")).each {
//            diyScenes.each {
                if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
                if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
                String sceneValue = it.getKey() + "=" + it.value.name
                state.diyEffects.add(sceneValue)
                state.diyEffects = state.diyEffects.sort()
            }
        }        
    }        
}

def getDevType() {
//    String state.DevType = null= 
    switch(device.getDataValue("deviceModel")) {
        case "H6117":
        case "H6163":
        case "H6168":
        case "H6172":
        case "H6173":
        case "H6175":
        case "H6176":
        case "H617A":
        case "H617C":
        case "H617E":
        case "H617F":        
        case "H618A":
        case "H618B":
        case "H618C":
        case "H618E":
        case "H618F":
        case "H619A":
        case "H619B": 
        case "H619C": 
        case "H619D":
        case "H619E":
        case "H619Z":
        case "H61A0":
        case "H61A1":
        case "H61A2":
        case "H61A3":
        case "H61A5":
        case "H61A8":
        case "H61A9":
        case "H61B2":
        case "H61C2": 
        case "H61C3":
        case "H61C5":
        case "H61E1":
        case "H61E0":
        case "H6167":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to RGBIC_STRIP")}; 
            device.updateDataValue("DevType", "RGBIC_Strip");
            break; 
        case "H6066": 
        case "H606A":
        case "H6061":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to Hexa_Light")};
            device.updateDataValue("DevType", "Hexa_Light");            
            break;
        case "H6067": //Not added yet
            device.updateDataValue("DevType", "Tri_Light"); 
            break;
        case "H6065":
            device.updateDataValue("DevType", "Y_Light");
            break;        
        case "H6072": 
            device.updateDataValue("DevType", "Lyra_Lamp");
            break;
        case "H607C":
        case "H6079":
            device.updateDataValue("DevType", "Lyra_Pro");
            break;        
        case "H6076":
            device.updateDataValue("DevType", "Basic_Lamp");
            break;
        case "H6078":
            device.updateDataValue("DevType", "Cylinder_Lamp");
            break;        
        case "H6052": 
        case "H6051":
            device.updateDataValue("DevType", "Table_Lamp");
            break;
        case "H6038": 
        case "H6039":
            device.updateDataValue("DevType", "Wall_Sconce");
            break;
        case "H6022":
            device.updateDataValue("DevType", "Table_Lamp_2");
            break;        
        case "H70C1":
        case "H70C2":
        case "H70C4":
        case "H70C5":
        case "H70C7":
        case "H70C9":
        case "H70CB":
            device.updateDataValue("DevType", "XMAS_Light");
            break;
        case "H610A": 
        case "H610B": 
        case "H6062":
            device.updateDataValue("DevType", "Wall_Light_Bar");
            break;
        case "H6046":
        case "H6056":
        case "H6047":
            device.updateDataValue("DevType", "TV_Light_Bar"); 
            break;        
        case "H6088":
        case "H6087":
        case "H608A": 
        case "H608B":
        case "H608C":
            device.updateDataValue("DevType", "Indoor_Pod_Lights");
            break;        
        case "H705A":
        case "H705B":
        case "H705C":
        case "H706A":
        case "H706B":
        case "H706C":
            device.updateDataValue("DevType", "Outdoor_Perm_Light");
            break;
        case "H7050":
        case "H7051":
        case "H7052":
        case "H7055":
            device.updateDataValue("DevType", "Outdoor_Pod_Light");
            break;
        case "H7060":
        case "H7061":
        case "H7062":
        case "H7065":
        case "H7066":
            device.updateDataValue("DevType", "Outdoor_Flood_Light");
            break;
        case "H70B1":
        case "H70BC":
            device.updateDataValue("DevType", "Curtain_Light");
            break;
        case "H70B3":
        case "H70B4":
        case "H70B5":
            device.updateDataValue("DevType", "Curtain_Light2");
            break;
        case "H7075":
            device.updateDataValue("DevType", "Outdoor_Wall_Light");
            break; 
        case "H6811":
            device.updateDataValue("DevType", "Net_Lights");
            break;
        case "H6091":
        case "H6092":
            device.updateDataValue("DevType", "Galaxy_Projector");
            break;
        case "H7020":
        case "H7021":
        case "H7028":
        case "H7041":
        case "H7042":
            device.updateDataValue("DevType", "Outdoor_String_Light");
            break;
        default: 
            if (debugLog) {log.debug ("getDevType(): Unknown device Type  ${device.getDataValue("deviceModel")}")};
            device.updateDataValue("DevType", "Generic");    
        break; 
        
    }       
}

def GoveeCommandBuilder(String command1, value1, String type) {   
    if (type=="ct") {
        if (debugLog) {log.debug "GoveeCommandBuilder(): Color temp action"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r 0
            g 0
            b 0
        }
            colorTemInKelvin value1}
    }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
   else if (type=="rgb") {
       if (debugLog) {log.debug "GoveeCommandBuilder(): rgb"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r value1.r
            g value1.g
            b value1.b
                
        }
            colorTemInKelvin 0}
    }
    def  command = cmd1.toString()
       if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
       else if (type=="status") {
           if (debugLog) {log.debug "GoveeCommandBuilder():status"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            }
    }
    def  command = cmd1.toString()
           if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
    else { 
        if (debugLog) {log.debug "GoveeCommandBuilder():other action"}
    JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            value value1}
        }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command
}
}

def sendCommandLan(String cmd) {
  def addr = getIPString();
    if (debugLog) {log.debug ("sendCommandLan(): ${cmd}")}

  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     ignoreResponse    : false,
                     callback: parse,
                     parseWarning: true,
                     destinationAddress: addr])  
  try {    
      if (debugLog) {log.debug("sendCommandLan(): ${pkt} to ip ${addr}")}
    sendHubCommand(pkt) 
      
  }
  catch (Exception e) {      
      logDebug e
  }      
}

def getIPString() {
    return device.getDataValue("IP")+":"+commandPort()
}


def parse(message) {  
    log.error "Got something to parseUDP"
    valueMap = [:]
    def message2 = message.replaceAll(/\s+/, "")  
    message2.split(",").each{ item ->
        valueMap.put(item.substring(0,(item.indexOf(':'))),item.substring((item.indexOf(':')+1),item.length()))
        } 
    def utf8String = new String(valueMap.payload.decodeBase64(), "UTF-8")
    log.error "UDP Response -> Error Type: ${valueMap.type} Error Message: ${utf8String}"    
}

def loadSceneFile() {
    
    String name = lanScenesFile
    
    if (name == null) {
        if (debugLog) {log.debug "loadSceneFile: File name is null using default values"}
        name = "GoveeLanScenes_"+getDataValue("DevType")+".json"    
    } 
    try {
        byte[] dBytes = downloadHubFile(name)
        if (debugLog) {log.debug "File loaded starting parse."}
        tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
        scenes = tmpEffects.get(0)
        return scenes 
    }
    catch (Exception e) {      
        if (debugLog) {log.debug "loadSceneFile: ${e}"}
    }
/*    if (debugLog) {log.debug "File loaded starting parse."}
        tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
        scenes = tmpEffects.get(0)
        return scenes */
}

def loadDIYFile() {
    byte[] dBytes
    try {
        dBytes = downloadHubFile("GoveeLanDIYScenes.json")
    }
    catch (Exception e) {
        if (debugLog) {log.debug "loadDIYFile: ${e}"}
    }
    if (dBytes != null) {
        tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
        if (debugLog) {log.debug "loadDIYFile: Loaded ${tmpEffects.get(0)} from ${goveeDIYScenesFile}"}
        diyEffects = tmpEffects.get(0)
        return diyEffects
    }
}

void devStatus() {    
        sendCommandLan(GoveeCommandBuilder("devStatus", null , "status"))
        if (debugLog) log.info "${device.label} status was requested."  
}

def ipLookup() {
    if (debugLog) {log.info("ipLookup: Looking up Alt ip of ${getDataValue("IP")}")}
    ipAddress = getDataValue("IP")
    return ipAddress
}

void lanAPIPost(data) {
    if (debugLog) {log.info("lanAPIPost: Processing update from LAN API. Data: ${data}")}
    if (data.onOff == 1) { onOffSwitch = on}
    if (data.onOff == 0) { onOffSwitch = off}
    
        if (onOffSwitch == "on") {
            if (onOffSwitch != device.currentValue("switch")) {
                if (debugLog) {log.info("lanAPIPost: Switch Changed to on.")}
                sendEvent(name: "switch", value: onOffSwitch)
            }
            if (data.brightness != device.currentValue("level")) {
                sendEvent(name: "level", value: data.brightness)
            } else {
                if (debugLog) {log.info("lanAPIPost: Brightness has not changed. Ignoring")}
            }
            if (data.colorTemInKelvin != device.currentValue("colorTemperature")) {
                sendEvent(name: "colorTemperature", value: data.colorTemInKelvin)
            } else {
                if (debugLog) {log.info("lanAPIPost: Color Temperature has not changed. Ignoring")}
            }
            rgb = []
            rgb = [data.color.r,data.color.g, data.color.b]
            hsv = hubitat.helper.ColorUtils.rgbToHSV(rgb)
            if (debugLog) {log.info("lanAPIPost: Hue is ${hsv.get(0)}.")}
            if (hsv.get(0) != device.currentValue("hue")) {
                sendEvent(name: "hue", value: hsv.get(0))
            } else {
                if (debugLog) {log.info("lanAPIPost: Color Hue has not changed. Ignoring")}
            } 
            if (debugLog) {log.info("lanAPIPost: Saturation is  is ${hsv.get(1)}.")}
            if (hsv.get(1) != device.currentValue("saturation")) {
                sendEvent(name: "saturation", value: hsv.get(1))
            } else {
                if (debugLog) {log.info("lanAPIPost: Color Saturation has not changed. Ignoring")}
            } 
        } else {
            if (onOffSwitch != device.currentValue("switch")) {
                if (debugLog) {log.info("lanAPIPost: Switch Changed to off.")}
                sendEvent(name: "switch", value: onOffSwitch)
            }
        }
}

void updateIPAdd(ipAddress) {
    if (debugLog) {log.info("updateIPAdd: New Ip Address fund for Device, Updating with ${ipAddress}")}
    device.updateDataValue("IP", ipAddress);
}

void retrieveIPAdd() {
    if (debugLog) {log.info("retrieveIPAdd: Reaching out to Parent device for IP Address")}
    deviceID = device.getDataValue("deviceID")
    if (parent.retrieveApiDevices().keySet().contains(deviceID)) {
        ipAddress = parent.retrieveApiDevices()."${deviceID}".ip
    } else {
        ipAddress = "N/A"
    }
    if (debugLog) {log.info("retrieveIPAdd: LAN API Ip Address for device is ${ipAddress}")}
    device.updateDataValue("IP", ipAddress)
}

void lanInitDefaultValues() {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
        sendEvent(name: "effectNum", value: 0) 
    	sendEvent(name: "colorMode", value: "CT") 
        sendEvent(name: "level", value: 0)  
        sendEvent(name: "colorTemperature", value: 2000)  
}
