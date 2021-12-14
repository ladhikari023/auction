/*
 * Item class, which has item id, name and initial price.
 * Resembles each item the house has
 */
public class Item {
    int ITEMID;
    String NAME;
    double INITIALPRICE;

    public Item(){}

    public int getItemID() {
        return ITEMID;
    }

    public String getName() {
        return NAME;
    }

    public double getInitialPrice() {
        return INITIALPRICE;
    }
}
