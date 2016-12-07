
public class Account{
	private final int MAX_BALANCE = 99999999;
	private int accountNum,
				balance;
	private String name;
	
	/**
	 * @param accountNum
	 * @param balance
	 * @param name 
	 * used to create new accounts, new accounts have a balance of 0 passed in
	 */
	public Account(int accountNum, int balance, String name) {
		this.accountNum = accountNum;
		this.name = name;
		this.balance = balance;
	}
	
	/**
	 * @return balance
	 * accessor for balance field
	 */
	public int getBalance(){
		return balance;
	}
	
	/**
	 * @param amount
	 * @return success or failure of method
	 */
	public boolean increaseBalance(int amount){
		if(balance + amount > MAX_BALANCE)
			return false;
		else{
			balance += amount;
			return true;
		}
	}
	
	/**
	 * @param amount
	 * @return success or failure of method
	 * checks legality of a withdraw transaction, if the transaction
	 * is legal, completes the transaction and reports success
	 */
	public boolean decreaseBalance(int amount){
		if(balance >= amount){
			balance -= amount;
			return true;
		}
		else
			return false;
	}
	
	/**
	 * @return
	 * accessor for account number
	 */
	public int getAccountNum(){
		return accountNum;
	}
	
	/**
	 * @return name
	 * accessor for name field
	 */
	public String getName(){
		return name;
	}	
	
	/**
	 * creates a string containing all account information in a format 
	 * acceptable for export to the master accounts file
	 */
	public String toString(){
		String bal = balance + "",
			   num = accountNum + "";
		while(bal.length() < 3){
			bal = "0" + bal;
		}
		while(num.length() < 8){
			num = "0" + num;
		}
		return num + " " + bal + " " + name;
	}
	
}
