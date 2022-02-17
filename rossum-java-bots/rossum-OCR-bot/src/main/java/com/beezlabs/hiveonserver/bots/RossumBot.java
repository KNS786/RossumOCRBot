package com.beezlabs.hiveonserver.bots;


import com.beezlabs.hiveonserver.libs.JavaBotTemplate;
import com.beezlabs.hiveonserver.usertask.models.TaskAttribute;
import com.beezlabs.tulip.libs.models.BotExecutionModel;
import com.beezlabs.tulip.libs.models.BotIdentity;
import com.beezlabs.tulip.libs.models.UserTaskModel;
import com.beezlabs.hiveonserver.usertask.libs.services.UserTaskService;
import com.google.gson.Gson;
import com.jayway.jsonpath.*;
import jdk.jshell.Snippet;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import java.util.Map;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class RossumBot extends JavaBotTemplate {
    DefaultHttpClient httpclient = new DefaultHttpClient();
    String downloadUri,fileName;

    @Override
    protected  void botLogic(BotExecutionModel botExecutionModel) {
        try{
            ArrayList<Map<String, Object>> dmsLink = (ArrayList<Map<String, Object>>) botExecutionModel.getProposedBotInputs().get("DMS_Link").getValue();
            Map<String,String> dmsReadCred=getIdentityBasicAuth("tulipDmsCred",botExecutionModel.getIdentityList());
            String UserName=dmsReadCred.get("username");
            String Password=dmsReadCred.get("password");
            String GetUrl=botExecutionModel.getProposedBotInputs().get("getLoginUrl").getValue().toString();
            String Key=Login(GetUrl,UserName,Password);
            for(Map<String,Object> DocumentLink : dmsLink){
                downloadUri=(String)DocumentLink.get("fileDownloadUri");
                fileName=(String)DocumentLink.get("fileSignature");
            }
            String GetUploadUrl=botExecutionModel.getProposedBotInputs().get("getUploadUrl").getValue().toString();
            addVariable("RossumFileUploadUri",GetUploadUrl);
            addVariable("filename",fileName);
            addVariable("fileDownloadUri",downloadUri);
            UploadFile(Key,GetUploadUrl,downloadUri);
            String CheckUploadStatusUrl=botExecutionModel.getProposedBotInputs().get("getUploadDocStatusUrl").getValue().toString();
            OCRDocStatus(Key,CheckUploadStatusUrl);
            String GetOcrStatusUrl=botExecutionModel.getProposedBotInputs().get("getOCRStatusUrl").getValue().toString();
            GetOCR(Key,GetOcrStatusUrl);
            httpclient.getConnectionManager().shutdown();
            success("Bot Executed Successfully");
        }catch(Exception  e){
            httpclient.getConnectionManager().shutdown();
            e.printStackTrace();
            failure("Bot Failed");
        }


    }
    private  Map<String, String> getIdentityBasicAuth(String username, List<BotIdentity> botIdentityList) {
        Map<String, String> value = new HashMap<>();
        for (BotIdentity botIdentity : botIdentityList) {
            if (botIdentity.getIdentityType().name().equals("BASIC_AUTH")) {
                if (botIdentity.getName().equals(username)) {
                    value.put("username", botIdentity.getCredential().getBasicAuth().getUsername());
                    value.put("password", botIdentity.getCredential().getBasicAuth().getPassword());
                }
            }
        }
        return value;
    }

    public  String Login(String url, String username, String password){


        try {

            HttpPost postRequest = new HttpPost(url);

            StringEntity params = new StringEntity(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password));
            postRequest.addHeader("content-type", "application/json");
            postRequest.setEntity(params);

            HttpResponse httpResponse = httpclient.execute(postRequest);
            HttpEntity entity = httpResponse.getEntity();

            if (entity != null) {

                JSONObject obj = new JSONObject(EntityUtils.toString(entity));

                String key = obj.getString("key");
                return  key;
            }else{
                throw new Exception("Getting Login Key Error");
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;

    }
    public  void UploadFile(String key, String url, String filePath){

        try{

            HttpPost postRequest = new HttpPost(url);

            File file = new File(filePath);

            FileEntity params = new FileEntity(file);
            postRequest.setHeader(HttpHeaders.AUTHORIZATION, "token " + key);
            postRequest.setHeader("Content-Disposition", String.format("attachment; filename=%s", file.getName()));
            postRequest.setEntity(params);

            HttpResponse httpResponse = httpclient.execute(postRequest);
            if(400 <= httpResponse.getStatusLine().getStatusCode() && httpResponse.getStatusLine().getStatusCode() >=599){
                throw new Exception("Uploading File Failed");
            }
            HttpEntity entity = httpResponse.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            System.out.println(responseString);

        }catch(Exception e){
            e.printStackTrace();

        }
    }
    public void GetOCR(String key,String url){

        try{
            HttpGet GetRequest=new HttpGet(url);
            GetRequest.setHeader(HttpHeaders.AUTHORIZATION,"token "+ key);

            HttpResponse httpResponse=httpclient.execute(GetRequest);
            if(400 <= httpResponse.getStatusLine().getStatusCode() && httpResponse.getStatusLine().getStatusCode() >=500){
                throw new Exception("Getting OCR Details is Error");
            }
            HttpEntity entity = httpResponse.getEntity();
            String responseString = EntityUtils.toString(entity,"UTF-8");

            String GettingLength="$.['content'][*].children[*]";
            ReadContext CtxGet=JsonPath.parse(responseString);
            List<Object> Content=CtxGet.read(GettingLength);
            System.out.print(Content.size());
            List<Object> Results = CtxGet.read("$.['results'][*].children[*]");
            Map<String, ArrayList<String>> OCRDetails = new HashMap<>();

            ReadContext ctx = JsonPath.parse(responseString);
            for (int i = 0; i < Content.size(); i++) {
                //Index bound range for getting Content and results JSON ARRAY
                try {
                    String JsonExpression = "$.['content'].[" + i + "].['children'].[*].['schema_id']";
                    List<String> resJson = ctx.read(JsonExpression);
                    int length = resJson.size();
                    String JsonExpKey, JsonExpValue, Key, Value;
                    for (int j = 0; j < length; j++) {
                        JsonExpKey = "$.['content'][" + i + "].children[" + j + "].schema_id";
                        JsonExpValue = "$.['content'][" + i + "].children[" + j + "].content.value";
                        Key = ctx.read(JsonExpKey);
                        Value = ctx.read(JsonExpValue);
                        if (Key != null && Value != null) {
                            if (!OCRDetails.containsKey(Key)) {
                                ArrayList<String> Res = new ArrayList<String>();
                                Res.add((String) Value);
                                OCRDetails.put(Key, Res);
                            } else {
                                OCRDetails.get(Key).add((String) Value);
                            }
                        }
                    }

                } catch (Exception e) {
                }
            }

            String JsonExpKey, JsonExpValue;
            List<String> ItemsValues=ctx.read("$.['results'][*].children[0].children[*].children[1].schema_id");

            JsonExpKey = "$.['results'][*].children[0].children[*].children[1].schema_id";
            JsonExpValue = "$.['results'][*].children[0].children[*].children[1].content.value";
            List<String> GetKey=ctx.read(JsonExpKey);
            List<String> GetValue=ctx.read(JsonExpValue);


            for(int DescriptionInd=0;DescriptionInd<GetValue.size();DescriptionInd++){
                if(!OCRDetails.containsKey(GetKey.get(DescriptionInd))){
                    ArrayList<String> res= new ArrayList<String>();
                    res.add(GetValue.get(DescriptionInd));
                    OCRDetails.put(GetKey.get(DescriptionInd),res);
                }
                else{
                    OCRDetails.get(GetKey.get(DescriptionInd)).add(GetValue.get(DescriptionInd));
                }
            }

            GetKey.clear();
            GetValue.clear();
            JsonExpKey="$.['results'][*].children[0].children[*].children[2].schema_id";
            JsonExpValue="$.['results'][*].children[0].children[*].children[2].content.value";
            GetKey=ctx.read(JsonExpKey);
            GetValue=ctx.read(JsonExpValue);
            for(int itmQtyIndex=0;itmQtyIndex<GetValue.size();itmQtyIndex++){
                if(!OCRDetails.containsKey(GetKey.get(itmQtyIndex))){
                    ArrayList<String> res= new ArrayList<String>();
                    res.add(GetValue.get(itmQtyIndex));
                    OCRDetails.put(GetKey.get(itmQtyIndex),res);
                }
                else{
                    OCRDetails.get(GetKey.get(itmQtyIndex)).add(GetValue.get(itmQtyIndex));
                }
            }

            GetKey.clear();
            GetValue.clear();
            JsonExpKey="$.['results'][*].children[0].children[*].children[7].schema_id";
            JsonExpValue="$.['results'][*].children[0].children[*].children[7].content.value";
            GetKey=ctx.read(JsonExpKey);
            GetValue=ctx.read(JsonExpValue);
            for(int itmAmtInd=0;itmAmtInd<GetValue.size();itmAmtInd++){
                if(!OCRDetails.containsKey(GetKey.get(itmAmtInd))){
                    ArrayList<String> res= new ArrayList<String>();
                    res.add(GetValue.get(itmAmtInd));
                    OCRDetails.put(GetKey.get(itmAmtInd),res);
                }
                else{
                    OCRDetails.get(GetKey.get(itmAmtInd)).add(GetValue.get(itmAmtInd));
                }
            }
            GetKey.clear();
            GetValue.clear();
            JsonExpKey="$.['results'][*].children[0].children[*].children[9].schema_id";
            JsonExpValue="$.['results'][*].children[0].children[*].children[9].content.value";
            GetKey=ctx.read(JsonExpKey);
            GetValue=ctx.read(JsonExpValue);

            for(int itmTotalAmtInd=0;itmTotalAmtInd<GetValue.size();itmTotalAmtInd++){
                if(!OCRDetails.containsKey(GetKey.get(itmTotalAmtInd))){
                    ArrayList<String> res= new ArrayList<String>();
                    res.add(GetValue.get(itmTotalAmtInd));
                    OCRDetails.put(GetKey.get(itmTotalAmtInd),res);
                }
                else{
                    OCRDetails.get(GetKey.get(itmTotalAmtInd)).add(GetValue.get(itmTotalAmtInd));
                }
            }

            UserTaskService taskService = new UserTaskService();
            if(OCRDetails.get("sender_name")!=null)
                taskService.fillHeaderDimensionList("Vendor Name", OCRDetails.get("sender_name").get(0));
            if(OCRDetails.get("order_id")!=null)
                taskService.fillHeaderDimensionList("PurchaseOrder", OCRDetails.get("order_id").get(0));
            if(OCRDetails.get("date_issue")!=null)
                taskService.fillHeaderDimensionList("Invoice Date",OCRDetails.get("date_issue").get(0));
            if(OCRDetails.get("amount_total_base")!=null)
                taskService.fillHeaderDimensionList("Invoice Total",OCRDetails.get("amount_total_base").get(0));
            if(OCRDetails.get("recipient_delivery_address")!=null)
                taskService.fillHeaderDimensionList("Recipient Delivery Address",OCRDetails.get("recipient_delivery_address").get(0));
            if(OCRDetails.get("document_id")!=null){
                taskService.fillHeaderDimensionList("Invoice Number",OCRDetails.get("document_id").get(0));
                addVariable("invoiceNumber",OCRDetails.get("document_id").get(0));
            }
            if(OCRDetails.get("amount_total_base")!=null)
                taskService.fillHeaderMetricList("Total Amount",Double.parseDouble(RemoveSpacesForAmt(OCRDetails.get("amount_total_base").get(0))),"ST");

            int ItemsSize=OCRDetails.get("item_description").size();

            for(int i=0;i<ItemsSize;i++) {
                TaskAttribute attribute = new TaskAttribute(true, "Amount", OCRDetails.get("item_amount_total").get(i).toString());
                List<TaskAttribute> attributeList = new ArrayList<>();
                attributeList.add(attribute);
                taskService.addTaskItem("Item No "+(i+1), OCRDetails.get("item_description").get(i), "Material Description",
                        "Quantity", OCRDetails.get("item_quantity").get(i), "Unit", OCRDetails.get("item_amount").get(i)+"ST", attributeList);
            }

            taskService.createUserTask("Rossum_UserTask");
            taskService.createHeaderUserTask("Rossum_UserTaskHeader");
            taskService.createAttachmentUserTask("Rossum_UserTaskAttachments", "Documents");
            addVariable("Rossum", taskService.TaskList);
            addVariable("Rossum_UserTaskHeader", taskService.headerTaskList);
            addVariable("Rossum_UserTaskAttachments", taskService.attachmentTaskList);


        }catch(Exception e){
            e.printStackTrace();
        }

    }

    //Check Whether the document is OCR information marked or not
    public void OCRDocStatus(String key,String  url){
        String StatusConfirmed="to_review";
        String StatusDetector="importing";
        boolean isToReview=true;


        while(isToReview && !StatusConfirmed.equals(StatusDetector)) {

            try {
                HttpGet GetRequest = new HttpGet(url);
                GetRequest.setHeader(HttpHeaders.AUTHORIZATION, "token " + key);

                HttpResponse httpResponse = httpclient.execute(GetRequest);
                if(400 <= httpResponse.getStatusLine().getStatusCode() && httpResponse.getStatusLine().getStatusCode() >=599){
                    throw new Exception("Getting Document Status is Error");
                }
                HttpEntity entity = httpResponse.getEntity();
                String responseString = EntityUtils.toString(entity, "UTF-8");
                String JsonExpressions = "$.['status']";
                ReadContext ctx = JsonPath.parse(responseString);
                String[] CtxList=new String[]{ctx.read(JsonExpressions)};
                StatusDetector= CtxList[0];

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }



    public  void GetDocumentContent(String key,String url){
        try{
            HttpGet GetRequest=new HttpGet(url);
            GetRequest.setHeader(HttpHeaders.AUTHORIZATION,"token "+ key);

            HttpResponse httpResponse=httpclient.execute(GetRequest);
            HttpEntity entity = httpResponse.getEntity();
            String responseString = EntityUtils.toString(entity,"UTF-8");
            System.out.println(responseString.toString());

        }catch(Exception e){
            e.printStackTrace();
        }
        finally{
            httpclient.getConnectionManager().shutdown();
        }

    }

    public String RemoveSpacesForAmt(String WhiteSpaceString){
        String Res="";
        for(int i=0;i<WhiteSpaceString.length();i++){
            if(WhiteSpaceString.charAt(i)==' '){
                continue;
            }else{
                Res+=WhiteSpaceString.charAt(i);
            }
        }
        return Res;

    }

}
