import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/*
    Handles with everything about Items on Auction
    Each Auction House is initialized with a instance of AuctionItems class
 */

public class AuctionItems {
    private final JSONArray jsonArray;
    private final JSONArray toShowArray;
    private JSONArray availableItems = new JSONArray();
    private final ArrayList<Item> items = new ArrayList<>();
    private final ArrayList<Timer> waitTime = new ArrayList<>();

    // Constructor
    public AuctionItems(){
        this.jsonArray = new JSONArray();
        this.toShowArray = new JSONArray();
    }

    // Creates arraylists of items that an auction house can have
    public void createItems(){
        String[] names = {"Television","Bedding Set","Flowers","Kid's Toys",
                "Star war's speical edition hoodie","Game Of Thrones Book","Refrigerator"};
        double[] prices = {120,80,130,200,160,40,37.23};
        for (int i = 0; i < 7; i++) {
            Item item = new Item();
            item.ITEMID = i;
            item.NAME = names[i];
            item.INITIALPRICE = prices[i];
            items.add(item);
        }
    }


    // converts items to jsob objects
    public void createJSONObjects() throws JSONException {
        for (int i = 0; i < items.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Keys.ITEMID.name(), items.get(i).getItemID());
            jsonObject.put(Keys.NAME.name(), items.get(i).getName());
            jsonObject.put(Keys.INIITIALPRICE.name(), items.get(i).getInitialPrice());
            jsonObject.put(Keys.CURRENTBID.name(), items.get(i).getInitialPrice());
            jsonObject.put(Keys.STATE.name(),"AVAILABLE");
            jsonObject.put(Keys.HIGHESTBIDDER.name(), -1);
            jsonObject.put(Keys.ACCOUNTID.name(), -1);
            jsonObject.put(Keys.ACTIVEITEM.name(),"0");
            jsonArray.put(jsonObject);
            availableItems.put(jsonObject);
            waitTime.add(new Timer());
        }
        int count = 0;
        while (count<3){
            int i = getRandomNumber(0, jsonArray.length());
            JSONObject json = jsonArray.getJSONObject(i);
            if (json.getString(Keys.ACTIVEITEM.name()).equals("0")){
                json.remove(Keys.ACTIVEITEM.name());
                json.put(Keys.ACTIVEITEM.name(),"1");
                toShowArray.put(json);
                count++;
            }
        }
    }

    // return Details of maximum three items at a time
    public String showItems() throws JSONException {
        String itemsString = "";
        try {
            for (int i = 0; i < toShowArray.length(); i++) {
                JSONObject jsonOb = toShowArray.getJSONObject(i);
                if (jsonOb.getString(Keys.STATE.name()).matches("AVAILABLE|ONBID")) {
                    itemsString += "ItemID " + jsonOb.getInt(Keys.ITEMID.name()) + " : {";
                    itemsString += jsonOb.getString(Keys.NAME.name()) + "} : ";
                    itemsString += "$" + jsonOb.getInt(Keys.CURRENTBID.name()) + " initial price ";
                    itemsString += "$" + jsonOb.getInt(Keys.INIITIALPRICE.name()) + ",";
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return itemsString;
    }

    // updates every time a item is sold to replace item of house with another item
    public void updateToShowArray() throws JSONException {
        availableItems = new JSONArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            if (json.getString(Keys.ACTIVEITEM.name()).equals("0")){
                availableItems.put(json);
            }
        }

        for (int i = 0; i < toShowArray.length(); i++) {
            JSONObject json = toShowArray.getJSONObject(i);
            if (json.getString(Keys.STATE.name()).matches("ITEMSOLD")){
                toShowArray.remove(i);
            }
        }
        if (availableItems.length()>0){
            int i = getRandomNumber(0,availableItems.length());
            JSONObject json = availableItems.getJSONObject(i);
            json.remove(Keys.ACTIVEITEM.name());
            json.put(Keys.ACTIVEITEM.name(),"1");
            toShowArray.put(json);
        }
    }

    // checks if any of the item is in midst of bid
    public boolean isBidActive() throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.getString(Keys.STATE.name()).equals("ONBID")) {
                return true;
            }
        }
        return false;
    }

    // sets timer to requested item with item id
    synchronized public void bidInterval(int id, TimerTask task) throws JSONException {
        int pos = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).getInt(Keys.ITEMID.name()) == id) {
                jsonArray.getJSONObject(i).remove(Keys.STATE.name());
                jsonArray.getJSONObject(i).put(Keys.STATE.name(),"ONBID");
                pos = i;
                break;
            }
        }

        if (pos == -1) { return; }

        waitTime.get(pos).cancel();
        Timer timer = new Timer();
        waitTime.set(pos, timer);
        timer.schedule(task, 30000);
    }

    // returns json object with item id
    public JSONObject getJSONItem(int itemID) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).getInt(Keys.ITEMID.name()) == itemID) {
                return jsonArray.getJSONObject(i);
            }
        }
        return null;
    }

    // returns true if all item is sold
    public boolean isItemListEmpty() throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonOb = jsonArray.getJSONObject(i);
            if (jsonOb.getString(Keys.STATE.name()).matches("AVAILABLE|ONBID")) {
                return false;
            }
        }
        return true;
    }
    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
