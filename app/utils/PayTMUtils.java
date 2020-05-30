package utils;


import com.paytm.pg.merchant.CheckSumServiceHelper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;



public  class PayTMUtils {

    public final static String MID="DIY12386817555501617";
    public final static String MERCHANT_KEY="bKMfNxPPf_QdZppa";
    public final static String INDUSTRY_TYPE_ID="Retail";
    public final static String CHANNEL_ID="WEB";
    public final static String WEBSITE="DIYtestingweb";
    public final static String PAYTM_URL="https://pguat.paytm.com/oltp-web/processTransaction";




    public static TreeMap<String, String> generatePayLoad(PaytmRequestModel paytmRequestModel) {

        System.out.println(paytmRequestModel.getCAMPAIGN_ID());
        System.out.println(paytmRequestModel.getCUST_ID());
        System.out.println(paytmRequestModel.getTXN_AMOUNT());
        System.out.println(paytmRequestModel.getCALLBACK_URL());
        TreeMap<String, String> paramMap = new TreeMap<String, String>();




        paramMap.put("CALLBACK_URL", paytmRequestModel.getCALLBACK_URL());
        paramMap.put("CHANNEL_ID", CHANNEL_ID);
        if (paytmRequestModel.getCUST_ID()== null) {
            UUID custId = UUID.randomUUID();
            paramMap.put("CUST_ID",custId.toString());//TODO this may be some other valye
            paytmRequestModel.setCUST_ID(custId.toString());
        }
        if(paytmRequestModel.getEMAIL() != null)
            paramMap.put("EMAIL", paytmRequestModel.getEMAIL());
        paramMap.put("INDUSTRY_TYPE_ID", INDUSTRY_TYPE_ID);
        paramMap.put("MID", MID);
        if (paytmRequestModel.getMOBILE_NO()!= null)
            paramMap.put("MOBILE_NO",paytmRequestModel.getMOBILE_NO());

        UUID orderId = UUID.randomUUID();
        paytmRequestModel.setORDER_ID(orderId);
        paramMap.put("ORDER_ID",orderId.toString() );
//        paramMap.put("ORDER_ID",UUID.randomUUID().toString() );

        paramMap.put("TXN_AMOUNT", paytmRequestModel.getTXN_AMOUNT().toPlainString());
        paramMap.put("WEBSITE", WEBSITE);



        try {
            String checkSum = CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(MERCHANT_KEY, paramMap);
            paramMap.put("CHECKSUMHASH", checkSum);

            System.out.println("Paytm Payload: " + paramMap);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//
//        ORDER_ID=vidisha123&
//        		CUST_ID=CUST001&
//        		INDUSTRY_TYPE_ID=Retail&
//        		CHANNEL_ID=WEB&
//        		TXN_AMOUNT=1&
//        		MID=DIY12386817555501617&WEBSITE=DIYtestingweb&PAYTM_MERCHANT_KEY=bKMfNxPPf_QdZppa

        return paramMap;
    }
    public static long givenUsingPlainJava_whenGeneratingRandomIntegerBounded_thenCorrect() {
        int leftLimit = 1;
        int rightLimit = 10;
        return  leftLimit + (int) (new Random().nextFloat() * (rightLimit - leftLimit));
    }

    public static PaymentRequest paymentRequestBuilder(PaytmRequestModel paytmRequestModel)
    {
        PaymentRequest pr = new PaymentRequest();
        pr.setChecksumHash(paytmRequestModel.getCHECKSUMHASH());
        pr.setCustomerId(paytmRequestModel.getCUST_ID());
        pr.setEmailId(paytmRequestModel.getEMAIL());
        pr.setMobileNo(paytmRequestModel.getMOBILE_NO());
        pr.setOrderId(paytmRequestModel.getORDER_ID());
        pr.setTxnAmount(paytmRequestModel.getTXN_AMOUNT());
        pr.setEmailId(paytmRequestModel.getEMAIL());
//    	pr.setChannelId(PaytmRequestModel.getChannelId());//Static Reference
        pr.setChannelId(CHANNEL_ID);//Static Reference
        pr.setCallbackUrl(paytmRequestModel.getCALLBACK_URL());
//    	pr.setIndustryTypeId(PaytmRequestModel.getIndustryTypeId());//Static Reference
        pr.setIndustryTypeId(INDUSTRY_TYPE_ID);
//    	pr.setWebsite(PaytmRequestModel.getWebsite());//Static Reference
        pr.setWebsite(WEBSITE);
//    	pr.setMid(PaytmRequestModel.getMid());//Static Reference
        pr.setMid(MID);
        return pr;
    }


    public static PaymentResponse paymentResponseBuilder(PaytmResponseModel paytmResponseModel)
    {

        PaymentResponse pr = new PaymentResponse();
        pr.setTransactionId(paytmResponseModel.getTXNID());
        pr.setOrderId(paytmResponseModel.getORDERID());
        pr.setTxnAmount(new BigDecimal(paytmResponseModel.getTXNAMOUNT()));
        pr.setStatus(paytmResponseModel.getSTATUS());
        pr.setTransactionType(paytmResponseModel.getTXNTYPE());
        pr.setGateWayName(paytmResponseModel.getGATEWAYNAME());
        pr.setResponseCode(paytmResponseModel.getRESPCODE());
        pr.setResponseMessage(paytmResponseModel.getRESPMSG());
        pr.setBankName(paytmResponseModel.getBANKNAME());
        pr.setPaymentMode(paytmResponseModel.getPAYMENTMODE());
        pr.setTransactionDate(paytmResponseModel.getTXNDATE());
        pr.setBankTransactionId(paytmResponseModel.getBANKTXNID());
        pr.setChecksumHash(paytmResponseModel.getCHECKSUMHASH());
        return pr;
    }


    public static boolean isValidChecksum(PaytmResponseModel response, PaymentRequest paymentRequest){

        System.out.println("In Valid Checksum");
        System.out.println("In Valid Checksum PaytmResponseModel"+response);
        System.out.println("In Valid Checksum PaymentRequest"+paymentRequest);

        TreeMap<String, String> paramMap = new TreeMap<String, String>();
        paramMap.put("CALLBACK_URL", paymentRequest.getCallbackUrl());
        paramMap.put("CHANNEL_ID", paymentRequest.getChannelId());

        if (paymentRequest.getCustomerId()!= null) {
            paramMap.put("CUST_ID",paymentRequest.getCustomerId());//TODO this may be some other valye
        }
        if(paymentRequest.getEmailId() != null)
            paramMap.put("EMAIL", paymentRequest.getEmailId());
        paramMap.put("INDUSTRY_TYPE_ID", paymentRequest.getIndustryTypeId());
        paramMap.put("MID", paymentRequest.getMid());
        if (paymentRequest.getMobileNo()!= null)
            paramMap.put("MOBILE_NO",paymentRequest.getMobileNo());

        paramMap.put("ORDER_ID",paymentRequest.getOrderId().toString());
//    paramMap.put("ORDER_ID",UUID.randomUUID().toString() );



        if(paymentRequest.getTxnAmount().equals(new BigDecimal(response.getTXNAMOUNT())))
        {
            paramMap.put("TXN_AMOUNT", paymentRequest.getTxnAmount().stripTrailingZeros().toPlainString());//Take amount from response
        }
        paramMap.put("WEBSITE", WEBSITE);


        String paytmChecksum = paymentRequest.getChecksumHash();
        System.out.println("paytmChecksum"+paytmChecksum);
        System.out.println("paramMap"+paramMap.toString());
        System.out.println("MERCHANT_KEY"+MERCHANT_KEY);
        boolean isValideChecksum = false;
        try{
            isValideChecksum = CheckSumServiceHelper.getCheckSumServiceHelper().verifycheckSum(MERCHANT_KEY, paramMap, paytmChecksum);
            System.out.println(isValideChecksum);
            // if checksum is validated Kindly verify the amount and status
            // if transaction is successful
            // kindly call Paytm Transaction Status API and verify the transaction amount and status.
            // If everything is fine then mark that transaction as successful into your DB.

        }catch(Exception e){
            e.printStackTrace();
        }
        return isValideChecksum;
    }


}
