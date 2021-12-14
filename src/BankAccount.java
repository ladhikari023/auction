/*
 * Class responsible for providing bank account to agent and auction house in the initialization
 */
public class BankAccount {
    private double totalBalance;
    private double availableBalance;
    private double balanceOnHold;

   // constructor
    public BankAccount(double amount) {
        this.totalBalance = amount;
        this.availableBalance = totalBalance;
    }

    // returns total balance
    public double getTotalBalance() {
        return totalBalance;
    }
    // returns available funds
    public double getAvailableBalance() {
        return availableBalance;
    }
    // returns balance on hold
    public double getBalanceOnHold() {
        return balanceOnHold;
    }

    // add amount to bank account
    public void addAmount(double amount) {
        if (amount <= 0) return;
        this.totalBalance += amount;
    }
    // block given amount, put the amount on hold
    public boolean blockAmount(double blockAmount) {
        if (blockAmount <= 0 || blockAmount > availableBalance) { return false; }
        balanceOnHold += blockAmount;
        availableBalance = totalBalance - balanceOnHold;
        return true;
    }

    // unblocks given amount from the bank account, removes the amount on hold
    public boolean unBlockAmount(double amountToUnblock) {
        if (amountToUnblock <= 0) { return false; }
        if (balanceOnHold < amountToUnblock) return false;
        balanceOnHold -= amountToUnblock;
        availableBalance += amountToUnblock;
        return true;
    }

    // transfers amount from given account to this account
    public boolean transferAmountTo(BankAccount acc, double transferAmount) {
        if (transferAmount <= 0) { return false; }
        if (balanceOnHold >= transferAmount) {
            balanceOnHold -= transferAmount;
            acc.addAmount(transferAmount);
            totalBalance -= transferAmount;
            return true;
        }
        return false;
    }
}
