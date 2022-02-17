package java.com.beezlabs.hiveonserver.bots;

import com.beezlabs.hiveonserver.bots.RossumTaskRead;
import com.beezlabs.hiveonserver.libs.JavaBotTemplate;
import com.beezlabs.tulip.libs.models.BotExecutionModel;
import com.beezlabs.tulip.libs.models.BotReplyModel;
import com.beezlabs.tulip.libs.models.Variable;
import com.beezlabs.tulip.libs.models.VariableType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.*;


public class RossumTaskReadTest {
    @Test
    public void TestClass(){
        BotExecutionModel botExecutionModel=new BotExecutionModel();
        List<Map<String,Object>> taskList=new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> dimensionList = new ArrayList<Map<String,Object>>();
        Map<String,Object>dimension = new HashMap<String,Object>();
        Map<String, Object> taskHeader = new HashMap<String, Object>();
        Map<String,Object> defaultValue = new HashMap<String,Object>();
        Map<String,Object> task=new HashMap<String,Object>();
        task.put("name", "Items");
        task.put("metric1", "1");
        task.put("metric2", "ST");
        task.put("approved",true);
        task.put("description", "10");
        task.put("taskVariables", null);
        task.put("metricCaption1", "Quantity");
        task.put("metricCaption2", "UoM");
        task.put("longDescription", "Purchase Order Item No");
        taskList.add(task);
        dimension.put("value", "140539");
        dimension.put("caption", "Purchase Order Number");
        dimensionList.add(dimension);
        taskHeader.put("key","Rossum_UsertaskHeader");
        taskHeader.put("type","approvalHeader");
        defaultValue.put("dimensionList",dimensionList);
        taskHeader.put("defaultValue",defaultValue);
        Map<String, Variable> proposedBotInputs=new HashMap<String,Variable>();
        proposedBotInputs.put("Rossum_UserTask_output",new Variable(taskList, VariableType.OBJECT, Object.class));
        proposedBotInputs.put("Rossum_UserTaskHeader",new Variable(taskHeader, VariableType.OBJECT, Object.class));
        botExecutionModel.setProposedBotInputs(proposedBotInputs);
        try{
            JavaBotTemplate RossumTaskRead =new RossumTaskRead();
            RossumTaskRead.replyCallback(this::botReplyHandler);

        }catch(Exception e){
            Assertions.fail(e.getMessage());
        }
    }
    public void botReplyHandler(BotReplyModel botReplyModel){
        Assertions.assertEquals("Bot Execution Successful" , botReplyModel.getBotMessage());
    }

}
