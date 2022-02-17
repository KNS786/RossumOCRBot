package com.beezlabs.hiveonserver.bots;

import com.beezlabs.hiveonserver.libs.JavaBotTemplate;
import com.beezlabs.tulip.libs.models.BotExecutionModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.util.*;

public class RossumTaskRead extends JavaBotTemplate{
    @Override
    protected void botLogic(BotExecutionModel model){
        try{
            List<Map<String,Object>> taskItems=(List<Map<String,Object>>) model.getProposedBotInputs().get("Rossum_UserTask_output").getValue();
            Map<String,Object> taskHeader=(Map<String,Object>) model.getProposedBotInputs().get("Rossum_UserTaskHeader").getValue();
            String json=new ObjectMapper().writeValueAsString(taskHeader);
            DocumentContext Ctx=JsonPath.parse(json);
            JsonPath jsonPath=JsonPath.compile("$..dimensionList[?(@.caption=='Purchase Order Number')]");
            List<Map<String,Object>> poNumber = Ctx.read(jsonPath);
            List<Map<String,Object>> items=new ArrayList<>();
            taskItems.forEach(i->{
                if((Boolean) i.get("approved")){
                    Map<String,Object> item=new HashMap<String,Object>();
                    item.put("pono",poNumber.get(0).get("value").toString());
                    item.put("poitemno",i.get("name"));
                    item.put("quantity",i.get("metric1"));
                    item.put("uom",i.get("metric2"));
                    item.put("material",i.get("description"));
                    items.add(item);
                }
            });
            addVariable("items",items);
            if(items.size() > 0){
                addVariable("has_approved",true);
            }else{
                addVariable("has_approved",false);
            }
            success("Bot Execution Successful");

        }catch(Exception e){
            failure("Bot Execution Failed "+ e.getMessage());
        }
    }

}