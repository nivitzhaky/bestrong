package utils;
import java.math.BigDecimal;
import java.util.UUID;


public class PaytmRequestModel {

//	public final static String MID = "WorldP64425807474247";
//	public final static String PAYTM_MERCHANT_KEY="kbzk1DSbJiV_O3p5";
//	public final static String INDUSTRY_TYPE_ID = "Retail";
//	public final static String CHANNEL_ID = "WEB";
//	public final static String WEBSITE = "worldpressplg";
//	public final static String PAYTM_URL = "https://pguat.paytm.com/oltp-web/processTransaction";

    public long CAMPAIGN_ID;
    public String REQUEST_TYPE;
    public UUID ORDER_ID;
    public BigDecimal TXN_AMOUNT;
    public String CHECKSUMHASH;
    public String MOBILE_NO;
    public String CUST_ID;
    public String EMAIL;
    public String CALLBACK_URL;
    public long getCAMPAIGN_ID() {
        return CAMPAIGN_ID;
    }
    public void setCAMPAIGN_ID(long cAMPAIGN_ID) {
        CAMPAIGN_ID = cAMPAIGN_ID;
    }
    public String getREQUEST_TYPE() {
        return REQUEST_TYPE;
    }
    public void setREQUEST_TYPE(String rEQUEST_TYPE) {
        REQUEST_TYPE = rEQUEST_TYPE;
    }
    public UUID getORDER_ID() {
        return ORDER_ID;
    }
    public void setORDER_ID(UUID oRDER_ID) {
        ORDER_ID = oRDER_ID;
    }
    public BigDecimal getTXN_AMOUNT() {
        return TXN_AMOUNT;
    }
    public void setTXN_AMOUNT(BigDecimal tXN_AMOUNT) {
        TXN_AMOUNT = tXN_AMOUNT;
    }
    public String getCHECKSUMHASH() {
        return CHECKSUMHASH;
    }
    public void setCHECKSUMHASH(String cHECKSUMHASH) {
        CHECKSUMHASH = cHECKSUMHASH;
    }
    public String getMOBILE_NO() {
        return MOBILE_NO;
    }
    public void setMOBILE_NO(String mOBILE_NO) {
        MOBILE_NO = mOBILE_NO;
    }
    public String getCUST_ID() {
        return CUST_ID;
    }
    public void setCUST_ID(String cUST_ID) {
        CUST_ID = cUST_ID;
    }
    public String getEMAIL() {
        return EMAIL;
    }
    public void setEMAIL(String eMAIL) {
        EMAIL = eMAIL;
    }
    public String getCALLBACK_URL() {
        return CALLBACK_URL;
    }
    public void setCALLBACK_URL(String cALLBACK_URL) {
        CALLBACK_URL = cALLBACK_URL;
    }
//	public static String getMid() {
//		return MID;
//	}
//	public static String getIndustryTypeId() {
//		return INDUSTRY_TYPE_ID;
//	}
//	public static String getChannelId() {
//		return CHANNEL_ID;
//	}
//	public static String getWebsite() {
//		return WEBSITE;
//	}
//	public static String getPaytmUrl() {
//		return PAYTM_URL;
//	}




}
