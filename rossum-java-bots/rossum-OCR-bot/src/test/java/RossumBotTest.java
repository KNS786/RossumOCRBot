import com.beezlabs.hiveonserver.bots.RossumBot;
import com.beezlabs.hiveonserver.libs.JavaBotTemplate;
import com.beezlabs.hiveonserver.libs.VariableMapInterface;
import com.beezlabs.tulip.libs.models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RossumBotTest{
    @Test
    public void Test(){
        BotExecutionModel botExecutionModel=new BotExecutionModel();
        Map<String,String> dmsLink = new HashMap<>();
        ArrayList<Map<String,String>> dms = new ArrayList();
        dmsLink.put("fileDownloadUri","https://tulip.beezlabs.com/api/core/dms/read/a2950580-0c74-34ee-88b5-b071ead6a9b3/tmpTest.pdf");
        dmsLink.put("fileSignature","a2950580-0c74-34ee-88b5-b071ead6a9b3/tmpTest.pdf");
        dms.add(dmsLink);
        Map<String,Variable> proposedBotInputs=new HashMap<>();
        proposedBotInputs.put("DMS_Link", new Variable(dms, VariableType.ARRAY, Object.class));
        proposedBotInputs.put("getLoginUrl",new Variable("https://api.elis.rossum.ai/v1/auth/login",VariableType.STRING,Object.class));
        proposedBotInputs.put("getUploadUrl",new Variable("https://api.elis.rossum.ai/v1/queues/171180/upload",VariableType.STRING,Object.class));
        proposedBotInputs.put("getOCRStatusUrl",new Variable("https://api.elis.rossum.ai/v1/annotations/12168953/content",VariableType.STRING,Object.class));
        proposedBotInputs.put("getUploadDocStatusUrl",new Variable("https://api.elis.rossum.ai/v1/annotations/12168953",VariableType.STRING,Object.class));
        BasicAuthModel genericCred=new BasicAuthModel();
        genericCred.setUsername("navani@beezlabs.com");
        genericCred.setPassword("navani.007");
        Credential credentialTulip=new Credential();
        credentialTulip.setBasicAuth(genericCred);
        BotIdentity tulipGenericCred=new BotIdentity("tulipDmsCred",credentialTulip,IdentityType.BASIC_AUTH);
        List<BotIdentity> identityList=new ArrayList<BotIdentity>();
        identityList.add(tulipGenericCred);
        botExecutionModel.setProposedBotInputs(proposedBotInputs);
        botExecutionModel.setIdentityList(identityList);
        try{
            JavaBotTemplate OcrTest=new RossumBot();
            OcrTest.replyCallback(this::botReplyHandler);
            OcrTest.runBot(botExecutionModel);

        }catch(Exception e){
           Assertions.fail(e.getMessage());
        }
    }

    private void botReplyHandler(BotReplyModel botReplyModel) {
        Assertions.assertEquals("Bot Executed Successfully",botReplyModel.getBotMessage());
    }

}