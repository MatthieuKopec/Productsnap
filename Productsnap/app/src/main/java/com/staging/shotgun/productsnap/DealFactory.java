package com.staging.shotgun.productsnap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by yoshi on 29/03/2015.
 */
public class DealFactory {
    private JSONObject  myBody = new JSONObject();

    private Boolean     error = false;

    public JSONObject getMyBody() {
        return myBody;
    }

    public Boolean getError() {
        return error;
    }

    DealFactory(Map data) {
        JSONObject deal = new JSONObject();
        try {
            deal.put("quantity", data.get("quantity"));
            deal.put("title", data.get("title"));
            deal.put("price", data.get("price"));
            deal.put("original_price", Integer.valueOf(99));
            deal.put("description", "Test Shotgun sur le cas de l'application business Android pour Starbucks.");
            deal.put("short_description", "Test Shotgun");
            deal.put("pictures", data.get("pictures"));
            deal.put("address", "my_warm_and_cosy_room_<3");
            deal.put("dealer_id", 1);
            deal.put("expires_at", data.get("expires_at"));
            deal.put("use_by", data.get("use_by"));
            deal.put("notification_msg", "Test Shotgun réussi ;)");
            deal.put("target", "105");
            myBody.put("deal", deal);
        } catch (JSONException e) {
            error = true;
        }
    }
}
